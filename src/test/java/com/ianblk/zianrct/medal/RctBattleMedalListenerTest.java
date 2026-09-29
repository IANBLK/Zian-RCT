package com.ianblk.zianrct.medal;

import com.ianblk.zianrct.config.ZianRctConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RctBattleMedalListenerTest {
    private static final List<ZianRctConfig.ChainEntry> CHAIN = List.of(
            new ZianRctConfig.ChainEntry("rassvet_leader_novato", null),
            new ZianRctConfig.ChainEntry("rassvet_leader_ferrum", null)
    );

    @Test
    void configuredTrainerIdAcceptsOnlyExactChainIds() {
        assertEquals(
                "rassvet_leader_novato",
                RctBattleMedalListener.configuredTrainerId("rassvet_leader_novato", CHAIN)
        );
        assertNull(RctBattleMedalListener.configuredTrainerId("other_trainer", CHAIN));
        assertNull(RctBattleMedalListener.configuredTrainerId("", CHAIN));
        assertNull(RctBattleMedalListener.configuredTrainerId(null, CHAIN));
    }
}
