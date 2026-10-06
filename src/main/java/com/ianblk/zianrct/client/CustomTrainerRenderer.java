package com.ianblk.zianrct.client;
import com.gitlab.srcmc.rctmod.client.renderer.TrainerRenderer;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class CustomTrainerRenderer extends TrainerRenderer {
    public CustomTrainerRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(TrainerMob trainer){
        ResourceLocation skin=CustomTrainerSkinState.get(trainer.getTrainerId());
        if(skin!=null && net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(skin).isPresent())return skin;
        return super.getTextureLocation(trainer);
    }
}
