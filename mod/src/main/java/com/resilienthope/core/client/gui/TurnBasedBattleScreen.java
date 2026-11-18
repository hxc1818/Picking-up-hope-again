package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.combat.BattleState;
import com.resilienthope.core.combat.TurnBasedCombatManager;
import com.resilienthope.core.network.PacketBattleAction;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.team.CharacterData;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TurnBasedBattleScreen extends Screen {
    private static final ResourceLocation TEX_LINYE = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_dawn.png");
    private static final ResourceLocation TEX_CHEN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_chen.png");
    private static final ResourceLocation TEX_SU = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_su.png");
    private static final ResourceLocation TEX_ZHENG = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_zheng.png");
    private static final ResourceLocation TEX_HAN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_han.png");
    private static final ResourceLocation TEX_ZHOU = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_zhou.png");

    private BattleState state;
    private int selectedEnemyIdx = 0;
    private int pendingActionMode = TurnBasedCombatManager.ACTION_BASIC; // 0=Basic(Q), 1=Skill(E)
    private int introTicks = 14;

    private Button basicBtn;
    private Button skillBtn;
    private Button confirmBtn;
    private Button fleeBtn;
    private Button exitResultBtn;

    public TurnBasedBattleScreen(BattleState initialState) {
        super(new TextComponent("回合制战术舞台"));
        this.state = initialState;
        ensureValidTarget();
    }

    public void updateState(BattleState newState) {
        this.state = newState;
        ensureValidTarget();
        refreshButtons();
    }

    private void ensureValidTarget() {
        if (state == null || state.enemies.isEmpty()) return;
        if (selectedEnemyIdx >= 0 && selectedEnemyIdx < state.enemies.size() && state.enemies.get(selectedEnemyIdx).isAlive()) {
            return;
        }
        for (int i = 0; i < state.enemies.size(); i++) {
            if (state.enemies.get(i).isAlive()) {
                selectedEnemyIdx = i;
                return;
            }
        }
        selectedEnemyIdx = 0;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    protected void init() {
        super.init();
        refreshButtons();
    }

    private void refreshButtons() {
        clearWidgets();
        if (state == null) return;

        if (state.finished) {
            int cw = 160;
            this.exitResultBtn = addRenderableWidget(new Button(this.width / 2 - cw / 2, this.height / 2 + 44, cw, 22,
                    new TextComponent("[Space] 返回大地图"), btn -> sendAction(TurnBasedCombatManager.ACTION_EXIT_FINISHED, -1)));
            return;
        }

        int ctrlX = this.width - 210;
        int ctrlY = this.height - 86;

        this.basicBtn = addRenderableWidget(new Button(ctrlX, ctrlY, 94, 22,
                new TextComponent("[Q] 普攻 +1点"), btn -> {
            if (pendingActionMode == TurnBasedCombatManager.ACTION_BASIC) {
                executePendingAction();
            } else {
                pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
            }
        }));

        this.skillBtn = addRenderableWidget(new Button(ctrlX + 98, ctrlY, 94, 22,
                new TextComponent("[E] 战技 -1点"), btn -> {
            if (state.skillPoints <= 0) return;
            if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL) {
                executePendingAction();
            } else {
                pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
            }
        }));
        this.skillBtn.active = state.skillPoints > 0;

        this.confirmBtn = addRenderableWidget(new Button(ctrlX, ctrlY + 26, 132, 22,
                new TextComponent("[Space] 确认出手"), btn -> executePendingAction()));

        this.fleeBtn = addRenderableWidget(new Button(ctrlX + 136, ctrlY + 26, 56, 22,
                new TextComponent("撤退"), btn -> sendAction(TurnBasedCombatManager.ACTION_FLEE, -1)));
        this.fleeBtn.active = state.canFlee;
    }

    private void executePendingAction() {
        if (state == null || state.finished) return;
        if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL && state.skillPoints <= 0) {
            pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
        }
        sendAction(pendingActionMode, -1);
    }

    private void sendAction(int type, int allyUltIdx) {
        PacketHandler.INSTANCE.sendToServer(new PacketBattleAction(type, selectedEnemyIdx, allyUltIdx));
    }

    private void cycleEnemyTarget(int dir) {
        if (state == null || state.enemies.isEmpty()) return;
        int n = state.enemies.size();
        for (int step = 1; step <= n; step++) {
            int idx = (selectedEnemyIdx + dir * step + n * 4) % n;
            if (state.enemies.get(idx).isAlive()) {
                selectedEnemyIdx = idx;
                return;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (introTicks > 0) introTicks--;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (state == null) return super.keyPressed(keyCode, scanCode, modifiers);

        if (state.finished) {
            if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
                sendAction(TurnBasedCombatManager.ACTION_EXIT_FINISHED, -1);
                return true;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_Q) {
            if (pendingActionMode == TurnBasedCombatManager.ACTION_BASIC) executePendingAction();
            else pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_E) {
            if (state.skillPoints > 0) {
                if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL) executePendingAction();
                else pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
            executePendingAction();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_D || keyCode == GLFW.GLFW_KEY_RIGHT) {
            cycleEnemyTarget(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_LEFT) {
            cycleEnemyTarget(-1);
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_4) {
            int slot = keyCode - GLFW.GLFW_KEY_1;
            if (slot < state.allies.size()) {
                BattleState.AllyUnit u = state.allies.get(slot);
                if (u.isAlive() && u.energy >= u.maxEnergy) {
                    sendAction(TurnBasedCombatManager.ACTION_ULT, slot);
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (state != null && !state.finished) {
            // Click Enemy Cards to select target
            int count = state.enemies.size();
            int cardW = Math.min(126, (this.width - 150) / Math.max(1, count) - 8);
            int totalW = count * (cardW + 8) - 8;
            int startX = 104 + Math.max(0, (this.width - 118 - totalW) / 2);
            int enemyY = 34;
            for (int i = 0; i < count; i++) {
                int cx = startX + i * (cardW + 8);
                if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= enemyY && mouseY <= enemyY + 86) {
                    if (state.enemies.get(i).isAlive()) {
                        this.selectedEnemyIdx = i;
                        return true;
                    }
                }
            }

            // Click Ally Cards to cast Ultimate when 100% ready
            int allyStartX = 14;
            int allyY = this.height - 92;
            for (int i = 0; i < state.allies.size(); i++) {
                int ax = allyStartX + i * 92;
                if (mouseX >= ax && mouseX <= ax + 88 && mouseY >= allyY && mouseY <= allyY + 78) {
                    BattleState.AllyUnit u = state.allies.get(i);
                    if (u.isAlive() && u.energy >= u.maxEnergy) {
                        sendAction(TurnBasedCombatManager.ACTION_ULT, i);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static ResourceLocation getPortraitForChar(String charId) {
        if (charId == null) return TEX_LINYE;
        return switch (charId) {
            case "chenfeng" -> TEX_CHEN;
            case "suqing" -> TEX_SU;
            case "laozheng" -> TEX_ZHENG;
            case "hanbing" -> TEX_HAN;
            case "zhoumin" -> TEX_ZHOU;
            default -> TEX_LINYE;
        };
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        if (state == null) return;

        // Dark tactical overlay instead of opaque dirt background so 3D arena shows through!
        GuiComponent.fill(ps, 0, 0, this.width, this.height, 0x66060B14);

        renderTopHeader(ps);
        renderActionTimeline(ps);
        renderEnemyCards(ps);
        renderBattleLogs(ps);
        renderAllyCards(ps);
        renderSkillPointPanel(ps);

        if (state.finished) {
            renderResultModal(ps);
        }

        super.render(ps, mouseX, mouseY, partialTicks);

        if (introTicks > 0) {
            int alpha = (int) ((introTicks / 14.0f) * 200);
            GuiComponent.fill(ps, 0, 0, this.width, this.height, (alpha << 24) | 0x00FFFFFF);
        }
    }

    private void renderTopHeader(PoseStack ps) {
        String title = "回合制战斗  ·  第 " + state.turnCount + " 回合"
                + ("boss_excavator".equals(state.encounterType) ? "  |  阶段 " + state.bossPhase + "/2" : "");
        GenshinHudOverlay.drawVoxelPanel(ps, 12, 6, this.width - 24, 20, 0xCC0A111C, 0xFF284664, 0xFF38BDF8);
        font.drawShadow(ps, new TextComponent(title), 20, 12, 0xFFE2E8F0);
        String hint = "[Q]普攻  [E]战技  [1-4]插队大招  [A/D/Tab]选目标  [Space]确认";
        font.draw(ps, new TextComponent(hint), this.width - font.width(hint) - 20, 12, 0xFF94A3B8);
    }

    private void renderActionTimeline(PoseStack ps) {
        int x = 12;
        int y = 32;
        int w = 86;
        int h = this.height - 134;
        GenshinHudOverlay.drawVoxelPanel(ps, x, y, w, h, 0xCC09101A, 0xFF1E3A5F, 0xFF38BDF8);
        font.draw(ps, new TextComponent("行动顺序"), x + 16, y + 6, 0xFF7DD3FC);

        record TimelineEntry(String label, float av, int color, boolean isCurrent, int warnCount) {}
        List<TimelineEntry> entries = new ArrayList<>();
        for (int i = 0; i < state.allies.size(); i++) {
            BattleState.AllyUnit a = state.allies.get(i);
            if (!a.isAlive()) continue;
            boolean cur = (i == state.activeAllyIndex);
            entries.add(new TimelineEntry(a.name, cur ? 0f : a.actionValue,
                    cur ? 0xFFFBBF24 : CharacterData.getElementColor(a.element), cur, 0));
        }
        for (BattleState.EnemyUnit e : state.enemies) {
            if (!e.isAlive()) continue;
            entries.add(new TimelineEntry(e.name, e.actionValue, 0xFFF87171, false, e.chargingUlt ? e.chargeCountdown : 0));
        }
        entries.sort(Comparator.comparingDouble(TimelineEntry::av));

        int ey = y + 20;
        for (int i = 0; i < Math.min(entries.size(), 7); i++) {
            TimelineEntry te = entries.get(i);
            if (ey + 20 > y + h - 4) break;
            int bg = te.isCurrent ? 0xD0263B52 : 0x90111B27;
            GenshinHudOverlay.drawVoxelPanel(ps, x + 4, ey, w - 8, 18, bg, te.color, te.color);
            String shortName = te.label.length() > 6 ? te.label.substring(0, 6) : te.label;
            font.draw(ps, new TextComponent((te.isCurrent ? "▶" : "") + shortName), x + 7, ey + 5, te.color);
            String avStr = te.warnCount > 0 ? ("蓄" + te.warnCount) : String.valueOf(Math.round(te.av));
            font.draw(ps, new TextComponent(avStr), x + w - font.width(avStr) - 7, ey + 5, te.warnCount > 0 ? 0xFFFF5252 : 0xFF94A3B8);
            ey += 21;
        }
    }

    private void renderEnemyCards(PoseStack ps) {
        int count = state.enemies.size();
        if (count == 0) return;
        int cardW = Math.min(126, (this.width - 150) / count - 8);
        int totalW = count * (cardW + 8) - 8;
        int startX = 104 + Math.max(0, (this.width - 118 - totalW) / 2);
        int y = 34;

        for (int i = 0; i < count; i++) {
            BattleState.EnemyUnit e = state.enemies.get(i);
            int cx = startX + i * (cardW + 8);
            boolean sel = (i == selectedEnemyIdx && e.isAlive());
            int border = !e.isAlive() ? 0xFF28303D : (sel ? 0xFFFBBF24 : (e.chargingUlt ? 0xFFEF4444 : 0xFF334155));

            GenshinHudOverlay.drawVoxelPanel(ps, cx, y, cardW, 86,
                    e.isAlive() ? (sel ? 0xD81B283A : 0xB80E1622) : 0x700A0E14, border, border);

            if (sel) {
                String tag = "▼ 锁定目标 ▼";
                font.drawShadow(ps, new TextComponent(tag), cx + (cardW - font.width(tag)) / 2.0f, y - 9, 0xFFFBBF24);
            }

            String title = e.name;
            if (font.width(title) > cardW - 10) title = title.substring(0, Math.min(title.length(), 7)) + "..";
            font.drawShadow(ps, new TextComponent(title), cx + 6, y + 6, e.isAlive() ? 0xFFF8FAFC : 0xFF64748B);
            font.draw(ps, new TextComponent("Lv." + e.level), cx + cardW - 28, y + 6, 0xFF94A3B8);

            // Weakness badges
            int wx = cx + 6;
            font.draw(ps, new TextComponent("弱:"), wx, y + 19, 0xFF94A3B8);
            wx += 16;
            for (String w : e.weaknesses) {
                int wCol = CharacterData.getElementColor(w);
                GuiComponent.fill(ps, wx, y + 18, wx + 13, y + 28, 0xCC0B131E);
                font.draw(ps, new TextComponent(BattleState.getElementShort(w)), wx + 2, y + 19, wCol);
                wx += 15;
            }

            // White Toughness Bar
            int barW = cardW - 12;
            GuiComponent.fill(ps, cx + 6, y + 32, cx + 6 + barW, y + 37, 0xCC1E293B);
            float tPct = Mth.clamp(e.toughness / Math.max(1f, e.maxToughness), 0f, 1f);
            GuiComponent.fill(ps, cx + 6, y + 32, cx + 6 + (int) (barW * tPct), y + 37, e.broken ? 0xFF64748B : 0xFFF8FAFC);

            // Red HP Bar
            GuiComponent.fill(ps, cx + 6, y + 40, cx + 6 + barW, y + 49, 0xCC1E293B);
            float hPct = Mth.clamp(e.hp / Math.max(1f, e.maxHp), 0f, 1f);
            GuiComponent.fill(ps, cx + 6, y + 40, cx + 6 + (int) (barW * hPct), y + 49, 0xFFEF4444);
            String hpStr = e.isAlive() ? ((int) e.hp + "/" + (int) e.maxHp) : "已击毁";
            font.draw(ps, new TextComponent(hpStr), cx + (cardW - font.width(hpStr)) / 2, y + 41, 0xFFFFFFFF);

            // Status tags
            StringBuilder statusSb = new StringBuilder();
            if (e.shieldProtected) statusSb.append("[双臂护罩减伤] ");
            if (e.broken) statusSb.append("[已破盾] ");
            if (e.chargingUlt) statusSb.append("[蓄力:").append(e.chargeCountdown).append("] ");
            String sDisp = BattleState.getStatusDisplay(e.statusEffect);
            if (!sDisp.isEmpty() && e.statusTurns > 0) statusSb.append("[").append(sDisp).append("]");

            font.draw(ps, new TextComponent(statusSb.toString()), cx + 6, y + 56,
                    e.chargingUlt ? 0xFFFF5252 : (e.broken ? 0xFFFBBF24 : 0xFF7DD3FC));
        }
    }

    private void renderBattleLogs(PoseStack ps) {
        int x = 104;
        int y = 128;
        int w = this.width - 116;
        int h = Math.max(46, this.height - 232);
        GenshinHudOverlay.drawVoxelPanel(ps, x, y, w, h, 0x90090F18, 0xFF1E293B, 0xFF38BDF8);

        int ly = y + 6;
        int maxShow = Math.max(2, (h - 10) / 12);
        int start = Math.max(0, state.logs.size() - maxShow);
        for (int i = start; i < state.logs.size(); i++) {
            String line = state.logs.get(i);
            int col = line.contains("弱点击破") || line.contains("终结技") ? 0xFFFBBF24
                    : (line.contains("警告") || line.contains("喷射") ? 0xFFF87171 : 0xFFE2E8F0);
            font.draw(ps, new TextComponent("• " + line), x + 8, ly, col);
            ly += 12;
        }
    }

    private void renderAllyCards(PoseStack ps) {
        int startX = 14;
        int y = this.height - 92;
        for (int i = 0; i < state.allies.size(); i++) {
            BattleState.AllyUnit a = state.allies.get(i);
            int ax = startX + i * 92;
            boolean isTurn = (i == state.activeAllyIndex);
            boolean ultReady = a.isAlive() && a.energy >= a.maxEnergy;
            int elemCol = CharacterData.getElementColor(a.element);
            int border = isTurn ? 0xFFFBBF24 : (ultReady ? 0xFF38BDF8 : 0xFF334155);

            GenshinHudOverlay.drawVoxelPanel(ps, ax, y, 88, 78,
                    isTurn ? 0xD81A2E44 : 0xB80E1622, border, border);

            GenshinHudOverlay.drawTexture(ps, getPortraitForChar(a.charId), ax + 4, y + 4, 22, 22, 32, 32);
            String shortName = a.name.contains("·") ? a.name.substring(a.name.indexOf('·') + 1) : a.name;
            font.drawShadow(ps, new TextComponent(shortName), ax + 29, y + 5, elemCol);
            font.draw(ps, new TextComponent(BattleState.getElementShort(a.element)), ax + 29, y + 16, elemCol);

            // HP Bar
            GuiComponent.fill(ps, ax + 4, y + 30, ax + 84, y + 38, 0xCC1E293B);
            float hPct = Mth.clamp(a.hp / Math.max(1f, a.maxHp), 0f, 1f);
            GuiComponent.fill(ps, ax + 4, y + 30, ax + 4 + (int) (80 * hPct), y + 38, hPct > 0.35f ? 0xFF22C55E : 0xFFEF4444);
            if (a.shield > 0) {
                int sW = Math.min(80, (int) (80 * (a.shield / Math.max(1f, a.maxHp))));
                GuiComponent.fill(ps, ax + 4, y + 36, ax + 4 + sW, y + 39, 0xFFE2E8F0);
            }
            font.draw(ps, new TextComponent((int) a.hp + "/" + (int) a.maxHp), ax + 6, y + 41, 0xFFE2E8F0);

            // Energy Bar + Ult prompt
            GuiComponent.fill(ps, ax + 4, y + 53, ax + 84, y + 59, 0xCC1E293B);
            float ePct = Mth.clamp(a.energy / Math.max(1f, a.maxEnergy), 0f, 1f);
            GuiComponent.fill(ps, ax + 4, y + 53, ax + 4 + (int) (80 * ePct), y + 59, ultReady ? 0xFFFBBF24 : 0xFF38BDF8);
            String uText = ultReady ? ("[" + (i + 1) + "] 大招就绪!") : ("能量 " + (int) a.energy + "%");
            font.draw(ps, new TextComponent(uText), ax + 6, y + 63, ultReady ? 0xFFFBBF24 : 0xFF94A3B8);
        }
    }

    private void renderSkillPointPanel(PoseStack ps) {
        int ctrlX = this.width - 210;
        int ctrlY = this.height - 96;
        GenshinHudOverlay.drawVoxelPanel(ps, ctrlX - 6, ctrlY - 18, 204, 88, 0xCC0A121D, 0xFF284664, 0xFFFBBF24);

        font.draw(ps, new TextComponent("战技点:"), ctrlX, ctrlY - 12, 0xFFE2E8F0);
        for (int i = 0; i < state.maxSkillPoints; i++) {
            int ox = ctrlX + 46 + i * 16;
            int oy = ctrlY - 13;
            boolean filled = i < state.skillPoints;
            GuiComponent.fill(ps, ox, oy, ox + 11, oy + 9, filled ? 0xFFFBBF24 : 0xFF1E293B);
        }
        String modeLabel = pendingActionMode == TurnBasedCombatManager.ACTION_SKILL ? "当前：战技 (-1点)" : "当前：普攻 (+1点)";
        font.draw(ps, new TextComponent(modeLabel), ctrlX + 130, ctrlY - 12,
                pendingActionMode == TurnBasedCombatManager.ACTION_SKILL ? 0xFF38BDF8 : 0xFF4ADE80);
    }

    private void renderResultModal(PoseStack ps) {
        int mw = 340;
        int mh = 128;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2 - 12;

        int col = state.victory ? 0xFFFBBF24 : 0xFFEF4444;
        GenshinHudOverlay.drawVoxelPanel(ps, mx, my, mw, mh, 0xF00B131E, col, col);

        String header = state.victory ? "战斗胜利！" : "撤退重整";
        font.drawShadow(ps, new TextComponent(header), mx + (mw - font.width(header)) / 2.0f, my + 14, col);

        String sum = state.rewardSummary != null ? state.rewardSummary : "";
        List<net.minecraft.util.FormattedCharSequence> wrapped = font.split(new TextComponent(sum), mw - 28);
        int ty = my + 36;
        for (var line : wrapped) {
            font.draw(ps, line, mx + 14, ty, 0xFFE2E8F0);
            ty += 12;
        }
    }
}
