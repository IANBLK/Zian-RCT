package com.ianblk.zianrct.network;

import com.ianblk.zianrct.ZianRCT;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MedalOpenPayload(int protocolVersion) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MedalOpenPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(ZianRCT.MOD_ID, "medal_open_v1")
    );
    public static final StreamCodec<ByteBuf, MedalOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MedalOpenPayload::protocolVersion,
            MedalOpenPayload::new
    );

    public MedalOpenPayload {
        MedalProtocol.validateVersion(protocolVersion);
    }

    public static MedalOpenPayload current() {
        return new MedalOpenPayload(MedalProtocol.CURRENT_VERSION);
    }

    @Override
    public CustomPacketPayload.Type<MedalOpenPayload> type() {
        return TYPE;
    }
}
