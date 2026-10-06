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
    private static final int PAGE_SIZE = 5;
    private final TrainerRewardService rewards;
    private final NpcEditorSessions sessions = new NpcEditorSessions();
    public NpcEditorService(TrainerRewardService rewards) {
        this.rewards = rewards; instance = this;
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> sessions.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> sessions.clear());
    }
    public static void receive(ServerPlayer player, NpcEditorAction action) {
        if (instance != null) instance.action(player, action);
    }
    public static void saveCustom(ServerPlayer player,com.ianblk.zianrct.creator.CustomTrainerSave payload){
        if(instance!=null)instance.custom(player,payload);
    }
    private void custom(ServerPlayer player,com.ianblk.zianrct.creator.CustomTrainerSave payload){
        if(!allowed(player,"admin.npc.edit") || !allowed(player,"admin.trainer.create"))return;
        UUID token=UUID.fromString(payload.nonce());
        var session=sessions.take(player.getUUID(),token,System.currentTimeMillis());
        if(session==null){
            var active=sessions.peek(player.getUUID());
            if(active!=null && active.mode()==NpcEditorState.Mode.DESIGN && active.token().equals(token))
                send(player,null,active.trainer(),NpcEditorState.Mode.DESIGN,"",0,"","Sesión renovada. Tu borrador se conserva; pulsa Guardar de nuevo.");
            return;
        }
        if(session.mode()!=NpcEditorState.Mode.DESIGN)return;
        try{
            var definition=com.ianblk.zianrct.creator.CustomTrainerStore.decode(payload.json());
            var old=com.ianblk.zianrct.creator.CustomTrainerStore.get(definition.id());
            boolean trialChanged=!Objects.equals(definition.trial(),old==null?null:old.trial())
                || (definition.trial()!=null && old!=null && (!definition.team().equals(old.team()) || !definition.format().equals(old.format())));
            if(trialChanged && !allowed(player,"admin.legendary.configure"))throw new IllegalArgumentException("Necesitas zianrct.admin.legendary.configure para cambiar una prueba.");
            if(!session.trainer().isEmpty() && !session.trainer().equals(definition.id()))throw new IllegalArgumentException("No cambies el ID de una definición existente.");
            if(session.trainer().isEmpty() && (com.ianblk.zianrct.creator.CustomTrainerStore.get(definition.id())!=null
                    || RCTMod.getInstance().getTrainerManager().isValidId(definition.id())))throw new IllegalArgumentException("Ya existe este ID. Edita su definición desde un NPC propio.");
            rewards.saveCustom(definition);
            send(player,null,definition.id(),NpcEditorState.Mode.CREATE,definition.id(),0,"","Definición guardada. Espera la recarga y crea el NPC seleccionado.");
        }catch(Exception error){
            String message=error.getMessage()==null?"No se pudo guardar":error.getMessage();
            // Refresh the nonce, while the client preserves the submitted draft after a failed save.
            send(player,null,session.trainer(),NpcEditorState.Mode.DESIGN,"",0,"",message);
        }
    }
    private int command(net.minecraft.commands.CommandSourceStack source) {
        try { open(source.getPlayerOrException()); return 1; }
        catch (Exception e) { source.sendFailure(Component.literal(e.getMessage())); return 0; }
    }
    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("zianrct").requires(s -> true)
                .then(Commands.literal("npc").requires(s -> RctPermissions.allows(s,"admin.npc.edit",true))
                        .executes(c -> command(c.getSource()))
                        .then(Commands.literal("edit").executes(c -> command(c.getSource())))));
    }
    private boolean allowed(ServerPlayer player, String permission) {
        return RctPermissions.allows(player.createCommandSourceStack(), permission, true);
    }
    public void open(ServerPlayer player) {
        if (!allowed(player,"admin.npc.edit")) throw new IllegalStateException("No tienes permiso para editar NPC.");
        send(player,null,"",NpcEditorState.Mode.LIST,"",0,"","");
    }
    private List<String> search(String query) {
        return RCTMod.getInstance().getTrainerManager().getAllData().map(Map.Entry::getKey)
                .filter(id -> id.length() <= 128 && id.contains(query.toLowerCase(Locale.ROOT))).sorted().toList();
    }
    private List<TrainerMob> loaded(ServerPlayer player) {
        List<TrainerMob> result = new ArrayList<>();
        for (var entity : player.serverLevel().getAllEntities())
            if (entity instanceof TrainerMob npc && npc.isAlive() && !npc.isRemoved()) result.add(npc);
        result.sort(Comparator.comparingDouble((TrainerMob n) -> n.distanceToSqr(player)).thenComparing(n -> n.getUUID().toString()));
        return result;
    }
    private TrainerMob lookup(ServerPlayer player, UUID id) {
        var entity = player.serverLevel().getEntity(id);
        if (!(entity instanceof TrainerMob npc) || !npc.isAlive() || npc.isRemoved())
            throw new IllegalStateException("El NPC ya no está cargado en tu dimensión. Actualiza la lista.");
        return npc;
    }
    private TrainerMob target(ServerPlayer player, NpcEditorSessions.Session session) {
        if (session.npc() == null) throw new IllegalStateException("Selecciona Modificar en la lista de NPC.");
        TrainerMob npc = lookup(player,session.npc());
        if (!session.trainer().equals(npc.getTrainerId())) throw new IllegalStateException("El entrenador cambió; selecciónalo de nuevo.");
        return npc;
    }
    private void editable(TrainerMob npc) {
        if (npc.isInBattle()) throw new IllegalStateException("Espera a que termine el combate.");
    }
    public void action(ServerPlayer player, NpcEditorAction action) {
        if (!allowed(player,"admin.npc.edit")) return;
        var session = sessions.take(player.getUUID(),UUID.fromString(action.nonce()),System.currentTimeMillis());
        if (session == null) {
            player.sendSystemMessage(Component.literal("Sesión caducada o petición repetida. Abre /zianrct npc de nuevo."));
            return;
        }
        var mode = session.mode();
        String selected=session.trainer(),query=session.query(),confirmation="";
        int page=session.page();
        TrainerMob npc=null;
        String notice="";
        try {
            if (!NpcEditorProtocol.allowed(mode,session.confirmation(),action.action()))
                throw new IllegalStateException("Acción no disponible en esta pantalla.");
            // Navigation never needs to resolve a stale entity.
            if (mode==NpcEditorState.Mode.EDIT && !Set.of("list","create","designer").contains(action.action())) npc=target(player,session);
            switch(action.action()) {
                case "list" -> {mode=NpcEditorState.Mode.LIST;npc=null;selected="";query="";page=0;}
                case "create" -> {mode=NpcEditorState.Mode.CREATE;npc=null;selected="";query="rassvet";page=0;}
                case "designer" -> {
                    require(player,"admin.trainer.create");
                    if(mode==NpcEditorState.Mode.EDIT && com.ianblk.zianrct.creator.CustomTrainerStore.get(selected)==null)
                        throw new IllegalArgumentException("Solo puedes editar definiciones propias; las de RCT se conservan.");
                    if(mode==NpcEditorState.Mode.CREATE)selected="";
                    mode=NpcEditorState.Mode.DESIGN;npc=null;query="";page=0;
                }
                case "search" -> {query=action.value();page=0;}
                case "choose_template" -> {
                    if (!RCTMod.getInstance().getTrainerManager().isValidId(action.value())) throw new IllegalArgumentException("ID de entrenador inexistente.");
                    selected=action.value();
                }
                case "next" -> page++;
                case "previous" -> page--;
                case "select_npc", "select_delete" -> {
                    UUID id=UUID.fromString(action.value());
                    if (!session.visibleNpcs().contains(id)) throw new IllegalArgumentException("Selecciona un NPC de la página actual.");
                    npc=lookup(player,id);selected=npc.getTrainerId();mode=NpcEditorState.Mode.EDIT;
                    if(action.action().equals("select_delete")){
                        require(player,"admin.npc.delete");editable(npc);confirmation="delete";
                    }
                }
                case "spawn_prompt" -> {
                    require(player,"admin.npc.spawn");
                    if(selected.isEmpty() || !RCTMod.getInstance().getTrainerManager().isValidId(selected))
                        throw new IllegalArgumentException("Selecciona primero un entrenador de la lista.");
                    confirmation="spawn";
                }
                case "spawn" -> {
                    require(player,"admin.npc.spawn");
                    if(!RCTMod.getInstance().getTrainerManager().isValidId(selected)) throw new IllegalArgumentException("El entrenador ya no está disponible.");
                    String id=selected;
                    if(loaded(player).stream().anyMatch(n -> id.equals(n.getTrainerId()) && n.distanceToSqr(player)<=256))
                        throw new IllegalStateException("Ya existe este entrenador a menos de 16 bloques. Modifícalo desde la lista.");
                    if(player.serverLevel().getEntitiesOfClass(TrainerMob.class,player.getBoundingBox().inflate(32)).size()>=32)
                        throw new IllegalStateException("Demasiados entrenadores cercanos.");
                    Vec3 ahead=Vec3.directionFromRotation(0,player.getYRot()).scale(2);
                    Vec3 at=player.position().add(ahead);
                    TrainerMob created=com.ianblk.zianrct.creator.CustomTrainerStore.get(selected)!=null
                            ?com.ianblk.zianrct.creator.CustomTrainerEntities.TYPE.get().create(player.serverLevel())
                            :TrainerMob.getEntityType().create(player.serverLevel());
                    if(created==null) throw new IllegalStateException("No se pudo crear el entrenador.");
                    created.setTrainerId(selected);created.moveTo(at.x,at.y,at.z,player.getYRot()+180,0);
                    created.setHomePos(BlockPos.containing(at));
                    if(!player.serverLevel().getChunkSource().hasChunk(created.blockPosition().getX()>>4,created.blockPosition().getZ()>>4)
                            || !player.serverLevel().noCollision(created)
                            || !player.serverLevel().getWorldBorder().isWithinBounds(created.getBoundingBox()))
                        throw new IllegalStateException("Libera espacio delante de ti.");
                    if(!player.serverLevel().addFreshEntity(created)) throw new IllegalStateException("No se pudo añadir el NPC.");
                    npc=created;mode=NpcEditorState.Mode.EDIT;
                    var spawner=RCTMod.getInstance().getTrainerSpawner();
                    if(!spawner.isRegistered(created)) spawner.register(created);
                    created.setPersistent(true);freeze(created,true);
                    notice="NPC creado. Ahora estás modificando ese NPC.";
                }
                case "persistent" -> {editable(npc);npc.setPersistent(!npc.isPersistenceRequired());}
                case "movement" -> {editable(npc);freeze(npc,!(npc.isNoAi()||npc.getPersistentData().getBoolean(FROZEN)));}
                case "move" -> {require(player,"admin.npc.move");editable(npc);moveHere(player,npc);notice="NPC movido a tu bloque y orientación.";}
                case "format" -> {
                    require(player,"admin.npc.configure");editable(npc);rewards.format(selected,action.value());
                    notice="Formato guardado para este ID. Aplicando recarga de RCT.";
                }
                case "delete_prompt" -> {require(player,"admin.npc.delete");editable(npc);confirmation="delete";}
                case "delete" -> {
                    require(player,"admin.npc.delete");editable(npc);
                    com.ianblk.zianrct.ZianRCT.LOGGER.info("[ZIAN-AUDIT] action=npc_delete admin={} npc={} trainer={}",player.getUUID(),npc.getUUID(),selected);
                    npc.discard();npc=null;selected="";mode=NpcEditorState.Mode.LIST;page=0;notice="NPC eliminado. Medallas y recompensas conservadas.";
                }
                case "cancel", "refresh" -> {}
                case "add_item","clear_items","money","remove_reward","policy" -> {
                    require(player,"admin.rewards.configure");editable(npc);
                    String id=selected;
                    if(RCTMod.getInstance().getTrainerSpawner().getSpawns().stream()
                            .anyMatch(n -> n.isInBattle() && id.equals(n.getTrainerId())))
                        throw new IllegalStateException("Espera a que terminen los combates contra este ID.");
                    switch(action.action()){
                        case "add_item" -> rewards.addItem(selected,player);
                        case "clear_items" -> rewards.clearItems(selected);
                        case "money" -> rewards.coins(selected,action.value(),Long.parseLong(action.extra()));
                        case "remove_reward" -> rewards.remove(selected);
                        case "policy" -> rewards.policy(selected,RewardDefinition.Mode.valueOf(action.value()),Long.parseLong(action.extra()));
                    }
                    notice=action.action().equals("policy")?"Tipo guardado. Aplicando revancha; premios previos conservados.":"Loot guardado; premios reservados conservados.";
                }
            }
            com.ianblk.zianrct.ZianRCT.LOGGER.info("[ZIAN-AUDIT] action=npc_manager admin={} npc={} trainer={} operation={} result=APPLIED",
                    player.getUUID(),npc==null?"template":npc.getUUID(),selected,action.action());
        } catch(Exception error) {
            notice=error.getMessage()==null?"No se pudo aplicar el cambio.":error.getMessage();
            if(mode==NpcEditorState.Mode.EDIT && npc==null){mode=NpcEditorState.Mode.LIST;selected="";}
        }
        send(player,npc,selected,mode,query,page,confirmation,notice);
    }
    private void require(ServerPlayer player,String permission){
        if(!allowed(player,permission))throw new IllegalStateException("No tienes permiso: zianrct."+permission);
    }
    private void moveHere(ServerPlayer player,TrainerMob npc){
        var at=NpcPlacement.at(player.getX(),player.getY(),player.getZ(),player.getYRot());
        var box=npc.getBoundingBox().move(at.x()-npc.getX(),at.y()-npc.getY(),at.z()-npc.getZ());
        var level=player.serverLevel();
        if(at.y()<level.getMinBuildHeight() || box.maxY>level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(box) || level.getBlockCollisions(npc,box).iterator().hasNext()
                || !level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,box,
                        e -> e!=npc && e!=player && e.isAlive()).isEmpty())
            throw new IllegalStateException("Tu bloque no tiene espacio libre para el NPC.");
        boolean persistent=npc.isPersistenceRequired();
        // Release the old RCT persistence registration before relocation, then register the new location.
        if(persistent)npc.setPersistent(false);
        try{
            npc.getNavigation().stop();npc.setDeltaMovement(Vec3.ZERO);
            npc.teleportTo(at.x(),at.y(),at.z());npc.setYRot(at.yaw());npc.setYHeadRot(at.yaw());npc.setYBodyRot(at.yaw());npc.setXRot(0);
            npc.setHomePos(BlockPos.containing(at.x(),at.y(),at.z()));
            if(npc.getPersistentData().getBoolean(FROZEN)){
                var data=npc.getPersistentData();data.putDouble("ZianRctEditorX",at.x());data.putDouble("ZianRctEditorY",at.y());data.putDouble("ZianRctEditorZ",at.z());
            }
        }finally{if(persistent&&!npc.isRemoved())npc.setPersistent(true);}
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
    private void send(ServerPlayer player,TrainerMob npc,String trainer,NpcEditorState.Mode mode,String query,int requestedPage,String confirmation,String notice){
        List<TrainerMob> all=mode==NpcEditorState.Mode.LIST?loaded(player):List.of();
        List<String> templates=mode==NpcEditorState.Mode.CREATE?search(query):List.of();
        int count=mode==NpcEditorState.Mode.LIST?all.size():templates.size();
        int pages=Math.max(1,(count+PAGE_SIZE-1)/PAGE_SIZE);
        int page=Math.max(0,Math.min(requestedPage,pages-1)),first=page*PAGE_SIZE,end=Math.min(count,first+PAGE_SIZE);
        List<String> matches=mode==NpcEditorState.Mode.CREATE?templates.subList(first,end):List.of();
        List<NpcEditorState.NpcView> views=new ArrayList<>();
        if(mode==NpcEditorState.Mode.LIST)for(TrainerMob n:all.subList(first,end)){
            var pos=n.blockPosition();
            views.add(new NpcEditorState.NpcView(n.getUUID().toString(),n.getTrainerId(),pos.getX(),pos.getY(),pos.getZ(),(int)Math.sqrt(n.distanceToSqr(player))));
        }
        Set<UUID> visible=new HashSet<>();
        views.forEach(v -> visible.add(UUID.fromString(v.uuid())));
        var session=sessions.open(player.getUUID(),npc==null?null:npc.getUUID(),trainer,query,mode,confirmation,page,visible,System.currentTimeMillis());
        RewardDefinition definition=null;List<String> descriptions=new ArrayList<>();
        if(mode==NpcEditorState.Mode.EDIT)try{
            definition=rewards.definitions().get(trainer);
            if(definition!=null)for(String item:definition.items())descriptions.add(rewards.describe(player,
                    new RewardClaim.Part(RewardClaim.Kind.ITEM,item,1,RewardClaim.Phase.PENDING,"")));
        }catch(Exception error){descriptions.add("Recompensas no disponibles; revisa el log.");}
        descriptions=descriptions.stream().map(s -> s.length()>256?s.substring(0,256):s).toList();
        if(notice.length()>256)notice=notice.substring(0,256);
        String next="";
        if(mode==NpcEditorState.Mode.EDIT)try{next=rewards.next(player,trainer);}catch(RuntimeException error){next="Recompensas no disponibles.";}
        String draft="";
        if(mode==NpcEditorState.Mode.DESIGN){
            var own=com.ianblk.zianrct.creator.CustomTrainerStore.get(trainer);
            if(own!=null)draft=GSON.toJson(own);
        }
        var state=new NpcEditorState(session.token().toString(),mode,trainer,npc==null?"":npc.getUUID().toString(),
                npc!=null && npc.isPersistenceRequired(),npc!=null && (npc.isNoAi()||npc.getPersistentData().getBoolean(FROZEN)),
                matches,views,descriptions,definition==null?"avecoins:coppercoin":definition.currency(),definition==null?0:definition.coins(),
                notice,query,page,pages,confirmation,definition==null?"UNIQUE":definition.mode().name(),
                definition==null?0:definition.cooldownMinutes(),next,mode==NpcEditorState.Mode.EDIT?rewards.format(trainer):"GEN_9_SINGLES",draft);
        PacketDistributor.sendToPlayer(player,new NpcEditorPayload(GSON.toJson(state)));
    }
}
