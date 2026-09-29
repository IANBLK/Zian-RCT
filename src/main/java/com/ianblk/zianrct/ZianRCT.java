package com.ianblk.zianrct;

import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ZianRCT.MOD_ID)
public final class ZianRCT {
    public static final String MOD_ID = "zianrct";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ZianRCT(IEventBus modEventBus, ModContainer container) {
        ZianRctConfig initialConfig = ZianRctConfig.load();
        ConfigState.replace(initialConfig);
        LOGGER.info("Zian RCT initialized with config: {}", initialConfig);
    }
}
