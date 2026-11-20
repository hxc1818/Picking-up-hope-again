package com.resilienthope.core.world.prefab;

import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.network.PacketBuilderPrefabSync;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative stamping engine for built-in reusable prefabs & custom copy/paste clipboards,
 * featuring multi-segment tiling, connective shape updates, and a 20-step per-player undo stack.
 */
public final class PrefabPlacementManager {
    private static final int MAX_UNDO_STEPS = 20;
    private static final int MAX_COPY_VOLUME = 48 * 48 * 48; // 110,592 blocks
    public static final String CLIPBOARD_ID = "custom_clipboard";

    private static final class UndoRecord {
        final ResourceKey<Level> dimension;
        final String label;
        final Map<BlockPos, BlockState> previousStates;

        UndoRecord(ResourceKey<Level> dimension, String label, Map<BlockPos, BlockState> previousStates) {
            this.dimension = dimension;
            this.label = label;
            this.previousStates = previousStates;
        }
    }

    private static final class SelectionState {
        ResourceKey<Level> dim;
        BlockPos pos1;
        BlockPos pos2;
    }

    public static final class ClipboardVolume {
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;
        public final int repeatStep;
        public final Map<BlockPos, BlockState> blocks;

        ClipboardVolume(int sizeX, int sizeY, int sizeZ, Map<BlockPos, BlockState> blocks) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.repeatStep = Math.max(1, sizeZ);
            this.blocks = blocks;
        }
    }

    private static final Map<UUID, Deque<UndoRecord>> UNDO_STACKS = new HashMap<>();
    private static final Map<UUID, SelectionState> SELECTIONS = new HashMap<>();
    private static final Map<UUID, ClipboardVolume> CLIPBOARDS = new HashMap<>();

    private PrefabPlacementManager() {}

    public static ClipboardVolume getClipboard(ServerPlayer player) {
        return player != null ? CLIPBOARDS.get(player.getUUID()) : null;
    }

    public static void syncClipboardToClient(ServerPlayer player) {
        if (player == null) return;
        ClipboardVolume clip = CLIPBOARDS.get(player.getUUID());
        if (clip != null) {
            PacketHandler.sendToPlayer(player, new PacketBuilderPrefabSync(true, clip.sizeX, clip.sizeY, clip.sizeZ));
        } else {
            PacketHandler.sendToPlayer(player, new PacketBuilderPrefabSync(false, 0, 0, 0));
        }
    }

    public static boolean placePrefab(ServerPlayer player, String prefabId, BlockPos anchor,
                                      int rotSteps, int repeatCount, int yOffset, boolean airOnly) {
        if (player == null || (!player.isCreative() && !player.hasPermissions(2))) {
            return false;
        }
        String label;
        int sizeX;
        int repeatStep;
        Map<BlockPos, BlockState> templateBlocks;

        if (CLIPBOARD_ID.equalsIgnoreCase(prefabId)) {
            ClipboardVolume clip = CLIPBOARDS.get(player.getUUID());
            if (clip == null || clip.blocks.isEmpty()) {
                PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("自定义剪贴板", "当前尚未复制任何区域！先用 /hope pos1、pos2 与 copy"));
                return false;
            }
            label = "自定义复制结构";
            sizeX = clip.sizeX;
            repeatStep = clip.repeatStep;
            templateBlocks = clip.blocks;
        } else {
            PrefabRegistry.PrefabDef def = PrefabRegistry.findById(prefabId);
            if (def == null) {
                PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("预制结构", "未找到结构模板：" + prefabId));
                return false;
            }
            label = def.name;
            sizeX = def.sizeX;
            repeatStep = def.repeatStep;
            templateBlocks = def.buildBlocks();
        }

        ServerLevel level = player.getLevel();
        int segs = Mth.clamp(repeatCount, 1, 10);
        int dyOff = Mth.clamp(yOffset, -8, 8);
        int rot = Math.floorMod(rotSteps, 4);
        Rotation mcRot = PrefabTransform.toRotation(rot);

        Map<BlockPos, BlockState> previousStates = new LinkedHashMap<>();
        Set<BlockPos> connectiveUpdates = new LinkedHashSet<>();
        int placedCount = 0;

        for (int seg = 0; seg < segs; seg++) {
            for (Map.Entry<BlockPos, BlockState> entry : templateBlocks.entrySet()) {
                BlockPos local = entry.getKey();
                BlockState state = entry.getValue();
                if (airOnly && state.isAir()) continue;

                BlockPos worldPos = PrefabTransform.localToWorld(
                        anchor, sizeX, local.getX(), local.getY(), local.getZ(), rot, seg, repeatStep, dyOff);
                if (!level.isInWorldBounds(worldPos)) continue;

                BlockState existing = level.getBlockState(worldPos);
                if (airOnly && !existing.isAir()) continue;

                previousStates.putIfAbsent(worldPos, existing);
                BlockState rotatedState = (mcRot == Rotation.NONE) ? state : state.rotate(mcRot);
                level.setBlock(worldPos, rotatedState, 2);
                placedCount++;

                if (isConnectiveBlock(rotatedState)) {
                    connectiveUpdates.add(worldPos);
                }
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos nPos = worldPos.relative(d);
                    if (level.isInWorldBounds(nPos) && isConnectiveBlock(level.getBlockState(nPos))) {
                        connectiveUpdates.add(nPos);
                    }
                }
            }
        }

        // Pass 2: recompute neighbor shapes for walls, railings, glass panes, and stairs cleanly
        for (BlockPos cPos : connectiveUpdates) {
            if (!level.isInWorldBounds(cPos)) continue;
            BlockState cur = level.getBlockState(cPos);
            if (!isConnectiveBlock(cur)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos nPos = cPos.relative(d);
                cur = cur.updateShape(d, level.getBlockState(nPos), level, cPos, nPos);
            }
            if (cur.getBlock() instanceof WallBlock) {
                BlockPos upPos = cPos.above();
                cur = cur.updateShape(Direction.UP, level.getBlockState(upPos), level, cPos, upPos);
            }
            level.setBlock(cPos, cur, 2);
        }

        if (!previousStates.isEmpty()) {
            Deque<UndoRecord> stack = UNDO_STACKS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
            stack.push(new UndoRecord(level.dimension(), label, previousStates));
            while (stack.size() > MAX_UNDO_STEPS) {
                stack.removeLast();
            }
        }

        String segNote = segs > 1 ? (" ×" + segs + "段") : "";
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("结构已放置",
                label + segNote + " (共 " + placedCount + " 格，Shift+左键或 /hope undo 可撤回)"));
        return true;
    }

    private static boolean isConnectiveBlock(BlockState state) {
        return state.getBlock() instanceof WallBlock
                || state.getBlock() instanceof IronBarsBlock
                || state.getBlock() instanceof StairBlock;
    }

    public static boolean undoLast(ServerPlayer player) {
        if (player == null) return false;
        Deque<UndoRecord> stack = UNDO_STACKS.get(player.getUUID());
        if (stack == null || stack.isEmpty()) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("撤回失败", "当前没有可撤回的结构操作记录。"));
            return false;
        }
        UndoRecord rec = stack.pop();
        MinecraftServer server = player.getServer();
        ServerLevel targetLevel = server != null ? server.getLevel(rec.dimension) : player.getLevel();
        if (targetLevel == null) return false;

        List<BlockPos> keys = new ArrayList<>(rec.previousStates.keySet());
        for (int i = keys.size() - 1; i >= 0; i--) {
            BlockPos pos = keys.get(i);
            if (targetLevel.isInWorldBounds(pos)) {
                targetLevel.setBlock(pos, rec.previousStates.get(pos), 2);
            }
        }

        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("已撤回结构",
                rec.label + " (已还原 " + keys.size() + " 格，剩余可撤回 " + stack.size() + " 步)"));
        return true;
    }

    public static void setPos1(ServerPlayer player, BlockPos pos) {
        SelectionState sel = SELECTIONS.computeIfAbsent(player.getUUID(), k -> new SelectionState());
        sel.dim = player.getLevel().dimension();
        sel.pos1 = pos.immutable();
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("选区起点 Pos1",
                "已设置起点：(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"));
    }

    public static void setPos2(ServerPlayer player, BlockPos pos) {
        SelectionState sel = SELECTIONS.computeIfAbsent(player.getUUID(), k -> new SelectionState());
        sel.dim = player.getLevel().dimension();
        sel.pos2 = pos.immutable();
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("选区终点 Pos2",
                "已设置终点：(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"));
    }

    public static boolean copySelection(ServerPlayer player) {
        SelectionState sel = SELECTIONS.get(player.getUUID());
        if (sel == null || sel.pos1 == null || sel.pos2 == null || !player.getLevel().dimension().equals(sel.dim)) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("复制选区",
                    "请先在同一维度使用 /hope pos1 与 /hope pos2 标定对角线区域！"));
            return false;
        }
        int minX = Math.min(sel.pos1.getX(), sel.pos2.getX());
        int maxX = Math.max(sel.pos1.getX(), sel.pos2.getX());
        int minY = Math.min(sel.pos1.getY(), sel.pos2.getY());
        int maxY = Math.max(sel.pos1.getY(), sel.pos2.getY());
        int minZ = Math.min(sel.pos1.getZ(), sel.pos2.getZ());
        int maxZ = Math.max(sel.pos1.getZ(), sel.pos2.getZ());

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        long total = (long) sizeX * sizeY * sizeZ;
        if (total > MAX_COPY_VOLUME) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("复制失败",
                    "选区体积过大(" + total + "格)，单次上限为 48x48x48 (110,592格)。"));
            return false;
        }

        ServerLevel level = player.getLevel();
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        int nonAirCount = 0;
        for (int lx = 0; lx < sizeX; lx++) {
            for (int ly = 0; ly < sizeY; ly++) {
                for (int lz = 0; lz < sizeZ; lz++) {
                    BlockPos wp = new BlockPos(minX + lx, minY + ly, minZ + lz);
                    BlockState bs = level.getBlockState(wp);
                    blocks.put(new BlockPos(lx, ly, lz), bs);
                    if (!bs.isAir()) nonAirCount++;
                }
            }
        }

        CLIPBOARDS.put(player.getUUID(), new ClipboardVolume(sizeX, sizeY, sizeZ, blocks));
        syncClipboardToClient(player);
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("复制成功",
                "已保存 " + sizeX + "×" + sizeY + "×" + sizeZ + "（含 " + nonAirCount + " 实体方块），可用投影仪或 /hope paste 粘贴！"));
        return true;
    }

    public static void giveProjector(ServerPlayer player) {
        if (player == null) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.PREFAB_PROJECTOR.get())) {
                if (i < 9) {
                    player.getInventory().selected = i;
                }
                PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("蓝图投影仪",
                        "已切换手中投影仪！右键放置，Shift+右键旋转，左键/[K]打开结构库"));
                return;
            }
        }

        ItemStack wand = new ItemStack(ModItems.PREFAB_PROJECTOR.get());
        int cur = player.getInventory().selected;
        if (player.getInventory().getItem(cur).isEmpty()) {
            player.getInventory().setItem(cur, wand);
        } else {
            int emptyHotbar = -1;
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getItem(i).isEmpty()) {
                    emptyHotbar = i;
                    break;
                }
            }
            if (emptyHotbar >= 0) {
                player.getInventory().setItem(emptyHotbar, wand);
                player.getInventory().selected = emptyHotbar;
            } else {
                ItemStack prev = player.getInventory().getItem(cur).copy();
                player.getInventory().setItem(cur, wand);
                player.getInventory().add(prev);
            }
        }
        player.containerMenu.broadcastChanges();
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("蓝图投影仪",
                "已发放蓝图结构投影仪！按 [K] 选建筑，右键原地/准星放置，Shift+右键旋转"));
    }
}
