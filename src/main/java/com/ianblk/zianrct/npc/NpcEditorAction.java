package com.ianblk.zianrct.npc;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;
import java.util.Set;

public record NpcEditorAction(String nonce, String action, String value, String extra) implements CustomPacketPayload {
    public static final Set<String> ACTIONS = NpcEditorProtocol.ACTIONS;
    public static final Type<NpcEditorAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("zianrct", "npc_editor_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcEditorAction> CODEC = new StreamCodec<>() {
        public NpcEditorAction decode(RegistryFriendlyByteBuf buf) { return new NpcEditorAction(buf.readUtf(40), buf.readUtf(32), buf.readUtf(128), buf.readUtf(32)); }
        public void encode(RegistryFriendlyByteBuf buf, NpcEditorAction data) {
            buf.writeUtf(data.nonce,40);buf.writeUtf(data.action,32);buf.writeUtf(data.value,128);buf.writeUtf(data.extra,32);
        }
    };
    public NpcEditorAction {
        NpcEditorProtocol.action(nonce, action, value, extra);
    }
    public Type<NpcEditorAction> type() { return TYPE; }
}
