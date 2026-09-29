package com.ianblk.zianrct.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    void stableTrainerOrderIsPreserved() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();

        assertEquals(
                profile.chain().stream().map(ZianRctConfig.ChainEntry::trainer).toList(),
                new ArrayList<>(profile.trainerUnlockCaps().keySet())
        );
    }

    @Test
    void explicitUnlockCapSurvivesJsonRoundTrip() throws IOException {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile original = defaults.activeProfileConfig();
        List<ZianRctConfig.ChainEntry> chain = new ArrayList<>(original.chain());
        chain.set(0, new ZianRctConfig.ChainEntry("rassvet_leader_novato", 25));

        ZianRctConfig.Profile changed = new ZianRctConfig.Profile(
                original.initialCap(), original.step(), original.maxCap(), original.series(),
                chain, original.medals(), original.giveMedalItem()
        );
        LinkedHashMap<String, ZianRctConfig.Profile> profiles = new LinkedHashMap<>();
        profiles.put("rassvet", changed);
        ZianRctConfig config = new ZianRctConfig(
                defaults.schemaVersion(), defaults.activeProfile(), profiles, defaults.messages()
        );

        Path path = tempDir.resolve(ZianRctConfigLoader.FILE_NAME);
        ZianRctConfigLoader.write(path, config);
        ZianRctConfig loaded = ZianRctConfigLoader.readExisting(path);

        assertEquals(25, loaded.activeProfileConfig().unlockCap(0));
        assertEquals(config, loaded);
    }

    @Test
    void decreasingUnlockCapIsRejected() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile original = defaults.activeProfileConfig();
        List<ZianRctConfig.ChainEntry> chain = new ArrayList<>(original.chain());
        chain.set(0, new ZianRctConfig.ChainEntry("rassvet_leader_novato", 30));
        chain.set(1, new ZianRctConfig.ChainEntry("rassvet_leader_ferrum", 20));

        ZianRctConfig invalid = withProfile(defaults, new ZianRctConfig.Profile(
                original.initialCap(), original.step(), original.maxCap(), original.series(),
                chain, original.medals(), original.giveMedalItem()
        ));

        assertTrue(invalid.validate().stream().anyMatch(message -> message.contains("no puede disminuir")));
    }

    @Test
    void invalidTrainerAndMedalIdsAreRejected() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile original = defaults.activeProfileConfig();
        List<ZianRctConfig.ChainEntry> chain = new ArrayList<>(original.chain());
        chain.set(0, new ZianRctConfig.ChainEntry("Líder Malo", null));
        List<ZianRctConfig.MedalDefinition> medals = new ArrayList<>(original.medals());
        medals.set(0, new ZianRctConfig.MedalDefinition(
                "Medalla Mala", "Líder Malo", "Mala", "Desc", null, null, 0
        ));

        ZianRctConfig invalid = withProfile(defaults, new ZianRctConfig.Profile(
                original.initialCap(), original.step(), original.maxCap(), original.series(),
                chain, medals, original.giveMedalItem()
        ));

        long invalidIdErrors = invalid.validate().stream()
                .filter(message -> message.contains("caracteres inválidos"))
                .count();
        assertTrue(invalidIdErrors >= 3);
    }

    @Test
    void nullElementsReachValidationInsteadOfCopyConstructorFailure() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile original = defaults.activeProfileConfig();
        List<ZianRctConfig.ChainEntry> chain = new ArrayList<>(original.chain());
        chain.set(0, null);
        List<ZianRctConfig.MedalDefinition> medals = new ArrayList<>(original.medals());
        medals.set(0, null);

        ZianRctConfig.Profile profile = new ZianRctConfig.Profile(
                original.initialCap(), original.step(), original.maxCap(), original.series(),
                chain, medals, original.giveMedalItem()
        );
        LinkedHashMap<String, ZianRctConfig.Profile> profiles = new LinkedHashMap<>();
        profiles.put("rassvet", profile);
        profiles.put("null_profile", null);
        ZianRctConfig invalid = new ZianRctConfig(
                defaults.schemaVersion(), defaults.activeProfile(), profiles, defaults.messages()
        );

        List<String> errors = invalid.validate();
        assertTrue(errors.stream().anyMatch(message -> message.contains("chain[0] no puede ser null")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("medals[0] no puede ser null")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("null_profile no puede ser null")));
    }

    @Test
    void invalidActiveProfileIsRejected() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig invalid = new ZianRctConfig(
                defaults.schemaVersion(), "missing", defaults.profiles(), defaults.messages()
        );

        ConfigValidationException exception = assertThrows(
                ConfigValidationException.class, invalid::validateOrThrow
        );
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
                original.initialCap(), original.step(), original.maxCap(), original.series(),
                original.chain(), medals, original.giveMedalItem()
        );
        ZianRctConfig invalid = withProfile(defaults, changed);

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

        assertThrows(ConfigValidationException.class, () -> ZianRctConfigLoader.readExisting(configPath));
    }

    @Test
    void unknownJsonKeysAreRejected() throws IOException {
        Path configPath = tempDir.resolve(ZianRctConfigLoader.FILE_NAME);
        ZianRctConfigLoader.write(configPath, ZianRctConfig.defaults());
        String json = Files.readString(configPath).replace("\"giveMedalItem\"", "\"giveMedalItm\"");
        Files.writeString(configPath, json);

        ConfigValidationException exception = assertThrows(
                ConfigValidationException.class,
                () -> ZianRctConfigLoader.readExisting(configPath)
        );
        assertTrue(exception.errors().stream().anyMatch(message -> message.contains("giveMedalItm")));
    }

    @Test
    void jsonWriterDoesNotHtmlEscapeAdminText() throws IOException {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Messages messages = new ZianRctConfig.Messages(
                "Cap <{cap}> & listo", defaults.messages().medalObtained(),
                defaults.messages().reloadSuccess(), defaults.messages().currentCap()
        );
        ZianRctConfig config = new ZianRctConfig(
                defaults.schemaVersion(), defaults.activeProfile(), defaults.profiles(), messages
        );
        Path path = tempDir.resolve(ZianRctConfigLoader.FILE_NAME);

        ZianRctConfigLoader.write(path, config);

        String json = Files.readString(path);
        assertTrue(json.contains("Cap <{cap}> & listo"));
        assertFalse(json.contains("\\u003c"));
        assertFalse(json.contains("\\u0026"));
    }

    private static ZianRctConfig withProfile(
            ZianRctConfig defaults,
            ZianRctConfig.Profile profile
    ) {
        LinkedHashMap<String, ZianRctConfig.Profile> profiles = new LinkedHashMap<>();
        profiles.put("rassvet", profile);
        return new ZianRctConfig(
                defaults.schemaVersion(), defaults.activeProfile(), profiles, defaults.messages()
        );
    }
}
