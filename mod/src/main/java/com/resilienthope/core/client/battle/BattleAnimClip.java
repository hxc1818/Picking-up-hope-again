package com.resilienthope.core.client.battle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Mth;

import java.util.*;

/**
 * Data-driven battle choreography clip parsed from battle_animations/*.json.
 * Drives entity root dash/leap arcs, articulated bone rotations/offsets,
 * synchronized camera keyframes, and timed VFX/particle/sound/shake/hitstop triggers.
 */
public class BattleAnimClip {

    public static final class RootKeyframe {
        public float t;
        public String anchor = "home"; // "home", "target", "arena_center"
        public float[] offset = new float[]{0f, 0f, 0f}; // [forwardTowardOpponent, up, right]
        public float yawOffset = 0f;
        public String ease = "linear";
    }

    public static final class BoneKeyframe {
        public float t;
        public float[] rot = new float[]{0f, 0f, 0f}; // degrees
        public float[] pos = new float[]{0f, 0f, 0f}; // ModelPart pixel units
        public String ease = "linear";
    }

    public static final class CameraKeyframe {
        public float t;
        public String posAnchor = "arena_center"; // "overview", "actor", "target", "arena_center"
        public float[] posOffset = new float[]{0f, 4.5f, 9.5f}; // [worldX or actorForward, up, worldZ or actorRight]
        public boolean localToActorFacing = false;
        public String lookAnchor = "arena_center";
        public float[] lookOffset = new float[]{0f, 1.1f, 0f};
        public float fov = 52.0f;
        public float roll = 0.0f;
        public String ease = "ease_in_out_cubic";
    }

    public static final class EventMarker {
        public float t;
        public String type = "impact"; // "impact", "vfx", "particle", "sound", "shake"
        public String anchor = "target"; // "actor", "target", "all_enemies", "all_allies", "arena_center", "weapon_tip"
        public String vfx = "";
        public String particle = "";
        public String sound = "";
        public float pitch = 1.0f;
        public float volume = 0.85f;
        public int count = 12;
        public float spread = 0.45f;
        public float scale = 1.0f;
        public float shake = 0.0f;
        public int hitStop = 0;
        public boolean suppressDamagePopup = false;
    }

    public record BonePose(float rxDeg, float ryDeg, float rzDeg, float px, float py, float pz) {
        public static final BonePose ZERO = new BonePose(0f, 0f, 0f, 0f, 0f, 0f);
    }

    public record CameraSample(String posAnchor, float[] posOffset, boolean localToActorFacing,
                               String lookAnchor, float[] lookOffset, float fov, float roll) {}

    public String id = "humanoid_idle";
    public int durationTicks = 24;
    public boolean loop = false;
    public String bannerStyle = "";
    public final List<RootKeyframe> rootMotion = new ArrayList<>();
    public final Map<String, List<BoneKeyframe>> bones = new LinkedHashMap<>();
    public final List<CameraKeyframe> camera = new ArrayList<>();
    public final List<EventMarker> events = new ArrayList<>();

    public BonePose sampleBone(String boneName, float progress) {
        List<BoneKeyframe> track = bones.get(boneName);
        if (track == null || track.isEmpty()) return BonePose.ZERO;
        float p = Mth.clamp(progress, 0f, 1f);
        if (track.size() == 1 || p <= track.get(0).t) {
            BoneKeyframe k = track.get(0);
            return new BonePose(k.rot[0], k.rot[1], k.rot[2], k.pos[0], k.pos[1], k.pos[2]);
        }
        BoneKeyframe last = track.get(track.size() - 1);
        if (p >= last.t) {
            return new BonePose(last.rot[0], last.rot[1], last.rot[2], last.pos[0], last.pos[1], last.pos[2]);
        }
        for (int i = 0; i < track.size() - 1; i++) {
            BoneKeyframe a = track.get(i);
            BoneKeyframe b = track.get(i + 1);
            if (p >= a.t && p <= b.t) {
                float span = Math.max(1e-4f, b.t - a.t);
                float u = applyEase(b.ease, (p - a.t) / span);
                return new BonePose(
                        Mth.lerp(u, a.rot[0], b.rot[0]),
                        Mth.lerp(u, a.rot[1], b.rot[1]),
                        Mth.lerp(u, a.rot[2], b.rot[2]),
                        Mth.lerp(u, a.pos[0], b.pos[0]),
                        Mth.lerp(u, a.pos[1], b.pos[1]),
                        Mth.lerp(u, a.pos[2], b.pos[2])
                );
            }
        }
        return BonePose.ZERO;
    }

    public static float applyEase(String ease, float u) {
        float t = Mth.clamp(u, 0f, 1f);
        if (ease == null) return t;
        return switch (ease) {
            case "ease_in_quad" -> t * t;
            case "ease_out_quad" -> 1f - (1f - t) * (1f - t);
            case "ease_in_out_quad" -> t < 0.5f ? 2f * t * t : 1f - 2f * (1f - t) * (1f - t);
            case "ease_in_cubic" -> t * t * t;
            case "ease_out_cubic" -> {
                float inv = 1f - t;
                yield 1f - inv * inv * inv;
            }
            case "ease_in_out_cubic" -> t < 0.5f
                    ? 4f * t * t * t
                    : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
            case "ease_out_back" -> {
                float c1 = 1.70158f;
                float c3 = c1 + 1f;
                float s = t - 1f;
                yield 1f + c3 * s * s * s + c1 * s * s;
            }
            case "step" -> t >= 1.0f ? 1.0f : 0.0f;
            default -> t;
        };
    }

    public static BattleAnimClip fromJson(String fallbackId, JsonObject obj) {
        BattleAnimClip clip = new BattleAnimClip();
        clip.id = getStr(obj, "id", fallbackId);
        clip.durationTicks = Math.max(1, getInt(obj, "durationTicks", 24));
        clip.loop = getBool(obj, "loop", false);
        clip.bannerStyle = getStr(obj, "bannerStyle", "");

        if (obj.has("rootMotion") && obj.get("rootMotion").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("rootMotion")) {
                if (!el.isJsonObject()) continue;
                JsonObject ko = el.getAsJsonObject();
                RootKeyframe rk = new RootKeyframe();
                rk.t = getFloat(ko, "t", 0f);
                rk.anchor = getStr(ko, "anchor", "home");
                rk.offset = getVec3(ko, "offset", new float[]{0f, 0f, 0f});
                rk.yawOffset = getFloat(ko, "yawOffset", 0f);
                rk.ease = getStr(ko, "ease", "linear");
                clip.rootMotion.add(rk);
            }
            clip.rootMotion.sort(Comparator.comparingDouble(k -> k.t));
        }

        if (obj.has("bones") && obj.get("bones").isJsonObject()) {
            JsonObject bonesObj = obj.getAsJsonObject("bones");
            for (Map.Entry<String, JsonElement> entry : bonesObj.entrySet()) {
                if (!entry.getValue().isJsonArray()) continue;
                List<BoneKeyframe> list = new ArrayList<>();
                for (JsonElement el : entry.getValue().getAsJsonArray()) {
                    if (!el.isJsonObject()) continue;
                    JsonObject bo = el.getAsJsonObject();
                    BoneKeyframe bk = new BoneKeyframe();
                    bk.t = getFloat(bo, "t", 0f);
                    bk.rot = getVec3(bo, "rot", new float[]{0f, 0f, 0f});
                    bk.pos = getVec3(bo, "pos", new float[]{0f, 0f, 0f});
                    bk.ease = getStr(bo, "ease", "linear");
                    list.add(bk);
                }
                list.sort(Comparator.comparingDouble(k -> k.t));
                clip.bones.put(entry.getKey(), list);
            }
        }

        if (obj.has("camera") && obj.get("camera").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("camera")) {
                if (!el.isJsonObject()) continue;
                JsonObject co = el.getAsJsonObject();
                CameraKeyframe ck = new CameraKeyframe();
                ck.t = getFloat(co, "t", 0f);
                ck.posAnchor = getStr(co, "posAnchor", "overview");
                ck.posOffset = getVec3(co, "posOffset", new float[]{0f, 0f, 0f});
                ck.localToActorFacing = getBool(co, "localToActorFacing", false);
                ck.lookAnchor = getStr(co, "lookAnchor", "arena_center");
                ck.lookOffset = getVec3(co, "lookOffset", new float[]{0f, 1.1f, 0f});
                ck.fov = getFloat(co, "fov", 52.0f);
                ck.roll = getFloat(co, "roll", 0.0f);
                ck.ease = getStr(co, "ease", "ease_in_out_cubic");
                clip.camera.add(ck);
            }
            clip.camera.sort(Comparator.comparingDouble(k -> k.t));
        }

        if (obj.has("events") && obj.get("events").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("events")) {
                if (!el.isJsonObject()) continue;
                JsonObject eo = el.getAsJsonObject();
                EventMarker ev = new EventMarker();
                ev.t = getFloat(eo, "t", 0.5f);
                ev.type = getStr(eo, "type", "impact");
                ev.anchor = getStr(eo, "anchor", "target");
                ev.vfx = getStr(eo, "vfx", "");
                ev.particle = getStr(eo, "particle", "");
                ev.sound = getStr(eo, "sound", "");
                ev.pitch = getFloat(eo, "pitch", 1.0f);
                ev.volume = getFloat(eo, "volume", 0.85f);
                ev.count = getInt(eo, "count", 12);
                ev.spread = getFloat(eo, "spread", 0.45f);
                ev.scale = getFloat(eo, "scale", 1.0f);
                ev.shake = getFloat(eo, "shake", 0.0f);
                ev.hitStop = getInt(eo, "hitStop", 0);
                ev.suppressDamagePopup = getBool(eo, "suppressDamagePopup", false);
                clip.events.add(ev);
            }
            clip.events.sort(Comparator.comparingDouble(e -> e.t));
        }
        return clip;
    }

    public static BattleAnimClip createFallbackClip(String id, boolean isLoopIdle) {
        BattleAnimClip c = new BattleAnimClip();
        c.id = id != null ? id : "fallback";
        c.durationTicks = isLoopIdle ? 40 : 18;
        c.loop = isLoopIdle;
        if (!isLoopIdle) {
            RootKeyframe k0 = new RootKeyframe(); k0.t = 0f; k0.anchor = "home";
            RootKeyframe k1 = new RootKeyframe(); k1.t = 0.35f; k1.anchor = "target"; k1.offset = new float[]{-1.4f, 0f, 0f}; k1.ease = "ease_out_cubic";
            RootKeyframe k2 = new RootKeyframe(); k2.t = 0.65f; k2.anchor = "target"; k2.offset = new float[]{-1.4f, 0f, 0f};
            RootKeyframe k3 = new RootKeyframe(); k3.t = 1.0f; k3.anchor = "home"; k3.ease = "ease_out_cubic";
            c.rootMotion.add(k0); c.rootMotion.add(k1); c.rootMotion.add(k2); c.rootMotion.add(k3);

            BoneKeyframe b0 = new BoneKeyframe(); b0.t = 0f; b0.rot = new float[]{-15f, 0f, 0f};
            BoneKeyframe b1 = new BoneKeyframe(); b1.t = 0.32f; b1.rot = new float[]{-125f, 0f, -15f}; b1.ease = "ease_out_quad";
            BoneKeyframe b2 = new BoneKeyframe(); b2.t = 0.55f; b2.rot = new float[]{25f, 0f, -10f}; b2.ease = "ease_out_cubic";
            BoneKeyframe b3 = new BoneKeyframe(); b3.t = 1.0f; b3.rot = new float[]{-15f, 0f, 0f};
            c.bones.put("right_arm", List.of(b0, b1, b2, b3));

            EventMarker ev = new EventMarker();
            ev.t = 0.45f;
            ev.type = "impact";
            ev.vfx = "slash_arc";
            ev.shake = 0.25f;
            c.events.add(ev);
        }
        return c;
    }

    private static String getStr(JsonObject o, String k, String def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : def;
    }

    private static float getFloat(JsonObject o, String k, float def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsFloat() : def;
    }

    private static int getInt(JsonObject o, String k, int def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : def;
    }

    private static boolean getBool(JsonObject o, String k, boolean def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsBoolean() : def;
    }

    private static float[] getVec3(JsonObject o, String k, float[] def) {
        if (!o.has(k) || !o.get(k).isJsonArray()) return def;
        JsonArray arr = o.getAsJsonArray(k);
        return new float[]{
                arr.size() > 0 ? arr.get(0).getAsFloat() : def[0],
                arr.size() > 1 ? arr.get(1).getAsFloat() : def[1],
                arr.size() > 2 ? arr.get(2).getAsFloat() : def[2]
        };
    }
}
