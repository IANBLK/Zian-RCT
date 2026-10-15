package com.ianblk.zianrct.legendary;
import com.ianblk.zianrct.storage.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SQLiteTrialTest {
  @TempDir Path world;
  @AfterEach void close()throws Exception{StorageFiles.closeAll();}
  @Test void frozenPokemonAndDeliveryBarrierSurviveSQLiteMigrationAndRestart()throws Exception{
    Path path=world.resolve("data/zianrct-legendary-trials.json");UUID owner=UUID.randomUUID();
    var old=TrialJournal.open(path);old.unlock(owner,"zian_custom_guardian","zian_custom_boss","mew",50,512,UUID.randomUUID());
    var frozen=old.victory(owner,"zian_custom_guardian",UUID.randomUUID(),new Random(1));old.phase(owner,"zian_custom_guardian",TrialJournal.Phase.APPLYING);
    var expected=old.get(owner,"zian_custom_guardian");StorageMaintenance.migrateOffline(world);Files.move(path,world.resolve("original.json"));
    var loaded=TrialJournal.open(path);assertEquals(expected,loaded.get(owner,"zian_custom_guardian"));
    assertEquals(frozen.pokemon(),loaded.get(owner,"zian_custom_guardian").pokemon());
    loaded.phase(owner,"zian_custom_guardian",TrialJournal.Phase.DELIVERED);StorageFiles.closeAll();
    var restarted=TrialJournal.open(path);assertFalse(restarted.unlock(owner,"zian_custom_guardian","zian_custom_boss","mew",50,512,UUID.randomUUID()));
    assertEquals(TrialJournal.Phase.DELIVERED,restarted.get(owner,"zian_custom_guardian").phase());
    assertEquals(frozen.ivs(),restarted.get(owner,"zian_custom_guardian").ivs());assertEquals(frozen.shiny(),restarted.get(owner,"zian_custom_guardian").shiny());
  }
}
