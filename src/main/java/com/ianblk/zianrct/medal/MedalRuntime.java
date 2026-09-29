package com.ianblk.zianrct.medal;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class MedalRuntime {
    private final MedalService medalService = new MedalService();
    private final RctBattleMedalListener battleListener = new RctBattleMedalListener(medalService);

    public MedalRuntime() {
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    }

    public MedalService service() {
        return medalService;
    }

    private void onServerStarted(ServerStartedEvent event) {
        medalService.start(event.getServer());
        battleListener.registerIfAvailable();
    }

    private void onServerStopped(ServerStoppedEvent event) {
        battleListener.resetRegistration();
        medalService.stop();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            medalService.reconcile(player);
        }
    }
}
