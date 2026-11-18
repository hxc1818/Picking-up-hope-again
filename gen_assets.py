import json
import os
from PIL import Image

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
    # 1.1 Engineering Cutter (Earth / Default)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 1, 13, 2, 14, (80, 90, 105, 255))
    px(img, 3, 12, (45, 52, 62, 255))
    px(img, 4, 11, (45, 52, 62, 255))
    rect(img, 3, 9, 6, 10, (215, 160, 45, 255))
    rect(img, 5, 10, 6, 12, (165, 115, 30, 255))
    px(img, 5, 10, (235, 195, 70, 255))
    for i in range(6, 15):
        j = 15 - i
        px(img, i, j, (215, 225, 235, 255))
        px(img, i - 1, j, (195, 145, 55, 255))
        px(img, i, j + 1, (55, 65, 78, 255))
        if i < 13:
            px(img, i - 1, j + 1, (85, 95, 110, 255))
    px(img, 14, 1, (255, 245, 210, 255))
    save_img(img, "textures/item/plasma_cutter.png")

    # 1.2 Thermal Sawblade (Fire)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 1, 13, 3, 14, (60, 70, 85, 255))
    px(img, 3, 12, (255, 110, 30, 255))
    rect(img, 4, 9, 6, 11, (190, 80, 25, 255))
    for i in range(6, 15):
        j = 15 - i
        px(img, i, j, (255, 185, 75, 255))
        px(img, i, j + 1, (180, 60, 35, 255))
        if i % 2 == 0:
            px(img, i - 1, j - 1, (255, 235, 140, 255))
        else:
            px(img, i - 1, j, (240, 105, 35, 255))
    px(img, 14, 1, (255, 255, 220, 255))
    save_img(img, "textures/item/cryo_sawblade.png")

    # 1.3 Hydro Pulse Gun (Water)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 3, 9, 5, 13, (48, 62, 82, 255))
    rect(img, 3, 6, 12, 9, (65, 90, 120, 255))
    rect(img, 6, 5, 10, 6, (35, 185, 255, 255))
    rect(img, 12, 7, 14, 8, (145, 235, 255, 255))
    rect(img, 5, 7, 8, 8, (0, 215, 255, 255))
    px(img, 4, 10, (180, 210, 235, 255))
    save_img(img, "textures/item/hydro_pulse_gun.png")

    # 1.4 Light Aero Blade (Air)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 1, 13, 2, 14, (65, 95, 85, 255))
    px(img, 3, 12, (40, 175, 135, 255))
    rect(img, 4, 10, 5, 11, (195, 235, 220, 255))
    for i in range(5, 15):
        j = 15 - i
        px(img, i, j, (210, 255, 235, 255))
        px(img, i - 1, j, (65, 220, 170, 255))
        if i < 13:
            px(img, i, j + 1, (45, 125, 105, 255))
    px(img, 14, 1, (255, 255, 255, 255))
    save_img(img, "textures/item/aero_blade.png")

    # 1.5 Heavy Alloy Greatblade (5-star)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 1, 13, 2, 14, (240, 190, 55, 255))
    px(img, 3, 12, (50, 55, 70, 255))
    px(img, 4, 11, (50, 55, 70, 255))
    rect(img, 3, 8, 6, 10, (245, 200, 65, 255))
    rect(img, 5, 10, 7, 12, (200, 145, 35, 255))
    px(img, 5, 10, (90, 225, 255, 255))
    for i in range(6, 15):
        j = 15 - i
        px(img, i, j, (245, 248, 255, 255))
        px(img, i - 1, j, (255, 195, 55, 255))
        px(img, i, j + 1, (75, 90, 115, 255))
        if i < 13:
            px(img, i - 1, j + 1, (90, 215, 255, 255))
    px(img, 14, 1, (255, 255, 240, 255))
    save_img(img, "textures/item/precursor_claymore.png")


def gen_chips():
    # Slot 1: Control Core
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 2, 2, 13, 13, (28, 38, 52, 255))
    rect(img, 3, 3, 12, 12, (42, 56, 76, 255))
    for x in (4, 7, 8, 11):
        px(img, x, 1, (220, 185, 55, 255))
        px(img, x, 14, (220, 185, 55, 255))
    rect(img, 5, 5, 10, 10, (65, 195, 245, 255))
    rect(img, 6, 6, 9, 9, (200, 245, 255, 255))
    save_img(img, "textures/item/fusion_core_chip.png")

    # Slot 2: Power Generator
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 2, 3, 13, 12, (45, 42, 48, 255))
    rect(img, 3, 4, 6, 11, (240, 135, 35, 255))
    rect(img, 9, 4, 12, 11, (240, 135, 35, 255))
    rect(img, 4, 6, 5, 9, (255, 235, 140, 255))
    rect(img, 10, 6, 11, 9, (255, 235, 140, 255))
    rect(img, 7, 5, 8, 10, (120, 130, 145, 255))
    save_img(img, "textures/item/void_drive_chip.png")

    # Slot 3: Energy Battery
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 4, 2, 11, 13, (35, 48, 60, 255))
    rect(img, 6, 1, 9, 2, (210, 220, 230, 255))
    rect(img, 5, 4, 10, 12, (20, 30, 42, 255))
    rect(img, 6, 5, 9, 6, (60, 240, 165, 255))
    rect(img, 6, 8, 9, 9, (50, 210, 245, 255))
    rect(img, 6, 11, 9, 11, (40, 165, 230, 255))
    save_img(img, "textures/item/servo_battery_chip.png")

    # Slot 4: Overclock Chip
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 2, 2, 13, 13, (40, 28, 58, 255))
    for i in range(3, 13, 2):
        px(img, i, 1, (245, 205, 65, 255))
        px(img, i, 14, (245, 205, 65, 255))
        px(img, 1, i, (245, 205, 65, 255))
        px(img, 14, i, (245, 205, 65, 255))
    rect(img, 4, 4, 11, 11, (175, 85, 245, 255))
    rect(img, 6, 6, 9, 9, (255, 220, 110, 255))
    save_img(img, "textures/item/overclock_chip.png")

    # Slot 5: Sensing Ring
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(4, 12):
        px(img, x, 2, (245, 205, 65, 255))
        px(img, x, 13, (245, 205, 65, 255))
    for y in range(4, 12):
        px(img, 2, y, (245, 205, 65, 255))
        px(img, 13, y, (245, 205, 65, 255))
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
        px(img, cx, cy, (235, 170, 35, 255))
    rect(img, 6, 5, 9, 10, (115, 225, 250, 255))
    rect(img, 7, 6, 8, 9, (255, 255, 255, 255))
    save_img(img, "textures/item/neural_halo_chip.png")


def gen_tiered_material_icon(shape, primary_col, accent_col, tier):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    border_cols = {
        1: (130, 145, 160, 255),
        2: (74, 222, 128, 255),
        3: (56, 189, 248, 255),
        4: (192, 132, 252, 255),
        5: (251, 191, 36, 255),
        6: (248, 113, 113, 255),
    }
    bcol = border_cols[tier]
    if shape == "char":
        # Industrial mechanical bolt / frame part
        rect(img, 4, 3, 11, 12, primary_col)
        rect(img, 3, 5, 12, 10, primary_col)
        rect(img, 6, 5, 9, 10, accent_col)
        rect(img, 7, 6, 8, 9, bcol)
    elif shape == "weapon":
        # Precision weapon barrel / gear block
        rect(img, 3, 4, 12, 11, primary_col)
        rect(img, 5, 2, 10, 13, primary_col)
        rect(img, 5, 6, 10, 9, accent_col)
        rect(img, 7, 7, 8, 8, bcol)
    else:
        # Tactical training manual / handbook
        rect(img, 3, 2, 12, 13, primary_col)
        rect(img, 4, 3, 5, 12, bcol)
        rect(img, 7, 4, 11, 6, accent_col)
        rect(img, 7, 8, 10, 9, (230, 235, 245, 255))

    # Tier pips along top-left
    for t in range(tier):
        px(img, 1 + t * 2, 14, bcol)
    return img


def gen_materials():
    # Legacy items
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for dy in range(-5, 6):
        w = 5 - abs(dy)
        for dx in range(-w, w + 1):
            c = (245, 215, 95, 255) if abs(dx) == w or abs(dy) == 5 else (65, 215, 245, 255)
            px(img, 7 + dx, 7 + dy, c)
    save_img(img, "textures/item/quantum_core.png")

    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 2, 3, 13, 12, (72, 82, 96, 255))
    rect(img, 3, 4, 12, 11, (105, 118, 135, 255))
    for i in range(3, 12):
        px(img, i, 10, (240, 175, 30, 255) if (i % 2 == 0) else (30, 35, 42, 255))
    save_img(img, "textures/item/alloy_plate.png")

    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for r, col in ((6, (255, 140, 20, 180)), (4, (255, 210, 70, 255)), (2, (255, 255, 230, 255))):
        for x in range(16):
            for y in range(16):
                if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= r ** 2:
                    px(img, x, y, col)
    save_img(img, "textures/item/precursor_spark.png")

    # Credit Coin
    coin = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d <= 36:
                px(coin, x, y, (215, 155, 35, 255))
            if d <= 22:
                px(coin, x, y, (250, 215, 85, 255))
    rect(coin, 7, 5, 8, 10, (175, 115, 20, 255))
    save_img(coin, "textures/item/credit_coin.png")

    # Character EXP Bottle
    exp = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(exp, 6, 2, 9, 4, (190, 205, 220, 255))
    rect(exp, 5, 5, 10, 13, (40, 55, 75, 255))
    rect(exp, 6, 7, 9, 12, (74, 222, 128, 255))
    rect(exp, 7, 8, 8, 10, (195, 255, 210, 255))
    save_img(exp, "textures/item/exp_bottle.png")

    # Equipment Essence (Dissolve Powder)
    dust = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(dust, 4, 8, 11, 13, (175, 105, 245, 255))
    rect(dust, 5, 6, 10, 11, (215, 155, 255, 255))
    rect(dust, 6, 4, 9, 8, (245, 215, 255, 255))
    px(dust, 4, 4, (255, 225, 110, 255))
    px(dust, 11, 5, (255, 225, 110, 255))
    save_img(dust, "textures/item/equip_essence.png")

    # Star Gacha Ticket
    ticket = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(ticket, 2, 4, 13, 11, (35, 95, 175, 255))
    rect(ticket, 3, 5, 12, 10, (56, 189, 248, 255))
    rect(ticket, 6, 6, 9, 9, (255, 215, 75, 255))
    px(ticket, 2, 7, (0, 0, 0, 0))
    px(ticket, 13, 7, (0, 0, 0, 0))
    save_img(ticket, "textures/item/star_ticket.png")

    # 1..6 Tier Materials (3 independent lines)
    for t in range(1, 7):
        save_img(gen_tiered_material_icon("char", (70, 82, 98, 255), (210, 155, 60, 255), t),
                 f"textures/item/char_mat_{t}.png")
        save_img(gen_tiered_material_icon("weapon", (62, 70, 86, 255), (75, 175, 235, 255), t),
                 f"textures/item/weapon_mat_{t}.png")
        save_img(gen_tiered_material_icon("talent", (48, 68, 92, 255), (235, 195, 80, 255), t),
                 f"textures/item/talent_mat_{t}.png")


# ============================================================
# 2. GUI PORTRAITS, ELEMENTS & SKILL ICONS
# ============================================================
def make_portrait(bg_col, hair_col, skin_col, visor_col, coat_col, accent_col):
    img = Image.new("RGBA", (32, 32), bg_col)
    for i in range(32):
        px(img, i, 0, accent_col)
        px(img, i, 31, accent_col)
        px(img, 0, i, accent_col)
        px(img, 31, i, accent_col)
    rect(img, 6, 22, 25, 30, coat_col)
    rect(img, 12, 22, 19, 30, accent_col)
    rect(img, 9, 7, 22, 21, skin_col)
    rect(img, 8, 4, 23, 11, hair_col)
    rect(img, 8, 11, 10, 17, hair_col)
    rect(img, 21, 11, 23, 17, hair_col)
    rect(img, 10, 12, 21, 15, (20, 26, 36, 255))
    rect(img, 11, 13, 14, 14, visor_col)
    rect(img, 17, 13, 20, 14, visor_col)
    rect(img, 14, 18, 17, 19, (120, 85, 80, 255))
    return img


def gen_gui_textures():
    # 1. Protagonist Lin Ye (Earth / Balanced)
    save_img(
        make_portrait((24, 30, 38, 255), (55, 60, 70, 255), (235, 205, 185, 255), (235, 185, 70, 255), (65, 78, 95, 255), (215, 165, 55, 255)),
        "textures/gui/portrait_dawn.png",
    )
    # 2. Chen Feng (Air / AoE)
    save_img(
        make_portrait((20, 36, 34, 255), (40, 85, 75, 255), (235, 205, 185, 255), (90, 240, 185, 255), (42, 62, 58, 255), (74, 222, 128, 255)),
        "textures/gui/portrait_chen.png",
    )
    # 3. Su Qing (Water / Healer)
    save_img(
        make_portrait((18, 34, 46, 255), (45, 65, 95, 255), (242, 215, 205, 255), (95, 225, 255, 255), (220, 232, 242, 255), (56, 189, 248, 255)),
        "textures/gui/portrait_su.png",
    )
    # 4. Lao Zheng (Fire / Shielder)
    save_img(
        make_portrait((38, 26, 18, 255), (95, 95, 105, 255), (220, 180, 150, 255), (255, 165, 55, 255), (185, 115, 35, 255), (251, 146, 60, 255)),
        "textures/gui/portrait_zheng.png",
    )
    # 5. Han Bing (Fire / Single DPS)
    save_img(
        make_portrait((35, 20, 25, 255), (35, 35, 48, 255), (230, 195, 180, 255), (248, 113, 113, 255), (50, 38, 45, 255), (239, 68, 68, 255)),
        "textures/gui/portrait_han.png",
    )
    # 6. Zhou Min (Water / Buffer)
    save_img(
        make_portrait((22, 30, 48, 255), (175, 195, 220, 255), (240, 210, 195, 255), (125, 211, 252, 255), (48, 68, 105, 255), (96, 165, 250, 255)),
        "textures/gui/portrait_zhou.png",
    )

    locked = Image.new("RGBA", (32, 32), (18, 22, 30, 255))
    rect(locked, 11, 13, 20, 23, (70, 80, 95, 255))
    rect(locked, 13, 8, 18, 13, (90, 100, 115, 255))
    rect(locked, 14, 10, 17, 13, (18, 22, 30, 255))
    save_img(locked, "textures/gui/portrait_locked.png")

    # Skill Icons
    se = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(2, 14):
        px(se, i, 15 - i, (220, 255, 255, 255))
        px(se, i, 14 - i, (0, 210, 255, 255))
    rect(se, 2, 11, 5, 13, (255, 200, 60, 255))
    save_img(se, "textures/gui/skill_e_dawn.png")

    sq = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d <= 36:
                px(sq, x, y, (255, 155, 25, 255))
            if d <= 16:
                px(sq, x, y, (255, 225, 95, 255))
    save_img(sq, "textures/gui/skill_q_dawn.png")


# ============================================================
# 3. ENTITY TEXTURES (64x64)
# ============================================================
def make_humanoid_skin(hair, skin, visor, torso, legs, accent):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rect(img, 0, 0, 31, 15, hair)
    rect(img, 8, 8, 15, 15, skin)
    rect(img, 8, 10, 15, 12, (25, 32, 42, 255))
    rect(img, 9, 11, 10, 11, visor)
    rect(img, 13, 11, 14, 11, visor)
    rect(img, 16, 16, 39, 31, torso)
    rect(img, 20, 20, 27, 31, accent)
    rect(img, 0, 16, 15, 31, legs)
    rect(img, 40, 16, 55, 31, torso)
    rect(img, 44, 28, 47, 31, skin)
    rect(img, 16, 48, 31, 63, legs)
    rect(img, 32, 48, 47, 63, torso)
    rect(img, 36, 60, 39, 63, skin)
    return img


def gen_entities():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x in range(0, 48):
        for y in range(0, 24):
            color = (48, 54, 65, 255)
            if x % 12 == 0 or x % 12 == 11 or y % 12 == 0 or y % 12 == 11:
                color = (25, 28, 35, 255)
            px(img, x, y, color)
    rect(img, 15, 15, 20, 20, (255, 50, 50, 255))
    rect(img, 17, 17, 18, 18, (255, 220, 220, 255))
    save_img(img, "textures/entity/security_drone.png")

    mech = Image.new("RGBA", (64, 64), (48, 52, 62, 255))
    for x in range(0, 64, 8):
        for y in range(0, 64):
            px(mech, x, y, (24, 26, 32, 255))
    for y in range(0, 64, 8):
        for x in range(0, 64):
            px(mech, x, y, (24, 26, 32, 255))
    rect(mech, 8, 8, 15, 15, (235, 165, 40, 255))
    rect(mech, 9, 10, 14, 12, (255, 120, 45, 255))
    rect(mech, 20, 22, 32, 30, (235, 165, 40, 255))
    rect(mech, 23, 24, 29, 28, (60, 205, 255, 255))
    save_img(mech, "textures/entity/precursor_mech.png")

    save_img(
        make_humanoid_skin((75, 80, 90, 255), (220, 180, 150, 255), (255, 185, 45, 255), (185, 125, 28, 255), (45, 50, 60, 255), (255, 205, 65, 255)),
        "textures/entity/npc_zheng.png",
    )
    save_img(
        make_humanoid_skin((40, 85, 75, 255), (230, 195, 175, 255), (90, 240, 185, 255), (42, 62, 58, 255), (32, 42, 40, 255), (74, 222, 128, 255)),
        "textures/entity/npc_han.png",
    )
    save_img(
        make_humanoid_skin((45, 65, 95, 255), (242, 215, 205, 255), (95, 225, 255, 255), (215, 228, 240, 255), (50, 65, 85, 255), (56, 189, 248, 255)),
        "textures/entity/npc_su.png",
    )


# ============================================================
# 4. MODELS, BLOCKSTATES, WEAPON ATTRIBUTES & LANG
# ============================================================
def gen_models_and_lang():
    for w in ("plasma_cutter", "cryo_sawblade", "hydro_pulse_gun", "aero_blade", "precursor_claymore"):
        save_json(
            {"parent": "minecraft:item/handheld", "textures": {"layer0": f"picking_hope_core:item/{w}"}},
            f"models/item/{w}.json",
        )
        save_json({"parent": "bettercombat:claymore"}, f"weapon_attributes/{w}.json", base=DATA)

    generated_items = [
        "fusion_core_chip",
        "void_drive_chip",
        "servo_battery_chip",
        "overclock_chip",
        "neural_halo_chip",
        "quantum_core",
        "alloy_plate",
        "precursor_spark",
        "credit_coin",
        "exp_bottle",
        "equip_essence",
        "star_ticket",
    ]
    for t in range(1, 7):
        generated_items.append(f"char_mat_{t}")
        generated_items.append(f"weapon_mat_{t}")
        generated_items.append(f"talent_mat_{t}")

    for it in generated_items:
        save_json(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"picking_hope_core:item/{it}"}},
            f"models/item/{it}.json",
        )

    save_json({"variants": {"": {"model": "picking_hope_core:block/orbital_drop_pod"}}}, "blockstates/orbital_drop_pod.json")
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

    save_json({"variants": {"": {"model": "picking_hope_core:block/precursor_altar"}}}, "blockstates/precursor_altar.json")
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
        "block.picking_hope_core.teleport_waypoint": "传送锚点",
        "item.picking_hope_core.teleport_waypoint": "传送锚点",
        "block.picking_hope_core.engineering_terminal": "模拟训练与管理终端",
        "item.picking_hope_core.engineering_terminal": "模拟训练与管理终端",
        "block.picking_hope_core.auxiliary_energy_core": "备用发电机组",
        "item.picking_hope_core.auxiliary_energy_core": "备用发电机组",
        "block.picking_hope_core.orbital_drop_pod": "轨道逃生舱",
        "item.picking_hope_core.orbital_drop_pod": "轨道逃生舱",
        "block.picking_hope_core.precursor_altar": "深空通讯塔控制台",
        "item.picking_hope_core.precursor_altar": "深空通讯塔控制台",
        "item.picking_hope_core.quantum_core": "旧版数据核心",
        "item.picking_hope_core.alloy_plate": "备用合金板",
        "item.picking_hope_core.precursor_spark": "高能反应堆零件",
        "item.picking_hope_core.credit_coin": "信用币",
        "item.picking_hope_core.exp_bottle": "角色经验瓶",
        "item.picking_hope_core.equip_essence": "装备强化粉尘",
        "item.picking_hope_core.star_ticket": "抽卡通行券",
        "item.picking_hope_core.plasma_cutter": "【地】工程切割刀",
        "item.picking_hope_core.cryo_sawblade": "【火】工业热熔锯",
        "item.picking_hope_core.hydro_pulse_gun": "【水】高压水冷枪",
        "item.picking_hope_core.aero_blade": "【气】轻型气动刃",
        "item.picking_hope_core.precursor_claymore": "【5★】重型合金大剑",
        "item.picking_hope_core.fusion_core_chip": "【1号位】生命控制芯片",
        "item.picking_hope_core.void_drive_chip": "【2号位】攻击驱动模块",
        "item.picking_hope_core.servo_battery_chip": "【3号位】能源供给电池",
        "item.picking_hope_core.overclock_chip": "【4号位】属性增幅芯片",
        "item.picking_hope_core.neural_halo_chip": "【5号位】战术辅助核心",
        "entity.picking_hope_core.security_drone": "故障巡检机器人",
        "entity.picking_hope_core.precursor_mech": "大型工程挖掘机",
        "entity.picking_hope_core.faction_npc": "幸存船员",
        "key.categories.picking_hope": "《重拾希望》按键绑定",
        "key.picking_hope.char_1": "小队槽位 1",
        "key.picking_hope.char_2": "小队槽位 2",
        "key.picking_hope.char_3": "小队槽位 3",
        "key.picking_hope.char_4": "小队槽位 4",
        "key.picking_hope.skill_e": "模拟训练副本 (E)",
        "key.picking_hope.burst_q": "快速备战面板 (Q)",
        "key.picking_hope.bag_b": "背包与合成溶解 (B)",
        "key.picking_hope.interact_f": "交互快捷键 (F)",
        "key.picking_hope.profile_c": "角色与装备养成 (C)",
        "key.picking_hope.wish_g": "抽卡界面 (G)",
    }
    en_us = {
        "itemGroup.picking_hope_core": "Picking Up Hope",
        "block.picking_hope_core.teleport_waypoint": "Teleport Waypoint",
        "item.picking_hope_core.teleport_waypoint": "Teleport Waypoint",
        "block.picking_hope_core.engineering_terminal": "Simulation & Management Terminal",
        "item.picking_hope_core.engineering_terminal": "Simulation & Management Terminal",
        "block.picking_hope_core.auxiliary_energy_core": "Backup Power Generator",
        "item.picking_hope_core.auxiliary_energy_core": "Backup Power Generator",
        "block.picking_hope_core.orbital_drop_pod": "Orbital Escape Pod",
        "item.picking_hope_core.orbital_drop_pod": "Orbital Escape Pod",
        "block.picking_hope_core.precursor_altar": "Deep-Space Comms Console",
        "item.picking_hope_core.precursor_altar": "Deep-Space Comms Console",
        "item.picking_hope_core.quantum_core": "Legacy Data Core",
        "item.picking_hope_core.alloy_plate": "Spare Alloy Plate",
        "item.picking_hope_core.precursor_spark": "Reactor Core Part",
        "item.picking_hope_core.credit_coin": "Credit Coin",
        "item.picking_hope_core.exp_bottle": "Character EXP Bottle",
        "item.picking_hope_core.equip_essence": "Equipment Upgrade Dust",
        "item.picking_hope_core.star_ticket": "Supply Pass Ticket",
        "item.picking_hope_core.plasma_cutter": "[Earth] Engineering Cutter",
        "item.picking_hope_core.cryo_sawblade": "[Fire] Industrial Torch Saw",
        "item.picking_hope_core.hydro_pulse_gun": "[Water] High-Pressure Water Gun",
        "item.picking_hope_core.aero_blade": "[Air] Light Pneumatic Blade",
        "item.picking_hope_core.precursor_claymore": "[5★] Heavy Alloy Greatblade",
        "item.picking_hope_core.fusion_core_chip": "[Slot 1] HP Control Chip",
        "item.picking_hope_core.void_drive_chip": "[Slot 2] ATK Drive Module",
        "item.picking_hope_core.servo_battery_chip": "[Slot 3] Energy Battery",
        "item.picking_hope_core.overclock_chip": "[Slot 4] Element Boost Chip",
        "item.picking_hope_core.neural_halo_chip": "[Slot 5] Tactical Core",
        "entity.picking_hope_core.security_drone": "Faulty Patrol Robot",
        "entity.picking_hope_core.precursor_mech": "Heavy Engineering Excavator",
        "entity.picking_hope_core.faction_npc": "Crew Member",
        "key.categories.picking_hope": "Picking Up Hope Keybinds",
        "key.picking_hope.char_1": "Party Slot 1",
        "key.picking_hope.char_2": "Party Slot 2",
        "key.picking_hope.char_3": "Party Slot 3",
        "key.picking_hope.char_4": "Party Slot 4",
        "key.picking_hope.skill_e": "Simulation Domains (E)",
        "key.picking_hope.burst_q": "Quick Status (Q)",
        "key.picking_hope.bag_b": "Inventory & Crafting (B)",
        "key.picking_hope.interact_f": "Interact (F)",
        "key.picking_hope.profile_c": "Character & Gear (C)",
        "key.picking_hope.wish_g": "Gacha Recruit (G)",
    }

    char_tier_names = ["1级人物零件·铁片", "2级人物零件·轴承", "3级人物零件·连杆", "4级人物零件·液压芯", "5级人物零件·精密轴", "6级人物零件·主控核"]
    weapon_tier_names = ["1级武器构件·粗胚", "2级武器构件·钢栓", "3级武器构件·导轨", "4级武器构件·稳压管", "5级武器构件·合金刃", "6级武器构件·精工心"]
    talent_tier_names = ["1级天赋手册·基础", "2级天赋手册·进阶", "3级天赋手册·实战", "4级天赋手册·战术", "5级天赋手册·专精", "6级天赋手册·大师"]
    for i in range(6):
        t = i + 1
        zh_cn[f"item.picking_hope_core.char_mat_{t}"] = char_tier_names[i]
        zh_cn[f"item.picking_hope_core.weapon_mat_{t}"] = weapon_tier_names[i]
        zh_cn[f"item.picking_hope_core.talent_mat_{t}"] = talent_tier_names[i]
        en_us[f"item.picking_hope_core.char_mat_{t}"] = f"Tier {t} Character Part"
        en_us[f"item.picking_hope_core.weapon_mat_{t}"] = f"Tier {t} Weapon Part"
        en_us[f"item.picking_hope_core.talent_mat_{t}"] = f"Tier {t} Talent Manual"

    save_json(zh_cn, "lang/zh_cn.json")
    save_json(en_us, "lang/en_us.json")


if __name__ == "__main__":
    gen_weapons()
    gen_chips()
    gen_materials()
    gen_gui_textures()
    gen_entities()
    gen_models_and_lang()
    print("All v2.0 assets, models, and localizations generated successfully!")
