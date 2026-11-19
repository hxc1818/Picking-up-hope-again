package com.resilienthope.core.client.gui;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Intercepts vanilla TitleScreen and PauseScreen to present the custom singleplayer RPG login/launch UI.
 */
public class TitleScreenOverrideHandler {

    @SubscribeEvent
    public static void onScreenOpen(ScreenOpenEvent event) {
        if (event.getScreen() instanceof TitleScreen) {
            event.setScreen(new HopeTitleScreen());
        } else if (event.getScreen() instanceof PauseScreen) {
            event.setScreen(new HopePauseScreen());
        }
    }
}
