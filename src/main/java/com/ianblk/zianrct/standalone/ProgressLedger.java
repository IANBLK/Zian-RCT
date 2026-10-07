package com.ianblk.zianrct.standalone;

import com.google.gson.*;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Per-player durable progress. A successful atomic write precedes publication of any victory. */
public final class ProgressLedger {
    private final Path path;
    private Set<String> defeated;
    private boolean leagueActive;
    private Map<String,Integer> losses;
    private Map<String,Integer> wins=Map.of();
    private Set<UUID> battles;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private ProgressLedger(Path path,Set<String> defeated,Map<String,Integer> losses,Set<UUID> battles,boolean leagueActive){
        this.path=path;this.defeated=Set.copyOf(defeated);this.losses=Map.copyOf(losses);this.battles=Set.copyOf(battles);this.leagueActive=leagueActive;
    }
    public static ProgressLedger open(Path path,Set<String> imported) throws IOException {
        return open(path,imported,!imported.isEmpty());
    }
    public static ProgressLedger open(Path path,Set<String> imported,boolean active) throws IOException {
        if(!Files.exists(path)){
            var ledger=new ProgressLedger(path,imported,Map.of(),Set.of(),active);
            ledger.write(ledger.defeated,ledger.losses,ledger.battles,active,ledger.wins);return ledger;
        }
        if(Files.size(path)>8*1024*1024)throw new IOException("Registro de progreso demasiado grande");
        try(var reader=Files.newBufferedReader(path)){
            var root=JsonParser.parseReader(reader).getAsJsonObject();
            if(root.get("schemaVersion").getAsInt()!=1)throw new IOException("Versión de progreso desconocida");
            Set<String> defeats=new LinkedHashSet<>();Map<String,Integer> losses=new LinkedHashMap<>();Set<UUID> battles=new LinkedHashSet<>();
            root.getAsJsonArray("defeated").forEach(e->defeats.add(e.getAsString()));
            root.getAsJsonObject("losses").entrySet().forEach(e->{int count=e.getValue().getAsBigDecimal().intValueExact();if(count<0)throw new IllegalArgumentException();losses.put(e.getKey(),count);});
            root.getAsJsonArray("battles").forEach(e->battles.add(UUID.fromString(e.getAsString())));
            var ledger=new ProgressLedger(path,defeats,losses,battles,root.has("leagueActive") && root.get("leagueActive").getAsBoolean());
            if(root.has("wins")){
                Map<String,Integer> wins=new LinkedHashMap<>();
                root.getAsJsonObject("wins").entrySet().forEach(e->{int count=e.getValue().getAsBigDecimal().intValueExact();if(count<0)throw new IllegalArgumentException();wins.put(e.getKey(),count);});
                ledger.wins=Map.copyOf(wins);
            }
            return ledger;
        }catch(RuntimeException error){throw new IOException("Progreso ilegible; archivo conservado",error);}
    }
    public boolean leagueActive(){return leagueActive;}
    public Set<String> defeated(){return defeated;}
    public int wins(String trainer){return wins.getOrDefault(trainer,0);}
    public int losses(String trainer){return losses.getOrDefault(trainer,0);}
    public boolean record(UUID battle,String trainer,boolean victory) throws IOException {
        return record(battle,trainer,victory,false);
    }
    public boolean record(UUID battle,String trainer,boolean victory,boolean league) throws IOException {
        Objects.requireNonNull(battle);Objects.requireNonNull(trainer);
        if(battles.contains(battle))return false;
        Set<String> next=new LinkedHashSet<>(defeated);Map<String,Integer> nextLosses=new LinkedHashMap<>(losses);Set<UUID> seen=new LinkedHashSet<>(battles);
        seen.add(battle);var nextWins=new LinkedHashMap<>(wins);
        if(victory){next.add(trainer);nextWins.merge(trainer,1,Math::addExact);}else nextLosses.merge(trainer,1,Math::addExact);
        write(next,nextLosses,seen,leagueActive||league,nextWins);wins=Map.copyOf(nextWins);leagueActive|=league;defeated=Set.copyOf(next);losses=Map.copyOf(nextLosses);battles=Set.copyOf(seen);return true;
    }
    public void replaceChain(Set<String> chain,Set<String> desired) throws IOException {
        var next=new LinkedHashSet<>(defeated);next.removeAll(chain);next.addAll(desired);
        write(next,losses,battles,true,wins);leagueActive=true;defeated=Set.copyOf(next);
    }
    private void write(Set<String> defeats,Map<String,Integer> losses,Set<UUID> battles,boolean active,Map<String,Integer> wins) throws IOException {
        var root=new JsonObject();root.addProperty("schemaVersion",1);root.addProperty("leagueActive",active);root.add("defeated",GSON.toJsonTree(defeats));root.add("losses",GSON.toJsonTree(losses));root.add("wins",GSON.toJsonTree(wins));root.add("battles",GSON.toJsonTree(battles));
        String serialized=GSON.toJson(root);if(serialized.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>8*1024*1024)throw new IOException("Registro de progreso demasiado grande");
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary=Files.createTempFile(path.toAbsolutePath().getParent(),"zian-progress-",".tmp");
        try{
            Files.writeString(temporary,serialized);
            try(var channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
            Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temporary);}
    }
}
