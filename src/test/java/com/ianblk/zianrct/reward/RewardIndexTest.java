package com.ianblk.zianrct.reward;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RewardIndexTest {
  @TempDir Path dir;
  @org.junit.jupiter.api.AfterEach void cleanup() throws Exception { RewardTestFiles.cleanup(dir); }
  @Test void ownersAndPhaseChangesRemainCorrectAfterReopen() throws Exception {
    var path=dir.resolve("claims.json");var journal=RewardJournal.open(path);
    var a=UUID.randomUUID();var b=UUID.randomUUID();
    var reward=new RewardDefinition("avecoins:coppercoin",1,List.of());
    assertTrue(journal.reserve(a,"one",reward));assertTrue(journal.reserve(b,"two",reward));
    var id=RewardJournal.id(a,"one");
    journal.phase(id,0,RewardClaim.Phase.APPLYING,"intent");
    assertEquals(1,journal.forPlayer(a).size());assertEquals(1,journal.forPlayer(b).size());
    assertEquals(RewardClaim.Phase.APPLYING,journal.forPlayer(a).getFirst().parts().getFirst().phase());
    assertThrows(UnsupportedOperationException.class,()->journal.forPlayer(a).clear());
    var reopened=RewardJournal.open(path);
    assertEquals(journal.forPlayer(a),reopened.forPlayer(a));
    assertTrue(reopened.forPlayer(UUID.randomUUID()).isEmpty());
  }
}
