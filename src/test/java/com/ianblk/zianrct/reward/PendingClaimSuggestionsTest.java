package com.ianblk.zianrct.reward;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PendingClaimSuggestionsTest {
    private RewardClaim claim(UUID player, RewardClaim.Phase phase) {
        return new RewardClaim(UUID.randomUUID(),player,"trainer",1,List.of(new RewardClaim.Part(RewardClaim.Kind.COINS,
                "avecoins:coppercoin",5,phase,"")));
    }
    @Test void onlyOwnActionablePendingIdsAreSuggested() {
        UUID owner=UUID.randomUUID(),other=UUID.randomUUID();
        var pending=claim(owner,RewardClaim.Phase.PENDING);
        var paid=claim(owner,RewardClaim.Phase.DELIVERED);
        var interrupted=claim(owner,RewardClaim.Phase.APPLYING);
        var review=claim(owner,RewardClaim.Phase.REVIEW_REQUIRED);
        var foreign=claim(other,RewardClaim.Phase.PENDING);
        assertEquals(List.of(pending.id().toString()),PendingClaimSuggestions.ids(List.of(pending,paid,interrupted,review,foreign),owner,""));
    }
    @Test void suggestionsHonorThePrefixAndDoNotChangeDeliveryState() {
        UUID owner=UUID.randomUUID();
        var pending=claim(owner,RewardClaim.Phase.PENDING);
        String id=pending.id().toString();
        assertEquals(List.of(id),PendingClaimSuggestions.ids(List.of(pending),owner,id.substring(0,8).toUpperCase(Locale.ROOT)));
        assertTrue(PendingClaimSuggestions.ids(List.of(pending),owner,"not-a-match").isEmpty());
        assertEquals(RewardClaim.Phase.PENDING,pending.parts().getFirst().phase());
    }
}
