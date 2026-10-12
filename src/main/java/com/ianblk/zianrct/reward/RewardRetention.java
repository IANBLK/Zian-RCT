package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.storage.StorageFiles;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.io.*;

/** Completed details can expire; durable IDs, source/cycle, cooldown and battle evidence remain. */
public final class RewardRetention {
  public record Result(long eligible,long protectedPending,boolean applied) {}
  public static Result cleanupOffline(Path input,boolean apply)throws Exception {
    Path world=input.toAbsolutePath().normalize();Properties settings=new Properties();
    try(var reader=Files.newBufferedReader(world.resolve("zian-storage/storage.properties"))){settings.load(reader);}
    if(!"sqlite-v1".equals(settings.getProperty(StorageFiles.MOD)))throw new IOException("SQLite not enabled");
    int days=Integer.parseInt(settings.getProperty(StorageFiles.MOD+".history-days",settings.getProperty("history-days","90")));
    int inactive=Integer.parseInt(settings.getProperty(StorageFiles.MOD+".inactive-days",settings.getProperty("inactive-days","30")));
    if(days<7||days>3650||inactive<1||inactive>3650)throw new IOException("Invalid retention policy");
    long now=System.currentTimeMillis(),old=now-days*86400000L,lastSeen=now-inactive*86400000L,eligible=0,pending=0;
    try(var session=FileChannel.open(world.resolve("session.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE)){
      var lock=session.tryLock();if(lock==null)throw new IOException("Stop Minecraft before cleanup");
      try(lock){
        for(Path path:StorageFiles.documents(world)){
          String name=world.relativize(path).toString().replace('\\','/');
          if(!name.matches("data/zianrct-rewards\\.json\\.players-[0-9a-f-]{36}/[0-9a-f-]{36}\\.json"))continue;
          var model=JsonParser.parseString(StorageFiles.readString(path)).getAsJsonObject();
          if(!model.keySet().equals(Set.of("schemaVersion","claims")) || model.get("schemaVersion").getAsInt()!=2)continue;
          var journal=RewardJournal.open(path); // domain validation including all prior cycles
          var receipts=new ArrayList<StorageFiles.RewardReceipt>();var ids=new HashSet<UUID>();var removed=new JsonArray();
          for(var raw:model.getAsJsonArray("claims")){
            var json=raw.getAsJsonObject();var claim=RewardFiles.GSON.fromJson(json,RewardClaim.class);
            if(!claim.complete()){pending++;continue;}
            if(claim.created()>=old || !StorageFiles.knownInactive(path,claim.player(),lastSeen))continue;
            if(!Set.of("id","player","trainer","created","parts","cycle","cooldownMinutes","battle","retired").containsAll(json.keySet()))continue;
            boolean known=true;for(var part:json.getAsJsonArray("parts"))if(!part.getAsJsonObject().keySet().equals(Set.of("kind","data","amount","phase","detail")))known=false;
            if(!known)continue;
            receipts.add(new StorageFiles.RewardReceipt(claim.id(),claim.player(),claim.trainer(),claim.cycle(),claim.created(),claim.cooldownMinutes(),claim.battle()));ids.add(claim.id());removed.add(raw);
          }
          eligible+=receipts.size();if(!apply||receipts.isEmpty())continue;
          var kept=new JsonArray();for(var raw:model.getAsJsonArray("claims"))if(!ids.contains(UUID.fromString(raw.getAsJsonObject().get("id").getAsString())))kept.add(raw);
          model.add("claims",kept);
          var audit=new JsonObject();audit.addProperty("schema",1);audit.addProperty("scope",name);audit.add("retired",removed);
          Path archive=path.getParent().resolve("reward-history.archive-"+UUID.randomUUID()+".json.gz");
          try(var gzip=new GZIPOutputStream(StorageFiles.newOutputStream(archive,StandardOpenOption.CREATE_NEW))){gzip.write(RewardFiles.GSON.toJson(audit).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
          if(!readArchive(archive).equals(audit))throw new IOException("Reward archive verification failed");
          StorageFiles.retireRewards(path,receipts,RewardFiles.GSON.toJson(model).getBytes(java.nio.charset.StandardCharsets.UTF_8));
          var verified=RewardJournal.open(path);
          for(var receipt:receipts){var minimal=verified.get(receipt.player(),receipt.id());if(minimal==null||!minimal.retired()||!minimal.complete())throw new IOException("Retired receipt verification failed");}
        }
        for(Path archive:StorageFiles.documents(world)){
          if(!archive.getFileName().toString().matches("reward-history\\.archive-[0-9a-f-]{36}\\.json\\.gz")||StorageFiles.modifiedMillis(archive)>=old)continue;
          var audit=readArchive(archive);if(!audit.keySet().equals(Set.of("schema","scope","retired"))||audit.get("schema").getAsInt()!=1)throw new IOException("Unknown reward archive format");
          Path source=world.resolve(audit.get("scope").getAsString()).normalize();
          if(!source.startsWith(world)||!source.getParent().equals(archive.getParent()))throw new IOException("Reward archive scope mismatch");
          for(var raw:audit.getAsJsonArray("retired")){
            var claim=RewardFiles.GSON.fromJson(raw,RewardClaim.class);
            var receipt=new StorageFiles.RewardReceipt(claim.id(),claim.player(),claim.trainer(),claim.cycle(),claim.created(),claim.cooldownMinutes(),claim.battle());
            if(!claim.complete()||!receipt.equals(StorageFiles.retiredReward(source,claim.player(),claim.trainer(),claim.id())))throw new IOException("Archive receipt missing or different; preserving audit history");
          }
          if(apply)StorageFiles.delete(archive);
        }
      }finally{StorageFiles.closeAll();}
    }
    return new Result(eligible,pending,apply);
  }
  private static JsonObject readArchive(Path archive)throws IOException {
    try(var input=new GZIPInputStream(StorageFiles.newInputStream(archive))){byte[] raw=input.readNBytes(16*1024*1024+1);if(raw.length>16*1024*1024)throw new IOException("Reward audit archive too large");return JsonParser.parseString(new String(raw,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
  }
  public static void main(String[] args)throws Exception{if(args.length!=2||!Set.of("preview","apply").contains(args[0]))throw new IllegalArgumentException("Usage: preview|apply <world>; stop server first");System.out.println(cleanupOffline(Path.of(args[1]),args[0].equals("apply")));}
}
