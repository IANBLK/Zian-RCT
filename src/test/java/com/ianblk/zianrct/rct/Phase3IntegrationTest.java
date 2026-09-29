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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase3IntegrationTest {
    @Test
    void packBuilderCreatesLinearRassvetProgressionInStableOrder() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();
        Map<String, byte[]> resources = RctPackJsonBuilder.build(profile, seriesSnapshot(), trainerSnapshots(profile));

        assertEquals(11, resources.size());
        assertEquals("series/rassvet.json", resources.keySet().iterator().next());
        String series = json(resources, "series/rassvet.json");
        String novato = json(resources, "mobs/trainers/single/rassvet_leader_novato.json");
        String ferrum = json(resources, "mobs/trainers/single/rassvet_leader_ferrum.json");
        assertTrue(series.contains("Rassvet League"));
        assertTrue(series.contains("\"initialLevelCap\": 10"));
        assertTrue(series.contains("\"customFutureField\": \"preserved\""));
        assertFalse(series.contains("relativeLevelCap"));
        assertTrue(novato.contains("\"requiredDefeats\": []"));
        assertTrue(novato.contains("\"relativeLevelCap\": 0"));
        assertTrue(ferrum.contains("rassvet_leader_novato"));
        assertTrue(ferrum.contains("\"relativeLevelCap\": 0"));
    }

    @Test
    void trainerCopyPreservesCommandsFollowdByOptionalAndUnknownFields() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();
        LinkedHashMap<String, TrainerPackSnapshot> snapshots = trainerSnapshots(profile);
        String first = profile.chain().getFirst().trainer();
        snapshots.put(first, new TrainerPackSnapshot(first, """
                {
                  "series": ["rassvet"],
                  "type": "rassvet_gym",
                  "requiredDefeats": [["old_requirement"]],
                  "relativeLevelCap": 99,
                  "optional": true,
                  "followdBy": ["future_trainer"],
                  "winCommands": {"0": ["say preserved"]},
                  "futureField": {"nested": 42}
                }
                """, 10));

        String generated = json(
                RctPackJsonBuilder.build(profile, seriesSnapshot(), snapshots),
                "mobs/trainers/single/" + first + ".json"
        );

        assertTrue(generated.contains("\"optional\": true"));
        assertTrue(generated.contains("\"followdBy\""));
        assertTrue(generated.contains("future_trainer"));
        assertTrue(generated.contains("\"winCommands\""));
        assertTrue(generated.contains("say preserved"));
        assertTrue(generated.contains("\"futureField\""));
        assertTrue(generated.contains("\"nested\": 42"));
        assertTrue(generated.contains("\"requiredDefeats\": []"));
        assertFalse(generated.contains("old_requirement"));
        assertTrue(generated.contains("\"relativeLevelCap\": 0"));
        assertFalse(generated.contains("\"relativeLevelCap\": 99"));
    }

    @Test
    void explicitConfiguredCapChangesGeneratedRelativeCap() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile base = defaults.activeProfileConfig();
        List<ZianRctConfig.ChainEntry> chain = new java.util.ArrayList<>(base.chain());
        chain.set(0, new ZianRctConfig.ChainEntry("rassvet_leader_novato", 25));
        ZianRctConfig.Profile profile = new ZianRctConfig.Profile(
                base.initialCap(), base.step(), base.maxCap(), base.series(), chain, base.medals(), base.giveMedalItem()
        );
        String ferrum = json(
                RctPackJsonBuilder.build(profile, seriesSnapshot(), trainerSnapshots(profile)),
                "mobs/trainers/single/rassvet_leader_ferrum.json"
        );
        assertTrue(ferrum.contains("\"relativeLevelCap\": 5"));
    }

    @Test
    void messagePlaceholdersAreRejectedByConfigValidation() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig invalid = new ZianRctConfig(
                defaults.schemaVersion(), defaults.activeProfile(), defaults.profiles(),
                new ZianRctConfig.Messages(
                        "Nivel {cap} para {player}",
                        "Medalla {medal}",
                        "Recargado {cap}",
                        "Cap {cap}"
                )
        );
        List<String> errors = invalid.validate();
        assertEquals(2, errors.stream().filter(error -> error.contains("marcador no permitido")).count());
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
        assertThrows(IllegalArgumentException.class, () -> RctProgressPlanner.desiredDefeatedPrefix(profile, 35));
    }

    private static LinkedHashMap<String, TrainerPackSnapshot> trainerSnapshots(ZianRctConfig.Profile profile) {
        LinkedHashMap<String, TrainerPackSnapshot> snapshots = new LinkedHashMap<>();
        int level = 10;
        for (ZianRctConfig.ChainEntry entry : profile.chain()) {
            snapshots.put(entry.trainer(), new TrainerPackSnapshot(entry.trainer(), trainerSource(), level));
            level += 10;
        }
        return snapshots;
    }

    private static SeriesPackSnapshot seriesSnapshot() {
        return new SeriesPackSnapshot("""
                {
                  "title": {"literal": "Rassvet League"},
                  "difficulty": 8,
                  "relativeLevelCap": 5,
                  "customFutureField": "preserved"
                }
                """);
    }

    private static String trainerSource() {
        return """
                {
                  "series": ["rassvet"],
                  "type": "rassvet_gym",
                  "requiredDefeats": [],
                  "maxTrainerWins": -1,
                  "maxTrainerDefeats": 1,
                  "battleCooldownTicks": 240,
                  "spawnWeightFactor": 0
                }
                """;
    }

    private static String json(Map<String, byte[]> resources, String path) {
        return new String(resources.get(path), StandardCharsets.UTF_8);
    }
}
