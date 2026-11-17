import json
import os
from PIL import Image, ImageDraw

BASE_RES = os.path.join("mod", "src", "main", "resources")
ASSETS = os.path.join(BASE_RES, "assets", "picking_hope_core")
DATA = os.path.join(BASE_RES, "data", "picking_hope_core")


def ensure_dir(path):
    os.makedirs(path, exist_ok=True)


def save_img(img, rel_path):
    full = os.path.join(ASSETS, rel_path)
    ensure_dir(os.path.dirname(full))
    img.save(full)
    print(f"Generated texture: {full}")


def save_json(obj, rel_path, base=ASSETS):
    full = os.path.join(base, rel_path)
    ensure_dir(os.path.dirname(full))
    with open(full, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
    print(f"Generated json: {full}")


def px(img, x, y, rgba):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), rgba)


def rect(img, x0, y0, x1, y1, rgba):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            px(img, x, y, rgba)


# ============================================================
# 1. ITEM TEXTURES (16x16 Voxel Pixel Art)
# ============================================================
def gen_weapons():
    # 1.1 Plasma Heavy Cutter (4-star)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Pommel & Handle (bottom-left)
    rect(img, 1, 13, 2, 14, (80, 90, 105, 255))
    px(img, 3, 12, (45, 52, 62, 255))
    px(img, 4, 11, (45, 52, 62, 255))
    # Heavy Industrial Guard
    rect(img, 3, 9, 6, 10, (235, 165, 30, 255))
    rect(img, 5, 10, 6, 12, (190, 120, 20, 255))
    # Glowing Core
    px(img, 5, 10, (0, 240, 255, 255))
    # Wide Plasma Blade extending to top-right
    for i in range(6, 15):
        j = 15 - i
        px(img, i, j, (190, 245, 255, 255))
        px(img, i - 1, j, (0, 205, 255, 255))
        px(img, i, j + 1, (38, 46, 58, 255))
        if i < 13:
            px(img, i - 1, j + 1, (65, 78, 95, 255))
    px(img, 14, 1, (240, 255, 255, 255))
    save_img(img, "textures/item/plasma_cutter.png")

    # 1.2 Cryo High-Frequency Sawblade (4-star)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 1, 13, 3, 14, (60, 70, 85, 255))
    px(img, 3, 12, (255, 140, 30, 255))
    rect(img, 4, 9, 6, 11, (200, 100, 20, 255))
    for i in range(6, 15):
        j = 15 - i
        px(img, i, j, (140, 235, 255, 255))
        px(img, i, j + 1, (55, 130, 200, 255))
        if i % 2 == 0:
            px(img, i - 1, j - 1, (215, 250, 255, 255))
        else:
            px(img, i - 1, j, (90, 195, 255, 255))
    px(img, 14, 1, (255, 255, 255, 255))
    save_img(img, "textures/item/cryo_sawblade.png")

    # 1.3 Precursor Superconductor Claymore (5-star)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 1, 13, 2, 14, (255, 200, 60, 255))
    px(img, 3, 12, (50, 35, 75, 255))
    px(img, 4, 11, (50, 35, 75, 255))
    # Winged Gold Crossguard
    rect(img, 3, 8, 6, 10, (255, 210, 70, 255))
    rect(img, 5, 10, 7, 12, (220, 160, 30, 255))
    px(img, 5, 10, (200, 110, 255, 255))
    # Energy Greatblade
    for i in range(6, 15):
        j = 15 - i
        px(img, i, j, (240, 220, 255, 255))
        px(img, i - 1, j, (175, 95, 255, 255))
        px(img, i, j + 1, (255, 195, 50, 255))
        if i < 13:
            px(img, i - 1, j + 1, (85, 235, 255, 255))
    px(img, 14, 1, (255, 255, 240, 255))
    save_img(img, "textures/item/precursor_claymore.png")


def gen_chips():
    # Slot 1: Control Core (Hexagonal Reactor Chip)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 2, 2, 13, 13, (28, 38, 52, 255))
    rect(img, 3, 3, 12, 12, (42, 56, 76, 255))
    for x in (4, 7, 8, 11):
        px(img, x, 1, (255, 210, 50, 255))
        px(img, x, 14, (255, 210, 50, 255))
    rect(img, 5, 5, 10, 10, (0, 195, 255, 255))
    rect(img, 6, 6, 9, 9, (190, 255, 255, 255))
    save_img(img, "textures/item/fusion_core_chip.png")

    # Slot 2: Power Generator (Amber/Orange Twin Thruster Module)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 2, 3, 13, 12, (45, 42, 48, 255))
    rect(img, 3, 4, 6, 11, (255, 135, 35, 255))
    rect(img, 9, 4, 12, 11, (255, 135, 35, 255))
    rect(img, 4, 6, 5, 9, (255, 235, 140, 255))
    rect(img, 10, 6, 11, 9, (255, 235, 140, 255))
    rect(img, 7, 5, 8, 10, (120, 130, 145, 255))
    px(img, 7, 2, (255, 200, 60, 255))
    px(img, 8, 2, (255, 200, 60, 255))
    save_img(img, "textures/item/void_drive_chip.png")

    # Slot 3: Energy Battery (High-Voltage Capsule)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 4, 2, 11, 13, (35, 48, 60, 255))
    rect(img, 6, 1, 9, 2, (210, 220, 230, 255))
    rect(img, 5, 4, 10, 12, (20, 30, 42, 255))
    rect(img, 6, 5, 9, 6, (60, 255, 175, 255))
    rect(img, 6, 8, 9, 9, (0, 225, 255, 255))
    rect(img, 6, 11, 9, 11, (0, 180, 255, 255))
    save_img(img, "textures/item/servo_battery_chip.png")

    # Slot 4: Overclock Chip (Gold & Magenta CPU Die)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 2, 2, 13, 13, (40, 24, 58, 255))
    for i in range(3, 13, 2):
        px(img, i, 1, (255, 215, 60, 255))
        px(img, i, 14, (255, 215, 60, 255))
        px(img, 1, i, (255, 215, 60, 255))
        px(img, 14, i, (255, 215, 60, 255))
    rect(img, 4, 4, 11, 11, (195, 55, 255, 255))
    rect(img, 6, 6, 9, 9, (255, 220, 110, 255))
    save_img(img, "textures/item/overclock_chip.png")

    # Slot 5: Neural Sensing Halo (5-Star Golden/Cyan Halo Ring)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Ring outline
    for x in range(4, 12):
        px(img, x, 2, (255, 215, 65, 255))
        px(img, x, 13, (255, 215, 65, 255))
    for y in range(4, 12):
        px(img, 2, y, (255, 215, 65, 255))
        px(img, 13, y, (255, 215, 65, 255))
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
        px(img, cx, cy, (255, 180, 30, 255))
    # Inner floating neural crystal
    rect(img, 6, 5, 9, 10, (120, 235, 255, 255))
    rect(img, 7, 6, 8, 9, (255, 255, 255, 255))
    px(img, 7, 2, (0, 240, 255, 255))
    px(img, 8, 2, (0, 240, 255, 255))
    save_img(img, "textures/item/neural_halo_chip.png")


def gen_materials():
    # Quantum Core (Star-Core Computation Prism)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for dy in range(-5, 6):
        w = 5 - abs(dy)
        for dx in range(-w, w + 1):
            c = (255, 225, 95, 255) if abs(dx) == w or abs(dy) == 5 else (0, 225, 255, 255)
            if abs(dx) <= 1 and abs(dy) <= 1:
                c = (230, 255, 255, 255)
            px(img, 7 + dx, 7 + dy, c)
    save_img(img, "textures/item/quantum_core.png")

    # Heavy-Industry Alloy Plate
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 2, 3, 13, 12, (72, 82, 96, 255))
    rect(img, 3, 4, 12, 11, (105, 118, 135, 255))
    # Hazard stripe
    for i in range(3, 12):
        px(img, i, 10, (240, 175, 30, 255) if (i % 2 == 0) else (30, 35, 42, 255))
    # Rivets
    for rx, ry in ((4, 5), (11, 5), (4, 8), (11, 8)):
        px(img, rx, ry, (200, 215, 230, 255))
    save_img(img, "textures/item/alloy_plate.png")

    # Precursor Fusion Spark
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for r, col in ((6, (255, 140, 20, 180)), (4, (255, 210, 70, 255)), (2, (255, 255, 230, 255))):
        for x in range(16):
            for y in range(16):
                if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= r ** 2:
                    px(img, x, y, col)
    # Star rays
    for i in range(1, 15):
        px(img, i, 7, (255, 240, 180, 255))
        px(img, 7, i, (255, 240, 180, 255))
    save_img(img, "textures/item/precursor_spark.png")


# ============================================================
# 2. GUI VOXEL PORTRAITS & SKILL ICONS
# ============================================================
def make_portrait(bg_col, hair_col, skin_col, visor_col, coat_col, accent_col):
    img = Image.new("RGBA", (32, 32), bg_col)
    # Subtle grid border
    for i in range(32):
        px(img, i, 0, accent_col)
        px(img, i, 31, accent_col)
        px(img, 0, i, accent_col)
        px(img, 31, i, accent_col)
    # Shoulders / Jacket
    rect(img, 6, 22, 25, 30, coat_col)
    rect(img, 12, 22, 19, 30, accent_col)
    # Head
    rect(img, 9, 7, 22, 21, skin_col)
    # Hair / Helmet
    rect(img, 8, 4, 23, 11, hair_col)
    rect(img, 8, 11, 10, 17, hair_col)
    rect(img, 21, 11, 23, 17, hair_col)
    # Visor / Eyes
    rect(img, 10, 12, 21, 15, (20, 26, 36, 255))
    rect(img, 11, 13, 14, 14, visor_col)
    rect(img, 17, 13, 20, 14, visor_col)
    # Mouth
    rect(img, 14, 18, 17, 19, (120, 85, 80, 255))
    return img


def gen_gui_textures():
    # Agent Zero Dawn
    save_img(
        make_portrait((18, 28, 44, 255), (210, 225, 240, 255), (235, 205, 185, 255), (0, 235, 255, 255), (38, 48, 68, 255), (0, 190, 255, 255)),
        "textures/gui/portrait_dawn.png",
    )
    # Locked Slot
    locked = Image.new("RGBA", (32, 32), (18, 22, 30, 255))
    rect(locked, 11, 13, 20, 23, (70, 80, 95, 255))
    rect(locked, 13, 8, 18, 13, (90, 100, 115, 255))
    rect(locked, 14, 10, 17, 13, (18, 22, 30, 255))
    px(locked, 15, 17, (180, 190, 210, 255))
    px(locked, 16, 17, (180, 190, 210, 255))
    px(locked, 15, 18, (180, 190, 210, 255))
    px(locked, 16, 18, (180, 190, 210, 255))
    save_img(locked, "textures/gui/portrait_locked.png")

    # Chief Engineer Zheng Tieshan
    save_img(
        make_portrait((35, 28, 18, 255), (90, 95, 105, 255), (220, 180, 150, 255), (255, 185, 45, 255), (195, 135, 30, 255), (255, 205, 70, 255)),
        "textures/gui/portrait_zheng.png",
    )
    # Bridge Defense Commander Han Zhenyue
    save_img(
        make_portrait((22, 28, 45, 255), (30, 38, 55, 255), (215, 185, 160, 255), (120, 190, 255, 255), (35, 50, 85, 255), (235, 190, 60, 255)),
        "textures/gui/portrait_han.png",
    )
    # Chief Scientist Su Wanxing
    save_img(
        make_portrait((18, 35, 42, 255), (45, 55, 75, 255), (242, 215, 205, 255), (125, 255, 235, 255), (220, 230, 240, 255), (70, 220, 235, 255)),
        "textures/gui/portrait_su.png",
    )

    # Skill E Icon (16x16 Plasma Dash Slash)
    se = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(2, 14):
        px(se, i, 15 - i, (220, 255, 255, 255))
        px(se, i, 14 - i, (0, 210, 255, 255))
        if i > 4:
            px(se, i - 2, 16 - i, (0, 140, 230, 255))
    rect(se, 2, 11, 5, 13, (255, 200, 60, 255))
    save_img(se, "textures/gui/skill_e_dawn.png")

    # Skill Q Icon (16x16 Spark Verdict Ultimate Burst)
    sq = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d <= 36:
                px(sq, x, y, (255, 155, 25, 255))
            if d <= 16:
                px(sq, x, y, (255, 225, 95, 255))
            if d <= 4:
                px(sq, x, y, (255, 255, 240, 255))
    for i in range(0, 16):
        px(sq, i, 7, (255, 245, 175, 255))
        px(sq, 7, i, (255, 245, 175, 255))
    save_img(sq, "textures/gui/skill_q_dawn.png")


# ============================================================
# 3. ENTITY TEXTURES (64x64)
# ============================================================
def make_humanoid_skin(hair, skin, visor, torso, legs, accent):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    # Head (0..32, 0..16)
    rect(img, 0, 0, 31, 15, hair)
    rect(img, 8, 8, 15, 15, skin)  # Front face
    rect(img, 8, 10, 15, 12, (25, 32, 42, 255))
    rect(img, 9, 11, 10, 11, visor)
    rect(img, 13, 11, 14, 11, visor)
    # Body (16..40, 16..32)
    rect(img, 16, 16, 39, 31, torso)
    rect(img, 20, 20, 27, 31, accent)
    # Right Leg (0..16, 16..32)
    rect(img, 0, 16, 15, 31, legs)
    # Right Arm (40..56, 16..32)
    rect(img, 40, 16, 55, 31, torso)
    rect(img, 44, 28, 47, 31, skin)
    # Left Leg (16..32, 48..64)
    rect(img, 16, 48, 31, 63, legs)
    # Left Arm (32..48, 48..64)
    rect(img, 32, 48, 47, 63, torso)
    rect(img, 36, 60, 39, 63, skin)
    return img


def gen_entities():
    # Security Drone (64x64)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x in range(0, 48):
        for y in range(0, 24):
            color = (48, 54, 65, 255)
            if x % 12 == 0 or x % 12 == 11 or y % 12 == 0 or y % 12 == 11:
                color = (25, 28, 35, 255)
            px(img, x, y, color)
    rect(img, 15, 15, 20, 20, (255, 50, 50, 255))
    rect(img, 17, 17, 18, 18, (255, 220, 220, 255))
    for y in range(3, 9, 2):
        for x in range(14, 22):
            px(img, x, y, (15, 18, 22, 255))
    save_img(img, "textures/entity/security_drone.png")

    # Precursor Heavy Mining Mech Boss (64x64 IronGolem layout!)
    mech = Image.new("RGBA", (64, 64), (42, 46, 56, 255))
    # Gold armor trims & cyan core accents across texture
    for x in range(0, 64, 8):
        for y in range(0, 64):
            px(mech, x, y, (24, 26, 32, 255))
    for y in range(0, 64, 8):
        for x in range(0, 64):
            px(mech, x, y, (24, 26, 32, 255))
    # Glowing core & visor strips
    rect(mech, 8, 8, 15, 15, (215, 165, 45, 255))
    rect(mech, 9, 10, 14, 12, (0, 235, 255, 255))
    rect(mech, 20, 22, 32, 30, (255, 175, 40, 255))
    rect(mech, 23, 24, 29, 28, (0, 245, 255, 255))
    save_img(mech, "textures/entity/precursor_mech.png")

    # NPCs (Humanoid 64x64 skins)
    save_img(
        make_humanoid_skin((75, 80, 90, 255), (220, 180, 150, 255), (255, 190, 40, 255), (185, 125, 28, 255), (45, 50, 60, 255), (255, 205, 65, 255)),
        "textures/entity/npc_zheng.png",
    )
    save_img(
        make_humanoid_skin((30, 38, 55, 255), (215, 185, 160, 255), (100, 190, 255, 255), (32, 48, 84, 255), (25, 32, 48, 255), (235, 190, 60, 255)),
        "textures/entity/npc_han.png",
    )
    save_img(
        make_humanoid_skin((40, 48, 65, 255), (242, 215, 205, 255), (85, 245, 235, 255), (215, 225, 235, 255), (50, 65, 85, 255), (0, 210, 240, 255)),
        "textures/entity/npc_su.png",
    )


# ============================================================
# 4. MODELS, BLOCKSTATES, WEAPON ATTRIBUTES & LANG
# ============================================================
def gen_models_and_lang():
    # Handheld weapon item models
    for w in ("plasma_cutter", "cryo_sawblade", "precursor_claymore"):
        save_json(
            {"parent": "minecraft:item/handheld", "textures": {"layer0": f"picking_hope_core:item/{w}"}},
            f"models/item/{w}.json",
        )
        save_json({"parent": "bettercombat:claymore"}, f"weapon_attributes/{w}.json", base=DATA)

    # Generated 2D item models (chips + materials)
    for it in (
        "fusion_core_chip",
        "void_drive_chip",
        "servo_battery_chip",
        "overclock_chip",
        "neural_halo_chip",
        "quantum_core",
        "alloy_plate",
        "precursor_spark",
    ):
        save_json(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"picking_hope_core:item/{it}"}},
            f"models/item/{it}.json",
        )

    # New Blocks: orbital_drop_pod & precursor_altar
    save_json(
        {"variants": {"": {"model": "picking_hope_core:block/orbital_drop_pod"}}},
        "blockstates/orbital_drop_pod.json",
    )
    save_json(
        {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "top": "minecraft:block/lodestone_top",
                "bottom": "minecraft:block/iron_block",
                "side": "minecraft:block/obsidian",
            },
        },
        "models/block/orbital_drop_pod.json",
    )
    save_json({"parent": "picking_hope_core:block/orbital_drop_pod"}, "models/item/orbital_drop_pod.json")

    save_json(
        {"variants": {"": {"model": "picking_hope_core:block/precursor_altar"}}},
        "blockstates/precursor_altar.json",
    )
    save_json(
        {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "top": "minecraft:block/beacon",
                "bottom": "minecraft:block/polished_blackstone",
                "side": "minecraft:block/crying_obsidian",
            },
        },
        "models/block/precursor_altar.json",
    )
    save_json({"parent": "picking_hope_core:block/precursor_altar"}, "models/item/precursor_altar.json")

    zh_cn = {
        "itemGroup.picking_hope_core": "重拾希望 (Picking Up Hope)",
        "block.picking_hope_core.teleport_waypoint": "星舰传送锚点",
        "item.picking_hope_core.teleport_waypoint": "星舰传送锚点",
        "block.picking_hope_core.engineering_terminal": "工程维护主控终端",
        "item.picking_hope_core.engineering_terminal": "工程维护主控终端",
        "block.picking_hope_core.auxiliary_energy_core": "辅助聚变能源核心",
        "item.picking_hope_core.auxiliary_energy_core": "辅助聚变能源核心",
        "block.picking_hope_core.orbital_drop_pod": "近地轨道空投舱",
        "item.picking_hope_core.orbital_drop_pod": "近地轨道空投舱",
        "block.picking_hope_core.precursor_altar": "先驱高能祭坛",
        "item.picking_hope_core.precursor_altar": "先驱高能祭坛",
        "item.picking_hope_core.quantum_core": "星辉算力芯片 (终端复现凭证)",
        "item.picking_hope_core.alloy_plate": "重工强化合金 (装备强化素材)",
        "item.picking_hope_core.precursor_spark": "先驱聚变火种",
        "item.picking_hope_core.plasma_cutter": "【专武·4★】等离子重切剑",
        "item.picking_hope_core.cryo_sawblade": "【专武·4★】极寒高频裂解刃",
        "item.picking_hope_core.precursor_claymore": "【专武·5★】先驱超导重剑",
        "item.picking_hope_core.fusion_core_chip": "【1号位·控制核心】聚变脉冲核心芯片",
        "item.picking_hope_core.void_drive_chip": "【2号位·动力发生器】脉冲动力发生器",
        "item.picking_hope_core.servo_battery_chip": "【3号位·能源电池】高能聚变电池",
        "item.picking_hope_core.overclock_chip": "【4号位·超频芯片】电浆超频芯片",
        "item.picking_hope_core.neural_halo_chip": "【5号位·神经感知环】神谕神经感知环",
        "entity.picking_hope_core.security_drone": "失控安保巡检机",
        "entity.picking_hope_core.precursor_mech": "先驱重型挖掘机甲·泰坦",
        "entity.picking_hope_core.faction_npc": "星舰阵营代表",
        "key.categories.picking_hope": "《重拾希望》按键绑定",
        "key.picking_hope.char_1": "小队成员 1 (零号特工·破晓)",
        "key.picking_hope.char_2": "小队成员 2 (预留槽位)",
        "key.picking_hope.char_3": "小队成员 3 (预留槽位)",
        "key.picking_hope.char_4": "小队成员 4 (预留槽位)",
        "key.picking_hope.skill_e": "科技战技 (E)",
        "key.picking_hope.burst_q": "终极爆发 (Q)",
        "key.picking_hope.bag_b": "星舰体素背包 (B)",
        "key.picking_hope.interact_f": "交互快捷键 (F)",
        "key.picking_hope.profile_c": "主角养成终端 (C)",
        "key.picking_hope.wish_g": "终端数据复现/祈愿 (G)",
    }
    en_us = {
        "itemGroup.picking_hope_core": "Picking Up Hope",
        "block.picking_hope_core.teleport_waypoint": "Starship Teleport Waypoint",
        "item.picking_hope_core.teleport_waypoint": "Starship Teleport Waypoint",
        "block.picking_hope_core.engineering_terminal": "Engineering Master Terminal",
        "item.picking_hope_core.engineering_terminal": "Engineering Master Terminal",
        "block.picking_hope_core.auxiliary_energy_core": "Auxiliary Fusion Energy Core",
        "item.picking_hope_core.auxiliary_energy_core": "Auxiliary Fusion Energy Core",
        "block.picking_hope_core.orbital_drop_pod": "Orbital Drop Pod",
        "item.picking_hope_core.orbital_drop_pod": "Orbital Drop Pod",
        "block.picking_hope_core.precursor_altar": "Precursor Energy Altar",
        "item.picking_hope_core.precursor_altar": "Precursor Energy Altar",
        "item.picking_hope_core.quantum_core": "Star-Core Token",
        "item.picking_hope_core.alloy_plate": "Reinforced Alloy Plate",
        "item.picking_hope_core.precursor_spark": "Precursor Fusion Spark",
        "item.picking_hope_core.plasma_cutter": "Plasma Heavy Cutter",
        "item.picking_hope_core.cryo_sawblade": "Cryo High-Freq Sawblade",
        "item.picking_hope_core.precursor_claymore": "Precursor Superconductor Claymore",
        "item.picking_hope_core.fusion_core_chip": "[Slot 1] Fusion Control Core",
        "item.picking_hope_core.void_drive_chip": "[Slot 2] Pulse Power Generator",
        "item.picking_hope_core.servo_battery_chip": "[Slot 3] High-Energy Fusion Cell",
        "item.picking_hope_core.overclock_chip": "[Slot 4] Plasma Overclock Chip",
        "item.picking_hope_core.neural_halo_chip": "[Slot 5] Neural Sensing Halo",
        "entity.picking_hope_core.security_drone": "Corrupted Security Drone",
        "entity.picking_hope_core.precursor_mech": "Precursor Mining Mech Boss",
        "entity.picking_hope_core.faction_npc": "Starship Faction Envoy",
        "key.categories.picking_hope": "Picking Up Hope Keybinds",
        "key.picking_hope.char_1": "Party Slot 1 (Dawn)",
        "key.picking_hope.char_2": "Party Slot 2 (Reserved)",
        "key.picking_hope.char_3": "Party Slot 3 (Reserved)",
        "key.picking_hope.char_4": "Party Slot 4 (Reserved)",
        "key.picking_hope.skill_e": "Tactical Skill (E)",
        "key.picking_hope.burst_q": "Ultimate Burst (Q)",
        "key.picking_hope.bag_b": "Voxel Inventory (B)",
        "key.picking_hope.interact_f": "Interact (F)",
        "key.picking_hope.profile_c": "Agent Profile Terminal (C)",
        "key.picking_hope.wish_g": "Terminal Reproduction Wish (G)",
    }
    save_json(zh_cn, "lang/zh_cn.json")
    save_json(en_us, "lang/en_us.json")


if __name__ == "__main__":
    gen_weapons()
    gen_chips()
    gen_materials()
    gen_gui_textures()
    gen_entities()
    gen_models_and_lang()
    print("All voxel assets, models, and localizations generated successfully!")
