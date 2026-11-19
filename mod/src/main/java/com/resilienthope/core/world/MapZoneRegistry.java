package com.resilienthope.core.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared irregular multi-rectangle region planner for both worlds:
 * - Starship (`starship` dim): 5 irregular zones spanning ~1560 x 820 blocks
 * - First Planet (`planet_cryo` dim): 8 irregular zones spanning ~11,700 x 9,700 blocks (>10k x 10k)
 * Completely common-safe (used by both server chunk-guiding and client interactive map GUI).
 */
public final class MapZoneRegistry {

    public static final class ZoneDef {
        public final String id;
        public final String code;
        public final String name;
        public final String dimensionId; // "starship" or "planet"
        public final List<int[]> rects;  // each {minX, maxX, minZ, maxZ}
        public final int centerX;
        public final int centerY;
        public final int centerZ;
        public final int guideHeight;
        public final String verticalNote;
        public final int mapColor;
        public final String borderBlockId;
        public final String summary;

        public final int minX, maxX, minZ, maxZ;

        public ZoneDef(String id, String code, String name, String dimensionId, List<int[]> rects,
                       int centerX, int centerY, int centerZ, int guideHeight, String verticalNote,
                       int mapColor, String borderBlockId, String summary) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.dimensionId = dimensionId;
            this.rects = Collections.unmodifiableList(rects);
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.guideHeight = guideHeight;
            this.verticalNote = verticalNote;
            this.mapColor = mapColor;
            this.borderBlockId = borderBlockId;
            this.summary = summary;

            int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE;
            int z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
            for (int[] r : rects) {
                x0 = Math.min(x0, r[0]);
                x1 = Math.max(x1, r[1]);
                z0 = Math.min(z0, r[2]);
                z1 = Math.max(z1, r[3]);
            }
            this.minX = x0;
            this.maxX = x1;
            this.minZ = z0;
            this.maxZ = z1;
        }

        public boolean contains(int x, int z) {
            if (x < minX || x > maxX || z < minZ || z > maxZ) return false;
            for (int[] r : rects) {
                if (x >= r[0] && x <= r[1] && z >= r[2] && z <= r[3]) {
                    return true;
                }
            }
            return false;
        }

        public boolean isOnOuterBorder(int x, int z) {
            if (!contains(x, z)) return false;
            return !contains(x + 1, z) || !contains(x - 1, z) || !contains(x, z + 1) || !contains(x, z - 1);
        }

        public boolean isLandmarkPillarSpot(int x, int z) {
            if (!isOnOuterBorder(x, z)) return false;
            for (int[] r : rects) {
                if ((x == r[0] || x == r[1]) && (z == r[2] || z == r[3])) {
                    return true;
                }
            }
            return Math.floorMod(x + z, 128) == 0;
        }

        public String sizeLabel() {
            return (maxX - minX) + "x" + (maxZ - minZ);
        }
    }

    public record RouteLink(String fromId, String toId, String label) {}

    public static final List<ZoneDef> STARSHIP_ZONES = new ArrayList<>();
    public static final List<ZoneDef> PLANET_ZONES = new ArrayList<>();
    public static final List<ZoneDef> ALL_ZONES = new ArrayList<>();
    public static final List<RouteLink> ROUTE_LINKS = new ArrayList<>();

    static {
        // ============================================================
        // MAP 1: STARSHIP ("重拾希望号") — 1,560 x 820 irregular vessel footprint
        // ============================================================
        STARSHIP_ZONES.add(new ZoneDef(
                "s1", "S1", "休眠与生活甲板", "starship",
                List.of(new int[]{-200, 160, -140, 140}, new int[]{-320, -200, -90, 90}),
                0, 60, 0, 16, "基准Y=60 建议舱高12~16",
                0xFF22D3EE, "minecraft:cyan_concrete",
                "船尾起居区：休眠舱大厅、食堂与储藏走廊（苏醒起点）"
        ));
        STARSHIP_ZONES.add(new ZoneDef(
                "s2", "S2", "破损观测长廊与中庭", "starship",
                List.of(new int[]{160, 640, -180, 180}, new int[]{260, 540, -280, -180}),
                400, 60, 0, 28, "基准Y=60 建议挑高20~28",
                0xFF60A5FA, "minecraft:light_blue_concrete",
                "船腹中段：北侧巨型陨石撞击破口、全息星图中庭与外景废墟"
        ));
        STARSHIP_ZONES.add(new ZoneDef(
                "s3", "S3", "舰桥战术与通讯区", "starship",
                List.of(new int[]{640, 980, -140, 140}, new int[]{980, 1240, -75, 75}),
                860, 60, 0, 22, "基准Y=60 舰首逐级抬升",
                0xFFFBBF24, "minecraft:yellow_concrete",
                "锐角舰首：主控舰桥、大尺寸全景玻璃窗、星图通讯室"
        ));
        STARSHIP_ZONES.add(new ZoneDef(
                "s4", "S4", "双层立体动力核心机库", "starship",
                List.of(new int[]{180, 620, 180, 440}, new int[]{300, 520, 440, 540}),
                400, 60, 310, 24, "上层Y=60 / 下层Y=40 贯通机库",
                0xFFFB923C, "minecraft:orange_concrete",
                "南翼重工机库：反应堆核心柱、天车龙门架、医务隔间"
        ));
        STARSHIP_ZONES.add(new ZoneDef(
                "s5", "S5", "轨道逃生发射井", "starship",
                List.of(new int[]{-180, 180, 180, 380}),
                0, 40, 280, 26, "下层Y=40 垂直发射井深26格",
                0xFFF87171, "minecraft:red_concrete",
                "船腹底层：撤离走廊、多联装轨道逃生舱发射台"
        ));

        ROUTE_LINKS.add(new RouteLink("s1", "s2", "主干走廊"));
        ROUTE_LINKS.add(new RouteLink("s2", "s3", "舰桥通道"));
        ROUTE_LINKS.add(new RouteLink("s2", "s4", "机库升降道"));
        ROUTE_LINKS.add(new RouteLink("s4", "s5", "应急撤离井"));

        // ============================================================
        // MAP 2: FIRST PLANET ("第一颗星球") — 11,700 x 9,700 irregular continent (> 10k x 10k)
        // ============================================================
        PLANET_ZONES.add(new ZoneDef(
                "p1", "P1", "逃生舱坠机峡谷", "planet",
                List.of(new int[]{-5100, -2200, -1300, 1100}, new int[]{-4400, -1700, 1100, 2100}),
                -3600, 63, 0, 14, "地表Y=64 峡谷两侧堆雪山脊",
                0xFF38BDF8, "minecraft:light_blue_concrete",
                "西境雪原大裂谷：迫降犁痕、断裂舱段残骸与急救营地"
        ));
        PLANET_ZONES.add(new ZoneDef(
                "p2", "P2", "极地科考前哨站", "planet",
                List.of(new int[]{-1500, 1500, -1400, 1400}),
                0, 63, 0, 14, "地表Y=64 中央辐射主据点",
                0xFF4ADE80, "minecraft:lime_concrete",
                "大陆中心：保温基地主楼、气象雷达塔、仓库与锚点广场"
        ));
        PLANET_ZONES.add(new ZoneDef(
                "p3", "P3", "冰川气象台与失事舰桥", "planet",
                List.of(new int[]{-2400, 1600, -4800, -1600}, new int[]{-4800, -2400, -4200, -1600}),
                -800, 63, -3200, 18, "西北至正北高耸冰川山弧",
                0xFFA5F3FC, "minecraft:cyan_concrete",
                "北境极寒冰原：陡峭冰峰山脉、高空科研站与早年失事舰首"
        ));
        PLANET_ZONES.add(new ZoneDef(
                "p4", "P4", "地热配电工业区", "planet",
                List.of(new int[]{1600, 3600, -1400, 1400}, new int[]{1800, 3200, -2200, -1400}),
                2600, 63, 0, 14, "地表Y=64 东境重工业输电谷",
                0xFFFBBF24, "minecraft:yellow_concrete",
                "中东工业谷：高压输电铁塔、变电厂房与融雪电缆管沟"
        ));
        PLANET_ZONES.add(new ZoneDef(
                "p5", "P5", "地下地热温泉裂谷", "planet",
                List.of(new int[]{-1800, 2000, 1600, 4600}, new int[]{2000, 3200, 2000, 4000}),
                200, 63, 3100, 16, "地表下可深挖至Y=-40造地下温泉洞",
                0xFFFB923C, "minecraft:orange_concrete",
                "南境温热大裂谷：地热蒸汽口、荧光苔藓洞穴、暖色温泉河谷"
        ));
        PLANET_ZONES.add(new ZoneDef(
                "p6", "P6", "深层冻土立体矿脉", "planet",
                List.of(new int[]{3700, 5200, -800, 2200}, new int[]{3300, 4900, 2200, 4200}),
                4400, 63, 1200, 18, "向下深挖至Y=-48搭建多层矿坑",
                0xFFA16207, "minecraft:brown_concrete",
                "东南巨型矿区：矿业龙门井架、螺旋采掘巷道与中Boss挖掘机大厅"
        ));
        PLANET_ZONES.add(new ZoneDef(
                "p7", "P7", "远古外星机械遗迹群", "planet",
                List.of(new int[]{1800, 4600, -4900, -2300}, new int[]{3700, 4600, -2300, -1000}),
                3200, 63, -3600, 24, "地上地下联动造巨大黑曜白金神殿",
                0xFFC084FC, "minecraft:purple_concrete",
                "东北高地遗迹：悬空几何碑林、硬光桥矩阵与光束谜题神殿"
        ));
        PLANET_ZONES.add(new ZoneDef(
                "p8", "P8", "外星休眠方舟停泊港", "planet",
                List.of(new int[]{4700, 6600, -5100, -1500}),
                5600, 63, -3300, 28, "地表至地下Y=-40深邃冰渊停泊巨港",
                0xFFF472B6, "minecraft:magenta_concrete",
                "远东北终章巨构：深埋冰川的远古外星方舟巨舰与裁决中枢厅"
        ));

        ROUTE_LINKS.add(new RouteLink("p1", "p2", "冰原撤离路"));
        ROUTE_LINKS.add(new RouteLink("p2", "p3", "北境登山道"));
        ROUTE_LINKS.add(new RouteLink("p2", "p4", "东配电干线"));
        ROUTE_LINKS.add(new RouteLink("p2", "p5", "南裂谷栈道"));
        ROUTE_LINKS.add(new RouteLink("p4", "p6", "矿区货运线"));
        ROUTE_LINKS.add(new RouteLink("p4", "p7", "东北考察线"));
        ROUTE_LINKS.add(new RouteLink("p6", "p7", "深层连通道"));
        ROUTE_LINKS.add(new RouteLink("p7", "p8", "方舟接驳桥"));

        ALL_ZONES.addAll(STARSHIP_ZONES);
        ALL_ZONES.addAll(PLANET_ZONES);
    }

    private MapZoneRegistry() {}

    public static List<ZoneDef> getZonesForDimId(String dimensionId) {
        if ("starship".equals(dimensionId)) return STARSHIP_ZONES;
        if ("planet".equals(dimensionId)) return PLANET_ZONES;
        return Collections.emptyList();
    }

    public static ZoneDef findById(String id) {
        if (id == null) return null;
        String lower = id.trim().toLowerCase();
        for (ZoneDef z : ALL_ZONES) {
            if (z.id.equals(lower)) return z;
        }
        return null;
    }

    public static ZoneDef findZoneAt(String dimensionId, int x, int z) {
        for (ZoneDef zDef : getZonesForDimId(dimensionId)) {
            if (zDef.contains(x, z)) return zDef;
        }
        return null;
    }

    public static int[] getBoundsForDimId(String dimensionId) {
        List<ZoneDef> list = getZonesForDimId(dimensionId);
        if (list.isEmpty()) return new int[]{-500, 500, -500, 500};
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (ZoneDef z : list) {
            minX = Math.min(minX, z.minX);
            maxX = Math.max(maxX, z.maxX);
            minZ = Math.min(minZ, z.minZ);
            maxZ = Math.max(maxZ, z.maxZ);
        }
        return new int[]{minX, maxX, minZ, maxZ};
    }

    /**
     * Checks if integer tile (x, z) lies on a dashed route marker line between linked zone centers.
     * Draws a crisp 1-block marker every 6 blocks along the connecting centerline.
     */
    public static boolean isOnRouteDashedLine(String dimensionId, int x, int z) {
        for (RouteLink link : ROUTE_LINKS) {
            ZoneDef a = findById(link.fromId());
            ZoneDef b = findById(link.toId());
            if (a == null || b == null || !dimensionId.equals(a.dimensionId)) continue;
            int minX = Math.min(a.centerX, b.centerX) - 2;
            int maxX = Math.max(a.centerX, b.centerX) + 2;
            int minZ = Math.min(a.centerZ, b.centerZ) - 2;
            int maxZ = Math.max(a.centerZ, b.centerZ) + 2;
            if (x < minX || x > maxX || z < minZ || z > maxZ) continue;

            double dx = b.centerX - a.centerX;
            double dz = b.centerZ - a.centerZ;
            double lenSq = dx * dx + dz * dz;
            if (lenSq < 1.0) continue;
            double t = ((x - a.centerX) * dx + (z - a.centerZ) * dz) / lenSq;
            if (t < 0.02 || t > 0.98) continue;
            double px = a.centerX + t * dx;
            double pz = a.centerZ + t * dz;
            double distSq = (x - px) * (x - px) + (z - pz) * (z - pz);
            if (distSq <= 0.65 && (Math.floorMod(Math.round((float) (t * Math.sqrt(lenSq))), 6) == 0)) {
                return true;
            }
        }
        return false;
    }
}
