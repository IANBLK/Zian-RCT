package com.ianblk.zianrct.client;

import com.ianblk.zianrct.network.MedalClientSnapshot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class ZianRctClient {
    private static final KeyMapping OPEN_MEDALS_KEY = new KeyMapping(
            "key.zianrct.open_medals",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.zianrct"
    );

    private ZianRctClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(OPEN_MEDALS_KEY));
        NeoForge.EVENT_BUS.addListener(ZianRctClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ZianRctClient::onLoggingOut);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        while (OPEN_MEDALS_KEY.consumeClick()) {
            openMedalCase(minecraft);
        }
        if (ClientMedalState.consumeOpenRequest()) {
            openMedalCase(minecraft);
        }

        for (String medalId : ClientMedalState.drainAwards()) {
            ClientMedalState.current().ifPresent(snapshot -> showMedalNotification(minecraft, snapshot, medalId));
        }
    }

    private static void openMedalCase(Minecraft minecraft) {
        if (ClientMedalState.current().isPresent()) {
            minecraft.setScreen(new MedalCaseScreen());
        } else {
            SystemToast.add(
                    minecraft.getToasts(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal("Zian RCT"),
                    Component.literal("Aún no se han sincronizado las medallas del servidor.")
            );
        }
    }

    private static void showMedalNotification(
            Minecraft minecraft,
            MedalClientSnapshot snapshot,
            String medalId
    ) {
        MedalClientSnapshot.MedalDefinitionView definition = snapshot.definitions().stream()
                .filter(medal -> medal != null && medalId.equals(medal.id()))
                .findFirst()
                .orElse(null);
        if (definition == null) {
            return;
        }

        SystemToast.add(
                minecraft.getToasts(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("¡Nueva medalla!"),
                Component.literal(definition.name())
        );
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMedalState.clear();
    }
}
