package com.ianblk.zianrct.network;

import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.client.ClientMedalState;
import com.ianblk.zianrct.config.ConfigState;
import com.ianblk.zianrct.medal.MedalService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ZianRctNetwork {
    private ZianRctNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var editor = event.registrar("npc4");
        editor.playToServer(com.ianblk.zianrct.creator.CustomTrainerSave.TYPE,com.ianblk.zianrct.creator.CustomTrainerSave.CODEC,
                (payload,context)->context.enqueueWork(()->{
                    if(context.player() instanceof ServerPlayer player)com.ianblk.zianrct.npc.NpcEditorService.saveCustom(player,payload);
                }));
        editor.playToClient(com.ianblk.zianrct.creator.CustomTrainerSkins.TYPE,com.ianblk.zianrct.creator.CustomTrainerSkins.CODEC,
                (payload,context)->context.enqueueWork(()->{
                    try{com.ianblk.zianrct.client.CustomTrainerSkinState.accept(payload.json());}
                    catch(RuntimeException error){ZianRCT.LOGGER.warn("Invalid custom trainer skin metadata",error);}
                }));
        editor.playToClient(com.ianblk.zianrct.npc.NpcEditorPayload.TYPE,
                com.ianblk.zianrct.npc.NpcEditorPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    try { com.ianblk.zianrct.client.ClientNpcEditorState.accept(payload.json()); }
                    catch (RuntimeException error) { ZianRCT.LOGGER.warn("Invalid NPC editor snapshot", error); }
                }));
        editor.playToServer(com.ianblk.zianrct.npc.NpcEditorAction.TYPE,
                com.ianblk.zianrct.npc.NpcEditorAction.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player)
                        com.ianblk.zianrct.npc.NpcEditorService.receive(player, payload);
                }));
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
        registrar.playToClient(
                MedalOpenPayload.TYPE,
                MedalOpenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    try {
                        MedalProtocol.validateVersion(payload.protocolVersion());
                        ClientMedalState.requestOpen();
                    } catch (RuntimeException exception) {
                        ZianRCT.LOGGER.error("Rejected invalid Zian RCT medal open payload.", exception);
                    }
                })
        );
        ZianRCT.LOGGER.info(
                "Registered required Zian RCT medal payload protocol v{}.",
                MedalProtocol.CURRENT_VERSION
        );
    }

    public static boolean sendSnapshot(ServerPlayer player, MedalService medalService) {
        try {
            MedalClientSnapshot snapshot = MedalSnapshotFactory.build(
                    ConfigState.current(),
                    medalService.medals(player)
            );
            PacketDistributor.sendToPlayer(player, MedalSyncPayload.fromSnapshot(snapshot));
            return true;
        } catch (RuntimeException exception) {
            ZianRCT.LOGGER.error(
                    "Could not send Zian RCT medal snapshot to {}. The player session remains active.",
                    player.getGameProfile().getName(),
                    exception
            );
            return false;
        }
    }

    public static boolean sendAward(ServerPlayer player, String medalId) {
        try {
            PacketDistributor.sendToPlayer(player, MedalAwardPayload.of(medalId));
            return true;
        } catch (RuntimeException exception) {
            ZianRCT.LOGGER.error(
                    "Could not send Zian RCT medal award '{}' to {}. The battle result remains committed.",
                    medalId,
                    player.getGameProfile().getName(),
                    exception
            );
            return false;
        }
    }

    public static boolean sendOpen(ServerPlayer player) {
        try {
            PacketDistributor.sendToPlayer(player, MedalOpenPayload.current());
            return true;
        } catch (RuntimeException exception) {
            ZianRCT.LOGGER.error(
                    "Could not open Zian RCT medal case for {}.",
                    player.getGameProfile().getName(),
                    exception
            );
            return false;
        }
    }
}
