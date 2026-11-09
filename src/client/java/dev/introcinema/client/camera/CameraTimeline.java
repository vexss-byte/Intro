package dev.introcinema.client.camera;

import dev.introcinema.client.animation.AnimationInterpolator.Easing;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered camera shots on the shared cinematic clock. Between two shots the camera eases from the
 * end of the previous shot into the start of the next, so there is never a hard cut.
 */
public final class CameraTimeline {
    /** A shot is a function of progress 0..1. */
    public interface Shot { CameraPose at(float p); }

    private record Entry(int start, int end, Shot shot, Easing ease) {}

    private final List<Entry> entries = new ArrayList<>();
    private final int blendTicks;

    public CameraTimeline(int blendTicks) { this.blendTicks = Math.max(0, blendTicks); }

    public CameraTimeline add(int startTick, int endTick, Shot shot, Easing ease) {
        if (endTick <= startTick) throw new IllegalArgumentException("shot must have positive length");
        if (!entries.isEmpty() && startTick < entries.get(entries.size() - 1).start())
            throw new IllegalArgumentException("shots must be added in start order");
        entries.add(new Entry(startTick, endTick, shot, ease));
        return this;
    }

    public boolean isEmpty() { return entries.isEmpty(); }

    /** Pose at a fractional tick; holds the last pose after the final shot. */
    public CameraPose sample(float tick) {
        if (entries.isEmpty()) return null;
        Entry cur = entries.get(0);
        int idx = 0;
        for (int i = 0; i < entries.size(); i++) { if (tick >= entries.get(i).start()) { cur = entries.get(i); idx = i; } }
        float p = Math.max(0f, Math.min(1f, (tick - cur.start()) / (cur.end() - cur.start())));
        CameraPose pose = cur.shot().at(cur.ease().apply(p));
        if (idx > 0 && blendTicks > 0 && tick - cur.start() < blendTicks) {
            Entry prev = entries.get(idx - 1);
            CameraPose prevEnd = prev.shot().at(1f);
            float w = (tick - cur.start()) / blendTicks;
            return CameraInterpolator.lerp(prevEnd, pose, w, Easing.SMOOTHSTEP);
        }
        return pose;
    }

    // ---- shot factories -------------------------------------------------------------------

    public static Shot still(CameraPose pose) { return p -> pose; }

    public static Shot move(CameraPose from, CameraPose to) { return p -> CameraInterpolator.lerp(from, to, p); }

    /** Dolly toward/away from a target while keeping it framed. */
    public static Shot dolly(double[] target, double[] from, double[] to, float fov, float roll) {
        return p -> CameraPose.lookAt(
                from[0] + (to[0] - from[0]) * p, from[1] + (to[1] - from[1]) * p, from[2] + (to[2] - from[2]) * p,
                target[0], target[1], target[2], roll, fov);
    }

    /** Orbit around a center; angles in degrees, height is camera Y offset from center. */
    public static Shot orbit(double cx, double cy, double cz, double radius, double height,
                             double startDeg, double endDeg, float fovFrom, float fovTo, float roll) {
        return p -> {
            double a = Math.toRadians(startDeg + (endDeg - startDeg) * p);
            double r = radius;
            return CameraPose.lookAt(cx + Math.sin(a) * r, cy + height, cz - Math.cos(a) * r,
                    cx, cy, cz, roll, fovFrom + (fovTo - fovFrom) * p);
        };
    }
}
