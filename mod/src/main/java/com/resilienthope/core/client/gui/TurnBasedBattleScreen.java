package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.combat.BattleState;
import com.resilienthope.core.combat.TurnBasedCombatManager;
import com.resilienthope.core.network.PacketBattleAction;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.team.CharacterData;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
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
    private int pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
    private int introTicks = 12;

    private SciFiButton basicBtn;
    private SciFiButton skillBtn;
    private SciFiButton confirmBtn;
    private SciFiButton fleeBtn;

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
            int cw = 150;
            addRenderableWidget(new SciFiButton(this.width / 2 - cw / 2, this.height / 2 + 40, cw, 20,
                    new TextComponent("[Space] 返回大地图"),
                    btn -> sendAction(TurnBasedCombatManager.ACTION_EXIT_FINISHED, -1), 0xFFFBBF24));
            return;
        }

        int rightPanelW = 166;
        int ctrlX = this.width - rightPanelW - 8;
        int ctrlY = this.height - 76;

        this.basicBtn = addRenderableWidget(new SciFiButton(ctrlX + 4, ctrlY + 20, 77, 20,
                new TextComponent("[Q]普攻 +1"), btn -> {
            if (pendingActionMode == TurnBasedCombatManager.ACTION_BASIC) executePendingAction();
            else pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
        }, 0xFF4ADE80));

        this.skillBtn = addRenderableWidget(new SciFiButton(ctrlX + 85, ctrlY + 20, 77, 20,
                new TextComponent("[E]战技 -1"), btn -> {
            if (state.skillPoints <= 0) return;
            if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL) executePendingAction();
            else pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
        }, 0xFF38BDF8));
        this.skillBtn.active = state.skillPoints > 0;

        this.confirmBtn = addRenderableWidget(new SciFiButton(ctrlX + 4, ctrlY + 44, 108, 20,
                new TextComponent("[Space] 确认出手"), btn -> executePendingAction(), 0xFFFBBF24));

        this.fleeBtn = addRenderableWidget(new SciFiButton(ctrlX + 116, ctrlY + 44, 46, 20,
                new TextComponent("撤退"), btn -> sendAction(TurnBasedCombatManager.ACTION_FLEE, -1), 0xFFEF4444));
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
            int count = state.enemies.size();
            int availW = this.width - 100;
            int cardW = Math.min(130, Math.max(84, (availW - (count - 1) * 6) / Math.max(1, count)));
            int totalW = count * (cardW + 6) - 6;
            int startX = 92 + Math.max(0, (availW - totalW) / 2);
            int enemyY = 28;
            for (int i = 0; i < count; i++) {
                int cx = startX + i * (cardW + 6);
                if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= enemyY && mouseY <= enemyY + 74) {
                    if (state.enemies.get(i).isAlive()) {
                        this.selectedEnemyIdx = i;
                        return true;
                    }
                }
            }

            int rightPanelW = 166;
            int ctrlPanelX = this.width - rightPanelW - 8;
            int allyAvailW = ctrlPanelX - 14;
            int nAllies = Math.max(1, state.allies.size());
            int cardGap = 4;
            int allyW = Math.min(86, (allyAvailW - (nAllies - 1) * cardGap) / nAllies);
            int allyY = this.height - 76;
            for (int i = 0; i < state.allies.size(); i++) {
                int ax = 8 + i * (allyW + cardGap);
                if (mouseX >= ax && mouseX <= ax + allyW && mouseY >= allyY && mouseY <= allyY + 70) {
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

        GuiComponent.fill(ps, 0, 0, this.width, this.height, 0x58060B14);

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
            int alpha = (int) ((introTicks / 12.0f) * 180);
            GuiComponent.fill(ps, 0, 0, this.width, this.height, (alpha << 24) | 0x00FFFFFF);
        }
    }

    private void renderTopHeader(PoseStack ps) {
        String title = "回合制战术 · 第 " + state.turnCount + " 回合"
                + ("boss_excavator".equals(state.encounterType) ? " | 阶段 " + state.bossPhase + "/2" : "");
        GenshinHudOverlay.drawVoxelPanel(ps, 8, 4, this.width - 16, 18, 0xD00A111C, 0xFF284664, 0xFF38BDF8);
        font.drawShadow(ps, new TextComponent(title), 14, 9, 0xFFE2E8F0);
        String hint = this.width >= 500
                ? "[Q]普攻  [E]战技  [1-4]大招  [A/D/Tab]选敌  [Space]确认"
                : "[Q]普 [E]技 [1-4]大 [Tab]选敌";
        font.draw(ps, new TextComponent(hint), this.width - font.width(hint) - 14, 9, 0xFF94A3B8);
    }

    private void renderActionTimeline(PoseStack ps) {
        int x = 8;
        int y = 26;
        int w = 78;
        int h = this.height - 108;
        GenshinHudOverlay.drawVoxelPanel(ps, x, y, w, h, 0xD009101A, 0xFF1E3A5F, 0xFF38BDF8);
        font.draw(ps, new TextComponent("行动队列"), x + 16, y + 5, 0xFF7DD3FC);

        record TimelineEntry(String label, float av, int color, boolean isCurrent, int warnCount) {}
        List<TimelineEntry> entries = new ArrayList<>();
        for (int i = 0; i < state.allies.size(); i++) {
            BattleState.AllyUnit a = state.allies.get(i);
            if (!a.isAlive()) continue;
            boolean cur = (i == state.activeAllyIndex);
            String shortN = a.name.contains("·") ? a.name.substring(a.name.indexOf('·') + 1) : a.name;
            entries.add(new TimelineEntry(shortN, cur ? 0f : a.actionValue,
                    cur ? 0xFFFBBF24 : CharacterData.getElementColor(a.element), cur, 0));
        }
        for (BattleState.EnemyUnit e : state.enemies) {
            if (!e.isAlive()) continue;
            entries.add(new TimelineEntry(e.name, e.actionValue, 0xFFF87171, false, e.chargingUlt ? e.chargeCountdown : 0));
        }
        entries.sort(Comparator.comparingDouble(TimelineEntry::av));

        int ey = y + 17;
        for (TimelineEntry te : entries) {
            if (ey + 18 > y + h - 3) break;
            int bg = te.isCurrent ? 0xD0263B52 : 0x90111B27;
            GenshinHudOverlay.drawVoxelPanel(ps, x + 3, ey, w - 6, 16, bg, te.color, te.color);
            String shortName = te.label.length() > 4 ? te.label.substring(0, 4) : te.label;
            font.draw(ps, new TextComponent((te.isCurrent ? "▶" : "") + shortName), x + 5, ey + 4, te.color);
            String avStr = te.warnCount > 0 ? ("蓄" + te.warnCount) : String.valueOf(Math.round(te.av));
            font.draw(ps, new TextComponent(avStr), x + w - font.width(avStr) - 5, ey + 4, te.warnCount > 0 ? 0xFFFF5252 : 0xFF94A3B8);
            ey += 18;
        }
    }

    private void renderEnemyCards(PoseStack ps) {
        int count = state.enemies.size();
        if (count == 0) return;
        int availW = this.width - 100;
        int cardW = Math.min(130, Math.max(84, (availW - (count - 1) * 6) / count));
        int totalW = count * (cardW + 6) - 6;
        int startX = 92 + Math.max(0, (availW - totalW) / 2);
        int y = 28;

        for (int i = 0; i < count; i++) {
            BattleState.EnemyUnit e = state.enemies.get(i);
            int cx = startX + i * (cardW + 6);
            boolean sel = (i == selectedEnemyIdx && e.isAlive());
            int border = !e.isAlive() ? 0xFF28303D : (sel ? 0xFFFBBF24 : (e.chargingUlt ? 0xFFEF4444 : 0xFF334155));

            GenshinHudOverlay.drawVoxelPanel(ps, cx, y, cardW, 74,
                    e.isAlive() ? (sel ? 0xD81B283A : 0xB80E1622) : 0x700A0E14, border, border);

            if (sel) {
                String tag = "▼ 锁定目标 ▼";
                font.drawShadow(ps, new TextComponent(tag), cx + (cardW - font.width(tag)) / 2.0f, y - 9, 0xFFFBBF24);
            }

            String title = e.name;
            int maxTitleW = cardW - 34;
            while (font.width(title) > maxTitleW && title.length() > 3) {
                title = title.substring(0, title.length() - 1);
            }
            font.drawShadow(ps, new TextComponent(title), cx + 5, y + 5, e.isAlive() ? 0xFFF8FAFC : 0xFF64748B);
            String lvStr = "Lv" + e.level;
            font.draw(ps, new TextComponent(lvStr), cx + cardW - font.width(lvStr) - 4, y + 5, 0xFF94A3B8);

            // Weaknesses
            int wx = cx + 5;
            font.draw(ps, new TextComponent("弱:"), wx, y + 17, 0xFF94A3B8);
            wx += 16;
            for (String w : e.weaknesses) {
                int wCol = CharacterData.getElementColor(w);
                GuiComponent.fill(ps, wx, y + 16, wx + 13, y + 26, 0xCC0B131E);
                font.draw(ps, new TextComponent(BattleState.getElementShort(w)), wx + 2, y + 17, wCol);
                wx += 15;
            }

            // White Toughness Bar
            int barW = cardW - 10;
            GuiComponent.fill(ps, cx + 5, y + 29, cx + 5 + barW, y + 34, 0xCC1E293B);
            float tPct = Mth.clamp(e.toughness / Math.max(1f, e.maxToughness), 0f, 1f);
            GuiComponent.fill(ps, cx + 5, y + 29, cx + 5 + (int) (barW * tPct), y + 34, e.broken ? 0xFF64748B : 0xFFF8FAFC);

            // Red HP Bar
            GuiComponent.fill(ps, cx + 5, y + 37, cx + 5 + barW, y + 46, 0xCC1E293B);
            float hPct = Mth.clamp(e.hp / Math.max(1f, e.maxHp), 0f, 1f);
            GuiComponent.fill(ps, cx + 5, y + 37, cx + 5 + (int) (barW * hPct), y + 46, 0xFFEF4444);
            String hpStr = e.isAlive() ? ((int) e.hp + "/" + (int) e.maxHp) : "已击毁";
            font.draw(ps, new TextComponent(hpStr), cx + (cardW - font.width(hpStr)) / 2, y + 38, 0xFFFFFFFF);

            // Status tags
            StringBuilder statusSb = new StringBuilder();
            if (e.shieldProtected) statusSb.append("[双臂减伤] ");
            if (e.broken) statusSb.append("[破盾] ");
            if (e.chargingUlt) statusSb.append("[蓄力:").append(e.chargeCountdown).append("] ");
            String sDisp = BattleState.getStatusDisplay(e.statusEffect);
            if (!sDisp.isEmpty() && e.statusTurns > 0) statusSb.append("[").append(sDisp).append("]");

            String stStr = statusSb.toString();
            if (!stStr.isEmpty()) {
                for (FormattedCharSequence line : font.split(new TextComponent(stStr), barW)) {
                    font.draw(ps, line, cx + 5, y + 51,
                            e.chargingUlt ? 0xFFFF5252 : (e.broken ? 0xFFFBBF24 : 0xFF7DD3FC));
                    break;
                }
            }
        }
    }

    private void renderBattleLogs(PoseStack ps) {
        int x = 92;
        int y = 106;
        int w = this.width - 100;
        int h = Math.max(36, this.height - 190);
        GenshinHudOverlay.drawVoxelPanel(ps, x, y, w, h, 0xA8090F18, 0xFF1E293B, 0xFF38BDF8);

        List<FormattedCharSequence> wrappedLines = new ArrayList<>();
        List<Integer> lineColors = new ArrayList<>();
        for (String line : state.logs) {
            int col = line.contains("弱点击破") || line.contains("终结技") ? 0xFFFBBF24
                    : (line.contains("警告") || line.contains("喷射") ? 0xFFF87171 : 0xFFE2E8F0);
            for (FormattedCharSequence seq : font.split(new TextComponent("• " + line), w - 14)) {
                wrappedLines.add(seq);
                lineColors.add(col);
            }
        }

        int maxShow = Math.max(2, (h - 8) / 11);
        int start = Math.max(0, wrappedLines.size() - maxShow);
        int ly = y + 5;
        for (int i = start; i < wrappedLines.size(); i++) {
            if (ly + 9 > y + h - 3) break;
            font.draw(ps, wrappedLines.get(i), x + 7, ly, lineColors.get(i));
            ly += 11;
        }
    }

    private void renderAllyCards(PoseStack ps) {
        int rightPanelW = 166;
        int ctrlPanelX = this.width - rightPanelW - 8;
        int availW = ctrlPanelX - 14;
        int n = Math.max(1, state.allies.size());
        int gap = 4;
        int cardW = Math.min(86, (availW - (n - 1) * gap) / n);
        int y = this.height - 76;

        for (int i = 0; i < state.allies.size(); i++) {
            BattleState.AllyUnit a = state.allies.get(i);
            int ax = 8 + i * (cardW + gap);
            boolean isTurn = (i == state.activeAllyIndex);
            boolean ultReady = a.isAlive() && a.energy >= a.maxEnergy;
            int elemCol = CharacterData.getElementColor(a.element);
            int border = isTurn ? 0xFFFBBF24 : (ultReady ? 0xFF38BDF8 : 0xFF334155);

            GenshinHudOverlay.drawVoxelPanel(ps, ax, y, cardW, 70,
                    isTurn ? 0xD81A2E44 : 0xB80E1622, border, border);

            GenshinHudOverlay.drawPortrait(ps, getPortraitForChar(a.charId), ax + 4, y + 4, 20, 20);
            String shortName = a.name.contains("·") ? a.name.substring(a.name.indexOf('·') + 1) : a.name;
            font.drawShadow(ps, new TextComponent(shortName), ax + 27, y + 4, elemCol);
            font.draw(ps, new TextComponent(BattleState.getElementShort(a.element)), ax + 27, y + 14, elemCol);

            int barW = cardW - 8;
            GuiComponent.fill(ps, ax + 4, y + 27, ax + 4 + barW, y + 34, 0xCC1E293B);
            float hPct = Mth.clamp(a.hp / Math.max(1f, a.maxHp), 0f, 1f);
            GuiComponent.fill(ps, ax + 4, y + 27, ax + 4 + (int) (barW * hPct), y + 34, hPct > 0.35f ? 0xFF22C55E : 0xFFEF4444);
            if (a.shield > 0) {
                int sW = Math.min(barW, (int) (barW * (a.shield / Math.max(1f, a.maxHp))));
                GuiComponent.fill(ps, ax + 4, y + 33, ax + 4 + sW, y + 35, 0xFFE2E8F0);
            }
            font.draw(ps, new TextComponent((int) a.hp + "/" + (int) a.maxHp), ax + 4, y + 37, 0xFFE2E8F0);

            GuiComponent.fill(ps, ax + 4, y + 48, ax + 4 + barW, y + 53, 0xCC1E293B);
            float ePct = Mth.clamp(a.energy / Math.max(1f, a.maxEnergy), 0f, 1f);
            GuiComponent.fill(ps, ax + 4, y + 48, ax + 4 + (int) (barW * ePct), y + 53, ultReady ? 0xFFFBBF24 : 0xFF38BDF8);
            String uText = ultReady ? ("[" + (i + 1) + "] 大招!") : ((int) a.energy + "%");
            font.draw(ps, new TextComponent(uText), ax + 4, y + 57, ultReady ? 0xFFFBBF24 : 0xFF94A3B8);
        }
    }

    private void renderSkillPointPanel(PoseStack ps) {
        int rightPanelW = 166;
        int ctrlX = this.width - rightPanelW - 8;
        int ctrlY = this.height - 76;
        GenshinHudOverlay.drawVoxelPanel(ps, ctrlX, ctrlY, rightPanelW, 70, 0xD00A121D, 0xFF284664, 0xFFFBBF24);

        font.draw(ps, new TextComponent("战技点:"), ctrlX + 6, ctrlY + 6, 0xFFE2E8F0);
        for (int i = 0; i < state.maxSkillPoints; i++) {
            int ox = ctrlX + 46 + i * 12;
            int oy = ctrlY + 6;
            boolean filled = i < state.skillPoints;
            GuiComponent.fill(ps, ox, oy, ox + 9, oy + 8, filled ? 0xFFFBBF24 : 0xFF1E293B);
        }
        String modeLabel = pendingActionMode == TurnBasedCombatManager.ACTION_SKILL ? "战技(-1)" : "普攻(+1)";
        font.draw(ps, new TextComponent(modeLabel), ctrlX + rightPanelW - font.width(modeLabel) - 6, ctrlY + 6,
                pendingActionMode == TurnBasedCombatManager.ACTION_SKILL ? 0xFF38BDF8 : 0xFF4ADE80);
    }

    private void renderResultModal(PoseStack ps) {
        int mw = Math.min(340, this.width - 32);
        int mh = 120;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2 - 12;

        int col = state.victory ? 0xFFFBBF24 : 0xFFEF4444;
        GenshinHudOverlay.drawVoxelPanel(ps, mx, my, mw, mh, 0xF00B131E, col, col);

        String header = state.victory ? "战斗胜利！" : "撤退重整";
        font.drawShadow(ps, new TextComponent(header), mx + (mw - font.width(header)) / 2.0f, my + 12, col);

        String sum = state.rewardSummary != null ? state.rewardSummary : "";
        List<FormattedCharSequence> wrapped = font.split(new TextComponent(sum), mw - 24);
        int ty = my + 32;
        for (var line : wrapped) {
            font.draw(ps, line, mx + 12, ty, 0xFFE2E8F0);
            ty += 12;
        }
    }
}
