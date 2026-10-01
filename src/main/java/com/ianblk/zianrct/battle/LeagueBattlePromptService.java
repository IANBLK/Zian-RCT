package com.ianblk.zianrct.battle;

import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.rct.RctProgressService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LeagueBattlePromptService {
    private static final long INVITE_TTL_MILLIS = 15_000L;
    private static final double MAX_ACCEPT_DISTANCE_SQR = 8.0D * 8.0D;

    private static final Map<String, String> PREREQUISITE_MESSAGES = Map.ofEntries(
            Map.entry("rassvet_leader_ferrum", "Aún no puedes retarme. Primero derrota a Novato y vuelve cuando estés listo."),
            Map.entry("rassvet_leader_aquila", "Antes de llegar hasta mí, debes superar a Ferrum. Vuelve cuando lo hayas derrotado."),
            Map.entry("rassvet_leader_voltar", "Todavía no estás listo para mi desafío. Primero derrota a Aquila."),
            Map.entry("rassvet_leader_engranaje", "Tu progreso aún no encaja en este engranaje. Primero vence a Voltar."),
            Map.entry("rassvet_leader_bruma", "No puedes atravesar la bruma todavía. Primero derrota a Engranaje."),
            Map.entry("rassvet_leader_cognitus", "Antes de poner a prueba tu mente conmigo, debes superar a Bruma."),
            Map.entry("rassvet_leader_forjax", "Aún te falta una prueba. Derrota primero a Cognitus."),
            Map.entry("rassvet_leader_glacius", "El hielo no recibe a cualquiera. Primero derrota a Forjax."),
            Map.entry("rassvet_master_aurelia", "Has llegado lejos, pero aún no es tu momento. Primero derrota a Glacius.")
    );

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

        String prerequisiteMessage = missingPrerequisiteMessage(player, trainer.getTrainerId());
        if (prerequisiteMessage != null) {
            pending.remove(player.getUUID());
            player.sendSystemMessage(
                    Component.literal("<" + trainerName(trainer.getTrainerId()) + "> ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(prerequisiteMessage).withStyle(ChatFormatting.WHITE))
            );
            return;
        }

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

        String prerequisiteMessage = missingPrerequisiteMessage(player, trainer.getTrainerId());
        if (prerequisiteMessage != null) {
            player.sendSystemMessage(
                    Component.literal("<" + trainerName(trainer.getTrainerId()) + "> ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(prerequisiteMessage).withStyle(ChatFormatting.WHITE))
            );
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

        Component line = Component.literal(trainerName(trainer.getTrainerId()) + ": ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("¿Quieres comenzar este desafío?").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(line);
        player.sendSystemMessage(Component.empty().append(accept).append(Component.literal("  ")).append(reject));
        player.sendSystemMessage(Component.literal("La invitación caduca en 15 segundos.").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String missingPrerequisiteMessage(ServerPlayer player, String trainerId) {
        ZianRctConfig.Profile profile = ConfigState.current().activeProfileConfig();
        List<ZianRctConfig.ChainEntry> chain = profile.chain();
        int index = -1;
        for (int i = 0; i < chain.size(); i++) {
            if (trainerId.equals(chain.get(i).trainer())) {
                index = i;
                break;
            }
        }
        if (index <= 0) {
            return null;
        }

        String requiredTrainer = chain.get(index - 1).trainer();
        try {
            if (RctProgressService.progress(player, profile).defeatedConfiguredTrainers().contains(requiredTrainer)) {
                return null;
            }
        } catch (RuntimeException exception) {
            ZianRCT.LOGGER.warn("Could not inspect Rassvet prerequisite progress for {} against {}", player.getGameProfile().getName(), trainerId, exception);
            return null;
        }
        return PREREQUISITE_MESSAGES.getOrDefault(
                trainerId,
                "Antes debes superar al líder anterior de la Liga Rassvet."
        );
    }

    private static String trainerName(String trainerId) {
        return switch (trainerId) {
            case "rassvet_leader_novato" -> "Líder Novato";
            case "rassvet_leader_ferrum" -> "Líder Ferrum";
            case "rassvet_leader_aquila" -> "Líder Aquila";
            case "rassvet_leader_voltar" -> "Líder Voltar";
            case "rassvet_leader_engranaje" -> "Líder Engranaje";
            case "rassvet_leader_bruma" -> "Líder Bruma";
            case "rassvet_leader_cognitus" -> "Líder Cognitus";
            case "rassvet_leader_forjax" -> "Líder Forjax";
            case "rassvet_leader_glacius" -> "Líder Glacius";
            case "rassvet_master_aurelia" -> "Maestra Aurelia";
            default -> "Líder Rassvet";
        };
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
