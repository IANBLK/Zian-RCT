package com.ianblk.zianrct.network;

import com.ianblk.zianrct.ZianRCT;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MedalSyncPayload(int protocolVersion, String snapshotJson) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MedalSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(ZianRCT.MOD_ID, "medal_sync_v1")
    );
    public static final StreamCodec<ByteBuf, MedalSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MedalSyncPayload::protocolVersion,
            ByteBufCodecs.stringUtf8(MedalProtocol.MAX_SNAPSHOT_UTF8_BYTES), MedalSyncPayload::snapshotJson,
            MedalSyncPayload::new
    );

    public MedalSyncPayload {
        MedalProtocol.validateVersion(protocolVersion);
        if (snapshotJson == null || MedalProtocol.utf8Length(snapshotJson) > MedalProtocol.MAX_SNAPSHOT_UTF8_BYTES) {
            throw new IllegalStateException("Payload de sincronización de medallas inválido");
        }
    }

    public static MedalSyncPayload fromSnapshot(MedalClientSnapshot snapshot) {
        return new MedalSyncPayload(MedalProtocol.CURRENT_VERSION, MedalProtocol.encode(snapshot));
    }

    public MedalClientSnapshot decodeSnapshot() {
        return MedalProtocol.decode(protocolVersion, snapshotJson);
    }

    @Override
    public CustomPacketPayload.Type<MedalSyncPayload> type() {
        return TYPE;
    }
}
