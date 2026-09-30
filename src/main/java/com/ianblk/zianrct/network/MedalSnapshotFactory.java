package com.ianblk.zianrct.network;

import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.medal.MedalRecord;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MedalSnapshotFactory {
    private MedalSnapshotFactory() {
    }

    public static MedalClientSnapshot build(ZianRctConfig config, List<MedalRecord> records) {
        ZianRctConfig.Profile profile = config.activeProfileConfig();
        Map<String, Integer> unlockCaps = profile.trainerUnlockCaps();

        List<MedalClientSnapshot.MedalDefinitionView> definitions = profile.medals().stream()
                .filter(medal -> medal != null)
                .sorted(Comparator.comparingInt(ZianRctConfig.MedalDefinition::order))
                .map(medal -> new MedalClientSnapshot.MedalDefinitionView(
                        medal.id(),
                        medal.trainer(),
                        medal.name(),
                        medal.description(),
                        medal.texture(),
                        medal.color(),
                        medal.order(),
                        unlockCaps.getOrDefault(medal.trainer(), profile.maxCap())
                ))
                .toList();

        Set<String> definedIds = new HashSet<>();
        for (MedalClientSnapshot.MedalDefinitionView definition : definitions) {
            definedIds.add(definition.id());
        }

        List<MedalClientSnapshot.OwnedMedalView> owned = records.stream()
                .filter(record -> definedIds.contains(record.medalId()))
                .map(record -> new MedalClientSnapshot.OwnedMedalView(
                        record.medalId(),
                        record.grantedAtEpochMilli(),
                        record.origin().name()
                ))
                .toList();

        return new MedalClientSnapshot(config.activeProfile(), definitions, owned);
    }
}
