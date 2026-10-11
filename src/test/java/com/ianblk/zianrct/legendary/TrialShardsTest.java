package com.ianblk.zianrct.legendary;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class TrialShardsTest {
  @TempDir Path root;
  @org.junit.jupiter.api.AfterEach void cleanup()throws Exception {com.ianblk.zianrct.reward.RewardTestFiles.cleanup(root);}
  @Test void migrationKeepsFrozenPokemonAndNeverReopensDeliveredOrApplyingTrials()throws Exception {
    Path path=root.resolve("trials.json");var old=TrialJournal.open(path);UUID a=UUID.randomUUID(),b=UUID.randomUUID();
    old.unlock(a,"zian_custom_guardian","zian_custom_boss","mew",50,512,UUID.randomUUID());
    var frozen=old.victory(a,"zian_custom_guardian",UUID.randomUUID(),new Random(1));old.phase(a,"zian_custom_guardian",TrialJournal.Phase.APPLYING);
    old.unlock(b,"zian_custom_guardian","zian_custom_boss","mew",50,512,UUID.randomUUID());old.victory(b,"zian_custom_guardian",UUID.randomUUID(),new Random(2));old.phase(b,"zian_custom_guardian",TrialJournal.Phase.APPLYING);old.phase(b,"zian_custom_guardian",TrialJournal.Phase.DELIVERED);
    var expected=new HashSet<>(old.all());TrialStorageMaintenance.migrateOffline(path);var loaded=TrialJournal.open(path);
    assertEquals(expected,new HashSet<>(loaded.all()));
    assertEquals(frozen.pokemon(),loaded.get(a,"zian_custom_guardian").pokemon());assertEquals(frozen.ivs(),loaded.get(a,"zian_custom_guardian").ivs());
    assertEquals(TrialJournal.Phase.APPLYING,loaded.get(a,"zian_custom_guardian").phase());
    assertFalse(loaded.unlock(b,"zian_custom_guardian","zian_custom_boss","mew",50,512,UUID.randomUUID()));
    assertEquals(TrialJournal.Phase.DELIVERED,loaded.victory(b,"zian_custom_guardian",UUID.randomUUID(),new Random(3)).phase());
    for(int i=0;i<64;i++)loaded.forPlayer(UUID.randomUUID());
    assertEquals(frozen.pokemon(),loaded.get(a,"zian_custom_guardian").pokemon());
  }
}
