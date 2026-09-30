package com.ianblk.zianrct.client;

import com.ianblk.zianrct.network.MedalClientSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientMedalStateTest {
    @AfterEach
    void clearState() {
        ClientMedalState.clear();
    }

    @Test
    void snapshotNeverCreatesAwardNotification() {
        MedalClientSnapshot snapshot = new MedalClientSnapshot(
                "rassvet",
                List.of(),
                List.of()
        );

        ClientMedalState.apply(snapshot);

        assertEquals("rassvet", ClientMedalState.current().orElseThrow().activeProfile());
        assertTrue(ClientMedalState.drainAwards().isEmpty());
    }

    @Test
    void awardPayloadQueueIsSeparateAndDrainsOnce() {
        ClientMedalState.notifyAward("novato");
        ClientMedalState.notifyAward("ferrum");

        assertEquals(List.of("novato", "ferrum"), ClientMedalState.drainAwards());
        assertTrue(ClientMedalState.drainAwards().isEmpty());
    }
}
