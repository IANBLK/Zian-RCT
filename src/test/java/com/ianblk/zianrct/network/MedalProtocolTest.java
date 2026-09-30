package com.ianblk.zianrct.network;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
                List.of(new MedalClientSnapshot.OwnedMedalView("novato", 1234L, "BATTLE"))
        );

        String encoded = MedalProtocol.encode(snapshot);
        MedalClientSnapshot decoded = MedalProtocol.decode(MedalProtocol.CURRENT_VERSION, encoded);

        assertEquals("rassvet", decoded.activeProfile());
        assertEquals(1, decoded.definitions().size());
        assertEquals(20, decoded.definitions().getFirst().unlockCap());
        assertEquals("novato", decoded.owned().getFirst().medalId());
    }

    @Test
    void incompatibleProtocolVersionIsRejected() {
        assertThrows(IllegalStateException.class, () -> MedalProtocol.decode(999, "{}"));
    }

    @Test
    void oversizedStringsAndListsAreRejected() {
        String oversizedId = "x".repeat(MedalProtocol.MAX_ID_LENGTH + 1);
        MedalClientSnapshot badId = new MedalClientSnapshot(
                "rassvet",
                List.of(new MedalClientSnapshot.MedalDefinitionView(
                        oversizedId, "trainer", "name", "", "", "#FFFFFF", 0, 20
                )),
                List.of()
        );
        assertThrows(IllegalStateException.class, () -> MedalProtocol.encode(badId));

        List<MedalClientSnapshot.MedalDefinitionView> tooMany = new ArrayList<>();
        for (int i = 0; i <= MedalProtocol.MAX_MEDALS; i++) {
            tooMany.add(new MedalClientSnapshot.MedalDefinitionView(
                    "m" + i, "t" + i, "Medal " + i, "", "", "#FFFFFF", i, 20
            ));
        }
        MedalClientSnapshot badList = new MedalClientSnapshot("rassvet", tooMany, List.of());
        assertThrows(IllegalStateException.class, () -> MedalProtocol.encode(badList));
    }
}
