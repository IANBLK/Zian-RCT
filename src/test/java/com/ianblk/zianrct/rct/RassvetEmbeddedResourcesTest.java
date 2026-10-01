package com.ianblk.zianrct.rct;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ianblk.zianrct.config.ZianRctConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RassvetEmbeddedResourcesTest {
    private static final Path ROOT = Path.of("src/main/resources/data/rctmod");
    private static final Path LANG_ROOT = Path.of("src/main/resources/assets/zianrct/lang");

    @Test
    void embeddedRassvetResourcesMatchDefaultProfile() throws IOException {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();
        List<String> ids = profile.chain().stream().map(ZianRctConfig.ChainEntry::trainer).toList();
        JsonObject enUs = parse(LANG_ROOT.resolve("en_us.json"));
        JsonObject esEs = parse(LANG_ROOT.resolve("es_es.json"));

        assertTrue(Files.isRegularFile(ROOT.resolve("series/rassvet.json")));
        assertTrue(Files.isRegularFile(ROOT.resolve("trainer_types/rassvet_gym.json")));
        parse(ROOT.resolve("series/rassvet.json"));
        parse(ROOT.resolve("trainer_types/rassvet_gym.json"));

        assertEquals(10, ids.size());
        for (int index = 0; index < ids.size(); index++) {
            String id = ids.get(index);
            Path mobPath = ROOT.resolve("mobs/trainers/single/" + id + ".json");
            Path teamPath = ROOT.resolve("trainers/" + id + ".json");
            Path dialogPath = ROOT.resolve("dialogs/trainers/single/" + id + ".json");

            JsonObject mob = parse(mobPath);
            JsonObject team = parse(teamPath);
            JsonObject dialog = parse(dialogPath);

            assertEquals("rassvet_gym", mob.get("type").getAsString(), id);
            assertTrue(mob.getAsJsonArray("series").asList().stream()
                    .anyMatch(element -> "rassvet".equals(element.getAsString())), id);

            JsonArray required = mob.getAsJsonArray("requiredDefeats");
            if (index == 0) {
                assertTrue(required.isEmpty(), id);
            } else {
                assertEquals(1, required.size(), id);
                assertEquals(ids.get(index - 1), required.get(0).getAsJsonArray().get(0).getAsString(), id);
            }

            assertFalse(team.getAsJsonArray("team").isEmpty(), id);
            assertTrue(dialog.has("battle_start"), id);
            assertTrue(dialog.has("on_battle_start"), id);
            assertTrue(dialog.has("on_battle_lost"), id);
            assertTrue(dialog.has("trainer_lost"), id);
            assertTrue(dialog.has("missing_beaten_trainer"), id);
            assertLocalizedDialog(dialog, enUs, esEs, id);

            assertFalse(containsNumericUnlockText(dialog.getAsJsonArray("on_battle_lost")), id);
            assertFalse(containsNumericUnlockText(dialog.getAsJsonArray("trainer_lost")), id);
        }

        JsonObject groupDialog = parse(ROOT.resolve("dialogs/trainers/groups/rassvet.json"));
        assertLocalizedDialog(groupDialog, enUs, esEs, "rassvet group");
    }

    private static JsonObject parse(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), path.toString());
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static void assertLocalizedDialog(JsonObject dialog, JsonObject enUs, JsonObject esEs, String id) {
        dialog.entrySet().forEach(entry -> {
            assertTrue(entry.getValue().isJsonArray(), id + ": " + entry.getKey());
            entry.getValue().getAsJsonArray().forEach(line -> {
                assertTrue(line.isJsonObject(), id + ": " + entry.getKey() + " must use RCT Text objects");
                JsonObject text = line.getAsJsonObject();
                assertTrue(text.has("literal") && !text.get("literal").getAsString().isBlank(), id + ": missing literal");
                assertTrue(text.has("translatable") && !text.get("translatable").getAsString().isBlank(), id + ": missing translatable");
                String key = text.get("translatable").getAsString();
                assertTrue(enUs.has(key), id + ": missing en_us key " + key);
                assertTrue(esEs.has(key), id + ": missing es_es key " + key);
            });
        });
    }

    private static boolean containsNumericUnlockText(JsonArray lines) {
        return lines.asList().stream()
                .map(RassvetEmbeddedResourcesTest::literalText)
                .filter(text -> text != null)
                .anyMatch(text -> text.matches(".*(?i:nivel)\\s+\\d+.*"));
    }

    private static String literalText(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return element.getAsString();
        }
        if (element.isJsonObject() && element.getAsJsonObject().has("literal")) {
            return element.getAsJsonObject().get("literal").getAsString();
        }
        return null;
    }
}
