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
    private com.ianblk.zianrct.rct.RctPackController packs;
    public void attach(com.ianblk.zianrct.rct.RctPackController packs){this.packs=packs;}
    public TrainerRewardService(){
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,this::onInteract);
    }
    private void onInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event){
        if(event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof com.gitlab.srcmc.rctmod.world.entities.TrainerMob npc){
            String reason=blocked(player,npc.getTrainerId());
            if(reason!=null){event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                player.sendSystemMessage(Component.literal(reason));}
        }
    }
    public String blocked(ServerPlayer player,String trainer){
        if(packs!=null && packs.isBusy())return "Se está recargando la configuración de entrenadores. Espera un momento.";
        String requested=com.ianblk.zianrct.rct.RctTrainerOptions.formats().get(trainer);
        try{
            if(requested!=null && !requested.equals(RCTMod.getInstance().getTrainerManager().getData(trainer).getTrainerTeam().getBattleFormat().name()))
                return "El formato de este entrenador aún no se ha verificado. Consulta al administrador.";
            if(config==null || !config.enabled())return null;
            var definition=config.definition(trainer);
            if(definition==null || definition.empty())return null;
            if(journal==null)return "El registro de premios no está disponible. Consulta al administrador.";
            journal.ensureHealthy();
            if(definition.mode()!=RewardDefinition.Mode.REPEAT)return null;
            if(RCTMod.getInstance().getTrainerManager().getData(trainer).getMaxTrainerDefeats()!=-1
                    || RCTMod.getInstance().getTrainerManager().getData(trainer).getMaxTrainerWins()!=-1)
                return "La revancha aún no se ha verificado. Consulta al administrador.";
            long remaining=journal.remaining(player.getUUID(),trainer,definition,System.currentTimeMillis());
            if(remaining==-2)return "Reclama o resuelve el premio anterior antes de repetir este desafío. /zianrct reward pending";
            if(remaining>0)return "Este desafío vuelve a estar disponible en "+waitText(remaining)+".";
            return null;
        }catch(Exception error){return "No se pudo verificar este desafío. Consulta al administrador.";}
    }
    private static String waitText(long millis){
        long seconds=(millis+999)/1000;return (seconds/3600)+"h "+((seconds%3600)/60)+"m "+(seconds%60)+"s";
    }
    private volatile TrainerRewardConfig config;
    private RewardJournal journal;
    private AvecoinsWallet wallet;
    private MinecraftServer server;
    private String walletStatus = "";
    private final Map<UUID, Long> lastClaim = new HashMap<>();
    public void start(MinecraftServer server) {
        this.server = server;
        config=null;journal=null;wallet=null;
        try {
            config = TrainerRewardConfig.open(FMLPaths.CONFIGDIR.get().resolve("zianrct-rewards.json"));
            journal = RewardJournal.open(server.getWorldPath(LevelResource.ROOT).resolve("data/zianrct-rewards.json"));
            bindWallet();
            ZianRCT.LOGGER.info("Zian RCT trainer rewards ready: {} definitions, {}", config.definitions().size(), walletStatus);
        } catch (Exception error) {
            journal = null;wallet=null;
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
    public void won(ServerPlayer player, String trainer, UUID battle) {
        try {
            ready();
            if (!config.enabled()) return;
            RewardDefinition definition = config.definition(trainer);
            if (definition == null || definition.empty()) return;
            // Reserve independently from medals; historical reconciliation and admin medal grants never call this.
            if (!journal.reserveAt(player.getUUID(), trainer, definition,System.currentTimeMillis(),battle)) {
                if(definition.mode()==RewardDefinition.Mode.REPEAT)player.sendSystemMessage(Component.literal(next(player,trainer)));
                return;
            }
            RewardClaim claim=journal.latest(player.getUUID(),trainer);
            ZianRCT.LOGGER.info("[ZIAN-AUDIT] action=trainer_reward_reserve playerUuid={} trainer={} operation={}",
                    player.getUUID(), trainer, claim.id());
            deliver(player, claim.id());
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
    public String next(ServerPlayer player,String trainer){
        ready();var definition=config.definition(trainer);
        if(definition==null || definition.empty())return "Sin premio configurado.";
        long remaining=journal.remaining(player.getUUID(),trainer,definition,System.currentTimeMillis());
        if(remaining==-1)return "Premio único ya obtenido.";
        if(remaining==-2)return "Premio anterior pendiente; reclámalo o pide revisión.";
        return remaining==0?"Premio disponible al ganar.":"Próximo premio en "+waitText(remaining)+".";
    }
    private void noBattle(String trainer){
        if(RCTMod.getInstance().getTrainerSpawner().getSpawns().stream().anyMatch(n->trainer.equals(n.getTrainerId()) && n.isInBattle()))
            throw new IllegalStateException("Espera a que terminen los combates contra este entrenador.");
        if(packs==null || packs.isBusy())throw new IllegalStateException("Ya hay una recarga de entrenadores en curso.");
    }
    public void policy(String trainer,RewardDefinition.Mode mode,long minutes) throws IOException {
        trainer(trainer);noBattle(trainer);
        if(mode==RewardDefinition.Mode.REPEAT){packs.requireTrainerSource(server,trainer,true);packs.requireTrainerSource(server,trainer,false);}
        RewardDefinition previous=config.definition(trainer);
        if(previous==null)previous=new RewardDefinition("",0,List.of());
        config.set(trainer,new RewardDefinition(previous.currency(),previous.coins(),previous.items(),mode,mode==RewardDefinition.Mode.UNIQUE?0:minutes));
        com.ianblk.zianrct.rct.RctTrainerOptions.rewardConfig(config.definitions());
        packs.regenerate(server,"reward policy changed");
    }
    public void format(String trainer,String format) throws IOException {
        trainer(trainer);noBattle(trainer);
        packs.requireTrainerSource(server,trainer,false);
        com.ianblk.zianrct.rct.RctTrainerOptions.format(trainer,format);
        packs.regenerate(server,"trainer battle format changed");
    }
    public String format(String trainer){
        String configured=com.ianblk.zianrct.rct.RctTrainerOptions.formats().get(trainer);
        if(configured!=null)return configured;
        try{return RCTMod.getInstance().getTrainerManager().getData(trainer).getTrainerTeam().getBattleFormat().name();}
        catch(RuntimeException error){return "GEN_9_SINGLES";}
    }
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
        if(previous==null)previous=new RewardDefinition("",0,List.of());
        config.set(trainer, previous.prize(currency, amount, previous.items()));
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
        if(previous==null)previous=new RewardDefinition("",0,List.of());
        config.set(trainer, previous.prize(previous.currency(),previous.coins(),items));
    }
    public void clearItems(String trainer) throws IOException {
        trainer(trainer); RewardDefinition previous = config.definition(trainer);
        if (previous != null) config.set(trainer, previous.prize(previous.currency(), previous.coins(), List.of()));
    }
    public void remove(String trainer) throws IOException { ready();noBattle(trainer);config.set(trainer,null);
        com.ianblk.zianrct.rct.RctTrainerOptions.rewardConfig(config.definitions());packs.regenerate(server,"reward removed"); }
    public void enabled(boolean enabled) throws IOException { ready(); config.enabled(enabled); }
    public void reload() throws IOException { ready();if(packs.isBusy())throw new IOException("Recarga en curso");
        if(RCTMod.getInstance().getTrainerSpawner().getSpawns().stream().anyMatch(n->n.isInBattle()))throw new IOException("Espera a que terminen los combates");
        config.reload();bindWallet();com.ianblk.zianrct.rct.RctTrainerOptions.rewardConfig(config.definitions());packs.regenerate(server,"reward config reload"); }
    public void resolve(UUID player, UUID id, int component, UUID admin, String decision, String evidence) throws IOException {
        ready(); journal.resolve(player, id, component, admin, decision, evidence);
        ZianRCT.LOGGER.warn("[ZIAN-AUDIT] action=trainer_reward_resolve admin={} playerUuid={} operation={} component={} decision={} evidence={}",
                admin, player, id, component + 1, decision, evidence);
    }
}
