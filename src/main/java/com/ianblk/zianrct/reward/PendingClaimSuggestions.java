package com.ianblk.zianrct.reward;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PendingClaimSuggestions {
    private PendingClaimSuggestions() {}
    public static List<String> ids(List<RewardClaim> claims, UUID player, String prefix) {
        String remaining = prefix.toLowerCase(Locale.ROOT);
        return claims.stream().filter(c -> c.player().equals(player) && !c.complete() && !c.review())
                .map(c -> c.id().toString()).filter(id -> id.startsWith(remaining)).sorted().limit(100).toList();
    }
}
