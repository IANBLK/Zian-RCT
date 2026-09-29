package com.ianblk.zianrct.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZianRctConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsAreValidAndUseRassvetProfile() {
        ZianRctConfig config = ZianRctConfig.defaults();

        assertTrue(config.validate().isEmpty());
        assertEquals("rassvet", config.activeProfile());
        assertEquals(10, config.activeProfileConfig().initialCap());
        assertEquals(100, config.activeProfileConfig().maxCap());
        assertEquals(10, config.activeProfileConfig().chain().size());
        assertEquals(10, config.activeProfileConfig().medals().size());
    }

    @Test
    void implicitUnlockCapsUseStepAndClampAtMaxCap() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();

        assertEquals(20, profile.unlockCap(0));
        assertEquals(90, profile.unlockCap(7));
        assertEquals(100, profile.unlockCap(8));
        assertEquals(100, profile.unlockCap(9));
    }

    @Test
    void invalidActiveProfileIsRejected() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig invalid = new ZianRctConfig(
                defaults.schemaVersion(),
                "missing",
                defaults.profiles(),
                defaults.messages()
        );

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, invalid::validateOrThrow);
        assertTrue(exception.errors().stream().anyMatch(message -> message.contains("activeProfile")));
    }

    @Test
    void duplicateMedalIdsAreRejected() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile original = defaults.activeProfileConfig();
        List<ZianRctConfig.MedalDefinition> medals = List.of(
                new ZianRctConfig.MedalDefinition("same", "rassvet_leader_novato", "Uno", "Desc", null, null, 0),
                new ZianRctConfig.MedalDefinition("same", "rassvet_leader_ferrum", "Dos", "Desc", null, null, 1)
        );
        ZianRctConfig.Profile changed = new ZianRctConfig.Profile(
                original.initialCap(),
                original.step(),
                original.maxCap(),
                original.series(),
                original.chain(),
                medals,
                original.giveMedalItem()
        );
        ZianRctConfig invalid = new ZianRctConfig(
                defaults.schemaVersion(),
                defaults.activeProfile(),
                Map.of("rassvet", changed),
                defaults.messages()
        );

        assertFalse(invalid.validate().isEmpty());
        assertTrue(invalid.validate().stream().anyMatch(message -> message.contains("duplicado")));
    }

    @Test
    void loaderCreatesAndReadsConfigWithoutMinecraftRuntime() throws IOException {
        Path configPath = tempDir.resolve("config").resolve(ZianRctConfigLoader.FILE_NAME);

        ZianRctConfig created = ZianRctConfigLoader.loadOrCreate(configPath);
        ZianRctConfig loaded = ZianRctConfigLoader.loadOrCreate(configPath);

        assertTrue(Files.exists(configPath));
        assertEquals(created, loaded);
        assertTrue(loaded.validate().isEmpty());
    }

    @Test
    void malformedJsonIsRejectedInsteadOfSilentlyUsingDefaults() throws IOException {
        Path configPath = tempDir.resolve(ZianRctConfigLoader.FILE_NAME);
        Files.writeString(configPath, "{ not-valid-json }");

        assertThrows(ConfigValidationException.class, () -> ZianRctConfigLoader.loadOrCreate(configPath));
    }
}
