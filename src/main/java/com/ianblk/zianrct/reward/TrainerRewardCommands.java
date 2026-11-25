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
        catch (Exception error) { source.sendFailure(Component.translatableWithFallback("zianrct.cmd.error", "%s", error.getMessage() == null ? Component.translatableWithFallback("zianrct.reward.cmd.failed_generic", "No se pudo completar la operación") : Component.literal(error.getMessage()))); return 0; }
    }
    private static UUID admin(CommandSourceStack source) { return source.getEntity() instanceof ServerPlayer player ? player.getUUID() : new UUID(0, 0); }
    private static void saved(CommandSourceStack source, String trainer) {
        source.sendSuccess(() -> Component.translatableWithFallback("zianrct.reward.cmd.saved","Recompensa de %s guardada. Es única por jugador; solo afecta futuras victorias.",trainer), true);
    }
    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestTrainers(TrainerRewardService service,com.mojang.brigadier.suggestion.SuggestionsBuilder b){var ids=new java.util.LinkedHashSet<String>();try{com.ianblk.zianrct.config.ConfigState.current().activeProfileConfig().chain().forEach(c->ids.add(c.trainer()));ids.addAll(service.definitions().keySet());}catch(RuntimeException ignored){}return SharedSuggestionProvider.suggest(ids,b);}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, TrainerRewardService service) {
        var reward = Commands.literal("reward");
        reward.then(Commands.literal("next").requires(s -> RctPermissions.allows(s,"rewards.claim",false))
                .then(Commands.argument("trainer",StringArgumentType.word()).suggests((c,b)->suggestTrainers(service,b)).executes(c -> execute(c.getSource(),()->{
                    var player=c.getSource().getPlayerOrException();
                    player.sendSystemMessage(com.ianblk.zianrct.Messages.format(service.next(player,StringArgumentType.getString(c,"trainer"))));
                }))));
        reward.then(Commands.literal("policy").requires(s -> RctPermissions.allows(s,"admin.rewards.configure",true))
                .then(Commands.argument("trainer",StringArgumentType.word()).suggests((c,b)->suggestTrainers(service,b))
                        .then(Commands.literal("unique").executes(c -> execute(c.getSource(),()->{
                            service.policy(StringArgumentType.getString(c,"trainer"),RewardDefinition.Mode.UNIQUE,0);
                            c.getSource().sendSuccess(()->Component.translatableWithFallback("zianrct.reward.cmd.unique_saved","Premio único guardado; aplicando configuración de RCT."),true);
                        })))
                        .then(Commands.literal("repeat").then(Commands.argument("minutes",LongArgumentType.longArg(1,43200))
                                .executes(c -> execute(c.getSource(),()->{
                                    service.policy(StringArgumentType.getString(c,"trainer"),RewardDefinition.Mode.REPEAT,LongArgumentType.getLong(c,"minutes"));
                                    c.getSource().sendSuccess(()->Component.translatableWithFallback("zianrct.reward.cmd.repeat_saved","Premio repetible guardado; aplicando revancha de RCT."),true);
                                }))))));
        reward.then(Commands.literal("pending")
                .requires(s -> RctPermissions.allows(s, "rewards.claim", false))
                .executes(c -> execute(c.getSource(), () -> {
                    var player = c.getSource().getPlayerOrException();
                    var pending = service.claims(player.getUUID()).stream().filter(r -> !r.complete()).toList();
                    if (pending.isEmpty()) player.sendSystemMessage(com.ianblk.zianrct.Messages.format(Component.translatableWithFallback("zianrct.reward.none_pending","No tienes recompensas pendientes.")));
                    for (var claim : pending.stream().limit(20).toList()) {
                        var message = Component.literal(claim.trainer() + " | " + service.reference(player.getUUID(),claim.id()) + " | ").append(Component.translatableWithFallback(claim.review() ? "zianrct.reward.state.review" : "zianrct.reward.state.pending", claim.review() ? "REVISIÓN" : "PENDIENTE"));
                        if (!claim.review()) message.append(Component.translatableWithFallback("zianrct.reward.claim_button"," [Reclamar]").withStyle(style -> style
                                .withColor(net.minecraft.ChatFormatting.GOLD)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                        "/zianrct reward claim " + service.reference(player.getUUID(),claim.id())))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                        Component.translatableWithFallback("zianrct.reward.claim_tooltip","Pulsa para reclamar este premio pendiente.")))));
                        player.sendSystemMessage(com.ianblk.zianrct.Messages.format(message));
                    }
                    if (pending.size() > 20) player.sendSystemMessage(com.ianblk.zianrct.Messages.format(Component.translatableWithFallback("zianrct.reward.showing_20","Mostrando 20 de %s. Reclama las disponibles y consulta de nuevo.",pending.size())));
                })));
        reward.then(Commands.literal("claim")
                .requires(s -> RctPermissions.allows(s, "rewards.claim", false))
                .then(Commands.argument("operation", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            var source = context.getSource();
                            if (source.getPlayer() != null && RctPermissions.allows(source, "rewards.claim", false)) {
                                try {
                                    var player = source.getPlayer();
                                    for (var claim : service.claims(player.getUUID())) if(!claim.complete()&&!claim.review()) {String ref=service.reference(player.getUUID(),claim.id());if(ref.toLowerCase(java.util.Locale.ROOT).startsWith(builder.getRemainingLowerCase()))builder.suggest(ref);}
                                } catch (RuntimeException ignored) {
                                    // An unavailable journal or expired player session never exposes another player's IDs.
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(c -> execute(c.getSource(), () -> service.claim(c.getSource().getPlayerOrException(),
                                service.resolveReference(c.getSource().getPlayerOrException().getUUID(),StringArgumentType.getString(c, "operation")))))));
        reward.then(Commands.literal("status").requires(s -> RctPermissions.allows(s, "admin.rewards.review", true))
                .executes(c -> execute(c.getSource(), () -> c.getSource().sendSuccess(() -> Component.literal(service.status()), false))));
        reward.then(Commands.literal("list").requires(s -> RctPermissions.allows(s, "admin.rewards.configure", true))
                .executes(c -> execute(c.getSource(), () -> {
                    if (service.definitions().isEmpty()) c.getSource().sendSuccess(() -> Component.translatableWithFallback("zianrct.reward.cmd.none_configured","Sin premios configurados."), false);
                    service.definitions().forEach((trainer, d) -> c.getSource().sendSuccess(() -> Component.translatableWithFallback("zianrct.reward.cmd.list_entry","%s: %s objetos, %s %s",trainer,d.items().size(),d.coins(),d.currency()), false));
                })));
        reward.then(Commands.literal("set-money").requires(s -> RctPermissions.allows(s, "admin.rewards.configure", true))
                .then(Commands.argument("trainer", StringArgumentType.word()).suggests((c,b)->suggestTrainers(service,b))
                        .then(Commands.argument("currency", StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(java.util.List.of("avecoins:coppercoin","avecoins:ironcoin","avecoins:goldcoin","avecoins:diamondcoin","avecoins:netheritecoin","avecoins:goldticket","avecoins:diamondticket","avecoins:netheriteticket"),b))
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, 1728))
                                        .executes(c -> execute(c.getSource(), () -> {
                                            String trainer = StringArgumentType.getString(c, "trainer");
                                            service.coins(trainer, StringArgumentType.getString(c, "currency"), LongArgumentType.getLong(c, "amount"));
                                            saved(c.getSource(), trainer);
                                        }))))));
        for (String action : java.util.List.of("add-item", "clear-items", "remove")) {
            reward.then(Commands.literal(action).requires(s -> RctPermissions.allows(s, "admin.rewards.configure", true))
                    .then(Commands.argument("trainer", StringArgumentType.word()).suggests((c,b)->suggestTrainers(service,b))
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
                    c.getSource().sendSuccess(() -> Component.translatableWithFallback("zianrct.reward.cmd.config_saved","Configuración guardada. Los premios pendientes siguen reclamables."), true);
                }))));
        reward.then(Commands.literal("reload").requires(s -> RctPermissions.allows(s, "admin.rewards.configure", true))
                .executes(c -> execute(c.getSource(), () -> {
                    service.reload();
                    c.getSource().sendSuccess(() -> Component.translatableWithFallback("zianrct.reward.cmd.reloaded","Configuración de recompensas recargada; no cambia premios ya reservados."), true);
                })));
        reward.then(Commands.literal("review").requires(s -> RctPermissions.allows(s, "admin.rewards.review", true))
                .then(Commands.argument("player", EntityArgument.player()).executes(c -> execute(c.getSource(), () -> {
                    var target = EntityArgument.getPlayer(c, "player");
                    var claims = service.claims(target.getUUID());
                    if (claims.isEmpty()) c.getSource().sendSuccess(() -> Component.translatableWithFallback("zianrct.reward.cmd.no_claims","Sin reclamaciones registradas."), false);
                    for (var claim : claims) {
                        c.getSource().sendSuccess(() -> Component.literal(service.reference(target.getUUID(),claim.id()) + " | " + target.getGameProfile().getName()+" | "+claim.trainer()), false);
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
                                    .suggests((c,b)->{try{var owner=EntityArgument.getPlayer(c,"player").getUUID();for(var claim:service.claims(owner))if(claim.review()){String ref=service.reference(owner,claim.id());if(ref.toLowerCase(java.util.Locale.ROOT).startsWith(b.getRemainingLowerCase()))b.suggest(ref);}}catch(Exception ignored){}return b.buildFuture();})
                                    .then(Commands.argument("component", IntegerArgumentType.integer(1, 9))
                                            .then(Commands.argument("evidence", StringArgumentType.greedyString())
                                                    .executes(c -> execute(c.getSource(), () -> {
                                                        service.resolve(EntityArgument.getPlayer(c, "player").getUUID(),
                                                                service.resolveReference(EntityArgument.getPlayer(c,"player").getUUID(),StringArgumentType.getString(c, "operation")),
                                                                IntegerArgumentType.getInteger(c, "component") - 1, admin(c.getSource()),
                                                                decision.equals("confirm-delivered") ? "confirmed_delivered" : "confirmed_compensated",
                                                                StringArgumentType.getString(c, "evidence"));
                                                        c.getSource().sendSuccess(() -> Component.translatableWithFallback("zianrct.reward.cmd.component_closed","Componente cerrado. No se entrega ni paga nada automáticamente."), true);
                                                    })))))));
        }
        dispatcher.register(Commands.literal("zianrct").requires(s -> true).then(reward));
    }
}
