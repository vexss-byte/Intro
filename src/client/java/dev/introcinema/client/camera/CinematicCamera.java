package dev.introcinema.client.camera;

/** Frame-rate independent smoothing on top of the timeline pose (hides server clock corrections). */
public final class CinematicCamera {
    private CameraPose smoothed;

    public void reset() { smoothed = null; }

    public CameraPose smooth(CameraPose target, float dtSeconds) {
        if (target == null) return null;
        if (smoothed == null) { smoothed = target; return target; }
        float k = 1f - (float) Math.exp(-Math.max(0f, Math.min(0.25f, dtSeconds)) * 22f);
        smoothed = CameraInterpolator.lerp(smoothed, target, k);
        return smoothed;
    }
}
