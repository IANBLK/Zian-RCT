package com.ianblk.zianrct.reward;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class TrainerRewardTest {
    @TempDir Path temp;
    @org.junit.jupiter.api.AfterEach void cleanup() throws Exception { RewardTestFiles.cleanup(temp); }
    private final UUID player = UUID.randomUUID();
    private final String trainer = "rassvet_leader_novato";
    private RewardDefinition itemReward() { return new RewardDefinition("", 0, List.of("{id:\"minecraft:diamond\",count:1}")); }
    private RewardDelivery.Port port(AtomicInteger calls, RewardDelivery.Result result) {
        return new RewardDelivery.Port() {
            public String unavailable(RewardClaim.Part part) { return null; }
            public RewardDelivery.Result apply(RewardClaim.Part part) { calls.incrementAndGet(); return result; }
        };
    }
    @Test void rewardIsOncePerPlayerAndTrainerAndFrozenAcrossConfigChangesAndRestarts() throws Exception {
        Path path = temp.resolve("journal.json");
        var journal = RewardJournal.open(path);
        assertTrue(journal.reserve(player, trainer, itemReward()));
        assertFalse(journal.reserve(player, trainer, new RewardDefinition("avecoins:goldcoin", 99, List.of())));
        assertEquals(RewardClaim.Kind.ITEM, journal.get(RewardJournal.id(player, trainer)).parts().getFirst().kind());
        AtomicInteger calls = new AtomicInteger();
        RewardDelivery.deliver(journal, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED));
        var restored = RewardJournal.open(path);
        assertFalse(restored.reserve(player, trainer, itemReward()));
        RewardDelivery.deliver(restored, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED));
        assertEquals(1, calls.get());
        assertTrue(restored.reserve(UUID.randomUUID(), trainer, itemReward()));
    }
    @Test void inventoryFullNeverStartsDeliveryAndCanBeClaimedLater() throws Exception {
        var journal = RewardJournal.open(temp.resolve("journal.json"));
        journal.reserve(player, trainer, itemReward());
        AtomicInteger calls = new AtomicInteger();
        RewardDelivery.deliver(journal, player, RewardJournal.id(player, trainer), new RewardDelivery.Port() {
            public String unavailable(RewardClaim.Part part) { return "inventory_full"; }
            public RewardDelivery.Result apply(RewardClaim.Part part) { fail("No mutation while full"); return null; }
        });
        assertEquals(RewardClaim.Phase.PENDING, journal.get(RewardJournal.id(player, trainer)).parts().getFirst().phase());
        RewardDelivery.deliver(journal, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED));
        assertEquals(1, calls.get());
    }
    @Test void partialRewardsSkipPaidComponentsAndRetryOnlyDeterministicallyDeferredCredits() throws Exception {
        Path path = temp.resolve("journal.json");
        var journal = RewardJournal.open(path);
        journal.reserve(player, trainer, new RewardDefinition("avecoins:coppercoin", 5, itemReward().items()));
        AtomicInteger items = new AtomicInteger(), coins = new AtomicInteger();
        var first = new RewardDelivery.Port() {
            public String unavailable(RewardClaim.Part part) { return null; }
            public RewardDelivery.Result apply(RewardClaim.Part part) {
                if (part.kind() == RewardClaim.Kind.ITEM) { items.incrementAndGet(); return RewardDelivery.Result.APPLIED; }
                coins.incrementAndGet(); return RewardDelivery.Result.DEFERRED;
            }
        };
        RewardDelivery.deliver(journal, player, RewardJournal.id(player, trainer), first);
        var restored = RewardJournal.open(path);
        RewardDelivery.deliver(restored, player, RewardJournal.id(player, trainer), new RewardDelivery.Port() {
            public String unavailable(RewardClaim.Part part) { return null; }
            public RewardDelivery.Result apply(RewardClaim.Part part) {
                assertEquals(RewardClaim.Kind.COINS, part.kind());
                coins.incrementAndGet(); return RewardDelivery.Result.APPLIED;
            }
        });
        assertEquals(1, items.get()); assertEquals(2, coins.get());
        assertTrue(restored.get(RewardJournal.id(player, trainer)).complete());
    }
    @Test void unconfirmedExternalSaveBlocksRetryAcrossRestartAndRequiresEvidence() throws Exception {
        Path path = temp.resolve("journal.json");
        var journal = RewardJournal.open(path);
        journal.reserve(player, trainer, itemReward());
        AtomicInteger calls = new AtomicInteger();
        RewardDelivery.deliver(journal, player, RewardJournal.id(player, trainer), new RewardDelivery.Port() {
            public String unavailable(RewardClaim.Part part) { return null; }
            public RewardDelivery.Result apply(RewardClaim.Part part) { calls.incrementAndGet(); throw new IllegalStateException("save unconfirmed"); }
        });
        var restored = RewardJournal.open(path);
        RewardDelivery.deliver(restored, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED));
        assertEquals(1, calls.get());
        assertTrue(restored.get(RewardJournal.id(player, trainer)).review());
        assertThrows(IllegalArgumentException.class, () -> restored.resolve(player, RewardJournal.id(player, trainer), 0, player, "confirmed_delivered", ""));
        restored.resolve(player, RewardJournal.id(player, trainer), 0, UUID.randomUUID(), "confirmed_compensated", "verified-inventory-and-manual-delivery");
        RewardDelivery.deliver(restored, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED));
        assertEquals(1, calls.get());
    }
    @Test void interruptedApplyingIntentIsNotRetriedOrReopened() throws Exception {
        Path path = temp.resolve("journal.json");
        var journal = RewardJournal.open(path);
        journal.reserve(player, trainer, itemReward());
        journal.phase(RewardJournal.id(player, trainer), 0, RewardClaim.Phase.APPLYING, "intent");
        var restored = RewardJournal.open(path);
        AtomicInteger calls = new AtomicInteger();
        RewardDelivery.deliver(restored, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED));
        assertEquals(0, calls.get());
        assertThrows(IllegalArgumentException.class, () -> restored.resolve(UUID.randomUUID(), RewardJournal.id(player, trainer), 0, player, "confirmed_delivered", "evidence"));
        restored.resolve(player, RewardJournal.id(player, trainer), 0, player, "confirmed_delivered", "verified");
        assertThrows(IllegalArgumentException.class, () -> restored.phase(RewardJournal.id(player, trainer), 0, RewardClaim.Phase.PENDING, "reset"));
    }
    @Test void claimsCannotBeTakenByAnotherPlayerOrResolvedWhilePending() throws Exception {
        var journal = RewardJournal.open(temp.resolve("journal.json"));
        journal.reserve(player, trainer, itemReward());
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> RewardDelivery.deliver(journal, UUID.randomUUID(), RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED)));
        assertThrows(IllegalArgumentException.class, () -> journal.resolve(player, RewardJournal.id(player, trainer), 0, player, "confirmed_delivered", "evidence"));
        assertEquals(0, calls.get());
    }
    @Test void duplicateDeliveryRequestsAreSerialized() throws Exception {
        var journal = RewardJournal.open(temp.resolve("journal.json"));
        journal.reserve(player, trainer, itemReward());
        AtomicInteger calls = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(pool.submit(() -> {
                try { RewardDelivery.deliver(journal, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED)); }
                catch (IOException e) { throw new RuntimeException(e); }
            }), pool.submit(() -> {
                try { RewardDelivery.deliver(journal, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED)); }
                catch (IOException e) { throw new RuntimeException(e); }
            }));
            for (var task : tasks) task.get(5, TimeUnit.SECONDS);
        }
        assertEquals(1, calls.get());
    }
    @Test void failedIntentPersistenceNeverCallsExternalDelivery() throws Exception {
        Path path = temp.resolve("journal.json");
        var journal = RewardJournal.open(path);
        journal.reserve(player, trainer, itemReward());
        Files.delete(path); Files.createDirectory(path);
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IOException.class, () -> RewardDelivery.deliver(journal, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED)));
        assertThrows(IOException.class, () -> RewardDelivery.deliver(journal, player, RewardJournal.id(player, trainer), port(calls, RewardDelivery.Result.APPLIED)));
        assertEquals(0, calls.get());
    }
    @Test void corruptJournalIsPreservedAndCannotBeOpenedAsEmpty() throws Exception {
        Path path = temp.resolve("journal.json");
        Files.writeString(path, "{invalid");
        assertThrows(IOException.class, () -> RewardJournal.open(path));
        assertEquals("{invalid", Files.readString(path));
    }
    @Test void configReloadAndPersistenceDoNotOverwriteFrozenClaims() throws Exception {
        Path path = temp.resolve("config.json");
        var config = TrainerRewardConfig.open(path);
        assertTrue(config.enabled()); assertTrue(config.definitions().isEmpty());
        config.set(trainer, itemReward()); config.enabled(false);
        var reopened = TrainerRewardConfig.open(path);
        assertFalse(reopened.enabled()); assertEquals(itemReward(), reopened.definition(trainer));
        Files.writeString(path, "{}");
        assertThrows(IOException.class, reopened::reload);
        assertEquals(itemReward(), reopened.definition(trainer));
    }
    @Test void configRejectsFractionalAmountsUnknownFieldsAndInvalidItems() throws Exception {
        String json = "{\"schemaVersion\":1,\"enabled\":true,\"trainers\":{\"novato\":{\"currency\":\"avecoins:coppercoin\",\"coins\":1.5,\"items\":[]}}}";
        assertThrows(IOException.class, () -> TrainerRewardConfig.parse(com.google.gson.JsonParser.parseString(json)));
        assertThrows(IllegalArgumentException.class, () -> new RewardDefinition("", 4, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RewardDefinition("", 0, Collections.nCopies(9, "{}")));
        assertThrows(IllegalArgumentException.class, () -> new RewardDefinition("", 0, List.of("x".repeat(65537))));
    }
    @Test void oversizedConfigIsRejectedBeforeReplacingTheWorkingFile() throws Exception {
        Path path = temp.resolve("config.json");
        var config = TrainerRewardConfig.open(path);
        var large = new RewardDefinition("", 0, Collections.nCopies(8, "x".repeat(65536)));
        config.set("first", large);
        byte[] previous = Files.readAllBytes(path);
        assertThrows(IOException.class, () -> config.set("second", large));
        assertArrayEquals(previous, Files.readAllBytes(path));
        assertEquals(1, config.definitions().size());
    }
}
