package com.ianblk.zianrct.config;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ConfigState {
    private static final AtomicReference<ZianRctConfig> CURRENT = new AtomicReference<>();

    private ConfigState() {
    }

    public static ZianRctConfig current() {
        ZianRctConfig config = CURRENT.get();
        if (config == null) {
            throw new IllegalStateException("Zian RCT config has not been initialized yet");
        }
        return config;
    }

    public static void replace(ZianRctConfig config) {
        CURRENT.set(Objects.requireNonNull(config, "config"));
    }
}
