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
import java.util.Set;

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
        resources.put(
                "series/" + profile.series() + ".json",
                bytes(seriesJson(profile, seriesSnapshot))
        );

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

    private static JsonObject seriesJson(
            ZianRctConfig.Profile profile,
            SeriesPackSnapshot snapshot
    ) {
        var parsed = JsonParser.parseString(snapshot.sourceJson());
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("RCT series source JSON must be an object");
        }
        JsonObject json = parsed.getAsJsonObject().deepCopy();
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
        JsonObject json = new JsonObject();
        json.addProperty("type", snapshot.type());
        if (snapshot.signatureItem() != null && !snapshot.signatureItem().isBlank()) {
            json.addProperty("signatureItem", snapshot.signatureItem());
        }

        JsonArray requiredDefeats = new JsonArray();
        if (index > 0) {
            JsonArray alternatives = new JsonArray();
            alternatives.add(profile.chain().get(index - 1).trainer());
            requiredDefeats.add(alternatives);
        }
        json.add("requiredDefeats", requiredDefeats);
        json.add("requiredSeries", nestedStringSets(snapshot.requiredSeries()));

        JsonArray series = new JsonArray();
        series.add(profile.series());
        json.add("series", series);
        json.add("substitutes", stringSet(snapshot.substitutes()));
        json.addProperty("optional", false);
        json.addProperty("maxTrainerWins", snapshot.maxTrainerWins());
        json.addProperty("maxTrainerDefeats", snapshot.maxTrainerDefeats());
        json.addProperty("battleCooldownTicks", snapshot.battleCooldownTicks());
        json.addProperty("relativeLevelCap", requiredCap - snapshot.maxTeamLevel());
        json.addProperty("spawnWeightFactor", snapshot.spawnWeightFactor());
        json.add("biomeTagBlacklist", stringSet(snapshot.biomeTagBlacklist()));
        json.add("biomeTagWhitelist", stringSet(snapshot.biomeTagWhitelist()));

        addOptional(json, "forceBattleOnSight", snapshot.forceBattleOnSight());
        addOptional(json, "forceBattleMaxDistance", snapshot.forceBattleMaxDistance());
        addOptional(json, "forceBattleLookTicks", snapshot.forceBattleLookTicks());
        addOptional(json, "forceBattleMaxLevelDiff", snapshot.forceBattleMaxLevelDiff());
        return json;
    }

    private static JsonArray nestedStringSets(List<Set<String>> values) {
        JsonArray outer = new JsonArray();
        values.forEach(set -> outer.add(stringSet(set)));
        return outer;
    }

    private static JsonArray stringSet(Set<String> values) {
        JsonArray array = new JsonArray();
        values.stream().sorted().forEach(array::add);
        return array;
    }

    private static void addOptional(JsonObject json, String key, Object value) {
        if (value instanceof Boolean bool) {
            json.addProperty(key, bool);
        } else if (value instanceof Number number) {
            json.addProperty(key, number);
        }
    }

    private static byte[] bytes(JsonObject json) {
        return GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
    }
}
