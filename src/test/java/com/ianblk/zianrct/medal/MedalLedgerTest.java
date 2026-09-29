package com.ianblk.zianrct.medal;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedalLedgerTest {
    @Test
    void grantIsIdempotentAndKeepsOriginalRecord() {
        MedalLedger ledger = new MedalLedger();
        UUID player = UUID.randomUUID();
        MedalRecord first = new MedalRecord("novato", 1000L, MedalOrigin.BATTLE);
        MedalRecord duplicate = new MedalRecord("novato", 2000L, MedalOrigin.COMMAND);

        assertTrue(ledger.grantIfAbsent(player, first, true));
        assertFalse(ledger.grantIfAbsent(player, duplicate, true));
        assertEquals(first, ledger.find(player, "novato").orElseThrow());
        assertEquals(1, ledger.medals(player).size());
    }

    @Test
    void revokePersistsAsTombstoneAndFreshGrantClearsIt() {
        MedalLedger ledger = new MedalLedger();
        UUID player = UUID.randomUUID();
        ledger.grantIfAbsent(player, new MedalRecord("novato", 1000L, MedalOrigin.BATTLE), true);
        ledger.grantIfAbsent(player, new MedalRecord("ferrum", 2000L, MedalOrigin.BATTLE), true);

        assertTrue(ledger.revoke(player, "novato"));
        assertFalse(ledger.find(player, "novato").isPresent());
        assertTrue(ledger.find(player, "ferrum").isPresent());
        assertTrue(ledger.isRevoked(player, "novato"));
        assertFalse(ledger.grantIfAbsent(
                player,
                new MedalRecord("novato", 3000L, MedalOrigin.RECONCILED),
                false
        ));
        assertTrue(ledger.grantIfAbsent(
                player,
                new MedalRecord("novato", 4000L, MedalOrigin.COMMAND),
                true
        ));
        assertFalse(ledger.isRevoked(player, "novato"));
    }
}
