package com.ianblk.zianrct.creator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class CustomTrainerEntities {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,"zianrct");
    public static final DeferredHolder<EntityType<?>,EntityType<IndependentTrainerMob>> TYPE=TYPES.register("independent_trainer",
            ()->EntityType.Builder.<IndependentTrainerMob>of(IndependentTrainerMob::new,MobCategory.MISC)
                    .sized(0.6f,1.8f).clientTrackingRange(10).build("zianrct:independent_trainer"));
    private CustomTrainerEntities() {}
    public static void register(IEventBus bus){
        TYPES.register(bus);
        bus.addListener((EntityAttributeCreationEvent event)->event.put(TYPE.get(),com.gitlab.srcmc.rctmod.world.entities.TrainerMob.createAttributes().build()));
    }
}
