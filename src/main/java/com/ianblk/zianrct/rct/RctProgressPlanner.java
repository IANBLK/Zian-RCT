package com.ianblk.zianrct.rct;

import com.ianblk.zianrct.config.ZianRctConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    public static List<Integer> reachableCaps(ZianRctConfig.Profile profile) {
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        values.add(profile.initialCap());
        for (int index = 0; index < profile.chain().size(); index++) {
            values.add(profile.unlockCap(index));
        }
        return List.copyOf(values);
    }

    public static int nextCap(ZianRctConfig.Profile profile, int currentCap) {
        List<Integer> caps = reachableCaps(profile);
        int index = caps.indexOf(currentCap);
        if (index < 0) {
            throw new IllegalArgumentException("El cap actual " + currentCap + " no pertenece a la progresión configurada");
        }
        if (index + 1 >= caps.size()) {
            throw new IllegalArgumentException("El jugador ya está en el último cap alcanzable");
        }
        return caps.get(index + 1);
    }

    public static int previousCap(ZianRctConfig.Profile profile, int currentCap) {
        List<Integer> caps = reachableCaps(profile);
        int index = caps.indexOf(currentCap);
        if (index < 0) {
            throw new IllegalArgumentException("El cap actual " + currentCap + " no pertenece a la progresión configurada");
        }
        if (index == 0) {
            throw new IllegalArgumentException("El jugador ya está en el cap inicial");
        }
        return caps.get(index - 1);
    }

    public static List<String> configuredDefeatsInOrder(
            ZianRctConfig.Profile profile,
            java.util.Set<String> defeatedIds
    ) {
        List<String> result = new ArrayList<>();
        for (ZianRctConfig.ChainEntry entry : profile.chain()) {
            if (defeatedIds.contains(entry.trainer())) {
                result.add(entry.trainer());
            }
        }
        return List.copyOf(result);
    }
}
