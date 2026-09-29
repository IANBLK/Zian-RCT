package com.ianblk.zianrct.medal;

import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.battle.BattleState;
import com.gitlab.srcmc.rctapi.api.events.Event;
import com.gitlab.srcmc.rctapi.api.events.Events;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.api.data.pack.TrainerMobData;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.config.ConfigState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RctBattleMedalListener {
    private static final String RCT_INSTANCE_ID = "rctmod";

    private final MedalService medalService;
    private boolean registered;

    public RctBattleMedalListener(MedalService medalService) {
        this.medalService = medalService;
    }

    public void registerIfAvailable() {
        if (registered) {
            return;
        }

        List<Map.Entry<String, RCTApi>> instances = RCTApi.getInstances().toList();
        ZianRCT.LOGGER.info(
                "RCTApi instances available for Zian RCT: {}",
                instances.stream().map(Map.Entry::getKey).toList()
        );

        RCTApi api = instances.stream()
                .filter(entry -> RCT_INSTANCE_ID.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (api == null) {
            ZianRCT.LOGGER.error(
                    "RCTApi instance '{}' is not available; Zian RCT medals will not listen for battle victories.",
                    RCT_INSTANCE_ID
            );
            return;
        }

        api.getEventContext().register(Events.BATTLE_ENDED, this::onBattleEnded);
        registered = true;
        ZianRCT.LOGGER.info(
                "Registered Zian RCT BATTLE_ENDED listener on RCTApi instance '{}'.",
                RCT_INSTANCE_ID
        );
    }

    private void onBattleEnded(Event<BattleState> event) {
        BattleState state = event == null ? null : event.getValue();
        if (state == null || state.isEndForced()) {
            return;
        }

        Set<ServerPlayer> winners = new LinkedHashSet<>();
        state.getWinners().forEach(trainer -> {
            if (trainer.getEntity() instanceof ServerPlayer player) {
                winners.add(player);
            }
        });
        if (winners.isEmpty()) {
            return;
        }

        Set<String> defeatedTrainerIds = new LinkedHashSet<>();
        state.getLosers().forEach(trainer -> {
            if (trainer.getEntity() instanceof TrainerMob trainerMob) {
                String trainerId = configuredTrainerId(trainerMob);
                if (trainerId != null) {
                    defeatedTrainerIds.add(trainerId);
                }
            }
        });
        if (defeatedTrainerIds.isEmpty()) {
            return;
        }

        for (ServerPlayer winner : winners) {
            MinecraftServer server = winner.serverLevel().getServer();
            Runnable grant = () -> defeatedTrainerIds.forEach(trainerId ->
                    medalService.medalForTrainer(trainerId).ifPresent(medal ->
                            medalService.grantIfAbsent(winner, medal.id(), MedalOrigin.BATTLE)
                    )
            );
            if (server.isSameThread()) {
                grant.run();
            } else {
                server.execute(grant);
            }
        }
    }

    private static String configuredTrainerId(TrainerMob trainerMob) {
        var trainerManager = RCTMod.getInstance().getTrainerManager();
        TrainerMobData mobData = trainerManager.getData(trainerMob);
        if (mobData == null) {
            return null;
        }

        for (var entry : ConfigState.current().activeProfileConfig().chain()) {
            if (entry == null) {
                continue;
            }
            TrainerMobData configured = trainerManager.getData(entry.trainer());
            if (configured == mobData || (configured != null && configured.equals(mobData))) {
                return entry.trainer();
            }
        }
        return null;
    }
}
