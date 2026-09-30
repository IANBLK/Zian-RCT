package com.ianblk.zianrct.rct;

import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.api.data.save.TrainerPlayerData;
import com.ianblk.zianrct.config.ZianRctConfig;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RctProgressService {
    private RctProgressService() {
    }

    public static int currentCap(ServerPlayer player) {
        return playerData(player).getLevelCap();
    }

    public static SetCapResult setCap(
            ServerPlayer player,
            ZianRctConfig.Profile profile,
            int targetCap
    ) {
        List<String> desiredPrefix = RctProgressPlanner.desiredDefeatedPrefix(profile, targetCap);
        TrainerPlayerData playerData = playerData(player);
        Set<String> before = new LinkedHashSet<>(playerData.getDefeatedTrainerIds());
        Set<String> chainIds = profile.chain().stream()
                .map(ZianRctConfig.ChainEntry::trainer)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> desired = new LinkedHashSet<>(desiredPrefix);

        for (String id : chainIds) {
            boolean has = before.contains(id);
            boolean shouldHave = desired.contains(id);
            if (shouldHave && !has) {
                playerData.addProgressDefeat(id);
            } else if (!shouldHave && has) {
                playerData.removeProgressDefeat(id);
            }
        }
        playerData.sync();

        int actual = playerData.getLevelCap();
        if (actual == targetCap) {
            return new SetCapResult(true, targetCap, actual, List.copyOf(desiredPrefix));
        }

        Set<String> after = new LinkedHashSet<>(playerData.getDefeatedTrainerIds());
        for (String id : chainIds) {
            boolean originallyHad = before.contains(id);
            boolean currentlyHas = after.contains(id);
            if (originallyHad && !currentlyHas) {
                playerData.addProgressDefeat(id);
            } else if (!originallyHad && currentlyHas) {
                playerData.removeProgressDefeat(id);
            }
        }
        playerData.sync();
        return new SetCapResult(false, targetCap, actual, List.copyOf(desiredPrefix));
    }

    public static SetCapResult addStep(ServerPlayer player, ZianRctConfig.Profile profile) {
        int current = currentCap(player);
        return setCap(player, profile, RctProgressPlanner.nextCap(profile, current));
    }

    public static SetCapResult removeStep(ServerPlayer player, ZianRctConfig.Profile profile) {
        int current = currentCap(player);
        return setCap(player, profile, RctProgressPlanner.previousCap(profile, current));
    }

    public static ProgressView progress(ServerPlayer player, ZianRctConfig.Profile profile) {
        TrainerPlayerData data = playerData(player);
        Set<String> defeated = new LinkedHashSet<>(data.getDefeatedTrainerIds());
        List<String> configuredDefeats = RctProgressPlanner.configuredDefeatsInOrder(profile, defeated);
        String nextTrainer = null;
        for (ZianRctConfig.ChainEntry entry : profile.chain()) {
            if (!defeated.contains(entry.trainer())) {
                nextTrainer = entry.trainer();
                break;
            }
        }
        return new ProgressView(
                data.getLevelCap(),
                configuredDefeats,
                nextTrainer,
                RctProgressPlanner.reachableCaps(profile)
        );
    }

    private static TrainerPlayerData playerData(ServerPlayer player) {
        return RCTMod.getInstance().getTrainerManager().getData(player);
    }

    public record SetCapResult(
            boolean success,
            int requestedCap,
            int observedCap,
            List<String> defeatedPrefix
    ) {
    }

    public record ProgressView(
            int currentCap,
            List<String> defeatedConfiguredTrainers,
            String nextTrainer,
            List<Integer> reachableCaps
    ) {
    }
}
