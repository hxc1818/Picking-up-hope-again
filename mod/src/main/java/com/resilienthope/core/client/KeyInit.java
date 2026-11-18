package com.resilienthope.core.client;

import com.resilienthope.core.client.gui.CharacterProfileScreen;
import com.resilienthope.core.client.gui.InfiniteInventoryScreen;
import com.resilienthope.core.client.gui.SimulationDomainScreen;
import com.resilienthope.core.client.gui.TerminalWishScreen;
import com.resilienthope.core.entity.FactionNpcEntity;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketSwitchCharacter;
import com.resilienthope.core.quest.QuestManager;
import com.resilienthope.core.subtitle.RadioSubtitleManager;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public class KeyInit {
    public static final String CATEGORY = "key.categories.picking_hope";

    public static KeyMapping KEY_CHAR_1;
    public static KeyMapping KEY_CHAR_2;
    public static KeyMapping KEY_CHAR_3;
    public static KeyMapping KEY_CHAR_4;
    public static KeyMapping KEY_SKILL_E;
    public static KeyMapping KEY_BURST_Q;
    public static KeyMapping KEY_BAG_B;
    public static KeyMapping KEY_INTERACT_F;
    public static KeyMapping KEY_PROFILE_C;
    public static KeyMapping KEY_WISH_G;

    public static void registerKeybinds() {
        KEY_CHAR_1 = new KeyMapping("key.picking_hope.char_1", GLFW.GLFW_KEY_1, CATEGORY);
        KEY_CHAR_2 = new KeyMapping("key.picking_hope.char_2", GLFW.GLFW_KEY_2, CATEGORY);
        KEY_CHAR_3 = new KeyMapping("key.picking_hope.char_3", GLFW.GLFW_KEY_3, CATEGORY);
        KEY_CHAR_4 = new KeyMapping("key.picking_hope.char_4", GLFW.GLFW_KEY_4, CATEGORY);
        KEY_SKILL_E = new KeyMapping("key.picking_hope.skill_e", GLFW.GLFW_KEY_E, CATEGORY);
        KEY_BURST_Q = new KeyMapping("key.picking_hope.burst_q", GLFW.GLFW_KEY_Q, CATEGORY);
        KEY_BAG_B = new KeyMapping("key.picking_hope.bag_b", GLFW.GLFW_KEY_B, CATEGORY);
        KEY_INTERACT_F = new KeyMapping("key.picking_hope.interact_f", GLFW.GLFW_KEY_F, CATEGORY);
        KEY_PROFILE_C = new KeyMapping("key.picking_hope.profile_c", GLFW.GLFW_KEY_C, CATEGORY);
        KEY_WISH_G = new KeyMapping("key.picking_hope.wish_g", GLFW.GLFW_KEY_G, CATEGORY);

        ClientRegistry.registerKeyBinding(KEY_CHAR_1);
        ClientRegistry.registerKeyBinding(KEY_CHAR_2);
        ClientRegistry.registerKeyBinding(KEY_CHAR_3);
        ClientRegistry.registerKeyBinding(KEY_CHAR_4);
        ClientRegistry.registerKeyBinding(KEY_SKILL_E);
        ClientRegistry.registerKeyBinding(KEY_BURST_Q);
        ClientRegistry.registerKeyBinding(KEY_BAG_B);
        ClientRegistry.registerKeyBinding(KEY_INTERACT_F);
        ClientRegistry.registerKeyBinding(KEY_PROFILE_C);
        ClientRegistry.registerKeyBinding(KEY_WISH_G);
    }

    public static float getSkillCooldownSeconds() {
        return 0.0f;
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

        if (KEY_PROFILE_C.consumeClick() || KEY_BURST_Q.consumeClick()) {
            mc.setScreen(new CharacterProfileScreen());
        }

        if (KEY_WISH_G.consumeClick()) {
            mc.setScreen(new TerminalWishScreen());
        }

        if (KEY_SKILL_E.consumeClick()) {
            if (QuestManager.getQuestStage() < 10) {
                RadioSubtitleManager.postRadioMessage("模拟终端", "模拟训练副本将在第 10 阶段抵达地面科考站后解锁！", 90);
            } else {
                mc.setScreen(new SimulationDomainScreen());
            }
        }

        if (KEY_INTERACT_F.consumeClick()) {
            while (mc.options.keySwapOffhand.consumeClick()) {}
            triggerInteraction(mc);
        }
    }

    private static void triggerSlotSwitch(int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            if (!team.getCharacter(slot).isUnlocked()) {
                mc.player.playSound(SoundEvents.DISPENSER_FAIL, 0.6f, 1.2f);
                RadioSubtitleManager.postRadioMessage("编队系统", "该位置队友尚未解锁。", 60);
                return;
            }
            if (team.getActiveSlotIndex() == slot) return;
            team.switchSlot(mc.player, slot);
            PacketHandler.INSTANCE.sendToServer(new PacketSwitchCharacter(slot));
            mc.player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.4f);
            RadioSubtitleManager.postRadioMessage(team.getCharacter(slot).getName(), "切换带队！随时准备迎敌。", 60);
        });
    }

    private static void triggerInteraction(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (mc.hitResult instanceof EntityHitResult entityHit) {
            if (mc.player.getEyePosition().distanceTo(entityHit.getLocation()) <= 4.8) {
                mc.gameMode.interact(mc.player, entityHit.getEntity(), InteractionHand.MAIN_HAND);
                return;
            }
        }
        if (mc.hitResult instanceof BlockHitResult blockHit) {
            if (mc.player.getEyePosition().distanceTo(blockHit.getLocation()) <= 4.8) {
                mc.gameMode.useItemOn(mc.player, mc.level, InteractionHand.MAIN_HAND, blockHit);
                return;
            }
        }
        List<FactionNpcEntity> npcs = mc.level.getEntitiesOfClass(FactionNpcEntity.class,
                new AABB(mc.player.blockPosition()).inflate(3.2));
        npcs.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)))
                .ifPresent(npc -> mc.gameMode.interact(mc.player, npc, InteractionHand.MAIN_HAND));
    }
}
