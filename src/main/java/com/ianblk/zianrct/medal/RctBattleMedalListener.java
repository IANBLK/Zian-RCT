package com.ianblk.zianrct.medal;

import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.battle.BattleState;
import com.gitlab.srcmc.rctapi.api.events.Event;
import com.gitlab.srcmc.rctapi.api.events.EventListener;
import com.gitlab.srcmc.rctapi.api.events.Events;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RctBattleMedalListener {
    private static final String RCT_INSTANCE_ID = "rctmod";

    private final MedalService medalService;
    private final com.ianblk.zianrct.reward.TrainerRewardService rewardService;
    private final EventListener<BattleState> battleEndedListener = this::onBattleEnded;
    private RCTApi registeredApi;

    public RctBattleMedalListener(MedalService medalService, com.ianblk.zianrct.reward.TrainerRewardService rewardService) {
        this.medalService = medalService;
        this.rewardService = rewardService;
    }

    public void registerIfAvailable() {
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

        if (registeredApi == api) {
            return;
        }
        if (registeredApi != null) {
            registeredApi.getEventContext().unregister(Events.BATTLE_ENDED, battleEndedListener);
        }

        api.getEventContext().register(Events.BATTLE_ENDED, battleEndedListener);
        registeredApi = api;
        ZianRCT.LOGGER.info(
                "Registered Zian RCT BATTLE_ENDED listener on RCTApi instance '{}'.",
                RCT_INSTANCE_ID
        );
    }

    public void resetRegistration() {
        if (registeredApi != null) {
            registeredApi.getEventContext().unregister(Events.BATTLE_ENDED, battleEndedListener);
            registeredApi = null;
        }
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
        List<ZianRctConfig.ChainEntry> chain = ConfigState.current().activeProfileConfig().chain();
        state.getLosers().forEach(trainer -> {
            if (trainer.getEntity() instanceof TrainerMob trainerMob) {
                String trainerId = configuredTrainerId(trainerMob.getTrainerId(), chain);
                if (trainerId == null && rewardService.interested(trainerMob.getTrainerId())) trainerId = trainerMob.getTrainerId();
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
            Runnable grant = () -> defeatedTrainerIds.forEach(trainerId -> {
                    medalService.medalForTrainer(trainerId).ifPresent(medal ->
                            medalService.grantIfAbsent(winner, medal.id(), MedalOrigin.BATTLE)
                    );
                    rewardService.won(winner, trainerId,state.getBattle()==null?null:state.getBattle().getBattleId());
            });
            if (server.isSameThread()) {
                grant.run();
            } else {
                server.execute(grant);
            }
        }
    }

    static String configuredTrainerId(String trainerId, List<ZianRctConfig.ChainEntry> chain) {
        if (trainerId == null || trainerId.isBlank() || chain == null) {
            return null;
        }
        for (ZianRctConfig.ChainEntry entry : chain) {
            if (entry != null && trainerId.equals(entry.trainer())) {
                return trainerId;
            }
        }
        return null;
    }
}
