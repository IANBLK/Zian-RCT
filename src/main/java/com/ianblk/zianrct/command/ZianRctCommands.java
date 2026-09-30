package com.ianblk.zianrct.command;

import com.ianblk.zianrct.medal.MedalOrigin;
import com.ianblk.zianrct.medal.MedalRecord;
import com.ianblk.zianrct.medal.MedalService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ZianRctCommands {
    private ZianRctCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, MedalService medalService) {
        dispatcher.register(
                Commands.literal("zianrct")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("medal")
                                .then(Commands.literal("give")
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .then(Commands.argument("medalla", StringArgumentType.word())
                                                        .executes(context -> give(
                                                                context.getSource(),
                                                                medalService,
                                                                EntityArgument.getPlayer(context, "jugador"),
                                                                StringArgumentType.getString(context, "medalla")
                                                        )))))
                                .then(Commands.literal("revoke")
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .then(Commands.argument("medalla", StringArgumentType.word())
                                                        .executes(context -> revoke(
                                                                context.getSource(),
                                                                medalService,
                                                                EntityArgument.getPlayer(context, "jugador"),
                                                                StringArgumentType.getString(context, "medalla")
                                                        )))))
                                .then(Commands.literal("list")
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .executes(context -> list(
                                                        context.getSource(),
                                                        medalService,
                                                        EntityArgument.getPlayer(context, "jugador")
                                                ))))
                        )
        );
    }

    private static int give(
            CommandSourceStack source,
            MedalService medalService,
            ServerPlayer target,
            String medalId
    ) {
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

    private static int revoke(
            CommandSourceStack source,
            MedalService medalService,
            ServerPlayer target,
            String medalId
    ) {
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

    private static int list(
            CommandSourceStack source,
            MedalService medalService,
            ServerPlayer target
    ) {
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
}
