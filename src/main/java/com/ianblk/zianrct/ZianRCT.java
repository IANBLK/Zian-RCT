package com.ianblk.zianrct;

import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.config.ZianRctConfigLoader;
import com.ianblk.zianrct.rct.RctPackController;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(ZianRCT.MOD_ID)
public final class ZianRCT {
    public static final String MOD_ID = "zianrct";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final RctPackController rctPackController;

    public ZianRCT(IEventBus modEventBus, ModContainer container) {
        ZianRctConfig initialConfig = loadInitialSnapshot();
        ConfigState.replace(initialConfig);
        this.rctPackController = new RctPackController(modEventBus);
        LOGGER.info("Zian RCT initialized with profile '{}'", initialConfig.activeProfile());
    }

    private static ZianRctConfig loadInitialSnapshot() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(ZianRctConfigLoader.FILE_NAME);

        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            try {
                return ZianRctConfigLoader.loadOrCreate(configPath);
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException(
                        "No se pudo cargar una configuración válida de Zian RCT desde " + configPath,
                        exception
                );
            }
        }

        if (Files.notExists(configPath)) {
            LOGGER.info(
                    "No local {} found on client bootstrap; using built-in defaults. Remote server snapshots will use ClientConfigState.",
                    ZianRctConfigLoader.FILE_NAME
            );
            return ZianRctConfig.defaults();
        }

        try {
            ZianRctConfig local = ZianRctConfigLoader.readExisting(configPath);
            LOGGER.info("Loaded local {} for integrated-server/LAN use.", ZianRctConfigLoader.FILE_NAME);
            return local;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn(
                    "Could not read local {} on client bootstrap; using defaults. The file was not modified.",
                    configPath,
                    exception
            );
            return ZianRctConfig.defaults();
        }
    }
}
