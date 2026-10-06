package com.ianblk.zianrct.client;
import com.ianblk.zianrct.creator.IndependentTrainerMob;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.resources.ResourceLocation;

public final class CustomTrainerRenderer extends MobRenderer<IndependentTrainerMob,PlayerModel<IndependentTrainerMob>> {
    public CustomTrainerRenderer(EntityRendererProvider.Context context){
        super(context,new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false),0.5f);
        addLayer(new ItemInHandLayer<>(this,context.getItemInHandRenderer()));
    }
    @Override protected void renderNameTag(IndependentTrainerMob trainer,net.minecraft.network.chat.Component title,
            com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,float partialTick){
        super.renderNameTag(trainer,trainer.getSpeech().isBlank()?title:net.minecraft.network.chat.Component.literal(trainer.getSpeech()),pose,buffers,light,partialTick);
    }
    @Override public ResourceLocation getTextureLocation(IndependentTrainerMob trainer){
        ResourceLocation skin=CustomTrainerSkinState.get(trainer.getTrainerId());
        return skin!=null?skin:ResourceLocation.fromNamespaceAndPath("zianrct","textures/entity/trainers/amber_captain.png");
    }
}
