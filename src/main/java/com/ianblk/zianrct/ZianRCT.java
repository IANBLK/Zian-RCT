package com.ianblk.zianrct;

import com.ianblk.zianrct.battle.LeagueBattlePromptService;
import com.ianblk.zianrct.client.ZianRctClient;
import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.config.ZianRctConfigLoader;
import com.ianblk.zianrct.medal.MedalRuntime;
import com.ianblk.zianrct.network.ZianRctNetwork;
import com.ianblk.zianrct.rct.RctPackController;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Mod(ZianRCT.MOD_ID)
public final class ZianRCT {
    public static final String MOD_ID = "zianrct";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final RctPackController rctPackController;
    private final MedalRuntime medalRuntime;
    private final LeagueBattlePromptService battlePromptService;

    public ZianRCT(IEventBus modEventBus, ModContainer container) {
        com.ianblk.zianrct.creator.CustomTrainerEntities.register(modEventBus);
        com.ianblk.zianrct.standalone.ExperienceCaps.register();
        ZianRctConfig bootstrapDefaults = ZianRctConfig.defaults();
        ConfigState.replace(bootstrapDefaults);
        modEventBus.addListener(ZianRctNetwork::registerPayloads);
        this.rctPackController = new RctPackController(modEventBus);
        this.medalRuntime = new MedalRuntime(rctPackController);
        this.battlePromptService = new LeagueBattlePromptService();
        new com.ianblk.zianrct.standalone.StandaloneSmoke();
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ZianRctClient.init(modEventBus);
        }
        LOGGER.info("Zian RCT initialized with profile '{}'", bootstrapDefaults.activeProfile());
    }

    public RctPackController rctPackController() {
        return rctPackController;
    }

    public MedalRuntime medalRuntime() {
        return medalRuntime;
    }

    public LeagueBattlePromptService battlePromptService() {
        return battlePromptService;
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        try{com.ianblk.zianrct.creator.CustomTrainerStore.boot(FMLPaths.CONFIGDIR.get());}
        catch(IOException|RuntimeException error){com.ianblk.zianrct.creator.CustomTrainerStore.clear();LOGGER.error("Custom trainer definitions unavailable; source file preserved",error);}
        try { com.ianblk.zianrct.rct.RctTrainerOptions.boot(FMLPaths.CONFIGDIR.get()); }
        catch (IOException | RuntimeException error) {
            com.ianblk.zianrct.rct.RctTrainerOptions.clear();
            LOGGER.error("Could not load trainer rematch/format options; source files preserved",error);
        }
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(ZianRctConfigLoader.FILE_NAME);
        try {
            ZianRctConfig loaded = ZianRctConfigLoader.loadOrCreate(configPath);
            ConfigState.replace(loaded);
            LOGGER.info(
                    "Loaded Zian RCT server configuration from {} with profile '{}'.",
                    configPath,
                    loaded.activeProfile()
            );
        } catch (IOException | RuntimeException exception) {
            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                throw new IllegalStateException(
                        "No se pudo cargar una configuración válida de Zian RCT desde " + configPath,
                        exception
                );
            }

            ZianRctConfig fallback = ZianRctConfig.defaults();
            ConfigState.replace(fallback);
            LOGGER.error(
                    "Could not load or create {} for the integrated server. Using built-in defaults for this world; "
                            + "the invalid file was left untouched.",
                    configPath,
                    exception
            );
        }
    }
}
