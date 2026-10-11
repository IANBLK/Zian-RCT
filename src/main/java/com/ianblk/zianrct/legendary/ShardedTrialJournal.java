package com.ianblk.zianrct.legendary;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Only online/recent owners retained in RAM; full closed/unconfirmed entitlements remain on disk. */
final class ShardedTrialJournal extends TrialJournal {
  private final Path manifest,directory;
  private final int limit;
  private final LinkedHashMap<UUID,TrialJournal> cache=new LinkedHashMap<>(16,.75f,true);
  ShardedTrialJournal(Path path,JsonObject model)throws IOException {
    super(path);manifest=path.toAbsolutePath().normalize();
    if(!model.keySet().equals(Set.of("schemaVersion","layout","directory","cacheLimit")) || !model.get("layout").getAsString().equals("per-player-v1"))throw new IOException("Invalid trial manifest");
    String name=model.get("directory").getAsString(),prefix=path.getFileName()+".players-";
    if(!name.startsWith(prefix) || !UUID.fromString(name.substring(prefix.length())).toString().equals(name.substring(prefix.length())))throw new IOException("Invalid trial directory");
    directory=manifest.getParent().resolve(name);limit=model.get("cacheLimit").getAsBigDecimal().intValueExact();
    if(limit<1 || limit>64 || !Files.isDirectory(directory) || Files.isSymbolicLink(directory))throw new IOException("Invalid trial cache/directory");
  }
  private TrialJournal owner(UUID player){
    if(Files.exists(manifest.resolveSibling(manifest.getFileName()+".maintenance")))throw new IllegalStateException("Trial maintenance in progress");
    var old=cache.get(player);if(old!=null)return old;
    try {
      Path file=directory.resolve(player+".json");
      if(Files.isSymbolicLink(file))throw new IOException("Trial symlink refused");
      if(Files.exists(file))try(var reader=Files.newBufferedReader(file)){
        if(Files.size(file)>16777216 || JsonParser.parseReader(reader).getAsJsonObject().get("schemaVersion").getAsInt()!=1)throw new IOException("Invalid owner trial schema");
      }
      var loaded=TrialJournal.open(file);
      if(loaded.all().stream().anyMatch(e->!player.equals(e.player())))throw new IOException("Trial belongs to another player");
      cache.put(player,loaded);if(cache.size()>limit)cache.remove(cache.keySet().iterator().next());return loaded;
    }catch(IOException error){throw new UncheckedIOException(error);}
  }
  @Override public synchronized Entry get(UUID player,String trainer){return owner(player).get(player,trainer);}
  @Override public synchronized List<Entry> forPlayer(UUID player){return owner(player).forPlayer(player);}
  @Override public synchronized List<Entry> all(){
    var result=new ArrayList<Entry>();
    try(var files=Files.list(directory)){
      for(var file:files.filter(p->p.getFileName().toString().endsWith(".json")).toList()){
        String name=file.getFileName().toString();UUID player=UUID.fromString(name.substring(0,name.length()-5));
        result.addAll(owner(player).forPlayer(player));
      }
    }catch(IOException error){throw new UncheckedIOException(error);}
    return List.copyOf(result);
  }
  @Override public synchronized boolean unlock(UUID player,String trainer,String boss,String species,int level,int shinyDenominator,UUID battle)throws IOException{return owner(player).unlock(player,trainer,boss,species,level,shinyDenominator,battle);}
  @Override public synchronized Entry victory(UUID player,String trainer,UUID battle,Random random)throws IOException{return owner(player).victory(player,trainer,battle,random);}
  @Override public synchronized void phase(UUID player,String trainer,Phase next)throws IOException{owner(player).phase(player,trainer,next);}
}
