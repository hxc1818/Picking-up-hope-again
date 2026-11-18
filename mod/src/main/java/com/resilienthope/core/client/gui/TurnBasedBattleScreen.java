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
    private final BattleStageRenderer stageRenderer = new BattleStageRenderer();
    private int selectedEnemyIdx = 0;
    private int pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
    private int introTicks = 12;
    private int pulseTicks = 0;

    private SciFiButton basicBtn;
    private SciFiButton skillBtn;
    private SciFiButton confirmBtn;
    private SciFiButton fleeBtn;

    // Last rendered floating enemy plate boxes for click selection
    private final List<int[]> plateBoxes = new ArrayList<>(); // {enemyIdx, x, y, w, h}

    public TurnBasedBattleScreen(BattleState initialState) {
        super(new TextComponent("3D 回合制战术舞台"));
        this.state = initialState;
        if (initialState != null) {
            if (!initialState.events.isEmpty()) {
                stageRenderer.enqueueEvents(initialState.events);
            }
            // In Tutorial 1/2/3 step 1, pre-select Skill [E] to feel snappy
            if (initialState.tutorialId > 0 && initialState.tutorialStep == 1 && initialState.skillPoints > 0) {
                pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
            }
        }
        ensureValidTarget();
    }

    public void updateState(BattleState newState) {
        this.state = newState;
        if (newState != null && !newState.events.isEmpty()) {
            stageRenderer.enqueueEvents(newState.events);
        }
        if (newState != null && newState.tutorialId > 0) {
            if (newState.tutorialHighlightBtn == 1 && newState.skillPoints > 0) {
                pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
            } else if (newState.tutorialHighlightBtn == 0) {
                pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
            }
        }
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

    private BattleState.AllyUnit getActiveActor() {
        if (state == null || state.activeAllyIndex < 0 || state.activeAllyIndex >= state.allies.size()) return null;
        return state.allies.get(state.activeAllyIndex);
    }

    private void refreshButtons() {
        clearWidgets();
        if (state == null) return;

        if (state.finished) {
            int cw = 156;
            addRenderableWidget(new SciFiButton(this.width / 2 - cw / 2, this.height / 2 + 42, cw, 20,
                    new TextComponent("[Space] 返回大地图"),
                    btn -> sendAction(TurnBasedCombatManager.ACTION_EXIT_FINISHED, -1), 0xFFFBBF24));
            return;
        }

        BattleState.AllyUnit actor = getActiveActor();
        String basicTitle = actor != null ? CharacterData.getBasicNameFor(actor.charId) : "普攻";
        String skillTitle = actor != null ? CharacterData.getSkillNameFor(actor.charId) : "战技";
        if (basicTitle.length() > 4) basicTitle = basicTitle.substring(0, 4);
        if (skillTitle.length() > 4) skillTitle = skillTitle.substring(0, 4);

        int rightPanelW = 172;
        int ctrlX = this.width - rightPanelW - 6;
        int ctrlY = this.height - 86;

        this.basicBtn = addRenderableWidget(new SciFiButton(ctrlX + 4, ctrlY + 32, 80, 20,
                new TextComponent("[Q]" + basicTitle), btn -> {
            if (pendingActionMode == TurnBasedCombatManager.ACTION_BASIC) executePendingAction();
            else pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
        }, 0xFF4ADE80));

        this.skillBtn = addRenderableWidget(new SciFiButton(ctrlX + 88, ctrlY + 32, 80, 20,
                new TextComponent("[E]" + skillTitle), btn -> {
            if (state.skillPoints <= 0) return;
            if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL) executePendingAction();
            else pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
        }, 0xFF38BDF8));
        this.skillBtn.active = state.skillPoints > 0;

        this.confirmBtn = addRenderableWidget(new SciFiButton(ctrlX + 4, ctrlY + 56, 114, 20,
                new TextComponent("[Space] 确认出手"), btn -> executePendingAction(), 0xFFFBBF24));

        this.fleeBtn = addRenderableWidget(new SciFiButton(ctrlX + 122, ctrlY + 56, 46, 20,
                new TextComponent("撤退"), btn -> sendAction(TurnBasedCombatManager.ACTION_FLEE, -1), 0xFFEF4444));
        this.fleeBtn.active = state.canFlee;
    }

    private void executePendingAction() {
        if (state == null || state.finished) return;
        if (stageRenderer.isAnimating()) {
            stageRenderer.skipAllAnimations();
            return;
        }
        if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL && state.skillPoints <= 0) {
            pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
        }
        sendAction(pendingActionMode, -1);
    }

    private void sendAction(int type, int allyUltIdx) {
        if (stageRenderer.isAnimating()) {
            stageRenderer.skipAllAnimations();
        }
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
        pulseTicks++;
        stageRenderer.tick(state);
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
            if (pendingActionMode == TurnBasedCombatManager.ACTION_BASIC && !stageRenderer.isAnimating()) {
                executePendingAction();
            } else {
                stageRenderer.skipAllAnimations();
                pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_E) {
            if (state.skillPoints > 0) {
                if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL && !stageRenderer.isAnimating()) {
                    executePendingAction();
                } else {
                    stageRenderer.skipAllAnimations();
                    pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
                }
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
            // 1. Click floating enemy plate or 3D enemy model hitbox
            for (int[] box : plateBoxes) {
                if (mouseX >= box[1] && mouseX <= box[1] + box[3] && mouseY >= box[2] && mouseY <= box[2] + box[4]) {
                    this.selectedEnemyIdx = box[0];
                    return true;
                }
            }
            for (int i = 0; i < stageRenderer.enemyAnchors.size() && i < state.enemies.size(); i++) {
                if (!state.enemies.get(i).isAlive()) continue;
                var anc = stageRenderer.enemyAnchors.get(i);
                int dx = (int) mouseX - anc.screenX();
                int dy = (int) mouseY - anc.screenY();
                if (dx * dx + dy * dy <= anc.hitRadius() * anc.hitRadius()) {
                    this.selectedEnemyIdx = i;
                    return true;
                }
            }

            // 2. Click Ally Card for Ultimate Interrupt
            int rightPanelW = 172;
            int ctrlPanelX = this.width - rightPanelW - 6;
            int allyAvailW = ctrlPanelX - 14;
            int nAllies = Math.max(1, state.allies.size());
            int cardGap = 4;
            int allyW = Math.min(88, (allyAvailW - (nAllies - 1) * cardGap) / nAllies);
            int allyY = this.height - 74;
            for (int i = 0; i < state.allies.size(); i++) {
                int ax = 8 + i * (allyW + cardGap);
                if (mouseX >= ax && mouseX <= ax + allyW && mouseY >= allyY && mouseY <= allyY + 68) {
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

        // Scenic tactical backdrop (space starship interior vs frozen cavern)
        int topGrad = state.iceTheme ? 0x90071828 : 0x90080D16;
        int botGrad = state.iceTheme ? 0xB80E2236 : 0xB80B1422;
        fillGradient(ps, 0, 0, this.width, this.height, topGrad, botGrad);

        // Render full 3D Battle Stage (3D characters, 3D mechs, 3D weapons, dash & slash VFX)
        stageRenderer.renderStage(ps, font, this.width, this.height, state, selectedEnemyIdx, partialTicks);

        // Render clean 2D Tactical HUD over 3D Stage
        renderTopHeader(ps);
        renderActionTimeline(ps);
        renderEnemyFloatingPlates(ps);
        renderAllyCards(ps);
        renderSkillPointPanel(ps);
        renderActionBanner(ps);
        renderTutorialOverlay(ps);

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
        String title = "3D 战术回合 · 第 " + state.turnCount + " 回合"
                + ("boss_excavator".equals(state.encounterType) ? " | 阶段 " + state.bossPhase + "/2" : "");
        GenshinHudOverlay.drawVoxelPanel(ps, 6, 4, this.width - 12, 18, 0xD00A111C, 0xFF284664, 0xFF38BDF8);
        font.drawShadow(ps, new TextComponent(title), 12, 9, 0xFFE2E8F0);
        String hint = stageRenderer.isAnimating()
                ? "[Space] 跳过动画演出"
                : (this.width >= 520 ? "[Q]普攻  [E]战技  [1-4]大招插队  [Tab/点击3D敌]选目标" : "[Q]普 [E]技 [1-4]大 [Tab]选敌");
        font.draw(ps, new TextComponent(hint), this.width - font.width(hint) - 12, 9,
                stageRenderer.isAnimating() ? 0xFFFBBF24 : 0xFF94A3B8);
    }

    private void renderActionBanner(PoseStack ps) {
        BattleState.AnimEvent ev = stageRenderer.getCurrentAnim();
        String txt = (ev != null && ev.bannerText != null && !ev.bannerText.isEmpty())
                ? ev.bannerText
                : (!state.logs.isEmpty() && state.tutorialId == 0 ? state.logs.get(state.logs.size() - 1) : "");
        if (txt.isEmpty()) return;
        if (state.tutorialId > 0 && state.tutorialStep > 0 && ev == null) return; // Don't crowd tutorial box

        int bw = Math.min(this.width - 104, font.width(txt) + 18);
        int bx = 82 + Math.max(0, (this.width - 88 - bw) / 2);
        int by = 25;
        int border = ev != null ? CharacterData.getElementColor(ev.element) : 0xFF38BDF8;
        GenshinHudOverlay.drawVoxelPanel(ps, bx, by, bw, 16, 0xD809121E, border, 0xFFFBBF24);
        font.drawShadow(ps, new TextComponent(txt), bx + (bw - font.width(txt)) / 2.0f, by + 4, 0xFFF8FAFC);
    }

    private void renderActionTimeline(PoseStack ps) {
        int x = 6;
        int y = 26;
        int w = 72;
        int h = this.height - 106;
        GenshinHudOverlay.drawVoxelPanel(ps, x, y, w, h, 0xC809101A, 0xFF1E3A5F, 0xFF38BDF8);
        font.draw(ps, new TextComponent("行动序"), x + 18, y + 5, 0xFF7DD3FC);

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
            if (ey + 17 > y + h - 3) break;
            int bg = te.isCurrent ? 0xD0263B52 : 0x90111B27;
            GenshinHudOverlay.drawVoxelPanel(ps, x + 3, ey, w - 6, 15, bg, te.color, te.color);
            String shortName = te.label.length() > 4 ? te.label.substring(0, 4) : te.label;
            font.draw(ps, new TextComponent((te.isCurrent ? "▶" : "") + shortName), x + 5, ey + 4, te.color);
            String avStr = te.warnCount > 0 ? ("蓄" + te.warnCount) : String.valueOf(Math.round(te.av));
            font.draw(ps, new TextComponent(avStr), x + w - font.width(avStr) - 5, ey + 4, te.warnCount > 0 ? 0xFFFF5252 : 0xFF94A3B8);
            ey += 17;
        }
    }

    private void renderEnemyFloatingPlates(PoseStack ps) {
        plateBoxes.clear();
        if (state.enemies.isEmpty() || stageRenderer.enemyAnchors.isEmpty()) return;

        List<Integer> living = new ArrayList<>();
        for (int i = 0; i < state.enemies.size(); i++) {
            if (state.enemies.get(i).isAlive()) living.add(i);
        }
        if (living.isEmpty()) return;

        int plateW = Math.min(96, Math.max(72, (this.width - 104) / Math.max(1, living.size()) - 4));
        int plateH = 42;
        int minAllowedX = 82;
        int maxAllowedX = this.width - plateW - 6;
        int minY = (state.tutorialId > 0 && state.tutorialStep > 0) ? 74 : 44;

        // Compute non-overlapping horizontal slots aligned to each 3D enemy's screenX
        List<int[]> positioned = new ArrayList<>();
        for (int idx : living) {
            if (idx >= stageRenderer.enemyAnchors.size()) continue;
            var anc = stageRenderer.enemyAnchors.get(idx);
            int px = Mth.clamp(anc.screenX() - plateW / 2, minAllowedX, Math.max(minAllowedX, maxAllowedX));
            int py = Mth.clamp(anc.topY() - plateH - 6, minY, this.height - 135);
            positioned.add(new int[]{idx, px, py});
        }
        positioned.sort(Comparator.comparingInt(a -> a[1]));
        for (int k = 1; k < positioned.size(); k++) {
            if (positioned.get(k)[1] < positioned.get(k - 1)[1] + plateW + 3) {
                positioned.get(k)[1] = positioned.get(k - 1)[1] + plateW + 3;
            }
        }
        for (int k = positioned.size() - 1; k >= 0; k--) {
            if (positioned.get(k)[1] > maxAllowedX) {
                positioned.get(k)[1] = maxAllowedX;
                if (k > 0 && positioned.get(k - 1)[1] > positioned.get(k)[1] - plateW - 3) {
                    positioned.get(k - 1)[1] = positioned.get(k)[1] - plateW - 3;
                }
            }
        }

        for (int[] p : positioned) {
            int i = p[0];
            int cx = p[1];
            int cy = p[2];
            BattleState.EnemyUnit e = state.enemies.get(i);
            plateBoxes.add(new int[]{i, cx, cy, plateW, plateH});

            boolean sel = (i == selectedEnemyIdx);
            int border = sel ? 0xFFFBBF24 : (e.chargingUlt ? 0xFFEF4444 : 0xFF334155);
            GenshinHudOverlay.drawVoxelPanel(ps, cx, cy, plateW, plateH, sel ? 0xDC1B293C : 0xC00D1520, border, border);

            if (sel) {
                String tag = "▼ 锁定";
                font.drawShadow(ps, new TextComponent(tag), cx + (plateW - font.width(tag)) / 2.0f, cy - 9, 0xFFFBBF24);
            }

            String title = e.name;
            int maxTitleW = plateW - 8;
            while (font.width(title) > maxTitleW && title.length() > 3) {
                title = title.substring(0, title.length() - 1);
            }
            font.drawShadow(ps, new TextComponent(title), cx + 4, cy + 3, 0xFFF8FAFC);

            // Weaknesses + status badge
            int wx = cx + 4;
            font.draw(ps, new TextComponent("弱:"), wx, cy + 12, 0xFF94A3B8);
            wx += 14;
            for (String w : e.weaknesses) {
                font.draw(ps, new TextComponent(BattleState.getElementShort(w)), wx, cy + 12, CharacterData.getElementColor(w));
                wx += 11;
            }
            if (e.debuffDefTurns > 0) {
                font.draw(ps, new TextComponent("破甲"), cx + plateW - 22, cy + 12, 0xFFFBBF24);
            } else if (e.broken) {
                font.draw(ps, new TextComponent("破盾"), cx + plateW - 22, cy + 12, 0xFFFBBF24);
            }

            // Toughness Bar + HP Bar
            int barW = plateW - 8;
            GuiComponent.fill(ps, cx + 4, cy + 21, cx + 4 + barW, cy + 24, 0xCC1E293B);
            float tPct = Mth.clamp(e.toughness / Math.max(1f, e.maxToughness), 0f, 1f);
            GuiComponent.fill(ps, cx + 4, cy + 21, cx + 4 + (int) (barW * tPct), cy + 24, e.broken ? 0xFF64748B : 0xFFF8FAFC);

            GuiComponent.fill(ps, cx + 4, cy + 25, cx + 4 + barW, cy + 31, 0xCC1E293B);
            float hPct = Mth.clamp(e.hp / Math.max(1f, e.maxHp), 0f, 1f);
            GuiComponent.fill(ps, cx + 4, cy + 25, cx + 4 + (int) (barW * hPct), cy + 31, 0xFFEF4444);

            String intent = e.intentText != null ? e.intentText : "";
            int intentCol = intent.contains("全体") || intent.contains("蓄力") ? 0xFFFF6B6B : 0xFFFCD34D;
            font.draw(ps, new TextComponent(intent), cx + 4, cy + 33, intentCol);
        }
    }

    private void renderAllyCards(PoseStack ps) {
        int rightPanelW = 172;
        int ctrlPanelX = this.width - rightPanelW - 6;
        int availW = ctrlPanelX - 14;
        int n = Math.max(1, state.allies.size());
        int gap = 4;
        int cardW = Math.min(88, (availW - (n - 1) * gap) / n);
        int y = this.height - 74;

        for (int i = 0; i < state.allies.size(); i++) {
            BattleState.AllyUnit a = state.allies.get(i);
            int ax = 8 + i * (cardW + gap);
            boolean isTurn = (i == state.activeAllyIndex);
            boolean ultReady = a.isAlive() && a.energy >= a.maxEnergy;
            boolean tutUltHighlight = (state.tutorialId > 0 && state.tutorialHighlightBtn == (10 + i));
            int elemCol = CharacterData.getElementColor(a.element);
            int border = tutUltHighlight ? ((pulseTicks % 10 < 5) ? 0xFFFBBF24 : 0xFFFFFFFF)
                    : (isTurn ? 0xFFFBBF24 : (ultReady ? 0xFF38BDF8 : 0xFF334155));

            GenshinHudOverlay.drawVoxelPanel(ps, ax, y, cardW, 68,
                    isTurn ? 0xD81A2E44 : 0xB80E1622, border, border);

            GenshinHudOverlay.drawPortrait(ps, getPortraitForChar(a.charId), ax + 4, y + 4, 20, 20);
            String shortName = a.name.contains("·") ? a.name.substring(a.name.indexOf('·') + 1) : a.name;
            font.drawShadow(ps, new TextComponent(shortName), ax + 27, y + 4, elemCol);
            font.draw(ps, new TextComponent(BattleState.getElementShort(a.element) + (a.buffAtkTurns > 0 ? " +攻" : "")),
                    ax + 27, y + 14, a.buffAtkTurns > 0 ? 0xFF4ADE80 : elemCol);

            int barW = cardW - 8;
            GuiComponent.fill(ps, ax + 4, y + 27, ax + 4 + barW, y + 34, 0xCC1E293B);
            float hPct = Mth.clamp(a.hp / Math.max(1f, a.maxHp), 0f, 1f);
            GuiComponent.fill(ps, ax + 4, y + 27, ax + 4 + (int) (barW * hPct), y + 34, hPct > 0.35f ? 0xFF22C55E : 0xFFEF4444);
            if (a.shield > 0) {
                int sW = Math.min(barW, (int) (barW * (a.shield / Math.max(1f, a.maxHp))));
                GuiComponent.fill(ps, ax + 4, y + 33, ax + 4 + sW, y + 35, 0xFFE2E8F0);
            }
            font.draw(ps, new TextComponent((int) a.hp + "/" + (int) a.maxHp), ax + 4, y + 37, 0xFFE2E8F0);

            GuiComponent.fill(ps, ax + 4, y + 47, ax + 4 + barW, y + 52, 0xCC1E293B);
            float ePct = Mth.clamp(a.energy / Math.max(1f, a.maxEnergy), 0f, 1f);
            GuiComponent.fill(ps, ax + 4, y + 47, ax + 4 + (int) (barW * ePct), y + 52, ultReady ? 0xFFFBBF24 : 0xFF38BDF8);
            String uText = ultReady ? ("[" + (i + 1) + "]大招") : ((int) a.energy + "%");
            font.draw(ps, new TextComponent(uText), ax + 4, y + 56, ultReady ? 0xFFFBBF24 : 0xFF94A3B8);

            if (tutUltHighlight) {
                int bounce = (pulseTicks % 12 < 6) ? -2 : 1;
                String arrow = "▼ 按[" + (i + 1) + "]";
                font.drawShadow(ps, new TextComponent(arrow), ax + (cardW - font.width(arrow)) / 2.0f, y - 11 + bounce, 0xFFFBBF24);
            }
        }
    }

    private void renderSkillPointPanel(PoseStack ps) {
        int rightPanelW = 172;
        int ctrlX = this.width - rightPanelW - 6;
        int ctrlY = this.height - 86;
        GenshinHudOverlay.drawVoxelPanel(ps, ctrlX, ctrlY, rightPanelW, 80, 0xD40A121D, 0xFF284664, 0xFFFBBF24);

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

        BattleState.AllyUnit actor = getActiveActor();
        if (actor != null) {
            String desc = pendingActionMode == TurnBasedCombatManager.ACTION_SKILL
                    ? CharacterData.getSkillDescFor(actor.charId, actor.role, actor.element)
                    : CharacterData.getBasicDescFor(actor.charId, actor.element);
            int colon = desc.indexOf('：');
            String clean = colon > 0 ? desc.substring(colon + 1) : desc;
            while (font.width(clean) > rightPanelW - 12 && clean.length() > 6) {
                clean = clean.substring(0, clean.length() - 1);
            }
            font.draw(ps, new TextComponent(clean), ctrlX + 6, ctrlY + 19, 0xFFCBD5E1);
        }

        // Bouncing tutorial arrow above [Q] or [E] button
        if (state.tutorialId > 0 && state.tutorialStep > 0 && !stageRenderer.isAnimating()) {
            int bounce = (pulseTicks % 12 < 6) ? -2 : 1;
            if (state.tutorialHighlightBtn == 0 && basicBtn != null) {
                String tag = "▼ 按[Q]";
                font.drawShadow(ps, new TextComponent(tag), ctrlX + 44 - font.width(tag) / 2.0f, ctrlY - 10 + bounce, 0xFF4ADE80);
            } else if (state.tutorialHighlightBtn == 1 && skillBtn != null) {
                String tag = "▼ 按[E]";
                font.drawShadow(ps, new TextComponent(tag), ctrlX + 128 - font.width(tag) / 2.0f, ctrlY - 10 + bounce, 0xFF38BDF8);
            }
        }
    }

    private void renderTutorialOverlay(PoseStack ps) {
        if (state.tutorialId <= 0 || state.tutorialStep <= 0 || state.finished) return;
        if (state.tutorialPrompt == null || state.tutorialPrompt.isEmpty()) return;

        int boxW = Math.min(280, this.width - 100);
        int bx = 82 + Math.max(0, (this.width - 88 - boxW) / 2);
        int by = 24;

        String speakerId = switch (state.tutorialId) {
            case 2 -> "chenfeng";
            case 3 -> "suqing";
            default -> "linye";
        };
        List<FormattedCharSequence> lines = font.split(new TextComponent(state.tutorialPrompt), boxW - 42);
        int boxH = Math.max(36, 16 + Math.min(3, lines.size()) * 10);

        GenshinHudOverlay.drawVoxelPanel(ps, bx, by, boxW, boxH, 0xE60A1522, 0xFFFBBF24, 0xFF38BDF8);
        GenshinHudOverlay.drawPortrait(ps, getPortraitForChar(speakerId), bx + 5, by + 5, 24, 24);
        font.drawShadow(ps, new TextComponent(state.tutorialTitle), bx + 34, by + 4, 0xFFFBBF24);

        int ty = by + 15;
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            font.draw(ps, lines.get(i), bx + 34, ty, 0xFFF8FAFC);
            ty += 10;
        }
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
