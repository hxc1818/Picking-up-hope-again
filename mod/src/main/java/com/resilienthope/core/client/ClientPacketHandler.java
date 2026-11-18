package com.resilienthope.core.client;

import com.resilienthope.core.client.gui.NpcDialogueScreen;
import com.resilienthope.core.client.gui.TerminalWishScreen;
import com.resilienthope.core.client.gui.TurnBasedBattleScreen;
import com.resilienthope.core.combat.BattleState;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ClientPacketHandler {

    public static void applyTeamData(CompoundTag teamNbt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || teamNbt == null) return;
        mc.player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            team.deserializeNBT(teamNbt);
        });
    }

    public static void openNpcDialogue(String npcId, int questStage) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new NpcDialogueScreen(npcId, questStage));
    }

    public static void handleWishResult(List<ItemStack> pulls) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TerminalWishScreen wishScreen) {
            wishScreen.setPullResults(pulls);
        }
    }

    public static void handleBattleSync(CompoundTag stateNbt) {
        Minecraft mc = Minecraft.getInstance();
        BattleState state = BattleState.fromNbt(stateNbt);
        if (!state.active) {
            if (mc.screen instanceof TurnBasedBattleScreen) {
                mc.setScreen(null);
            }
            return;
        }
        if (mc.screen instanceof TurnBasedBattleScreen screen) {
            screen.updateState(state);
        } else {
            mc.setScreen(new TurnBasedBattleScreen(state));
        }
    }
}
