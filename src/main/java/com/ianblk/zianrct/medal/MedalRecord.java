package com.ianblk.zianrct.medal;

import java.util.Objects;

public record MedalRecord(
        String medalId,
        long grantedAtEpochMilli,
        MedalOrigin origin
) {
    public MedalRecord {
        if (medalId == null || medalId.isBlank()) {
            throw new IllegalArgumentException("medalId no puede estar vacío");
        }
        if (grantedAtEpochMilli < 0) {
            throw new IllegalArgumentException("grantedAtEpochMilli no puede ser negativo");
        }
        origin = Objects.requireNonNull(origin, "origin");
    }
}
