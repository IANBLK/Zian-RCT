package com.ianblk.zianrct.npc;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NpcEditorPayload(String json) implements CustomPacketPayload {
    public static final Type<NpcEditorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("zianrct", "npc_editor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcEditorPayload> CODEC = new StreamCodec<>() {
        public NpcEditorPayload decode(RegistryFriendlyByteBuf buf) { return new NpcEditorPayload(buf.readUtf(16384)); }
        public void encode(RegistryFriendlyByteBuf buf, NpcEditorPayload data) { buf.writeUtf(data.json, 16384); }
    };
    public NpcEditorPayload { NpcEditorProtocol.snapshot(json); }
    public Type<NpcEditorPayload> type() { return TYPE; }
}
