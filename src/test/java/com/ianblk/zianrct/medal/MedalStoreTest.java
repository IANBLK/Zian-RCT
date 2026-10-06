package com.ianblk.zianrct.medal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MedalStoreTest {
    @TempDir
    Path tempDir;
    @org.junit.jupiter.api.AfterEach
    void cleanup() throws Exception { com.ianblk.zianrct.reward.RewardTestFiles.cleanup(tempDir); }

    @Test
    void failedWriteDoesNotGrantOrOverwriteTheExistingLedger() throws IOException {
        Path file = tempDir.resolve("medals.json");
        UUID player = UUID.randomUUID();
        MedalStore store = MedalStore.open(file);
        store.grantIfAbsent(player, new MedalRecord("novato", 1L, MedalOrigin.BATTLE), true);
        String saved = Files.readString(file);
        Files.createDirectory(tempDir.resolve("medals.json.tmp"));
        assertThrows(IOException.class, () -> store.grantIfAbsent(player,
                new MedalRecord("ferrum", 2L, MedalOrigin.BATTLE), true));
        assertFalse(store.has(player, "ferrum"));
        assertEquals(saved, Files.readString(file));
        assertFalse(MedalStore.open(file).has(player, "ferrum"));
    }

    @Test
    void nullMedalRecordsAreRejectedWithoutChangingTheFile() throws IOException {
        Path file = tempDir.resolve("invalid.json");
        String json = "{\"schemaVersion\":2,\"players\":[{\"uuid\":\"" + UUID.randomUUID()
                + "\",\"medals\":[null]}]}";
        Files.writeString(file, json);
        assertThrows(IOException.class, () -> MedalStore.open(file));
        assertEquals(json, Files.readString(file));
    }

    @Test
    void persistenceSurvivesReopenAndDuplicateGrantDoesNotOverwrite() throws IOException {
        Path file = tempDir.resolve("world/data/zianrct-medals.json");
        UUID player = UUID.randomUUID();

        MedalStore first = MedalStore.open(file);
        assertTrue(first.grantIfAbsent(
                player,
                new MedalRecord("novato", 1111L, MedalOrigin.BATTLE),
                true
        ));
        assertFalse(first.grantIfAbsent(
                player,
                new MedalRecord("novato", 9999L, MedalOrigin.COMMAND),
                true
        ));

        MedalStore reopened = MedalStore.open(file);
        assertEquals(1, reopened.medals(player).size());
        MedalRecord persisted = reopened.medals(player).getFirst();
        assertEquals("novato", persisted.medalId());
        assertEquals(1111L, persisted.grantedAtEpochMilli());
        assertEquals(MedalOrigin.BATTLE, persisted.origin());
    }

    @Test
    void revokePersistsAndBlocksReconciliationUntilFreshGrantClearsIt() throws IOException {
        Path file = tempDir.resolve("zianrct-medals.json");
        UUID player = UUID.randomUUID();
        MedalStore store = MedalStore.open(file);
        store.grantIfAbsent(player, new MedalRecord("novato", 1111L, MedalOrigin.BATTLE), true);

        assertTrue(store.revoke(player, "novato"));
        MedalStore reopened = MedalStore.open(file);
        assertTrue(reopened.medals(player).isEmpty());
        assertTrue(reopened.isRevoked(player, "novato"));
        assertFalse(reopened.grantIfAbsent(
                player,
                new MedalRecord("novato", 2222L, MedalOrigin.RECONCILED),
                false
        ));
        assertTrue(reopened.isRevoked(player, "novato"));

        assertTrue(reopened.grantIfAbsent(
                player,
                new MedalRecord("novato", 3333L, MedalOrigin.BATTLE),
                true
        ));
        MedalStore afterBattle = MedalStore.open(file);
        assertFalse(afterBattle.isRevoked(player, "novato"));
        assertEquals(3333L, afterBattle.medals(player).getFirst().grantedAtEpochMilli());
    }

    @Test
    void schemaOneFilesMigrateWithEmptyRevocationSet() throws IOException {
        Path file = tempDir.resolve("legacy.json");
        UUID player = UUID.randomUUID();
        Files.writeString(file, """
                {
                  "schemaVersion": 1,
                  "players": [
                    {
                      "uuid": "%s",
                      "medals": [
                        {
                          "medalId": "novato",
                          "grantedAtEpochMilli": 1111,
                          "origin": "BATTLE"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(player));

        MedalStore migrated = MedalStore.open(file);
        assertEquals(1, migrated.medals(player).size());
        assertFalse(migrated.isRevoked(player, "novato"));
    }
}
