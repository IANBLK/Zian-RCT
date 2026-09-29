package com.ianblk.zianrct.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ZianRctConfigLoader {
    public static final String FILE_NAME = "zianrct.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
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

        try (Reader reader = Files.newBufferedReader(configPath)) {
            ZianRctConfig loaded = GSON.fromJson(reader, ZianRctConfig.class);
            if (loaded == null) {
                throw new ConfigValidationException(java.util.List.of("El archivo de configuración está vacío"));
            }
            loaded.validateOrThrow();
            return loaded;
        } catch (JsonParseException exception) {
            throw new ConfigValidationException(java.util.List.of("JSON inválido: " + exception.getMessage()));
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
}
