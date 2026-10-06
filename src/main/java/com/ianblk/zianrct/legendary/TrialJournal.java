package com.ianblk.zianrct.legendary;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.io.IOException;
import java.util.*;

/** Write-ahead entitlement ledger. Uncertain deliveries never reopen automatically. */
public final class TrialJournal {
    public enum Phase { UNLOCKED, READY, APPLYING, DELIVERED, REVIEW }
    public record Entry(UUID player,String trainer,String boss,String species,int level,int shinyDenominator,
                        UUID bossBattle,UUID victoryBattle,UUID pokemon,List<Integer> ivs,boolean shiny,Phase phase){
        public Entry {
            if(player==null || bossBattle==null || phase==null || trainer==null || !trainer.matches("zian_custom_[a-z0-9_]{1,48}")
                || boss==null || !boss.matches("zian_custom_[a-z0-9_]{1,48}") || species==null || !species.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_]+")
                || level<1 || level>100 || shinyDenominator<1 || shinyDenominator>1000000)throw new IllegalArgumentException("Prueba inválida");
            ivs=List.copyOf(ivs);
            if(phase!=Phase.UNLOCKED && (victoryBattle==null || pokemon==null || ivs.size()!=6 || ivs.stream().anyMatch(i->i<25 || i>30)))throw new IllegalArgumentException("Premio no congelado");
            if(phase==Phase.UNLOCKED && (victoryBattle!=null || pokemon!=null || !ivs.isEmpty()))throw new IllegalArgumentException("Desbloqueo inválido");
        }
        public Entry phase(Phase next){return new Entry(player,trainer,boss,species,level,shinyDenominator,bossBattle,victoryBattle,pokemon,ivs,shiny,next);}
    }
    private final Path path;
    private final Map<String,Entry> entries=new LinkedHashMap<>();
    private boolean healthy=true;
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private TrialJournal(Path path){this.path=path;}
    private static String key(UUID player,String trainer){return player+":"+trainer;}
    public static TrialJournal open(Path path) throws IOException {
        var result=new TrialJournal(path);
        if(Files.exists(path)){
            if(Files.size(path)>16777216)throw new IOException("Registro legendario demasiado grande");
            try(var reader=Files.newBufferedReader(path)){
                var root=JsonParser.parseReader(reader).getAsJsonObject();
                if(!root.keySet().equals(Set.of("schemaVersion","entries")) || root.get("schemaVersion").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Esquema inválido");
                for(var element:root.getAsJsonArray("entries")){var entry=GSON.fromJson(element,Entry.class);if(result.entries.putIfAbsent(key(entry.player(),entry.trainer()),entry)!=null)throw new IllegalArgumentException("Duplicado");}
            }catch(RuntimeException e){throw new IOException("Registro legendario inválido; conservado",e);}
        }
        return result;
    }
    public synchronized Entry get(UUID player,String trainer){requireHealthy();return entries.get(key(player,trainer));}
    public synchronized List<Entry> all(){requireHealthy();return List.copyOf(entries.values());}
    public synchronized boolean unlock(UUID player,String trainer,String boss,String species,int level,int shinyDenominator,UUID battle) throws IOException {
        requireHealthy();if(get(player,trainer)!=null)return false;
        put(new Entry(player,trainer,boss,species,level,shinyDenominator,battle,null,null,List.of(),false,Phase.UNLOCKED));return true;
    }
    public synchronized Entry victory(UUID player,String trainer,UUID battle,Random random) throws IOException {
        var old=get(player,trainer);if(old==null || old.phase()!=Phase.UNLOCKED)return old;
        if(battle==null || battle.equals(old.bossBattle()))throw new IllegalArgumentException("Victoria sin evidencia independiente");
        var ivs=new ArrayList<Integer>();for(int i=0;i<6;i++)ivs.add(25+random.nextInt(6));
        var next=new Entry(player,trainer,old.boss(),old.species(),old.level(),old.shinyDenominator(),old.bossBattle(),battle,UUID.randomUUID(),ivs,random.nextInt(old.shinyDenominator())==0,Phase.READY);
        put(next);return next;
    }
    public synchronized void phase(UUID player,String trainer,Phase next) throws IOException {
        var old=get(player,trainer);if(old==null)throw new IllegalArgumentException("Prueba inexistente");
        boolean allowed=(old.phase()==Phase.READY && next==Phase.APPLYING)
            || (old.phase()==Phase.APPLYING && (next==Phase.DELIVERED || next==Phase.REVIEW))
            || (old.phase()==Phase.REVIEW && next==Phase.DELIVERED);
        if(!allowed)throw new IllegalArgumentException("Transición legendaria prohibida");put(old.phase(next));
    }
    private void requireHealthy(){if(!healthy)throw new IllegalStateException("Registro bloqueado por un error de guardado");}
    private void put(Entry entry) throws IOException {
        requireHealthy();var next=new LinkedHashMap<>(entries);next.put(key(entry.player(),entry.trainer()),entry);
        var root=new JsonObject();root.addProperty("schemaVersion",1);root.add("entries",GSON.toJsonTree(next.values()));
        byte[] bytes=GSON.toJson(root).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if(bytes.length>16777216)throw new IOException("Registro lleno");
        Files.createDirectories(path.getParent());Path temporary=Files.createTempFile(path.getParent(),"zianrct-trial-",".tmp");
        try{Files.write(temporary,bytes);try(var channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
            Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);entries.clear();entries.putAll(next);
        }catch(IOException error){healthy=false;throw error;}finally{Files.deleteIfExists(temporary);}
    }
}
