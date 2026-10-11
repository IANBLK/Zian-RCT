package com.ianblk.zianrct.reward;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ShardedRewardTest {
  @TempDir Path root;
  @org.junit.jupiter.api.AfterEach void cleanup()throws Exception { RewardTestFiles.cleanup(root); }
  private RewardDefinition unique(){return new RewardDefinition("avecoins:coppercoin",1,List.of());}
  private RewardDefinition repeat(){return new RewardDefinition("avecoins:coppercoin",1,List.of(),RewardDefinition.Mode.REPEAT,1);}
  private void finish(RewardJournal journal,UUID owner,UUID id)throws Exception {
    RewardDelivery.deliver(journal,owner,id,new RewardDelivery.Port(){
      public String unavailable(RewardClaim.Part part){return null;}
      public RewardDelivery.Result apply(RewardClaim.Part part){return RewardDelivery.Result.APPLIED;}
    });
  }
  @Test void migrationPreservesOwnersPendingStatesCyclesAndBattleReceipts()throws Exception {
    Path path=root.resolve("claims.json");var old=RewardJournal.open(path);
    UUID a=UUID.randomUUID(),b=UUID.randomUUID(),battle=UUID.randomUUID();
    old.reserve(a,"one",unique());
    old.reserveAt(b,"repeat",repeat(),1000,battle);finish(old,b,old.latest(b,"repeat").id());
    old.reserveAt(b,"repeat",repeat(),9000000000000L,UUID.randomUUID());
    UUID pending=old.latest(b,"repeat").id();old.phase(pending,0,RewardClaim.Phase.APPLYING,"intent");
    var expectedA=old.forPlayer(a);var expectedB=old.forPlayer(b);byte[] before=Files.readAllBytes(path);
    var result=RewardStorageMaintenance.migrateOffline(path);assertEquals(2,result.players());assertEquals(3,result.claims());
    assertArrayEquals(before,Files.readAllBytes(result.archive()));
    var loaded=RewardJournal.open(path);
    assertEquals(expectedA,loaded.forPlayer(a));assertEquals(expectedB,loaded.forPlayer(b));
    assertNull(loaded.get(a,pending));assertNotNull(loaded.get(b,pending));
    var calls=new AtomicInteger();
    RewardDelivery.deliver(loaded,b,pending,new RewardDelivery.Port(){
      public String unavailable(RewardClaim.Part p){return null;}
      public RewardDelivery.Result apply(RewardClaim.Part p){calls.incrementAndGet();return RewardDelivery.Result.APPLIED;}
    });assertEquals(0,calls.get());
    loaded.resolve(b,pending,0,UUID.randomUUID(),"confirmed_delivered","verified from inventory");
    assertFalse(loaded.reserveAt(b,"repeat",repeat(),9000000060000L,battle));
    assertTrue(loaded.reserveAt(b,"repeat",repeat(),9000000060000L,UUID.randomUUID()));
    assertEquals(2,loaded.latest(b,"repeat").cycle());
    assertEquals(3,RewardJournal.open(path).forPlayer(b).size());
  }
  @Test void cachePressureDuringExternalDeliveryCannotEvictTheActiveReceipt()throws Exception {
    Path path=root.resolve("claims.json");RewardStorageMaintenance.migrateOffline(path);
    var journal=RewardJournal.open(path);UUID owner=UUID.randomUUID();journal.reserve(owner,"one",unique());
    UUID id=RewardJournal.id(owner,"one");
    RewardDelivery.deliver(journal,owner,id,new RewardDelivery.Port(){
      public String unavailable(RewardClaim.Part p){return null;}
      public RewardDelivery.Result apply(RewardClaim.Part p){for(int n=0;n<64;n++)journal.forPlayer(UUID.randomUUID());return RewardDelivery.Result.APPLIED;}
    });
    assertTrue(RewardJournal.open(path).get(owner,id).complete());
  }
  @Test void commitFailuresKeepEitherLegacyOrShardedStateAndACompleteBackup()throws Exception {
    for(var point:RewardStorageMaintenance.Point.values()) {
      Path folder=root.resolve(point.toString());Files.createDirectory(folder);Path path=folder.resolve("claims.json");
      UUID owner=UUID.randomUUID();var old=RewardJournal.open(path);old.reserve(owner,"one",unique());
      var expected=old.forPlayer(owner);byte[] before=Files.readAllBytes(path);
      assertThrows(java.io.IOException.class,()->RewardStorageMaintenance.migrateOffline(path,p->{if(p==point)throw new java.io.IOException("injected");}));
      if(point==RewardStorageMaintenance.Point.SHARDS_PREPARED)assertArrayEquals(before,Files.readAllBytes(path));
      assertEquals(expected,RewardJournal.open(path).forPlayer(owner));
      assertFalse(Files.exists(path.resolveSibling("claims.json.maintenance")));
      try(var files=Files.list(folder)) {
        var archive=files.filter(p->p.getFileName().toString().startsWith("claims.json.archive-")).findFirst().orElseThrow();
        assertArrayEquals(before,Files.readAllBytes(archive));
      }
    }
  }
  @Test void wrongOwnerInShardIsRejectedWithoutRepair()throws Exception {
    Path path=root.resolve("claims.json");UUID a=UUID.randomUUID(),b=UUID.randomUUID();var old=RewardJournal.open(path);
    old.reserve(a,"one",unique());old.reserve(b,"two",unique());var result=RewardStorageMaintenance.migrateOffline(path);
    Files.copy(result.directory().resolve(b+".json"),result.directory().resolve(a+".json"),StandardCopyOption.REPLACE_EXISTING);
    assertThrows(java.io.UncheckedIOException.class,()->RewardJournal.open(path).get(a,RewardJournal.id(a,"one")));
  }

  @Test void totalReceiptsAboveOldGlobalLimitRemainValidInSeparateOwnerFiles()throws Exception {
    Path path=root.resolve("large.json");var migration=RewardStorageMaintenance.migrateOffline(path);
    UUID a=UUID.randomUUID(),b=UUID.randomUUID();
    for(var owner:List.of(a,b)) {
      var entries=new ArrayList<RewardClaim>();
      for(int i=0;i<11000;i++) {
        String trainer="trainer_"+i;
        entries.add(new RewardClaim(RewardJournal.id(owner,trainer),owner,trainer,1000,
          List.of(new RewardClaim.Part(RewardClaim.Kind.COINS,"avecoins:coppercoin",1,RewardClaim.Phase.DELIVERED,"synthetic confirmed"))));
      }
      var json=new com.google.gson.JsonObject();json.addProperty("schemaVersion",2);json.add("claims",RewardFiles.GSON.toJsonTree(entries));
      RewardFiles.write(migration.directory().resolve(owner+".json"),json);
    }
    var loaded=RewardJournal.open(path);
    assertEquals(11000,loaded.forPlayer(a).size());assertEquals(11000,loaded.forPlayer(b).size());
    assertNull(loaded.get(a,RewardJournal.id(b,"trainer_0")));
    assertTrue(loaded.get(a,RewardJournal.id(a,"trainer_0")).complete());
  }
}
