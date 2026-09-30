package com.ianblk.zianrct.network;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void largestAsciiFieldBoundSnapshotFitsUtf8Envelope() {
        List<MedalClientSnapshot.MedalDefinitionView> definitions = new ArrayList<>();
        List<MedalClientSnapshot.OwnedMedalView> owned = new ArrayList<>();
        for (int i = 0; i < MedalProtocol.MAX_MEDALS; i++) {
            String suffix = Integer.toString(i);
            String id = "m".repeat(MedalProtocol.MAX_ID_LENGTH - suffix.length()) + suffix;
            String trainer = "t".repeat(MedalProtocol.MAX_ID_LENGTH - suffix.length()) + suffix;
            definitions.add(new MedalClientSnapshot.MedalDefinitionView(
                    id,
                    trainer,
                    "n".repeat(MedalProtocol.MAX_NAME_LENGTH),
                    "d".repeat(MedalProtocol.MAX_DESCRIPTION_LENGTH),
                    "x".repeat(MedalProtocol.MAX_TEXTURE_LENGTH),
                    "#" + "a".repeat(MedalProtocol.MAX_COLOR_LENGTH - 1),
                    i,
                    10_000
            ));
            owned.add(new MedalClientSnapshot.OwnedMedalView(
                    id,
                    1L + i,
                    "o".repeat(MedalProtocol.MAX_ORIGIN_LENGTH)
            ));
        }

        String encoded = MedalProtocol.encode(new MedalClientSnapshot(
                "p".repeat(MedalProtocol.MAX_PROFILE_LENGTH),
                definitions,
                owned
        ));

        assertTrue(encoded.getBytes(StandardCharsets.UTF_8).length <= MedalProtocol.MAX_SNAPSHOT_UTF8_BYTES);
        assertEquals(MedalProtocol.MAX_MEDALS,
                MedalProtocol.decode(MedalProtocol.CURRENT_VERSION, encoded).definitions().size());
    }

    @Test
    void accentedTextIsMeasuredInUtf8BytesNotJavaCharacters() {
        MedalClientSnapshot snapshot = new MedalClientSnapshot(
                "rassvet",
                List.of(new MedalClientSnapshot.MedalDefinitionView(
                        "novato",
                        "rassvet_leader_novato",
                        "á".repeat(64),
                        "á".repeat(512),
                        "zianrct:textures/gui/medals/novato.png",
                        "#D49A35",
                        0,
                        20
                )),
                List.of()
        );

        String encoded = MedalProtocol.encode(snapshot);
        assertTrue(MedalProtocol.utf8Length(encoded) > encoded.length());
        assertTrue(MedalProtocol.utf8Length(encoded) <= MedalProtocol.MAX_SNAPSHOT_UTF8_BYTES);
    }

    @Test
    void multibyteSnapshotThatRespectsFieldLengthsCanStillExceedByteBudget() {
        List<MedalClientSnapshot.MedalDefinitionView> definitions = new ArrayList<>();
        for (int i = 0; i < MedalProtocol.MAX_MEDALS; i++) {
            definitions.add(new MedalClientSnapshot.MedalDefinitionView(
                    "m" + i,
                    "t" + i,
                    "界".repeat(MedalProtocol.MAX_NAME_LENGTH),
                    "界".repeat(MedalProtocol.MAX_DESCRIPTION_LENGTH),
                    "界".repeat(MedalProtocol.MAX_TEXTURE_LENGTH),
                    "界".repeat(MedalProtocol.MAX_COLOR_LENGTH),
                    i,
                    100
            ));
        }

        MedalClientSnapshot snapshot = new MedalClientSnapshot("rassvet", definitions, List.of());
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> MedalProtocol.encode(snapshot));
        assertTrue(exception.getMessage().contains("bytes UTF-8"));
    }
}
