package com.ianblk.zianrct.creator;
import com.google.gson.Gson;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public record CustomTrainerSkins(String json) implements CustomPacketPayload {
    public static final Type<CustomTrainerSkins> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("zianrct","custom_trainer_skins"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CustomTrainerSkins> CODEC=new StreamCodec<>(){
        public CustomTrainerSkins decode(RegistryFriendlyByteBuf b){return new CustomTrainerSkins(b.readUtf(16384));}
        public void encode(RegistryFriendlyByteBuf b,CustomTrainerSkins p){b.writeUtf(p.json,16384);}
    };
    public CustomTrainerSkins {if(json==null || json.length()>16384)throw new IllegalArgumentException("Skin metadata too large");}
    public Type<CustomTrainerSkins> type(){return TYPE;}
    public static void sync(ServerPlayer player){PacketDistributor.sendToPlayer(player,new CustomTrainerSkins(new Gson().toJson(CustomTrainerStore.skins())));}
    public static void sync(MinecraftServer server){server.getPlayerList().getPlayers().forEach(CustomTrainerSkins::sync);}
}
