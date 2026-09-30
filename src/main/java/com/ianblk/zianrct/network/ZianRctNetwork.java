package com.ianblk.zianrct.network;

import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.client.ClientMedalState;
import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.config.ZianRctConfig;
import com.ianblk.zianrct.medal.MedalRecord;
import com.ianblk.zianrct.medal.MedalService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ZianRctNetwork {
    private ZianRctNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MedalProtocol.NETWORK_VERSION);
        registrar.playToClient(
                MedalSyncPayload.TYPE,
                MedalSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    try {
                        ClientMedalState.apply(payload.decodeSnapshot());
                    } catch (RuntimeException exception) {
                        ZianRCT.LOGGER.error("Rejected invalid Zian RCT medal sync payload.", exception);
                    }
                })
        );
        registrar.playToClient(
                MedalAwardPayload.TYPE,
                MedalAwardPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    try {
                        ClientMedalState.notifyAward(payload.medalId());
                    } catch (RuntimeException exception) {
                        ZianRCT.LOGGER.error("Rejected invalid Zian RCT medal award payload.", exception);
                    }
                })
        );
        ZianRCT.LOGGER.info(
                "Registered required Zian RCT medal payload protocol v{}.",
                MedalProtocol.CURRENT_VERSION
        );
    }

    public static void sendSnapshot(ServerPlayer player, MedalService medalService) {
        ZianRctConfig config = ConfigState.current();
        ZianRctConfig.Profile profile = config.activeProfileConfig();
        Map<String, Integer> unlockCaps = profile.trainerUnlockCaps();

        List<MedalClientSnapshot.MedalDefinitionView> definitions = profile.medals().stream()
                .filter(medal -> medal != null)
                .sorted(Comparator.comparingInt(ZianRctConfig.MedalDefinition::order))
                .map(medal -> new MedalClientSnapshot.MedalDefinitionView(
                        medal.id(),
                        medal.trainer(),
                        medal.name(),
                        medal.description(),
                        medal.texture(),
                        medal.color(),
                        medal.order(),
                        unlockCaps.getOrDefault(medal.trainer(), profile.maxCap())
                ))
                .toList();

        List<MedalClientSnapshot.OwnedMedalView> owned = new ArrayList<>();
        for (MedalRecord record : medalService.medals(player)) {
            owned.add(new MedalClientSnapshot.OwnedMedalView(
                    record.medalId(),
                    record.grantedAtEpochMilli(),
                    record.origin().name()
            ));
        }

        MedalClientSnapshot snapshot = new MedalClientSnapshot(
                config.activeProfile(),
                definitions,
                owned
        );
        PacketDistributor.sendToPlayer(player, MedalSyncPayload.fromSnapshot(snapshot));
    }

    public static void sendAward(ServerPlayer player, String medalId) {
        PacketDistributor.sendToPlayer(player, MedalAwardPayload.of(medalId));
    }
}
