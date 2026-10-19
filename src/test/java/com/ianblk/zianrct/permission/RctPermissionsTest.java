package com.ianblk.zianrct.permission;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RctPermissionsTest {
    @Test void battleParticipationNeverConsultsPermissionProvider() {
        for (String action : new String[]{"battle", "legendary.claim"}) {
            assertTrue(RctPermissions.evaluate(action, false, false, () -> {
                fail("Public battles must not consult the provider");
                return LuckPermsLookup.Decision.FALSE;
            }));
        }
    }
    @Test void publicBattleNamesCannotBypassAdministrativeChecks() {
        assertFalse(RctPermissions.evaluate("battle", true, false, () -> LuckPermsLookup.Decision.UNDEFINED));
        assertFalse(RctPermissions.evaluate("admin.npc.edit", true, true, () -> LuckPermsLookup.Decision.FALSE));
        assertFalse(RctPermissions.evaluate("admin.legendary.resolve", true, true, () -> {
            throw new IllegalStateException("provider unavailable");
        }));
    }
    @Test void explicitDenialOverridesOperatorDefault() {
        assertFalse(RctPermissions.evaluate(true, () -> LuckPermsLookup.Decision.FALSE));
    }
    @Test void grantAllowsDelegatedAdministrationWithoutOp() {
        assertTrue(RctPermissions.evaluate(false, () -> LuckPermsLookup.Decision.TRUE));
    }
    @Test void absentOrUndefinedProviderPreservesDefaults() {
        for (var decision : new LuckPermsLookup.Decision[] {LuckPermsLookup.Decision.ABSENT, LuckPermsLookup.Decision.UNDEFINED}) {
            assertTrue(RctPermissions.evaluate(true, () -> decision));
            assertFalse(RctPermissions.evaluate(false, () -> decision));
        }
    }
    @Test void unavailableProviderNeverGrantsAccess() {
        assertFalse(RctPermissions.evaluate(true, () -> {throw new IllegalStateException("not ready");}));
        assertFalse(RctPermissions.evaluate(true, () -> {throw new NoClassDefFoundError("missing API");}));
    }
}
