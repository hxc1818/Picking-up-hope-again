import json
import os

OUT_DIR = os.path.join("mod", "src", "main", "resources", "assets", "picking_hope_core", "battle_animations")


def save_clip(clip):
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, f"{clip['id']}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(clip, f, ensure_ascii=False, indent=2)


def rk(t, anchor="home", offset=(0.0, 0.0, 0.0), yaw=0.0, ease="ease_in_out_cubic"):
    return {
        "t": round(t, 4),
        "anchor": anchor,
        "offset": [round(float(x), 3) for x in offset],
        "yawOffset": round(float(yaw), 2),
        "ease": ease,
    }


def bk(t, rot=(0.0, 0.0, 0.0), pos=(0.0, 0.0, 0.0), ease="ease_in_out_quad"):
    return {
        "t": round(t, 4),
        "rot": [round(float(x), 2) for x in rot],
        "pos": [round(float(x), 2) for x in pos],
        "ease": ease,
    }


def ck(t, pos_anchor="overview", pos_offset=(0.0, 0.0, 0.0),
       look_anchor="arena_center", look_offset=(0.0, 1.1, 0.0),
       fov=52.0, roll=0.0, local=False, ease="ease_in_out_cubic"):
    return {
        "t": round(t, 4),
        "posAnchor": pos_anchor,
        "posOffset": [round(float(x), 3) for x in pos_offset],
        "localToActorFacing": bool(local),
        "lookAnchor": look_anchor,
        "lookOffset": [round(float(x), 3) for x in look_offset],
        "fov": round(float(fov), 1),
        "roll": round(float(roll), 2),
        "ease": ease,
    }


def ev(t, ev_type="impact", anchor="target", vfx="", particle="", sound="",
       pitch=1.0, volume=0.85, count=12, spread=0.5, scale=1.0, shake=0.0, hit_stop=0, suppress=False):
    return {
        "t": round(t, 4),
        "type": ev_type,
        "anchor": anchor,
        "vfx": vfx,
        "particle": particle,
        "sound": sound,
        "pitch": round(float(pitch), 2),
        "volume": round(float(volume), 2),
        "count": int(count),
        "spread": round(float(spread), 2),
        "scale": round(float(scale), 2),
        "shake": round(float(shake), 2),
        "hitStop": int(hit_stop),
        "suppressDamagePopup": bool(suppress),
    }


def build_shared_clips():
    # 1. humanoid_idle
    save_clip({
        "id": "humanoid_idle",
        "durationTicks": 36,
        "loop": True,
        "bones": {
            "torso": [bk(0.0, (2, 0, 0)), bk(0.5, (5, 0, 0)), bk(1.0, (2, 0, 0))],
            "right_arm": [bk(0.0, (-22, 0, 8)), bk(0.5, (-30, 0, 10)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (12, 0, -8)), bk(0.5, (18, 0, -10)), bk(1.0, (12, 0, -8))],
            "right_leg": [bk(0.0, (-8, 0, 0)), bk(0.5, (-5, 0, 0)), bk(1.0, (-8, 0, 0))],
            "left_leg": [bk(0.0, (8, 0, 0)), bk(0.5, (5, 0, 0)), bk(1.0, (8, 0, 0))],
        },
        "rootMotion": [],
        "camera": [],
        "events": [],
    })

    # 2. humanoid_hurt
    save_clip({
        "id": "humanoid_hurt",
        "durationTicks": 12,
        "loop": False,
        "bones": {
            "torso": [bk(0.0, (0, 0, 0)), bk(0.3, (-22, 12, 0), ease="ease_out_quad"), bk(1.0, (0, 0, 0))],
            "head": [bk(0.0, (0, 0, 0)), bk(0.3, (-18, 0, 0)), bk(1.0, (0, 0, 0))],
            "right_arm": [bk(0.0, (-22, 0, 8)), bk(0.3, (28, 0, 22)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (12, 0, -8)), bk(0.3, (-35, 0, -20)), bk(1.0, (12, 0, -8))],
        },
        "rootMotion": [],
        "camera": [],
        "events": [],
    })

    # 3. humanoid_ready
    save_clip({
        "id": "humanoid_ready",
        "durationTicks": 28,
        "loop": True,
        "bones": {
            "torso": [bk(0.0, (10, -12, 0)), bk(0.5, (14, -15, 0)), bk(1.0, (10, -12, 0))],
            "right_arm": [bk(0.0, (-105, -12, 10)), bk(0.5, (-115, -14, 12)), bk(1.0, (-105, -12, 10))],
            "left_arm": [bk(0.0, (-48, 22, -16)), bk(0.5, (-54, 24, -18)), bk(1.0, (-48, 22, -16))],
            "right_leg": [bk(0.0, (-18, 0, 4)), bk(1.0, (-18, 0, 4))],
            "left_leg": [bk(0.0, (18, 0, -4)), bk(1.0, (18, 0, -4))],
        },
        "rootMotion": [],
        "camera": [],
        "events": [],
    })

    # 4. humanoid_down
    save_clip({
        "id": "humanoid_down",
        "durationTicks": 20,
        "loop": True,
        "bones": {
            "right_arm": [bk(0.0, (15, 0, 20))],
            "left_arm": [bk(0.0, (-20, 0, -18))],
        },
        "rootMotion": [],
        "camera": [],
        "events": [],
    })

    # 5-10. Enemy Idle Loops
    save_clip({
        "id": "enemy_idle_cleaner",
        "durationTicks": 20,
        "loop": True,
        "bones": {
            "body": [bk(0.0, (0, 0, 0), (0, 0, 0)), bk(0.5, (2, 0, 0), (0, -0.3, 0)), bk(1.0, (0, 0, 0), (0, 0, 0))],
            "brush_l": [bk(0.0, (0, 0, 0), ease="linear"), bk(1.0, (0, 360, 0), ease="linear")],
            "brush_r": [bk(0.0, (0, 0, 0), ease="linear"), bk(1.0, (0, -360, 0), ease="linear")],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    save_clip({
        "id": "enemy_idle_drone",
        "durationTicks": 28,
        "loop": True,
        "bones": {
            "body": [bk(0.0, (0, 0, 0), (0, -1.5, 0)), bk(0.5, (5, 0, 0), (0, -3.5, 0)), bk(1.0, (0, 0, 0), (0, -1.5, 0))],
            "ring": [bk(0.0, (0, 0, 0), (0, -1.5, 0), ease="linear"), bk(1.0, (0, 360, 0), (0, -1.5, 0), ease="linear")],
            "thrust_l": [bk(0.0, (8, 0, 0), (0, -1.5, 0)), bk(0.5, (20, 0, 0), (0, -3.5, 0)), bk(1.0, (8, 0, 0), (0, -1.5, 0))],
            "thrust_r": [bk(0.0, (8, 0, 0), (0, -1.5, 0)), bk(0.5, (20, 0, 0), (0, -3.5, 0)), bk(1.0, (8, 0, 0), (0, -1.5, 0))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    save_clip({
        "id": "enemy_idle_hound",
        "durationTicks": 30,
        "loop": True,
        "bones": {
            "jaw": [bk(0.0, (-4, 0, 0)), bk(0.5, (10, 0, 0)), bk(1.0, (-4, 0, 0))],
            "tail": [bk(0.0, (8, -16, 0)), bk(0.5, (12, 16, 0)), bk(1.0, (8, -16, 0))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    save_clip({
        "id": "enemy_idle_guard",
        "durationTicks": 36,
        "loop": True,
        "bones": {
            "torso": [bk(0.0, (4, 0, 0)), bk(0.5, (8, 0, 0)), bk(1.0, (4, 0, 0))],
            "arm_l": [bk(0.0, (-22, 10, -8)), bk(0.5, (-28, 12, -10)), bk(1.0, (-22, 10, -8))],
            "arm_r": [bk(0.0, (-34, -6, 6)), bk(0.5, (-40, -8, 8)), bk(1.0, (-34, -6, 6))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    save_clip({
        "id": "enemy_idle_driller",
        "durationTicks": 20,
        "loop": True,
        "bones": {
            "drill": [bk(0.0, (0, 0, 0), ease="linear"), bk(1.0, (0, 0, 360), ease="linear")],
            "body": [bk(0.0, (0, 0, 0), (0, 0, 0)), bk(0.5, (0, 0, 0), (0, -0.35, 0)), bk(1.0, (0, 0, 0), (0, 0, 0))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    save_clip({
        "id": "enemy_idle_boss",
        "durationTicks": 40,
        "loop": True,
        "bones": {
            "head": [bk(0.0, (0, -10, 0)), bk(0.5, (4, 10, 0)), bk(1.0, (0, -10, 0))],
            "arm_l": [bk(0.0, (-20, 0, -6)), bk(0.5, (-34, 0, -10)), bk(1.0, (-20, 0, -6))],
            "arm_r": [bk(0.0, (-28, 0, 6)), bk(0.5, (-14, 0, 10)), bk(1.0, (-28, 0, 6))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    # 11-12. Enemy Hurt & Break
    save_clip({
        "id": "enemy_hurt",
        "durationTicks": 12,
        "loop": False,
        "bones": {
            "body": [bk(0.0, (0, 0, 0)), bk(0.3, (-18, 0, 0)), bk(1.0, (0, 0, 0))],
            "torso": [bk(0.0, (0, 0, 0)), bk(0.3, (-22, 10, 0)), bk(1.0, (0, 0, 0))],
            "arm_l": [bk(0.0, (-20, 0, 0)), bk(0.3, (20, 0, -25)), bk(1.0, (-20, 0, 0))],
            "arm_r": [bk(0.0, (-20, 0, 0)), bk(0.3, (20, 0, 25)), bk(1.0, (-20, 0, 0))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    save_clip({
        "id": "enemy_break",
        "durationTicks": 26,
        "loop": True,
        "bones": {
            "body": [bk(0.0, (16, 0, -8), (0, 2.0, 0)), bk(0.5, (20, 0, 8), (0, 2.4, 0)), bk(1.0, (16, 0, -8), (0, 2.0, 0))],
            "torso": [bk(0.0, (24, 0, -6)), bk(0.5, (28, 0, 6)), bk(1.0, (24, 0, -6))],
            "head": [bk(0.0, (25, -14, 0)), bk(0.5, (25, 14, 0)), bk(1.0, (25, -14, 0))],
            "arm_l": [bk(0.0, (12, 0, -18)), bk(1.0, (12, 0, -18))],
            "arm_r": [bk(0.0, (12, 0, 18)), bk(1.0, (12, 0, 18))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })


def build_character_clips():
    # ==================== LIN YE (Earth / Heavy Blade) ====================
    save_clip({
        "id": "linye_basic",
        "durationTicks": 20,
        "bones": {
            "torso": [bk(0.0, (4, 0, 0)), bk(0.28, (8, -28, 0)), bk(0.50, (28, 26, 0), ease="ease_out_cubic"), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-25, 0, 10)), bk(0.28, (-138, 12, 20)), bk(0.50, (-15, -24, -18), ease="ease_out_cubic"), bk(1.0, (-25, 0, 10))],
            "left_arm": [bk(0.0, (15, 0, -10)), bk(0.30, (-45, 0, -15)), bk(0.55, (32, 0, -18)), bk(1.0, (15, 0, -10))],
            "weapon": [bk(0.0, (0, 0, 0)), bk(0.28, (-15, 0, 0)), bk(0.50, (32, 0, 0)), bk(1.0, (0, 0, 0))],
            "right_leg": [bk(0.0, (0, 0, 0)), bk(0.35, (-34, 0, 0)), bk(1.0, (0, 0, 0))],
            "left_leg": [bk(0.0, (0, 0, 0)), bk(0.35, (34, 0, 0)), bk(1.0, (0, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.30, "target", (-1.55, 0.25, 0.0), ease="ease_out_cubic"),
            rk(0.62, "target", (-1.55, 0.0, 0.0)),
            rk(1.0, "home", ease="ease_out_cubic"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.38, "target", (-2.2, 2.9, 5.2), "target", (0, 1.1, 0), 48, 1.8, local=True),
            ck(0.70, "target", (-2.2, 2.9, 5.2), "target", (0, 1.1, 0), 49, 0.0, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.20, "sound", "actor", sound="sweep", pitch=0.95),
            ev(0.46, "impact", "target", vfx="slash_arc", particle="crit", sound="strong_hit", count=14, shake=0.25, hit_stop=1),
        ],
    })

    save_clip({
        "id": "linye_skill",
        "durationTicks": 26,
        "bones": {
            "torso": [bk(0.0, (5, 0, 0)), bk(0.28, (-24, 0, 0)), bk(0.50, (36, 0, 0), ease="ease_out_cubic"), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-30, 0, 10)), bk(0.28, (-168, 0, 10)), bk(0.50, (-18, 0, -10), ease="ease_out_cubic"), bk(1.0, (-25, 0, 10))],
            "left_arm": [bk(0.0, (12, 0, -10)), bk(0.28, (-155, 0, -12)), bk(0.50, (-22, 0, 10)), bk(1.0, (12, 0, -10))],
            "weapon": [bk(0.0, (0, 0, 0)), bk(0.28, (-20, 0, 0)), bk(0.50, (38, 0, 0)), bk(1.0, (0, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.26, "home", (2.4, 1.55, 0.0), ease="ease_out_quad"),
            rk(0.48, "target", (-1.65, 0.0, 0.0), ease="ease_in_cubic"),
            rk(0.72, "target", (-1.65, 0.0, 0.0)),
            rk(1.0, "home", ease="ease_out_cubic"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.32, "target", (-3.8, 2.8, 6.0), "target", (0, 1.0, 0), 46, -2.5, local=True),
            ck(0.62, "target", (-3.2, 2.2, 5.4), "target", (0, 0.9, 0), 48, 2.0, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.22, "sound", "actor", sound="sweep", pitch=0.85),
            ev(0.48, "impact", "target", vfx="earth_cleave", particle="crit", sound="anvil", pitch=1.1, count=18, scale=1.35, shake=0.45, hit_stop=3),
        ],
    })

    save_clip({
        "id": "linye_ult",
        "durationTicks": 44,
        "bannerStyle": "ULT",
        "bones": {
            "torso": [bk(0.0, (8, -28, 0)), bk(0.25, (-28, 0, 0)), bk(0.45, (-35, 0, 0)), bk(0.60, (42, 0, 0), ease="ease_out_cubic"), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-105, -20, 15)), bk(0.30, (-175, 0, 10)), bk(0.60, (-10, 0, -14), ease="ease_out_cubic"), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (-45, 20, -15)), bk(0.30, (-165, 0, -10)), bk(0.60, (-14, 0, 12)), bk(1.0, (12, 0, -8))],
            "weapon": [bk(0.0, (-10, 0, 0)), bk(0.32, (-22, 0, 0)), bk(0.60, (44, 0, 0)), bk(1.0, (0, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home", (-0.4, 0.0, 0.0)),
            rk(0.24, "home", (0.5, 0.0, 0.0)),
            rk(0.46, "target", (-3.5, 3.0, 0.0), ease="ease_out_cubic"),
            rk(0.60, "target", (-1.55, 0.0, 0.0), ease="ease_in_cubic"),
            rk(0.80, "target", (-1.55, 0.0, 0.0)),
            rk(1.0, "home", ease="ease_out_cubic"),
        ],
        "camera": [
            ck(0.0, "actor", (2.6, 1.55, 2.7), "actor", (0, 1.45, 0), 40, -4.0, local=True),
            ck(0.28, "actor", (2.2, 1.35, -2.9), "actor", (0.3, 1.5, 0), 42, 3.5, local=True),
            ck(0.48, "target", (-4.8, 4.2, 6.2), "target", (0, 1.2, 0), 50, -3.0, local=True),
            ck(0.64, "target", (-3.4, 2.1, 6.8), "target", (0, 0.9, 0), 56, 2.5, local=True),
            ck(0.82, "arena_center", (-2.0, 5.2, 9.5), "target", (0, 1.0, 0), 54, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.05, "sound", "actor", sound="beacon", pitch=1.15),
            ev(0.38, "sound", "actor", sound="sweep", pitch=0.8),
            ev(0.60, "impact", "target", vfx="earth_cleave", particle="explosion", sound="explode", count=14, scale=1.8, shake=0.72, hit_stop=4),
            ev(0.74, "vfx", "all_enemies", vfx="shockwave_ring", particle="lava", sound="anvil", pitch=0.85, count=12, scale=1.45, shake=0.45),
        ],
    })

    save_clip({
        "id": "linye_ready",
        "durationTicks": 30,
        "loop": True,
        "bones": {
            "torso": [bk(0.0, (12, -20, 0)), bk(0.5, (16, -24, 0)), bk(1.0, (12, -20, 0))],
            "right_arm": [bk(0.0, (-125, -10, 16)), bk(0.5, (-132, -12, 18)), bk(1.0, (-125, -10, 16))],
            "left_arm": [bk(0.0, (-55, 20, -12)), bk(1.0, (-55, 20, -12))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    # ==================== CHEN FENG (Air / Dual Spin Blade) ====================
    save_clip({
        "id": "chenfeng_basic",
        "durationTicks": 17,
        "bones": {
            "torso": [bk(0.0, (6, 0, 0)), bk(0.32, (20, -35, 0)), bk(0.55, (22, 40, 0), ease="ease_out_cubic"), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-30, 0, 14)), bk(0.30, (-92, 35, 35)), bk(0.55, (-95, -45, -25)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (18, 0, -14)), bk(0.45, (42, 0, -35)), bk(1.0, (12, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.34, "target", (-1.35, 0.2, 0.0), yaw=180.0, ease="ease_out_cubic"),
            rk(0.62, "target", (-1.45, 0.0, 0.0), yaw=360.0),
            rk(1.0, "home", yaw=360.0, ease="ease_out_cubic"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.40, "target", (-2.4, 2.6, 5.0), "target", (0, 1.1, 0), 47, -2.5, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.22, "sound", "actor", sound="sweep", pitch=1.3),
            ev(0.48, "impact", "target", vfx="slash_arc", particle="cloud", sound="crit", count=12, scale=1.1, shake=0.22, hit_stop=1),
        ],
    })

    save_clip({
        "id": "chenfeng_skill",
        "durationTicks": 26,
        "bones": {
            "torso": [bk(0.0, (8, 0, 0)), bk(0.45, (18, 0, 0)), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-25, 0, 12)), bk(0.25, (-90, 0, 68)), bk(0.75, (-90, 0, 68)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (15, 0, -12)), bk(0.25, (-80, 0, -65)), bk(0.75, (-80, 0, -65)), bk(1.0, (12, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.28, "target", (-0.6, 0.45, 0.0), yaw=180.0, ease="ease_out_quad"),
            rk(0.70, "target", (-0.6, 0.35, 0.0), yaw=720.0, ease="linear"),
            rk(1.0, "home", yaw=720.0, ease="ease_out_cubic"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.45, "target", (-3.5, 4.4, 7.6), "target", (0, 1.1, 0), 55, 3.0, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.30, "vfx", "all_enemies", vfx="wind_vortex", particle="cloud", sound="sweep", pitch=1.4, count=14, scale=1.15),
            ev(0.55, "impact", "all_enemies", vfx="wind_vortex", particle="enchanted_hit", sound="strong_hit", count=14, scale=1.25, shake=0.35, hit_stop=2),
        ],
    })

    save_clip({
        "id": "chenfeng_followup",
        "durationTicks": 22,
        "bones": {
            "torso": [bk(0.0, (24, 0, 0)), bk(0.5, (28, 0, 0)), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-90, 0, 75)), bk(0.75, (-90, 0, 75)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (-85, 0, -70)), bk(0.75, (-85, 0, -70)), bk(1.0, (12, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.28, "target", (0.2, 0.85, -2.0), yaw=360.0, ease="ease_out_cubic"),
            rk(0.64, "target", (0.2, 0.45, 2.0), yaw=900.0, ease="ease_in_out_quad"),
            rk(1.0, "home", yaw=1080.0, ease="ease_out_cubic"),
        ],
        "camera": [
            ck(0.0, "target", (-4.2, 3.8, 7.2), "target", (0, 1.1, 0), 52, -3.5, local=True),
            ck(0.62, "target", (-2.8, 3.2, 6.8), "target", (0, 1.1, 0), 55, 2.5, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.24, "vfx", "all_enemies", vfx="wind_vortex", sound="sweep", pitch=1.55, scale=1.1),
            ev(0.48, "impact", "all_enemies", vfx="claw_slash", particle="enchanted_hit", sound="crit", count=14, scale=1.3, shake=0.35, hit_stop=2),
        ],
    })

    save_clip({
        "id": "chenfeng_ult",
        "durationTicks": 42,
        "bannerStyle": "ULT",
        "bones": {
            "torso": [bk(0.0, (15, -30, 0)), bk(0.32, (28, 0, 0)), bk(0.75, (20, 0, 0)), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-110, 0, 45)), bk(0.35, (-95, 0, 82)), bk(0.80, (-95, 0, 82)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (-95, 0, -45)), bk(0.35, (-95, 0, -82)), bk(0.80, (-95, 0, -82)), bk(1.0, (12, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.26, "home", (-0.8, 0.0, 0.0)),
            rk(0.45, "target", (-1.5, 2.1, 0.0), yaw=360.0, ease="ease_out_cubic"),
            rk(0.76, "target", (0.3, 0.5, 0.0), yaw=1080.0, ease="ease_in_out_quad"),
            rk(1.0, "home", yaw=1080.0, ease="ease_out_cubic"),
        ],
        "camera": [
            ck(0.0, "actor", (2.4, 1.4, 2.4), "actor", (0, 1.4, 0), 40, 4.0, local=True),
            ck(0.28, "actor", (2.9, 1.8, -2.5), "actor", (0, 1.4, 0), 44, -3.0, local=True),
            ck(0.50, "target", (-4.8, 4.0, 6.2), "target", (0, 1.1, 0), 54, -4.0, local=True),
            ck(0.75, "target", (-2.6, 5.2, 8.2), "target", (0, 1.2, 0), 58, 3.0, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.06, "sound", "actor", sound="beacon", pitch=1.35),
            ev(0.44, "vfx", "all_enemies", vfx="wind_vortex", particle="cloud", sound="sweep", pitch=1.5, count=18, scale=1.5),
            ev(0.65, "impact", "all_enemies", vfx="wind_vortex", particle="enchanted_hit", sound="explode", pitch=1.3, count=18, scale=1.85, shake=0.65, hit_stop=3),
        ],
    })

    save_clip({
        "id": "chenfeng_ready",
        "durationTicks": 24,
        "loop": True,
        "bones": {
            "torso": [bk(0.0, (18, 25, 0)), bk(0.5, (22, 28, 0)), bk(1.0, (18, 25, 0))],
            "right_arm": [bk(0.0, (-88, -32, 35)), bk(0.5, (-95, -36, 40)), bk(1.0, (-88, -32, 35))],
            "left_arm": [bk(0.0, (25, 0, -35)), bk(1.0, (25, 0, -35))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    # ==================== SU QING (Water / Nano Mist Healer) ====================
    save_clip({
        "id": "suqing_basic",
        "durationTicks": 20,
        "bones": {
            "torso": [bk(0.0, (2, 0, 0)), bk(0.28, (5, 16, 0)), bk(0.52, (-4, 16, 0)), bk(1.0, (2, 0, 0))],
            "right_arm": [bk(0.0, (-22, 0, 8)), bk(0.28, (-92, -10, 0)), bk(0.52, (-105, -10, 0)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (10, 0, -8)), bk(0.30, (-65, 20, -10)), bk(1.0, (10, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.25, "home", (1.2, 0.0, 0.0)),
            rk(0.70, "home", (1.2, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.38, "arena_center", (-1.5, 4.2, 8.8), "target", (-1.5, 1.0, 0), 50, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.24, "sound", "actor", sound="shot", pitch=1.35),
            ev(0.45, "impact", "target", vfx="water_wave", particle="splash", sound="shot", pitch=1.1, count=14, scale=1.0, shake=0.20, hit_stop=1),
        ],
    })

    save_clip({
        "id": "suqing_skill",
        "durationTicks": 26,
        "bones": {
            "torso": [bk(0.0, (2, 0, 0)), bk(0.40, (-12, 0, 0)), bk(1.0, (2, 0, 0))],
            "right_arm": [bk(0.0, (-20, 0, 8)), bk(0.40, (-145, 0, 22)), bk(1.0, (-20, 0, 8))],
            "left_arm": [bk(0.0, (10, 0, -8)), bk(0.40, (-145, 0, -22)), bk(1.0, (10, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.35, "home", (1.4, 0.0, 0.0)),
            rk(0.75, "home", (1.4, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.40, "arena_center", (0.5, 3.8, 9.2), "arena_center", (-3.5, 1.2, 0), 48, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.38, "impact", "all_allies", vfx="heal_pillar", particle="heart", sound="heal", pitch=1.2, count=8, scale=1.15),
        ],
    })

    save_clip({
        "id": "suqing_ult",
        "durationTicks": 42,
        "bannerStyle": "ULT",
        "bones": {
            "torso": [bk(0.0, (4, 15, 0)), bk(0.30, (-14, 0, 0)), bk(0.65, (-16, 0, 0)), bk(1.0, (2, 0, 0))],
            "right_arm": [bk(0.0, (-100, 0, 10)), bk(0.45, (-158, 0, 28)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (-95, 0, -10)), bk(0.45, (-158, 0, -28)), bk(1.0, (10, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.35, "home", (1.8, 0.35, 0.0)),
            rk(0.76, "home", (1.8, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "actor", (2.5, 1.45, 2.4), "actor", (0, 1.45, 0), 40, -2.5, local=True),
            ck(0.35, "actor", (3.2, 1.7, -2.2), "actor", (0, 1.35, 0), 42, 2.0, local=True),
            ck(0.62, "arena_center", (1.5, 3.6, 8.8), "arena_center", (-4.2, 1.2, 0), 50, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.08, "sound", "actor", sound="beacon", pitch=1.4),
            ev(0.55, "impact", "all_allies", vfx="heal_pillar", particle="bubble", sound="heal", pitch=1.3, count=14, scale=1.35),
            ev(0.66, "vfx", "all_allies", vfx="shield_dome", particle="heart", count=8, scale=1.0),
        ],
    })

    save_clip({
        "id": "suqing_ready",
        "durationTicks": 28,
        "loop": True,
        "bones": {
            "torso": [bk(0.0, (4, 12, 0)), bk(0.5, (6, 14, 0)), bk(1.0, (4, 12, 0))],
            "right_arm": [bk(0.0, (-94, -8, 4)), bk(0.5, (-98, -10, 4)), bk(1.0, (-94, -8, 4))],
            "left_arm": [bk(0.0, (-72, 18, -12)), bk(1.0, (-72, 18, -12))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    # ==================== LAO ZHENG (Fire / Heavy Hammer & Shielder) ====================
    save_clip({
        "id": "laozheng_basic",
        "durationTicks": 22,
        "bones": {
            "torso": [bk(0.0, (4, 0, 0)), bk(0.32, (6, -34, 0)), bk(0.56, (26, 34, 0), ease="ease_out_cubic"), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-28, 0, 12)), bk(0.32, (-125, -30, 38)), bk(0.56, (-38, 25, -18), ease="ease_out_cubic"), bk(1.0, (-28, 0, 12))],
            "weapon": [bk(0.0, (0, 0, 0)), bk(0.56, (35, 0, 0)), bk(1.0, (0, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.34, "target", (-1.65, 0.0, 0.0), ease="ease_out_cubic"),
            rk(0.68, "target", (-1.65, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.45, "target", (-2.6, 2.7, 5.2), "target", (0, 1.1, 0), 47, 1.5, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.24, "sound", "actor", sound="sweep", pitch=0.72),
            ev(0.50, "impact", "target", vfx="earth_cleave", particle="flame", sound="anvil", pitch=0.9, count=14, scale=1.1, shake=0.32, hit_stop=2),
        ],
    })

    save_clip({
        "id": "laozheng_skill",
        "durationTicks": 28,
        "bones": {
            "torso": [bk(0.0, (4, 0, 0)), bk(0.32, (-16, 0, 0)), bk(0.54, (28, 0, 0), ease="ease_out_cubic"), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-30, 0, 10)), bk(0.32, (-165, 0, 12)), bk(0.54, (-25, 0, -10), ease="ease_out_cubic"), bk(1.0, (-30, 0, 10))],
            "left_arm": [bk(0.0, (12, 0, -10)), bk(0.32, (-150, 0, -12)), bk(0.54, (-25, 0, 10)), bk(1.0, (12, 0, -10))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.30, "home", (1.6, 0.4, 0.0)),
            rk(0.54, "home", (1.6, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.45, "arena_center", (0.5, 3.6, 9.2), "arena_center", (-3.8, 1.1, 0), 48, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.52, "impact", "all_allies", vfx="shield_dome", particle="flame", sound="shield_block", pitch=0.95, count=10, scale=1.15, shake=0.35, hit_stop=2),
        ],
    })

    save_clip({
        "id": "laozheng_ult",
        "durationTicks": 44,
        "bannerStyle": "ULT",
        "bones": {
            "torso": [bk(0.0, (8, -22, 0)), bk(0.32, (-25, 0, 0)), bk(0.60, (40, 0, 0), ease="ease_out_cubic"), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-110, 0, 15)), bk(0.36, (-175, 0, 10)), bk(0.60, (-20, 0, -10)), bk(1.0, (-28, 0, 10))],
            "left_arm": [bk(0.0, (-90, 0, -15)), bk(0.36, (-168, 0, -10)), bk(0.60, (-20, 0, 10)), bk(1.0, (12, 0, -10))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.28, "home", (0.6, 0.0, 0.0)),
            rk(0.46, "arena_center", (-1.5, 2.2, 0.0), ease="ease_out_cubic"),
            rk(0.60, "arena_center", (0.0, 0.0, 0.0), ease="ease_in_cubic"),
            rk(0.82, "arena_center", (0.0, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "actor", (2.6, 1.35, 2.5), "actor", (0, 1.4, 0), 40, -3.5, local=True),
            ck(0.34, "actor", (3.1, 1.85, -2.5), "actor", (0, 1.4, 0), 44, 3.0, local=True),
            ck(0.58, "arena_center", (-2.0, 3.4, 8.6), "arena_center", (1.5, 0.8, 0), 54, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.08, "sound", "actor", sound="anvil", pitch=0.75),
            ev(0.58, "impact", "all_enemies", vfx="shockwave_ring", particle="lava", sound="explode", count=18, scale=1.9, shake=0.7, hit_stop=4),
            ev(0.66, "vfx", "all_allies", vfx="shield_dome", sound="shield_block", pitch=1.1, scale=1.15),
        ],
    })

    save_clip({
        "id": "laozheng_ready",
        "durationTicks": 30,
        "loop": True,
        "bones": {
            "torso": [bk(0.0, (10, -15, 0)), bk(0.5, (14, -18, 0)), bk(1.0, (10, -15, 0))],
            "right_arm": [bk(0.0, (-85, 25, 20)), bk(1.0, (-85, 25, 20))],
            "left_arm": [bk(0.0, (-78, -22, -22)), bk(1.0, (-78, -22, -22))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    # ==================== HAN BING (Fire / Long-Range Sniper) ====================
    save_clip({
        "id": "hanbing_basic",
        "durationTicks": 19,
        "bones": {
            "torso": [bk(0.0, (4, 15, 0)), bk(0.32, (6, 28, 0)), bk(0.45, (-8, 28, 0)), bk(1.0, (4, 15, 0))],
            "right_arm": [bk(0.0, (-22, 0, 8)), bk(0.32, (-92, 0, 0)), bk(0.45, (-112, 0, 4)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (10, 0, -8)), bk(0.32, (-88, 22, 0)), bk(0.45, (-104, 22, 0)), bk(1.0, (10, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.42, "home", (-0.35, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.38, "arena_center", (-1.0, 3.8, 8.8), "target", (-1.5, 1.1, 0), 48, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.34, "impact", "target", vfx="sniper_tracer", particle="flame", sound="blaze", pitch=1.35, count=12, scale=1.0, shake=0.22, hit_stop=1),
        ],
    })

    save_clip({
        "id": "hanbing_skill",
        "durationTicks": 26,
        "bones": {
            "torso": [bk(0.0, (8, 24, 0)), bk(0.45, (12, 32, 0)), bk(0.58, (-12, 32, 0)), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-90, 0, 0)), bk(0.48, (-94, -4, 0)), bk(0.58, (-120, -4, 4)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (-85, 24, 0)), bk(0.48, (-90, 26, 0)), bk(0.58, (-110, 26, 0)), bk(1.0, (10, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.45, "home", (-0.5, -0.15, 0.0)),
            rk(0.62, "home", (-1.0, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.32, "actor", (-2.2, 1.75, 2.2), "target", (0, 1.15, 0), 40, -1.5, local=True),
            ck(0.68, "target", (-3.2, 2.2, 4.8), "target", (0, 1.1, 0), 44, 0.0, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.20, "sound", "actor", sound="beacon", pitch=1.7, volume=0.7),
            ev(0.50, "impact", "target", vfx="sniper_tracer", particle="explosion", sound="explode", pitch=1.35, count=16, scale=1.5, shake=0.45, hit_stop=3),
        ],
    })

    save_clip({
        "id": "hanbing_ult",
        "durationTicks": 44,
        "bannerStyle": "ULT",
        "bones": {
            "torso": [bk(0.0, (6, 28, 0)), bk(0.46, (10, 34, 0)), bk(0.58, (-18, 34, 0)), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-92, 0, 0)), bk(0.48, (-94, -4, 0)), bk(0.58, (-132, -4, 6)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (-88, 25, 0)), bk(0.48, (-90, 26, 0)), bk(0.58, (-122, 26, 0)), bk(1.0, (10, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.46, "home", (-0.6, 0.0, 0.0)),
            rk(0.60, "home", (-1.45, 0.0, 0.0), ease="ease_out_cubic"),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "actor", (2.5, 1.5, 2.1), "actor", (0, 1.45, 0), 38, -4.0, local=True),
            ck(0.32, "actor", (-2.2, 1.7, 2.2), "target", (0, 1.15, 0), 42, -2.0, local=True),
            ck(0.54, "target", (-3.8, 2.0, 4.6), "target", (0, 1.1, 0), 45, 0.0, local=True),
            ck(0.76, "target", (-3.0, 2.6, 6.2), "target", (0, 1.2, 0), 56, 2.5, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.10, "sound", "actor", sound="beacon", pitch=1.6),
            ev(0.52, "vfx", "target", vfx="fire_beam", particle="flame", sound="blaze", pitch=1.1, count=18, scale=1.65, shake=0.45, hit_stop=2),
            ev(0.64, "impact", "target", vfx="earth_cleave", particle="explosion", sound="explode", pitch=0.95, count=20, scale=1.9, shake=0.78, hit_stop=4),
        ],
    })

    save_clip({
        "id": "hanbing_ready",
        "durationTicks": 30,
        "loop": True,
        "bones": {
            "torso": [bk(0.0, (8, 32, 0)), bk(0.5, (10, 34, 0)), bk(1.0, (8, 32, 0))],
            "right_arm": [bk(0.0, (-94, -4, 0)), bk(1.0, (-94, -4, 0))],
            "left_arm": [bk(0.0, (-88, 24, 0)), bk(1.0, (-88, 24, 0))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })

    # ==================== ZHOU MIN (Water / Tactical Command Buffer) ====================
    save_clip({
        "id": "zhoumin_basic",
        "durationTicks": 19,
        "bones": {
            "torso": [bk(0.0, (4, 14, 0)), bk(0.32, (8, 22, 0)), bk(0.55, (-4, 22, 0)), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-22, 0, 8)), bk(0.32, (-96, -8, 0)), bk(0.55, (-110, -8, 4)), bk(1.0, (-22, 0, 8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.32, "home", (1.3, 0.0, 0.5)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.42, "arena_center", (-1.0, 4.0, 8.8), "target", (-1.0, 1.1, 0), 50, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.36, "impact", "target", vfx="water_wave", particle="splash", sound="shot", pitch=1.25, count=12, scale=1.05, shake=0.2, hit_stop=1),
        ],
    })

    save_clip({
        "id": "zhoumin_skill",
        "durationTicks": 26,
        "bones": {
            "torso": [bk(0.0, (4, 0, 0)), bk(0.38, (-8, -18, 0)), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-22, 0, 8)), bk(0.38, (-125, -18, 12)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (12, 0, -8)), bk(0.38, (-98, 20, -18)), bk(1.0, (12, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.34, "home", (1.6, 0.0, 0.0)),
            rk(0.75, "home", (1.6, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.45, "arena_center", (0.5, 3.8, 9.2), "arena_center", (-3.5, 1.1, 0), 48, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.42, "impact", "all_allies", vfx="buff_aura", particle="end_rod", sound="beacon", pitch=1.35, count=10, scale=1.15),
        ],
    })

    save_clip({
        "id": "zhoumin_ult",
        "durationTicks": 42,
        "bannerStyle": "ULT",
        "bones": {
            "torso": [bk(0.0, (4, -18, 0)), bk(0.34, (-12, 12, 0)), bk(1.0, (4, 0, 0))],
            "right_arm": [bk(0.0, (-95, 0, 10)), bk(0.44, (-148, -18, 20)), bk(1.0, (-22, 0, 8))],
            "left_arm": [bk(0.0, (-80, 0, -10)), bk(0.44, (-142, 18, -20)), bk(1.0, (10, 0, -8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.32, "home", (1.9, 0.4, 0.0)),
            rk(0.78, "home", (1.9, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "actor", (2.5, 1.5, 2.5), "actor", (0, 1.45, 0), 41, 3.0, local=True),
            ck(0.34, "actor", (2.8, 1.8, -2.5), "actor", (0, 1.35, 0), 44, -2.5, local=True),
            ck(0.62, "arena_center", (1.8, 3.9, 9.0), "arena_center", (-4.2, 1.1, 0), 50, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.10, "sound", "actor", sound="beacon", pitch=1.5),
            ev(0.55, "impact", "all_allies", vfx="buff_aura", particle="end_rod", sound="heal", pitch=1.25, count=14, scale=1.45, shake=0.25),
            ev(0.68, "vfx", "all_allies", vfx="water_wave", particle="bubble", count=10, scale=1.15),
        ],
    })

    save_clip({
        "id": "zhoumin_ready",
        "durationTicks": 28,
        "loop": True,
        "bones": {
            "torso": [bk(0.0, (4, -14, 0)), bk(0.5, (6, -16, 0)), bk(1.0, (4, -14, 0))],
            "right_arm": [bk(0.0, (-110, -14, 8)), bk(0.5, (-118, -16, 10)), bk(1.0, (-110, -14, 8))],
            "left_arm": [bk(0.0, (16, 0, -12)), bk(1.0, (16, 0, -12))],
        },
        "rootMotion": [], "camera": [], "events": [],
    })


def build_enemy_clips():
    # ==================== CLEANER BOT ====================
    save_clip({
        "id": "cleaner_ram",
        "durationTicks": 20,
        "bones": {
            "body": [bk(0.0, (0, 0, 0)), bk(0.35, (14, 0, 0)), bk(1.0, (0, 0, 0))],
            "brush_l": [bk(0.0, (0, 0, 0), ease="linear"), bk(1.0, (0, 900, 0), ease="linear")],
            "brush_r": [bk(0.0, (0, 0, 0), ease="linear"), bk(1.0, (0, -900, 0), ease="linear")],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.42, "target", (-1.4, 0.0, 0.0), ease="ease_in_cubic"),
            rk(0.68, "target", (-1.4, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.45, "target", (2.5, 3.0, 5.2), "target", (0, 0.9, 0), 48, 0.0, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.18, "sound", "actor", sound="piston", pitch=1.1),
            ev(0.44, "impact", "target", vfx="shockwave_ring", particle="smoke", sound="mech_slam", count=10, scale=0.95, shake=0.3, hit_stop=2),
        ],
    })

    save_clip({
        "id": "cleaner_spray",
        "durationTicks": 24,
        "bones": {
            "body": [bk(0.0, (0, 0, 0)), bk(0.35, (-10, -25, 0)), bk(0.65, (-10, 25, 0)), bk(1.0, (0, 0, 0))],
            "brush_l": [bk(0.0, (0, 0, 0), ease="linear"), bk(1.0, (0, 720, 0), ease="linear")],
            "brush_r": [bk(0.0, (0, 0, 0), ease="linear"), bk(1.0, (0, -720, 0), ease="linear")],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.30, "target", (-2.2, 0.0, 0.0), yaw=-28.0),
            rk(0.66, "target", (-2.2, 0.0, 0.0), yaw=28.0),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.48, "arena_center", (-1.5, 4.2, 9.0), "target", (0, 1.0, 0), 50, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.44, "impact", "target", vfx="water_wave", particle="splash", sound="shot", pitch=0.95, count=14, scale=1.25, shake=0.28, hit_stop=1),
        ],
    })

    # ==================== SECURITY DRONE ====================
    save_clip({
        "id": "drone_pulse",
        "durationTicks": 20,
        "bones": {
            "body": [bk(0.0, (0, 0, 0), (0, -2, 0)), bk(0.35, (18, 0, 0), (0, -3.5, 0)), bk(0.55, (-12, 0, 0), (0, -2.5, 0)), bk(1.0, (0, 0, 0), (0, -2, 0))],
            "ring": [bk(0.0, (0, 0, 0), (0, -2, 0), ease="linear"), bk(1.0, (0, 540, 0), (0, -2, 0), ease="linear")],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.32, "home", (1.6, 0.35, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.44, "arena_center", (0.8, 4.0, 8.8), "target", (1.2, 1.1, 0), 50, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.36, "impact", "target", vfx="pulse_bolts", particle="electric", sound="blaze", pitch=1.35, count=12, scale=1.0, shake=0.22, hit_stop=1),
        ],
    })

    save_clip({
        "id": "drone_scan_lock",
        "durationTicks": 24,
        "bones": {
            "body": [bk(0.0, (0, 0, 0), (0, -3, 0)), bk(0.5, (14, 0, 0), (0, -7, 0)), bk(1.0, (0, 0, 0), (0, -3, 0))],
            "ring": [bk(0.0, (0, 0, 0), (0, -3, 0), ease="linear"), bk(1.0, (0, 1080, 0), (0, -7, 0), ease="linear")],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.45, "home", (0.8, 0.75, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.45, "actor", (2.4, 2.2, -3.5), "target", (0, 1.1, 0), 48, 0.0, local=True),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.24, "sound", "actor", sound="beacon", pitch=1.65),
            ev(0.50, "impact", "target", vfx="sniper_tracer", particle="flame", sound="shot", pitch=1.45, count=12, scale=1.1, shake=0.25, hit_stop=1),
        ],
    })

    save_clip({
        "id": "drone_sweep",
        "durationTicks": 28,
        "bones": {
            "body": [bk(0.0, (12, 0, 0), (0, -3, 0)), bk(0.5, (22, 0, -18), (0, -4, 0)), bk(1.0, (0, 0, 0), (0, -2, 0))],
            "ring": [bk(0.0, (0, 0, 0), (0, -3, 0), ease="linear"), bk(1.0, (0, 900, 0), (0, -3, 0), ease="linear")],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.32, "target", (-1.8, 0.65, -2.8), yaw=35.0),
            rk(0.66, "target", (-1.8, 0.65, 2.8), yaw=-35.0),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.48, "arena_center", (1.2, 4.6, 9.2), "target", (0, 1.0, 0), 54, -1.5),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.36, "vfx", "all_allies", vfx="pulse_bolts", particle="smoke", sound="wither_shoot", pitch=1.3, count=8, scale=0.95),
            ev(0.60, "impact", "all_allies", vfx="pulse_bolts", particle="flame", sound="blast", count=10, scale=1.1, shake=0.35, hit_stop=2),
        ],
    })

    # ==================== MECH HOUND ====================
    save_clip({
        "id": "hound_pounce",
        "durationTicks": 22,
        "bones": {
            "body": [bk(0.0, (0, 0, 0)), bk(0.24, (-14, 0, 0)), bk(0.48, (22, 0, 0)), bk(1.0, (0, 0, 0))],
            "jaw": [bk(0.0, (0, 0, 0)), bk(0.35, (-32, 0, 0)), bk(0.52, (16, 0, 0)), bk(1.0, (0, 0, 0))],
            "leg_fl": [bk(0.0, (0, 0, 0)), bk(0.38, (-55, 0, 0)), bk(1.0, (0, 0, 0))],
            "leg_fr": [bk(0.0, (0, 0, 0)), bk(0.38, (-55, 0, 0)), bk(1.0, (0, 0, 0))],
            "leg_bl": [bk(0.0, (0, 0, 0)), bk(0.38, (55, 0, 0)), bk(1.0, (0, 0, 0))],
            "leg_br": [bk(0.0, (0, 0, 0)), bk(0.38, (55, 0, 0)), bk(1.0, (0, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.22, "home", (-0.6, -0.15, 0.0)),
            rk(0.42, "arena_center", (0.5, 1.55, 0.0), ease="ease_out_quad"),
            rk(0.54, "target", (-1.45, 0.1, 0.0), ease="ease_in_cubic"),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.48, "arena_center", (-1.5, 3.6, 8.2), "target", (0, 1.0, 0), 48, 2.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.30, "sound", "actor", sound="bite", pitch=1.1),
            ev(0.52, "impact", "target", vfx="claw_slash", particle="flame", sound="bite", pitch=0.85, count=14, scale=1.25, shake=0.35, hit_stop=2),
        ],
    })

    save_clip({
        "id": "hound_tail_sweep",
        "durationTicks": 25,
        "bones": {
            "tail": [bk(0.0, (0, 0, 0)), bk(0.48, (18, 85, 0)), bk(1.0, (0, 0, 0))],
            "jaw": [bk(0.0, (-10, 0, 0)), bk(0.5, (-28, 0, 0)), bk(1.0, (0, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.32, "target", (-1.5, 0.3, 0.0), yaw=0.0),
            rk(0.65, "target", (-1.5, 0.0, 0.0), yaw=360.0),
            rk(1.0, "home", yaw=360.0),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.50, "arena_center", (-1.2, 4.0, 8.8), "target", (0, 1.0, 0), 50, -2.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.48, "impact", "target", vfx="slash_arc", particle="crit", sound="sweep", pitch=1.2, count=16, scale=1.35, shake=0.38, hit_stop=2),
        ],
    })

    # ==================== HEAVY GUARD / COMMANDER MECH ====================
    save_clip({
        "id": "guard_shield_bash",
        "durationTicks": 24,
        "bones": {
            "torso": [bk(0.0, (6, 0, 0)), bk(0.32, (12, 22, 0)), bk(0.54, (28, -28, 0)), bk(1.0, (4, 0, 0))],
            "arm_l": [bk(0.0, (-22, 10, -8)), bk(0.45, (-95, 25, 10)), bk(1.0, (-22, 10, -8))],
            "arm_r": [bk(0.0, (-34, 0, 8)), bk(0.45, (24, 0, 16)), bk(1.0, (-34, 0, 8))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.40, "target", (-1.55, 0.0, 0.0), ease="ease_in_cubic"),
            rk(0.70, "target", (-1.55, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.46, "arena_center", (-1.5, 3.6, 8.4), "target", (0, 1.1, 0), 47, 1.5),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.24, "sound", "actor", sound="mech_slam", pitch=0.85),
            ev(0.48, "impact", "target", vfx="shockwave_ring", particle="crit", sound="anvil", pitch=0.95, count=14, scale=1.2, shake=0.42, hit_stop=2),
        ],
    })

    save_clip({
        "id": "guard_shockwave",
        "durationTicks": 30,
        "bones": {
            "torso": [bk(0.0, (6, 0, 0)), bk(0.32, (-20, 0, 0)), bk(0.55, (32, 0, 0), ease="ease_out_cubic"), bk(1.0, (4, 0, 0))],
            "arm_l": [bk(0.0, (-22, 0, -10)), bk(0.32, (-150, 0, -18)), bk(0.55, (-12, 0, -10)), bk(1.0, (-22, 0, -10))],
            "arm_r": [bk(0.0, (-34, 0, 10)), bk(0.32, (-150, 0, 18)), bk(0.55, (-12, 0, 10)), bk(1.0, (-34, 0, 10))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.30, "home", (1.8, 0.85, 0.0), ease="ease_out_quad"),
            rk(0.54, "home", (2.4, 0.0, 0.0), ease="ease_in_cubic"),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.42, "arena_center", (0.5, 4.6, 9.6), "arena_center", (-1.5, 1.0, 0), 56, -2.5),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.52, "impact", "all_allies", vfx="shockwave_ring", particle="explosion", sound="explode", count=14, scale=1.55, shake=0.6, hit_stop=3),
        ],
    })

    save_clip({
        "id": "guard_overclock",
        "durationTicks": 26,
        "bones": {
            "torso": [bk(0.0, (0, 0, 0)), bk(0.45, (-22, 0, 0)), bk(1.0, (4, 0, 0))],
            "arm_l": [bk(0.0, (-20, 0, -10)), bk(0.45, (-110, 0, -45)), bk(1.0, (-20, 0, -10))],
            "arm_r": [bk(0.0, (-20, 0, 10)), bk(0.45, (-110, 0, 45)), bk(1.0, (-20, 0, 10))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.45, "home", (0.8, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.44, "arena_center", (2.2, 4.0, 8.8), "actor", (0, 1.3, 0), 48, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.32, "vfx", "actor", vfx="shield_dome", particle="flame", sound="beacon", pitch=0.85, count=14, scale=1.15),
            ev(0.56, "impact", "all_allies", vfx="shockwave_ring", particle="electric", sound="blast", count=10, scale=1.15, shake=0.35),
        ],
    })

    # ==================== MINE DRILLER ====================
    save_clip({
        "id": "driller_pierce",
        "durationTicks": 25,
        "bones": {
            "drill": [bk(0.0, (0, 0, 0), ease="linear"), bk(1.0, (0, 0, 1440), ease="linear")],
            "body": [bk(0.0, (0, 0, 0)), bk(0.35, (14, 0, 0)), bk(1.0, (0, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.22, "home", (-0.8, 0.0, 0.0)),
            rk(0.46, "target", (-1.45, 0.0, 0.0), ease="ease_in_cubic"),
            rk(0.72, "target", (-1.45, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.48, "arena_center", (-1.5, 3.8, 8.5), "target", (0, 1.0, 0), 48, -2.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.22, "sound", "actor", sound="piston", pitch=0.8),
            ev(0.48, "impact", "target", vfx="drill_burst", particle="crit", sound="anvil", pitch=1.15, count=18, scale=1.35, shake=0.48, hit_stop=3),
        ],
    })

    save_clip({
        "id": "driller_quake",
        "durationTicks": 30,
        "bones": {
            "drill": [bk(0.0, (0, 0, 0), ease="linear"), bk(1.0, (0, 0, 1800), ease="linear")],
            "body": [bk(0.0, (0, 0, 0)), bk(0.35, (38, 0, 0)), bk(0.75, (38, 0, 0)), bk(1.0, (0, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.32, "home", (2.2, 0.45, 0.0)),
            rk(0.55, "home", (2.2, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.52, "arena_center", (0.2, 4.8, 9.6), "arena_center", (-2.0, 0.9, 0), 55, 2.5),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.50, "impact", "all_allies", vfx="earth_cleave", particle="explosion", sound="explode", pitch=0.85, count=14, scale=1.45, shake=0.62, hit_stop=3),
        ],
    })

    # ==================== EXCAVATOR BOSS (LEFT ARM / RIGHT ARM / CORE) ====================
    save_clip({
        "id": "boss_arm_l_scoop",
        "durationTicks": 26,
        "bones": {
            "arm_l": [bk(0.0, (-20, 0, 0)), bk(0.34, (-115, 25, -20)), bk(0.58, (25, -45, 10), ease="ease_out_cubic"), bk(1.0, (-20, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.38, "target", (-1.6, 0.2, 0.0), ease="ease_out_cubic"),
            rk(0.70, "target", (-1.6, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.48, "arena_center", (-1.2, 4.0, 8.8), "target", (0, 1.0, 0), 50, -1.5),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.50, "impact", "target", vfx="claw_slash", particle="crit", sound="mech_slam", count=16, scale=1.45, shake=0.45, hit_stop=2),
        ],
    })

    save_clip({
        "id": "boss_arm_l_crush",
        "durationTicks": 26,
        "bones": {
            "arm_l": [bk(0.0, (-20, 0, 0)), bk(0.32, (-158, 0, 0)), bk(0.54, (10, 0, 0), ease="ease_in_cubic"), bk(1.0, (-20, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.32, "target", (-2.2, 1.65, 0.0), ease="ease_out_quad"),
            rk(0.52, "target", (-1.4, 0.0, 0.0), ease="ease_in_cubic"),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.48, "arena_center", (-1.8, 3.8, 8.6), "target", (0, 0.9, 0), 48, 2.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.52, "impact", "target", vfx="earth_cleave", particle="explosion", sound="anvil", pitch=0.8, count=16, scale=1.45, shake=0.55, hit_stop=3),
        ],
    })

    save_clip({
        "id": "boss_arm_r_hammer",
        "durationTicks": 26,
        "bones": {
            "arm_r": [bk(0.0, (-25, 0, 0)), bk(0.34, (-165, 0, 8)), bk(0.54, (14, 0, 0), ease="ease_in_cubic"), bk(1.0, (-25, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.36, "target", (-1.6, 1.2, 0.0)),
            rk(0.54, "target", (-1.45, 0.0, 0.0), ease="ease_in_cubic"),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.48, "arena_center", (-1.8, 3.8, 8.6), "target", (0, 0.9, 0), 48, -2.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.52, "impact", "target", vfx="shockwave_ring", particle="lava", sound="anvil", pitch=0.75, count=16, scale=1.45, shake=0.55, hit_stop=3),
        ],
    })

    save_clip({
        "id": "boss_arm_r_tremor",
        "durationTicks": 32,
        "bones": {
            "arm_r": [bk(0.0, (-25, 0, 0)), bk(0.26, (-150, 0, 0)), bk(0.42, (10, 0, 0)), bk(0.60, (-150, 0, 0)), bk(0.76, (10, 0, 0)), bk(1.0, (-25, 0, 0))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.28, "home", (2.8, 0.9, 0.0)),
            rk(0.42, "home", (2.8, 0.0, 0.0)),
            rk(0.76, "home", (2.8, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.50, "arena_center", (0.2, 4.8, 9.6), "arena_center", (-2.0, 1.0, 0), 56, 2.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.42, "vfx", "all_allies", vfx="shockwave_ring", sound="mech_slam", pitch=0.75, scale=1.25, shake=0.35),
            ev(0.75, "impact", "all_allies", vfx="shockwave_ring", particle="explosion", sound="explode", count=14, scale=1.55, shake=0.62, hit_stop=3),
        ],
    })

    save_clip({
        "id": "boss_core_laser",
        "durationTicks": 28,
        "bones": {
            "torso": [bk(0.0, (0, 0, 0)), bk(0.35, (14, 0, 0)), bk(0.72, (16, 0, 0)), bk(1.0, (0, 0, 0))],
            "arm_l": [bk(0.0, (-20, 0, -10)), bk(0.40, (-85, 10, -14)), bk(1.0, (-20, 0, -10))],
            "arm_r": [bk(0.0, (-20, 0, 10)), bk(0.40, (-85, -10, 14)), bk(1.0, (-20, 0, 10))],
        },
        "rootMotion": [
            rk(0.0, "home"),
            rk(0.40, "home", (0.8, 0.0, 0.0)),
            rk(1.0, "home"),
        ],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.44, "arena_center", (0.8, 4.2, 9.2), "target", (0, 1.1, 0), 50, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.24, "sound", "actor", sound="blaze", pitch=0.75),
            ev(0.46, "impact", "target", vfx="fire_beam", particle="flame", sound="fire", count=18, scale=1.45, shake=0.45, hit_stop=2),
        ],
    })

    save_clip({
        "id": "boss_core_charge",
        "durationTicks": 28,
        "bones": {
            "torso": [bk(0.0, (0, 0, 0)), bk(0.38, (-20, 0, 0)), bk(0.58, (25, 0, 0)), bk(1.0, (0, 0, 0))],
            "arm_l": [bk(0.0, (-20, 0, -10)), bk(0.38, (-145, 0, -28)), bk(0.58, (-45, 0, -10)), bk(1.0, (-20, 0, -10))],
            "arm_r": [bk(0.0, (-20, 0, 10)), bk(0.38, (-145, 0, 28)), bk(0.58, (-45, 0, 10)), bk(1.0, (-20, 0, 10))],
        },
        "rootMotion": [],
        "camera": [
            ck(0.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
            ck(0.48, "arena_center", (0.8, 4.6, 9.6), "arena_center", (-1.5, 1.2, 0), 56, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.32, "vfx", "actor", vfx="shockwave_ring", particle="lava", sound="blaze", pitch=0.7, count=16, scale=1.45),
            ev(0.54, "impact", "all_allies", vfx="fire_beam", particle="flame", sound="explode", count=14, scale=1.45, shake=0.55, hit_stop=3),
        ],
    })

    save_clip({
        "id": "boss_core_heat_beam",
        "durationTicks": 44,
        "bannerStyle": "BOSS_ULT",
        "bones": {
            "torso": [bk(0.0, (-18, 0, 0)), bk(0.32, (-28, 0, 0)), bk(0.50, (24, -18, 0)), bk(0.75, (24, 18, 0)), bk(1.0, (0, 0, 0))],
            "arm_l": [bk(0.0, (-135, 0, -28)), bk(0.45, (-95, 20, -20)), bk(1.0, (-20, 0, -10))],
            "arm_r": [bk(0.0, (-135, 0, 28)), bk(0.45, (-95, -20, 20)), bk(1.0, (-20, 0, 10))],
        },
        "rootMotion": [],
        "camera": [
            ck(0.0, "actor", (-4.8, 2.8, 4.2), "actor", (0, 2.2, 0), 42, -3.0, local=False),
            ck(0.32, "actor", (-4.2, 3.2, -3.8), "actor", (0, 2.1, 0), 44, 3.0, local=False),
            ck(0.54, "arena_center", (1.2, 4.8, 9.8), "arena_center", (-2.5, 1.3, 0), 58, 0.0),
            ck(1.0, "overview", (0, 0, 0), "arena_center", (0, 1.1, 0), 54),
        ],
        "events": [
            ev(0.08, "sound", "actor", sound="blaze", pitch=0.6),
            ev(0.48, "vfx", "all_allies", vfx="heat_beam", particle="lava", sound="fire", pitch=0.75, count=18, scale=2.1, shake=0.55, hit_stop=2),
            ev(0.68, "impact", "all_allies", vfx="shockwave_ring", particle="explosion", sound="explode", count=18, scale=1.9, shake=0.82, hit_stop=4),
        ],
    })


def main():
    build_shared_clips()
    build_character_clips()
    build_enemy_clips()
    files = sorted(f for f in os.listdir(OUT_DIR) if f.endswith(".json"))
    print(f"Generated {len(files)} battle animation clips in {OUT_DIR}")


if __name__ == "__main__":
    main()
