package com.ianblk.zianrct.creator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record CustomTrainerSave(String nonce,String json) implements CustomPacketPayload {
    public static final Type<CustomTrainerSave> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("zianrct","custom_trainer_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CustomTrainerSave> CODEC=new StreamCodec<>(){
        public CustomTrainerSave decode(RegistryFriendlyByteBuf b){return new CustomTrainerSave(b.readUtf(40),b.readUtf(16384));}
        public void encode(RegistryFriendlyByteBuf b,CustomTrainerSave p){b.writeUtf(p.nonce,40);b.writeUtf(p.json,16384);}
    };
    public CustomTrainerSave {UUID.fromString(nonce);if(json==null || json.length()>16384)throw new IllegalArgumentException("Definición demasiado grande");}
    public Type<CustomTrainerSave> type(){return TYPE;}
}
