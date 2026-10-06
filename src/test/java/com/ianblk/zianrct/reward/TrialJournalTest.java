package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.legendary.TrialJournal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrialJournalTest {
    @TempDir Path temp;
    @org.junit.jupiter.api.AfterEach void cleanup() throws Exception{RewardTestFiles.cleanup(temp);}
    private final UUID player=UUID.randomUUID(),bossBattle=UUID.randomUUID();
    private final String trainer="zian_custom_legend",boss="zian_custom_boss";
    private TrialJournal opened() throws Exception{var journal=TrialJournal.open(temp.resolve("trials.json"));journal.unlock(player,trainer,boss,"mewtwo",70,512,bossBattle);return journal;}
    @Test void bossUnlockIsPerPlayerAndCannotBeReissuedAfterCompletion() throws Exception{
        var journal=opened();assertFalse(journal.unlock(player,trainer,boss,"mewtwo",70,512,UUID.randomUUID()));
        var victory=journal.victory(player,trainer,UUID.randomUUID(),new Random(1));
        journal.phase(player,trainer,TrialJournal.Phase.APPLYING);journal.phase(player,trainer,TrialJournal.Phase.DELIVERED);
        var restored=TrialJournal.open(temp.resolve("trials.json"));assertEquals(TrialJournal.Phase.DELIVERED,restored.get(player,trainer).phase());
        assertFalse(restored.unlock(player,trainer,boss,"mewtwo",70,512,UUID.randomUUID()));
        assertEquals(victory.pokemon(),restored.victory(player,trainer,UUID.randomUUID(),new Random()).pokemon());
        assertTrue(restored.unlock(UUID.randomUUID(),trainer,boss,"mewtwo",70,512,UUID.randomUUID()));
    }
    @Test void winningWithoutQualificationDoesNotReserveAPokemon() throws Exception{
        var journal=TrialJournal.open(temp.resolve("trials.json"));assertNull(journal.victory(player,trainer,UUID.randomUUID(),new Random()));
        assertTrue(journal.all().isEmpty());
    }
    @Test void shinyAndIvsAreFrozenBeforeDeliveryAndNeverRerolledOnRetry() throws Exception{
        var journal=opened();var first=journal.victory(player,trainer,UUID.randomUUID(),new Random(1));
        assertEquals(6,first.ivs().size());assertTrue(first.ivs().stream().allMatch(i->i>=25 && i<=30));
        var restored=TrialJournal.open(temp.resolve("trials.json"));assertEquals(first,restored.victory(player,trainer,UUID.randomUUID(),new Random(99)));
        assertNotNull(first.pokemon());assertEquals(TrialJournal.Phase.READY,first.phase());
    }
    @Test void interruptedOrUncertainDeliveryCannotReopenReadyOrUnlock() throws Exception{
        var journal=opened();journal.victory(player,trainer,UUID.randomUUID(),new Random());journal.phase(player,trainer,TrialJournal.Phase.APPLYING);
        var restored=TrialJournal.open(temp.resolve("trials.json"));assertEquals(TrialJournal.Phase.APPLYING,restored.get(player,trainer).phase());
        assertThrows(IllegalArgumentException.class,()->restored.phase(player,trainer,TrialJournal.Phase.READY));
        restored.phase(player,trainer,TrialJournal.Phase.REVIEW);
        assertFalse(restored.unlock(player,trainer,boss,"mewtwo",70,512,UUID.randomUUID()));
        restored.phase(player,trainer,TrialJournal.Phase.DELIVERED);
        assertThrows(IllegalArgumentException.class,()->restored.phase(player,trainer,TrialJournal.Phase.UNLOCKED));
    }
    @Test void anotherPlayerCannotTransitionOrClaimTheEntitlement() throws Exception{
        var journal=opened();UUID stranger=UUID.randomUUID();assertNull(journal.get(stranger,trainer));
        assertThrows(IllegalArgumentException.class,()->journal.phase(stranger,trainer,TrialJournal.Phase.APPLYING));
        assertThrows(IllegalArgumentException.class,()->journal.victory(player,trainer,bossBattle,new Random()));
    }
    @Test void corruptLedgerIsNotReplacedWithAnEmptyOne() throws Exception{
        var path=temp.resolve("trials.json");Files.writeString(path,"{}");
        assertThrows(java.io.IOException.class,()->TrialJournal.open(path));assertEquals("{}",Files.readString(path));
    }
    @Test void persistenceFailureBlocksFurtherMutations() throws Exception{
        var journal=opened();var path=temp.resolve("trials.json");Files.delete(path);Files.createDirectory(path);
        assertThrows(java.io.IOException.class,()->journal.victory(player,trainer,UUID.randomUUID(),new Random()));
        assertThrows(IllegalStateException.class,()->journal.get(player,trainer));
    }
}
