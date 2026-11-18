package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketNpcAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class NpcDialogueScreen extends Screen {
    private static final ResourceLocation TEX_ZHENG = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_zheng.png");
    private static final ResourceLocation TEX_CHEN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_chen.png");
    private static final ResourceLocation TEX_SU = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_su.png");
    private static final ResourceLocation TEX_HAN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_han.png");

    private final String npcId;
    private final int questStage;
    private final List<String> pages = new ArrayList<>();
    private int pageIndex = 0;
    private int revealChars = 0;

    public NpcDialogueScreen(String npcId, int questStage) {
        super(new TextComponent("船员对话"));
        this.npcId = npcId;
        this.questStage = questStage;
        buildScript();
    }

    private void buildScript() {
        if (questStage <= 4 && "chen".equals(npcId)) {
            pages.add("林野？谢天谢地总算见到活人了！刚才飞船猛地震了一下，整条B区直接断电，安保机器人全发了疯。");
            pages.add("我是巡检组陈风，擅长【气属性·对群】攻击。咱俩配合：你用 [Q] 普攻攒战技点，我用 [E] 战技一次性扫平对面多台机器！");
            pages.add("我这有两瓶经验瓶和信用币先给你拿着。邀请我入队后，咱们一起把中继大厅里的巡逻机清理干净，再到东北侧舰桥夺回权限！");
        } else if (questStage <= 10 && "su".equals(npcId)) {
            pages.add("林野、陈风，你们终于赶到下层动力房了！上面刚传来结构报警，情况很不乐观。");
            pages.add("我是医务室的苏晴，负责【水属性·治疗】。全队血线危急时，用我的战技可以大量回血并清理异常状态；能量满按 [1~4] 随时插队大招救场！");
            pages.add("先收下我准备的经验瓶，拿上医务室物资箱的 3★ 生命芯片升级角色，再去清理机房重装守卫、重启备用发电机！");
        } else if (questStage == 17 && "zheng".equals(npcId)) {
            pages.add("你们几个命真大，从逃生舱摔下来居然全员平安！我是早先留守这座地面科考站的老郑。");
            pages.add("前面冻土矿坑里的机器比飞船上的硬得多。记住：打怪不会直接涨装备经验，必须先把多余芯片在 [B] 背包溶解成【强化粉尘】。");
            pages.add("拿好安全屋物资箱里的芯片去强化到 +3 解锁副词条！然后再用屋里的【模拟终端】体验一把刷本热身，准备好了咱们就去夺回东配电室！");
        } else if (questStage == 19 && "zheng".equals(npcId)) {
            pages.add("芯片强化好了吧？安全屋里这台【模拟训练终端】连着母舰训练库，按 [E] 或直接点右边按钮就能进本刷经验瓶、信用币或突破材料。");
            pages.add("先去任意挑战一把【模拟副本】实战演练一下，顺便体会体力与材料三合一合成！打完出来我们就去东配电房给矿坑梯通电！");
        } else if (questStage == 24 && "zheng".equals(npcId)) {
            pages.add("好小子！真把地下矿坑那台双阶段【大型工程挖掘机】给拆了！深空通讯塔信号满格，主力舰队已经回传了安全接应坐标！");
            pages.add("这是科考站库存的 20 张【抽卡通行券】，按 [G] 就能通过端末呼叫并招募其他车厢的精锐同伴（如狙击手韩冰、调度员周敏和防护员老郑）。");
            pages.add("接下来可随时按 [E] 挑战 6 大模拟副本刷 1~6 级材料、突破世界等级上限，把小队练成最强阵容吧！");
        } else {
            switch (npcId) {
                case "chen" -> pages.add("陈风随时准备配合！按 [B] 打开背包，同系列 1~5 级低阶材料可以 3 合 1 升阶成高阶材料。");
                case "su" -> pages.add("苏晴随时照看大家状态。别忘了打怪不会自动涨角色经验，需要按 [C] 用【经验瓶】手动提升等级哦。");
                default -> pages.add("老郑盯着模拟终端和通讯站呢。需要材料或挑战世界等级突破试炼时，随时按 [E] 打开副本终端！");
            }
        }
    }

    private String getSpeakerTitle() {
        return switch (npcId) {
            case "chen" -> "【巡检员】陈风 (气·对群)";
            case "su" -> "【医疗员】苏晴 (水·治疗)";
            case "han" -> "【狙击手】韩冰 (火·对单)";
            default -> "【技术员】老郑";
        };
    }

    private ResourceLocation getPortrait() {
        return switch (npcId) {
            case "chen" -> TEX_CHEN;
            case "su" -> TEX_SU;
            case "han" -> TEX_HAN;
            default -> TEX_ZHENG;
        };
    }

    private int boxW() { return Math.min(this.width - 24, 540); }
    private int boxH() { return 100; }
    private int boxX() { return (this.width - boxW()) / 2; }
    private int boxY() { return this.height - boxH() - 14; }
    private int rightActionW() { return Math.min(154, (int) (boxW() * 0.34f)); }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        boolean onLastPage = pageIndex >= pages.size() - 1;
        int rw = rightActionW();
        int btnX = boxX() + boxW() - rw - 8;
        int by = boxY();

        if (!onLastPage) {
            addRenderableWidget(new SciFiButton(btnX, by + 68, rw, 20, new TextComponent("下一句 ▶ (Space)"),
                    btn -> advancePage(), 0xFF38BDF8));
            return;
        }

        if (questStage <= 4 && "chen".equals(npcId)) {
            addRenderableWidget(new SciFiButton(btnX, by + 44, rw, 20,
                    new TextComponent("◆ 邀请陈风入队"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "join_chen"));
                onClose();
            }, 0xFF4ADE80));
        } else if (questStage <= 10 && "su".equals(npcId)) {
            addRenderableWidget(new SciFiButton(btnX, by + 44, rw, 20,
                    new TextComponent("◆ 邀请苏晴入队"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "join_su"));
                onClose();
            }, 0xFF38BDF8));
        } else if (questStage == 17 && "zheng".equals(npcId)) {
            addRenderableWidget(new SciFiButton(btnX, by + 44, rw, 20,
                    new TextComponent("◆ 开始整备装备"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "zheng_outpost_guide"));
                onClose();
            }, 0xFFFBBF24));
        } else if (questStage == 24 && "zheng".equals(npcId)) {
            addRenderableWidget(new SciFiButton(btnX, by + 44, rw, 20,
                    new TextComponent("◆ 领 20 张抽卡券"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "finish_tutorial"));
                onClose();
            }, 0xFFFBBF24));
        } else if ("zheng".equals(npcId) && questStage >= 15) {
            addRenderableWidget(new SciFiButton(btnX, by + 44, rw, 20,
                    new TextComponent("◆ 打开模拟副本(E)"), btn ->
                    Minecraft.getInstance().setScreen(new SimulationDomainScreen()), 0xFF38BDF8));
        }

        addRenderableWidget(new SciFiButton(btnX, by + 70, rw, 20, new TextComponent("结束交谈 (ESC)"),
                btn -> onClose(), 0xFF94A3B8));
    }

    private void advancePage() {
        String full = pages.get(pageIndex);
        if (revealChars < full.length()) {
            revealChars = full.length();
            return;
        }
        if (pageIndex < pages.size() - 1) {
            pageIndex++;
            revealChars = 0;
            rebuildButtons();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (pageIndex < pages.size()) {
            String full = pages.get(pageIndex);
            if (revealChars < full.length()) {
                revealChars = Math.min(full.length(), revealChars + 3);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_F) {
            advancePage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        int bw = boxW();
        int bh = boxH();
        int bx = boxX();
        int by = boxY();
        int rw = rightActionW();

        GenshinHudOverlay.drawVoxelPanel(poseStack, bx, by, bw, bh, 0xEE09111A, 0xFF4A90E2, 0xFFFFD54F);
        GenshinHudOverlay.drawVoxelPanel(poseStack, bx + 10, by + 12, 54, 54, 0xD00B1522, 0xFF4FC3F7, 0xFFFFCC00);
        GenshinHudOverlay.drawPortrait(poseStack, getPortrait(), bx + 13, by + 15, 48, 48);

        font.drawShadow(poseStack, new TextComponent(getSpeakerTitle()), bx + 72, by + 12, 0xFFFFD54F);

        int textMaxW = Math.max(80, bw - 72 - rw - 20);
        if (pageIndex < pages.size()) {
            String full = pages.get(pageIndex);
            String visible = full.substring(0, Math.min(full.length(), revealChars));
            int ty = by + 28;
            for (FormattedCharSequence line : font.split(new TextComponent(visible), textMaxW)) {
                if (ty + 10 > by + bh - 6) break;
                font.draw(poseStack, line, bx + 72, ty, 0xFFF2F6FA);
                ty += 11;
            }
        }

        String pageHint = String.format("(%d / %d)", pageIndex + 1, pages.size());
        font.draw(poseStack, new TextComponent(pageHint), bx + 16, by + 74, 0xFF88AABB);

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
