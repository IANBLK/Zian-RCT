package com.ianblk.zianrct.config;

import com.ianblk.zianrct.network.MedalProtocol;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ZianRctConfigNetworkLimitsTest {
    @Test
    void rejectsProtocolFieldLimitsBeforeLoginCanEncodeThem() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile original = defaults.activeProfileConfig();
        List<ZianRctConfig.MedalDefinition> medals = new ArrayList<>(original.medals());
        ZianRctConfig.MedalDefinition first = medals.getFirst();
        medals.set(0, new ZianRctConfig.MedalDefinition(
                "x".repeat(MedalProtocol.MAX_ID_LENGTH + 1),
                first.trainer(),
                "r".repeat(MedalProtocol.MAX_TRAINER_NAME_LENGTH + 1),
                "n".repeat(MedalProtocol.MAX_NAME_LENGTH + 1),
                "d".repeat(MedalProtocol.MAX_DESCRIPTION_LENGTH + 1),
                "t".repeat(MedalProtocol.MAX_TEXTURE_LENGTH + 1),
                "c".repeat(MedalProtocol.MAX_COLOR_LENGTH + 1),
                first.order()
        ));

        ZianRctConfig invalid = withProfile(defaults, new ZianRctConfig.Profile(
                original.initialCap(), original.step(), original.maxCap(), original.series(),
                original.chain(), medals, original.giveMedalItem()
        ));

        List<String> errors = invalid.validate();
        assertTrue(errors.stream().anyMatch(message -> message.contains("id supera")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("trainerName supera")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("name supera")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("description supera")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("texture supera")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("color supera")));
    }

    @Test
    void rejectsMoreMedalsThanProtocolAllows() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile original = defaults.activeProfileConfig();
        List<ZianRctConfig.MedalDefinition> medals = new ArrayList<>();
        for (int i = 0; i <= MedalProtocol.MAX_MEDALS; i++) {
            medals.add(new ZianRctConfig.MedalDefinition(
                    "m" + i,
                    original.chain().get(i % original.chain().size()).trainer(),
                    null,
                    "Medalla " + i,
                    "Desc",
                    "",
                    "",
                    i
            ));
        }

        ZianRctConfig invalid = withProfile(defaults, new ZianRctConfig.Profile(
                original.initialCap(), original.step(), original.maxCap(), original.series(),
                original.chain(), medals, original.giveMedalItem()
        ));

        assertTrue(invalid.validate().stream().anyMatch(message -> message.contains("medals supera el máximo")));
    }

    @Test
    void defaultConfigProducesNetworkSafeSnapshot() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        assertTrue(defaults.validate().isEmpty());
    }

    private static ZianRctConfig withProfile(ZianRctConfig defaults, ZianRctConfig.Profile profile) {
        LinkedHashMap<String, ZianRctConfig.Profile> profiles = new LinkedHashMap<>();
        profiles.put(defaults.activeProfile(), profile);
        return new ZianRctConfig(
                defaults.schemaVersion(), defaults.activeProfile(), profiles, defaults.messages()
        );
    }
}
