package com.resilienthope.core.world;

import com.resilienthope.core.PickingHopeCore;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Custom dimension keys separating the Starship void build space, the 10k x 10k Cryo Planet build space,
 * and the isolated 3D turn-based battle arena dimension.
 */
public final class ModDimensions {
    public static final ResourceKey<Level> STARSHIP = ResourceKey.create(
            Registry.DIMENSION_REGISTRY, new ResourceLocation(PickingHopeCore.MOD_ID, "starship"));

    public static final ResourceKey<Level> PLANET_CRYO = ResourceKey.create(
            Registry.DIMENSION_REGISTRY, new ResourceLocation(PickingHopeCore.MOD_ID, "planet_cryo"));

    public static final ResourceKey<Level> BATTLE_ARENA = ResourceKey.create(
            Registry.DIMENSION_REGISTRY, new ResourceLocation(PickingHopeCore.MOD_ID, "battle_arena"));

    private ModDimensions() {}

    public static String getDisplayName(ResourceKey<Level> dim) {
        if (STARSHIP.equals(dim)) return "重拾希望号·星舰";
        if (PLANET_CRYO.equals(dim)) return "第一颗星球·极寒大陆";
        if (BATTLE_ARENA.equals(dim)) return "战术战斗空间";
        return "初始主世界";
    }

    public static String toZoneDimensionId(ResourceKey<Level> dim) {
        if (STARSHIP.equals(dim)) return "starship";
        if (PLANET_CRYO.equals(dim)) return "planet";
        return "";
    }
}
