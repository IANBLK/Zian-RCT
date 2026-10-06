package com.ianblk.zianrct.permission;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RctPermissionsTest {
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
