import json
import os
import re
from PIL import Image, ImageDraw

BASE_RES = os.path.join("mod", "src", "main", "resources")
ASSETS = os.path.join(BASE_RES, "assets", "picking_hope_core")


def ensure_dir(path):
    os.makedirs(path, exist_ok=True)


def save_img(img, rel_path):
    full = os.path.join(ASSETS, rel_path)
    ensure_dir(os.path.dirname(full))
    img.save(full)


def save_json(obj, rel_path):
    full = os.path.join(ASSETS, rel_path)
    ensure_dir(os.path.dirname(full))
    with open(full, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def px(img, x, y, rgba):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), rgba)


def rect(img, x0, y0, x1, y1, rgba):
    x_min, x_max = min(x0, x1), max(x0, x1)
    y_min, y_max = min(y0, y1), max(y0, y1)
    for x in range(x_min, x_max + 1):
        for y in range(y_min, y_max + 1):
            px(img, x, y, rgba)


def draw_line(img, x0, y0, x1, y1, rgba, width=1):
    d = ImageDraw.Draw(img)
    d.line([(x0, y0), (x1, y1)], fill=rgba, width=width)


def add_circle(img, cx, cy, r, rgba, outline=None):
    d = ImageDraw.Draw(img)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=rgba, outline=outline)


def make_cube(f, t, faces_tex, uv_mode="full"):
    faces = {}
    for d in ("north", "south", "east", "west", "up", "down"):
        tex_ref = faces_tex.get(d, faces_tex.get("all"))
        if tex_ref:
            if uv_mode == "fit":
                if d in ("north", "south"):
                    uv = [f[0], 16 - t[1], t[0], 16 - f[1]]
                elif d in ("east", "west"):
                    uv = [f[2], 16 - t[1], t[2], 16 - f[1]]
                else:
                    uv = [f[0], f[2], t[0], t[2]]
                uv = [max(0, min(16, v)) for v in uv]
            else:
                uv = [0, 0, 16, 16]
            faces[d] = {"texture": f"#{tex_ref}", "uv": uv}
    return {"from": f, "to": t, "faces": faces}


# ============================================================
# 1. PROCEDURAL 32x32 BLOCK & PROP TEXTURES
# ============================================================
def gen_textures():
    # --- Starship Theme ---
    # starship_grate_floor: transparent center diamond/grid slots + steel frame
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rect(img, 0, 0, 31, 2, (46, 56, 70, 255))
    rect(img, 0, 29, 31, 31, (46, 56, 70, 255))
    rect(img, 0, 0, 2, 31, (46, 56, 70, 255))
    rect(img, 29, 0, 31, 31, (46, 56, 70, 255))
    for i in range(6, 28, 6):
        rect(img, i, 2, i + 1, 29, (68, 82, 100, 255))
        rect(img, 2, i, 29, i + 1, (68, 82, 100, 255))
    for x in range(3, 29, 6):
        px(img, x, 2, (235, 175, 45, 255))
    save_img(img, "textures/block/starship_grate_floor.png")

    # starship_white_panel: clean off-white medbay / cabin bulkhead
    img = Image.new("RGBA", (32, 32), (208, 218, 228, 255))
    rect(img, 1, 1, 30, 30, (226, 234, 242, 255))
    rect(img, 3, 3, 28, 28, (236, 242, 248, 255))
    rect(img, 1, 15, 30, 16, (170, 184, 198, 255))
    rect(img, 15, 1, 16, 30, (170, 184, 198, 255))
    rect(img, 4, 24, 14, 26, (56, 189, 248, 255))
    save_img(img, "textures/block/starship_white_panel.png")

    # starship_dark_rib + top
    img = Image.new("RGBA", (32, 32), (26, 32, 42, 255))
    rect(img, 2, 0, 29, 31, (36, 44, 58, 255))
    for x in (5, 12, 19, 26):
        rect(img, x, 0, x + 2, 31, (54, 65, 82, 255))
    rect(img, 0, 14, 31, 17, (22, 27, 36, 255))
    save_img(img, "textures/block/starship_dark_rib.png")

    top = Image.new("RGBA", (32, 32), (24, 30, 40, 255))
    rect(top, 3, 3, 28, 28, (46, 56, 72, 255))
    rect(top, 8, 8, 23, 23, (30, 36, 48, 255))
    save_img(top, "textures/block/starship_dark_rib_top.png")

    # starship_ceiling_tile
    img = Image.new("RGBA", (32, 32), (44, 52, 66, 255))
    rect(img, 2, 2, 29, 29, (58, 68, 84, 255))
    for i in range(5, 27, 5):
        rect(img, i, 5, i + 2, 26, (38, 46, 58, 255))
    rect(img, 0, 0, 31, 1, (28, 34, 44, 255))
    rect(img, 0, 30, 31, 31, (28, 34, 44, 255))
    save_img(img, "textures/block/starship_ceiling_tile.png")

    # starship_damaged_hull: scorched hull + glowing orange embers
    img = Image.new("RGBA", (32, 32), (26, 28, 34, 255))
    rect(img, 2, 2, 29, 29, (38, 42, 50, 255))
    draw_line(img, 4, 4, 27, 25, (14, 15, 18, 255), 3)
    draw_line(img, 24, 5, 8, 26, (18, 19, 22, 255), 2)
    draw_line(img, 9, 10, 21, 20, (245, 115, 35, 255), 1)
    px(img, 14, 15, (255, 200, 90, 255))
    px(img, 18, 18, (255, 140, 45, 255))
    save_img(img, "textures/block/starship_damaged_hull.png")

    # starship_railing (transparent item & metal rail bar texture)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rect(img, 3, 2, 6, 30, (74, 88, 108, 255))
    rect(img, 25, 2, 28, 30, (74, 88, 108, 255))
    rect(img, 2, 5, 29, 9, (125, 145, 170, 255))
    rect(img, 2, 16, 29, 19, (90, 108, 130, 255))
    rect(img, 4, 6, 27, 7, (56, 189, 248, 255))
    save_img(img, "textures/block/starship_railing.png")

    # starship_railing_solid (opaque metallic bar texture for 3D railing elements)
    rs = Image.new("RGBA", (32, 32), (62, 74, 92, 255))
    rect(rs, 2, 2, 29, 29, (92, 110, 134, 255))
    rect(rs, 2, 13, 29, 17, (56, 189, 248, 255))
    save_img(rs, "textures/block/starship_railing_solid.png")

    # starship_viewport_pane
    img = Image.new("RGBA", (32, 32), (95, 195, 240, 110))
    rect(img, 0, 0, 31, 2, (58, 70, 88, 255))
    rect(img, 0, 29, 31, 31, (58, 70, 88, 255))
    draw_line(img, 6, 22, 22, 6, (215, 248, 255, 180), 2)
    save_img(img, "textures/block/starship_viewport_pane.png")

    # starship_airlock_door_top / bottom / item
    dt = Image.new("RGBA", (32, 32), (48, 58, 74, 255))
    rect(dt, 2, 2, 29, 29, (68, 82, 102, 255))
    rect(dt, 7, 5, 24, 18, (85, 185, 235, 190))
    rect(dt, 5, 22, 26, 26, (235, 175, 45, 255))
    save_img(dt, "textures/block/starship_airlock_door_top.png")

    db = Image.new("RGBA", (32, 32), (40, 48, 62, 255))
    rect(db, 2, 2, 29, 29, (58, 70, 88, 255))
    rect(db, 5, 5, 26, 25, (48, 58, 74, 255))
    rect(db, 24, 10, 27, 16, (56, 189, 248, 255))
    save_img(db, "textures/block/starship_airlock_door_bottom.png")

    di = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rect(di, 9, 1, 22, 30, (68, 82, 102, 255))
    rect(di, 11, 3, 20, 12, (90, 200, 245, 255))
    rect(di, 11, 14, 20, 16, (235, 175, 45, 255))
    rect(di, 11, 18, 20, 28, (48, 58, 74, 255))
    save_img(di, "textures/block/starship_airlock_door_item.png")

    # starship_vent_hatch
    img = Image.new("RGBA", (32, 32), (46, 56, 70, 255))
    rect(img, 2, 2, 29, 29, (64, 78, 96, 255))
    for y in range(6, 26, 4):
        rect(img, 5, y, 26, y + 1, (22, 28, 36, 255))
    save_img(img, "textures/block/starship_vent_hatch.png")

    # cyan & amber strips
    for name, c1, c2 in (
        ("cyan_light_strip", (35, 155, 225, 255), (165, 242, 255, 255)),
        ("amber_light_strip", (225, 125, 25, 255), (255, 220, 115, 255)),
    ):
        im = Image.new("RGBA", (32, 32), (38, 46, 58, 255))
        rect(im, 2, 6, 29, 25, c1)
        rect(im, 4, 10, 27, 21, c2)
        save_img(im, f"textures/block/{name}.png")

    # --- Planet / Polar / Outpost / Mine / Geothermal Theme ---
    # permafrost_bricks
    img = Image.new("RGBA", (32, 32), (42, 52, 66, 255))
    for row in range(4):
        y0 = row * 8
        rect(img, 0, y0, 31, y0 + 6, (66, 82, 102, 255))
        rect(img, 1, y0 + 1, 30, y0 + 5, (78, 96, 118, 255))
        off = 8 if row % 2 else 0
        rect(img, off, y0, off + 1, y0 + 6, (36, 45, 58, 255))
        rect(img, (off + 16) % 32, y0, (off + 16) % 32 + 1, y0 + 6, (36, 45, 58, 255))
    save_img(img, "textures/block/permafrost_bricks.png")

    # glacier_ice_block
    img = Image.new("RGBA", (32, 32), (95, 185, 235, 170))
    rect(img, 2, 2, 29, 29, (125, 210, 248, 165))
    draw_line(img, 4, 12, 14, 4, (220, 248, 255, 210), 2)
    draw_line(img, 16, 26, 26, 14, (220, 248, 255, 190), 2)
    save_img(img, "textures/block/glacier_ice_block.png")

    # outpost_insulated_wall (clean polar white-silver + warm orange trim)
    img = Image.new("RGBA", (32, 32), (185, 196, 208, 255))
    rect(img, 2, 2, 29, 29, (222, 230, 238, 255))
    rect(img, 2, 12, 29, 17, (235, 125, 40, 255))
    rect(img, 2, 18, 29, 19, (64, 74, 88, 255))
    save_img(img, "textures/block/outpost_insulated_wall.png")

    # outpost_roof_plating
    img = Image.new("RGBA", (32, 32), (62, 74, 90, 255))
    for i in range(0, 32, 4):
        rect(img, i, 0, i + 1, 31, (82, 98, 118, 255))
        rect(img, i + 2, 0, i + 3, 31, (48, 58, 72, 255))
    save_img(img, "textures/block/outpost_roof_plating.png")

    # mine_steel_beam + top
    img = Image.new("RGBA", (32, 32), (52, 56, 62, 255))
    rect(img, 3, 0, 28, 31, (84, 90, 98, 255))
    rect(img, 9, 0, 22, 31, (48, 52, 58, 255))
    rect(img, 2, 14, 29, 17, (145, 92, 46, 255))
    save_img(img, "textures/block/mine_steel_beam.png")
    img = Image.new("RGBA", (32, 32), (52, 56, 62, 255))
    rect(img, 4, 4, 27, 27, (84, 90, 98, 255))
    save_img(img, "textures/block/mine_steel_beam_top.png")

    # mine_steel_truss (cutout criss-cross scaffold)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rect(img, 0, 0, 31, 3, (78, 84, 92, 255))
    rect(img, 0, 28, 31, 31, (78, 84, 92, 255))
    rect(img, 0, 0, 3, 31, (78, 84, 92, 255))
    rect(img, 28, 0, 31, 31, (78, 84, 92, 255))
    draw_line(img, 2, 2, 29, 29, (112, 118, 126, 255), 3)
    draw_line(img, 29, 2, 2, 29, (112, 118, 126, 255), 3)
    save_img(img, "textures/block/mine_steel_truss.png")

    # mine_rusted_plate
    img = Image.new("RGBA", (32, 32), (76, 64, 58, 255))
    rect(img, 2, 2, 29, 29, (98, 84, 76, 255))
    rect(img, 5, 6, 16, 14, (138, 76, 42, 255))
    rect(img, 18, 18, 27, 26, (124, 68, 38, 255))
    save_img(img, "textures/block/mine_rusted_plate.png")

    # mine_concrete_floor
    img = Image.new("RGBA", (32, 32), (96, 100, 106, 255))
    for x in range(32):
        for y in range(32):
            v = ((x * 19 + y * 29) ^ (x + y)) % 11
            if v < 2:
                px(img, x, y, (82, 86, 92, 255))
            elif v > 8:
                px(img, x, y, (112, 116, 122, 255))
    rect(img, 0, 31, 31, 31, (72, 76, 82, 255))
    rect(img, 31, 0, 31, 31, (72, 76, 82, 255))
    save_img(img, "textures/block/mine_concrete_floor.png")

    # geothermal_basalt
    img = Image.new("RGBA", (32, 32), (38, 34, 42, 255))
    for x in range(32):
        for y in range(32):
            v = (x * 11 + y * 17) % 9
            if v < 2:
                px(img, x, y, (28, 24, 30, 255))
            elif v > 6:
                px(img, x, y, (56, 50, 60, 255))
    save_img(img, "textures/block/geothermal_basalt.png")

    # geothermal_warm_stone
    img = Image.open(os.path.join(ASSETS, "textures/block/geothermal_basalt.png")).copy()
    draw_line(img, 4, 8, 26, 14, (235, 115, 35, 255), 2)
    draw_line(img, 6, 22, 25, 24, (255, 175, 55, 255), 1)
    save_img(img, "textures/block/geothermal_warm_stone.png")

    # geothermal_steam_vent + top
    save_img(Image.open(os.path.join(ASSETS, "textures/block/geothermal_basalt.png")), "textures/block/geothermal_steam_vent_side.png")
    sv = Image.new("RGBA", (32, 32), (48, 50, 56, 255))
    for y in range(4, 28, 4):
        rect(sv, 4, y, 27, y + 1, (215, 120, 45, 255))
    save_img(sv, "textures/block/geothermal_steam_vent_top.png")

    # geothermal_glow_moss
    img = Image.open(os.path.join(ASSETS, "textures/block/geothermal_basalt.png")).copy()
    for (cx, cy) in ((7, 7), (19, 10), (11, 21), (24, 23)):
        add_circle(img, cx, cy, 4, (35, 145, 95, 255))
        add_circle(img, cx, cy, 2, (110, 245, 175, 255))
    save_img(img, "textures/block/geothermal_glow_moss.png")

    # --- Alien Ruins & Ark Harbor Theme ---
    # ruin_obsidian_block
    img = Image.new("RGBA", (32, 32), (18, 20, 28, 255))
    rect(img, 2, 2, 29, 29, (26, 28, 40, 255))
    rect(img, 6, 6, 25, 25, (20, 22, 32, 255))
    save_img(img, "textures/block/ruin_obsidian_block.png")

    # ruin_platinum_trim
    img = Image.new("RGBA", (32, 32), (178, 186, 198, 255))
    rect(img, 2, 2, 29, 29, (214, 222, 232, 255))
    rect(img, 6, 6, 25, 25, (192, 202, 214, 255))
    rect(img, 14, 2, 17, 29, (235, 195, 95, 255))
    save_img(img, "textures/block/ruin_platinum_trim.png")

    # ruin_carved_glyph
    img = Image.open(os.path.join(ASSETS, "textures/block/ruin_obsidian_block.png")).copy()
    rect(img, 6, 6, 25, 7, (56, 215, 210, 255))
    rect(img, 6, 24, 25, 25, (56, 215, 210, 255))
    rect(img, 12, 11, 19, 20, (240, 195, 85, 255))
    rect(img, 14, 13, 17, 18, (24, 26, 36, 255))
    save_img(img, "textures/block/ruin_carved_glyph.png")

    # ruin_circuit_teal & gold
    for name, c in (("ruin_circuit_teal", (45, 235, 225, 255)), ("ruin_circuit_gold", (250, 200, 75, 255))):
        im = Image.open(os.path.join(ASSETS, "textures/block/ruin_obsidian_block.png")).copy()
        rect(im, 0, 15, 31, 16, c)
        rect(im, 15, 0, 16, 31, c)
        rect(im, 11, 11, 20, 20, c)
        rect(im, 13, 13, 18, 18, (240, 255, 255, 255))
        save_img(im, f"textures/block/{name}.png")

    # ruin_pillar + top
    im = Image.new("RGBA", (32, 32), (22, 24, 34, 255))
    for x in (4, 12, 20, 27):
        rect(im, x, 0, x + 2, 31, (195, 204, 216, 255))
    rect(im, 14, 0, 17, 31, (45, 225, 215, 255))
    save_img(im, "textures/block/ruin_pillar.png")
    im = Image.new("RGBA", (32, 32), (22, 24, 34, 255))
    rect(im, 4, 4, 27, 27, (195, 204, 216, 255))
    rect(im, 10, 10, 21, 21, (45, 225, 215, 255))
    save_img(im, "textures/block/ruin_pillar_top.png")

    # ruin_floor_tile
    im = Image.new("RGBA", (32, 32), (30, 34, 48, 255))
    rect(im, 2, 2, 14, 14, (44, 48, 66, 255))
    rect(im, 17, 2, 29, 14, (44, 48, 66, 255))
    rect(im, 2, 17, 14, 29, (44, 48, 66, 255))
    rect(im, 17, 17, 29, 29, (44, 48, 66, 255))
    rect(im, 14, 14, 17, 17, (235, 190, 80, 255))
    save_img(im, "textures/block/ruin_floor_tile.png")

    # ruin_energy_core
    im = Image.new("RGBA", (32, 32), (20, 48, 64, 255))
    rect(im, 4, 4, 27, 27, (35, 185, 210, 255))
    rect(im, 9, 9, 22, 22, (165, 252, 255, 255))
    rect(im, 0, 0, 3, 31, (200, 210, 225, 255))
    rect(im, 28, 0, 31, 31, (200, 210, 225, 255))
    save_img(im, "textures/block/ruin_energy_core.png")

    # ruin_hardlight_bridge & barrier
    im = Image.new("RGBA", (32, 32), (45, 225, 235, 155))
    rect(im, 0, 0, 31, 2, (180, 252, 255, 235))
    rect(im, 0, 29, 31, 31, (180, 252, 255, 235))
    rect(im, 4, 14, 27, 17, (210, 255, 255, 200))
    save_img(im, "textures/block/ruin_hardlight_bridge.png")

    im = Image.new("RGBA", (32, 32), (55, 220, 245, 125))
    rect(im, 0, 0, 31, 2, (205, 248, 255, 220))
    for y in (8, 16, 24):
        rect(im, 2, y, 29, y + 1, (165, 248, 255, 180))
    save_img(im, "textures/block/ruin_hardlight_barrier.png")

    # ruin_ark_hull & docking_beacon
    im = Image.new("RGBA", (32, 32), (34, 30, 52, 255))
    rect(im, 2, 2, 29, 29, (48, 42, 72, 255))
    rect(im, 4, 14, 27, 17, (235, 185, 85, 255))
    save_img(im, "textures/block/ruin_ark_hull.png")

    im = Image.new("RGBA", (32, 32), (34, 30, 52, 255))
    rect(im, 3, 3, 28, 28, (245, 115, 205, 255))
    rect(im, 8, 8, 23, 23, (255, 235, 175, 255))
    save_img(im, "textures/block/ruin_docking_beacon.png")

    # --- Prop & Furniture Textures (32x32 crisp pixel art) ---
    def save_solid(name, base, accent=None, stripe=None):
        p_img = Image.new("RGBA", (32, 32), base)
        rect(p_img, 2, 2, 29, 29, (min(255, base[0] + 16), min(255, base[1] + 16), min(255, base[2] + 16), 255))
        if accent:
            rect(p_img, 5, 5, 26, 26, accent)
        if stripe:
            rect(p_img, 3, 13, 28, 18, stripe)
        save_img(p_img, f"textures/block/{name}.png")

    save_solid("prop_metal_dark", (38, 46, 58, 255), (52, 62, 78, 255))
    save_solid("prop_metal_light", (195, 205, 218, 255), (222, 230, 240, 255))
    save_solid("prop_amber_trim", (46, 54, 66, 255), None, (240, 170, 40, 255))
    save_solid("prop_bed_fabric", (45, 95, 138, 255), (65, 135, 185, 255))
    save_solid("prop_barrel_tex", (52, 74, 96, 255), (68, 96, 124, 255), (235, 165, 45, 255))
    save_solid("prop_blackbox_tex", (215, 95, 28, 255), (245, 128, 42, 255), (255, 225, 110, 255))
    save_solid("prop_ice_crystal", (95, 210, 250, 200), (195, 248, 255, 235))

    # Server front panel texture with blinking indicator dots
    sr = Image.new("RGBA", (32, 32), (24, 30, 40, 255))
    for y in range(4, 28, 6):
        rect(sr, 3, y, 28, y + 4, (36, 44, 58, 255))
        rect(sr, 5, y + 1, 7, y + 3, (56, 189, 248, 255))
        rect(sr, 9, y + 1, 11, y + 3, (74, 222, 128, 255))
    save_img(sr, "textures/block/prop_server_front.png")

    # Monitor glass screen
    ms = Image.new("RGBA", (32, 32), (14, 24, 38, 255))
    rect(ms, 2, 2, 29, 29, (22, 40, 62, 255))
    rect(ms, 4, 4, 20, 7, (56, 189, 248, 255))
    rect(ms, 4, 10, 26, 13, (74, 222, 128, 255))
    rect(ms, 4, 16, 16, 19, (251, 191, 36, 255))
    rect(ms, 22, 16, 27, 26, (56, 189, 248, 255))
    save_img(ms, "textures/block/prop_screen_glow.png")


# ============================================================
# 2. VANILLA 1.18.2 BLOCKSTATES & MULTI-CUBE VOXEL MODELS
# ============================================================
def gen_stair_blockstate(stair_id, full_id):
    # Exact vanilla 1.18.2 stone_brick_stairs rotation table
    table = {
        ("east", "bottom", "inner_left"): {"y": 270, "uvlock": True, "m": "inner"},
        ("east", "bottom", "inner_right"): {"m": "inner"},
        ("east", "bottom", "outer_left"): {"y": 270, "uvlock": True, "m": "outer"},
        ("east", "bottom", "outer_right"): {"m": "outer"},
        ("east", "bottom", "straight"): {"m": "base"},
        ("east", "top", "inner_left"): {"x": 180, "uvlock": True, "m": "inner"},
        ("east", "top", "inner_right"): {"x": 180, "y": 90, "uvlock": True, "m": "inner"},
        ("east", "top", "outer_left"): {"x": 180, "uvlock": True, "m": "outer"},
        ("east", "top", "outer_right"): {"x": 180, "y": 90, "uvlock": True, "m": "outer"},
        ("east", "top", "straight"): {"x": 180, "uvlock": True, "m": "base"},

        ("north", "bottom", "inner_left"): {"y": 180, "uvlock": True, "m": "inner"},
        ("north", "bottom", "inner_right"): {"y": 270, "uvlock": True, "m": "inner"},
        ("north", "bottom", "outer_left"): {"y": 180, "uvlock": True, "m": "outer"},
        ("north", "bottom", "outer_right"): {"y": 270, "uvlock": True, "m": "outer"},
        ("north", "bottom", "straight"): {"y": 270, "uvlock": True, "m": "base"},
        ("north", "top", "inner_left"): {"x": 180, "y": 270, "uvlock": True, "m": "inner"},
        ("north", "top", "inner_right"): {"x": 180, "uvlock": True, "m": "inner"},
        ("north", "top", "outer_left"): {"x": 180, "y": 270, "uvlock": True, "m": "outer"},
        ("north", "top", "outer_right"): {"x": 180, "uvlock": True, "m": "outer"},
        ("north", "top", "straight"): {"x": 180, "y": 270, "uvlock": True, "m": "base"},

        ("south", "bottom", "inner_left"): {"m": "inner"},
        ("south", "bottom", "inner_right"): {"y": 90, "uvlock": True, "m": "inner"},
        ("south", "bottom", "outer_left"): {"m": "outer"},
        ("south", "bottom", "outer_right"): {"y": 90, "uvlock": True, "m": "outer"},
        ("south", "bottom", "straight"): {"y": 90, "uvlock": True, "m": "base"},
        ("south", "top", "inner_left"): {"x": 180, "y": 90, "uvlock": True, "m": "inner"},
        ("south", "top", "inner_right"): {"x": 180, "y": 180, "uvlock": True, "m": "inner"},
        ("south", "top", "outer_left"): {"x": 180, "y": 90, "uvlock": True, "m": "outer"},
        ("south", "top", "outer_right"): {"x": 180, "y": 180, "uvlock": True, "m": "outer"},
        ("south", "top", "straight"): {"x": 180, "y": 90, "uvlock": True, "m": "base"},

        ("west", "bottom", "inner_left"): {"y": 90, "uvlock": True, "m": "inner"},
        ("west", "bottom", "inner_right"): {"y": 180, "uvlock": True, "m": "inner"},
        ("west", "bottom", "outer_left"): {"y": 90, "uvlock": True, "m": "outer"},
        ("west", "bottom", "outer_right"): {"y": 180, "uvlock": True, "m": "outer"},
        ("west", "bottom", "straight"): {"y": 180, "uvlock": True, "m": "base"},
        ("west", "top", "inner_left"): {"x": 180, "y": 180, "uvlock": True, "m": "inner"},
        ("west", "top", "inner_right"): {"x": 180, "y": 270, "uvlock": True, "m": "inner"},
        ("west", "top", "outer_left"): {"x": 180, "y": 180, "uvlock": True, "m": "outer"},
        ("west", "top", "outer_right"): {"x": 180, "y": 270, "uvlock": True, "m": "outer"},
        ("west", "top", "straight"): {"x": 180, "y": 180, "uvlock": True, "m": "base"},
    }
    variants = {}
    for (facing, half, shape), cfg in table.items():
        m_suffix = "" if cfg["m"] == "base" else ("_" + cfg["m"])
        entry = {"model": f"picking_hope_core:block/{stair_id}{m_suffix}"}
        if "x" in cfg:
            entry["x"] = cfg["x"]
        if "y" in cfg:
            entry["y"] = cfg["y"]
        if cfg.get("uvlock"):
            entry["uvlock"] = True
        variants[f"facing={facing},half={half},shape={shape}"] = entry
    save_json({"variants": variants}, f"blockstates/{stair_id}.json")

    tex = f"picking_hope_core:block/{full_id}"
    for suffix, parent in (("", "minecraft:block/stairs"), ("_inner", "minecraft:block/inner_stairs"), ("_outer", "minecraft:block/outer_stairs")):
        save_json({"parent": parent, "textures": {"bottom": tex, "top": tex, "side": tex}}, f"models/block/{stair_id}{suffix}.json")
    save_json({"parent": f"picking_hope_core:block/{stair_id}"}, f"models/item/{stair_id}.json")


def gen_slab_blockstate(slab_id, tex_id, double_model_id=None):
    double_model = double_model_id if double_model_id else tex_id
    if double_model_id:
        # Custom double model (e.g., ruin_hardlight_bridge_double)
        save_json({"parent": "minecraft:block/cube_all", "textures": {"all": f"picking_hope_core:block/{tex_id}"}},
                  f"models/block/{double_model_id}.json")
    save_json({
        "variants": {
            "type=bottom": {"model": f"picking_hope_core:block/{slab_id}"},
            "type=double": {"model": f"picking_hope_core:block/{double_model}"},
            "type=top": {"model": f"picking_hope_core:block/{slab_id}_top"},
        }
    }, f"blockstates/{slab_id}.json")
    tex = f"picking_hope_core:block/{tex_id}"
    save_json({"parent": "minecraft:block/slab", "textures": {"bottom": tex, "top": tex, "side": tex}}, f"models/block/{slab_id}.json")
    save_json({"parent": "minecraft:block/slab_top", "textures": {"bottom": tex, "top": tex, "side": tex}}, f"models/block/{slab_id}_top.json")
    save_json({"parent": f"picking_hope_core:block/{slab_id}"}, f"models/item/{slab_id}.json")


def gen_pillar_blockstate(pillar_id):
    save_json({
        "variants": {
            "axis=x": {"model": f"picking_hope_core:block/{pillar_id}_horizontal", "x": 90, "y": 90},
            "axis=y": {"model": f"picking_hope_core:block/{pillar_id}"},
            "axis=z": {"model": f"picking_hope_core:block/{pillar_id}_horizontal", "x": 90},
        }
    }, f"blockstates/{pillar_id}.json")
    texs = {"end": f"picking_hope_core:block/{pillar_id}_top", "side": f"picking_hope_core:block/{pillar_id}"}
    save_json({"parent": "minecraft:block/cube_column", "textures": texs}, f"models/block/{pillar_id}.json")
    save_json({"parent": "minecraft:block/cube_column_horizontal", "textures": texs}, f"models/block/{pillar_id}_horizontal.json")
    save_json({"parent": f"picking_hope_core:block/{pillar_id}"}, f"models/item/{pillar_id}.json")


def gen_railing_and_panes():
    # 1. starship_railing (3D metallic handrail post + horizontal bars)
    save_json({
        "multipart": [
            {"apply": {"model": "picking_hope_core:block/starship_railing_post"}},
            {"when": {"north": "true"}, "apply": {"model": "picking_hope_core:block/starship_railing_side"}},
            {"when": {"east": "true"}, "apply": {"model": "picking_hope_core:block/starship_railing_side", "y": 90}},
            {"when": {"south": "true"}, "apply": {"model": "picking_hope_core:block/starship_railing_side", "y": 180}},
            {"when": {"west": "true"}, "apply": {"model": "picking_hope_core:block/starship_railing_side", "y": 270}},
        ]
    }, "blockstates/starship_railing.json")
    save_json({
        "textures": {"particle": "picking_hope_core:block/starship_railing_solid", "t": "picking_hope_core:block/starship_railing_solid"},
        "elements": [make_cube([6.5, 0, 6.5], [9.5, 16, 9.5], {"all": "t"})],
    }, "models/block/starship_railing_post.json")
    save_json({
        "textures": {"particle": "picking_hope_core:block/starship_railing_solid", "t": "picking_hope_core:block/starship_railing_solid"},
        "elements": [
            make_cube([7, 13, 0], [9, 15, 7], {"all": "t"}),
            make_cube([7.2, 6, 0], [8.8, 8, 7], {"all": "t"}),
            make_cube([7.5, 0, 1], [8.5, 13, 2], {"all": "t"}),
        ],
    }, "models/block/starship_railing_side.json")
    save_json({"parent": "minecraft:item/generated", "textures": {"layer0": "picking_hope_core:block/starship_railing"}},
              "models/item/starship_railing.json")

    # 2. Translucent thin panes: starship_viewport_pane & ruin_hardlight_barrier
    for pid in ("starship_viewport_pane", "ruin_hardlight_barrier"):
        save_json({
            "multipart": [
                {"apply": {"model": f"picking_hope_core:block/{pid}_post"}},
                {"when": {"north": "true"}, "apply": {"model": f"picking_hope_core:block/{pid}_side"}},
                {"when": {"east": "true"}, "apply": {"model": f"picking_hope_core:block/{pid}_side", "y": 90}},
                {"when": {"south": "true"}, "apply": {"model": f"picking_hope_core:block/{pid}_side", "y": 180}},
                {"when": {"west": "true"}, "apply": {"model": f"picking_hope_core:block/{pid}_side", "y": 270}},
            ]
        }, f"blockstates/{pid}.json")
        save_json({
            "textures": {"particle": f"picking_hope_core:block/{pid}", "t": f"picking_hope_core:block/{pid}"},
            "elements": [make_cube([7, 0, 7], [9, 16, 9], {"all": "t"})],
        }, f"models/block/{pid}_post.json")
        save_json({
            "textures": {"particle": f"picking_hope_core:block/{pid}", "t": f"picking_hope_core:block/{pid}"},
            "elements": [make_cube([7, 0, 0], [9, 16, 7], {"all": "t"})],
        }, f"models/block/{pid}_side.json")
        save_json({"parent": "minecraft:item/generated", "textures": {"layer0": f"picking_hope_core:block/{pid}"}},
                  f"models/item/{pid}.json")


def gen_door_and_hatch():
    # starship_airlock_door — exact vanilla 1.18.2 iron_door rotation table
    def entry(model_name, y=None):
        d = {"model": f"picking_hope_core:block/{model_name}"}
        if y:
            d["y"] = y
        return d

    did = "starship_airlock_door"
    y_base = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, yb in y_base.items():
        for half, hname in (("lower", "bottom"), ("upper", "top")):
            # hinge=left open=false -> model base, y=yb
            variants[f"facing={facing},half={half},hinge=left,open=false"] = entry(f"{did}_{hname}", yb)
            # hinge=left open=true -> model hinge, y=(yb+90)%360
            variants[f"facing={facing},half={half},hinge=left,open=true"] = entry(f"{did}_{hname}_hinge", (yb + 90) % 360)
            # hinge=right open=false -> model hinge, y=yb
            variants[f"facing={facing},half={half},hinge=right,open=false"] = entry(f"{did}_{hname}_hinge", yb)
            # hinge=right open=true -> model base, y=(yb+270)%360
            variants[f"facing={facing},half={half},hinge=right,open=true"] = entry(f"{did}_{hname}", (yb + 270) % 360)

    save_json({"variants": variants}, f"blockstates/{did}.json")
    texs = {"top": f"picking_hope_core:block/{did}_top", "bottom": f"picking_hope_core:block/{did}_bottom"}
    save_json({"parent": "minecraft:block/door_bottom", "textures": texs}, f"models/block/{did}_bottom.json")
    save_json({"parent": "minecraft:block/door_bottom_rh", "textures": texs}, f"models/block/{did}_bottom_hinge.json")
    save_json({"parent": "minecraft:block/door_top", "textures": texs}, f"models/block/{did}_top.json")
    save_json({"parent": "minecraft:block/door_top_rh", "textures": texs}, f"models/block/{did}_top_hinge.json")
    save_json({"parent": "minecraft:item/generated", "textures": {"layer0": f"picking_hope_core:block/{did}_item"}},
              f"models/item/{did}.json")

    # starship_vent_hatch — exact vanilla 1.18.2 iron_trapdoor table
    hid = "starship_vent_hatch"
    h_vars = {}
    for facing, yb in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        h_vars[f"facing={facing},half=bottom,open=false"] = {"model": f"picking_hope_core:block/{hid}_bottom"}
        h_vars[f"facing={facing},half=top,open=false"] = {"model": f"picking_hope_core:block/{hid}_top"}
        op = {"model": f"picking_hope_core:block/{hid}_open"}
        if yb:
            op["y"] = yb
        h_vars[f"facing={facing},half=bottom,open=true"] = op
        h_vars[f"facing={facing},half=top,open=true"] = op
    save_json({"variants": h_vars}, f"blockstates/{hid}.json")
    htex = {"texture": f"picking_hope_core:block/{hid}"}
    save_json({"parent": "minecraft:block/template_orientable_trapdoor_bottom", "textures": htex}, f"models/block/{hid}_bottom.json")
    save_json({"parent": "minecraft:block/template_orientable_trapdoor_top", "textures": htex}, f"models/block/{hid}_top.json")
    save_json({"parent": "minecraft:block/template_orientable_trapdoor_open", "textures": htex}, f"models/block/{hid}_open.json")
    save_json({"parent": f"picking_hope_core:block/{hid}_bottom"}, f"models/item/{hid}.json")


def gen_new_models_and_states():
    # 1. Simple cube_all blocks
    simple_cubes = [
        "starship_grate_floor", "starship_white_panel", "starship_ceiling_tile", "starship_damaged_hull",
        "permafrost_bricks", "glacier_ice_block", "outpost_insulated_wall", "outpost_roof_plating",
        "mine_steel_truss", "mine_rusted_plate", "mine_concrete_floor",
        "geothermal_basalt", "geothermal_warm_stone", "geothermal_glow_moss",
        "ruin_obsidian_block", "ruin_platinum_trim", "ruin_carved_glyph",
        "ruin_circuit_teal", "ruin_circuit_gold", "ruin_floor_tile", "ruin_energy_core",
        "ruin_ark_hull", "ruin_docking_beacon",
    ]
    for b in simple_cubes:
        save_json({"variants": {"": {"model": f"picking_hope_core:block/{b}"}}}, f"blockstates/{b}.json")
        save_json({"parent": "minecraft:block/cube_all", "textures": {"all": f"picking_hope_core:block/{b}"}},
                  f"models/block/{b}.json")
        save_json({"parent": f"picking_hope_core:block/{b}"}, f"models/item/{b}.json")

    # steam vent (top grate + basalt sides)
    save_json({"variants": {"": {"model": "picking_hope_core:block/geothermal_steam_vent"}}},
              "blockstates/geothermal_steam_vent.json")
    save_json({
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "top": "picking_hope_core:block/geothermal_steam_vent_top",
            "bottom": "picking_hope_core:block/geothermal_steam_vent_side",
            "side": "picking_hope_core:block/geothermal_steam_vent_side",
        },
    }, "models/block/geothermal_steam_vent.json")
    save_json({"parent": "picking_hope_core:block/geothermal_steam_vent"}, "models/item/geothermal_steam_vent.json")

    # 2. 10 Stairs
    for stair_id, full_id in (
        ("starship_floor_stairs", "starship_floor"),
        ("starship_wall_stairs", "starship_wall"),
        ("starship_hull_stairs", "starship_hull"),
        ("permafrost_stairs", "permafrost_rock"),
        ("permafrost_bricks_stairs", "permafrost_bricks"),
        ("outpost_roof_stairs", "outpost_roof_plating"),
        ("geothermal_basalt_stairs", "geothermal_basalt"),
        ("ruin_obsidian_stairs", "ruin_obsidian_block"),
        ("ruin_platinum_stairs", "ruin_platinum_trim"),
        ("ruin_ark_stairs", "ruin_ark_hull"),
    ):
        gen_stair_blockstate(stair_id, full_id)

    # 3. 13 Slabs
    for slab_id, tex_id, double_id in (
        ("starship_floor_slab", "starship_floor", None),
        ("starship_wall_slab", "starship_wall", None),
        ("starship_hull_slab", "starship_hull", None),
        ("starship_white_slab", "starship_white_panel", None),
        ("permafrost_slab", "permafrost_rock", None),
        ("permafrost_bricks_slab", "permafrost_bricks", None),
        ("snow_packed_slab", "glacier_snow", None),
        ("outpost_insulated_slab", "outpost_insulated_wall", None),
        ("geothermal_basalt_slab", "geothermal_basalt", None),
        ("ruin_obsidian_slab", "ruin_obsidian_block", None),
        ("ruin_platinum_slab", "ruin_platinum_trim", None),
        ("ruin_hardlight_bridge", "ruin_hardlight_bridge", "ruin_hardlight_bridge_double"),
        ("ruin_ark_slab", "ruin_ark_hull", None),
    ):
        gen_slab_blockstate(slab_id, tex_id, double_id)

    # 4. 3 Pillars
    for pid in ("starship_dark_rib", "mine_steel_beam", "ruin_pillar"):
        gen_pillar_blockstate(pid)

    # 5. Railings, panes, door & vent hatch
    gen_railing_and_panes()
    gen_door_and_hatch()

    # 6. Rotatable 3D small sculpted voxel props (facing=N/E/S/W)
    rotatable_props = {
        "cyan_light_strip": {
            "textures": {"particle": "picking_hope_core:block/cyan_light_strip", "t": "picking_hope_core:block/cyan_light_strip", "d": "picking_hope_core:block/prop_metal_dark"},
            "elements": [
                make_cube([1, 6, 15], [15, 10, 16], {"all": "d"}),
                make_cube([2, 7, 14], [14, 9, 15.2], {"all": "t"}),
            ],
        },
        "amber_light_strip": {
            "textures": {"particle": "picking_hope_core:block/amber_light_strip", "t": "picking_hope_core:block/amber_light_strip", "d": "picking_hope_core:block/prop_metal_dark"},
            "elements": [
                make_cube([1, 6, 15], [15, 10, 16], {"all": "d"}),
                make_cube([2, 7, 14], [14, 9, 15.2], {"all": "t"}),
            ],
        },
        "ruin_ancient_stele": {
            "textures": {"particle": "picking_hope_core:block/ruin_carved_glyph", "obs": "picking_hope_core:block/ruin_obsidian_block", "glyph": "picking_hope_core:block/ruin_carved_glyph", "gold": "picking_hope_core:block/ruin_circuit_gold"},
            "elements": [
                make_cube([1, 0, 4], [15, 3, 12], {"all": "obs"}),
                make_cube([3, 3, 6], [13, 15, 10], {"north": "glyph", "south": "glyph", "east": "obs", "west": "obs", "up": "gold", "down": "obs"}),
                make_cube([5, 15, 6.5], [11, 16, 9.5], {"all": "gold"}),
            ],
        },
        "prop_monitor_desk": {
            "textures": {"particle": "picking_hope_core:block/prop_metal_dark", "d": "picking_hope_core:block/prop_metal_dark", "l": "picking_hope_core:block/prop_metal_light", "s": "picking_hope_core:block/prop_screen_glow"},
            "elements": [
                make_cube([1, 0, 3], [3, 8, 13], {"all": "d"}),
                make_cube([13, 0, 3], [15, 8, 13], {"all": "d"}),
                make_cube([1, 8, 2], [15, 9.5, 14], {"all": "l"}),
                make_cube([6, 9.5, 11.5], [10, 11.5, 13], {"all": "d"}),
                make_cube([2, 10.5, 11], [7.5, 15.5, 12.5], {"north": "s", "south": "d", "east": "d", "west": "d", "up": "d", "down": "d"}),
                make_cube([8.5, 10.5, 11], [14, 15.5, 12.5], {"north": "s", "south": "d", "east": "d", "west": "d", "up": "d", "down": "d"}),
            ],
        },
        "prop_wall_screen": {
            "textures": {"particle": "picking_hope_core:block/prop_screen_glow", "d": "picking_hope_core:block/prop_metal_dark", "s": "picking_hope_core:block/prop_screen_glow"},
            "elements": [
                make_cube([1, 2, 14.5], [15, 14, 16], {"all": "d"}),
                make_cube([2, 3, 13.8], [14, 13, 14.6], {"north": "s", "all": "d"}),
            ],
        },
        "prop_server_rack": {
            "textures": {"particle": "picking_hope_core:block/prop_server_front", "d": "picking_hope_core:block/prop_metal_dark", "f": "picking_hope_core:block/prop_server_front"},
            "elements": [
                make_cube([2, 0, 2], [14, 16, 14], {"north": "f", "south": "d", "east": "d", "west": "d", "up": "d", "down": "d"}),
            ],
        },
        "prop_cable_pipe": {
            "textures": {"particle": "picking_hope_core:block/prop_amber_trim", "t": "picking_hope_core:block/prop_amber_trim", "d": "picking_hope_core:block/prop_metal_dark"},
            "elements": [
                make_cube([0, 11, 5], [16, 14, 8], {"all": "d"}),
                make_cube([0, 12, 9], [16, 14.5, 11.5], {"all": "t"}),
                make_cube([3, 10, 4], [5, 15.5, 12.5], {"all": "t"}),
                make_cube([11, 10, 4], [13, 15.5, 12.5], {"all": "t"}),
            ],
        },
        "prop_vent_duct": {
            "textures": {"particle": "picking_hope_core:block/starship_vent_hatch", "d": "picking_hope_core:block/prop_metal_light", "v": "picking_hope_core:block/starship_vent_hatch"},
            "elements": [
                make_cube([2, 9, 0], [14, 16, 16], {"north": "v", "south": "v", "all": "d"}),
                make_cube([1, 8.5, 2], [15, 16, 3.5], {"all": "d"}),
                make_cube([1, 8.5, 12.5], [15, 16, 14], {"all": "d"}),
            ],
        },
        "prop_medical_bed": {
            "textures": {"particle": "picking_hope_core:block/prop_bed_fabric", "d": "picking_hope_core:block/prop_metal_light", "b": "picking_hope_core:block/prop_bed_fabric"},
            "elements": [
                make_cube([2, 0, 1], [3.5, 4, 2.5], {"all": "d"}),
                make_cube([12.5, 0, 1], [14, 4, 2.5], {"all": "d"}),
                make_cube([2, 0, 13.5], [3.5, 4, 15], {"all": "d"}),
                make_cube([12.5, 0, 13.5], [14, 4, 15], {"all": "d"}),
                make_cube([1, 4, 0], [15, 5.5, 16], {"all": "d"}),
                make_cube([1.5, 5.5, 0.5], [14.5, 7.8, 15.5], {"all": "b"}),
                make_cube([3, 7.8, 11], [13, 9.2, 15], {"all": "d"}),
            ],
        },
        "prop_med_cabinet": {
            "textures": {"particle": "picking_hope_core:block/starship_white_panel", "w": "picking_hope_core:block/starship_white_panel", "g": "picking_hope_core:block/cyan_light_strip"},
            "elements": [
                make_cube([2, 0, 6], [14, 16, 15.5], {"north": "w", "south": "w", "east": "w", "west": "w", "up": "w", "down": "w"}),
                make_cube([3, 7, 5.6], [13, 14, 6.2], {"all": "g"}),
            ],
        },
        "prop_storage_rack": {
            "textures": {"particle": "picking_hope_core:block/prop_metal_dark", "d": "picking_hope_core:block/prop_metal_dark", "l": "picking_hope_core:block/prop_metal_light", "c": "picking_hope_core:block/supply_crate_side"},
            "elements": [
                make_cube([1, 0, 3], [2.5, 16, 4.5], {"all": "d"}),
                make_cube([13.5, 0, 3], [15, 16, 4.5], {"all": "d"}),
                make_cube([1, 0, 11.5], [2.5, 16, 13], {"all": "d"}),
                make_cube([13.5, 0, 11.5], [15, 16, 13], {"all": "d"}),
                make_cube([1.5, 2, 3.5], [14.5, 3, 12.5], {"all": "l"}),
                make_cube([1.5, 9, 3.5], [14.5, 10, 12.5], {"all": "l"}),
                make_cube([3, 3, 4.5], [7, 7, 11], {"all": "c"}),
                make_cube([9, 10, 5], [13, 14.5, 10.5], {"all": "c"}),
            ],
        },
        "prop_lab_desk": {
            "textures": {"particle": "picking_hope_core:block/prop_metal_light", "l": "picking_hope_core:block/prop_metal_light", "d": "picking_hope_core:block/prop_metal_dark", "s": "picking_hope_core:block/cyan_light_strip"},
            "elements": [
                make_cube([1, 0, 3], [3, 8.5, 13], {"all": "d"}),
                make_cube([13, 0, 3], [15, 8.5, 13], {"all": "d"}),
                make_cube([0.5, 8.5, 2], [15.5, 10, 14], {"all": "l"}),
                make_cube([3, 10, 5], [6, 13.5, 8], {"all": "s"}),
                make_cube([9, 10, 6], [13, 11.5, 10], {"all": "d"}),
            ],
        },
        "prop_crew_chair": {
            "textures": {"particle": "picking_hope_core:block/prop_metal_dark", "d": "picking_hope_core:block/prop_metal_dark", "c": "picking_hope_core:block/prop_bed_fabric"},
            "elements": [
                make_cube([4, 0, 7], [12, 1.5, 9], {"all": "d"}),
                make_cube([7, 0, 4], [9, 1.5, 12], {"all": "d"}),
                make_cube([7, 1.5, 7], [9, 6, 9], {"all": "d"}),
                make_cube([3.5, 6, 3.5], [12.5, 7.8, 12.5], {"all": "c"}),
                make_cube([4, 7.8, 10.5], [12, 15, 12.5], {"all": "c"}),
            ],
        },
        "prop_floor_lamp": {
            "textures": {"particle": "picking_hope_core:block/cyan_light_strip", "d": "picking_hope_core:block/prop_metal_dark", "t": "picking_hope_core:block/cyan_light_strip"},
            "elements": [
                make_cube([5, 0, 5], [11, 2, 11], {"all": "d"}),
                make_cube([7, 2, 7], [9, 13, 9], {"all": "d"}),
                make_cube([6, 12.5, 5], [10, 16, 10], {"all": "t"}),
            ],
        },
        "prop_breaker_box": {
            "textures": {"particle": "picking_hope_core:block/prop_amber_trim", "d": "picking_hope_core:block/prop_metal_dark", "a": "picking_hope_core:block/prop_amber_trim", "g": "picking_hope_core:block/generator_coil"},
            "elements": [
                make_cube([3, 2, 12], [13, 14, 16], {"north": "a", "all": "d"}),
                make_cube([5, 8, 11], [8, 11, 12], {"all": "g"}),
                make_cube([10, 4, 11], [11.5, 9, 12], {"all": "a"}),
            ],
        },
        "prop_blackbox_recorder": {
            "textures": {"particle": "picking_hope_core:block/prop_blackbox_tex", "b": "picking_hope_core:block/prop_blackbox_tex", "d": "picking_hope_core:block/prop_metal_dark"},
            "elements": [
                make_cube([3, 0, 3], [13, 7, 13], {"all": "b"}),
                make_cube([6, 7, 6], [10, 8.5, 10], {"all": "d"}),
            ],
        },
        "prop_ruin_relay": {
            "textures": {"particle": "picking_hope_core:block/ruin_circuit_teal", "obs": "picking_hope_core:block/ruin_obsidian_block", "c": "picking_hope_core:block/ruin_circuit_teal"},
            "elements": [
                make_cube([4, 0, 4], [12, 2.5, 12], {"all": "obs"}),
                make_cube([6.5, 2.5, 6.5], [9.5, 9, 9.5], {"all": "obs"}),
                make_cube([5, 9, 5], [11, 14.5, 11], {"all": "c"}),
            ],
        },
    }

    for pid, spec in rotatable_props.items():
        save_json({
            "variants": {
                "facing=north": {"model": f"picking_hope_core:block/{pid}"},
                "facing=east": {"model": f"picking_hope_core:block/{pid}", "y": 90},
                "facing=south": {"model": f"picking_hope_core:block/{pid}", "y": 180},
                "facing=west": {"model": f"picking_hope_core:block/{pid}", "y": 270},
            }
        }, f"blockstates/{pid}.json")
        save_json({
            "parent": "minecraft:block/block",
            "textures": spec["textures"],
            "elements": spec["elements"],
        }, f"models/block/{pid}.json")
        save_json({"parent": f"picking_hope_core:block/{pid}"}, f"models/item/{pid}.json")

    # 7. Non-rotatable sculpted 3D VoxelPropBlocks
    static_props = {
        "prop_cargo_barrel": {
            "textures": {"particle": "picking_hope_core:block/prop_barrel_tex", "b": "picking_hope_core:block/prop_barrel_tex", "d": "picking_hope_core:block/prop_metal_dark"},
            "elements": [
                make_cube([3, 0, 3], [13, 14.5, 13], {"up": "d", "down": "d", "all": "b"}),
                make_cube([2.5, 4, 2.5], [13.5, 5.5, 13.5], {"all": "d"}),
                make_cube([2.5, 10, 2.5], [13.5, 11.5, 13.5], {"all": "d"}),
            ],
        },
        "prop_ice_crystal": {
            "textures": {"particle": "picking_hope_core:block/prop_ice_crystal", "c": "picking_hope_core:block/prop_ice_crystal", "r": "picking_hope_core:block/permafrost_rock"},
            "elements": [
                make_cube([4, 0, 4], [12, 2, 12], {"all": "r"}),
                make_cube([6, 2, 6], [10, 14, 10], {"all": "c"}),
                make_cube([3.5, 2, 4.5], [6.5, 9, 7.5], {"all": "c"}),
                make_cube([9.5, 2, 8], [12.5, 10.5, 11], {"all": "c"}),
            ],
        },
        "ruin_floating_prism": {
            "textures": {"particle": "picking_hope_core:block/ruin_energy_core", "o": "picking_hope_core:block/ruin_obsidian_block", "g": "picking_hope_core:block/ruin_circuit_gold", "t": "picking_hope_core:block/ruin_circuit_teal"},
            "elements": [
                make_cube([4, 0, 4], [12, 2, 12], {"all": "o"}),
                make_cube([5.5, 5, 5.5], [10.5, 12, 10.5], {"all": "t"}),
                make_cube([4.5, 7.5, 4.5], [11.5, 9.5, 11.5], {"all": "g"}),
                make_cube([6.5, 12, 6.5], [9.5, 15, 9.5], {"all": "g"}),
            ],
        },
    }
    for pid, spec in static_props.items():
        save_json({"variants": {"": {"model": f"picking_hope_core:block/{pid}"}}}, f"blockstates/{pid}.json")
        save_json({
            "parent": "minecraft:block/block",
            "textures": spec["textures"],
            "elements": spec["elements"],
        }, f"models/block/{pid}.json")
        save_json({"parent": f"picking_hope_core:block/{pid}"}, f"models/item/{pid}.json")


# ============================================================
# 3. INCREMENTAL MERGE INTO LANG FILES
# ============================================================
def update_lang():
    zh_path = os.path.join(ASSETS, "lang", "zh_cn.json")
    en_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(zh_path, "r", encoding="utf-8") as f:
        zh = json.load(f)
    with open(en_path, "r", encoding="utf-8") as f:
        en = json.load(f)

    # Creative tabs & new keybind
    zh["itemGroup.picking_hope_starship"] = "重拾希望 · 星舰建筑"
    zh["itemGroup.picking_hope_planet"] = "重拾希望 · 冰原矿区地热"
    zh["itemGroup.picking_hope_ruins"] = "重拾希望 · 外星遗迹方舟"
    zh["itemGroup.picking_hope_props"] = "重拾希望 · 家具与小道具"
    zh["key.picking_hope.builder_map_m"] = "造景全图蓝图与传送 (M)"

    en["itemGroup.picking_hope_starship"] = "Hope: Starship Blocks"
    en["itemGroup.picking_hope_planet"] = "Hope: Cryo Planet & Mine"
    en["itemGroup.picking_hope_ruins"] = "Hope: Alien Ruins & Ark"
    en["itemGroup.picking_hope_props"] = "Hope: Furniture & Props"
    en["key.picking_hope.builder_map_m"] = "Builder Region Blueprint (M)"

    names = {
        "starship_floor_stairs": ("星舰合金地板楼梯", "Starship Floor Stairs"),
        "starship_floor_slab": ("星舰合金地板台阶", "Starship Floor Slab"),
        "starship_wall_stairs": ("星舰加厚舱壁楼梯", "Starship Wall Stairs"),
        "starship_wall_slab": ("星舰加厚舱壁台阶", "Starship Wall Slab"),
        "starship_hull_stairs": ("星舰重装甲外壳楼梯", "Starship Hull Stairs"),
        "starship_hull_slab": ("星舰重装甲外壳台阶", "Starship Hull Slab"),
        "starship_grate_floor": ("星舰镂空金属格栅地板", "Starship Metal Grate Floor"),
        "starship_white_panel": ("洁白舱室壁板", "Clean Cabin White Panel"),
        "starship_white_slab": ("洁白舱室台阶", "Clean Cabin White Slab"),
        "starship_dark_rib": ("深灰舰体结构钢柱", "Dark Starship Structural Rib"),
        "starship_ceiling_tile": ("星舰通风吊顶板", "Starship Ceiling Tile"),
        "starship_damaged_hull": ("损毁焦黑舰体装甲", "Damaged Scorched Hull"),
        "starship_railing": ("金属安全护栏", "Starship Safety Railing"),
        "starship_viewport_pane": ("强化舷窗玻璃板", "Starship Viewport Pane"),
        "starship_airlock_door": ("气密防爆舱门", "Starship Airlock Door"),
        "starship_vent_hatch": ("金属通风活板门", "Metal Vent Hatch"),
        "cyan_light_strip": ("青蓝墙壁灯带", "Cyan Wall Light Strip"),
        "amber_light_strip": ("琥珀警示灯带", "Amber Warning Light Strip"),

        "permafrost_stairs": ("冻土岩层楼梯", "Permafrost Rock Stairs"),
        "permafrost_slab": ("冻土岩层台阶", "Permafrost Rock Slab"),
        "permafrost_bricks": ("冻土条纹石砖", "Permafrost Bricks"),
        "permafrost_bricks_stairs": ("冻土石砖楼梯", "Permafrost Brick Stairs"),
        "permafrost_bricks_slab": ("冻土石砖台阶", "Permafrost Brick Slab"),
        "glacier_ice_block": ("冰川透蓝坚冰", "Translucent Glacier Ice"),
        "snow_packed_slab": ("极地压实雪台阶", "Packed Glacier Snow Slab"),
        "outpost_insulated_wall": ("科考站白橙保温墙板", "Outpost Insulated Wall"),
        "outpost_insulated_slab": ("科考站保温墙台阶", "Outpost Insulated Slab"),
        "outpost_roof_plating": ("科考站波纹铁屋顶", "Outpost Roof Plating"),
        "outpost_roof_stairs": ("科考站屋顶斜坡楼梯", "Outpost Roof Stairs"),
        "mine_steel_beam": ("矿井工字钢柱", "Mine Steel I-Beam"),
        "mine_steel_truss": ("矿井镂空铁桁架", "Mine Steel Truss"),
        "mine_rusted_plate": ("矿区锈蚀工业钢板", "Rusted Industrial Plate"),
        "mine_concrete_floor": ("矿区硬化混凝土地板", "Mine Concrete Floor"),
        "geothermal_basalt": ("地热裂谷黑玄武岩", "Geothermal Dark Basalt"),
        "geothermal_basalt_stairs": ("地热玄武岩楼梯", "Geothermal Basalt Stairs"),
        "geothermal_basalt_slab": ("地热玄武岩台阶", "Geothermal Basalt Slab"),
        "geothermal_warm_stone": ("温热地热裂隙岩", "Geothermal Warm Stone"),
        "geothermal_steam_vent": ("地热蒸汽排放口", "Geothermal Steam Vent"),
        "geothermal_glow_moss": ("地下荧光苔藓岩", "Geothermal Glow Moss Stone"),

        "ruin_obsidian_block": ("遗迹平滑黑曜石", "Ruin Smooth Obsidian"),
        "ruin_obsidian_stairs": ("遗迹黑曜石楼梯", "Ruin Obsidian Stairs"),
        "ruin_obsidian_slab": ("遗迹黑曜石台阶", "Ruin Obsidian Slab"),
        "ruin_platinum_trim": ("遗迹白金金属饰板", "Ruin Platinum Trim"),
        "ruin_platinum_stairs": ("遗迹白金楼梯", "Ruin Platinum Stairs"),
        "ruin_platinum_slab": ("遗迹白金台阶", "Ruin Platinum Slab"),
        "ruin_carved_glyph": ("远古铭文浮雕砖", "Ancient Carved Glyph"),
        "ruin_circuit_teal": ("青光外星电路方块", "Teal Alien Circuit Block"),
        "ruin_circuit_gold": ("金光外星电路方块", "Gold Alien Circuit Block"),
        "ruin_pillar": ("远古遗迹立柱", "Ancient Ruin Pillar"),
        "ruin_floor_tile": ("遗迹圣殿大地砖", "Ruin Sanctuary Floor Tile"),
        "ruin_energy_core": ("遗迹多面体能量核心", "Ruin Faceted Energy Core"),
        "ruin_hardlight_bridge": ("半透明硬光桥板", "Translucent Hardlight Bridge"),
        "ruin_hardlight_barrier": ("遗迹硬光能量屏障", "Ruin Hardlight Barrier"),
        "ruin_ark_hull": ("休眠方舟暗金外壳", "Ark Dark-Gold Hull"),
        "ruin_ark_stairs": ("方舟外壳楼梯", "Ark Hull Stairs"),
        "ruin_ark_slab": ("方舟外壳台阶", "Ark Hull Slab"),
        "ruin_docking_beacon": ("方舟停泊港导航信标", "Ark Docking Beacon"),
        "ruin_ancient_stele": ("遗迹全息石碑", "Ruin Ancient Stele"),
        "ruin_floating_prism": ("悬浮能量棱柱", "Floating Resonance Prism"),

        "prop_monitor_desk": ("双屏战术办公桌", "Dual-Screen Desk Console"),
        "prop_wall_screen": ("挂墙电子监控屏幕", "Wall Telemetry Screen"),
        "prop_server_rack": ("闪烁数据服务器机柜", "Data Server Rack"),
        "prop_cable_pipe": ("吊顶工业粗电缆管", "Overhead Cable Conduit"),
        "prop_vent_duct": ("金属通风管道箱", "Ventilation Duct Box"),
        "prop_medical_bed": ("医务室恢复病床", "Medbay Recovery Bed"),
        "prop_med_cabinet": ("医药存储柜", "Medical Supply Cabinet"),
        "prop_storage_rack": ("工业多层货架", "Industrial Storage Rack"),
        "prop_lab_desk": ("极地科研实验台", "Polar Research Desk"),
        "prop_crew_chair": ("船员旋转座椅", "Crew Swivel Chair"),
        "prop_cargo_barrel": ("钢制物资圆桶", "Steel Cargo Barrel"),
        "prop_floor_lamp": ("极地立式照明工作灯", "Portable Work Floor Lamp"),
        "prop_ice_crystal": ("发光极地冰晶簇", "Glowing Ice Crystal Cluster"),
        "prop_breaker_box": ("配电闸门控制箱", "Breaker Control Box"),
        "prop_blackbox_recorder": ("坠机橙色黑匣子", "Flight Blackbox Recorder"),
        "prop_ruin_relay": ("外星三脚谐振中继柱", "Alien Relay Tripod"),
    }

    for bid, (zname, ename) in names.items():
        zh[f"block.picking_hope_core.{bid}"] = zname
        zh[f"item.picking_hope_core.{bid}"] = zname
        en[f"block.picking_hope_core.{bid}"] = ename
        en[f"item.picking_hope_core.{bid}"] = ename

    save_json(zh, "lang/zh_cn.json")
    save_json(en, "lang/en_us.json")
    print(f"Updated zh_cn ({len(zh)} keys) and en_us ({len(en)} keys).")


# ============================================================
# 4. FULL INTEGRITY VERIFIER AGAINST ModBlocks.java
# ============================================================
def verify_all_blocks():
    mod_blocks_path = os.path.join("mod", "src", "main", "java", "com", "resilienthope", "core", "init", "ModBlocks.java")
    src = open(mod_blocks_path, "r", encoding="utf-8").read()
    block_ids = re.findall(r'BLOCKS\.register\(\s*"([a-z0-9_]+)"', src)
    print(f"Found {len(block_ids)} registered blocks in ModBlocks.java.")

    zh = json.load(open(os.path.join(ASSETS, "lang", "zh_cn.json"), encoding="utf-8"))
    errors = []

    def check_model_ref(model_ref, ctx):
        if model_ref.startswith("minecraft:"):
            return
        if not model_ref.startswith("picking_hope_core:"):
            errors.append(f"{ctx}: unexpected namespace in {model_ref}")
            return
        rel = model_ref.split(":", 1)[1]
        mpath = os.path.join(ASSETS, "models", rel.replace("/", os.sep) + ".json")
        if not os.path.exists(mpath):
            errors.append(f"{ctx}: missing model file {mpath}")
            return
        mdata = json.load(open(mpath, encoding="utf-8"))
        for tk, tv in mdata.get("textures", {}).items():
            if tv.startswith("#") or tv.startswith("minecraft:"):
                continue
            if not tv.startswith("picking_hope_core:"):
                errors.append(f"{mpath}: bad texture ns {tv}")
                continue
            trel = tv.split(":", 1)[1]
            tpath = os.path.join(ASSETS, "textures", trel.replace("/", os.sep) + ".png")
            if not os.path.exists(tpath):
                errors.append(f"{mpath}: missing texture PNG {tpath}")

    for bid in block_ids:
        if f"block.picking_hope_core.{bid}" not in zh:
            errors.append(f"Missing zh_cn key for {bid}")
        bs_path = os.path.join(ASSETS, "blockstates", f"{bid}.json")
        if not os.path.exists(bs_path):
            errors.append(f"Missing blockstate: {bs_path}")
        else:
            bs = json.load(open(bs_path, encoding="utf-8"))
            if "variants" in bs:
                for vname, val in bs["variants"].items():
                    items = val if isinstance(val, list) else [val]
                    for it in items:
                        check_model_ref(it["model"], f"blockstate {bid}[{vname}]")
            if "multipart" in bs:
                for idx, part in enumerate(bs["multipart"]):
                    ap = part["apply"]
                    items = ap if isinstance(ap, list) else [ap]
                    for it in items:
                        check_model_ref(it["model"], f"blockstate {bid} multipart[{idx}]")

        item_path = os.path.join(ASSETS, "models", "item", f"{bid}.json")
        if not os.path.exists(item_path):
            errors.append(f"Missing item model: {item_path}")
        else:
            im = json.load(open(item_path, encoding="utf-8"))
            if "parent" in im:
                check_model_ref(im["parent"], f"item model {bid}")
            for tk, tv in im.get("textures", {}).items():
                if tv.startswith("picking_hope_core:"):
                    trel = tv.split(":", 1)[1]
                    tpath = os.path.join(ASSETS, "textures", trel.replace("/", os.sep) + ".png")
                    if not os.path.exists(tpath):
                        errors.append(f"{item_path}: missing texture PNG {tpath}")

    if errors:
        print("INTEGRITY CHECK FAILED:")
        for e in errors:
            print(" -", e)
        raise SystemExit(1)
    print(f"ALL {len(block_ids)} blocks passed 100% blockstate, model, texture & lang verification!")


if __name__ == "__main__":
    gen_textures()
    gen_new_models_and_states()
    update_lang()
    verify_all_blocks()
