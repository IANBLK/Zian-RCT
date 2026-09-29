package com.ianblk.zianrct;

import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.config.ZianRctConfigLoader;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Mod(ZianRCT.MOD_ID)
public final class ZianRCT {
    public static final String MOD_ID = "zianrct";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ZianRCT(IEventBus modEventBus, ModContainer container) {
        ZianRctConfig initialConfig = loadInitialSnapshot();
        ConfigState.replace(initialConfig);
        LOGGER.info("Zian RCT initialized with profile '{}'", initialConfig.activeProfile());
    }

    private static ZianRctConfig loadInitialSnapshot() {
        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            LOGGER.info("Client bootstrap uses built-in defaults only; authoritative Zian RCT config will come from the server.");
            return ZianRctConfig.defaults();
        }

        Path configPath = FMLPaths.CONFIGDIR.get().resolve(ZianRctConfigLoader.FILE_NAME);
        try {
            return ZianRctConfigLoader.loadOrCreate(configPath);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("No se pudo cargar una configuración válida de Zian RCT desde " + configPath, exception);
        }
    }
}
