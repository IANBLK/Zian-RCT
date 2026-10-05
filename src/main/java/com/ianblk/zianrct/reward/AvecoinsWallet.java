package com.ianblk.zianrct.reward;

import java.util.Set;
import java.util.UUID;

/** Independent copy of the verified Zian Utilities 2.3/2.4 wallet boundary; no Utilities dependency. */
final class AvecoinsWallet {
    private final Access access;
    private AvecoinsWallet(Access access) { this.access = access; }
    static AvecoinsWallet bind() { return new AvecoinsWallet(new ReflectiveAvecoinsWallet()); }
    boolean currency(String currency) { return access.currencies().contains(currency); }
    RewardDelivery.Result credit(UUID player, String currency, long amount) throws Exception {
        return switch (access.credit(player, currency, amount)) {
            case APPLIED -> RewardDelivery.Result.APPLIED;
            case WALLET_FULL -> RewardDelivery.Result.DEFERRED;
            case INSUFFICIENT_FUNDS -> RewardDelivery.Result.UNCERTAIN;
        };
    }
    interface Access {
        Set<String> currencies();
        long balance(UUID player, String currency) throws Exception;
        Mutation credit(UUID player, String currency, long amount) throws Exception;
        Mutation debit(UUID player, String currency, long amount) throws Exception;
    }
    enum Mutation { APPLIED, WALLET_FULL, INSUFFICIENT_FUNDS }
}
