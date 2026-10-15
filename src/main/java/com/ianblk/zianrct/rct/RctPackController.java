package com.ianblk.zianrct.rct;

import com.google.gson.*;
import com.gitlab.srcmc.rctapi.api.models.TrainerModel;
import com.gitlab.srcmc.rctapi.api.battle.*;
import com.ianblk.zianrct.standalone.*;
import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.config.*;
import com.ianblk.zianrct.rct.pack.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Direct, transactional loading into our API instance; never reloads the Minecraft resource registry. */
public final class RctPackController {
    private MinecraftServer activeServer;
    private boolean busy;
    private boolean startupPending;
    private boolean reloadPending;
    public RctPackController(IEventBus ignored){
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event)->{
            activeServer=event.getServer();StandaloneRuntime.getInstance().start(activeServer);
            startupPending=true;
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)->{
            if(event.getServer()!=activeServer)return;
            if(startupPending){startupPending=false;regenerate(activeServer,"server start");}
            else if(reloadPending){reloadPending=false;regenerate(activeServer,"server data reload");}
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event)->{
            StandaloneRuntime.getInstance().stop();activeServer=null;busy=false;startupPending=false;reloadPending=false;
            com.ianblk.zianrct.creator.CustomTrainerStore.clear();RctTrainerOptions.clear();
        });
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event)->{
            if(event.getUpdateCause()==TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD && activeServer!=null)
                reloadPending=true;
        });
    }
    public CompletableFuture<ConfigReloadResult> reloadConfig(MinecraftServer server,ZianRctConfig next,String reason){
        if(server!=activeServer || busy)return CompletableFuture.completedFuture(new ConfigReloadResult(false,"Servidor no disponible o recarga en curso."));
        busy=true;
        try{next.validateOrThrow();install(server,next,reason);ConfigState.replace(next);
            return CompletableFuture.completedFuture(new ConfigReloadResult(true,next.messages().reloadSuccess()));
        }catch(Exception error){ZianRCT.LOGGER.error("Trainer reload rejected; previous catalog retained",error);
            return CompletableFuture.completedFuture(new ConfigReloadResult(false,"No se recargó Zian RCT: "+error.getMessage()));
        }finally{busy=false;}
    }
    public void regenerate(MinecraftServer server,String reason){
        if(server==null || server!=activeServer || busy)return;
        busy=true;
        try{install(server,ConfigState.current(),reason);}
        catch(Exception error){ZianRCT.LOGGER.error("Standalone trainer loading failed; previous catalog retained",error);}
        finally{busy=false;}
    }
    private void install(MinecraftServer server,ZianRctConfig config,String reason){
        var runtime=StandaloneRuntime.getInstance();
        if(!runtime.api().getBattleManager().getStates().isEmpty())throw new IllegalStateException("Espera a que terminen los combates");
        GeneratedCandidate candidate=buildCandidate(server,config);
        Set<String> ids=new LinkedHashSet<>();candidate.profile().chain().forEach(e->ids.add(e.trainer()));
        ids.addAll(com.ianblk.zianrct.creator.CustomTrainerStore.all().keySet());
        Map<String,TrainerDefinition> definitions=new LinkedHashMap<>();
        var gson=runtime.api().gsonBuilder().create();
        for(String id:ids){
            JsonObject team=json(server,candidate,"trainers/"+id+".json",false);
            JsonObject mob=json(server,candidate,"mobs/trainers/single/"+id+".json",false);
            JsonObject dialogs=json(server,candidate,"dialogs/trainers/single/"+id+".json",true);
            TrainerModel model=gson.fromJson(team,TrainerModel.class);
            BattleFormat format=BattleFormat.valueOf(team.has("battleFormat")?team.get("battleFormat").getAsString():"GEN_9_SINGLES");
            if(model==null || model.getTeam()==null || model.getTeam().isEmpty() || model.getTeam().size()>6)
                throw new IllegalArgumentException("Equipo inválido: "+id);
            if(format==BattleFormat.GEN_9_DOUBLES && model.getTeam().size()<2)throw new IllegalArgumentException("Dobles requiere dos Pokémon: "+id);
            BattleRules rules=team.has("battleRules")?gson.fromJson(team.get("battleRules"),BattleRules.class):new BattleRules.Builder().withMaxItemUses(3).build();
            // Public API validation; temporary Pokemon objects are released after each team.
            String validationKey="validate:"+id;
            try{runtime.api().getTrainerRegistry().registerNPC(validationKey,model);}
            finally{runtime.api().getTrainerRegistry().unregisterById(validationKey);}
            definitions.put(id,new TrainerDefinition(model,format,rules,mob,dialogs));
        }
        for(var d:com.ianblk.zianrct.creator.CustomTrainerStore.all().values()){
            if(d.trial()!=null)com.ianblk.zianrct.legendary.LegendaryTrials.validate(d);
            if(d.autoMoves())ZianRCT.LOGGER.info("Zian RCT automatic trainer moves verified: {}",d.id());
            if(d.trial()!=null)ZianRCT.LOGGER.info("Zian RCT legendary trial definition verified: {}",d.id());
        }
        runtime.replace(definitions);
        Map<String,Integer> levels=new LinkedHashMap<>();definitions.forEach((id,d)->levels.put(id,d.level()));
        ZianRCT.LOGGER.info("Zian RCT standalone team levels: {}",levels);
        ZianRCT.LOGGER.info("Zian RCT custom trainer definitions verified: {}",com.ianblk.zianrct.creator.CustomTrainerStore.all().keySet());
        ZianRCT.LOGGER.info("Zian RCT generated progression verified for {} trainers; standalone RCTAPI catalog ready after {}",candidate.profile().chain().size(),reason);
        ZianRCT.LOGGER.info("Zian RCT standalone catalog: {} trainer models; {} retained NPC teams",definitions.size(),runtime.retainedNpcTeams());
    }
    private static JsonObject json(MinecraftServer server,GeneratedCandidate candidate,String key,boolean optional){
        byte[] generated=candidate.generated().get(key);
        String source=generated==null?readOriginalJsonIfPresent(server,ResourceLocation.fromNamespaceAndPath("rctmod",key)).orElse(optional?"{}":null):new String(generated,java.nio.charset.StandardCharsets.UTF_8);
        if(source==null)throw new IllegalArgumentException("Falta la definición: "+key);
        return JsonParser.parseString(source).getAsJsonObject();
    }
    public boolean isBusy(){return busy;}
    public void requireTrainerSource(MinecraftServer server,String trainer,boolean mob){
        if(com.ianblk.zianrct.creator.CustomTrainerStore.get(trainer)!=null)return;
        String key=(mob?"mobs/trainers/single/":"trainers/")+trainer+".json";
        if(readOriginalJsonIfPresent(server,ResourceLocation.fromNamespaceAndPath("rctmod",key)).isEmpty())
            throw new IllegalArgumentException("Definición de entrenador no encontrada: "+key);
    }
    private GeneratedCandidate buildCandidate(MinecraftServer server, ZianRctConfig config) {
        ZianRctConfig.Profile profile = config.activeProfileConfig();
        if (profile == null) {
            throw new IllegalStateException("El perfil activo no existe: " + config.activeProfile());
        }

        ResourceLocation seriesLocation = ResourceLocation.fromNamespaceAndPath(
                ZianRctVirtualPack.RCT_NAMESPACE,
                "series/" + profile.series() + ".json"
        );
        Optional<String> seriesJson = readOriginalJsonIfPresent(server, seriesLocation);
        if (seriesJson.isEmpty()) {
            throw new IllegalStateException("configured RCT series resource is not loaded: " + seriesLocation);
        }

        SeriesPackSnapshot series = new SeriesPackSnapshot(seriesJson.get());
        LinkedHashMap<String, TrainerPackSnapshot> snapshots = new LinkedHashMap<>();

        for (ZianRctConfig.ChainEntry entry : profile.chain()) {
            String id = entry.trainer();
            ResourceLocation mobLocation = ResourceLocation.fromNamespaceAndPath(
                    ZianRctVirtualPack.RCT_NAMESPACE,
                    "mobs/trainers/single/" + id + ".json"
            );
            ResourceLocation teamLocation = ResourceLocation.fromNamespaceAndPath(
                    ZianRctVirtualPack.RCT_NAMESPACE,
                    "trainers/" + id + ".json"
            );

            Optional<String> mobJson = readOriginalJsonIfPresent(server, mobLocation);
            Optional<String> teamJson = readOriginalJsonIfPresent(server, teamLocation);
            if (mobJson.isEmpty() || teamJson.isEmpty()) {
                String missing = mobJson.isEmpty() ? mobLocation.toString() : teamLocation.toString();
                throw new IllegalStateException("configured RCT trainer resource is not loaded: " + missing);
            }

            int maxTeamLevel = maxTeamLevel(teamJson.get(), id);
            snapshots.put(id, new TrainerPackSnapshot(id, mobJson.get(), maxTeamLevel));
        }

        Map<String, TrainerPackSnapshot> stableSnapshots =
                Collections.unmodifiableMap(new LinkedHashMap<>(snapshots));
        Map<String, byte[]> generated = new LinkedHashMap<>(RctPackJsonBuilder.build(profile, series, stableSnapshots));
        for(var d:com.ianblk.zianrct.creator.CustomTrainerStore.all().values())com.ianblk.zianrct.creator.CustomTrainerValidation.validate(d);
        generated.putAll(com.ianblk.zianrct.creator.CustomTrainerResources.build(com.ianblk.zianrct.creator.CustomTrainerStore.all()));
        for(String trainer:RctTrainerOptions.repeat()){
            String key="mobs/trainers/single/"+trainer+".json";
            String source=generated.containsKey(key)?new String(generated.get(key),java.nio.charset.StandardCharsets.UTF_8)
                    :readOriginalJsonIfPresent(server,ResourceLocation.fromNamespaceAndPath("rctmod",key)).orElseThrow(()->new IllegalStateException("Entrenador repetible inexistente: "+trainer));
            generated.put(key,RctTrainerOverrides.repeatMob(source).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        for(var format:RctTrainerOptions.formats().entrySet()){
            String key="trainers/"+format.getKey()+".json";
            String source=generated.containsKey(key)?new String(generated.get(key),java.nio.charset.StandardCharsets.UTF_8):readOriginalJsonIfPresent(server,ResourceLocation.fromNamespaceAndPath("rctmod",key))
                    .orElseThrow(()->new IllegalStateException("Equipo inexistente: "+format.getKey()));
            generated.put(key,RctTrainerOverrides.format(source,format.getValue()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return new GeneratedCandidate(profile, generated, stableSnapshots);
    }

    private static Optional<String> readOriginalJsonIfPresent(
            MinecraftServer server,
            ResourceLocation location
    ) {
        List<Resource> stack = server.getResourceManager().getResourceStack(location);
        for (int index = stack.size() - 1; index >= 0; index--) {
            Resource resource = stack.get(index);
            if (ZianRctVirtualPack.PACK_ID.equals(resource.sourcePackId())) {
                continue;
            }
            try (BufferedReader reader = resource.openAsReader()) {
                return Optional.of(reader.lines().collect(java.util.stream.Collectors.joining("\n")));
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read original RCT resource: " + location, exception);
            }
        }
        return Optional.empty();
    }

    private static int maxTeamLevel(String sourceJson, String trainerId) {
        var parsed = JsonParser.parseString(sourceJson);
        if (!parsed.isJsonObject()) {
            throw new IllegalStateException("RCT trainer team JSON is not an object: " + trainerId);
        }
        JsonArray team = parsed.getAsJsonObject().getAsJsonArray("team");
        if (team == null || team.isEmpty()) {
            throw new IllegalStateException("RCT trainer has no usable team: " + trainerId);
        }
        int max = 0;
        int index = 0;
        for (var element : team) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("RCT trainer team entry " + index + " is not an object: " + trainerId);
            }
            JsonObject pokemon = element.getAsJsonObject();
            if (!pokemon.has("level") || !pokemon.get("level").isJsonPrimitive()
                    || !pokemon.getAsJsonPrimitive("level").isNumber()) {
                throw new IllegalStateException(
                        "RCT trainer team entry " + index + " has no numeric level: " + trainerId
                );
            }
            int level = pokemon.get("level").getAsInt();
            if (level <= 0) {
                throw new IllegalStateException(
                        "RCT trainer team entry " + index + " has invalid level " + level + ": " + trainerId
                );
            }
            max = Math.max(max, level);
            index++;
        }
        return max;
    }

    public record ConfigReloadResult(boolean success,String message){}
    private record GeneratedCandidate(ZianRctConfig.Profile profile,Map<String,byte[]> generated,Map<String,TrainerPackSnapshot> snapshots){}
}
