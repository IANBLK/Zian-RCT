package com.ianblk.zianrct.reward;

import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.ianblk.zianrct.ZianRCT;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.util.*;
import java.nio.file.Path;

public final class TrainerRewardService {
    private volatile TrainerRewardConfig config;
    private RewardJournal journal;
    private AvecoinsWallet wallet;
    private MinecraftServer server;
    private String walletStatus = "";
    private final Map<UUID, Long> lastClaim = new HashMap<>();
    public void start(MinecraftServer server) {
        this.server = server;
        try {
            config = TrainerRewardConfig.open(FMLPaths.CONFIGDIR.get().resolve("zianrct-rewards.json"));
            journal = RewardJournal.open(server.getWorldPath(LevelResource.ROOT).resolve("data/zianrct-rewards.json"));
            bindWallet();
            ZianRCT.LOGGER.info("Zian RCT trainer rewards ready: {} definitions, {}", config.definitions().size(), walletStatus);
        } catch (Exception error) {
            config = null; journal = null;
            ZianRCT.LOGGER.error("Trainer rewards disabled; invalid configuration/journal preserved", error);
        }
    }
    public void stop() { config = null; journal = null; wallet = null; server = null; lastClaim.clear(); }
    private void bindWallet() {
        wallet = null;
        try { wallet = AvecoinsWallet.bind(); walletStatus = "AVECOINS compatible"; }
        catch (RuntimeException | LinkageError error) { walletStatus = error.getMessage() == null ? "AVECOINS no disponible" : error.getMessage(); }
    }
    private void ready() {
        if (config == null || journal == null || server == null) throw new IllegalStateException("Recompensas no disponibles; revisa el log");
        if (!server.isSameThread()) throw new IllegalStateException("La operación requiere el hilo del servidor");
    }
    public boolean configured(String trainer) {
        TrainerRewardConfig active = config;
        return active != null && active.enabled() && active.definition(trainer) != null;
    }
    public void won(ServerPlayer player, String trainer) {
        try {
            ready();
            if (!config.enabled()) return;
            RewardDefinition definition = config.definition(trainer);
            if (definition == null || definition.empty()) return;
            // Reserve independently from medals; historical reconciliation and admin medal grants never call this.
            if (!journal.reserve(player.getUUID(), trainer, definition)) return;
            ZianRCT.LOGGER.info("[ZIAN-AUDIT] action=trainer_reward_reserve playerUuid={} trainer={} operation={}",
                    player.getUUID(), trainer, RewardJournal.id(player.getUUID(), trainer));
            deliver(player, RewardJournal.id(player.getUUID(), trainer));
        } catch (Exception error) {
            ZianRCT.LOGGER.error("Trainer reward could not be completed for {} / {}", player.getUUID(), trainer, error);
            player.sendSystemMessage(Component.literal("La recompensa del entrenador necesita revisión. No repitas pagos manualmente sin revisar el registro."));
        }
    }
    public List<RewardClaim> claims(UUID player) { ready(); return journal.forPlayer(player); }
    public void notifyPending(ServerPlayer player) {
        if (journal == null) return;
        long count = journal.forPlayer(player.getUUID()).stream().filter(c -> !c.complete()).count();
        if (count > 0) player.sendSystemMessage(Component.literal("Tienes " + count + " recompensa(s) de entrenadores pendiente(s). Usa /zianrct reward pending."));
    }
    public Map<String, RewardDefinition> definitions() { ready(); return config.definitions(); }
    public String status() { return config == null ? "Recompensas desactivadas por error" : "enabled=" + config.enabled() + "; " + walletStatus; }
    public String describe(ServerPlayer player, RewardClaim.Part part) {
        if (part.kind() == RewardClaim.Kind.COINS) return part.amount() + " " + part.data();
        try {
            ItemStack stack = item(player, part.data());
            return stack.isEmpty() ? "Objeto no disponible" : stack.getCount() + " x " + stack.getHoverName().getString();
        } catch (Exception error) { return "Objeto no disponible"; }
    }
    public void claim(ServerPlayer player, UUID id) throws IOException {
        ready();
        long now = System.nanoTime();
        Long before = lastClaim.get(player.getUUID());
        if (before != null && now - before < 1_000_000_000L) throw new IllegalStateException("Espera un segundo antes de reclamar otra vez");
        lastClaim.put(player.getUUID(), now);
        deliver(player, id);
    }
    private void deliver(ServerPlayer player, UUID id) throws IOException {
        ready();
        RewardDelivery.deliver(journal, player.getUUID(), id, new RewardDelivery.Port() {
            public String unavailable(RewardClaim.Part part) {
                if (!player.isAlive() || player.isRemoved()) return "player_not_alive";
                if (part.kind() == RewardClaim.Kind.COINS)
                    return wallet == null ? "avecoins_unavailable" : !wallet.currency(part.data()) ? "unsupported_currency" : null;
                try {
                    ItemStack item = item(player, part.data());
                    return item.isEmpty() || item.getCount() > item.getMaxStackSize()
                            ? "configured_item_unavailable" : !fits(player, item) ? "inventory_full" : null;
                } catch (Exception error) { return "configured_item_invalid"; }
            }
            public RewardDelivery.Result apply(RewardClaim.Part part) throws Exception {
                if (part.kind() == RewardClaim.Kind.COINS) return wallet.credit(player.getUUID(), part.data(), part.amount());
                ItemStack item = item(player, part.data());
                player.getInventory().add(item);
                if (!item.isEmpty()) return RewardDelivery.Result.UNCERTAIN;
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
                // Persist the inventory before closing the external delivery intent.
                var expected = player.saveWithoutId(new CompoundTag()).get("Inventory");
                server.getPlayerList().save(player);
                Path saved = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID() + ".dat");
                try (var channel = java.nio.channels.FileChannel.open(saved, java.nio.file.StandardOpenOption.WRITE)) {
                    channel.force(true);
                }
                var persisted = net.minecraft.nbt.NbtIo.readCompressed(saved, net.minecraft.nbt.NbtAccounter.create(8L * 1024 * 1024));
                if (expected == null || !expected.equals(persisted.get("Inventory"))) return RewardDelivery.Result.UNCERTAIN;
                return RewardDelivery.Result.APPLIED;
            }
        });
        RewardClaim result = journal.get(id);
        player.sendSystemMessage(Component.literal(result.complete() ? "Recompensa de " + result.trainer() + " entregada."
                : result.review() ? "Recompensa " + id + " pendiente de revisión administrativa."
                : "Recompensa " + id + " pendiente. Libera espacio o revisa la cartera y usa /zianrct reward claim " + id));
        ZianRCT.LOGGER.info("[ZIAN-AUDIT] action=trainer_reward_claim playerUuid={} trainer={} operation={} result={}",
                player.getUUID(), result.trainer(), id, result.complete() ? "DELIVERED" : result.review() ? "REVIEW" : "PENDING");
    }
    private static ItemStack item(ServerPlayer player, String data) throws Exception {
        return ItemStack.parseOptional(player.registryAccess(), TagParser.parseTag(data));
    }
    private static boolean fits(ServerPlayer player, ItemStack prize) {
        int capacity = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.isEmpty()) capacity += Math.min(prize.getMaxStackSize(), player.getInventory().getMaxStackSize());
            else if (ItemStack.isSameItemSameComponents(slot, prize))
                capacity += Math.max(0, Math.min(slot.getMaxStackSize(), player.getInventory().getMaxStackSize()) - slot.getCount());
            if (capacity >= prize.getCount()) return true;
        }
        return false;
    }
    private void trainer(String trainer) {
        ready(); TrainerRewardConfig.validId(trainer);
        if (!RCTMod.getInstance().getTrainerManager().isValidId(trainer)) throw new IllegalArgumentException("Entrenador RCT inexistente: " + trainer);
    }
    public void coins(String trainer, String currency, long amount) throws IOException {
        trainer(trainer);
        if (wallet != null && amount > 0 && !wallet.currency(currency)) throw new IllegalArgumentException("Moneda no gestionada por AVECOINS");
        RewardDefinition previous = config.definition(trainer);
        config.set(trainer, new RewardDefinition(currency, amount, previous == null ? List.of() : previous.items()));
    }
    public void addItem(String trainer, ServerPlayer admin) throws IOException {
        trainer(trainer);
        ItemStack stack = admin.getMainHandItem();
        if (stack.isEmpty() || stack.getCount() > stack.getMaxStackSize()) throw new IllegalArgumentException("Sostén un objeto válido en la mano principal");
        var saved = stack.copy().save(admin.registryAccess());
        if (!(saved instanceof CompoundTag tag)) throw new IllegalArgumentException("No se pudo guardar el objeto");
        RewardDefinition previous = config.definition(trainer);
        List<String> items = new ArrayList<>(previous == null ? List.of() : previous.items());
        items.add(tag.toString());
        config.set(trainer, new RewardDefinition(previous == null ? "" : previous.currency(), previous == null ? 0 : previous.coins(), items));
    }
    public void clearItems(String trainer) throws IOException {
        trainer(trainer); RewardDefinition previous = config.definition(trainer);
        if (previous != null) config.set(trainer, new RewardDefinition(previous.currency(), previous.coins(), List.of()));
    }
    public void remove(String trainer) throws IOException { ready(); config.set(trainer, null); }
    public void enabled(boolean enabled) throws IOException { ready(); config.enabled(enabled); }
    public void reload() throws IOException { ready(); config.reload(); bindWallet(); }
    public void resolve(UUID player, UUID id, int component, UUID admin, String decision, String evidence) throws IOException {
        ready(); journal.resolve(player, id, component, admin, decision, evidence);
        ZianRCT.LOGGER.warn("[ZIAN-AUDIT] action=trainer_reward_resolve admin={} playerUuid={} operation={} component={} decision={} evidence={}",
                admin, player, id, component + 1, decision, evidence);
    }
}
