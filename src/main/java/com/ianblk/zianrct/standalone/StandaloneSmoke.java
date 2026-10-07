package com.ianblk.zianrct.standalone;

import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.creator.*;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import java.util.*;

/** Opt-in isolated-world native integration checks. Inactive in normal installations. */
public final class StandaloneSmoke {
    private int phase,ticks;
    private boolean failed;
    private IndependentTrainerMob doubles,singles;
    private ServerPlayer player;
    private UUID battle;
    private boolean previousBoss,previousGuardian;
    private static final UUID PLAYER=UUID.fromString("00000000-0000-0000-0000-000000000105");
    public StandaloneSmoke(){
        if(!"true".equals(System.getenv("ZIANRCT_STANDALONE_SMOKE")))return;
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e)->{phase=0;ticks=0;failed=false;player=null;doubles=null;singles=null;});
    }
    private void tick(ServerTickEvent.Post event){
        if(failed || phase==10)return;
        var runtime=StandaloneRuntime.getInstance();if(!runtime.isValidId("zian_custom_ci_legendary"))return;
        try{
            var server=event.getServer();
            if(phase==0){
                try{Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");throw new IllegalStateException("RCTMod must be absent");}catch(ClassNotFoundException expected){}
                if(runtime.retainedNpcTeams()!=0)throw new IllegalStateException("Trainer templates retained Pokemon teams");
                Path marker=server.getWorldPath(LevelResource.ROOT).resolve("data/zianrct-standalone-smoke.marker");
                boolean restart=Files.exists(marker);
                doubles=npc(server,"00000000-0000-0000-0000-000000000101","zian_custom_ci",false,restart,0);
                singles=npc(server,"00000000-0000-0000-0000-000000000102","zian_custom_ci_legendary",false,restart,3);
                npc(server,"00000000-0000-0000-0000-000000000103","rassvet_leader_novato",true,restart,6);
                if(!"127.0.0.1".equals(server.getLocalIp()))throw new IllegalStateException("Smoke fixtures require an isolated loopback server");
                player=FakePlayerFactory.get(server.overworld(),new GameProfile(PLAYER,"ZianApiSmoke"));
                player.moveTo(doubles.getX()+2,doubles.getY(),doubles.getZ(),0,0);
                // PlayerBattleActor resolves participants by UUID through PlayerList. Test-only registration.
                var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
                @SuppressWarnings("unchecked") var players=(Map<UUID,ServerPlayer>)field.get(server.getPlayerList());
                players.put(PLAYER,player);
                Path old=server.getWorldPath(LevelResource.ROOT).resolve("data/rctmod.player."+PLAYER+".stat.dat");
                if(!Files.exists(old)){
                    var root=new CompoundTag();var data=new CompoundTag();var defeats=new ListTag();
                    defeats.add(StringTag.valueOf("rassvet_leader_novato"));defeats.add(StringTag.valueOf("rassvet_leader_ferrum"));data.put("progressDefeats",defeats);data.putString("currentSeries","rassvet");root.put("data",data);NbtIo.writeCompressed(root,old);
                }
                byte[] source=Files.readAllBytes(old);var progress=runtime.getData(player);
                previousBoss=progress.getDefeatedTrainerIds().contains("zian_custom_ci");previousGuardian=progress.getDefeatedTrainerIds().contains("zian_custom_ci_legendary");
                if(progress.getLevelCap()!=30 || !Arrays.equals(source,Files.readAllBytes(old)))throw new IllegalStateException("Legacy progress migration failed");
                var party=Cobblemon.INSTANCE.getStorage().getParty(player);party.clearParty();
                for(int i=0;i<3;i++)party.add(PokemonProperties.Companion.parse("mewtwo level=10 moves=psychic,shadowball,swift,recover").create(player));
                if(!runtime.start(doubles,player))throw new IllegalStateException("Native doubles battle did not start");
                battle=runtime.api().getBattleManager().getStates().iterator().next().getBattle().getBattleId();
                if(!doubles.isInBattle())throw new IllegalStateException("NPC battle association missing");
                ExperienceCapsSmoke.verify(server,runtime.api().getBattleManager().getState(battle).getBattle());
                Files.writeString(marker,"Own NPC persistence and legacy migration fixture\n");
                ZianRCT.LOGGER.info("Zian RCT standalone smoke: native doubles started; NPC {}",restart?"restart persistence verified":"NBT fixture created");
                phase=1;ticks=0;
            }else if(phase==1 && ++ticks>40){runtime.api().getBattleManager().end(battle,true);phase=2;ticks=0;}
            else if(phase==2 && ++ticks>20){
                if(doubles.isInBattle() || runtime.retainedNpcTeams()!=0)throw new IllegalStateException("Doubles battle teams were not released");
                if(!runtime.start(singles,player))throw new IllegalStateException("Native singles battle did not start");
                battle=runtime.api().getBattleManager().getStates().iterator().next().getBattle().getBattleId();phase=3;ticks=0;
            }else if(phase==3 && ++ticks>40){runtime.api().getBattleManager().end(battle,true);phase=4;ticks=0;}
            else if(phase==4 && ++ticks>20){
                if(singles.isInBattle() || runtime.retainedNpcTeams()!=0)throw new IllegalStateException("Singles battle teams were not released");
                if(runtime.getData(player).getDefeatedTrainerIds().contains("zian_custom_ci")!=previousBoss || runtime.getData(player).getDefeatedTrainerIds().contains("zian_custom_ci_legendary")!=previousGuardian)throw new IllegalStateException("Forced battle termination granted progression");
                if(!runtime.start(doubles,player))throw new IllegalStateException("Could not start winner-event test");
                battle=runtime.api().getBattleManager().getStates().iterator().next().getBattle().getBattleId();phase=5;ticks=0;
            }else if(phase==5 && ++ticks>40){
                runtime.api().getBattleManager().getState(battle).getBattle().writeShowdownAction(">forcewin p1");phase=6;ticks=0;
            }else if(phase==6){
                if(++ticks>400)throw new IllegalStateException("Native winner event did not finish");
                if(!doubles.isInBattle()){
                    if(!runtime.getData(player).getDefeatedTrainerIds().contains("zian_custom_ci"))throw new IllegalStateException("Native winner event did not save progression");
                    if(!previousGuardian && !com.ianblk.zianrct.legendary.LegendaryTrials.canStart(PLAYER,"zian_custom_ci_legendary"))throw new IllegalStateException("Boss win did not unlock trial");
                    if(previousGuardian){if(!com.ianblk.zianrct.legendary.LegendaryTrials.delivered(PLAYER,"zian_custom_ci_legendary"))throw new IllegalStateException("Previous legendary delivery was not durably closed");ZianRCT.LOGGER.info("Zian RCT standalone smoke passed: restart NPCs, closed legendary trial, native winner event");phase=10;}
                    else{if(!runtime.start(singles,player))throw new IllegalStateException("Could not start legendary event test");
                        battle=runtime.api().getBattleManager().getStates().iterator().next().getBattle().getBattleId();phase=7;ticks=0;}
                }
            }else if(phase==7 && ++ticks>40){
                runtime.api().getBattleManager().getState(battle).getBattle().writeShowdownAction(">forcewin p1");phase=8;ticks=0;
            }else if(phase==8){
                if(++ticks>600)throw new IllegalStateException("Legendary event delivery did not finish");
                if(!singles.isInBattle() && runtime.getData(player).getDefeatedTrainerIds().contains("zian_custom_ci_legendary")){
                    var pc=Cobblemon.INSTANCE.getStorage().getPC(player);
                    boolean received=false;for(var p:pc)if(p.getSpecies().getName().equalsIgnoreCase("mewtwo"))received=true;
                    if(received && com.ianblk.zianrct.legendary.LegendaryTrials.delivered(PLAYER,"zian_custom_ci_legendary")){
                        ZianRCT.LOGGER.info("Zian RCT standalone smoke passed: singles, doubles, released teams, forced-end protection, legacy progress, NPC NBT, native winner events, legendary award");phase=10;
                    }
                }
            }
        }catch(Exception error){failed=true;ZianRCT.LOGGER.error("Zian RCT standalone smoke FAILED",error);}
    }
    private IndependentTrainerMob npc(net.minecraft.server.MinecraftServer server,String id,String trainer,boolean legacy,boolean restart,int offset){
        UUID uuid=UUID.fromString(id);var level=server.overworld();var entity=level.getEntity(uuid);
        if(restart && !(entity instanceof IndependentTrainerMob))throw new IllegalStateException("Saved NPC missing after restart: "+uuid);
        if(entity instanceof IndependentTrainerMob existing){
            if(!trainer.equals(existing.getTrainerId()) || !existing.isPersistenceRequired() || !existing.isNoAi())throw new IllegalStateException("Saved NPC properties changed");return existing;
        }
        var npc=(legacy?CustomTrainerEntities.LEGACY_TYPE:CustomTrainerEntities.TYPE).get().create(level);
        if(npc==null)throw new IllegalStateException("NPC creation failed");
        var spawn=level.getSharedSpawnPos();npc.setUUID(uuid);npc.setTrainerId(trainer);npc.moveTo(spawn.getX()+offset,spawn.getY()+3,spawn.getZ(),75,0);
        npc.setPersistent(true);npc.setNoAi(true);npc.setNoGravity(true);npc.setHomePos(BlockPos.containing(npc.position()));
        CompoundTag tag=npc.saveWithoutId(new CompoundTag());var restored=(legacy?CustomTrainerEntities.LEGACY_TYPE:CustomTrainerEntities.TYPE).get().create(level);restored.load(tag);
        if(!restored.getTrainerId().equals(trainer) || !restored.getHomePos().equals(npc.getHomePos()) || !restored.getUUID().equals(uuid))throw new IllegalStateException("NPC NBT round trip failed");
        if(!level.addFreshEntity(npc))throw new IllegalStateException("Could not add smoke NPC");return npc;
    }
}
