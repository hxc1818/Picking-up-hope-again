package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.client.battle.BattleCameraController;
import com.resilienthope.core.client.battle.BattleSceneClientController;
import com.resilienthope.core.client.battle.BattleVfxManager;
import com.resilienthope.core.combat.ArenaTheme;
import com.resilienthope.core.combat.BattleArenaLayout;
import com.resilienthope.core.combat.BattleCombatPreview;
import com.resilienthope.core.combat.BattleState;
import com.resilienthope.core.combat.TurnBasedCombatManager;
import com.resilienthope.core.network.PacketBattleAction;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.team.CharacterData;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Transparent tactical HUD layered directly over the live 3D Minecraft Hangar Deck battle scene.
 * Supports 3D entity raycast targeting, minimal graphic-only enemy overhead bars with flashing
 * expected toughness reduction, two-step Ultimate aiming preview, and keyframed Ult cut-in banners.
 */
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
    private int pendingUltSlot = -1;
    private int introTicks = 10;
    private int pulseTicks = 0;

    private SciFiButton basicBtn;
    private SciFiButton skillBtn;
    private SciFiButton confirmBtn;
    private SciFiButton fleeBtn;

    // {enemyIdx, x, y, w, h}
    private final List<int[]> overheadHitboxes = new ArrayList<>();

    public TurnBasedBattleScreen(BattleState initialState) {
        super(new TextComponent("3D 回合制战术舞台"));
        this.state = initialState;
        BattleSceneClientController.setActiveScreen(this);
        if (initialState != null && initialState.tutorialId > 0
                && initialState.tutorialStep == 1 && initialState.skillPoints > 0) {
            pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
        }
        ensureValidTarget();
        syncSelectionContext();
    }

    public void updateState(BattleState newState) {
        this.state = newState;
        BattleSceneClientController.setActiveScreen(this);
        if (newState == null || newState.finished) {
            pendingUltSlot = -1;
        } else {
            if (pendingUltSlot >= 0) {
                if (pendingUltSlot >= newState.allies.size()) {
                    pendingUltSlot = -1;
                } else {
                    BattleState.AllyUnit u = newState.allies.get(pendingUltSlot);
                    if (!u.isAlive() || u.energy < u.maxEnergy) {
                        pendingUltSlot = -1;
                    }
                }
            }
            if (newState.tutorialId > 0) {
                if (newState.tutorialHighlightBtn == 1 && newState.skillPoints > 0) {
                    pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
                } else if (newState.tutorialHighlightBtn == 0) {
                    pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
                }
            }
        }
        ensureValidTarget();
        syncSelectionContext();
        refreshButtons();
    }

    private void syncSelectionContext() {
        BattleSceneClientController.updateSelectionContext(selectedEnemyIdx, pendingActionMode, pendingUltSlot);
    }

    private void ensureValidTarget() {
        if (state == null || state.enemies.isEmpty()) return;
        if (selectedEnemyIdx >= 0 && selectedEnemyIdx < state.enemies.size()
                && state.enemies.get(selectedEnemyIdx).isAlive()) {
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

        int rightPanelW = 176;
        int ctrlX = this.width - rightPanelW - 6;
        int ctrlY = this.height - 86;

        this.basicBtn = addRenderableWidget(new SciFiButton(ctrlX + 4, ctrlY + 32, 82, 20,
                new TextComponent("[Q]" + basicTitle), btn -> {
            if (pendingUltSlot >= 0) {
                cancelUltAiming();
                pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
                syncSelectionContext();
                return;
            }
            if (pendingActionMode == TurnBasedCombatManager.ACTION_BASIC && !BattleSceneClientController.isAnimating()) {
                executePendingAction();
            } else {
                BattleSceneClientController.skipAllAnimations();
                pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
                syncSelectionContext();
            }
        }, 0xFF4ADE80));

        this.skillBtn = addRenderableWidget(new SciFiButton(ctrlX + 90, ctrlY + 32, 82, 20,
                new TextComponent("[E]" + skillTitle), btn -> {
            if (state.skillPoints <= 0) return;
            if (pendingUltSlot >= 0) {
                cancelUltAiming();
                pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
                syncSelectionContext();
                return;
            }
            if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL && !BattleSceneClientController.isAnimating()) {
                executePendingAction();
            } else {
                BattleSceneClientController.skipAllAnimations();
                pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
                syncSelectionContext();
            }
        }, 0xFF38BDF8));
        this.skillBtn.active = state.skillPoints > 0;

        this.confirmBtn = addRenderableWidget(new SciFiButton(ctrlX + 4, ctrlY + 56, 118, 20,
                new TextComponent(pendingUltSlot >= 0 ? "[Space] 确认大招" : "[Space] 确认出手"),
                btn -> executePendingAction(), 0xFFFBBF24));

        this.fleeBtn = addRenderableWidget(new SciFiButton(ctrlX + 126, ctrlY + 56, 46, 20,
                new TextComponent("撤退"), btn -> sendAction(TurnBasedCombatManager.ACTION_FLEE, -1), 0xFFEF4444));
        this.fleeBtn.active = state.canFlee;
    }

    private void updateConfirmButtonLabel() {
        if (confirmBtn != null) {
            confirmBtn.setMessage(new TextComponent(pendingUltSlot >= 0 ? "[Space] 确认大招" : "[Space] 确认出手"));
        }
    }

    private void handleUltSlotInput(int slot) {
        if (state == null || state.finished || slot < 0 || slot >= state.allies.size()) return;
        BattleState.AllyUnit u = state.allies.get(slot);
        if (!u.isAlive() || u.energy < u.maxEnergy) return;

        if (BattleSceneClientController.isAnimating()) {
            BattleSceneClientController.skipAllAnimations();
        }

        if (pendingUltSlot == slot) {
            pendingUltSlot = -1;
            syncSelectionContext();
            updateConfirmButtonLabel();
            sendAction(TurnBasedCombatManager.ACTION_ULT, slot);
        } else {
            pendingUltSlot = slot;
            syncSelectionContext();
            updateConfirmButtonLabel();
        }
    }

    private void cancelUltAiming() {
        pendingUltSlot = -1;
        syncSelectionContext();
        updateConfirmButtonLabel();
    }

    private void executePendingAction() {
        if (state == null || state.finished) return;
        if (BattleSceneClientController.isAnimating()) {
            BattleSceneClientController.skipAllAnimations();
            return;
        }
        if (pendingUltSlot >= 0) {
            int slot = pendingUltSlot;
            pendingUltSlot = -1;
            syncSelectionContext();
            updateConfirmButtonLabel();
            sendAction(TurnBasedCombatManager.ACTION_ULT, slot);
            return;
        }
        if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL && state.skillPoints <= 0) {
            pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
            syncSelectionContext();
        }
        sendAction(pendingActionMode, -1);
    }

    private void sendAction(int type, int allyUltIdx) {
        if (BattleSceneClientController.isAnimating()) {
            BattleSceneClientController.skipAllAnimations();
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
                syncSelectionContext();
                return;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (introTicks > 0) introTicks--;
        pulseTicks++;
        if (pendingUltSlot >= 0 && state != null) {
            if (state.finished || pendingUltSlot >= state.allies.size()
                    || !state.allies.get(pendingUltSlot).isAlive()
                    || state.allies.get(pendingUltSlot).energy < state.allies.get(pendingUltSlot).maxEnergy) {
                cancelUltAiming();
            }
        }
        syncSelectionContext();
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

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (pendingUltSlot >= 0) {
                cancelUltAiming();
                return true;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_SLASH) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new ChatScreen("/battle_anim "));
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_Q) {
            if (pendingUltSlot >= 0) {
                cancelUltAiming();
                pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
                syncSelectionContext();
            } else if (pendingActionMode == TurnBasedCombatManager.ACTION_BASIC && !BattleSceneClientController.isAnimating()) {
                executePendingAction();
            } else {
                BattleSceneClientController.skipAllAnimations();
                pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
                syncSelectionContext();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_E) {
            if (pendingUltSlot >= 0) {
                cancelUltAiming();
                if (state.skillPoints > 0) {
                    pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
                    syncSelectionContext();
                }
            } else if (state.skillPoints > 0) {
                if (pendingActionMode == TurnBasedCombatManager.ACTION_SKILL && !BattleSceneClientController.isAnimating()) {
                    executePendingAction();
                } else {
                    BattleSceneClientController.skipAllAnimations();
                    pendingActionMode = TurnBasedCombatManager.ACTION_SKILL;
                    syncSelectionContext();
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
            handleUltSlotInput(keyCode - GLFW.GLFW_KEY_1);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (state != null && !state.finished) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && pendingUltSlot >= 0) {
                cancelUltAiming();
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                // 1. Widgets (bottom-right buttons) take highest click priority
                if (super.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }

                // 2. Ally Cards for two-step Ultimate aiming & casting
                int[] cardMetrics = computeAllyCardMetrics();
                int cardW = cardMetrics[0];
                int gap = cardMetrics[1];
                int cardY = cardMetrics[2];
                for (int i = 0; i < state.allies.size(); i++) {
                    int ax = 8 + i * (cardW + gap);
                    if (mouseX >= ax && mouseX <= ax + cardW && mouseY >= cardY && mouseY <= cardY + 68) {
                        handleUltSlotInput(i);
                        return true;
                    }
                }

                // 3. Minimal enemy overhead bar hitboxes
                for (int[] box : overheadHitboxes) {
                    if (mouseX >= box[1] && mouseX <= box[1] + box[3] && mouseY >= box[2] && mouseY <= box[2] + box[4]) {
                        selectedEnemyIdx = box[0];
                        syncSelectionContext();
                        return true;
                    }
                }

                // 4. Direct 3D raycast onto live BattleActorEntity hitboxes
                int rayHit = BattleCameraController.raycastEnemyEntity(state, mouseX, mouseY, this.width, this.height);
                if (rayHit >= 0) {
                    selectedEnemyIdx = rayHit;
                    syncSelectionContext();
                    return true;
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

        // Live 3D Hangar Deck renders behind this completely transparent HUD
        renderEnemyOverheadBars(ps, partialTicks);
        renderFloatingPopups(ps, partialTicks);
        renderUltCutInBanner(ps, partialTicks);

        renderTopHeader(ps);
        renderActionTimeline(ps);
        int tutBottom = renderTutorialOverlay(ps);
        renderLockedTargetStrip(ps, tutBottom);
        renderActionBanner(ps);
        renderAllyCards(ps);
        renderSkillPointPanel(ps);

        if (state.finished) {
            renderResultModal(ps);
        }

        super.render(ps, mouseX, mouseY, partialTicks);

        if (introTicks > 0) {
            int alpha = (int) ((introTicks / 10.0f) * 140);
            GuiComponent.fill(ps, 0, 0, this.width, this.height, (alpha << 24) | 0x00FFFFFF);
        }
    }

    /**
     * Strictly graphic-only enemy overhead bars anchored to 3D enemy positions:
     * gold selector chevron (selected enemy) + pixel-art weakness icons + white toughness bar
     * with flashing expected drop + solid red HP bar. Zero text labels above enemy heads.
     */
    private void renderEnemyOverheadBars(PoseStack ps, float partialTicks) {
        overheadHitboxes.clear();
        if (BattleSceneClientController.isAnimating() || state.finished) return;

        BattleCombatPreview.PreviewResult preview = BattleSceneClientController.getLastPreview();
        BattleState.AllyUnit previewActor = (pendingUltSlot >= 0 && pendingUltSlot < state.allies.size())
                ? state.allies.get(pendingUltSlot)
                : getActiveActor();
        String activeElem = previewActor != null ? previewActor.element : "";

        float pulse01 = 0.5f + 0.5f * Mth.sin((pulseTicks + partialTicks) * 0.45f);

        for (int i = 0; i < state.enemies.size(); i++) {
            BattleState.EnemyUnit e = state.enemies.get(i);
            if (!e.isAlive()) continue;

            String arch = BattleArenaLayout.resolveEnemyArchetype(e);
            Vec3 worldPos = BattleArenaLayout.enemyHomePos(i, state.enemies.size())
                    .add(0.0D, BattleArenaLayout.getOverheadBarHeight(arch) + 0.35D, 0.0D);
            double[] proj = BattleCameraController.projectWorldToScreen(worldPos, this.width, this.height);
            if (proj == null) continue;

            int cx = (int) Math.round(proj[0]);
            int cy = (int) Math.round(proj[1]);
            if (cx < 68 || cx > this.width - 10 || cy < 20 || cy > this.height - 92) continue;

            overheadHitboxes.add(new int[]{i, cx - 32, cy - 22, 64, 30});

            int barW = 54;
            int barX = cx - barW / 2;
            int iconsY = cy - 15;

            // Selected enemy graphic chevron
            if (i == selectedEnemyIdx) {
                int bounce = (pulseTicks % 14 < 7) ? -1 : 0;
                int triY = iconsY - 6 + bounce;
                GuiComponent.fill(ps, cx - 3, triY, cx + 4, triY + 1, 0xFFFFD700);
                GuiComponent.fill(ps, cx - 2, triY + 1, cx + 3, triY + 2, 0xFFFFD700);
                GuiComponent.fill(ps, cx - 1, triY + 2, cx + 2, triY + 3, 0xFFFFD700);
                GuiComponent.fill(ps, cx, triY + 3, cx + 1, triY + 4, 0xFFFFD700);
            }

            // Pixel-art weakness icons row (no text)
            int nW = e.weaknesses.size();
            if (nW > 0) {
                int totalW = nW * 10 + (nW - 1) * 2;
                int ix = cx - totalW / 2;
                for (String w : e.weaknesses) {
                    if (w.equals(activeElem)) {
                        int hiCol = pulse01 > 0.5f ? 0xFFFDE047 : 0xCCFFFFFF;
                        GuiComponent.fill(ps, ix - 1, iconsY - 1, ix + 11, iconsY + 11, hiCol);
                    }
                    ElementIconRenderer.draw(ps, w, ix, iconsY, 10);
                    ix += 12;
                }
            }

            // Toughness bar (54x4) with HSR-style flashing expected drop preview
            int tY = cy - 3;
            float maxT = Math.max(1.0F, e.maxToughness);
            float curPct = Mth.clamp(e.toughness / maxT, 0.0F, 1.0F);
            float drop = preview != null ? preview.expectedToughnessDrop.getOrDefault(i, 0.0F) : 0.0F;
            boolean willBreak = preview != null && Boolean.TRUE.equals(preview.willBreak.get(i));
            float remAfterDrop = Mth.clamp((e.toughness - drop) / maxT, 0.0F, curPct);

            int curW = Math.round(barW * curPct);
            int remW = Math.round(barW * remAfterDrop);

            int tBorder = willBreak ? (pulse01 > 0.5f ? 0xFFFF9F1C : 0xFFFFFFFF) : 0xFF1E293B;
            GuiComponent.fill(ps, barX - 1, tY - 1, barX + barW + 1, tY + 5, tBorder);
            GuiComponent.fill(ps, barX, tY, barX + barW, tY + 4, 0xD00F172A);
            if (remW > 0) {
                GuiComponent.fill(ps, barX, tY, barX + remW, tY + 4, e.broken ? 0xFF64748B : 0xFFF8FAFC);
            }
            if (drop > 0.01F && curW > remW) {
                int flashCol;
                if (willBreak) {
                    flashCol = pulse01 > 0.5f ? 0xFFFFFFFF : 0xFFFF9F1C;
                } else {
                    flashCol = pulse01 > 0.5f ? 0xFFFBBF24 : 0x88FDE68A;
                }
                GuiComponent.fill(ps, barX + remW, tY, barX + curW, tY + 4, flashCol);
            }

            // Solid red HP bar (54x5) — intentionally NO expected HP drop preview
            int hY = tY + 5;
            float hPct = Mth.clamp(e.hp / Math.max(1.0F, e.maxHp), 0.0F, 1.0F);
            int hFillW = Math.round(barW * hPct);
            GuiComponent.fill(ps, barX - 1, hY - 1, barX + barW + 1, hY + 6, 0xFF0F172A);
            GuiComponent.fill(ps, barX, hY, barX + barW, hY + 5, 0xD00F172A);
            if (hFillW > 0) {
                GuiComponent.fill(ps, barX, hY, barX + hFillW, hY + 5, e.chargingUlt ? 0xFFFF5252 : 0xFFEF4444);
            }
        }
    }

    private void renderFloatingPopups(PoseStack ps, float partialTicks) {
        List<BattleVfxManager.FloatingPopup> popups = BattleVfxManager.getPopups();
        if (popups.isEmpty()) return;

        for (BattleVfxManager.FloatingPopup p : popups) {
            float progress = Mth.clamp((p.age + partialTicks) / Math.max(1, p.maxAge), 0.0F, 1.0F);
            Vec3 pos = p.worldPos.add(0.0D, progress * 0.95D, 0.0D);
            double[] proj = BattleCameraController.projectWorldToScreen(pos, this.width, this.height);
            if (proj == null) continue;

            float alphaF = progress > 0.70F ? Mth.clamp(1.0F - (progress - 0.70F) / 0.30F, 0.05F, 1.0F) : 1.0F;
            int alpha = ((int) (alphaF * 255.0F)) & 0xFF;
            int col = (alpha << 24) | (p.color & 0x00FFFFFF);

            float scale = p.big ? 1.35F : 1.0F;
            ps.pushPose();
            ps.translate(proj[0], proj[1], 0.0D);
            ps.scale(scale, scale, 1.0F);
            int w = font.width(p.text);
            font.drawShadow(ps, new TextComponent(p.text), -w / 2.0F, -4.0F, col);
            ps.popPose();
        }
    }

    private void renderUltCutInBanner(PoseStack ps, float partialTicks) {
        BattleState.AnimEvent ev = BattleSceneClientController.getCurrentEvent();
        if (ev == null || !"ULT".equals(ev.animType)) return;
        float progress = BattleSceneClientController.getCurrentAnimProgress(partialTicks);
        if (progress >= 0.30F) return;

        float slideIn = Mth.clamp(progress / 0.07F, 0.0F, 1.0F);
        float fadeOut = progress > 0.23F ? Mth.clamp(1.0F - (progress - 0.23F) / 0.07F, 0.0F, 1.0F) : 1.0F;
        int elemCol = CharacterData.getElementColor(ev.element);

        int bw = Math.min(250, this.width - 100);
        int bh = 48;
        int targetX = 88;
        int bx = (int) Mth.lerp(slideIn, -bw - 20, targetX);
        int by = this.height / 2 - 52;

        int bgAlpha = Math.max(24, (int) (0xDC * fadeOut));
        int bgCol = (bgAlpha << 24) | 0x09131F;
        int borderAlpha = Math.max(40, (int) (0xFF * fadeOut));
        int borderCol = (borderAlpha << 24) | (elemCol & 0x00FFFFFF);

        GenshinHudOverlay.drawVoxelPanel(ps, bx, by, bw, bh, bgCol, borderCol, 0xFFFBBF24);
        GenshinHudOverlay.drawPortrait(ps, getPortraitForChar(ev.actorId), bx + 5, by + 4, 40, 40);
        ElementIconRenderer.draw(ps, ev.element, bx + 50, by + 7, 14);
        font.drawShadow(ps, new TextComponent(ev.actorName), bx + 68, by + 9, 0xFFFBBF24);
        String skill = (ev.skillTitle != null && !ev.skillTitle.isEmpty()) ? ev.skillTitle : "终结技";
        font.drawShadow(ps, new TextComponent(skill), bx + 50, by + 26, 0xFFF8FAFC);
    }

    private void renderTopHeader(PoseStack ps) {
        String themeName = ArenaTheme.fromId(state.arenaTheme).displayName;
        String title = themeName + " · 第 " + state.turnCount + " 回合"
                + ("boss_excavator".equals(state.encounterType) ? " | 阶段 " + state.bossPhase + "/2" : "");
        GenshinHudOverlay.drawVoxelPanel(ps, 6, 4, this.width - 12, 18, 0xC80A111C, 0xFF284664, 0xFF38BDF8);
        font.drawShadow(ps, new TextComponent(title), 12, 9, 0xFFE2E8F0);

        String hint;
        int hintCol;
        if (BattleSceneClientController.isAnimating()) {
            hint = "[Space] 跳过演出";
            hintCol = 0xFFFBBF24;
        } else if (pendingUltSlot >= 0) {
            hint = "大招瞄准中 · [Space/再按]确认  [右键/Esc]取消";
            hintCol = 0xFFFDE047;
        } else {
            hint = this.width >= 540
                    ? "[Q]普攻  [E]战技  [1-4]大招瞄准  [Tab/点击3D敌]选目标"
                    : "[Q]普 [E]技 [1-4]大 [点击3D敌]选";
            hintCol = 0xFF94A3B8;
        }
        font.draw(ps, new TextComponent(hint), this.width - font.width(hint) - 12, 9, hintCol);
    }

    private void renderActionBanner(PoseStack ps) {
        BattleState.AnimEvent ev = BattleSceneClientController.getCurrentEvent();
        String txt = (ev != null && ev.bannerText != null && !ev.bannerText.isEmpty())
                ? ev.bannerText
                : (BattleSceneClientController.isAnimating() || state.tutorialId > 0 || state.logs.isEmpty()
                        ? "" : state.logs.get(state.logs.size() - 1));
        if (txt.isEmpty()) return;

        int maxArea = this.width - 104;
        int bw = Math.min(maxArea, font.width(txt) + 18);
        int bx = 82 + Math.max(0, (this.width - 88 - bw) / 2);
        int by = ev != null ? 25 : 43;
        int border = ev != null ? CharacterData.getElementColor(ev.element) : 0xFF38BDF8;
        GenshinHudOverlay.drawVoxelPanel(ps, bx, by, bw, 15, 0xD009121E, border, 0xFFFBBF24);
        font.drawShadow(ps, new TextComponent(txt), bx + (bw - font.width(txt)) / 2.0f, by + 3, 0xFFF8FAFC);
    }

    private void renderLockedTargetStrip(PoseStack ps, int tutBottom) {
        if (BattleSceneClientController.isAnimating() || state.finished) return;
        if (selectedEnemyIdx < 0 || selectedEnemyIdx >= state.enemies.size()) return;
        BattleState.EnemyUnit e = state.enemies.get(selectedEnemyIdx);
        if (!e.isAlive()) return;

        BattleCombatPreview.PreviewResult preview = BattleSceneClientController.getLastPreview();
        boolean willBreak = preview != null && Boolean.TRUE.equals(preview.willBreak.get(selectedEnemyIdx));

        int y = tutBottom > 0 ? (tutBottom + 3) : 24;
        StringBuilder sb = new StringBuilder();
        sb.append(e.name)
                .append("  HP ").append((int) Math.ceil(e.hp)).append("/").append((int) e.maxHp)
                .append("  韧 ").append((int) Math.ceil(e.toughness)).append("/").append((int) e.maxToughness);
        if (e.broken) sb.append(" [已击破]");
        else if (willBreak) sb.append(" [可击破]");
        if (e.debuffDefTurns > 0) sb.append(" [破甲]");
        if (e.chargingUlt) sb.append(" [蓄力]");
        if (e.intentText != null && !e.intentText.isEmpty()) {
            sb.append(" · ").append(e.intentText);
        }

        String text = sb.toString();
        int iconsW = e.weaknesses.size() * 12;
        int availW = Math.max(120, this.width - 96);
        int w = Math.min(availW, iconsW + font.width(text) + 18);
        while (iconsW + font.width(text) + 18 > availW && text.length() > 10) {
            text = text.substring(0, text.length() - 1);
        }

        int x = 82 + Math.max(0, (this.width - 88 - w) / 2);
        int border = willBreak ? 0xFFFF9F1C : (e.chargingUlt ? 0xFFEF4444 : 0xFFFBBF24);
        GenshinHudOverlay.drawVoxelPanel(ps, x, y, w, 16, 0xCC0A1320, border, border);

        int ix = x + 6;
        for (String wk : e.weaknesses) {
            ElementIconRenderer.draw(ps, wk, ix, y + 3, 10);
            ix += 12;
        }
        int textCol = willBreak ? 0xFFFDE047 : 0xFFF8FAFC;
        font.drawShadow(ps, new TextComponent(text), ix + 3, y + 4, textCol);
    }

    private void renderActionTimeline(PoseStack ps) {
        int x = 6;
        int y = 26;
        int w = 72;
        int h = this.height - 106;
        GenshinHudOverlay.drawVoxelPanel(ps, x, y, w, h, 0xC009101A, 0xFF1E3A5F, 0xFF38BDF8);
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
            int bg = te.isCurrent ? 0xC8263B52 : 0x90111B27;
            GenshinHudOverlay.drawVoxelPanel(ps, x + 3, ey, w - 6, 15, bg, te.color, te.color);
            String shortName = te.label.length() > 4 ? te.label.substring(0, 4) : te.label;
            font.draw(ps, new TextComponent((te.isCurrent ? "▶" : "") + shortName), x + 5, ey + 4, te.color);
            String avStr = te.warnCount > 0 ? ("蓄" + te.warnCount) : String.valueOf(Math.round(te.av));
            font.draw(ps, new TextComponent(avStr), x + w - font.width(avStr) - 5, ey + 4, te.warnCount > 0 ? 0xFFFF5252 : 0xFF94A3B8);
            ey += 17;
        }
    }

    private int[] computeAllyCardMetrics() {
        int rightPanelW = 176;
        int ctrlPanelX = this.width - rightPanelW - 6;
        int availW = ctrlPanelX - 14;
        int n = Math.max(1, state.allies.size());
        int gap = 4;
        int cardW = Math.min(90, Math.max(54, (availW - (n - 1) * gap) / n));
        int cardY = this.height - 74;
        return new int[]{cardW, gap, cardY};
    }

    private void renderAllyCards(PoseStack ps) {
        int[] m = computeAllyCardMetrics();
        int cardW = m[0];
        int gap = m[1];
        int y = m[2];

        for (int i = 0; i < state.allies.size(); i++) {
            BattleState.AllyUnit a = state.allies.get(i);
            int ax = 8 + i * (cardW + gap);
            boolean isTurn = (i == state.activeAllyIndex);
            boolean ultReady = a.isAlive() && a.energy >= a.maxEnergy;
            boolean isAiming = (pendingUltSlot == i);
            boolean tutUltHighlight = (state.tutorialId > 0 && state.tutorialHighlightBtn == (10 + i));
            int elemCol = CharacterData.getElementColor(a.element);
            int border = isAiming ? 0xFFFFD700
                    : (tutUltHighlight ? ((pulseTicks % 10 < 5) ? 0xFFFBBF24 : 0xFFFFFFFF)
                    : (isTurn ? 0xFFFBBF24 : (ultReady ? 0xFF38BDF8 : 0xFF334155)));

            GenshinHudOverlay.drawVoxelPanel(ps, ax, y, cardW, 68,
                    isAiming ? 0xE224364B : (isTurn ? 0xCC1A2E44 : 0xB40E1622), border, border);

            GenshinHudOverlay.drawPortrait(ps, getPortraitForChar(a.charId), ax + 4, y + 4, 20, 20);
            String shortName = a.name.contains("·") ? a.name.substring(a.name.indexOf('·') + 1) : a.name;
            font.drawShadow(ps, new TextComponent(shortName), ax + 27, y + 4, elemCol);

            // Pixel-art element emblem instead of plain text character
            ElementIconRenderer.draw(ps, a.element, ax + 27, y + 14, 10);
            if (a.buffAtkTurns > 0) {
                font.draw(ps, new TextComponent("+攻"), ax + 39, y + 15, 0xFF4ADE80);
            }

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
            String uText = isAiming ? "▶ 瞄准中" : (ultReady ? ("[" + (i + 1) + "]大招") : ((int) a.energy + "%"));
            font.draw(ps, new TextComponent(uText), ax + 4, y + 56,
                    isAiming ? 0xFFFFD700 : (ultReady ? 0xFFFBBF24 : 0xFF94A3B8));

            if (tutUltHighlight) {
                int bounce = (pulseTicks % 12 < 6) ? -2 : 1;
                String arrow = isAiming ? "▼ 再按施放" : ("▼ 按[" + (i + 1) + "]");
                font.drawShadow(ps, new TextComponent(arrow), ax + (cardW - font.width(arrow)) / 2.0f, y - 11 + bounce, 0xFFFBBF24);
            }
        }
    }

    private void renderSkillPointPanel(PoseStack ps) {
        int rightPanelW = 176;
        int ctrlX = this.width - rightPanelW - 6;
        int ctrlY = this.height - 86;
        int borderCol = pendingUltSlot >= 0 ? 0xFFFFD700 : 0xFF284664;
        GenshinHudOverlay.drawVoxelPanel(ps, ctrlX, ctrlY, rightPanelW, 80, 0xD00A121D, borderCol, 0xFFFBBF24);

        font.draw(ps, new TextComponent("战技点:"), ctrlX + 6, ctrlY + 6, 0xFFE2E8F0);
        for (int i = 0; i < state.maxSkillPoints; i++) {
            int ox = ctrlX + 46 + i * 11;
            int oy = ctrlY + 6;
            boolean filled = i < state.skillPoints;
            GuiComponent.fill(ps, ox, oy, ox + 8, oy + 8, filled ? 0xFFFBBF24 : 0xFF1E293B);
        }

        if (pendingUltSlot >= 0 && pendingUltSlot < state.allies.size()) {
            BattleState.AllyUnit ultActor = state.allies.get(pendingUltSlot);
            font.draw(ps, new TextComponent("终结技瞄准"), ctrlX + rightPanelW - font.width("终结技瞄准") - 6, ctrlY + 6, 0xFFFFD700);
            String desc = CharacterData.getUltNameFor(ultActor.charId) + " · 右键/Esc取消";
            while (font.width(desc) > rightPanelW - 12 && desc.length() > 6) {
                desc = desc.substring(0, desc.length() - 1);
            }
            font.draw(ps, new TextComponent(desc), ctrlX + 6, ctrlY + 19, 0xFFFDE047);
        } else {
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
        }

        // Bouncing tutorial arrow above [Q] or [E] button
        if (state.tutorialId > 0 && state.tutorialStep > 0 && !BattleSceneClientController.isAnimating()) {
            int bounce = (pulseTicks % 12 < 6) ? -2 : 1;
            if (state.tutorialHighlightBtn == 0 && basicBtn != null) {
                String tag = "▼ 按[Q]";
                font.drawShadow(ps, new TextComponent(tag), ctrlX + 45 - font.width(tag) / 2.0f, ctrlY - 10 + bounce, 0xFF4ADE80);
            } else if (state.tutorialHighlightBtn == 1 && skillBtn != null) {
                String tag = "▼ 按[E]";
                font.drawShadow(ps, new TextComponent(tag), ctrlX + 131 - font.width(tag) / 2.0f, ctrlY - 10 + bounce, 0xFF38BDF8);
            }
        }
    }

    private int renderTutorialOverlay(PoseStack ps) {
        if (state.tutorialId <= 0 || state.tutorialStep <= 0 || state.finished) return 0;
        if (state.tutorialPrompt == null || state.tutorialPrompt.isEmpty()) return 0;

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

        GenshinHudOverlay.drawVoxelPanel(ps, bx, by, boxW, boxH, 0xDE0A1522, 0xFFFBBF24, 0xFF38BDF8);
        GenshinHudOverlay.drawPortrait(ps, getPortraitForChar(speakerId), bx + 5, by + 5, 24, 24);
        font.drawShadow(ps, new TextComponent(state.tutorialTitle), bx + 34, by + 4, 0xFFFBBF24);

        int ty = by + 15;
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            font.draw(ps, lines.get(i), bx + 34, ty, 0xFFF8FAFC);
            ty += 10;
        }
        return by + boxH;
    }

    private void renderResultModal(PoseStack ps) {
        int mw = Math.min(340, this.width - 32);
        int mh = 120;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2 - 12;

        int col = state.victory ? 0xFFFBBF24 : 0xFFEF4444;
        GenshinHudOverlay.drawVoxelPanel(ps, mx, my, mw, mh, 0xE80B131E, col, col);

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
