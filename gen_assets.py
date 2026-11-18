import json
import math
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


def save_json(obj, rel_path, base=ASSETS):
    full = os.path.join(base, rel_path)
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


def outline_silhouette(img, outline_rgba=(14, 18, 26, 255)):
    """Adds a crisp 1px dark outline around non-transparent pixels for readable item icons."""
    w, h = img.size
    src = img.copy()
    for x in range(w):
        for y in range(h):
            if src.getpixel((x, y))[3] == 0:
                has_neighbor = False
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and src.getpixel((nx, ny))[3] > 20:
                        has_neighbor = True
                        break
                if has_neighbor:
                    img.putpixel((x, y), outline_rgba)


def add_line(img, x0, y0, x1, y1, rgba, width=1):
    d = ImageDraw.Draw(img)
    d.line([(x0, y0), (x1, y1)], fill=rgba, width=width)


def add_circle(img, cx, cy, r, rgba, outline=None):
    d = ImageDraw.Draw(img)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=rgba, outline=outline)


def add_poly(img, pts, rgba, outline=None):
    d = ImageDraw.Draw(img)
    d.polygon(pts, fill=rgba, outline=outline)


# ============================================================
# 1. CUSTOM SCI-FI & PLANETARY BLOCK TEXTURES (32x32)
# ============================================================
def gen_block_textures():
    # 1. starship_floor: dark gunmetal plating with beveled seams & cyan corner bolts
    img = Image.new("RGBA", (32, 32), (38, 46, 58, 255))
    rect(img, 1, 1, 30, 30, (48, 58, 72, 255))
    rect(img, 2, 2, 14, 14, (56, 67, 82, 255))
    rect(img, 17, 2, 29, 14, (54, 65, 80, 255))
    rect(img, 2, 17, 14, 29, (54, 65, 80, 255))
    rect(img, 17, 17, 29, 29, (56, 67, 82, 255))
    # Anti-slip ridges
    for bx in (2, 17):
        for by in (2, 17):
            for i in range(4, 11, 3):
                rect(img, bx + i, by + 3, bx + i + 1, by + 9, (40, 48, 60, 255))
            px(img, bx + 1, by + 1, (90, 185, 225, 255))
    rect(img, 0, 15, 31, 16, (26, 32, 42, 255))
    rect(img, 15, 0, 16, 31, (26, 32, 42, 255))
    save_img(img, "textures/block/starship_floor.png")

    # 2. starship_wall: brushed bulkhead panel with horizontal rib and status strip
    img = Image.new("RGBA", (32, 32), (42, 50, 64, 255))
    rect(img, 1, 1, 30, 30, (64, 76, 94, 255))
    rect(img, 3, 3, 28, 12, (78, 92, 112, 255))
    rect(img, 3, 18, 28, 28, (56, 68, 84, 255))
    rect(img, 2, 14, 29, 16, (28, 35, 45, 255))
    rect(img, 4, 14, 14, 15, (56, 189, 248, 255))
    for rx in (4, 27):
        for ry in (4, 27):
            px(img, rx, ry, (135, 150, 170, 255))
    save_img(img, "textures/block/starship_wall.png")

    # 3. starship_hull: heavy armored carbon-titanium hull
    img = Image.new("RGBA", (32, 32), (24, 29, 38, 255))
    rect(img, 2, 2, 29, 29, (34, 41, 53, 255))
    rect(img, 4, 4, 27, 27, (28, 34, 44, 255))
    for i in range(4, 28, 6):
        rect(img, i, 6, i + 2, 25, (42, 50, 64, 255))
    rect(img, 2, 15, 29, 16, (18, 22, 29, 255))
    save_img(img, "textures/block/starship_hull.png")

    # 4. hazard_stripe_block: industrial amber & charcoal diagonal stripes
    img = Image.new("RGBA", (32, 32), (32, 36, 44, 255))
    for x in range(32):
        for y in range(32):
            band = ((x + y) // 6) % 2
            if x < 2 or x > 29 or y < 2 or y > 29:
                px(img, x, y, (46, 52, 62, 255))
            elif band == 0:
                px(img, x, y, (235, 175, 40, 255) if (x + y) % 6 != 0 else (255, 210, 85, 255))
            else:
                px(img, x, y, (32, 36, 44, 255))
    save_img(img, "textures/block/hazard_stripe_block.png")

    # 5. starship_light: crisp LED cabin luminaire
    img = Image.new("RGBA", (32, 32), (36, 44, 56, 255))
    rect(img, 2, 2, 29, 29, (68, 82, 102, 255))
    rect(img, 4, 4, 27, 27, (145, 225, 250, 255))
    rect(img, 6, 6, 25, 25, (220, 248, 255, 255))
    rect(img, 4, 15, 27, 16, (68, 82, 102, 255))
    save_img(img, "textures/block/starship_light.png")

    # 6. alarm_beacon: emergency pulsing amber-crimson light
    img = Image.new("RGBA", (32, 32), (34, 38, 48, 255))
    rect(img, 3, 3, 28, 28, (60, 42, 46, 255))
    rect(img, 6, 6, 25, 25, (225, 55, 55, 255))
    rect(img, 9, 9, 22, 22, (255, 130, 80, 255))
    rect(img, 12, 12, 19, 19, (255, 220, 160, 255))
    save_img(img, "textures/block/alarm_beacon.png")

    # 7. viewport_glass: semi-transparent reinforced starship window
    img = Image.new("RGBA", (32, 32), (105, 195, 235, 85))
    rect(img, 0, 0, 31, 2, (52, 64, 80, 255))
    rect(img, 0, 29, 31, 31, (52, 64, 80, 255))
    rect(img, 0, 0, 2, 31, (52, 64, 80, 255))
    rect(img, 29, 0, 31, 31, (52, 64, 80, 255))
    for i in range(6, 16):
        px(img, i, 18 - i, (210, 245, 255, 165))
        px(img, i + 10, 24 - i, (210, 245, 255, 135))
    save_img(img, "textures/block/viewport_glass.png")

    # 8. permafrost_rock: fractured dark cryo-basalt stone
    img = Image.new("RGBA", (32, 32), (48, 58, 72, 255))
    for x in range(32):
        for y in range(32):
            v = ((x * 17 + y * 31) ^ (x * y)) % 13
            if v < 3:
                px(img, x, y, (36, 45, 58, 255))
            elif v > 10:
                px(img, x, y, (72, 88, 108, 255))
    rect(img, 4, 9, 18, 10, (28, 36, 48, 255))
    rect(img, 14, 21, 28, 22, (28, 36, 48, 255))
    save_img(img, "textures/block/permafrost_rock.png")

    # 9. glacier_snow: crisp blue-white packed polar frost
    img = Image.new("RGBA", (32, 32), (218, 234, 246, 255))
    for x in range(32):
        for y in range(32):
            v = (x * 13 + y * 23) % 11
            if v < 2:
                px(img, x, y, (192, 216, 236, 255))
            elif v == 10:
                px(img, x, y, (242, 250, 255, 255))
    save_img(img, "textures/block/glacier_snow.png")

    # 10. frozen_ore_vein: permafrost stone with glowing cyan energy crystal veins
    img = Image.open(os.path.join(ASSETS, "textures/block/permafrost_rock.png")).copy()
    for cx, cy in ((8, 8), (22, 12), (12, 23), (24, 25)):
        rect(img, cx - 2, cy - 2, cx + 2, cy + 2, (35, 165, 225, 255))
        rect(img, cx - 1, cy - 1, cx + 1, cy + 1, (140, 235, 255, 255))
        px(img, cx, cy, (235, 255, 255, 255))
    save_img(img, "textures/block/frozen_ore_vein.png")

    # === 3D Voxel Prop Specific Textures (all pure custom pixel art) ===
    # Waypoint
    wp_base = Image.new("RGBA", (32, 32), (42, 52, 66, 255))
    rect(wp_base, 2, 2, 29, 29, (58, 72, 92, 255))
    rect(wp_base, 6, 13, 25, 18, (45, 175, 235, 255))
    save_img(wp_base, "textures/block/waypoint_base.png")

    wp_core = Image.new("RGBA", (32, 32), (35, 155, 235, 255))
    rect(wp_core, 3, 3, 28, 28, (90, 215, 255, 255))
    rect(wp_core, 8, 8, 23, 23, (210, 248, 255, 255))
    save_img(wp_core, "textures/block/waypoint_core.png")

    # Engineering Terminal
    tb = Image.new("RGBA", (32, 32), (46, 54, 66, 255))
    rect(tb, 2, 2, 29, 29, (64, 76, 92, 255))
    rect(tb, 4, 22, 27, 26, (235, 175, 45, 255))
    save_img(tb, "textures/block/terminal_body.png")

    ts = Image.new("RGBA", (32, 32), (16, 28, 42, 255))
    rect(ts, 2, 2, 29, 29, (22, 42, 64, 255))
    rect(ts, 4, 5, 27, 8, (56, 189, 248, 255))
    rect(ts, 4, 11, 18, 13, (74, 222, 128, 255))
    rect(ts, 4, 16, 24, 18, (251, 191, 36, 255))
    rect(ts, 4, 21, 14, 23, (125, 211, 252, 255))
    save_img(ts, "textures/block/terminal_screen.png")

    # Auxiliary Energy Core / Generator
    gf = Image.new("RGBA", (32, 32), (38, 44, 54, 255))
    rect(gf, 2, 2, 29, 29, (56, 64, 78, 255))
    for i in range(4, 28, 6):
        rect(gf, i, 4, i + 2, 27, (225, 145, 35, 255))
    save_img(gf, "textures/block/generator_frame.png")

    gc = Image.new("RGBA", (32, 32), (45, 210, 145, 255))
    rect(gc, 4, 4, 27, 27, (110, 245, 190, 255))
    rect(gc, 9, 9, 22, 22, (225, 255, 240, 255))
    save_img(gc, "textures/block/generator_coil.png")

    # Orbital Drop Pod
    dh = Image.new("RGBA", (32, 32), (55, 62, 74, 255))
    rect(dh, 2, 2, 29, 29, (74, 84, 98, 255))
    rect(dh, 2, 22, 29, 27, (215, 110, 35, 255))
    save_img(dh, "textures/block/droppod_hull.png")

    dg = Image.new("RGBA", (32, 32), (45, 135, 185, 255))
    rect(dg, 4, 4, 27, 27, (120, 215, 250, 255))
    add_line(dg, 6, 22, 24, 6, (230, 250, 255, 255), 2)
    save_img(dg, "textures/block/droppod_glass.png")

    # Comms Console
    cb = Image.new("RGBA", (32, 32), (32, 40, 54, 255))
    rect(cb, 2, 2, 29, 29, (48, 60, 82, 255))
    rect(cb, 6, 12, 25, 16, (251, 191, 36, 255))
    save_img(cb, "textures/block/comms_base.png")

    cs = Image.new("RGBA", (32, 32), (25, 38, 62, 255))
    rect(cs, 4, 4, 27, 27, (251, 191, 36, 255))
    rect(cs, 8, 8, 23, 23, (56, 189, 248, 255))
    rect(cs, 12, 12, 19, 19, (255, 255, 255, 255))
    save_img(cs, "textures/block/comms_screen.png")

    # Armored Supply Crate
    scs = Image.new("RGBA", (32, 32), (48, 58, 72, 255))
    rect(scs, 2, 2, 29, 29, (68, 82, 102, 255))
    rect(scs, 4, 5, 27, 26, (54, 66, 84, 255))
    rect(scs, 12, 8, 19, 15, (251, 191, 36, 255))  # Gold lock clasp
    rect(scs, 14, 10, 17, 13, (56, 189, 248, 255))  # Cyan lock LED
    rect(scs, 3, 20, 28, 23, (235, 165, 40, 255))
    save_img(scs, "textures/block/supply_crate_side.png")

    sct = Image.new("RGBA", (32, 32), (48, 58, 72, 255))
    rect(sct, 2, 2, 29, 29, (74, 88, 108, 255))
    rect(sct, 6, 6, 25, 25, (60, 72, 90, 255))
    rect(sct, 11, 11, 20, 20, (56, 189, 248, 255))
    save_img(sct, "textures/block/supply_crate_top.png")

    # Cryo Pod
    cpb = Image.new("RGBA", (32, 32), (44, 54, 68, 255))
    rect(cpb, 2, 2, 29, 29, (68, 82, 102, 255))
    rect(cpb, 4, 14, 27, 17, (56, 189, 248, 255))
    save_img(cpb, "textures/block/cryo_pod_base.png")

    cpg = Image.new("RGBA", (32, 32), (55, 165, 215, 195))
    rect(cpg, 3, 3, 28, 28, (125, 225, 250, 210))
    add_line(cpg, 6, 25, 25, 6, (230, 252, 255, 240), 2)
    save_img(cpg, "textures/block/cryo_pod_glass.png")

    # Conduit Pillar
    cp = Image.new("RGBA", (32, 32), (42, 50, 62, 255))
    rect(cp, 4, 0, 27, 31, (64, 76, 92, 255))
    rect(cp, 11, 0, 20, 31, (38, 155, 215, 255))
    rect(cp, 14, 0, 17, 31, (145, 235, 255, 255))
    for ry in (4, 15, 26):
        rect(cp, 2, ry, 29, ry + 2, (220, 165, 45, 255))
    save_img(cp, "textures/block/conduit_pipe.png")

    # Holotable
    ht = Image.new("RGBA", (32, 32), (24, 34, 48, 255))
    rect(ht, 3, 3, 28, 28, (18, 52, 82, 255))
    add_circle(ht, 15, 15, 9, (22, 95, 145, 255), (56, 189, 248, 255))
    add_circle(ht, 15, 15, 4, (145, 235, 255, 255))
    save_img(ht, "textures/block/holotable_top.png")

    hb = Image.new("RGBA", (32, 32), (40, 185, 245, 200))
    rect(hb, 6, 6, 25, 25, (145, 235, 255, 220))
    save_img(hb, "textures/block/holotable_beam.png")


# ============================================================
# 2. MULTI-ELEMENT 3D VOXEL BLOCK MODELS & BLOCKSTATES
# ============================================================
def make_cube_element(f, t, faces_tex):
    faces = {}
    for dir_name in ("north", "south", "east", "west", "up", "down"):
        tex_ref = faces_tex.get(dir_name, faces_tex.get("all"))
        if tex_ref:
            faces[dir_name] = {"texture": f"#{tex_ref}", "uv": [0, 0, 16, 16]}
    return {"from": f, "to": t, "faces": faces}


def gen_block_models_and_states():
    # Simple full-cube custom blocks
    simple_blocks = [
        "starship_floor", "starship_wall", "starship_hull", "hazard_stripe_block",
        "starship_light", "alarm_beacon", "viewport_glass", "permafrost_rock",
        "glacier_snow", "frozen_ore_vein",
    ]
    for b in simple_blocks:
        save_json({"variants": {"": {"model": f"picking_hope_core:block/{b}"}}}, f"blockstates/{b}.json")
        save_json({
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"picking_hope_core:block/{b}"},
        }, f"models/block/{b}.json")
        save_json({"parent": f"picking_hope_core:block/{b}"}, f"models/item/{b}.json")

    # 3D Multi-Element Voxel Models
    voxel_defs = {
        "teleport_waypoint": {
            "textures": {
                "particle": "picking_hope_core:block/waypoint_base",
                "base": "picking_hope_core:block/waypoint_base",
                "core": "picking_hope_core:block/waypoint_core",
            },
            "elements": [
                make_cube_element([1, 0, 1], [15, 3, 15], {"all": "base"}),
                make_cube_element([3, 3, 3], [13, 6, 13], {"all": "base"}),
                make_cube_element([5, 6, 5], [11, 14, 11], {"all": "core"}),
                make_cube_element([3, 14, 3], [13, 16, 13], {"all": "base"}),
            ],
        },
        "engineering_terminal": {
            "textures": {
                "particle": "picking_hope_core:block/terminal_body",
                "body": "picking_hope_core:block/terminal_body",
                "screen": "picking_hope_core:block/terminal_screen",
            },
            "elements": [
                make_cube_element([2, 0, 2], [14, 4, 14], {"all": "body"}),
                make_cube_element([4, 4, 5], [12, 10, 12], {"all": "body"}),
                make_cube_element([2, 9, 2], [14, 11, 8], {"all": "body"}),
                make_cube_element([2, 10, 8], [14, 16, 13], {"north": "screen", "south": "body", "east": "body", "west": "body", "up": "body", "down": "body"}),
            ],
        },
        "auxiliary_energy_core": {
            "textures": {
                "particle": "picking_hope_core:block/generator_frame",
                "frame": "picking_hope_core:block/generator_frame",
                "coil": "picking_hope_core:block/generator_coil",
            },
            "elements": [
                make_cube_element([1, 0, 1], [15, 3, 15], {"all": "frame"}),
                make_cube_element([3, 3, 3], [13, 13, 13], {"all": "coil"}),
                make_cube_element([1, 3, 1], [4, 14, 4], {"all": "frame"}),
                make_cube_element([12, 3, 1], [15, 14, 4], {"all": "frame"}),
                make_cube_element([1, 3, 12], [4, 14, 15], {"all": "frame"}),
                make_cube_element([12, 3, 12], [15, 14, 15], {"all": "frame"}),
                make_cube_element([1, 13, 1], [15, 16, 15], {"all": "frame"}),
            ],
        },
        "orbital_drop_pod": {
            "textures": {
                "particle": "picking_hope_core:block/droppod_hull",
                "hull": "picking_hope_core:block/droppod_hull",
                "glass": "picking_hope_core:block/droppod_glass",
            },
            "elements": [
                make_cube_element([1, 0, 1], [15, 3, 15], {"all": "hull"}),
                make_cube_element([2, 3, 2], [14, 12, 14], {"north": "glass", "south": "hull", "east": "hull", "west": "hull", "up": "hull", "down": "hull"}),
                make_cube_element([3, 12, 3], [13, 15, 13], {"all": "hull"}),
                make_cube_element([5, 15, 5], [11, 16, 11], {"all": "glass"}),
            ],
        },
        "precursor_altar": {
            "textures": {
                "particle": "picking_hope_core:block/comms_base",
                "base": "picking_hope_core:block/comms_base",
                "screen": "picking_hope_core:block/comms_screen",
            },
            "elements": [
                make_cube_element([1, 0, 1], [15, 4, 15], {"all": "base"}),
                make_cube_element([3, 4, 3], [13, 10, 13], {"all": "base"}),
                make_cube_element([4, 10, 4], [12, 14, 12], {"all": "screen"}),
                make_cube_element([7, 14, 7], [9, 16, 9], {"all": "screen"}),
            ],
        },
        "supply_crate": {
            "textures": {
                "particle": "picking_hope_core:block/supply_crate_side",
                "side": "picking_hope_core:block/supply_crate_side",
                "top": "picking_hope_core:block/supply_crate_top",
            },
            "elements": [
                make_cube_element([1, 0, 1], [15, 9, 15], {"north": "side", "south": "side", "east": "side", "west": "side", "up": "top", "down": "top"}),
                make_cube_element([2, 9, 2], [14, 13, 14], {"north": "side", "south": "side", "east": "side", "west": "side", "up": "top", "down": "top"}),
                make_cube_element([6, 7, 0], [10, 11, 2], {"all": "top"}),
            ],
        },
        "cryo_sleep_pod": {
            "textures": {
                "particle": "picking_hope_core:block/cryo_pod_base",
                "base": "picking_hope_core:block/cryo_pod_base",
                "glass": "picking_hope_core:block/cryo_pod_glass",
            },
            "elements": [
                make_cube_element([1, 0, 1], [15, 3, 15], {"all": "base"}),
                make_cube_element([2, 3, 2], [14, 13, 6], {"all": "base"}),
                make_cube_element([3, 3, 6], [13, 13, 13], {"all": "glass"}),
                make_cube_element([1, 13, 1], [15, 16, 15], {"all": "base"}),
            ],
        },
        "conduit_pillar": {
            "textures": {
                "particle": "picking_hope_core:block/conduit_pipe",
                "pipe": "picking_hope_core:block/conduit_pipe",
            },
            "elements": [
                make_cube_element([3, 0, 3], [13, 2, 13], {"all": "pipe"}),
                make_cube_element([5, 2, 5], [11, 14, 11], {"all": "pipe"}),
                make_cube_element([3, 14, 3], [13, 16, 13], {"all": "pipe"}),
            ],
        },
        "holotable_prop": {
            "textures": {
                "particle": "picking_hope_core:block/terminal_body",
                "base": "picking_hope_core:block/terminal_body",
                "top": "picking_hope_core:block/holotable_top",
                "beam": "picking_hope_core:block/holotable_beam",
            },
            "elements": [
                make_cube_element([4, 0, 4], [12, 7, 12], {"all": "base"}),
                make_cube_element([1, 7, 1], [15, 10, 15], {"north": "base", "south": "base", "east": "base", "west": "base", "up": "top", "down": "base"}),
                make_cube_element([5, 10, 5], [11, 14, 11], {"all": "beam"}),
            ],
        },
    }

    for name, model in voxel_defs.items():
        save_json({"variants": {"": {"model": f"picking_hope_core:block/{name}"}}}, f"blockstates/{name}.json")
        save_json(model, f"models/block/{name}.json")
        save_json({"parent": f"picking_hope_core:block/{name}"}, f"models/item/{name}.json")


# ============================================================
# 3. HIGH-RES 32x32 ITEM & EQUIPMENT TEXTURES
# ============================================================
def gen_weapons():
    # 1. plasma_cutter: 32x32 Industrial Earth Engineering Blade
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Handle & pommel
    add_line(img, 4, 27, 9, 22, (52, 60, 74, 255), 3)
    px(img, 3, 28, (235, 175, 45, 255))
    # Crossguard
    add_poly(img, [(6, 20), (11, 17), (14, 21), (10, 24)], (215, 155, 35, 255))
    # Heavy steel blade spine + plasma edge
    add_poly(img, [(10, 19), (26, 3), (28, 5), (14, 21)], (205, 218, 232, 255))
    add_poly(img, [(11, 18), (25, 4), (27, 4), (12, 20)], (255, 235, 155, 255))
    add_line(img, 12, 20, 26, 6, (72, 86, 104, 255), 2)
    rect(img, 9, 18, 12, 21, (56, 189, 248, 255))
    outline_silhouette(img)
    save_img(img, "textures/item/plasma_cutter.png")

    # 2. cryo_sawblade: 32x32 Industrial Fire Thermal Torch Saw
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    add_line(img, 4, 27, 10, 21, (45, 50, 62, 255), 3)
    add_poly(img, [(7, 18), (12, 16), (16, 22), (10, 24)], (185, 65, 25, 255))
    add_poly(img, [(11, 18), (26, 3), (29, 6), (15, 21)], (92, 104, 120, 255))
    # Heated sawteeth along top edge
    for i in range(0, 14, 3):
        tx, ty = 12 + i, 17 - i
        rect(img, tx - 1, ty - 2, tx + 1, ty, (255, 155, 45, 255))
        px(img, tx, ty - 2, (255, 235, 140, 255))
    add_line(img, 13, 19, 25, 7, (255, 115, 40, 255), 2)
    outline_silhouette(img)
    save_img(img, "textures/item/cryo_sawblade.png")

    # 3. hydro_pulse_gun: 32x32 High-Pressure Water Rifle
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    add_poly(img, [(5, 19), (10, 19), (9, 27), (4, 27)], (52, 66, 86, 255))
    rect(img, 5, 11, 25, 18, (64, 86, 116, 255))
    rect(img, 24, 13, 29, 16, (92, 116, 148, 255))
    rect(img, 28, 13, 30, 16, (56, 189, 248, 255))
    # Hydro cooling tank
    rect(img, 10, 7, 20, 11, (38, 165, 235, 255))
    rect(img, 12, 8, 18, 10, (165, 240, 255, 255))
    rect(img, 10, 13, 19, 16, (32, 195, 245, 255))
    outline_silhouette(img)
    save_img(img, "textures/item/hydro_pulse_gun.png")

    # 4. aero_blade: 32x32 Sleek Air Tactical Saber
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    add_line(img, 4, 27, 9, 22, (46, 78, 72, 255), 3)
    rect(img, 8, 18, 12, 22, (195, 245, 225, 255))
    add_poly(img, [(10, 19), (28, 3), (27, 7), (13, 21)], (74, 222, 128, 255))
    add_line(img, 11, 18, 28, 3, (225, 255, 242, 255), 2)
    outline_silhouette(img)
    save_img(img, "textures/item/aero_blade.png")

    # 5. precursor_claymore: 32x32 5-Star Heavy Alloy Greatblade
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rect(img, 3, 26, 6, 29, (245, 195, 55, 255))
    add_line(img, 5, 26, 10, 21, (48, 54, 68, 255), 3)
    add_poly(img, [(6, 19), (12, 15), (16, 20), (11, 25)], (245, 195, 55, 255))
    add_poly(img, [(11, 18), (27, 2), (29, 5), (15, 21)], (232, 242, 255, 255))
    add_line(img, 11, 18, 27, 2, (255, 215, 85, 255), 2)
    add_line(img, 14, 19, 27, 6, (56, 189, 248, 255), 2)
    rect(img, 9, 17, 12, 20, (125, 235, 255, 255))
    outline_silhouette(img)
    save_img(img, "textures/item/precursor_claymore.png")


def gen_chips():
    def chip_base(board_col, edge_col):
        img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        rect(img, 5, 5, 26, 26, edge_col)
        rect(img, 6, 6, 25, 25, board_col)
        # Gold contact pins
        for p in range(8, 25, 4):
            rect(img, p, 3, p + 1, 5, (235, 190, 55, 255))
            rect(img, p, 26, p + 1, 28, (235, 190, 55, 255))
            rect(img, 3, p, 5, p + 1, (235, 190, 55, 255))
            rect(img, 26, p, 28, p + 1, (235, 190, 55, 255))
        return img

    # Slot 1: HP Control Chip (Cross + Cyan Pulse Core)
    c1 = chip_base((24, 42, 62, 255), (56, 135, 185, 255))
    rect(c1, 13, 9, 18, 22, (74, 222, 128, 255))
    rect(c1, 9, 13, 22, 18, (74, 222, 128, 255))
    rect(c1, 13, 13, 18, 18, (215, 255, 230, 255))
    outline_silhouette(c1)
    save_img(c1, "textures/item/fusion_core_chip.png")

    # Slot 2: ATK Drive Module (Dual Amber Turbines)
    c2 = chip_base((48, 34, 28, 255), (195, 115, 45, 255))
    rect(c2, 8, 9, 14, 22, (245, 145, 45, 255))
    rect(c2, 17, 9, 23, 22, (245, 145, 45, 255))
    rect(c2, 10, 12, 12, 19, (255, 235, 155, 255))
    rect(c2, 19, 12, 21, 19, (255, 235, 155, 255))
    outline_silhouette(c2)
    save_img(c2, "textures/item/void_drive_chip.png")

    # Slot 3: Energy Battery (High-density cyan/green energy bars)
    c3 = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rect(c3, 11, 3, 20, 6, (195, 210, 225, 255))
    rect(c3, 8, 6, 23, 28, (34, 48, 64, 255))
    rect(c3, 10, 8, 21, 26, (20, 30, 42, 255))
    rect(c3, 11, 10, 20, 13, (74, 222, 128, 255))
    rect(c3, 11, 15, 20, 18, (56, 189, 248, 255))
    rect(c3, 11, 20, 20, 24, (96, 165, 250, 255))
    outline_silhouette(c3)
    save_img(c3, "textures/item/servo_battery_chip.png")

    # Slot 4: Element Overclock Chip (Purple crystal prism)
    c4 = chip_base((44, 24, 68, 255), (192, 132, 252, 255))
    add_poly(c4, [(16, 8), (23, 16), (16, 23), (8, 16)], (192, 132, 252, 255))
    add_poly(c4, [(16, 11), (20, 16), (16, 20), (11, 16)], (255, 225, 120, 255))
    outline_silhouette(c4)
    save_img(c4, "textures/item/overclock_chip.png")

    # Slot 5: Tactical Halo Core (5-Star Golden Precision Gyro)
    c5 = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    add_circle(c5, 15, 15, 11, (48, 42, 26, 255), (251, 191, 36, 255))
    add_circle(c5, 15, 15, 7, (32, 72, 108, 255), (245, 205, 85, 255))
    add_circle(c5, 15, 15, 4, (125, 230, 255, 255))
    px(c5, 15, 15, (255, 255, 255, 255))
    outline_silhouette(c5)
    save_img(c5, "textures/item/neural_halo_chip.png")


def gen_tiered_material_icon(shape, primary_col, accent_col, tier):
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    tier_cols = {
        1: (148, 163, 184, 255),
        2: (74, 222, 128, 255),
        3: (56, 189, 248, 255),
        4: (192, 132, 252, 255),
        5: (251, 191, 36, 255),
        6: (248, 113, 113, 255),
    }
    bcol = tier_cols[tier]
    # Subtle tiered frame corner glow
    rect(img, 4, 4, 27, 27, (24, 30, 42, 220))
    rect(img, 5, 5, 26, 26, (34, 44, 58, 220))

    if shape == "char":
        # Mechanical bearing / piston / hydraulic core
        add_circle(img, 16, 16, 8, primary_col, bcol)
        add_circle(img, 16, 16, 4, accent_col)
        rect(img, 14, 5, 17, 9, bcol)
        rect(img, 14, 22, 17, 26, bcol)
    elif shape == "weapon":
        # Weapon receiver / rail / stabilizer barrel
        add_poly(img, [(8, 22), (21, 9), (24, 12), (11, 25)], primary_col, bcol)
        rect(img, 12, 11, 21, 19, accent_col)
        rect(img, 14, 13, 19, 17, bcol)
    else:
        # Tactical Training Manual (bound codex with glowing stripe)
        rect(img, 8, 6, 24, 25, primary_col)
        rect(img, 8, 6, 11, 25, bcol)
        rect(img, 14, 9, 22, 14, accent_col)
        rect(img, 14, 17, 21, 19, (230, 240, 250, 255))
        rect(img, 14, 21, 19, 22, bcol)

    # Clear tier pips along bottom
    for t in range(tier):
        rx = 5 + t * 4
        rect(img, rx, 26, rx + 2, 28, bcol)
    outline_silhouette(img)
    return img


def gen_materials():
    # Legacy items (32x32)
    qc = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    add_poly(qc, [(16, 4), (27, 16), (16, 27), (5, 16)], (56, 189, 248, 255), (251, 191, 36, 255))
    add_poly(qc, [(16, 9), (22, 16), (16, 22), (10, 16)], (215, 250, 255, 255))
    outline_silhouette(qc)
    save_img(qc, "textures/item/quantum_core.png")

    ap = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rect(ap, 5, 7, 26, 24, (78, 90, 108, 255))
    rect(ap, 7, 9, 24, 22, (112, 126, 146, 255))
    rect(ap, 7, 20, 24, 22, (235, 175, 45, 255))
    outline_silhouette(ap)
    save_img(ap, "textures/item/alloy_plate.png")

    ps = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    add_circle(ps, 16, 16, 10, (245, 130, 35, 255), (255, 215, 85, 255))
    add_circle(ps, 16, 16, 6, (255, 215, 85, 255))
    add_circle(ps, 16, 16, 3, (255, 255, 235, 255))
    outline_silhouette(ps)
    save_img(ps, "textures/item/precursor_spark.png")

    # Credit Coin (32x32 detailed sci-fi token)
    coin = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    add_circle(coin, 16, 16, 11, (205, 145, 25, 255), (245, 200, 65, 255))
    add_circle(coin, 16, 16, 8, (250, 215, 85, 255))
    rect(coin, 14, 10, 17, 22, (175, 115, 15, 255))
    rect(coin, 12, 12, 19, 14, (175, 115, 15, 255))
    rect(coin, 12, 18, 19, 20, (175, 115, 15, 255))
    outline_silhouette(coin)
    save_img(coin, "textures/item/credit_coin.png")

    # Character EXP Bottle (32x32 glass vial + glowing green serum)
    exp = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rect(exp, 12, 4, 19, 7, (195, 210, 228, 255))
    rect(exp, 13, 7, 18, 10, (90, 135, 175, 255))
    add_poly(exp, [(12, 10), (19, 10), (24, 18), (24, 27), (8, 27), (8, 18)], (42, 62, 86, 255), (145, 195, 235, 255))
    add_poly(exp, [(10, 17), (22, 17), (22, 25), (10, 25)], (74, 222, 128, 255))
    rect(exp, 12, 19, 16, 23, (195, 255, 215, 255))
    outline_silhouette(exp)
    save_img(exp, "textures/item/exp_bottle.png")

    # Equipment Upgrade Dust (32x32 crystalline purple powder mound)
    dust = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    add_poly(dust, [(6, 25), (16, 10), (26, 25)], (168, 85, 247, 255))
    add_poly(dust, [(10, 24), (16, 13), (22, 24)], (216, 160, 255, 255))
    rect(dust, 14, 17, 18, 22, (250, 230, 255, 255))
    for sx, sy in ((8, 10), (23, 9), (11, 7), (21, 15)):
        rect(dust, sx, sy, sx + 1, sy + 1, (251, 191, 36, 255))
    outline_silhouette(dust)
    save_img(dust, "textures/item/equip_essence.png")

    # Supply Pass Ticket (32x32 perforated boarding pass with gold star)
    ticket = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rect(ticket, 4, 8, 27, 23, (30, 82, 165, 255))
    rect(ticket, 6, 10, 25, 21, (56, 189, 248, 255))
    rect(ticket, 12, 12, 19, 19, (251, 191, 36, 255))
    rect(ticket, 14, 14, 17, 17, (255, 245, 195, 255))
    for y in (15, 16):
        rect(ticket, 4, y, 5, y, (0, 0, 0, 0))
        rect(ticket, 26, y, 27, y, (0, 0, 0, 0))
    outline_silhouette(ticket)
    save_img(ticket, "textures/item/star_ticket.png")

    # 1..6 Tier Materials (3 independent lines, 32x32)
    for t in range(1, 7):
        save_img(gen_tiered_material_icon("char", (82, 96, 118, 255), (235, 175, 55, 255), t),
                 f"textures/item/char_mat_{t}.png")
        save_img(gen_tiered_material_icon("weapon", (72, 82, 102, 255), (56, 189, 248, 255), t),
                 f"textures/item/weapon_mat_{t}.png")
        save_img(gen_tiered_material_icon("talent", (52, 74, 106, 255), (245, 200, 75, 255), t),
                 f"textures/item/talent_mat_{t}.png")


# ============================================================
# 4. 64x64 DETAILED CHARACTER PORTRAITS & SKILL ICONS
# ============================================================
def make_portrait_64(bg_col, hair_col, hair_hi, skin_col, skin_sh, visor_col, coat_col, accent_col, accessory="visor"):
    img = Image.new("RGBA", (64, 64), bg_col)
    # Corner frame brackets
    rect(img, 0, 0, 63, 1, accent_col)
    rect(img, 0, 62, 63, 63, accent_col)
    rect(img, 0, 0, 1, 63, accent_col)
    rect(img, 62, 0, 63, 63, accent_col)

    # Coat / Shoulders / Tactical Collar
    add_poly(img, [(10, 61), (18, 43), (45, 43), (53, 61)], coat_col)
    add_poly(img, [(24, 43), (39, 43), (35, 61), (28, 61)], accent_col)
    rect(img, 27, 39, 36, 46, skin_sh)

    # Face & Jaw shading
    add_poly(img, [(20, 18), (43, 18), (42, 36), (36, 42), (27, 42), (21, 36)], skin_col)
    rect(img, 20, 32, 23, 38, skin_sh)

    # Hair volume + highlights
    rect(img, 18, 9, 45, 21, hair_col)
    rect(img, 16, 14, 21, 36, hair_col)
    rect(img, 42, 14, 47, 36, hair_col)
    rect(img, 22, 11, 41, 14, hair_hi)
    # Bangs
    rect(img, 22, 18, 26, 24, hair_col)
    rect(img, 30, 18, 34, 23, hair_col)
    rect(img, 38, 18, 41, 24, hair_col)

    # Eyes / Tactical Goggles
    if accessory == "goggles":
        rect(img, 19, 13, 44, 18, (35, 40, 50, 255))
        rect(img, 21, 14, 29, 17, visor_col)
        rect(img, 34, 14, 42, 17, visor_col)
    rect(img, 23, 26, 28, 29, (250, 252, 255, 255))
    rect(img, 35, 26, 40, 29, (250, 252, 255, 255))
    rect(img, 25, 26, 27, 29, visor_col)
    rect(img, 37, 26, 39, 29, visor_col)
    px(img, 26, 27, (255, 255, 255, 255))
    px(img, 38, 27, (255, 255, 255, 255))

    # Mouth & nose subtle shadow
    rect(img, 31, 32, 32, 34, skin_sh)
    rect(img, 29, 37, 34, 38, (145, 95, 88, 255))

    # Headset mic / badge
    rect(img, 44, 27, 47, 33, accent_col)
    rect(img, 37, 34, 45, 35, (180, 210, 235, 255))
    return img


def gen_gui_textures():
    # 64x64 Portraits
    save_img(make_portrait_64((20, 28, 38, 255), (48, 55, 68, 255), (84, 96, 114, 255),
                              (238, 206, 186, 255), (208, 168, 145, 255),
                              (235, 175, 45, 255), (58, 72, 90, 255), (217, 155, 45, 255)),
             "textures/gui/portrait_dawn.png")
    save_img(make_portrait_64((18, 34, 32, 255), (36, 86, 74, 255), (68, 138, 118, 255),
                              (238, 206, 186, 255), (208, 168, 145, 255),
                              (74, 222, 128, 255), (42, 64, 58, 255), (74, 222, 128, 255)),
             "textures/gui/portrait_chen.png")
    save_img(make_portrait_64((16, 30, 44, 255), (42, 64, 98, 255), (78, 110, 158, 255),
                              (244, 218, 208, 255), (214, 178, 168, 255),
                              (56, 189, 248, 255), (222, 234, 244, 255), (56, 189, 248, 255)),
             "textures/gui/portrait_su.png")
    save_img(make_portrait_64((34, 24, 18, 255), (96, 98, 108, 255), (145, 148, 162, 255),
                              (224, 182, 152, 255), (186, 142, 112, 255),
                              (251, 146, 60, 255), (175, 105, 32, 255), (251, 146, 60, 255), "goggles"),
             "textures/gui/portrait_zheng.png")
    save_img(make_portrait_64((34, 18, 24, 255), (30, 32, 44, 255), (62, 66, 86, 255),
                              (236, 202, 188, 255), (200, 162, 148, 255),
                              (239, 68, 68, 255), (46, 34, 42, 255), (239, 68, 68, 255)),
             "textures/gui/portrait_han.png")
    save_img(make_portrait_64((18, 28, 46, 255), (168, 190, 218, 255), (215, 230, 248, 255),
                              (242, 214, 198, 255), (210, 176, 160, 255),
                              (96, 165, 250, 255), (44, 68, 108, 255), (96, 165, 250, 255)),
             "textures/gui/portrait_zhou.png")

    locked = Image.new("RGBA", (64, 64), (16, 22, 32, 255))
    rect(locked, 0, 0, 63, 1, (38, 54, 74, 255))
    rect(locked, 0, 62, 63, 63, (38, 54, 74, 255))
    rect(locked, 22, 27, 41, 46, (68, 82, 100, 255))
    rect(locked, 25, 16, 38, 27, (88, 102, 122, 255))
    rect(locked, 28, 19, 35, 27, (16, 22, 32, 255))
    save_img(locked, "textures/gui/portrait_locked.png")

    # Skill Icons (16x16)
    se = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(2, 14):
        px(se, i, 15 - i, (220, 255, 255, 255))
        px(se, i, 14 - i, (0, 210, 255, 255))
    rect(se, 2, 11, 5, 13, (255, 200, 60, 255))
    save_img(se, "textures/gui/skill_e_dawn.png")

    sq = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    add_circle(sq, 7, 7, 6, (255, 155, 25, 255))
    add_circle(sq, 7, 7, 4, (255, 225, 95, 255))
    save_img(sq, "textures/gui/skill_q_dawn.png")


# ============================================================
# 5. ENTITY TEXTURES (64x64)
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
# 6. ITEM MODELS & LOCALIZATIONS
# ============================================================
def gen_item_models_and_lang():
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
        "block.picking_hope_core.supply_crate": "装甲物资箱",
        "item.picking_hope_core.supply_crate": "装甲物资箱",
        "block.picking_hope_core.cryo_sleep_pod": "休眠维生舱",
        "item.picking_hope_core.cryo_sleep_pod": "休眠维生舱",
        "block.picking_hope_core.conduit_pillar": "工业能量导管柱",
        "item.picking_hope_core.conduit_pillar": "工业能量导管柱",
        "block.picking_hope_core.holotable_prop": "全息星图沙盘",
        "item.picking_hope_core.holotable_prop": "全息星图沙盘",
        "block.picking_hope_core.starship_floor": "星舰合金地板",
        "item.picking_hope_core.starship_floor": "星舰合金地板",
        "block.picking_hope_core.starship_wall": "星舰加厚舱壁",
        "item.picking_hope_core.starship_wall": "星舰加厚舱壁",
        "block.picking_hope_core.starship_hull": "星舰重装甲外壳",
        "item.picking_hope_core.starship_hull": "星舰重装甲外壳",
        "block.picking_hope_core.hazard_stripe_block": "工业警戒条纹块",
        "item.picking_hope_core.hazard_stripe_block": "工业警戒条纹块",
        "block.picking_hope_core.starship_light": "星舰照明灯板",
        "item.picking_hope_core.starship_light": "星舰照明灯板",
        "block.picking_hope_core.alarm_beacon": "应急警报灯",
        "item.picking_hope_core.alarm_beacon": "应急警报灯",
        "block.picking_hope_core.viewport_glass": "星舰强化舷窗",
        "item.picking_hope_core.viewport_glass": "星舰强化舷窗",
        "block.picking_hope_core.permafrost_rock": "冻土岩层块",
        "item.picking_hope_core.permafrost_rock": "冻土岩层块",
        "block.picking_hope_core.glacier_snow": "极地覆雪层",
        "item.picking_hope_core.glacier_snow": "极地覆雪层",
        "block.picking_hope_core.frozen_ore_vein": "发光晶矿岩脉",
        "item.picking_hope_core.frozen_ore_vein": "发光晶矿岩脉",
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
        "block.picking_hope_core.supply_crate": "Armored Supply Crate",
        "item.picking_hope_core.supply_crate": "Armored Supply Crate",
        "block.picking_hope_core.cryo_sleep_pod": "Cryo Sleep Pod",
        "item.picking_hope_core.cryo_sleep_pod": "Cryo Sleep Pod",
        "block.picking_hope_core.conduit_pillar": "Industrial Conduit Pillar",
        "item.picking_hope_core.conduit_pillar": "Industrial Conduit Pillar",
        "block.picking_hope_core.holotable_prop": "Holographic Map Table",
        "item.picking_hope_core.holotable_prop": "Holographic Map Table",
        "block.picking_hope_core.starship_floor": "Starship Alloy Floor",
        "item.picking_hope_core.starship_floor": "Starship Alloy Floor",
        "block.picking_hope_core.starship_wall": "Starship Bulkhead Wall",
        "item.picking_hope_core.starship_wall": "Starship Bulkhead Wall",
        "block.picking_hope_core.starship_hull": "Starship Armored Hull",
        "item.picking_hope_core.starship_hull": "Starship Armored Hull",
        "block.picking_hope_core.hazard_stripe_block": "Hazard Stripe Block",
        "item.picking_hope_core.hazard_stripe_block": "Hazard Stripe Block",
        "block.picking_hope_core.starship_light": "Starship Luminaire",
        "item.picking_hope_core.starship_light": "Starship Luminaire",
        "block.picking_hope_core.alarm_beacon": "Emergency Alarm Beacon",
        "item.picking_hope_core.alarm_beacon": "Emergency Alarm Beacon",
        "block.picking_hope_core.viewport_glass": "Reinforced Viewport Glass",
        "item.picking_hope_core.viewport_glass": "Reinforced Viewport Glass",
        "block.picking_hope_core.permafrost_rock": "Permafrost Basalt",
        "item.picking_hope_core.permafrost_rock": "Permafrost Basalt",
        "block.picking_hope_core.glacier_snow": "Polar Glacier Snow",
        "item.picking_hope_core.glacier_snow": "Polar Glacier Snow",
        "block.picking_hope_core.frozen_ore_vein": "Glowing Cryo Ore Vein",
        "item.picking_hope_core.frozen_ore_vein": "Glowing Cryo Ore Vein",
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
    gen_block_textures()
    gen_block_models_and_states()
    gen_weapons()
    gen_chips()
    gen_materials()
    gen_gui_textures()
    gen_entities()
    gen_item_models_and_lang()
    print("All v2.0 high-res 32x32 items, 64x64 portraits, custom blocks & 3D voxel models generated!")
