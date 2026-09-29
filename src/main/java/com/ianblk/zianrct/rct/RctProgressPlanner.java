package com.ianblk.zianrct.rct;

import com.ianblk.zianrct.config.ZianRctConfig;

import java.util.List;

public final class RctProgressPlanner {
    private RctProgressPlanner() {
    }

    public static List<String> desiredDefeatedPrefix(
            ZianRctConfig.Profile profile,
            int targetCap
    ) {
        if (targetCap == profile.initialCap()) {
            return List.of();
        }

        for (int index = 0; index < profile.chain().size(); index++) {
            if (profile.unlockCap(index) == targetCap) {
                return profile.chain().subList(0, index + 1).stream()
                        .map(ZianRctConfig.ChainEntry::trainer)
                        .toList();
            }
        }
        throw new IllegalArgumentException(
                "El cap " + targetCap + " no es alcanzable por la cadena configurada"
        );
    }
}
