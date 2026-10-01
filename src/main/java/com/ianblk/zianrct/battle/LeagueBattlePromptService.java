package com.ianblk.zianrct.battle;

import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LeagueBattlePromptService {
    private static final long INVITE_TTL_MILLIS = 15_000L;
    private static final double MAX_ACCEPT_DISTANCE_SQR = 8.0D * 8.0D;

    private final Map<UUID, PendingInvite> pending = new ConcurrentHashMap<>();

    public LeagueBattlePromptService() {
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof TrainerMob trainer)) {
            return;
        }
        if (!isRassvetTrainer(trainer.getTrainerId())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!trainer.canBattleAgainst(player)) {
            pending.remove(player.getUUID());
            trainer.startBattleWith(player);
            return;
        }

        PendingInvite invite = new PendingInvite(
                trainer.getUUID(),
                trainer.getTrainerId(),
                System.currentTimeMillis() + INVITE_TTL_MILLIS
        );
        pending.put(player.getUUID(), invite);
        sendPrompt(player, trainer, invite);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("zianrctbattle")
                        .then(Commands.literal("accept")
                                .then(Commands.argument("trainer", StringArgumentType.word())
                                        .executes(context -> accept(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "trainer")
                                        ))))
                        .then(Commands.literal("reject")
                                .then(Commands.argument("trainer", StringArgumentType.word())
                                        .executes(context -> reject(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "trainer")
                                        ))))
        );
    }

    private int accept(CommandSourceStack source, String trainerUuidText) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        PendingInvite invite = pending.remove(player.getUUID());
        if (invite == null) {
            player.sendSystemMessage(Component.literal("La invitación de combate ya no está disponible.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (invite.expiresAtMillis() < System.currentTimeMillis()) {
            player.sendSystemMessage(Component.literal("La invitación de combate ha caducado.").withStyle(ChatFormatting.RED));
            return 0;
        }

        UUID requestedTrainer;
        try {
            requestedTrainer = UUID.fromString(trainerUuidText);
        } catch (IllegalArgumentException exception) {
            return 0;
        }
        if (!invite.trainerUuid().equals(requestedTrainer)) {
            player.sendSystemMessage(Component.literal("Esta invitación ya no corresponde al entrenador seleccionado.").withStyle(ChatFormatting.RED));
            return 0;
        }

        Entity entity = player.serverLevel().getEntity(invite.trainerUuid());
        if (!(entity instanceof TrainerMob trainer)
                || !invite.trainerId().equals(trainer.getTrainerId())
                || !isRassvetTrainer(trainer.getTrainerId())) {
            player.sendSystemMessage(Component.literal("El entrenador ya no está disponible.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (player.distanceToSqr(trainer) > MAX_ACCEPT_DISTANCE_SQR) {
            player.sendSystemMessage(Component.literal("Te alejaste demasiado del entrenador. Interactúa con él otra vez.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!trainer.canBattleAgainst(player)) {
            trainer.startBattleWith(player);
            return 0;
        }

        player.sendSystemMessage(Component.literal("Desafío aceptado. ¡Prepárate para combatir!").withStyle(ChatFormatting.GREEN));
        trainer.startBattleWith(player);
        return 1;
    }

    private int reject(CommandSourceStack source, String trainerUuidText) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        PendingInvite invite = pending.get(player.getUUID());
        if (invite == null) {
            return 0;
        }
        if (!invite.trainerUuid().toString().equalsIgnoreCase(trainerUuidText)) {
            return 0;
        }

        pending.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("Has rechazado el desafío. Regresa cuando estés preparado.").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private void sendPrompt(ServerPlayer player, TrainerMob trainer, PendingInvite invite) {
        String uuid = invite.trainerUuid().toString();
        Component accept = Component.literal("[✔ Aceptar]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/zianrctbattle accept " + uuid))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Aceptar el desafío"))));
        Component reject = Component.literal("[✖ Rechazar]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/zianrctbattle reject " + uuid))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Rechazar el desafío"))));

        Component line = Component.literal("Líder Rassvet: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("¿Quieres comenzar este desafío?").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(line);
        player.sendSystemMessage(Component.empty().append(accept).append(Component.literal("  ")).append(reject));
        player.sendSystemMessage(Component.literal("La invitación caduca en 15 segundos.").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean isRassvetTrainer(String trainerId) {
        if (trainerId == null || trainerId.isBlank()) {
            return false;
        }
        ZianRctConfig.Profile profile = ConfigState.current().activeProfileConfig();
        return profile.chain().stream()
                .map(ZianRctConfig.ChainEntry::trainer)
                .anyMatch(trainerId::equals);
    }

    private record PendingInvite(UUID trainerUuid, String trainerId, long expiresAtMillis) {
    }
}
