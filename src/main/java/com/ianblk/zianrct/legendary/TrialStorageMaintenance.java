package com.ianblk.zianrct.legendary;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.util.*;

/** Offline migration preserving frozen UUID/IV/shiny results and all trial phases. */
public final class TrialStorageMaintenance {
  public static Path migrateOffline(Path path)throws IOException {
    path=path.toAbsolutePath().normalize();if(Files.isSymbolicLink(path))throw new IOException("Trial symlink refused");
    boolean exists=Files.exists(path);if(exists && Files.size(path)>16777216)throw new IOException("Oversized trials");
    byte[] before=exists?Files.readAllBytes(path):null;
    var old=TrialJournal.open(path);if(old instanceof ShardedTrialJournal)throw new IOException("Trials already sharded");
    var groups=new LinkedHashMap<UUID,List<TrialJournal.Entry>>();
    for(var entry:old.all())groups.computeIfAbsent(entry.player(),p->new ArrayList<>()).add(entry);
    Path parent=path.getParent();Files.createDirectories(parent);
    String token=UUID.randomUUID().toString(),name=path.getFileName()+".players-"+token;
    Path stage=parent.resolve(name+".staging"),directory=parent.resolve(name),marker=path.resolveSibling(path.getFileName()+".maintenance");
    boolean ownsMarker=false;var gson=new GsonBuilder().setPrettyPrinting().create();
    try {
      Files.writeString(marker,"Offline trial migration; directory="+name,StandardOpenOption.CREATE_NEW);ownsMarker=true;forceFile(marker);forceDirectory(parent);Files.createDirectory(stage);
      for(var entry:groups.entrySet()){
        var json=new JsonObject();json.addProperty("schemaVersion",1);json.add("entries",gson.toJsonTree(entry.getValue()));
        Path target=stage.resolve(entry.getKey()+".json");Files.writeString(target,gson.toJson(json),StandardOpenOption.CREATE_NEW);forceFile(target);
        if(!TrialJournal.open(target).all().equals(entry.getValue()))throw new IOException("Trial verification failed");
      }
      forceDirectory(stage);
      if(exists){
        if(!Arrays.equals(before,Files.readAllBytes(path)))throw new IOException("Trials changed; server must be offline");
        Path archive=parent.resolve(path.getFileName()+".archive-"+token);Files.write(archive,before,StandardOpenOption.CREATE_NEW);forceFile(archive);
        if(!Arrays.equals(before,Files.readAllBytes(archive)))throw new IOException("Trial archive verification failed");
      }else if(Files.exists(path))throw new IOException("Trials appeared; server must be offline");
      forceDirectory(parent);Files.move(stage,directory,StandardCopyOption.ATOMIC_MOVE);forceDirectory(parent);
      var manifest=new JsonObject();manifest.addProperty("schemaVersion",2);manifest.addProperty("layout","per-player-v1");manifest.addProperty("directory",name);manifest.addProperty("cacheLimit",32);
      Path temp=Files.createTempFile(parent,"trials-manifest-",".tmp");
      try{Files.writeString(temp,gson.toJson(manifest));forceFile(temp);Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);forceDirectory(parent);}finally{Files.deleteIfExists(temp);}
    }finally{if(ownsMarker){Files.deleteIfExists(marker);forceDirectory(parent);}}
    return directory;
  }
  private static void forceFile(Path path)throws IOException{try(var c=FileChannel.open(path,StandardOpenOption.WRITE)){c.force(true);}}
  private static void forceDirectory(Path path)throws IOException{if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"))try(var c=FileChannel.open(path,StandardOpenOption.READ)){c.force(true);}}
  public static void main(String[] args)throws Exception{if(args.length!=2 || !args[0].equals("migrate-offline"))throw new IllegalArgumentException("Usage: migrate-offline <trials.json>; stop server and disable restart first");System.out.println(migrateOffline(Path.of(args[1])));}
}
