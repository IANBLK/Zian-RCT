package com.ianblk.zianrct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LocalizedExceptionTest {
    @Test void messageStaysSpanishAndCarriesKey() {
        var error = new LocalizedException.Argument("zianrct.err.unknown_move", "Movimiento desconocido: %s", "tackle");
        assertEquals("Movimiento desconocido: tackle", error.getMessage());
        assertEquals("zianrct.err.unknown_move", error.msg().key());
        assertEquals("tackle", error.msg().args()[0]);
        assertEquals("No tienes permiso: zianrct.a", new LocalizedException.State("zianrct.err.no_permission", "No tienes permiso: zianrct.%s", "a").getMessage());
    }
}
