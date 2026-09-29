package com.ianblk.zianrct.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ZianRctConfigLoader {
    public static final String FILE_NAME = "zianrct.json";

    private static final Set<String> ROOT_KEYS = Set.of(
            "schemaVersion", "activeProfile", "profiles", "messages"
    );
    private static final Set<String> PROFILE_KEYS = Set.of(
            "initialCap", "step", "maxCap", "series", "chain", "medals", "giveMedalItem"
    );
    private static final Set<String> CHAIN_KEYS = Set.of("trainer", "unlockCap");
    private static final Set<String> MEDAL_KEYS = Set.of(
            "id", "trainer", "name", "description", "texture", "color", "order"
    );
    private static final Set<String> MESSAGE_KEYS = Set.of(
            "capUnlocked", "medalObtained", "reloadSuccess", "currentCap"
    );

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private ZianRctConfigLoader() {
    }

    public static ZianRctConfig loadOrCreate(Path configPath) throws IOException {
        if (Files.notExists(configPath)) {
            ZianRctConfig defaults = ZianRctConfig.defaults();
            defaults.validateOrThrow();
            write(configPath, defaults);
            return defaults;
        }
        return readExisting(configPath);
    }

    public static ZianRctConfig readExisting(Path configPath) throws IOException {
        if (Files.notExists(configPath)) {
            throw new IOException("El archivo de configuración no existe: " + configPath);
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || element.isJsonNull()) {
                throw new ConfigValidationException(List.of("El archivo de configuración está vacío"));
            }
            if (!element.isJsonObject()) {
                throw new ConfigValidationException(List.of("La raíz del JSON debe ser un objeto"));
            }

            List<String> unknownKeys = findUnknownKeys(element.getAsJsonObject());
            if (!unknownKeys.isEmpty()) {
                throw new ConfigValidationException(unknownKeys);
            }

            ZianRctConfig loaded = GSON.fromJson(element, ZianRctConfig.class);
            if (loaded == null) {
                throw new ConfigValidationException(List.of("El archivo de configuración está vacío"));
            }
            loaded.validateOrThrow();
            return loaded;
        } catch (JsonParseException exception) {
            throw new ConfigValidationException(List.of("JSON inválido: " + exception.getMessage()));
        }
    }

    public static void write(Path configPath, ZianRctConfig config) throws IOException {
        config.validateOrThrow();

        Path parent = configPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temporary = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary)) {
            GSON.toJson(config, writer);
        }

        try {
            Files.move(
                    temporary,
                    configPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<String> findUnknownKeys(JsonObject root) {
        List<String> errors = new ArrayList<>();
        checkKeys(root, ROOT_KEYS, "$", errors);

        JsonElement profilesElement = root.get("profiles");
        if (profilesElement != null && profilesElement.isJsonObject()) {
            for (var entry : profilesElement.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                String profilePath = "$.profiles." + entry.getKey();
                JsonObject profile = entry.getValue().getAsJsonObject();
                checkKeys(profile, PROFILE_KEYS, profilePath, errors);

                JsonElement chainElement = profile.get("chain");
                if (chainElement != null && chainElement.isJsonArray()) {
                    JsonArray chain = chainElement.getAsJsonArray();
                    for (int index = 0; index < chain.size(); index++) {
                        JsonElement item = chain.get(index);
                        if (item != null && item.isJsonObject()) {
                            checkKeys(item.getAsJsonObject(), CHAIN_KEYS,
                                    profilePath + ".chain[" + index + "]", errors);
                        }
                    }
                }

                JsonElement medalsElement = profile.get("medals");
                if (medalsElement != null && medalsElement.isJsonArray()) {
                    JsonArray medals = medalsElement.getAsJsonArray();
                    for (int index = 0; index < medals.size(); index++) {
                        JsonElement item = medals.get(index);
                        if (item != null && item.isJsonObject()) {
                            checkKeys(item.getAsJsonObject(), MEDAL_KEYS,
                                    profilePath + ".medals[" + index + "]", errors);
                        }
                    }
                }
            }
        }

        JsonElement messagesElement = root.get("messages");
        if (messagesElement != null && messagesElement.isJsonObject()) {
            checkKeys(messagesElement.getAsJsonObject(), MESSAGE_KEYS, "$.messages", errors);
        }

        return List.copyOf(errors);
    }

    private static void checkKeys(
            JsonObject object,
            Set<String> allowed,
            String path,
            List<String> errors
    ) {
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) {
                errors.add("Clave desconocida en " + path + ": " + key);
            }
        }
    }
}
