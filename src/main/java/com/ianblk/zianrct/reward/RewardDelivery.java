package com.ianblk.zianrct.reward;

import java.io.IOException;
import java.util.UUID;

public final class RewardDelivery {
    public enum Result { APPLIED, DEFERRED, UNCERTAIN }
    public interface Port {
        String unavailable(RewardClaim.Part part);
        Result apply(RewardClaim.Part part) throws Exception;
    }
    private RewardDelivery() {}
    public static void deliver(RewardJournal journal, UUID player, UUID id, Port port) throws IOException {
        synchronized (journal) { deliverLocked(journal, player, id, port); }
    }
    private static void deliverLocked(RewardJournal journal, UUID player, UUID id, Port port) throws IOException {
        journal.ensureHealthy();
        var claim = journal.get(id);
        if (claim == null || !claim.player().equals(player)) throw new IllegalArgumentException("Reclamación inexistente para este jugador");
        if (claim.review()) return;
        for (int i = 0; i < claim.parts().size(); i++) {
            var part = journal.get(id).parts().get(i);
            if (part.phase() == RewardClaim.Phase.DELIVERED) continue;
            String unavailable = port.unavailable(part);
            if (unavailable != null) {
                journal.phase(id, i, RewardClaim.Phase.PENDING, unavailable);
                return;
            }
            journal.phase(id, i, RewardClaim.Phase.APPLYING, "external_delivery_started");
            Result result;
            try { result = port.apply(part); }
            catch (Exception | LinkageError error) { result = Result.UNCERTAIN; }
            if (result == Result.APPLIED) {
                journal.phase(id, i, RewardClaim.Phase.DELIVERED, "confirmed");
            } else {
                journal.phase(id, i, result == Result.DEFERRED ? RewardClaim.Phase.PENDING : RewardClaim.Phase.REVIEW_REQUIRED,
                        result == Result.DEFERRED ? "delivery_deferred_without_mutation" : "external_delivery_unconfirmed");
                return;
            }
        }
    }
}
