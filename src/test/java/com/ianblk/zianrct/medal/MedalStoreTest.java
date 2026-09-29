package com.ianblk.zianrct.medal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedalStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistenceSurvivesReopenAndDuplicateGrantDoesNotOverwrite() throws IOException {
        Path file = tempDir.resolve("world/data/zianrct-medals.json");
        UUID player = UUID.randomUUID();

        MedalStore first = MedalStore.open(file);
        assertTrue(first.grantIfAbsent(
                player,
                new MedalRecord("novato", 1111L, MedalOrigin.BATTLE)
        ));
        assertFalse(first.grantIfAbsent(
                player,
                new MedalRecord("novato", 9999L, MedalOrigin.COMMAND)
        ));

        MedalStore reopened = MedalStore.open(file);
        assertEquals(1, reopened.medals(player).size());
        MedalRecord persisted = reopened.medals(player).getFirst();
        assertEquals("novato", persisted.medalId());
        assertEquals(1111L, persisted.grantedAtEpochMilli());
        assertEquals(MedalOrigin.BATTLE, persisted.origin());
    }

    @Test
    void revokeIsPersisted() throws IOException {
        Path file = tempDir.resolve("zianrct-medals.json");
        UUID player = UUID.randomUUID();
        MedalStore store = MedalStore.open(file);
        store.grantIfAbsent(player, new MedalRecord("novato", 1111L, MedalOrigin.RECONCILED));

        assertTrue(store.revoke(player, "novato"));
        assertTrue(MedalStore.open(file).medals(player).isEmpty());
    }
}
