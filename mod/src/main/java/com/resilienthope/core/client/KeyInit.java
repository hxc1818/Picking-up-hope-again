package com.resilienthope.core.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.resilienthope.core.client.gui.InfiniteInventoryScreen;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketSwitchCharacter;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class KeyInit {
    public static final String CATEGORY = "key.categories.picking_hope";

    public static KeyMapping KEY_CHAR_1;
    public static KeyMapping KEY_CHAR_2;
    public static KeyMapping KEY_CHAR_3;
    public static KeyMapping KEY_CHAR_4;
    public static KeyMapping KEY_SKILL_E;
    public static KeyMapping KEY_BURST_Q;
    public static KeyMapping KEY_BAG_B;

    public static void registerKeybinds() {
        KEY_CHAR_1 = new KeyMapping("key.picking_hope.char_1", GLFW.GLFW_KEY_1, CATEGORY);
        KEY_CHAR_2 = new KeyMapping("key.picking_hope.char_2", GLFW.GLFW_KEY_2, CATEGORY);
        KEY_CHAR_3 = new KeyMapping("key.picking_hope.char_3", GLFW.GLFW_KEY_3, CATEGORY);
        KEY_CHAR_4 = new KeyMapping("key.picking_hope.char_4", GLFW.GLFW_KEY_4, CATEGORY);
        KEY_SKILL_E = new KeyMapping("key.picking_hope.skill_e", GLFW.GLFW_KEY_E, CATEGORY);
        KEY_BURST_Q = new KeyMapping("key.picking_hope.burst_q", GLFW.GLFW_KEY_Q, CATEGORY);
        KEY_BAG_B = new KeyMapping("key.picking_hope.bag_b", GLFW.GLFW_KEY_B, CATEGORY);

        ClientRegistry.registerKeyBinding(KEY_CHAR_1);
        ClientRegistry.registerKeyBinding(KEY_CHAR_2);
        ClientRegistry.registerKeyBinding(KEY_CHAR_3);
        ClientRegistry.registerKeyBinding(KEY_CHAR_4);
        ClientRegistry.registerKeyBinding(KEY_SKILL_E);
        ClientRegistry.registerKeyBinding(KEY_BURST_Q);
        ClientRegistry.registerKeyBinding(KEY_BAG_B);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (KEY_CHAR_1.consumeClick()) triggerSlotSwitch(0);
        if (KEY_CHAR_2.consumeClick()) triggerSlotSwitch(1);
        if (KEY_CHAR_3.consumeClick()) triggerSlotSwitch(2);
        if (KEY_CHAR_4.consumeClick()) triggerSlotSwitch(3);

        if (KEY_BAG_B.consumeClick()) {
            mc.setScreen(new InfiniteInventoryScreen());
        }
    }

    private static void triggerSlotSwitch(int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            team.switchSlot(mc.player, slot);
            PacketHandler.INSTANCE.sendToServer(new PacketSwitchCharacter(slot));
        });
    }
}
