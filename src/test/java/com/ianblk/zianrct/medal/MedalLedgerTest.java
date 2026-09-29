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

        assertTrue(ledger.grantIfAbsent(player, first));
        assertFalse(ledger.grantIfAbsent(player, duplicate));
        assertEquals(first, ledger.find(player, "novato").orElseThrow());
        assertEquals(1, ledger.medals(player).size());
    }

    @Test
    void revokeRemovesOnlyRequestedMedal() {
        MedalLedger ledger = new MedalLedger();
        UUID player = UUID.randomUUID();
        ledger.grantIfAbsent(player, new MedalRecord("novato", 1000L, MedalOrigin.BATTLE));
        ledger.grantIfAbsent(player, new MedalRecord("ferrum", 2000L, MedalOrigin.BATTLE));

        assertTrue(ledger.revoke(player, "novato"));
        assertFalse(ledger.find(player, "novato").isPresent());
        assertTrue(ledger.find(player, "ferrum").isPresent());
        assertFalse(ledger.revoke(player, "novato"));
    }
}
