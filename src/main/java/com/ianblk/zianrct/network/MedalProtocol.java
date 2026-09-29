package com.ianblk.zianrct.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class MedalProtocol {
    public static final int CURRENT_VERSION = 1;
    public static final String NETWORK_VERSION = "1";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private MedalProtocol() {
    }

    public static String encode(MedalClientSnapshot snapshot) {
        return GSON.toJson(snapshot);
    }

    public static MedalClientSnapshot decode(int protocolVersion, String snapshotJson) {
        if (protocolVersion != CURRENT_VERSION) {
            throw new IllegalStateException(
                    "Versión de protocolo de medallas incompatible: " + protocolVersion
                            + " (esperada " + CURRENT_VERSION + ")"
            );
        }
        MedalClientSnapshot snapshot = GSON.fromJson(snapshotJson == null ? "{}" : snapshotJson, MedalClientSnapshot.class);
        if (snapshot == null) {
            throw new IllegalStateException("El snapshot de medallas recibido está vacío");
        }
        return snapshot;
    }
}
