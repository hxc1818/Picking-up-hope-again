import os
import struct

def _s(x):
    b = x.encode("utf-8")
    return struct.pack(">H", len(b)) + b

def _int(name, v):
    return b"\x03" + _s(name) + struct.pack(">i", v)

def _str_tag(name, v):
    return b"\x08" + _s(name) + _s(v)

def _list_int(name, vals):
    payload = b"".join(struct.pack(">i", v) for v in vals)
    return b"\x09" + _s(name) + b"\x03" + struct.pack(">i", len(vals)) + payload

def _list_comp(name, comps):
    payload = b"".join(c + b"\x00" for c in comps)
    return b"\x09" + _s(name) + b"\x0a" + struct.pack(">i", len(comps)) + payload

# Palette indices
# 0: air
# 1: gray_concrete (floor)
# 2: black_concrete (walls/roof)
# 3: sea_lantern (lights)
# 4: iron_block (pedestals/machinery)
# 5: picking_hope_core:teleport_waypoint (waypoint)
# 6: yellow_concrete (hazard line)
# 7: chest (starter chest)

palette = [
    _str_tag("Name", "minecraft:air"),
    _str_tag("Name", "minecraft:gray_concrete"),
    _str_tag("Name", "minecraft:black_concrete"),
    _str_tag("Name", "minecraft:sea_lantern"),
    _str_tag("Name", "minecraft:iron_block"),
    _str_tag("Name", "picking_hope_core:teleport_waypoint"),
    _str_tag("Name", "minecraft:yellow_concrete"),
    _str_tag("Name", "minecraft:chest"),
]

SIZE_X = 40
SIZE_Y = 7
SIZE_Z = 20

blocks = []

def set_block(x, y, z, state):
    blocks.append(_list_int("pos", [x, y, z]) + _int("state", state))

for x in range(SIZE_X):
    for z in range(SIZE_Z):
        # Floor (y = 0)
        is_hazard = (x in (12, 13, 24, 25)) and (7 <= z <= 12)
        floor_state = 6 if is_hazard else 1
        set_block(x, 0, z, floor_state)

        # Roof (y = 6)
        is_light = (x % 6 == 3) and (z % 6 == 3)
        roof_state = 3 if is_light else 2
        set_block(x, 6, z, roof_state)

        # Outer bounding walls
        for y in range(1, 6):
            if x == 0 or x == SIZE_X - 1 or z == 0 or z == SIZE_Z - 1:
                set_block(x, y, z, 2)
            else:
                # Interior partitioning:
                # Room 1: Awakening Chamber (x: 1..12, z: 1..18)
                # Corridor: x: 13..24, z: 7..12
                # Room 2: Fusion Hub (x: 25..38, z: 1..18)
                if (13 <= x <= 24) and not (7 <= z <= 12):
                    # Corridor side walls
                    set_block(x, y, z, 2)
                elif x == 12 and not (8 <= z <= 11):
                    # Airlock 1 wall
                    set_block(x, y, z, 2)
                elif x == 24 and not (8 <= z <= 11):
                    # Airlock 2 wall
                    set_block(x, y, z, 2)

# Special interior blocks:
# Awakening Bay: Starter pedestal and chest
set_block(6, 1, 10, 4) # Iron pedestal
set_block(6, 2, 10, 7) # Starter chest

# Fusion Hub: Central Teleport Waypoint
set_block(32, 1, 10, 5) # Starship Teleport Waypoint!
# Fusion generator columns
for y in range(1, 6):
    set_block(32, y, 5, 4)
    set_block(32, y, 15, 4)
    set_block(30, y, 10, 4)
    set_block(34, y, 10, 4)

root = (
    _int("DataVersion", 2860)
    + _list_int("size", [SIZE_X, SIZE_Y, SIZE_Z])
    + _list_comp("palette", palette)
    + _list_comp("blocks", blocks)
    + _list_comp("entities", [])
)
nbt = b"\x0a" + _s("") + root + b"\x00"

# Target paths
paths = [
    "mod/src/main/resources/data/picking_hope_core/structures/starship_prologue.nbt",
    "Minecraft/versions/1.18.2-Forge_40.3.0/generated/picking_hope_core/structures/starship_prologue.nbt",
    "Minecraft/generated/picking_hope_core/structures/starship_prologue.nbt",
]

for p in paths:
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "wb") as f:
        f.write(nbt)
    print(f"Generated structure: {p} ({len(nbt)} bytes, {len(blocks)} blocks)")
