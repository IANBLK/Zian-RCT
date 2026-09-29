package com.ianblk.zianrct.rct;

import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.api.data.pack.TrainerMobData;
import com.gitlab.srcmc.rctmod.api.service.TrainerManager;
import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.rct.pack.RctPackJsonBuilder;
import com.ianblk.zianrct.rct.pack.SeriesPackSnapshot;
import com.ianblk.zianrct.rct.pack.TrainerPackSnapshot;
import com.ianblk.zianrct.rct.pack.ZianRctPackSource;
import com.ianblk.zianrct.rct.pack.ZianRctVirtualPack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class RctPackController {
    private final ZianRctVirtualPack pack = new ZianRctVirtualPack();

    public RctPackController(IEventBus modEventBus) {
        modEventBus.addListener(this::onAddPackFinders);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            event.addRepositorySource(new ZianRctPackSource(pack));
            ZianRCT.LOGGER.info("Registered Zian RCT virtual SERVER_DATA pack at TOP priority.");
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ZianRctConfig config = ConfigState.current();
        List<String> messageErrors = MessagePlaceholderValidator.validate(config.messages());
        if (!messageErrors.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid Zian RCT message placeholders: " + String.join("; ", messageErrors)
            );
        }

        ZianRctConfig.Profile profile = config.activeProfileConfig();
        RCTMod rct = RCTMod.getInstance();
        TrainerManager trainerManager = rct.getTrainerManager();

        if (!rct.getSeriesManager().getSeriesIds().contains(profile.series())) {
            pack.clear();
            ZianRCT.LOGGER.warn(
                    "Zian RCT progression pack remains inactive because configured RCT series '{}' is not loaded.",
                    profile.series()
            );
            return;
        }

        List<String> missing = profile.chain().stream()
                .map(ZianRctConfig.ChainEntry::trainer)
                .filter(id -> !trainerManager.isValidId(id))
                .toList();

        if (!missing.isEmpty()) {
            pack.clear();
            ZianRCT.LOGGER.warn(
                    "Zian RCT progression pack remains inactive because configured RCT trainers are not loaded: {}",
                    String.join(", ", missing)
            );
            return;
        }

        SeriesPackSnapshot seriesSnapshot = snapshotSeries(server, profile.series());
        LinkedHashMap<String, TrainerPackSnapshot> snapshots = new LinkedHashMap<>();
        for (ZianRctConfig.ChainEntry entry : profile.chain()) {
            TrainerMobData data = trainerManager.getData(entry.trainer());
            TrainerPackSnapshot snapshot = snapshot(entry.trainer(), data);
            if (snapshot.maxTeamLevel() <= 0) {
                pack.clear();
                throw new IllegalStateException(
                        "RCT trainer '" + entry.trainer()
                                + "' has no usable team level; progression override was not activated"
                );
            }
            snapshots.put(entry.trainer(), snapshot);
        }

        pack.replaceResources(RctPackJsonBuilder.build(profile, seriesSnapshot, snapshots));
        ZianRCT.LOGGER.info(
                "Generated {} Zian RCT virtual resources for profile '{}'; reloading server data.",
                pack.resourceCount(),
                config.activeProfile()
        );

        server.reloadResources(server.getPackRepository().getSelectedIds())
                .whenComplete((ignored, error) -> server.execute(() -> {
                    if (error != null) {
                        pack.clear();
                        ZianRCT.LOGGER.error("Failed to reload generated Zian RCT progression pack.", error);
                        return;
                    }
                    try {
                        trainerManager.loadTrainers();
                        verifyLoadedProgression(profile, trainerManager);
                        ZianRCT.LOGGER.info(
                                "Zian RCT Phase 3 generated progression loaded and verified after resource reload."
                        );
                    } catch (RuntimeException exception) {
                        ZianRCT.LOGGER.error(
                                "Generated Zian RCT resources reloaded, but RCT verification failed.",
                                exception
                        );
                    }
                }));
    }

    private static SeriesPackSnapshot snapshotSeries(MinecraftServer server, String seriesId) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                ZianRctVirtualPack.RCT_NAMESPACE,
                "series/" + seriesId + ".json"
        );
        var resource = server.getResourceManager().getResource(location)
                .orElseThrow(() -> new IllegalStateException("RCT series resource not found: " + location));
        try (BufferedReader reader = resource.openAsReader()) {
            return new SeriesPackSnapshot(reader.lines().collect(Collectors.joining("\n")));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read RCT series resource: " + location, exception);
        }
    }

    private static TrainerPackSnapshot snapshot(String trainerId, TrainerMobData data) {
        int maxTeamLevel = data.getTrainerTeam().getTeam().stream()
                .mapToInt(pokemon -> pokemon.getLevel())
                .max()
                .orElse(0);

        return new TrainerPackSnapshot(
                trainerId,
                data.getType().id(),
                data.getSignatureItem(),
                copyNested(data.getRequiredSeries()),
                new LinkedHashSet<>(data.getSubstitutes()),
                data.getMaxTrainerWins(),
                data.getMaxTrainerDefeats(),
                data.getBattleCooldownTicks(),
                data.getSpawnWeightFactor(),
                new LinkedHashSet<>(data.getBiomeTagBlacklist()),
                new LinkedHashSet<>(data.getBiomeTagWhitelist()),
                data.getForceBattleOnSight().has() ? data.getForceBattleOnSight().get() : null,
                data.getForceBattleMaxDistance().has() ? data.getForceBattleMaxDistance().get() : null,
                data.getForceBattleLookTicks().has() ? data.getForceBattleLookTicks().get() : null,
                data.getForceBattleMaxLevelDiff().has() ? data.getForceBattleMaxLevelDiff().get() : null,
                maxTeamLevel
        );
    }

    private static List<Set<String>> copyNested(List<Set<String>> source) {
        List<Set<String>> copy = new ArrayList<>();
        source.forEach(set -> copy.add(Set.copyOf(set)));
        return List.copyOf(copy);
    }

    private static void verifyLoadedProgression(
            ZianRctConfig.Profile profile,
            TrainerManager trainerManager
    ) {
        for (int index = 0; index < profile.chain().size(); index++) {
            String id = profile.chain().get(index).trainer();
            if (!trainerManager.isValidId(id)) {
                throw new IllegalStateException("Trainer disappeared after reload: " + id);
            }
            TrainerMobData loaded = trainerManager.getData(id);
            List<Set<String>> required = loaded.getRequiredDefeats(profile.series());
            if (index == 0) {
                if (!required.isEmpty()) {
                    throw new IllegalStateException("First trainer unexpectedly has required defeats: " + id);
                }
            } else {
                String previous = profile.chain().get(index - 1).trainer();
                boolean matches = required.size() == 1 && required.getFirst().contains(previous);
                if (!matches) {
                    throw new IllegalStateException(
                            "Trainer '" + id + "' did not load required predecessor '" + previous + "'"
                    );
                }
            }
            if (!loaded.getRelativeLevelCap().has()) {
                throw new IllegalStateException("Trainer did not load generated relativeLevelCap: " + id);
            }
        }
    }
}
