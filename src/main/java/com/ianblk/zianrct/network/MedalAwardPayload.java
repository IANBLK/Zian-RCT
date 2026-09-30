package com.ianblk.zianrct.network;

import com.ianblk.zianrct.ZianRCT;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MedalAwardPayload(int protocolVersion, String medalId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MedalAwardPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(ZianRCT.MOD_ID, "medal_award_v1")
    );
    public static final StreamCodec<ByteBuf, MedalAwardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MedalAwardPayload::protocolVersion,
            ByteBufCodecs.stringUtf8(MedalProtocol.MAX_ID_LENGTH), MedalAwardPayload::medalId,
            MedalAwardPayload::new
    );

    public MedalAwardPayload {
        MedalProtocol.validateVersion(protocolVersion);
        MedalProtocol.validateMedalId(medalId);
    }

    public static MedalAwardPayload of(String medalId) {
        return new MedalAwardPayload(MedalProtocol.CURRENT_VERSION, medalId);
    }

    @Override
    public CustomPacketPayload.Type<MedalAwardPayload> type() {
        return TYPE;
    }
}
