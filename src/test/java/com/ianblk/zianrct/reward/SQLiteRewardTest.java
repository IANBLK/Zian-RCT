package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.storage.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SQLiteRewardTest {
  @TempDir Path world;
  @AfterEach void close()throws Exception{StorageFiles.closeAll();}
  @Test void migratedPendingAndPaidReceiptsSurviveWritesAndCacheEviction()throws Exception{
    Path path=world.resolve("data/zianrct-rewards.json");
    UUID owner=UUID.randomUUID(),battle=UUID.randomUUID();
    var reward=new RewardDefinition("avecoins:coppercoin",1,List.of(),RewardDefinition.Mode.REPEAT,1);
    var old=RewardJournal.open(path);old.reserveAt(owner,"fixture",reward,1000,battle);
    UUID id=old.latest(owner,"fixture").id();old.phase(id,0,RewardClaim.Phase.APPLYING,"uncertain external effect");
    RewardStorageMaintenance.migrateOffline(path);StorageMaintenance.migrateOffline(world);
    Files.move(path,world.resolve("legacy-rewards.json"));
    var model=com.google.gson.JsonParser.parseString(Files.readString(world.resolve("legacy-rewards.json"))).getAsJsonObject();
    Files.move(world.resolve("data").resolve(model.get("directory").getAsString()),world.resolve("legacy-owners"));
    var migrated=RewardJournal.open(path);assertEquals(RewardClaim.Phase.APPLYING,migrated.get(owner,id).parts().getFirst().phase());
    AtomicInteger effects=new AtomicInteger();
    RewardDelivery.Port port=new RewardDelivery.Port(){public String unavailable(RewardClaim.Part p){return null;}public RewardDelivery.Result apply(RewardClaim.Part p){effects.incrementAndGet();for(int n=0;n<64;n++)migrated.forPlayer(UUID.randomUUID());return RewardDelivery.Result.APPLIED;}};
    RewardDelivery.deliver(migrated,owner,id,port);assertEquals(0,effects.get());
    migrated.resolve(owner,id,0,UUID.randomUUID(),"confirmed_delivered","synthetic inventory proof");
    assertFalse(migrated.reserveAt(owner,"fixture",reward,9000000000000L,battle));
    assertTrue(migrated.reserveAt(owner,"fixture",reward,9000000000000L,UUID.randomUUID()));
    var next=migrated.latest(owner,"fixture");RewardDelivery.deliver(migrated,owner,next.id(),port);assertEquals(1,effects.get());
    StorageFiles.closeAll();var reopened=RewardJournal.open(path);
    assertTrue(reopened.get(owner,id).complete());assertTrue(reopened.get(owner,next.id()).complete());
    assertFalse(reopened.reserveAt(owner,"fixture",reward,9000000060000L,battle));
    assertFalse(Files.exists(path));
  }

  @Test void retiredCompletedDetailsKeepCyclesCooldownAndBattleReceipts()throws Exception{
    Path path=world.resolve("data/zianrct-rewards.json");UUID owner=UUID.randomUUID(),battle=UUID.randomUUID();
    long old=System.currentTimeMillis()-100L*86400000L;
    Files.createDirectories(world.resolve("playerdata"));Path saved=world.resolve("playerdata").resolve(owner+".dat");Files.write(saved,new byte[]{1});Files.setLastModifiedTime(saved,java.nio.file.attribute.FileTime.fromMillis(old));
    var reward=new RewardDefinition("avecoins:coppercoin",1,List.of(),RewardDefinition.Mode.REPEAT,1);
    var journal=RewardJournal.open(path);assertTrue(journal.reserveAt(owner,"fixture",reward,old,battle));
    UUID id=journal.latest(owner,"fixture").id();journal.phase(id,0,RewardClaim.Phase.APPLYING,"synthetic reservation");journal.phase(id,0,RewardClaim.Phase.DELIVERED,"synthetic proof");
    // Use an aged fixture; completion records the current wall clock in Manager.
    var json=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    json.getAsJsonArray("claims").get(0).getAsJsonObject().addProperty("created",old);
    Files.writeString(path,RewardFiles.GSON.toJson(json));
    RewardStorageMaintenance.migrateOffline(path);StorageMaintenance.migrateOffline(world);
    assertEquals(1,RewardRetention.cleanupOffline(world,false).eligible());
    assertEquals(1,RewardRetention.cleanupOffline(world,true).eligible());
    var reopened=RewardJournal.open(path);assertTrue(reopened.get(owner,id).retired());assertTrue(reopened.get(owner,id).complete());
    assertFalse(reopened.reserveAt(owner,"fixture",reward,System.currentTimeMillis(),battle));
    assertTrue(reopened.reserveAt(owner,"fixture",reward,System.currentTimeMillis(),UUID.randomUUID()));
    assertEquals(1,reopened.latest(owner,"fixture").cycle());
    StorageFiles.closeAll();reopened=RewardJournal.open(path);
    assertTrue(reopened.get(owner,id).retired());assertEquals(1,reopened.latest(owner,"fixture").cycle());
    assertEquals(1,RewardRetention.cleanupOffline(world,true).protectedPending());
  }

  @Test void clockAdjustmentCannotHideTheLatestRetiredCycle()throws Exception{
    Path path=world.resolve("data/zianrct-rewards.json");UUID owner=UUID.randomUUID();long old=System.currentTimeMillis()-100L*86400000L;
    Files.createDirectories(world.resolve("playerdata"));Path saved=world.resolve("playerdata").resolve(owner+".dat");Files.write(saved,new byte[]{1});Files.setLastModifiedTime(saved,java.nio.file.attribute.FileTime.fromMillis(old));
    var reward=new RewardDefinition("avecoins:coppercoin",1,List.of(),RewardDefinition.Mode.REPEAT,1);var journal=RewardJournal.open(path);
    for(int n=0;n<2;n++){assertTrue(journal.reserveAt(owner,"fixture",reward,9000000000000L+n*60000L,UUID.randomUUID()));var id=journal.latest(owner,"fixture").id();journal.phase(id,0,RewardClaim.Phase.APPLYING,"");journal.phase(id,0,RewardClaim.Phase.DELIVERED,"");}
    var json=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    json.getAsJsonArray("claims").get(1).getAsJsonObject().addProperty("created",old);Files.writeString(path,RewardFiles.GSON.toJson(json));
    RewardStorageMaintenance.migrateOffline(path);StorageMaintenance.migrateOffline(world);assertEquals(1,RewardRetention.cleanupOffline(world,true).eligible());
    journal=RewardJournal.open(path);assertEquals(1,journal.latest(owner,"fixture").cycle());assertTrue(journal.latest(owner,"fixture").retired());
    assertTrue(journal.reserveAt(owner,"fixture",reward,9000000060000L,UUID.randomUUID()));assertEquals(2,journal.latest(owner,"fixture").cycle());
  }
}
