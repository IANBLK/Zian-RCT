package com.ianblk.zianrct.rct;

import com.ianblk.zianrct.config.ZianRctConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RctProgressPlannerTest {
    @Test
    void reachableCapsAreDistinctAndOrdered() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();

        assertEquals(
                List.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 100),
                RctProgressPlanner.reachableCaps(profile)
        );
    }

    @Test
    void cap100StopsAtGlaciusAndDoesNotRequireAurelia() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();

        List<String> prefix = RctProgressPlanner.desiredDefeatedPrefix(profile, 100);

        assertEquals(9, prefix.size());
        assertEquals("rassvet_leader_glacius", prefix.getLast());
    }

    @Test
    void nextAndPreviousCapUseConfiguredDistinctSteps() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();

        assertEquals(20, RctProgressPlanner.nextCap(profile, 10));
        assertEquals(90, RctProgressPlanner.previousCap(profile, 100));
        assertThrows(IllegalArgumentException.class, () -> RctProgressPlanner.previousCap(profile, 10));
        assertThrows(IllegalArgumentException.class, () -> RctProgressPlanner.nextCap(profile, 100));
    }

    @Test
    void configuredDefeatsPreserveChainOrder() {
        ZianRctConfig.Profile profile = ZianRctConfig.defaults().activeProfileConfig();

        assertEquals(
                List.of("rassvet_leader_novato", "rassvet_leader_aquila"),
                RctProgressPlanner.configuredDefeatsInOrder(
                        profile,
                        Set.of("rassvet_leader_aquila", "rassvet_leader_novato", "outside")
                )
        );
    }
}
