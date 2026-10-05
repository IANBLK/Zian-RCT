package com.ianblk.zianrct.npc;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.google.gson.Gson;
import com.ianblk.zianrct.permission.RctPermissions;
import com.ianblk.zianrct.reward.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

public final class NpcEditorService {
    private static NpcEditorService instance;
    private static final String FROZEN = "ZianRctEditorFrozen";
    private static final Gson GSON = new Gson();
    private final TrainerRewardService rewards;
    private final NpcEditorSessions sessions = new NpcEditorSessions();
    public NpcEditorService(TrainerRewardService rewards) {
        this.rewards = rewards;
        instance = this;
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> sessions.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> sessions.clear());
    }
    public static void receive(ServerPlayer player, NpcEditorAction action) {
        if (instance != null) instance.action(player, action);
    }
    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("zianrct").requires(s -> true)
                .then(Commands.literal("npc").then(Commands.literal("edit")
                        .requires(s -> RctPermissions.allows(s, "admin.npc.edit", true))
                        .executes(c -> {
                            try { open(c.getSource().getPlayerOrException()); return 1; }
                            catch (Exception e) { c.getSource().sendFailure(Component.literal(e.getMessage())); return 0; }
                        }))));
    }
    private boolean allowed(ServerPlayer player, String permission) {
        return RctPermissions.allows(player.createCommandSourceStack(), permission, true);
    }
    public void open(ServerPlayer player) {
        if (!allowed(player, "admin.npc.edit")) throw new IllegalStateException("No tienes permiso para editar NPC.");
        TrainerMob nearby = nearest(player);
        String trainer = nearby == null ? "rassvet_leader_novato" : nearby.getTrainerId();
        send(player, nearby, trainer, search(trainer), "");
    }
    private List<String> search(String prefix) {
        return RCTMod.getInstance().getTrainerManager().getAllData()
                .map(Map.Entry::getKey).filter(id -> id.length() <= 128 && id.contains(prefix.toLowerCase(Locale.ROOT)))
                .sorted().limit(12).toList();
    }
    private TrainerMob nearest(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(TrainerMob.class, player.getBoundingBox().inflate(8),
                npc -> npc.isAlive() && !npc.isRemoved() && npc.distanceToSqr(player) <= 64)
                .stream().min(Comparator.comparingDouble(npc -> npc.distanceToSqr(player))).orElse(null);
    }
    private TrainerMob target(ServerPlayer player, NpcEditorSessions.Session session) {
        if (session.npc() == null) return null;
        var entity = player.serverLevel().getEntity(session.npc());
        if (!(entity instanceof TrainerMob npc) || !npc.isAlive() || npc.isRemoved() || npc.distanceToSqr(player) > 64
                || !session.trainer().equals(npc.getTrainerId())) throw new IllegalStateException("El NPC cambió, está lejos o no está cargado. Selecciónalo de nuevo.");
        return npc;
    }
    private void editable(TrainerMob npc) {
        if (npc == null) throw new IllegalStateException("Selecciona un NPC cercano o haz aparecer uno.");
        if (npc.isInBattle()) throw new IllegalStateException("Espera a que termine el combate.");
    }
    public void action(ServerPlayer player, NpcEditorAction action) {
        if (!allowed(player, "admin.npc.edit")) return;
        var session = sessions.take(player.getUUID(), UUID.fromString(action.nonce()), System.currentTimeMillis());
        if (session == null) {
            player.sendSystemMessage(Component.literal("Sesión caducada o petición repetida. Si no responde, abre /zianrct npc edit de nuevo."));
            return;
        }
        TrainerMob npc = null;
        String selected = session.trainer();
        List<String> matches = session.matches();
        String notice = "";
        try {
            npc = target(player, session);
            switch (action.action()) {
                case "search" -> matches = search(action.value());
                case "select" -> {
                    if (!RCTMod.getInstance().getTrainerManager().isValidId(action.value())) throw new IllegalArgumentException("ID de entrenador RCT inexistente.");
                    selected = action.value(); npc = null; matches = search(selected);
                }
                case "nearby" -> {
                    npc = nearest(player);
                    if (npc == null) throw new IllegalStateException("No hay un entrenador a menos de 8 bloques.");
                    selected = npc.getTrainerId(); matches = search(selected);
                }
                case "spawn" -> {
                    if (!allowed(player, "admin.npc.spawn")) throw new IllegalStateException("No tienes permiso para hacer aparecer entrenadores.");
                    if (!RCTMod.getInstance().getTrainerManager().isValidId(selected)) throw new IllegalArgumentException("Selecciona un ID válido.");
                    if (npc != null) throw new IllegalStateException("Ya hay un NPC seleccionado. Selecciona una plantilla para crear otro.");
                    if (player.serverLevel().getEntitiesOfClass(TrainerMob.class, player.getBoundingBox().inflate(32)).size() >= 32)
                        throw new IllegalStateException("Demasiados entrenadores cercanos.");
                    Vec3 forward = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize().scale(2);
                    Vec3 at = player.position().add(forward);
                    TrainerMob created = TrainerMob.getEntityType().create(player.serverLevel());
                    if (created == null) throw new IllegalStateException("No se pudo crear el entrenador.");
                    created.setTrainerId(selected);
                    created.moveTo(at.x, at.y, at.z, player.getYRot() + 180, 0);
                    created.setHomePos(BlockPos.containing(at));
                    if (!player.serverLevel().getChunkSource().hasChunk(created.blockPosition().getX() >> 4, created.blockPosition().getZ() >> 4)
                            || !player.serverLevel().noCollision(created)
                            || !player.serverLevel().getWorldBorder().isWithinBounds(created.getBoundingBox())) throw new IllegalStateException("Libera espacio delante de ti.");
                    if (!player.serverLevel().addFreshEntity(created)) throw new IllegalStateException("No se pudo añadir el NPC.");
                    npc = created;
                    var spawner = RCTMod.getInstance().getTrainerSpawner();
                    if (!spawner.isRegistered(created)) spawner.register(created);
                    created.setPersistent(true);
                    freeze(created, true);
                    notice = "NPC creado: permanente y sin movimiento.";
                }
                case "persistent" -> { editable(npc); npc.setPersistent(!npc.isPersistenceRequired()); }
                case "movement" -> { editable(npc); freeze(npc, !(npc.isNoAi() || npc.getPersistentData().getBoolean(FROZEN))); }
                case "add_item", "clear_items", "money", "remove_reward" -> {
                    if (!allowed(player, "admin.rewards.configure")) throw new IllegalStateException("No tienes permiso para configurar loot.");
                    String lootId = selected;
                    if ((npc != null && npc.isInBattle()) || RCTMod.getInstance().getTrainerSpawner().getSpawns().stream()
                            .anyMatch(other -> other.isInBattle() && lootId.equals(other.getTrainerId())))
                        throw new IllegalStateException("Espera a que terminen los combates contra este ID de entrenador.");
                    switch (action.action()) {
                        case "add_item" -> rewards.addItem(selected, player);
                        case "clear_items" -> rewards.clearItems(selected);
                        case "money" -> rewards.coins(selected, action.value(), Long.parseLong(action.extra()));
                        case "remove_reward" -> rewards.remove(selected);
                    }
                    notice = "Loot guardado. Solo afecta futuras victorias; único por jugador e ID.";
                }
            }
            com.ianblk.zianrct.ZianRCT.LOGGER.info("[ZIAN-AUDIT] action=npc_editor admin={} npc={} trainer={} operation={} result=APPLIED",
                    player.getUUID(), npc == null ? "template" : npc.getUUID(), selected, action.action());
        } catch (Exception e) { notice = e.getMessage() == null ? "No se pudo aplicar el cambio." : e.getMessage(); }
        send(player, npc, selected, matches, notice);
    }
    private void freeze(TrainerMob npc, boolean frozen) {
        var data = npc.getPersistentData();
        boolean ownedFreeze = data.getBoolean(FROZEN);
        if (frozen && !data.getBoolean(FROZEN)) {
            data.putBoolean("ZianRctEditorPreviousGravity", npc.isNoGravity());
            data.putDouble("ZianRctEditorX", npc.getX()); data.putDouble("ZianRctEditorY", npc.getY()); data.putDouble("ZianRctEditorZ", npc.getZ());
        }
        data.putBoolean(FROZEN, frozen); npc.setNoAi(frozen);
        if (frozen || ownedFreeze) npc.setNoGravity(frozen || data.getBoolean("ZianRctEditorPreviousGravity"));
        npc.getNavigation().stop(); npc.setDeltaMovement(Vec3.ZERO);
    }
    private void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof TrainerMob npc) || npc.level().isClientSide() || npc.isRemoved() || !npc.isAlive()
                || npc.isInBattle() || !npc.getPersistentData().getBoolean(FROZEN)) return;
        var data = npc.getPersistentData();
        npc.getNavigation().stop(); npc.setDeltaMovement(Vec3.ZERO);
        double x = data.getDouble("ZianRctEditorX"), y = data.getDouble("ZianRctEditorY"), z = data.getDouble("ZianRctEditorZ");
        if (npc.position().distanceToSqr(new Vec3(x,y,z)) > 0.0001) npc.teleportTo(x,y,z);
    }
    private void send(ServerPlayer player, TrainerMob npc, String trainer, List<String> matches, String notice) {
        var session = sessions.open(player.getUUID(), npc == null ? null : npc.getUUID(), trainer, matches, System.currentTimeMillis());
        RewardDefinition reward = null;
        List<String> descriptions = new ArrayList<>();
        try {
            reward = rewards.definitions().get(trainer);
            if (reward != null) {
                for (String item : reward.items()) descriptions.add(rewards.describe(player,
                        new RewardClaim.Part(RewardClaim.Kind.ITEM, item, 1, RewardClaim.Phase.PENDING, "")));
            }
        } catch (Exception error) { descriptions.add("Recompensas no disponibles; revisa el log."); }
        descriptions = descriptions.stream().map(s -> s.length() > 256 ? s.substring(0,256) : s).toList();
        if (notice.length() > 256) notice = notice.substring(0,256);
        var state = new NpcEditorState(session.token().toString(), trainer, npc == null ? "" : npc.getUUID().toString(),
                npc != null && npc.isPersistenceRequired(), npc != null && (npc.isNoAi() || npc.getPersistentData().getBoolean(FROZEN)),
                matches, descriptions, reward == null ? "avecoins:coppercoin" : reward.currency(), reward == null ? 0 : reward.coins(), notice);
        PacketDistributor.sendToPlayer(player, new NpcEditorPayload(GSON.toJson(state)));
    }
}
