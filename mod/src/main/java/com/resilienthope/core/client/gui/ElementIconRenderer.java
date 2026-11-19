package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.team.CharacterData;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws 16x16 pixel-art element emblems (Water / Earth / Air / Fire) across battle & RPG UIs.
 */
public final class ElementIconRenderer {
    public static final ResourceLocation TEX_WATER = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/elem_water.png");
    public static final ResourceLocation TEX_EARTH = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/elem_earth.png");
    public static final ResourceLocation TEX_AIR = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/elem_air.png");
    public static final ResourceLocation TEX_FIRE = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/elem_fire.png");

    private ElementIconRenderer() {}

    public static ResourceLocation getTexture(String element) {
        if (element == null) return TEX_EARTH;
        return switch (element) {
            case CharacterData.ELEM_WATER -> TEX_WATER;
            case CharacterData.ELEM_AIR -> TEX_AIR;
            case CharacterData.ELEM_FIRE -> TEX_FIRE;
            default -> TEX_EARTH;
        };
    }

    public static void draw(PoseStack ps, String element, int x, int y, int size) {
        GenshinHudOverlay.drawTexture(ps, getTexture(element), x, y, size, size, 16, 16);
    }
}
