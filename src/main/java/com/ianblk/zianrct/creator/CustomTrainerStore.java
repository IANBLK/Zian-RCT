package com.ianblk.zianrct.creator;
import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public final class CustomTrainerStore {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile Map<String,CustomTrainer> definitions=Map.of();
    private static Path path;
    private CustomTrainerStore() {}
    public static void boot(Path configDir) throws IOException {
        Path candidatePath=configDir.resolve("zianrct-custom-trainers.json");
        Map<String,CustomTrainer> parsed=new LinkedHashMap<>();
        if(Files.exists(candidatePath)){
            if(Files.size(candidatePath)>1048576)throw new IOException("Archivo de entrenadores demasiado grande.");
            try(var reader=Files.newBufferedReader(candidatePath)){
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                if(!json.keySet().equals(Set.of("schemaVersion","trainers")) || json.get("schemaVersion").getAsBigDecimal().intValueExact()!=1)
                    throw new IllegalArgumentException("Esquema inválido");
                for(var entry:json.getAsJsonArray("trainers")){
                    CustomTrainer trainer=GSON.fromJson(entry,CustomTrainer.class);
                    if(parsed.putIfAbsent(trainer.id(),trainer)!=null)throw new IllegalArgumentException("ID duplicado");
                }
                if(parsed.size()>64)throw new IllegalArgumentException("Máximo 64 definiciones propias.");
            }catch(RuntimeException e){throw new IOException("Definiciones inválidas; archivo conservado",e);}
        }
        path=candidatePath;definitions=Map.copyOf(parsed);
    }
    public static void clear(){definitions=Map.of();path=null;}
    public static CustomTrainer get(String id){return definitions.get(id);}
    public static Map<String,CustomTrainer> all(){return definitions;}
    public static void save(CustomTrainer trainer) throws IOException {
        if(path==null)throw new IOException("Creador no disponible; revisa el log.");
        Map<String,CustomTrainer> next=new LinkedHashMap<>(definitions);next.put(trainer.id(),trainer);
        if(next.size()>64)throw new IOException("Máximo 64 entrenadores propios.");
        var json=new JsonObject();json.addProperty("schemaVersion",1);json.add("trainers",GSON.toJsonTree(next.values()));
        String content=GSON.toJson(json);
        if(content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1048576)throw new IOException("Archivo demasiado grande.");
        Path temp=Files.createTempFile(path.getParent(),"zianrct-creator-",".tmp");
        try{
            Files.writeString(temp,content);
            try(var channel=java.nio.channels.FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}
            Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            definitions=Map.copyOf(next);
        }finally{Files.deleteIfExists(temp);}
    }
    public static CustomTrainer decode(String json){
        if(json==null || json.length()>16384)throw new IllegalArgumentException("Definición demasiado grande");
        return GSON.fromJson(json,CustomTrainer.class);
    }
    public static Map<String,String> skins(){
        Map<String,String> result=new HashMap<>();definitions.forEach((id,d)->result.put(id,CustomTrainer.SKINS.get(d.skin())));return result;
    }
}
