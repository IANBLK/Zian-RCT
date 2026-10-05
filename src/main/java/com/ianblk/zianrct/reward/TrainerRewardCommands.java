package com.ianblk.zianrct.reward;

import com.ianblk.zianrct.permission.RctPermissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class TrainerRewardCommands {
    private TrainerRewardCommands() {}
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static int execute(CommandSourceStack source, Action action) {
        try { action.run(); return 1; }
        catch (Exception error) { source.sendFailure(Component.literal(error.getMessage() == null ? "No se pudo completar la operación" : error.getMessage())); return 0; }
    }
    private static UUID admin(CommandSourceStack source) { return source.getEntity() instanceof ServerPlayer player ? player.getUUID() : new UUID(0, 0); }
    private static void saved(CommandSourceStack source, String trainer) {
        source.sendSuccess(() -> Component.literal("Recompensa de " + trainer + " guardada. Es única por jugador; solo afecta futuras victorias."), true);
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, TrainerRewardService service) {
        var reward = Commands.literal("reward");
        reward.then(Commands.literal("pending")
                .requires(s -> RctPermissions.allows(s, "rewards.claim", false))
                .executes(c -> execute(c.getSource(), () -> {
                    var player = c.getSource().getPlayerOrException();
                    var pending = service.claims(player.getUUID()).stream().filter(r -> !r.complete()).toList();
                    if (pending.isEmpty()) player.sendSystemMessage(Component.literal("No tienes recompensas pendientes."));
                    for (var claim : pending.stream().limit(20).toList()) player.sendSystemMessage(Component.literal(claim.trainer() + " | " + claim.id() + " | " + (claim.review() ? "REVISIÓN" : "PENDIENTE")));
                    if (pending.size() > 20) player.sendSystemMessage(Component.literal("Mostrando 20 de " + pending.size() + ". Reclama las disponibles y consulta de nuevo."));
                })));
        reward.then(Commands.literal("claim")
                .requires(s -> RctPermissions.allows(s, "rewards.claim", false))
                .then(Commands.argument("operation", StringArgumentType.word())
                        .executes(c -> execute(c.getSource(), () -> service.claim(c.getSource().getPlayerOrException(),
                                UUID.fromString(StringArgumentType.getString(c, "operation")))))));
        reward.then(Commands.literal("status").requires(s -> RctPermissions.allows(s, "admin.rewards.review", true))
                .executes(c -> execute(c.getSource(), () -> c.getSource().sendSuccess(() -> Component.literal(service.status()), false))));
        reward.then(Commands.literal("list").requires(s -> RctPermissions.allows(s, "admin.rewards.configure", true))
                .executes(c -> execute(c.getSource(), () -> {
                    if (service.definitions().isEmpty()) c.getSource().sendSuccess(() -> Component.literal("Sin premios configurados."), false);
                    service.definitions().forEach((trainer, d) -> c.getSource().sendSuccess(() -> Component.literal(trainer + ": " + d.items().size() + " objetos, " + d.coins() + " " + d.currency()), false));
                })));
        reward.then(Commands.literal("set-money").requires(s -> RctPermissions.allows(s, "admin.rewards.configure", true))
                .then(Commands.argument("trainer", StringArgumentType.word())
                        .then(Commands.argument("currency", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, 1728))
                                        .executes(c -> execute(c.getSource(), () -> {
                                            String trainer = StringArgumentType.getString(c, "trainer");
                                            service.coins(trainer, StringArgumentType.getString(c, "currency"), LongArgumentType.getLong(c, "amount"));
                                            saved(c.getSource(), trainer);
                                        }))))));
        for (String action : java.util.List.of("add-item", "clear-items", "remove")) {
            reward.then(Commands.literal(action).requires(s -> RctPermissions.allows(s, "admin.rewards.configure", true))
                    .then(Commands.argument("trainer", StringArgumentType.word())
                            .executes(c -> execute(c.getSource(), () -> {
                                String trainer = StringArgumentType.getString(c, "trainer");
                                switch (action) {
                                    case "add-item" -> service.addItem(trainer, c.getSource().getPlayerOrException());
                                    case "clear-items" -> service.clearItems(trainer);
                                    case "remove" -> service.remove(trainer);
                                }
                                saved(c.getSource(), trainer);
                            }))));
        }
        reward.then(Commands.literal("enabled").requires(s -> RctPermissions.allows(s, "admin.rewards.configure", true))
                .then(Commands.argument("value", BoolArgumentType.bool()).executes(c -> execute(c.getSource(), () -> {
                    service.enabled(BoolArgumentType.getBool(c, "value"));
                    c.getSource().sendSuccess(() -> Component.literal("Configuración guardada. Los premios pendientes siguen reclamables."), true);
                }))));
        reward.then(Commands.literal("reload").requires(s -> RctPermissions.allows(s, "admin.rewards.configure", true))
                .executes(c -> execute(c.getSource(), () -> {
                    service.reload();
                    c.getSource().sendSuccess(() -> Component.literal("Configuración de recompensas recargada; no cambia premios ya reservados."), true);
                })));
        reward.then(Commands.literal("review").requires(s -> RctPermissions.allows(s, "admin.rewards.review", true))
                .then(Commands.argument("player", EntityArgument.player()).executes(c -> execute(c.getSource(), () -> {
                    var target = EntityArgument.getPlayer(c, "player");
                    var claims = service.claims(target.getUUID());
                    if (claims.isEmpty()) c.getSource().sendSuccess(() -> Component.literal("Sin reclamaciones registradas."), false);
                    for (var claim : claims) {
                        c.getSource().sendSuccess(() -> Component.literal(claim.id() + " | " + claim.trainer()), false);
                        for (int i = 0; i < claim.parts().size(); i++) {
                            int index = i + 1;
                            var part = claim.parts().get(i);
                            c.getSource().sendSuccess(() -> Component.literal("  " + index + ": " + service.describe(target, part) + " | " + part.phase() + " | " + part.detail()), false);
                        }
                    }
                }))));
        for (String decision : java.util.List.of("confirm-delivered", "confirm-compensated")) {
            reward.then(Commands.literal(decision).requires(s -> RctPermissions.allows(s, "admin.rewards.resolve", true))
                    .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("operation", StringArgumentType.word())
                                    .then(Commands.argument("component", IntegerArgumentType.integer(1, 9))
                                            .then(Commands.argument("evidence", StringArgumentType.greedyString())
                                                    .executes(c -> execute(c.getSource(), () -> {
                                                        service.resolve(EntityArgument.getPlayer(c, "player").getUUID(),
                                                                UUID.fromString(StringArgumentType.getString(c, "operation")),
                                                                IntegerArgumentType.getInteger(c, "component") - 1, admin(c.getSource()),
                                                                decision.equals("confirm-delivered") ? "confirmed_delivered" : "confirmed_compensated",
                                                                StringArgumentType.getString(c, "evidence"));
                                                        c.getSource().sendSuccess(() -> Component.literal("Componente cerrado. No se entrega ni paga nada automáticamente."), true);
                                                    })))))));
        }
        dispatcher.register(Commands.literal("zianrct").requires(s -> true).then(reward));
    }
}
