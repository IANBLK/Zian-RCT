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
import java.util.concurrent.CompletableFuture;

public final class RctPackController {
    private static final int VERIFICATION_MAX_TICKS = 100;

    private final ZianRctVirtualPack pack = new ZianRctVirtualPack();
    private MinecraftServer activeServer;
    private boolean generatedReloadInFlight;
    private PendingVerification pendingVerification;
    private ConfigTransaction configTransaction;

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
        com.ianblk.zianrct.creator.CustomTrainerStore.clear();
        RctTrainerOptions.clear();
        activeServer = null;
        generatedReloadInFlight = false;
        pendingVerification = null;
        if (configTransaction != null) {
            configTransaction.future().complete(new ConfigReloadResult(false, "El servidor se detuvo durante la recarga."));
            configTransaction = null;
        }
        pack.clear();
    }

    private void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }

        MinecraftServer server = activeServer;
        if (server == null || isBusy()) {
            return;
        }

        server.execute(() -> {
            if (server == activeServer && !isBusy()) {
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

            if (pending.transactional()) {
                ConfigTransaction transaction = configTransaction;
                if (transaction == null) {
                    ZianRCT.LOGGER.error("Transactional RCT verification completed without an active transaction.");
                    return;
                }
                ConfigState.replace(transaction.nextConfig());
                configTransaction = null;
                transaction.future().complete(new ConfigReloadResult(
                        true,
                        transaction.nextConfig().messages().reloadSuccess()
                ));
            }

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
            if (pending.transactional()) {
                rollbackConfigTransaction(
                        event.getServer(),
                        "La verificación de la progresión de RCT no coincidió tras " + VERIFICATION_MAX_TICKS + " ticks.",
                        exception
                );
                return;
            }

            ZianRCT.LOGGER.error(
                    "Zian RCT generated progression could not be verified within {} server ticks after {}. "
                            + "Generated overrides will be disabled to avoid serving unverified progression.",
                    VERIFICATION_MAX_TICKS,
                    pending.reason(),
                    exception
            );
            deactivate(event.getServer(), "verification failed after " + pending.reason());
        }
    }

    public CompletableFuture<ConfigReloadResult> reloadConfig(
            MinecraftServer server,
            ZianRctConfig nextConfig,
            String reason
    ) {
        CompletableFuture<ConfigReloadResult> future = new CompletableFuture<>();
        if (server != activeServer) {
            future.complete(new ConfigReloadResult(false, "Zian RCT no tiene un servidor activo para recargar."));
            return future;
        }
        if (isBusy()) {
            future.complete(new ConfigReloadResult(false, "Ya hay una recarga o verificación de Zian RCT en curso."));
            return future;
        }

        final GeneratedCandidate candidate;
        try {
            nextConfig.validateOrThrow();
            candidate = buildCandidate(server, nextConfig);
        } catch (RuntimeException exception) {
            future.complete(new ConfigReloadResult(false, "No se puede aplicar la configuración: " + rootMessage(exception)));
            return future;
        }

        Map<ResourceLocation, byte[]> previousResources = pack.snapshotResources();
        configTransaction = new ConfigTransaction(nextConfig, previousResources, future);

        if (!pack.replaceResourcesIfChanged(candidate.generated())) {
            try {
                verifyLoadedProgression(
                        candidate.profile(),
                        RCTMod.getInstance().getTrainerManager(),
                        candidate.snapshots()
                );
                ConfigState.replace(nextConfig);
                configTransaction = null;
                future.complete(new ConfigReloadResult(true, nextConfig.messages().reloadSuccess()));
            } catch (RuntimeException exception) {
                rollbackConfigTransaction(
                        server,
                        "La configuración genera los mismos recursos, pero RCT no refleja la progresión esperada.",
                        exception
                );
            }
            return future;
        }

        generatedReloadInFlight = true;
        try {
            server.reloadResources(server.getPackRepository().getSelectedIds())
                    .whenComplete((ignored, error) -> server.execute(() -> {
                        generatedReloadInFlight = false;
                        if (error != null) {
                            rollbackConfigTransaction(
                                    server,
                                    "Falló la recarga de recursos generados de RCT.",
                                    error
                            );
                            return;
                        }
                        scheduleVerification(
                                candidate.profile(),
                                candidate.snapshots(),
                                reason + " (transactional reload)",
                                true
                        );
                    }));
        } catch (RuntimeException exception) {
            generatedReloadInFlight = false;
            rollbackConfigTransaction(server, "No se pudo iniciar la recarga de recursos de RCT.", exception);
        }
        return future;
    }

    public void regenerate(MinecraftServer server, String reason) {
        if (configTransaction != null || generatedReloadInFlight || pendingVerification != null) {
            ZianRCT.LOGGER.warn("Skipped Zian RCT regeneration after {} because another reload is still in progress.", reason);
            return;
        }

        try {
            ZianRctConfig config = ConfigState.current();
            GeneratedCandidate candidate = buildCandidate(server, config);

            if (!pack.replaceResourcesIfChanged(candidate.generated())) {
                scheduleVerification(
                        candidate.profile(),
                        candidate.snapshots(),
                        reason + " (generated bytes unchanged)",
                        false
                );
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
            requestGeneratedReload(server, candidate, reason);
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

    private GeneratedCandidate buildCandidate(MinecraftServer server, ZianRctConfig config) {
        ZianRctConfig.Profile profile = config.activeProfileConfig();
        if (profile == null) {
            throw new IllegalStateException("El perfil activo no existe: " + config.activeProfile());
        }

        ResourceLocation seriesLocation = ResourceLocation.fromNamespaceAndPath(
                ZianRctVirtualPack.RCT_NAMESPACE,
                "series/" + profile.series() + ".json"
        );
        Optional<String> seriesJson = readOriginalJsonIfPresent(server, seriesLocation);
        if (seriesJson.isEmpty()) {
            throw new IllegalStateException("configured RCT series resource is not loaded: " + seriesLocation);
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
                throw new IllegalStateException("configured RCT trainer resource is not loaded: " + missing);
            }

            int maxTeamLevel = maxTeamLevel(teamJson.get(), id);
            snapshots.put(id, new TrainerPackSnapshot(id, mobJson.get(), maxTeamLevel));
        }

        Map<String, TrainerPackSnapshot> stableSnapshots =
                Collections.unmodifiableMap(new LinkedHashMap<>(snapshots));
        Map<String, byte[]> generated = new LinkedHashMap<>(RctPackJsonBuilder.build(profile, series, stableSnapshots));
        for(var d:com.ianblk.zianrct.creator.CustomTrainerStore.all().values())com.ianblk.zianrct.creator.CustomTrainerValidation.validate(d);
        generated.putAll(com.ianblk.zianrct.creator.CustomTrainerResources.build(com.ianblk.zianrct.creator.CustomTrainerStore.all()));
        for(String trainer:RctTrainerOptions.repeat()){
            String key="mobs/trainers/single/"+trainer+".json";
            String source=generated.containsKey(key)?new String(generated.get(key),java.nio.charset.StandardCharsets.UTF_8)
                    :readOriginalJsonIfPresent(server,ResourceLocation.fromNamespaceAndPath("rctmod",key)).orElseThrow(()->new IllegalStateException("Entrenador repetible inexistente: "+trainer));
            generated.put(key,RctTrainerOverrides.repeatMob(source).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        for(var format:RctTrainerOptions.formats().entrySet()){
            String key="trainers/"+format.getKey()+".json";
            String source=generated.containsKey(key)?new String(generated.get(key),java.nio.charset.StandardCharsets.UTF_8):readOriginalJsonIfPresent(server,ResourceLocation.fromNamespaceAndPath("rctmod",key))
                    .orElseThrow(()->new IllegalStateException("Equipo inexistente: "+format.getKey()));
            generated.put(key,RctTrainerOverrides.format(source,format.getValue()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return new GeneratedCandidate(profile, generated, stableSnapshots);
    }

    private void rollbackConfigTransaction(MinecraftServer server, String reason, Throwable cause) {
        ConfigTransaction transaction = configTransaction;
        if (transaction == null) {
            ZianRCT.LOGGER.error("Could not roll back Zian RCT config transaction because no transaction is active.", cause);
            return;
        }

        pendingVerification = null;
        boolean resourcesChanged = pack.restoreResources(transaction.previousResources());
        String failureMessage = reason + " Se conservó la configuración anterior.";
        ZianRCT.LOGGER.error("{} Rolling back generated RCT resources.", failureMessage, cause);

        if (!resourcesChanged) {
            configTransaction = null;
            transaction.future().complete(new ConfigReloadResult(false, failureMessage));
            return;
        }

        generatedReloadInFlight = true;
        try {
            server.reloadResources(server.getPackRepository().getSelectedIds())
                    .whenComplete((ignored, rollbackError) -> server.execute(() -> {
                        generatedReloadInFlight = false;
                        configTransaction = null;
                        if (rollbackError != null) {
                            ZianRCT.LOGGER.error("Failed to reload the previous RCT resources during rollback.", rollbackError);
                            transaction.future().complete(new ConfigReloadResult(
                                    false,
                                    failureMessage + " Además falló la recarga de los recursos anteriores; revisa el log antes de continuar."
                            ));
                            return;
                        }
                        transaction.future().complete(new ConfigReloadResult(false, failureMessage));
                    }));
        } catch (RuntimeException rollbackStartError) {
            generatedReloadInFlight = false;
            configTransaction = null;
            ZianRCT.LOGGER.error("Could not start RCT resource rollback reload.", rollbackStartError);
            transaction.future().complete(new ConfigReloadResult(
                    false,
                    failureMessage + " Además no se pudo iniciar la recarga de rollback; revisa el log antes de continuar."
            ));
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
            GeneratedCandidate candidate,
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
                                    "Failed to reload generated Zian RCT progression pack. Generated overrides will be disabled.",
                                    error
                            );
                            deactivate(server, "generated resource reload failed after " + reason);
                            return;
                        }

                        scheduleVerification(
                                candidate.profile(),
                                candidate.snapshots(),
                                reason + " (generated reload)",
                                false
                        );
                    }));
        } catch (RuntimeException exception) {
            generatedReloadInFlight = false;
            throw exception;
        }
    }

    private void scheduleVerification(
            ZianRctConfig.Profile profile,
            Map<String, TrainerPackSnapshot> snapshots,
            String reason,
            boolean transactional
    ) {
        pendingVerification = new PendingVerification(
                profile,
                Collections.unmodifiableMap(new LinkedHashMap<>(snapshots)),
                VERIFICATION_MAX_TICKS,
                reason,
                transactional
        );
    }

    public boolean isBusy() {
        return generatedReloadInFlight || pendingVerification != null || configTransaction != null;
    }
    public void requireTrainerSource(MinecraftServer server,String trainer,boolean mob) {
        if(com.ianblk.zianrct.creator.CustomTrainerStore.get(trainer)!=null)return;
        String key=(mob?"mobs/trainers/single/":"trainers/")+trainer+".json";
        if(readOriginalJsonIfPresent(server,ResourceLocation.fromNamespaceAndPath("rctmod",key)).isEmpty())
            throw new IllegalArgumentException("Este ajuste requiere un entrenador con definición individual en datapack: "+key);
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
            throw new IllegalStateException("RCT trainer has no usable team: " + trainerId);
        }
        int max = 0;
        int index = 0;
        for (var element : team) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("RCT trainer team entry " + index + " is not an object: " + trainerId);
            }
            JsonObject pokemon = element.getAsJsonObject();
            if (!pokemon.has("level") || !pokemon.get("level").isJsonPrimitive()
                    || !pokemon.getAsJsonPrimitive("level").isNumber()) {
                throw new IllegalStateException(
                        "RCT trainer team entry " + index + " has no numeric level: " + trainerId
                );
            }
            int level = pokemon.get("level").getAsInt();
            if (level <= 0) {
                throw new IllegalStateException(
                        "RCT trainer team entry " + index + " has invalid level " + level + ": " + trainerId
                );
            }
            max = Math.max(max, level);
            index++;
        }
        return max;
    }

    private static void verifyLoadedProgression(
            ZianRctConfig.Profile profile,
            TrainerManager trainerManager,
            Map<String, TrainerPackSnapshot> snapshots
    ) {
        for(var d:com.ianblk.zianrct.creator.CustomTrainerStore.all().values()){
            if(!trainerManager.isValidId(d.id()))throw new IllegalStateException("Entrenador propio no cargado: "+d.id());
            if(d.trial()!=null){
                com.ianblk.zianrct.legendary.LegendaryTrials.validate(d);
                ZianRCT.LOGGER.info("Zian RCT legendary trial definition verified: {}",d.id());
            }
            if(d.autoMoves()){
                var prepared=com.ianblk.zianrct.creator.AutomaticTrainerMoves.prepare(d);
                com.ianblk.zianrct.creator.CustomTrainerValidation.validate(prepared);
                ZianRCT.LOGGER.info("Zian RCT automatic trainer moves verified: {}",d.id());
            }
            var dialogue=trainerManager.getData(d.id()).getDialog();
            var contexts=new java.util.LinkedHashMap<String,String>();
            contexts.put("on_battle_start",d.start());contexts.put("on_battle_lost",d.playerWins());contexts.put("on_battle_won",d.playerLoses());
            for(String context:java.util.List.of("trainer_busy","player_busy","on_cooldown","missing_pokemon","over_level_cap","wrong_series","missing_required_series","missing_required_trainer","done_generic","unknown_reason"))contexts.put(context,"required");
            for(var context:contexts.entrySet()){
                if(context.getValue().isBlank())continue;
                var messages=dialogue.get(context.getKey());
                if(messages==null || messages.length==0)throw new IllegalStateException("Diálogo propio no cargado: "+d.id()+" / "+context.getKey());
                for(var message:messages){
                    if(message==null || message.getTranslatable()==null || message.getTranslatable().isBlank())
                        throw new IllegalStateException("Diálogo propio sin clave de burbuja: "+d.id()+" / "+context.getKey());
                    // Exercise the same public component construction used by RCT's speech queue.
                    net.minecraft.network.chat.Component.translatable(message.getTranslatable()).getString();
                    message.getComponent().getString();
                }
            }
            String expected=RctTrainerOptions.formats().getOrDefault(d.id(),d.format());
            if(trainerManager.getData(d.id()).getTrainerTeam().getBattleFormat()==null || !trainerManager.getData(d.id()).getTrainerTeam().getBattleFormat().name().equals(expected))
                throw new IllegalStateException("Formato propio no cargado: "+d.id());
        }
        if(!com.ianblk.zianrct.creator.CustomTrainerStore.all().isEmpty())
            ZianRCT.LOGGER.info("Zian RCT custom trainer definitions verified: {}",com.ianblk.zianrct.creator.CustomTrainerStore.all().keySet());
        for(String trainer:RctTrainerOptions.repeat())
            if(!trainerManager.isValidId(trainer) || trainerManager.getData(trainer).getMaxTrainerDefeats()!=-1
                    || trainerManager.getData(trainer).getMaxTrainerWins()!=-1)
                throw new IllegalStateException("Revancha no cargada: "+trainer);
        for(var format:RctTrainerOptions.formats().entrySet()){
            if(!trainerManager.isValidId(format.getKey()) || trainerManager.getData(format.getKey()).getTrainerTeam().getBattleFormat()==null
                    || !trainerManager.getData(format.getKey()).getTrainerTeam().getBattleFormat().name().equals(format.getValue()))
                throw new IllegalStateException("Formato no cargado: "+format.getKey());
        }
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

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    public record ConfigReloadResult(boolean success, String message) {
    }

    private record GeneratedCandidate(
            ZianRctConfig.Profile profile,
            Map<String, byte[]> generated,
            Map<String, TrainerPackSnapshot> snapshots
    ) {
    }

    private record ConfigTransaction(
            ZianRctConfig nextConfig,
            Map<ResourceLocation, byte[]> previousResources,
            CompletableFuture<ConfigReloadResult> future
    ) {
    }

    private record PendingVerification(
            ZianRctConfig.Profile profile,
            Map<String, TrainerPackSnapshot> snapshots,
            int remainingTicks,
            String reason,
            boolean transactional
    ) {
        private PendingVerification withRemainingTicks(int nextRemainingTicks) {
            return new PendingVerification(profile, snapshots, nextRemainingTicks, reason, transactional);
        }
    }
}
