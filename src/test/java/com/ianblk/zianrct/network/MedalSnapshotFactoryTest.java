package com.ianblk.zianrct.network;

import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.medal.MedalOrigin;
import com.ianblk.zianrct.medal.MedalRecord;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MedalSnapshotFactoryTest {
    @Test
    void orphanedOwnedMedalsAreFilteredOutOfSnapshot() {
        MedalClientSnapshot snapshot = MedalSnapshotFactory.build(
                ZianRctConfig.defaults(),
                List.of(
                        new MedalRecord("novato", 100L, MedalOrigin.COMMAND),
                        new MedalRecord("medalla_eliminada", 200L, MedalOrigin.BATTLE)
                )
        );

        assertEquals(1, snapshot.owned().size());
        assertEquals("novato", snapshot.owned().getFirst().medalId());
        assertEquals("Novato", snapshot.definitions().getFirst().trainerName());
        MedalProtocol.validateSnapshot(snapshot);
    }

    @Test
    void missingTrainerNameFallsBackToReadableTrainerId() {
        ZianRctConfig defaults = ZianRctConfig.defaults();
        ZianRctConfig.Profile base = defaults.activeProfileConfig();
        ZianRctConfig.MedalDefinition first = base.medals().getFirst();
        ZianRctConfig.MedalDefinition withoutName = new ZianRctConfig.MedalDefinition(
                first.id(), first.trainer(), null, first.name(), first.description(), first.texture(), first.color(), first.order()
        );
        var medals = new java.util.ArrayList<>(base.medals());
        medals.set(0, withoutName);
        var profiles = new LinkedHashMap<>(defaults.profiles());
        profiles.put("rassvet", new ZianRctConfig.Profile(
                base.initialCap(), base.step(), base.maxCap(), base.series(), base.chain(), medals, base.giveMedalItem()
        ));
        ZianRctConfig config = new ZianRctConfig(defaults.schemaVersion(), defaults.activeProfile(), profiles, defaults.messages());

        MedalClientSnapshot snapshot = MedalSnapshotFactory.build(config, List.of());
        assertEquals("Novato", snapshot.definitions().getFirst().trainerName());
    }
}
