package com.ianblk.zianrct.client;
import com.ianblk.zianrct.creator.IndependentTrainerMob;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.resources.ResourceLocation;

public final class CustomTrainerRenderer extends MobRenderer<IndependentTrainerMob,PlayerModel<IndependentTrainerMob>> {
    private final PlayerModel<IndependentTrainerMob> classic,slim;
    public CustomTrainerRenderer(EntityRendererProvider.Context context){
        super(context,new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false),0.5f);
        classic=model;slim=new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM),true);
        addLayer(new ItemInHandLayer<>(this,context.getItemInHandRenderer()));
    }
    @Override public void render(IndependentTrainerMob trainer,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){model=com.ianblk.zianrct.creator.CustomTrainer.slim(getTextureLocation(trainer).toString())?slim:classic;super.render(trainer,yaw,partial,pose,buffers,light);}
    @Override protected void renderNameTag(IndependentTrainerMob trainer,net.minecraft.network.chat.Component title,
            com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,float partialTick){
        super.renderNameTag(trainer,trainer.getSpeech().getString().isBlank()?title:trainer.getSpeech(),pose,buffers,light,partialTick);
    }
    @Override public ResourceLocation getTextureLocation(IndependentTrainerMob trainer){
        ResourceLocation skin=CustomTrainerSkinState.get(trainer.getTrainerId());
        if(skin==null){
            String league=com.ianblk.zianrct.creator.LeagueTrainerSkins.texture(trainer.getTrainerId());
            if(league!=null)skin=ResourceLocation.parse(league);
        }
        return skin!=null?skin:ResourceLocation.fromNamespaceAndPath("zianrct","textures/entity/trainers/amber_captain.png");
    }
}
