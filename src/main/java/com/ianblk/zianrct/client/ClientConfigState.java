package com.ianblk.zianrct.client;

import com.ianblk.zianrct.config.ZianRctConfig;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class ClientConfigState {
    private static final AtomicReference<ZianRctConfig> REMOTE = new AtomicReference<>();

    private ClientConfigState() {
    }

    public static Optional<ZianRctConfig> currentRemote() {
        return Optional.ofNullable(REMOTE.get());
    }

    public static void replaceRemote(ZianRctConfig config) {
        REMOTE.set(config);
    }

    public static void clearRemote() {
        REMOTE.set(null);
    }
}
