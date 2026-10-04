package com.ianblk.zianrct.command;

import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.config.ZianRctConfigLoader;
import com.ianblk.zianrct.medal.MedalOrigin;
import com.ianblk.zianrct.medal.MedalRecord;
import com.ianblk.zianrct.medal.MedalService;
import com.ianblk.zianrct.network.ZianRctNetwork;
import com.ianblk.zianrct.rct.RctPackController;
import com.ianblk.zianrct.rct.RctProgressService;
import com.ianblk.zianrct.permission.RctPermissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ZianRctCommands {
    private ZianRctCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            MedalService medalService,
            RctPackController packController
    ) {
        dispatcher.register(
                Commands.literal("medals")
                        .requires(source -> RctPermissions.allows(source, "medals", false))
                        .executes(context -> openMedals(context.getSource(), medalService))
        );

        dispatcher.register(
                Commands.literal("zianrct")
                        .then(Commands.literal("medal")
                                .then(Commands.literal("give")
                                        .requires(source -> RctPermissions.allows(source, "admin.medal.give", true))
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .then(Commands.argument("medalla", StringArgumentType.word())
                                                        .suggests((context, builder) -> suggestMedalIds(builder))
                                                        .executes(context -> give(
                                                                context.getSource(),
                                                                medalService,
                                                                EntityArgument.getPlayer(context, "jugador"),
                                                                StringArgumentType.getString(context, "medalla")
                                                        )))))
                                .then(Commands.literal("revoke")
                                        .requires(source -> RctPermissions.allows(source, "admin.medal.revoke", true))
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .then(Commands.argument("medalla", StringArgumentType.word())
                                                        .suggests((context, builder) -> suggestMedalIds(builder))
                                                        .executes(context -> revoke(
                                                                context.getSource(),
                                                                medalService,
                                                                EntityArgument.getPlayer(context, "jugador"),
                                                                StringArgumentType.getString(context, "medalla")
                                                        )))))
                                .then(Commands.literal("list")
                                        .requires(source -> RctPermissions.allows(source, "admin.medal.list", true))
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .executes(context -> list(
                                                        context.getSource(),
                                                        medalService,
                                                        EntityArgument.getPlayer(context, "jugador")
                                                ))))
                        )
                        .then(Commands.literal("cap")
                                .then(Commands.literal("get")
                                        .requires(source -> RctPermissions.allows(source, "admin.cap.get", true))
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .executes(context -> capGet(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "jugador")
                                                ))))
                                .then(Commands.literal("set")
                                        .requires(source -> RctPermissions.allows(source, "admin.cap.set", true))
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .then(Commands.argument("cap", IntegerArgumentType.integer(1, 10_000))
                                                        .executes(context -> capSet(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "jugador"),
                                                                IntegerArgumentType.getInteger(context, "cap")
                                                        )))))
                                .then(Commands.literal("add")
                                        .requires(source -> RctPermissions.allows(source, "admin.cap.add", true))
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .executes(context -> capAdd(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "jugador")
                                                ))))
                                .then(Commands.literal("remove")
                                        .requires(source -> RctPermissions.allows(source, "admin.cap.remove", true))
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .executes(context -> capRemove(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "jugador")
                                                ))))
                        )
                        .then(Commands.literal("progress")
                                .requires(source -> RctPermissions.allows(source, "admin.progress", true))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(context -> progress(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "jugador")
                                        ))))
                        .then(Commands.literal("reload")
                                .requires(source -> RctPermissions.allows(source, "admin.reload", true))
                                .executes(context -> reload(
                                        context.getSource(),
                                        medalService,
                                        packController
                                )))
        );
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestMedalIds(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        for (ZianRctConfig.MedalDefinition medal : ConfigState.current().activeProfileConfig().medals()) {
            if (medal != null && medal.id() != null) {
                builder.suggest(medal.id());
            }
        }
        return builder.buildFuture();
    }

    private static int openMedals(CommandSourceStack source, MedalService medalService) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("/medals solo puede ejecutarlo un jugador."));
            return 0;
        }
        if (!ZianRctNetwork.sendSnapshot(player, medalService)) {
            source.sendFailure(Component.literal("No se pudo sincronizar el medallero con el cliente."));
            return 0;
        }
        if (!ZianRctNetwork.sendOpen(player)) {
            source.sendFailure(Component.literal("No se pudo abrir el medallero en el cliente."));
            return 0;
        }
        return 1;
    }

    private static int give(CommandSourceStack source, MedalService medalService, ServerPlayer target, String medalId) {
        MedalService.GrantResult result = medalService.grantIfAbsent(target, medalId, MedalOrigin.COMMAND);
        return switch (result) {
            case GRANTED -> {
                source.sendSuccess(
                        () -> Component.literal("Medalla '" + medalId + "' concedida a " + target.getGameProfile().getName() + "."),
                        true
                );
                yield 1;
            }
            case ALREADY_PRESENT -> {
                source.sendFailure(Component.literal("El jugador ya posee la medalla '" + medalId + "'."));
                yield 0;
            }
            case UNKNOWN_MEDAL -> {
                source.sendFailure(Component.literal("La medalla '" + medalId + "' no existe en el perfil activo."));
                yield 0;
            }
            case INACTIVE -> {
                source.sendFailure(Component.literal("El servicio de medallas no está disponible."));
                yield 0;
            }
            case PERSISTENCE_ERROR -> {
                source.sendFailure(Component.literal("No se pudo guardar la medalla en disco."));
                yield 0;
            }
            case REVOKED -> {
                source.sendFailure(Component.literal("La medalla está revocada y no puede reconciliarse automáticamente."));
                yield 0;
            }
        };
    }

    private static int revoke(CommandSourceStack source, MedalService medalService, ServerPlayer target, String medalId) {
        if (medalService.medalDefinition(medalId).isEmpty()) {
            source.sendFailure(Component.literal("La medalla '" + medalId + "' no existe en el perfil activo."));
            return 0;
        }
        if (!medalService.revoke(target, medalId)) {
            source.sendFailure(Component.literal("No se pudo revocar la medalla '" + medalId + "' o no había cambios que guardar."));
            return 0;
        }
        source.sendSuccess(
                () -> Component.literal("Medalla '" + medalId + "' revocada a " + target.getGameProfile().getName() + "."),
                true
        );
        return 1;
    }

    private static int list(CommandSourceStack source, MedalService medalService, ServerPlayer target) {
        List<MedalRecord> medals = medalService.medals(target);
        if (medals.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(target.getGameProfile().getName() + " no tiene medallas registradas."),
                    false
            );
            return 1;
        }
        String joined = medals.stream()
                .map(record -> record.medalId() + " [" + record.origin().name() + "]")
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        source.sendSuccess(
                () -> Component.literal("Medallas de " + target.getGameProfile().getName() + ": " + joined),
                false
        );
        return medals.size();
    }

    private static int capGet(CommandSourceStack source, ServerPlayer target) {
        try {
            int cap = RctProgressService.currentCap(target);
            String message = ConfigState.current().messages().currentCap().replace("{cap}", Integer.toString(cap));
            source.sendSuccess(
                    () -> Component.literal(target.getGameProfile().getName() + ": " + message),
                    false
            );
            return cap;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("No se pudo leer el tope de RCT: " + exception.getMessage()));
            return 0;
        }
    }

    private static int capSet(CommandSourceStack source, ServerPlayer target, int requestedCap) {
        return reportCapChange(source, target, () -> RctProgressService.setCap(
                target,
                ConfigState.current().activeProfileConfig(),
                requestedCap
        ));
    }

    private static int capAdd(CommandSourceStack source, ServerPlayer target) {
        return reportCapChange(source, target, () -> RctProgressService.addStep(
                target,
                ConfigState.current().activeProfileConfig()
        ));
    }

    private static int capRemove(CommandSourceStack source, ServerPlayer target) {
        return reportCapChange(source, target, () -> RctProgressService.removeStep(
                target,
                ConfigState.current().activeProfileConfig()
        ));
    }

    private static int reportCapChange(
            CommandSourceStack source,
            ServerPlayer target,
            java.util.function.Supplier<RctProgressService.SetCapResult> action
    ) {
        try {
            RctProgressService.SetCapResult result = action.get();
            if (!result.success()) {
                source.sendFailure(Component.literal(
                        "RCT no confirmó el tope solicitado " + result.requestedCap()
                                + "; observó " + result.observedCap() + ". Se restauró el progreso anterior."
                ));
                return 0;
            }
            source.sendSuccess(
                    () -> Component.literal(
                            "Tope de " + target.getGameProfile().getName()
                                    + " establecido en " + result.observedCap()
                                    + ". Entrenadores de cadena marcados: " + result.defeatedPrefix().size() + "."
                    ),
                    true
            );
            return 1;
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("No se pudo modificar el progreso de RCT: " + exception.getMessage()));
            return 0;
        }
    }

    private static int progress(CommandSourceStack source, ServerPlayer target) {
        try {
            ZianRctConfig config = ConfigState.current();
            RctProgressService.ProgressView progress = RctProgressService.progress(
                    target,
                    config.activeProfileConfig()
            );
            String next = progress.nextTrainer() == null ? "completada" : progress.nextTrainer();
            String defeated = progress.defeatedConfiguredTrainers().isEmpty()
                    ? "ninguno"
                    : String.join(", ", progress.defeatedConfiguredTrainers());
            source.sendSuccess(
                    () -> Component.literal(
                            "Progreso de " + target.getGameProfile().getName()
                                    + " | perfil=" + config.activeProfile()
                                    + " | cap=" + progress.currentCap()
                                    + " | derrotados=" + defeated
                                    + " | siguiente=" + next
                                    + " | caps=" + progress.reachableCaps()
                    ),
                    false
            );
            return progress.currentCap();
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("No se pudo leer el progreso de RCT: " + exception.getMessage()));
            return 0;
        }
    }

    private static int reload(
            CommandSourceStack source,
            MedalService medalService,
            RctPackController packController
    ) {
        Path path = FMLPaths.CONFIGDIR.get().resolve(ZianRctConfigLoader.FILE_NAME);
        final ZianRctConfig next;
        try {
            next = ZianRctConfigLoader.loadOrCreate(path);
        } catch (IOException | RuntimeException exception) {
            source.sendFailure(Component.literal("No se recargó Zian RCT: " + exception.getMessage()));
            return 0;
        }

        var completion = packController.reloadConfig(source.getServer(), next, "/zianrct reload");
        if (completion.isDone()) {
            RctPackController.ConfigReloadResult immediate = completion.join();
            if (!immediate.success()) {
                source.sendFailure(Component.literal(immediate.message()));
                return 0;
            }
        }

        source.sendSuccess(
                () -> Component.literal("Recarga de Zian RCT iniciada; se aplicará solo si RCT verifica la progresión generada."),
                false
        );

        completion.thenAccept(result -> {
            if (!result.success()) {
                source.sendFailure(Component.literal("No se aplicó Zian RCT: " + result.message()));
                return;
            }

            int syncFailures = 0;
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                if (!ZianRctNetwork.sendSnapshot(player, medalService)) {
                    syncFailures++;
                }
            }

            int finalSyncFailures = syncFailures;
            source.sendSuccess(
                    () -> Component.literal(
                            finalSyncFailures == 0
                                    ? result.message()
                                    : result.message() + " No se pudo reenviar el snapshot a "
                                            + finalSyncFailures + " jugador(es)."
                    ),
                    true
            );
        });
        return 1;
    }
}
