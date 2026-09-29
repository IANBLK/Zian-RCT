package com.ianblk.zianrct.medal;

import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.network.ZianRctNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MedalService {
    private static final String STORE_FILE = "zianrct-medals.json";

    private volatile MedalStore store;

    public void start(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(STORE_FILE);
        try {
            this.store = MedalStore.open(path);
            ZianRCT.LOGGER.info("Zian RCT medal store ready at {}.", path);
        } catch (IOException | RuntimeException exception) {
            this.store = null;
            backupCorrupt(path);
            ZianRCT.LOGGER.error(
                    "Zian RCT medal persistence could not be opened. Medal grants are disabled for this server session.",
                    exception
            );
        }
    }

    public void stop() {
        this.store = null;
    }

    public GrantResult grantIfAbsent(ServerPlayer player, String medalId, MedalOrigin origin) {
        return grantIfAbsent(player, medalId, origin, true);
    }

    private GrantResult grantIfAbsent(
            ServerPlayer player,
            String medalId,
            MedalOrigin origin,
            boolean syncClient
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(origin, "origin");

        Optional<ZianRctConfig.MedalDefinition> definition = medalDefinition(medalId);
        if (definition.isEmpty()) {
            return GrantResult.UNKNOWN_MEDAL;
        }

        MedalStore active = store;
        if (active == null) {
            return GrantResult.INACTIVE;
        }

        boolean clearRevocation = origin == MedalOrigin.BATTLE || origin == MedalOrigin.COMMAND;
        if (origin == MedalOrigin.RECONCILED && active.isRevoked(player.getUUID(), medalId)) {
            return GrantResult.REVOKED;
        }

        MedalRecord record = new MedalRecord(
                medalId,
                Instant.now().toEpochMilli(),
                origin
        );
        try {
            boolean granted = active.grantIfAbsent(player.getUUID(), record, clearRevocation);
            if (!granted) {
                return GrantResult.ALREADY_PRESENT;
            }
            ZianRCT.LOGGER.info(
                    "Granted Zian RCT medal '{}' to {} from {}.",
                    medalId,
                    player.getGameProfile().getName(),
                    origin
            );
            if (origin == MedalOrigin.BATTLE || origin == MedalOrigin.RECONCILED) {
                String template = ConfigState.current().messages().medalObtained();
                String message = template.replace("{medal}", definition.get().name());
                player.sendSystemMessage(Component.literal(message));
            }
            if (syncClient) {
                List<String> notifications = origin == MedalOrigin.BATTLE || origin == MedalOrigin.RECONCILED
                        ? List.of(medalId)
                        : List.of();
                ZianRctNetwork.sendSnapshot(player, this, notifications);
            }
            return GrantResult.GRANTED;
        } catch (IOException exception) {
            ZianRCT.LOGGER.error(
                    "Could not persist medal '{}' for {}. Grant was not committed.",
                    medalId,
                    player.getGameProfile().getName(),
                    exception
            );
            return GrantResult.PERSISTENCE_ERROR;
        }
    }

    public boolean revoke(ServerPlayer player, String medalId) {
        MedalStore active = store;
        if (active == null) {
            return false;
        }
        try {
            boolean changed = active.revoke(player.getUUID(), medalId);
            if (changed) {
                ZianRctNetwork.sendSnapshot(player, this, List.of());
            }
            return changed;
        } catch (IOException exception) {
            ZianRCT.LOGGER.error(
                    "Could not persist medal revocation '{}' for {}.",
                    medalId,
                    player.getGameProfile().getName(),
                    exception
            );
            return false;
        }
    }

    public List<MedalRecord> medals(ServerPlayer player) {
        MedalStore active = store;
        return active == null ? List.of() : active.medals(player.getUUID());
    }

    public Optional<ZianRctConfig.MedalDefinition> medalForTrainer(String trainerId) {
        return ConfigState.current().activeProfileConfig().medals().stream()
                .filter(medal -> medal != null && trainerId.equals(medal.trainer()))
                .findFirst();
    }

    public Optional<ZianRctConfig.MedalDefinition> medalDefinition(String medalId) {
        return ConfigState.current().activeProfileConfig().medals().stream()
                .filter(medal -> medal != null && medalId.equals(medal.id()))
                .findFirst();
    }

    public void reconcile(ServerPlayer player) {
        MedalStore active = store;
        if (active == null) {
            return;
        }

        List<String> reconciled = new ArrayList<>();
        MinecraftServer server = player.serverLevel().getServer();
        var trainerManager = RCTMod.getInstance().getTrainerManager();
        for (ZianRctConfig.MedalDefinition medal : ConfigState.current().activeProfileConfig().medals()) {
            if (medal == null
                    || active.has(player.getUUID(), medal.id())
                    || active.isRevoked(player.getUUID(), medal.id())) {
                continue;
            }

            boolean historicalWin = false;
            for (ServerLevel level : server.getAllLevels()) {
                try {
                    int count = trainerManager
                            .getBattleMemory(level, medal.trainer())
                            .getDefeatByCount(medal.trainer(), player);
                    if (count > 0) {
                        historicalWin = true;
                        break;
                    }
                } catch (RuntimeException exception) {
                    ZianRCT.LOGGER.warn(
                            "Could not inspect RCT battle memory for trainer '{}' in dimension '{}'.",
                            medal.trainer(),
                            level.dimension().location(),
                            exception
                    );
                }
            }

            if (historicalWin) {
                GrantResult result = grantIfAbsent(player, medal.id(), MedalOrigin.RECONCILED, false);
                if (result == GrantResult.GRANTED) {
                    reconciled.add(medal.id());
                }
            }
        }
        ZianRctNetwork.sendSnapshot(player, this, reconciled);
    }

    private static void backupCorrupt(Path path) {
        if (Files.notExists(path)) {
            return;
        }
        Path backup = path.resolveSibling(path.getFileName() + ".corrupt");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            ZianRCT.LOGGER.error("Copied unreadable medal persistence to {} before disabling medal grants.", backup);
        } catch (IOException backupException) {
            ZianRCT.LOGGER.error("Could not create corrupt medal persistence backup for {}.", path, backupException);
        }
    }

    public enum GrantResult {
        GRANTED,
        ALREADY_PRESENT,
        REVOKED,
        UNKNOWN_MEDAL,
        INACTIVE,
        PERSISTENCE_ERROR
    }
}
