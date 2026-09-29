package com.ianblk.zianrct.rct;

import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.rct.pack.RctPackJsonBuilder;
import com.ianblk.zianrct.rct.pack.SeriesPackSnapshot;
import com.ianblk.zianrct.rct.pack.TrainerPackSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase3IntegrationTest {
    @Test
    void packBuilderCreatesLinearRassvetProgressionInStableOrder() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();
        LinkedHashMap<String, TrainerPackSnapshot> snapshots = trainerSnapshots(profile);

        Map<String, byte[]> resources = RctPackJsonBuilder.build(profile, seriesSnapshot(), snapshots);

        assertEquals(11, resources.size());
        assertEquals("series/rassvet.json", resources.keySet().iterator().next());

        String series = json(resources, "series/rassvet.json");
        String novato = json(resources, "mobs/trainers/single/rassvet_leader_novato.json");
        String ferrum = json(resources, "mobs/trainers/single/rassvet_leader_ferrum.json");
        assertTrue(series.contains("rassvet.title"));
        assertTrue(series.contains("Rassvet League"));
        assertTrue(series.contains("\"difficulty\": 8"));
        assertTrue(series.contains("\"initialLevelCap\": 10"));
        assertTrue(series.contains("previous_series"));
        assertTrue(series.contains("\"customFutureField\": \"preserved\""));
        assertFalse(series.contains("relativeLevelCap"));
        assertTrue(novato.contains("\"requiredDefeats\": []"));
        assertTrue(novato.contains("\"relativeLevelCap\": 0"));
        assertTrue(ferrum.contains("rassvet_leader_novato"));
        assertTrue(ferrum.contains("\"relativeLevelCap\": 0"));
    }

    @Test
    void explicitConfiguredCapChangesGeneratedRelativeCap() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile base = defaults.activeProfileConfig();
        List<ZianRctConfig.ChainEntry> chain = new java.util.ArrayList<>(base.chain());
        chain.set(0, new ZianRctConfig.ChainEntry("rassvet_leader_novato", 25));
        ZianRctConfig.Profile profile = new ZianRctConfig.Profile(
                base.initialCap(), base.step(), base.maxCap(), base.series(),
                chain, base.medals(), base.giveMedalItem()
        );

        String ferrum = json(
                RctPackJsonBuilder.build(profile, seriesSnapshot(), trainerSnapshots(profile)),
                "mobs/trainers/single/rassvet_leader_ferrum.json"
        );
        assertTrue(ferrum.contains("\"relativeLevelCap\": 5"));
    }

    @Test
    void messagePlaceholdersRejectUnknownVariables() {
        ZianRctConfig.Messages invalid = new ZianRctConfig.Messages(
                "Nivel {cap} para {player}",
                "Medalla {medal}",
                "Recargado {cap}",
                "Cap {cap}"
        );

        List<String> errors = MessagePlaceholderValidator.validate(invalid);
        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(error -> error.contains("{player}")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("reloadSuccess")));
    }

    @Test
    void adminCapPlannerUsesMinimalPrefixAndRejectsUnreachableCap() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();

        assertEquals(List.of(), RctProgressPlanner.desiredDefeatedPrefix(profile, 10));
        assertEquals(
                List.of("rassvet_leader_novato", "rassvet_leader_ferrum"),
                RctProgressPlanner.desiredDefeatedPrefix(profile, 30)
        );
        assertEquals(9, RctProgressPlanner.desiredDefeatedPrefix(profile, 100).size());
        assertThrows(
                IllegalArgumentException.class,
                () -> RctProgressPlanner.desiredDefeatedPrefix(profile, 35)
        );
    }

    private static LinkedHashMap<String, TrainerPackSnapshot> trainerSnapshots(
            ZianRctConfig.Profile profile
    ) {
        LinkedHashMap<String, TrainerPackSnapshot> snapshots = new LinkedHashMap<>();
        int level = 10;
        for (ZianRctConfig.ChainEntry entry : profile.chain()) {
            snapshots.put(entry.trainer(), trainerSnapshot(entry.trainer(), level));
            level += 10;
        }
        return snapshots;
    }

    private static SeriesPackSnapshot seriesSnapshot() {
        return new SeriesPackSnapshot("""
                {
                  "title": {"literal": "Rassvet League", "translatable": "rassvet.title"},
                  "description": {"literal": "Original description", "translatable": "rassvet.description"},
                  "difficulty": 8,
                  "relativeLevelCap": 5,
                  "hideTrainerIdentities": true,
                  "requiredSeries": [["previous_series"]],
                  "customFutureField": "preserved"
                }
                """);
    }

    private static TrainerPackSnapshot trainerSnapshot(String id, int level) {
        return new TrainerPackSnapshot(
                id,
                "leader",
                "",
                List.of(),
                Set.of(),
                3,
                1,
                240,
                1.0F,
                Set.of("is_void"),
                Set.of("is_overworld"),
                null,
                null,
                null,
                null,
                level
        );
    }

    private static String json(Map<String, byte[]> resources, String path) {
        return new String(resources.get(path), StandardCharsets.UTF_8);
    }
}
