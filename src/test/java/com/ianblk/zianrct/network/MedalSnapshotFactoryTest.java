package com.ianblk.zianrct.network;

import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.medal.MedalOrigin;
import com.ianblk.zianrct.medal.MedalRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MedalSnapshotFactoryTest {
    @Test
    void orphanedOwnedMedalsAreFilteredOutOfSnapshot() {
        MedalClientSnapshot snapshot = MedalSnapshotFactory.build(
                ZianRctConfig.defaults(),
                List.of(
                        new MedalRecord("novato", 100L, MedalOrigin.COMMAND),
                        new MedalRecord("medalla_eliminada", 200L, MedalOrigin.BATTLE)
                )
        );

        assertEquals(1, snapshot.owned().size());
        assertEquals("novato", snapshot.owned().getFirst().medalId());
        MedalProtocol.validateSnapshot(snapshot);
    }
}
