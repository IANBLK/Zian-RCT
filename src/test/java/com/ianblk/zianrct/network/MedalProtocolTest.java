package com.ianblk.zianrct.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MedalProtocolTest {
    @Test
    void versionedSnapshotRoundTripsThroughJsonEnvelope() {
        MedalClientSnapshot snapshot = new MedalClientSnapshot(
                "rassvet",
                List.of(new MedalClientSnapshot.MedalDefinitionView(
                        "novato",
                        "rassvet_leader_novato",
                        "Medalla Novato",
                        "Primera medalla",
                        "zianrct:textures/gui/medals/novato.png",
                        "#D49A35",
                        0,
                        20
                )),
                List.of(new MedalClientSnapshot.OwnedMedalView("novato", 1234L, "BATTLE")),
                List.of("novato")
        );

        String encoded = MedalProtocol.encode(snapshot);
        MedalClientSnapshot decoded = MedalProtocol.decode(MedalProtocol.CURRENT_VERSION, encoded);

        assertEquals("rassvet", decoded.activeProfile());
        assertEquals(1, decoded.definitions().size());
        assertEquals(20, decoded.definitions().getFirst().unlockCap());
        assertEquals("novato", decoded.owned().getFirst().medalId());
        assertEquals(List.of("novato"), decoded.notifications());
    }

    @Test
    void incompatibleProtocolVersionIsRejected() {
        assertThrows(IllegalStateException.class, () -> MedalProtocol.decode(999, "{}"));
    }
}
