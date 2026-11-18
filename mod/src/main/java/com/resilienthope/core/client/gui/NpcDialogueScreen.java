package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketNpcAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
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
        if (questStage == 2 && "chen".equals(npcId)) {
            pages.add("林野？谢天谢地总算见到活人了！刚才飞船猛地震了一下，整条B区直接断电，安保机器人全发了疯。");
            pages.add("我是巡检组陈风，擅长【气属性·对群】攻击。咱俩配合一下：你平时多用 [Q] 普攻攒战技点，我用 [E] 战技一次性扫掉对面多台机器！");
            pages.add("这有两瓶经验瓶和一点信用币你拿着。准备好了咱们就把大厅里的巡逻机清干净！");
        } else if (questStage == 5 && "su".equals(npcId)) {
            pages.add("林野、陈风，你们终于赶到下层动力房了！上面刚传来结构报警，情况很不乐观。");
            pages.add("我是医务室的苏晴，负责【水属性·治疗】。战斗里全队血线危急时，用我的战技可以帮队友回血和清理异常状态；能量满按 [1~4] 还能随时插队大招急救！");
            pages.add("先清理掉机房里的重装守卫，再去把旁边那台【备用发电机组】拉闸重启！");
        } else if (questStage == 10 && "zheng".equals(npcId)) {
            pages.add("你们几个命真大，从逃生舱摔下来居然全员平安！我是早先留守这处地面科考站的老郑。");
            pages.add("后面矿坑里的机器可比飞船上的硬得多。记住：打怪不会直接给装备经验，你得先去箱子里拿几块不用的 2 级芯片，按 [B] 在背包里溶解成【强化粉尘】。");
            pages.add("拿了粉尘后按 [C] 打开角色界面，把主力芯片强化到 +3，每升 3 级都会解锁或提升一条副词条！弄好了去东边配电室把电梯通上电！");
        } else if (questStage == 14 && "zheng".equals(npcId)) {
            pages.add("好小子！真把地下矿坑那台老古董大型工程挖掘机给拆了！通讯塔信号已经恢复满格，重拾希望号的主力舰队刚回传了安全坐标！");
            pages.add("这是科考站库存的 20 张【抽卡通行券】，按 [G] 就能通过终端呼叫并招募其他车厢的精锐同伴了（比如防护员老郑、狙击手韩冰和调度员周敏）。");
            pages.add("后续想提升战力，可以随时按 [E] 或访问身旁的终端进入【模拟训练副本】刷金币、经验瓶和 1~6 级材料，主等级达标还能突破世界等级上限！");
        } else {
            switch (npcId) {
                case "chen" -> pages.add("陈风待命中！记得按 [B] 检查材料，同系列 1~5 级低级材料可以 3 合 1 合成高一阶材料哦。");
                case "su" -> pages.add("苏晴随时照看大家健康。提醒一下：打怪不会直接涨角色经验，要按 [C] 用【经验瓶】给角色手动升级。");
                default -> pages.add("老郑在这里看着模拟终端。缺钱缺经验瓶时，按 [E] 打开副本面板去刷一把最省心！");
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

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        boolean onLastPage = pageIndex >= pages.size() - 1;
        int btnX = this.width - 250;
        int btnBaseY = this.height - 135;

        if (!onLastPage) {
            addRenderableWidget(new Button(btnX, btnBaseY + 60, 210, 22, new TextComponent("下一句 ▶ (Space)"), btn -> advancePage()));
            return;
        }

        if (questStage == 2 && "chen".equals(npcId)) {
            addRenderableWidget(new Button(btnX, btnBaseY + 25, 220, 22,
                    new TextComponent("◆ 邀请陈风入队，清理大厅"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "join_chen"));
                onClose();
            }));
        } else if (questStage == 5 && "su".equals(npcId)) {
            addRenderableWidget(new Button(btnX, btnBaseY + 25, 220, 22,
                    new TextComponent("◆ 邀请苏晴入队，准备重启电源"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "join_su"));
                onClose();
            }));
        } else if (questStage == 10 && "zheng".equals(npcId)) {
            addRenderableWidget(new Button(btnX, btnBaseY + 25, 220, 22,
                    new TextComponent("◆ 确认强化指导，准备前往配电房"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "zheng_outpost_guide"));
                onClose();
            }));
        } else if (questStage == 14 && "zheng".equals(npcId)) {
            addRenderableWidget(new Button(btnX, btnBaseY + 25, 220, 22,
                    new TextComponent("◆ 发送信号并领取 20 张抽卡券"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "finish_tutorial"));
                onClose();
            }));
        } else if ("zheng".equals(npcId) && questStage >= 10) {
            addRenderableWidget(new Button(btnX, btnBaseY + 25, 220, 22,
                    new TextComponent("◆ 打开模拟训练副本面板"), btn ->
                    Minecraft.getInstance().setScreen(new SimulationDomainScreen())));
        }

        addRenderableWidget(new Button(btnX, btnBaseY + 62, 220, 20, new TextComponent("结束交谈 (ESC)"), btn -> onClose()));
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
        int boxW = Math.min(this.width - 40, 560);
        int boxH = 112;
        int boxX = (this.width - boxW) / 2;
        int boxY = this.height - boxH - 16;

        GenshinHudOverlay.drawVoxelPanel(poseStack, boxX, boxY, boxW, boxH, 0xEC09111A, 0xFF4A90E2, 0xFFFFD54F);
        GenshinHudOverlay.drawVoxelPanel(poseStack, boxX + 12, boxY + 14, 56, 56, 0xD00B1522, 0xFF4FC3F7, 0xFFFFCC00);
        GenshinHudOverlay.drawTexture(poseStack, getPortrait(), boxX + 16, boxY + 18, 48, 48, 32, 32);

        font.drawShadow(poseStack, new TextComponent(getSpeakerTitle()), boxX + 80, boxY + 14, 0xFFFFD54F);

        if (pageIndex < pages.size()) {
            String full = pages.get(pageIndex);
            String visible = full.substring(0, Math.min(full.length(), revealChars));
            int ty = boxY + 32;
            for (FormattedCharSequence line : font.split(new TextComponent(visible), boxW - 104)) {
                font.draw(poseStack, line, boxX + 80, ty, 0xFFF2F6FA);
                ty += 12;
            }
        }

        String pageHint = String.format("(%d / %d)", pageIndex + 1, pages.size());
        font.draw(poseStack, new TextComponent(pageHint), boxX + 18, boxY + 80, 0xFF88AABB);

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
