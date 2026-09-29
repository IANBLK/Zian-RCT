package com.ianblk.zianrct.rct;

import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.api.data.pack.TrainerMobData;
import com.gitlab.srcmc.rctmod.api.service.TrainerManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RctPackController {
    private static final int VERIFICATION_MAX_TICKS = 100;

    private final ZianRctVirtualPack pack = new ZianRctVirtualPack();
    private MinecraftServer activeServer;
    private boolean generatedReloadInFlight;
    private PendingVerification pendingVerification;

    public RctPackController(IEventBus modEventBus) {
        modEventBus.addListener(this::onAddPackFinders);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(this::onServerPostTick);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            event.addRepositorySource(new ZianRctPackSource(pack));
            ZianRCT.LOGGER.info("Registered Zian RCT virtual SERVER_DATA pack at TOP priority.");
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        activeServer = event.getServer();
        regenerate(activeServer, "server start");
    }

    private void onServerStopped(ServerStoppedEvent event) {
        activeServer = null;
        generatedReloadInFlight = false;
        pendingVerification = null;
        pack.clear();
    }

    private void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }

        MinecraftServer server = activeServer;
        if (server == null || generatedReloadInFlight) {
            return;
        }

        server.execute(() -> {
            if (server == activeServer && !generatedReloadInFlight) {
                regenerate(server, "server data reload");
            }
        });
    }

    private void onServerPostTick(ServerTickEvent.Post event) {
        if (event.getServer() != activeServer || pendingVerification == null) {
            return;
        }

        PendingVerification pending = pendingVerification;
        try {
            verifyLoadedProgression(
                    pending.profile(),
                    RCTMod.getInstance().getTrainerManager(),
                    pending.snapshots()
            );
            pendingVerification = null;
            ZianRCT.LOGGER.info(
                    "Zian RCT generated progression verified after {}.",
                    pending.reason()
            );
        } catch (RuntimeException exception) {
            int remaining = pending.remainingTicks() - 1;
            if (remaining > 0) {
                pendingVerification = pending.withRemainingTicks(remaining);
                return;
            }

            pendingVerification = null;
            ZianRCT.LOGGER.error(
                    "Zian RCT generated progression could not be verified within {} server ticks after {}. "
                            + "The server remains online and generated data will be retried on the next data reload.",
                    VERIFICATION_MAX_TICKS,
                    pending.reason(),
                    exception
            );
        }
    }

    public void regenerate(MinecraftServer server, String reason) {
        try {
            ZianRctConfig config = ConfigState.current();
            ZianRctConfig.Profile profile = config.activeProfileConfig();

            ResourceLocation seriesLocation = ResourceLocation.fromNamespaceAndPath(
                    ZianRctVirtualPack.RCT_NAMESPACE,
                    "series/" + profile.series() + ".json"
            );
            Optional<String> seriesJson = readOriginalJsonIfPresent(server, seriesLocation);
            if (seriesJson.isEmpty()) {
                deactivate(server, "configured RCT series resource is not loaded: " + seriesLocation);
                return;
            }

            SeriesPackSnapshot series = new SeriesPackSnapshot(seriesJson.get());
            LinkedHashMap<String, TrainerPackSnapshot> snapshots = new LinkedHashMap<>();

            for (ZianRctConfig.ChainEntry entry : profile.chain()) {
                String id = entry.trainer();
                ResourceLocation mobLocation = ResourceLocation.fromNamespaceAndPath(
                        ZianRctVirtualPack.RCT_NAMESPACE,
                        "mobs/trainers/single/" + id + ".json"
                );
                ResourceLocation teamLocation = ResourceLocation.fromNamespaceAndPath(
                        ZianRctVirtualPack.RCT_NAMESPACE,
                        "trainers/" + id + ".json"
                );

                Optional<String> mobJson = readOriginalJsonIfPresent(server, mobLocation);
                Optional<String> teamJson = readOriginalJsonIfPresent(server, teamLocation);
                if (mobJson.isEmpty() || teamJson.isEmpty()) {
                    String missing = mobJson.isEmpty() ? mobLocation.toString() : teamLocation.toString();
                    deactivate(server, "configured RCT trainer resource is not loaded: " + missing);
                    return;
                }

                int maxTeamLevel = maxTeamLevel(teamJson.get(), id);
                if (maxTeamLevel <= 0) {
                    deactivate(server, "trainer '" + id + "' has no usable team level");
                    return;
                }

                snapshots.put(id, new TrainerPackSnapshot(id, mobJson.get(), maxTeamLevel));
            }

            Map<String, TrainerPackSnapshot> stableSnapshots =
                    Collections.unmodifiableMap(new LinkedHashMap<>(snapshots));
            Map<String, byte[]> generated = RctPackJsonBuilder.build(profile, series, stableSnapshots);

            if (!pack.replaceResourcesIfChanged(generated)) {
                scheduleVerification(profile, stableSnapshots, reason + " (generated bytes unchanged)");
                ZianRCT.LOGGER.info(
                        "Zian RCT progression unchanged after {}; generated pack already matches original RCT data.",
                        reason
                );
                return;
            }

            pendingVerification = null;
            ZianRCT.LOGGER.info(
                    "Generated {} Zian RCT virtual resources for profile '{}' after {}; reloading server data.",
                    pack.resourceCount(),
                    config.activeProfile(),
                    reason
            );
            requestGeneratedReload(server, profile, stableSnapshots, reason);
        } catch (RuntimeException exception) {
            pendingVerification = null;
            generatedReloadInFlight = false;
            ZianRCT.LOGGER.error(
                    "Zian RCT progression regeneration failed after {}. Generated overrides will be disabled "
                            + "so RCT can fall back to the original data. The server remains online.",
                    reason,
                    exception
            );
            try {
                deactivate(server, "regeneration failed after " + reason);
            } catch (RuntimeException deactivateException) {
                generatedReloadInFlight = false;
                ZianRCT.LOGGER.error(
                        "Zian RCT could not disable generated overrides after a regeneration failure. "
                                + "The server remains online, but another reload may be required.",
                        deactivateException
                );
            }
        }
    }

    private void deactivate(MinecraftServer server, String reason) {
        pendingVerification = null;
        boolean changed = pack.clearIfChanged();
        ZianRCT.LOGGER.warn("Zian RCT progression pack is inactive because {}.", reason);
        if (!changed) {
            generatedReloadInFlight = false;
            return;
        }

        generatedReloadInFlight = true;
        try {
            server.reloadResources(server.getPackRepository().getSelectedIds())
                    .whenComplete((ignored, error) -> server.execute(() -> {
                        generatedReloadInFlight = false;
                        if (error != null) {
                            ZianRCT.LOGGER.error(
                                    "Failed to reload after disabling Zian RCT generated overrides. "
                                            + "The server remains online.",
                                    error
                            );
                        }
                    }));
        } catch (RuntimeException exception) {
            generatedReloadInFlight = false;
            throw exception;
        }
    }

    private void requestGeneratedReload(
            MinecraftServer server,
            ZianRctConfig.Profile profile,
            Map<String, TrainerPackSnapshot> snapshots,
            String reason
    ) {
        generatedReloadInFlight = true;
        try {
            server.reloadResources(server.getPackRepository().getSelectedIds())
                    .whenComplete((ignored, error) -> server.execute(() -> {
                        generatedReloadInFlight = false;
                        if (error != null) {
                            pendingVerification = null;
                            ZianRCT.LOGGER.error(
                                    "Failed to reload generated Zian RCT progression pack. The server remains online.",
                                    error
                            );
                            return;
                        }

                        scheduleVerification(profile, snapshots, reason + " (generated reload)");
                    }));
        } catch (RuntimeException exception) {
            generatedReloadInFlight = false;
            throw exception;
        }
    }

    private void scheduleVerification(
            ZianRctConfig.Profile profile,
            Map<String, TrainerPackSnapshot> snapshots,
            String reason
    ) {
        pendingVerification = new PendingVerification(
                profile,
                Collections.unmodifiableMap(new LinkedHashMap<>(snapshots)),
                VERIFICATION_MAX_TICKS,
                reason
        );
    }

    private static Optional<String> readOriginalJsonIfPresent(
            MinecraftServer server,
            ResourceLocation location
    ) {
        List<Resource> stack = server.getResourceManager().getResourceStack(location);
        for (int index = stack.size() - 1; index >= 0; index--) {
            Resource resource = stack.get(index);
            if (ZianRctVirtualPack.PACK_ID.equals(resource.sourcePackId())) {
                continue;
            }
            try (BufferedReader reader = resource.openAsReader()) {
                return Optional.of(reader.lines().collect(java.util.stream.Collectors.joining("\n")));
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read original RCT resource: " + location, exception);
            }
        }
        return Optional.empty();
    }

    private static int maxTeamLevel(String sourceJson, String trainerId) {
        var parsed = JsonParser.parseString(sourceJson);
        if (!parsed.isJsonObject()) {
            throw new IllegalStateException("RCT trainer team JSON is not an object: " + trainerId);
        }
        JsonArray team = parsed.getAsJsonObject().getAsJsonArray("team");
        if (team == null || team.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (var element : team) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject pokemon = element.getAsJsonObject();
            if (pokemon.has("level") && pokemon.get("level").isJsonPrimitive()) {
                max = Math.max(max, pokemon.get("level").getAsInt());
            }
        }
        return max;
    }

    private static void verifyLoadedProgression(
            ZianRctConfig.Profile profile,
            TrainerManager trainerManager,
            Map<String, TrainerPackSnapshot> snapshots
    ) {
        LinkedHashMap<String, Integer> relativeCaps = new LinkedHashMap<>();
        for (int index = 0; index < profile.chain().size(); index++) {
            String id = profile.chain().get(index).trainer();
            if (!trainerManager.isValidId(id)) {
                throw new IllegalStateException("Trainer disappeared after reload: " + id);
            }

            TrainerMobData loaded = trainerManager.getData(id);
            List<java.util.Set<String>> required = loaded.getRequiredDefeats(profile.series());
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

            int requiredCap = index == 0 ? profile.initialCap() : profile.unlockCap(index - 1);
            int expectedRelative = requiredCap - snapshots.get(id).maxTeamLevel();
            if (!loaded.getRelativeLevelCap().has()) {
                throw new IllegalStateException("Trainer did not load generated relativeLevelCap: " + id);
            }
            int actualRelative = loaded.getRelativeLevelCap().get();
            if (actualRelative != expectedRelative) {
                throw new IllegalStateException(
                        "Trainer '" + id + "' relativeLevelCap mismatch: expected "
                                + expectedRelative + " but RCT loaded " + actualRelative
                );
            }
            relativeCaps.put(id, actualRelative);
        }

        ZianRCT.LOGGER.info(
                "Zian RCT generated progression verified for {} trainers; relative caps: {}",
                relativeCaps.size(),
                relativeCaps
        );
    }

    private record PendingVerification(
            ZianRctConfig.Profile profile,
            Map<String, TrainerPackSnapshot> snapshots,
            int remainingTicks,
            String reason
    ) {
        private PendingVerification withRemainingTicks(int nextRemainingTicks) {
            return new PendingVerification(profile, snapshots, nextRemainingTicks, reason);
        }
    }
}
