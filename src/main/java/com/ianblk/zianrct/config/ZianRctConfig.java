package com.ianblk.zianrct.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ianblk.zianrct.ZianRCT;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public record ZianRctConfig(int initialCap, int step, int maxCap, String series) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "zianrct.json";

    public static ZianRctConfig defaults() {
        return new ZianRctConfig(10, 10, 100, "rassvet");
    }

    public static ZianRctConfig load() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);

        try {
            Files.createDirectories(configPath.getParent());
            if (Files.notExists(configPath)) {
                ZianRctConfig defaults = defaults();
                try (Writer writer = Files.newBufferedWriter(configPath)) {
                    GSON.toJson(defaults, writer);
                }
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(configPath)) {
                ZianRctConfig loaded = GSON.fromJson(reader, ZianRctConfig.class);
                if (loaded == null) {
                    throw new IOException("Configuration file is empty");
                }
                return loaded;
            }
        } catch (Exception exception) {
            ZianRCT.LOGGER.error("Failed to load {}. Falling back to phase-1 defaults.", configPath, exception);
            return defaults();
        }
    }
}
