package com.ianblk.zianrct.standalone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgressLedgerTest {
    @TempDir Path root;
    @Test void victoryAndLossSurviveRestartAndDuplicateBattleCannotRepublish() throws Exception {
        Path path=root.resolve("player.json");UUID win=UUID.randomUUID(),loss=UUID.randomUUID();
        var ledger=ProgressLedger.open(path,Set.of("old_trainer"));
        assertTrue(ledger.record(win,"boss",true));assertTrue(ledger.record(loss,"guardian",false));
        var restored=ProgressLedger.open(path,Set.of("must_not_reimport"));
        assertEquals(Set.of("old_trainer","boss"),restored.defeated());assertEquals(1,restored.losses("guardian"));assertEquals(1,restored.wins("boss"));
        assertFalse(restored.record(win,"boss",true));assertFalse(restored.record(loss,"guardian",false));
        assertEquals(1,restored.losses("guardian"));
    }
    @Test void administratorChangesOnlyConfiguredChainAndDoNotEraseOtherVictories() throws Exception {
        Path path=root.resolve("player.json");var ledger=ProgressLedger.open(path,Set.of("novato","ferrum","custom_boss"));
        ledger.replaceChain(Set.of("novato","ferrum"),Set.of("novato"));
        assertEquals(Set.of("novato","custom_boss"),ProgressLedger.open(path,Set.of()).defeated());
        assertEquals(0,ledger.wins("novato"));
    }
    @Test void independentVictoriesDoNotActivateLeagueCapsAndLeagueActivationIsDurable() throws Exception {
        Path path=root.resolve("player.json");var ledger=ProgressLedger.open(path,Set.of(),false);
        ledger.record(UUID.randomUUID(),"custom_boss",true);
        assertFalse(ProgressLedger.open(path,Set.of()).leagueActive());
        ledger.record(UUID.randomUUID(),"novato",true,true);
        assertTrue(ProgressLedger.open(path,Set.of()).leagueActive());
        ledger.replaceChain(Set.of("novato"),Set.of());
        assertEquals(1,ledger.wins("novato"));
        assertTrue(ProgressLedger.open(path,Set.of()).leagueActive());
    }
    @Test void corruptProgressIsPreservedAndNeverReplacedWithEmptyProgress() throws Exception {
        Path path=root.resolve("player.json");Files.writeString(path,"broken");
        assertThrows(java.io.IOException.class,()->ProgressLedger.open(path,Set.of("imported")));
        assertEquals("broken",Files.readString(path));
    }
    @Test void failedAtomicWriteDoesNotPublishVictory() throws Exception {
        Path path=root.resolve("player.json");var ledger=ProgressLedger.open(path,Set.of());
        Files.delete(path);Files.createDirectory(path);Files.writeString(path.resolve("obstruction"),"keep");
        assertThrows(java.io.IOException.class,()->ledger.record(UUID.randomUUID(),"boss",true));
        assertTrue(ledger.defeated().isEmpty());assertEquals("keep",Files.readString(path.resolve("obstruction")));
    }
}
