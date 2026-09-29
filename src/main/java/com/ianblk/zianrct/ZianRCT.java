package com.ianblk.zianrct;

import com.ianblk.zianrct.config.ZianRctConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ZianRCT.MOD_ID)
public final class ZianRCT {
    public static final String MOD_ID = "zianrct";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final ZianRctConfig config;

    public ZianRCT() {
        this.config = ZianRctConfig.load();
        LOGGER.info("Zian RCT initialized with config: {}", config);
    }

    public ZianRctConfig config() {
        return config;
    }
}
