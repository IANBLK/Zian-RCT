package com.ianblk.zianrct.reward;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TrainerWalletTest {
    @TempDir Path temp;
    @org.junit.jupiter.api.AfterEach void cleanup() throws Exception { RewardTestFiles.cleanup(temp); }
    public static class Crafting { public static final Set<String> MANAGED_RESULTS = Set.of("avecoins:coppercoin", "avecoins:goldticket"); }
    public static class Data {
        public static final int SLOT_COUNT = 27, STACK_SIZE = 64;
        public final Map<UUID, Map<String, Long>> users = new HashMap<>();
        public Data copy() { var result = new Data(); users.forEach((u, b) -> result.users.put(u, new HashMap<>(b))); return result; }
        public long balance(UUID player, String currency) { return balances(player).getOrDefault(currency, 0L); }
        public Map<String, Long> balances(UUID player) { return users.getOrDefault(player, Map.of()); }
        public void credit(UUID player, String currency, long amount) {
            Store.credits.incrementAndGet();
            users.computeIfAbsent(player, p -> new HashMap<>()).merge(currency, amount, Long::sum);
        }
        public boolean debit(UUID player, String currency, long amount) { return false; }
    }
    public static class Store {
        static Data data = new Data();
        static boolean confirmed = true;
        static AtomicInteger credits = new AtomicInteger();
        public static Data get() { return data; }
        public static boolean save(Data candidate) { data = candidate; return confirmed; }
    }
    private ReflectiveAvecoinsWallet reset() {
        Store.data = new Data(); Store.confirmed = true; Store.credits.set(0);
        return new ReflectiveAvecoinsWallet(Crafting.class, Store.class, Data.class);
    }
    @Test void verifiedWalletCreditsACopyAndHonorsCapacityBeforeMutation() throws Exception {
        var wallet = reset(); UUID player = UUID.randomUUID();
        assertEquals(AvecoinsWallet.Mutation.APPLIED, wallet.credit(player, "avecoins:coppercoin", 5));
        assertEquals(5, wallet.balance(player, "avecoins:coppercoin"));
        assertEquals(AvecoinsWallet.Mutation.WALLET_FULL, wallet.credit(player, "avecoins:coppercoin", 1728));
        assertEquals(1, Store.credits.get());
    }
    @Test void otherCurrencyStacksConsumeCapacity() throws Exception {
        var wallet = reset(); UUID player = UUID.randomUUID();
        Store.data.users.put(player, new HashMap<>(Map.of("avecoins:goldticket", 1728L)));
        assertEquals(AvecoinsWallet.Mutation.WALLET_FULL, wallet.credit(player, "avecoins:coppercoin", 1));
        assertEquals(0, Store.credits.get());
    }
    @Test void unconfirmedWalletSaveDoesNotReplayTheCredit() throws Exception {
        var wallet = reset(); UUID player = UUID.randomUUID();
        Store.confirmed = false;
        Path path = temp.resolve("journal.json");
        var journal = RewardJournal.open(path);
        journal.reserve(player, "trainer", new RewardDefinition("avecoins:coppercoin", 5, List.of()));
        var port = new RewardDelivery.Port() {
            public String unavailable(RewardClaim.Part part) { return null; }
            public RewardDelivery.Result apply(RewardClaim.Part part) throws Exception {
                wallet.credit(player, part.data(), part.amount());
                return RewardDelivery.Result.APPLIED;
            }
        };
        RewardDelivery.deliver(journal, player, RewardJournal.id(player, "trainer"), port);
        RewardDelivery.deliver(RewardJournal.open(path), player, RewardJournal.id(player, "trainer"), port);
        assertEquals(5, wallet.balance(player, "avecoins:coppercoin"));
        assertEquals(1, Store.credits.get());
        assertTrue(journal.get(RewardJournal.id(player, "trainer")).review());
    }
    // La compatibilidad ya no depende del numero de version (2.5, 3.0, 4.1...) sino de las firmas usadas.
    public static class NoDebit { public static final int SLOT_COUNT = 27, STACK_SIZE = 64;
        public NoDebit copy() { return this; } public long balance(UUID p, String c) { return 0; }
        public Map<String, Long> balances(UUID p) { return Map.of(); } public void credit(UUID p, String c, long a) { } }
    public static class NoDebitStore { public static NoDebit get() { return null; } public static boolean save(NoDebit d) { return true; } }
    public static class WrongBalance { public static final int SLOT_COUNT = 27, STACK_SIZE = 64;
        public WrongBalance copy() { return this; } public int balance(UUID p, String c) { return 0; }
        public Map<String, Long> balances(UUID p) { return Map.of(); } public void credit(UUID p, String c, long a) { }
        public boolean debit(UUID p, String c, long a) { return false; } }
    public static class WrongStore { public static WrongBalance get() { return null; } public static boolean save(WrongBalance d) { return true; } }
    @Test void anyVersionWithTheFullContractIsAccepted() throws Exception {
        var probe = AvecoinsContractProbe.inspectLayout(Crafting.class, Store.class, Data.class);
        assertTrue(probe.compatible());
        assertEquals(List.of("avecoins:coppercoin", "avecoins:goldticket"), probe.currencies());
    }
    @Test void missingMethodIsRejectedNamingIt() throws Exception {
        var probe = AvecoinsContractProbe.inspectLayout(Crafting.class, NoDebitStore.class, NoDebit.class);
        assertFalse(probe.compatible());
        assertTrue(probe.detail().contains("debit"));
        assertThrows(IllegalStateException.class, () -> new ReflectiveAvecoinsWallet(Crafting.class, NoDebitStore.class, NoDebit.class));
    }
    @Test void changedSignatureIsRejected() throws Exception {
        // balance(UUID,String) devuelve int en vez de long: puede ser un contrato distinto.
        var probe = AvecoinsContractProbe.inspectLayout(Crafting.class, WrongStore.class, WrongBalance.class);
        assertFalse(probe.compatible());
    }
}
