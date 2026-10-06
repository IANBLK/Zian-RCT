package com.ianblk.zianrct.standalone;

import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.battle.BattleState;
import com.gitlab.srcmc.rctapi.api.events.*;
import com.gitlab.srcmc.rctapi.api.trainer.*;
import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.creator.IndependentTrainerMob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.*;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Own API instance and lazy battle teams. Contains no linkage to RCTMod. */
public final class StandaloneRuntime {
    private static final StandaloneRuntime INSTANCE=new StandaloneRuntime();
    private RCTApi api;
    private MinecraftServer server;
    private Map<String,TrainerDefinition> definitions=Map.of();
    private Map<String,com.ianblk.zianrct.creator.CustomTrainer> customSnapshot=Map.of();
    private final Map<UUID,PlayerProgress> progress=new HashMap<>();
    private final Map<UUID,UUID> activeNpcBattles=new HashMap<>();
    private final Map<UUID,String> battleRegistrations=new HashMap<>();
    private final Set<UUID> pendingCleanup=new LinkedHashSet<>();
    private final com.gitlab.srcmc.rctapi.api.events.EventListener<BattleState> ended=this::ended;
    private StandaloneRuntime(){
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)->{
            if(event.getServer()!=server)return;
            for(UUID id:List.copyOf(pendingCleanup)){
                getSpawns().stream().filter(n->id.equals(activeNpcBattles.get(n.getUUID()))).forEach(IndependentTrainerMob::finishBattle);
                activeNpcBattles.values().removeIf(id::equals);String key=battleRegistrations.remove(id);if(key!=null)api().getTrainerRegistry().unregisterById(key);
                pendingCleanup.remove(id);
            }
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->{progress.remove(event.getEntity().getUUID());if(api!=null)api.getTrainerRegistry().unregisterById("player:"+event.getEntity().getUUID());});
    }
    public static StandaloneRuntime getInstance(){return INSTANCE;}
    public RCTApi api(){if(api==null)throw new IllegalStateException("RCTAPI aún no está listo");return api;}
    public void start(MinecraftServer server){
        this.server=server;api=RCTApi.initInstance("zianrct");api.getTrainerRegistry().init(server);
        api.getEventContext().register(Events.BATTLE_ENDED,ended);
    }
    public void stop(){
        if(api!=null){
            for(var state:List.copyOf(api.getBattleManager().getStates()))api.getBattleManager().end(state.getBattle().getBattleId(),true);
            api.getEventContext().unregister(Events.BATTLE_ENDED,ended);api.getTrainerRegistry().clear();}
        definitions=Map.of();customSnapshot=Map.of();progress.clear();activeNpcBattles.clear();battleRegistrations.clear();pendingCleanup.clear();server=null;
    }
    public void replace(Map<String,TrainerDefinition> candidate){
        if(!api().getBattleManager().getStates().isEmpty() || !activeNpcBattles.isEmpty())throw new IllegalStateException("Espera a que terminen los combates");
        definitions=Map.copyOf(candidate);customSnapshot=Map.copyOf(com.ianblk.zianrct.creator.CustomTrainerStore.all());api().getTrainerRegistry().clearNPCs();
    }
    public StandaloneRuntime getTrainerManager(){return this;}
    public StandaloneRuntime getTrainerSpawner(){return this;}
    public long retainedNpcTeams(){return api().getTrainerRegistry().getIds().stream().filter(id->api().getTrainerRegistry().getById(id) instanceof TrainerNPC).count();}
    public boolean isCurrent(String id){return Objects.equals(customSnapshot.get(id),com.ianblk.zianrct.creator.CustomTrainerStore.get(id));}
    public boolean isValidId(String id){return definitions.containsKey(id);}
    public Stream<Map.Entry<String,TrainerDefinition>> getAllData(){return definitions.entrySet().stream();}
    public TrainerDefinition getData(String id){var d=definitions.get(id);if(d==null)throw new IllegalArgumentException("Entrenador inexistente: "+id);return d;}
    public PlayerProgress getData(Player player){
        if(!(player instanceof ServerPlayer target))throw new IllegalStateException("Solo disponible en servidor");
        return progress.computeIfAbsent(player.getUUID(),uuid->{
            Path path=server.getWorldPath(LevelResource.ROOT).resolve("data/zianrct-progress/"+uuid+".json");
            try{
                Set<String> imported=Files.exists(path)?Set.of():legacyProgress(target);
                boolean league=ConfigState.current().activeProfileConfig().chain().stream().anyMatch(e->imported.contains(e.trainer()));
                return new PlayerProgress(ProgressLedger.open(path,imported,league));
            }
            catch(Exception error){throw new IllegalStateException("No se pudo leer tu progreso; no se reiniciará",error);}
        });
    }
    private Set<String> legacyProgress(ServerPlayer player) throws java.io.IOException {
        Set<String> result=new LinkedHashSet<>();
        for(var level:server.getAllLevels()){
            Path file=net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),server.getWorldPath(LevelResource.ROOT)).resolve("data/rctmod.player."+player.getUUID()+".stat.dat");
            if(!Files.exists(file))continue;
            var root=NbtIo.readCompressed(file,NbtAccounter.create(8L*1024*1024));
            if(!root.contains("data",Tag.TAG_COMPOUND) || !root.getCompound("data").contains("progressDefeats",Tag.TAG_LIST))
                throw new java.io.IOException("El progreso anterior de RCT no tiene un formato reconocido");
            for(var entry:root.getCompound("data").getList("progressDefeats",Tag.TAG_STRING))result.add(entry.getAsString());
            var profile=ConfigState.current().activeProfileConfig();
            if(root.getCompound("data").getCompound("completedSeries").getInt(profile.series())>0)
                profile.chain().forEach(e->result.add(e.trainer()));
        }
        if(!result.isEmpty())ZianRCT.LOGGER.info("Imported legacy RCT progression for {} without modifying source data",player.getUUID());
        return result;
    }
    public int getActivePokemon(Player player){return (int)Arrays.stream(new TrainerPlayer((ServerPlayer)player).getTeam()).filter(p->p!=null && p.getCurrentHealth()>0).count();}
    public int getPlayerLevel(Player player){return Arrays.stream(new TrainerPlayer((ServerPlayer)player).getTeam()).filter(Objects::nonNull).mapToInt(p->p.getLevel()).max().orElse(0);}
    public boolean isInBattle(Player player){return com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer((ServerPlayer)player)!=null;}
    public List<IndependentTrainerMob> getSpawns(){
        if(server==null)return List.of();List<IndependentTrainerMob> result=new ArrayList<>();
        for(var level:server.getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof IndependentTrainerMob npc && !npc.isRemoved())result.add(npc);
        return result;
    }
    public boolean isRegistered(IndependentTrainerMob npc){return true;}
    public void register(IndependentTrainerMob npc){} // Vanilla entity persistence owns the lifecycle.
    public boolean isLeague(String trainer){return ConfigState.current().activeProfileConfig().chain().stream().anyMatch(e->e.trainer().equals(trainer));}
    public boolean busy(UUID npc){return activeNpcBattles.containsKey(npc);}
    public boolean start(IndependentTrainerMob npc,ServerPlayer player){
        String key="npc:"+npc.getUUID();var registry=api().getTrainerRegistry();
        TrainerDefinition definition=getData(npc.getTrainerId());
        TrainerPlayer participant=registry.getById("player:"+player.getUUID(),TrainerPlayer.class);
        if(participant==null)participant=registry.registerPlayer("player:"+player.getUUID(),player);
        TrainerNPC trainer=registry.registerNPC(key,definition.model());trainer.setEntity(npc);
        try{
            UUID id=api().getBattleManager().startBattle(List.of(participant),List.of(trainer),(com.gitlab.srcmc.rctapi.api.battle.BattleFormatProvider)definition.format(),definition.rules());
            if(id==null){registry.unregisterById(key);return false;}
            activeNpcBattles.put(npc.getUUID(),id);battleRegistrations.put(id,key);return true;
        }catch(RuntimeException error){registry.unregisterById(key);throw error;}
    }
    private void ended(Event<BattleState> event){
        BattleState state=event.getValue();if(state==null || state.getBattle()==null)return;
        if(!server.isSameThread()){server.execute(()->ended(event));return;}
        UUID id=state.getBattle().getBattleId();
        try{
            if(!state.isEndForced()){
                for(var winner:state.getWinners())if(winner.getEntity() instanceof ServerPlayer player)
                    for(var loser:state.getLosers())if(loser.getEntity() instanceof IndependentTrainerMob npc){
                        npc.reply(player,"on_battle_lost");
                    }
                for(var loser:state.getLosers())if(loser.getEntity() instanceof ServerPlayer player)
                    for(var winner:state.getWinners())if(winner.getEntity() instanceof IndependentTrainerMob npc){
                        getData(player).ledger().record(id,npc.getTrainerId(),false,isLeague(npc.getTrainerId()));npc.reply(player,"on_battle_won");
                    }
            }
        }catch(Exception error){ZianRCT.LOGGER.error("Battle completion failed for {}",id,error);}
        finally{pendingCleanup.add(id);}

    }
    public BattleMemory getBattleMemory(net.minecraft.server.level.ServerLevel level,String trainer){return new BattleMemory();}
    public final class BattleMemory {public int getDefeatByCount(String trainer,Player player){return getData(player).ledger().wins(trainer);}}
}
