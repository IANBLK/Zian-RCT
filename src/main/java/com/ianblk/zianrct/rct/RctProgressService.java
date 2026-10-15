package com.ianblk.zianrct.rct;

import com.ianblk.zianrct.standalone.StandaloneRuntime;
import com.ianblk.zianrct.standalone.PlayerProgress;
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
        PlayerProgress playerData = playerData(player);
        Set<String> before = new LinkedHashSet<>(playerData.getDefeatedTrainerIds());
        int beforeCap = playerData.getLevelCap();
        Set<String> chainIds = profile.chain().stream()
                .map(ZianRctConfig.ChainEntry::trainer)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> desired = new LinkedHashSet<>(desiredPrefix);

        try {
            applyChainState(playerData, chainIds, desired);
            playerData.sync();

            int actual = playerData.getLevelCap();
            if (actual == targetCap) {
                return new SetCapResult(true, targetCap, actual, List.copyOf(desiredPrefix));
            }

            restoreAndVerify(playerData, chainIds, before, beforeCap);
            return new SetCapResult(false, targetCap, actual, List.copyOf(desiredPrefix));
        } catch (RuntimeException exception) {
            try {
                restoreAndVerify(playerData, chainIds, before, beforeCap);
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
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
        PlayerProgress data = playerData(player);
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

    private static void applyChainState(
            PlayerProgress playerData,
            Set<String> chainIds,
            Set<String> desired
    ) {
        playerData.replaceChain(chainIds,desired);
    }

    private static void restoreAndVerify(
            PlayerProgress playerData,
            Set<String> chainIds,
            Set<String> before,
            int beforeCap
    ) {
        applyChainState(playerData, chainIds, before);
        playerData.sync();

        Set<String> restored = new LinkedHashSet<>(playerData.getDefeatedTrainerIds());
        for (String id : chainIds) {
            if (restored.contains(id) != before.contains(id)) {
                throw new IllegalStateException("RCT no pudo restaurar el progreso anterior para el entrenador: " + id);
            }
        }

        int restoredCap = playerData.getLevelCap();
        if (restoredCap != beforeCap) {
            throw new IllegalStateException(
                    "RCT restauró los ids de progreso, pero el tope quedó en " + restoredCap
                            + " en vez de " + beforeCap
            );
        }
    }

    private static PlayerProgress playerData(ServerPlayer player) {
        return StandaloneRuntime.getInstance().getTrainerManager().getData(player);
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
