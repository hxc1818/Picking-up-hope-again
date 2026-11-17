"""Generate a 20x20 Minecraft 1.18.2 structure NBT file.

The platform is made of stone at y=0, with a single
`picking_hope_core:teleport_waypoint` block at the center.
No external dependencies: writes raw (uncompressed) NBT, which is what
structure blocks expect.
"""
import struct

# --- Minimal NBT writer (big-endian) ---------------------------------------
def _s(x):
    b = x.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _int(name, v):
    return b"\x03" + _s(name) + struct.pack(">i", v)          # TAG_Int


def _str_tag(name, v):
    return b"\x08" + _s(name) + _s(v)                          # TAG_String


def _list_int(name, vals):
    payload = b"".join(struct.pack(">i", v) for v in vals)     # TAG_Int elements
    return b"\x09" + _s(name) + b"\x03" + struct.pack(">i", len(vals)) + payload


def _list_comp(name, comps):
    payload = b"".join(c + b"\x00" for c in comps)             # each compound + TAG_End
    return b"\x09" + _s(name) + b"\x0a" + struct.pack(">i", len(comps)) + payload


# --- Build the structure ---------------------------------------------------
SIZE = 20
CX = CZ = SIZE // 2                       # center block at (10, 0, 10)

# Palette: index 0 = stone, index 1 = the teleport waypoint
palette = [
    _str_tag("Name", "minecraft:stone"),
    _str_tag("Name", "picking_hope_core:teleport_waypoint"),
]

blocks = []
for x in range(SIZE):
    for z in range(SIZE):
        state = 1 if (x == CX and z == CZ) else 0
        blocks.append(_list_int("pos", [x, 0, z]) + _int("state", state))

root = (
    _int("DataVersion", 2860)              # Minecraft 1.18.2
    + _list_int("size", [SIZE, 1, SIZE])
    + _list_comp("palette", palette)
    + _list_comp("blocks", blocks)
    + _list_comp("entities", [])           # no entities
)
nbt = b"\x0a" + _s("") + root + b"\x00"    # root TAG_Compound, empty name

with open("platform.nbt", "wb") as f:
    f.write(nbt)

print(f"Wrote platform.nbt ({len(nbt)} bytes, {len(blocks)} blocks)")
