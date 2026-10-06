package com.ianblk.zianrct.reward;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Narrow reflection boundary for the inspected AVECOINS wallet contract. */
final class ReflectiveAvecoinsWallet implements AvecoinsWallet.Access {
    private static final long SLOT_COUNT = 27;
    private static final long STACK_SIZE = 64;
    private static final long MAX_BALANCE = SLOT_COUNT * STACK_SIZE;

    private Set<String> currencies;
    private Class<?> storeClass;
    private Method get;
    private Method save;
    private Method copy;
    private Method balance;
    private Method balances;
    private Method credit;
    private Method debit;

    ReflectiveAvecoinsWallet() {
        var probe = AvecoinsContractProbe.inspect();
        if (!probe.compatible()) throw new IllegalStateException(probe.detail());
        try {
            Class<?> store = Class.forName("net.sundggs.avecoins.shop.WalletStore");
            Class<?> data = Class.forName("net.sundggs.avecoins.shop.WalletData");
            initialize(store, data, probe.currencies());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("AVECOINS wallet binding failed", error);
        }
    }

    /** Testable without loading AVECOINS or starting Minecraft. */
    ReflectiveAvecoinsWallet(Class<?> crafting, Class<?> store, Class<?> data) {
        try {
            var probe = AvecoinsContractProbe.inspectLayout(crafting, store, data);
            if (!probe.compatible()) throw new IllegalStateException(probe.detail());
            initialize(store, data, probe.currencies());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("AVECOINS wallet binding failed", error);
        }
    }

    private void initialize(Class<?> store, Class<?> data, java.util.List<String> managed)
        throws ReflectiveOperationException {
        currencies = Set.copyOf(managed);
        storeClass = store;
        get = store.getMethod("get");
        save = store.getMethod("save", data);
        copy = data.getMethod("copy");
        balance = data.getMethod("balance", UUID.class, String.class);
        balances = data.getMethod("balances", UUID.class);
        credit = data.getMethod("credit", UUID.class, String.class, long.class);
        debit = data.getMethod("debit", UUID.class, String.class, long.class);
    }

    @Override
    public Set<String> currencies() {
        return currencies;
    }

    @Override
    public long balance(UUID playerId, String currencyId) throws Exception {
        synchronized (storeClass) {
            return (long) balance.invoke(requireWallet(), playerId, currencyId);
        }
    }

    @Override
    public AvecoinsWallet.Mutation credit(UUID playerId, String currencyId, long amount)
        throws Exception {
        synchronized (storeClass) {
            Object current = requireWallet();
            if (capacity(current, playerId, currencyId) < amount) {
                return AvecoinsWallet.Mutation.WALLET_FULL;
            }
            Object candidate = copy.invoke(current);
            credit.invoke(candidate, playerId, currencyId, amount);
            if (!Boolean.TRUE.equals(save.invoke(null, candidate))) {
                throw new IllegalStateException("AVECOINS save unconfirmed");
            }
            return AvecoinsWallet.Mutation.APPLIED;
        }
    }

    @Override
    public AvecoinsWallet.Mutation debit(UUID playerId, String currencyId, long amount)
        throws Exception {
        synchronized (storeClass) {
            Object candidate = copy.invoke(requireWallet());
            if (!Boolean.TRUE.equals(debit.invoke(candidate, playerId, currencyId, amount))) {
                return AvecoinsWallet.Mutation.INSUFFICIENT_FUNDS;
            }
            if (!Boolean.TRUE.equals(save.invoke(null, candidate))) {
                throw new IllegalStateException("AVECOINS save unconfirmed");
            }
            return AvecoinsWallet.Mutation.APPLIED;
        }
    }

    private Object requireWallet() throws Exception {
        Object value = get.invoke(null);
        if (value == null) throw new IllegalStateException("AVECOINS wallet unavailable");
        return value;
    }

    private long capacity(Object current, UUID playerId, String currencyId) throws Exception {
        Object raw = balances.invoke(current, playerId);
        if (!(raw instanceof Map<?, ?> values)) {
            throw new IllegalStateException("AVECOINS balances unavailable");
        }
        long otherSlots = 0;
        long currentAmount = 0;
        for (var entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof Long value)
                || value < 0 || value > MAX_BALANCE) {
                throw new IllegalStateException("AVECOINS balance layout invalid");
            }
            if (key.equals(currencyId)) currentAmount = value;
            else otherSlots = Math.addExact(otherSlots, (value + STACK_SIZE - 1) / STACK_SIZE);
        }
        return Math.max(0, (SLOT_COUNT - otherSlots) * STACK_SIZE - currentAmount);
    }
}

