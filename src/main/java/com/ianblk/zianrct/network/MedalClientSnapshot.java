package com.ianblk.zianrct.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MedalClientSnapshot(
        String activeProfile,
        List<MedalDefinitionView> definitions,
        List<OwnedMedalView> owned
) {
    public MedalClientSnapshot {
        activeProfile = activeProfile == null ? "" : activeProfile;
        definitions = definitions == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(definitions));
        owned = owned == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(owned));
    }

    public Map<String, OwnedMedalView> ownedById() {
        LinkedHashMap<String, OwnedMedalView> result = new LinkedHashMap<>();
        for (OwnedMedalView medal : owned) {
            if (medal != null && medal.medalId() != null) {
                result.put(medal.medalId(), medal);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public record MedalDefinitionView(
            String id,
            String trainer,
            String trainerName,
            String name,
            String description,
            String texture,
            String color,
            int order,
            int unlockCap
    ) {
    }

    public record OwnedMedalView(
            String medalId,
            long grantedAtEpochMilli,
            String origin
    ) {
    }
}
