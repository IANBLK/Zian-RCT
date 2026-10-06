package com.ianblk.zianrct.rct;
import com.ianblk.zianrct.reward.*;
import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Server configuration snapshots consumed by the generated RCT pack; never touches player progress. */
public final class RctTrainerOptions {
    private static volatile Set<String> repeat = Set.of();
    private static volatile Map<String,String> formats = Map.of();
    private static Path path;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private RctTrainerOptions() {}
    public static void boot(Path configDir) throws IOException {
        path=configDir.resolve("zianrct-trainer-options.json");
        Map<String,String> loaded=new LinkedHashMap<>();
        if(Files.exists(path)){
            if(Files.size(path)>1048576)throw new IOException("Opciones de entrenador demasiado grandes");
            try(var reader=Files.newBufferedReader(path)){
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                if(!json.keySet().equals(Set.of("schemaVersion","formats")) || json.get("schemaVersion").getAsBigDecimal().intValueExact()!=1)
                    throw new IllegalArgumentException("Opciones inválidas");
                for(var entry:json.getAsJsonObject("formats").entrySet()){
                    validate(entry.getKey(),entry.getValue().getAsString());loaded.put(entry.getKey(),entry.getValue().getAsString());
                }
                if(loaded.size()>256)throw new IllegalArgumentException("Demasiados formatos personalizados");
            }catch(RuntimeException error){throw new IOException("Opciones inválidas; archivo conservado",error);}
        }
        formats=Map.copyOf(loaded);
        rewardConfig(TrainerRewardConfig.open(configDir.resolve("zianrct-rewards.json")).definitions());
    }
    public static void rewardConfig(Map<String,RewardDefinition> definitions){
        Set<String> ids=new HashSet<>();
        definitions.forEach((id,d)->{if(d.mode()==RewardDefinition.Mode.REPEAT)ids.add(id);});
        repeat=Set.copyOf(ids);
    }
    public static Set<String> repeat(){return repeat;}
    public static Map<String,String> formats(){return formats;}
    public static void clear(){repeat=Set.of();formats=Map.of();path=null;}
    private static void validate(String trainer,String format){
        if(trainer==null || trainer.length()>128 || !trainer.matches("[a-z0-9_.-]+")
                || !Set.of("GEN_9_SINGLES","GEN_9_DOUBLES").contains(format))throw new IllegalArgumentException("Formato inválido");
    }
    public static void format(String trainer,String format) throws IOException {
        validate(trainer,format);
        if(path==null)throw new IOException("Opciones no inicializadas");
        Map<String,String> candidate=new LinkedHashMap<>(formats);candidate.put(trainer,format);
        if(candidate.size()>256)throw new IOException("Máximo 256 formatos personalizados");
        var json=new JsonObject();json.addProperty("schemaVersion",1);json.add("formats",GSON.toJsonTree(candidate));
        Path temp=Files.createTempFile(path.getParent(),"zianrct-options-",".tmp");
        try{
            Files.writeString(temp,GSON.toJson(json));
            try(var channel=java.nio.channels.FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}
            Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            formats=Map.copyOf(candidate);
        }finally{Files.deleteIfExists(temp);}
    }
}
