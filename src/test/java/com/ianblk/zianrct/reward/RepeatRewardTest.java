package com.ianblk.zianrct.reward;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RepeatRewardTest {
    @TempDir Path temp;
    @org.junit.jupiter.api.AfterEach void cleanup() throws Exception { RewardTestFiles.cleanup(temp); }
    private final UUID player=UUID.randomUUID();
    private RewardDefinition repeat(long minutes){return new RewardDefinition("avecoins:coppercoin",5,List.of(),RewardDefinition.Mode.REPEAT,minutes);}
    private void finish(RewardJournal journal,UUID id) throws Exception {
        RewardDelivery.deliver(journal,player,id,new RewardDelivery.Port(){
            public String unavailable(RewardClaim.Part p){return null;}
            public RewardDelivery.Result apply(RewardClaim.Part p){return RewardDelivery.Result.APPLIED;}
        });
    }
    @Test void cyclesPersistCooldownAndNeverReuseDeliveredIdsOrBattleIds() throws Exception {
        Path path=temp.resolve("journal.json");var journal=RewardJournal.open(path);
        UUID battle=UUID.randomUUID();
        assertTrue(journal.reserveAt(player,"trainer",repeat(1),1000,battle));
        UUID first=journal.latest(player,"trainer").id();assertEquals(RewardJournal.id(player,"trainer"),first);
        finish(journal,first);
        assertFalse(journal.reserveAt(player,"trainer",repeat(1),60999,UUID.randomUUID()));
        assertFalse(journal.reserveAt(player,"trainer",repeat(1),61000,battle));
        var restored=RewardJournal.open(path);
        assertTrue(restored.reserveAt(player,"trainer",repeat(1),61000,UUID.randomUUID()));
        UUID second=restored.latest(player,"trainer").id();assertNotEquals(first,second);
        assertEquals(1,restored.latest(player,"trainer").cycle());
        assertTrue(restored.get(first).complete());
        assertFalse(restored.reserveAt(player,"trainer",repeat(1),900000,UUID.randomUUID()));
        finish(restored,second);
        assertTrue(restored.reserveAt(player,"trainer",repeat(1),121000,UUID.randomUUID()));
        assertEquals(2,restored.latest(player,"trainer").cycle());
        assertEquals(3,RewardJournal.open(path).forPlayer(player).size());
    }
    @Test void editingWaitDoesNotShortenAFrozenExistingInterval() throws Exception {
        var journal=RewardJournal.open(temp.resolve("journal.json"));
        journal.reserveAt(player,"trainer",repeat(60),1000,UUID.randomUUID());
        finish(journal,journal.latest(player,"trainer").id());
        assertFalse(journal.reserveAt(player,"trainer",repeat(1),61000,UUID.randomUUID()));
        assertEquals(3540000,journal.remaining(player,"trainer",repeat(1),61000));
        assertTrue(journal.reserveAt(player,"trainer",repeat(1),3601000,UUID.randomUUID()));
        assertEquals(1,journal.latest(player,"trainer").cooldownMinutes());
    }
    @Test void legacyOneTimeClaimsKeepTheirIdAndRequireExplicitRepeatPolicy() throws Exception {
        Path path=temp.resolve("journal.json");
        UUID id=RewardJournal.id(player,"trainer");
        String old="{\"schemaVersion\":1,\"claims\":[{\"id\":\""+id+"\",\"player\":\""+player+"\",\"trainer\":\"trainer\",\"created\":1000,\"parts\":[{\"kind\":\"COINS\",\"data\":\"avecoins:coppercoin\",\"amount\":5,\"phase\":\"DELIVERED\",\"detail\":\"confirmed\"}]}]}";
        Files.writeString(path,old);
        var journal=RewardJournal.open(path);
        assertFalse(journal.reserve(player,"trainer",new RewardDefinition("avecoins:coppercoin",5,List.of())));
        assertTrue(journal.reserveAt(player,"trainer",repeat(1),2000,UUID.randomUUID()));
        assertTrue(journal.get(id).complete());
        assertEquals(1,journal.latest(player,"trainer").cycle());
        assertEquals(2,RewardJournal.open(path).forPlayer(player).size());
    }
    @Test void uncertainAndPendingPreviousCyclesBlockNewRewards() throws Exception {
        var journal=RewardJournal.open(temp.resolve("journal.json"));
        journal.reserveAt(player,"trainer",repeat(1),1000,UUID.randomUUID());
        assertEquals(-2,journal.remaining(player,"trainer",repeat(1),900000));
        assertFalse(journal.reserveAt(player,"trainer",repeat(1),900000,UUID.randomUUID()));
        var id=journal.latest(player,"trainer").id();
        journal.phase(id,0,RewardClaim.Phase.APPLYING,"intent");
        assertFalse(journal.reserveAt(player,"trainer",repeat(1),900000,UUID.randomUUID()));
        assertTrue(journal.get(id).review());
    }
    @Test void legacyConfigDefaultsUniqueAndEditingPrizePreservesRepeatMode() throws Exception {
        Path path=temp.resolve("config.json");
        Files.writeString(path,"{\"schemaVersion\":1,\"enabled\":true,\"trainers\":{\"trainer\":{\"currency\":\"avecoins:coppercoin\",\"coins\":5,\"items\":[]}}}");
        var config=TrainerRewardConfig.open(path);
        assertEquals(RewardDefinition.Mode.UNIQUE,config.definition("trainer").mode());
        config.set("trainer",repeat(10).prize("avecoins:coppercoin",9,List.of("{}")));
        var restored=TrainerRewardConfig.open(path);
        assertEquals(RewardDefinition.Mode.REPEAT,restored.definition("trainer").mode());
        assertEquals(10,restored.definition("trainer").cooldownMinutes());
        assertEquals(9,restored.definition("trainer").coins());
    }
    @Test void repeatRequiresBoundedNonzeroWaitAndBattleEvidence() throws Exception {
        assertThrows(IllegalArgumentException.class,()->repeat(0));
        assertThrows(IllegalArgumentException.class,()->repeat(43201));
        var journal=RewardJournal.open(temp.resolve("journal.json"));
        assertThrows(IllegalArgumentException.class,()->journal.reserve(player,"trainer",repeat(1)));
    }
}
