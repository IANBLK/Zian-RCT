package com.ianblk.zianrct.rct;

import static org.junit.jupiter.api.Assertions.*;

import com.ianblk.zianrct.LocalizedException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ReloadGateTest {
    @Test void invalidStageLeavesStateUntouched() {
        var live = new AtomicReference<>("viejo");
        var gate = new ReloadGate();
        assertThrows(IllegalStateException.class, () -> gate.run(() -> { throw new IllegalStateException("archivo inválido"); }, live::set));
        assertEquals("viejo", live.get());
        assertFalse(gate.isRunning());
    }

    @Test void validStageSwapsState() {
        var live = new AtomicReference<>("viejo");
        var gate = new ReloadGate();
        gate.run(() -> "nuevo", live::set);
        assertEquals("nuevo", live.get());
    }

    @Test void oneReloadAtATimeAndFailureReleasesGate() {
        var gate = new ReloadGate();
        var error = assertThrows(LocalizedException.State.class, () -> gate.run(() -> { gate.run(() -> 1, x -> {}); return 1; }, x -> {}));
        assertEquals("zianrct.err.reload_running", error.msg().key());
        assertFalse(gate.isRunning());
        gate.run(() -> 1, x -> {});
    }
}
