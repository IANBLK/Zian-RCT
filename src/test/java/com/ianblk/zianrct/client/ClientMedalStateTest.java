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
    void applyStoresSnapshotAndNotificationsDrainOnce() {
        MedalClientSnapshot snapshot = new MedalClientSnapshot(
                "rassvet",
                List.of(),
                List.of(),
                List.of("novato", "ferrum")
        );

        ClientMedalState.apply(snapshot);

        assertEquals("rassvet", ClientMedalState.current().orElseThrow().activeProfile());
        assertEquals(List.of("novato", "ferrum"), ClientMedalState.drainNotifications());
        assertTrue(ClientMedalState.drainNotifications().isEmpty());
    }
}
