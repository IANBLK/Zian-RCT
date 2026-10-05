package com.ianblk.zianrct.reward;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class TrainerRewardConfig {
    private final Path path;
    private Model model;
    private TrainerRewardConfig(Path path, Model model) { this.path = path; this.model = model; }
    public static TrainerRewardConfig open(Path path) throws IOException {
        if (!Files.exists(path)) {
            Model initial = new Model(1, true, Map.of());
            RewardFiles.write(path, initial);
            return new TrainerRewardConfig(path, initial);
        }
        return new TrainerRewardConfig(path, parse(RewardFiles.read(path, 1048576)));
    }
    static Model parse(JsonElement root) throws IOException {
        try {
            var json = root.getAsJsonObject();
            requireKeys(json, Set.of("schemaVersion", "enabled", "trainers"));
            if (json.get("schemaVersion").getAsBigDecimal().intValueExact() != 1) throw new IllegalArgumentException("Versión no soportada");
            if (!json.get("enabled").isJsonPrimitive() || !json.get("enabled").getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("enabled debe ser booleano");
            var trainers = json.getAsJsonObject("trainers");
            if (trainers.size() > 256) throw new IllegalArgumentException("Máximo 256 entrenadores");
            Map<String, RewardDefinition> definitions = new LinkedHashMap<>();
            for (var entry : trainers.entrySet()) {
                validId(entry.getKey());
                var definition = entry.getValue().getAsJsonObject();
                requireKeys(definition, Set.of("currency", "coins", "items"));
                List<String> items = new ArrayList<>();
                for (var item : definition.getAsJsonArray("items")) {
                    if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Objeto inválido");
                    items.add(item.getAsString());
                }
                definitions.put(entry.getKey(), new RewardDefinition(definition.get("currency").getAsString(),
                        definition.get("coins").getAsBigDecimal().longValueExact(), items));
            }
            return new Model(1, json.get("enabled").getAsBoolean(), Collections.unmodifiableMap(definitions));
        } catch (RuntimeException error) { throw new IOException("Configuración de recompensas inválida", error); }
    }
    private static void requireKeys(JsonObject object, Set<String> keys) {
        if (!object.keySet().equals(keys)) throw new IllegalArgumentException("Campos incompletos o desconocidos");
    }
    static void validId(String id) {
        if (id == null || id.length() > 128 || !id.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("ID de entrenador inválido");
    }
    public synchronized boolean enabled() { return model.enabled(); }
    public synchronized Map<String, RewardDefinition> definitions() { return Map.copyOf(model.trainers()); }
    public synchronized RewardDefinition definition(String trainer) { return model.trainers().get(trainer); }
    public synchronized void set(String trainer, RewardDefinition definition) throws IOException {
        validId(trainer);
        Map<String, RewardDefinition> next = new LinkedHashMap<>(model.trainers());
        if (definition == null) next.remove(trainer); else next.put(trainer, definition);
        if (next.size() > 256) throw new IOException("Máximo 256 entrenadores");
        Model candidate = new Model(1, model.enabled(), Collections.unmodifiableMap(next));
        save(candidate); model = candidate;
    }
    public synchronized void enabled(boolean enabled) throws IOException {
        Model candidate = new Model(1, enabled, model.trainers());
        save(candidate); model = candidate;
    }
    private void save(Model candidate) throws IOException {
        if (RewardFiles.GSON.toJson(candidate).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1048576)
            throw new IOException("La configuración supera 1 MiB; se conserva la anterior");
        RewardFiles.write(path, candidate);
    }
    public synchronized void reload() throws IOException { model = parse(RewardFiles.read(path, 1048576)); }
    record Model(int schemaVersion, boolean enabled, Map<String, RewardDefinition> trainers) {}
}
