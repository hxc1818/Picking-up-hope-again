package com.resilienthope.core.init;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.entity.SecurityDroneEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, PickingHopeCore.MOD_ID);

    public static final RegistryObject<EntityType<SecurityDroneEntity>> SECURITY_DRONE =
            ENTITY_TYPES.register("security_drone",
                    () -> EntityType.Builder.of(SecurityDroneEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 0.8f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(PickingHopeCore.MOD_ID, "security_drone").toString()));
}
