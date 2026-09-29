package com.ianblk.zianrct.rct.pack;

import java.util.List;
import java.util.Set;

public record TrainerPackSnapshot(
        String trainerId,
        String type,
        String signatureItem,
        List<Set<String>> requiredSeries,
        Set<String> substitutes,
        int maxTrainerWins,
        int maxTrainerDefeats,
        int battleCooldownTicks,
        float spawnWeightFactor,
        Set<String> biomeTagBlacklist,
        Set<String> biomeTagWhitelist,
        Boolean forceBattleOnSight,
        Float forceBattleMaxDistance,
        Integer forceBattleLookTicks,
        Integer forceBattleMaxLevelDiff,
        int maxTeamLevel
) {
}
