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

    public static SetCapResult setCap(
            ServerPlayer player,
            ZianRctConfig.Profile profile,
            int targetCap
    ) {
        List<String> desiredPrefix = RctProgressPlanner.desiredDefeatedPrefix(profile, targetCap);
        TrainerPlayerData playerData = RCTMod.getInstance().getTrainerManager().getData(player);
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

    public record SetCapResult(
            boolean success,
            int requestedCap,
            int observedCap,
            List<String> defeatedPrefix
    ) {
    }
}
