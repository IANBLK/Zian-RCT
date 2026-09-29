package com.ianblk.zianrct.rct.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ianblk.zianrct.config.ZianRctConfig;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RctPackJsonBuilder {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private RctPackJsonBuilder() {
    }

    public static Map<String, byte[]> build(
            ZianRctConfig.Profile profile,
            SeriesPackSnapshot seriesSnapshot,
            Map<String, TrainerPackSnapshot> snapshots
    ) {
        LinkedHashMap<String, byte[]> resources = new LinkedHashMap<>();
        resources.put("series/" + profile.series() + ".json", bytes(seriesJson(profile, seriesSnapshot)));

        List<ZianRctConfig.ChainEntry> chain = profile.chain();
        for (int index = 0; index < chain.size(); index++) {
            ZianRctConfig.ChainEntry entry = chain.get(index);
            TrainerPackSnapshot snapshot = snapshots.get(entry.trainer());
            if (snapshot == null) {
                throw new IllegalArgumentException("Missing trainer snapshot: " + entry.trainer());
            }
            int requiredCap = index == 0 ? profile.initialCap() : profile.unlockCap(index - 1);
            resources.put(
                    "mobs/trainers/single/" + entry.trainer() + ".json",
                    bytes(trainerJson(profile, snapshot, index, requiredCap))
            );
        }
        return java.util.Collections.unmodifiableMap(resources);
    }

    private static JsonObject seriesJson(ZianRctConfig.Profile profile, SeriesPackSnapshot snapshot) {
        JsonObject json = object(snapshot.sourceJson(), "RCT series source JSON").deepCopy();
        json.remove("relativeLevelCap");
        json.addProperty("initialLevelCap", profile.initialCap());
        return json;
    }

    private static JsonObject trainerJson(
            ZianRctConfig.Profile profile,
            TrainerPackSnapshot snapshot,
            int index,
            int requiredCap
    ) {
        JsonObject json = object(snapshot.sourceJson(), "RCT trainer source JSON for " + snapshot.trainerId()).deepCopy();

        JsonArray requiredDefeats = new JsonArray();
        if (index > 0) {
            JsonArray alternatives = new JsonArray();
            alternatives.add(profile.chain().get(index - 1).trainer());
            requiredDefeats.add(alternatives);
        }
        json.add("requiredDefeats", requiredDefeats);
        json.addProperty("relativeLevelCap", requiredCap - snapshot.maxTeamLevel());
        return json;
    }

    private static JsonObject object(String source, String label) {
        var parsed = JsonParser.parseString(source);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException(label + " must be an object");
        }
        return parsed.getAsJsonObject();
    }

    private static byte[] bytes(JsonObject json) {
        return GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
    }
}
