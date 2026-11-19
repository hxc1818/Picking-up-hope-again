package com.resilienthope.core.init;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.entity.BattleActorEntity;
import com.resilienthope.core.entity.BattleCameraEntity;
import com.resilienthope.core.entity.FactionNpcEntity;
import com.resilienthope.core.entity.PrecursorMechBossEntity;
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

    public static final RegistryObject<EntityType<PrecursorMechBossEntity>> PRECURSOR_MECH_BOSS =
            ENTITY_TYPES.register("precursor_mech",
                    () -> EntityType.Builder.of(PrecursorMechBossEntity::new, MobCategory.MONSTER)
                            .sized(1.8f, 3.0f)
                            .clientTrackingRange(12)
                            .build(new ResourceLocation(PickingHopeCore.MOD_ID, "precursor_mech").toString()));

    public static final RegistryObject<EntityType<FactionNpcEntity>> FACTION_NPC =
            ENTITY_TYPES.register("faction_npc",
                    () -> EntityType.Builder.of(FactionNpcEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(PickingHopeCore.MOD_ID, "faction_npc").toString()));

    public static final RegistryObject<EntityType<BattleActorEntity>> BATTLE_ACTOR =
            ENTITY_TYPES.register("battle_actor",
                    () -> EntityType.Builder.of(BattleActorEntity::new, MobCategory.MISC)
                            .sized(0.9f, 2.0f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build(new ResourceLocation(PickingHopeCore.MOD_ID, "battle_actor").toString()));

    public static final RegistryObject<EntityType<BattleCameraEntity>> BATTLE_CAMERA =
            ENTITY_TYPES.register("battle_camera",
                    () -> EntityType.Builder.of(BattleCameraEntity::new, MobCategory.MISC)
                            .sized(0.2f, 0.2f)
                            .clientTrackingRange(4)
                            .build(new ResourceLocation(PickingHopeCore.MOD_ID, "battle_camera").toString()));
}
