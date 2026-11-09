package dev.introcinema.client.hud;

/** Time-based helpers for slide / fade / typewriter / gradient. All inputs in ticks. */
public final class TextAnimator {
    private TextAnimator() {}

    public static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    public static float easeOut(float t) { t = clamp01(t); return 1f - (1f - t) * (1f - t) * (1f - t); }

    /** 0 -> 1 over fadeTicks after start, stays 1, then 1 -> 0 over fadeTicks before end. */
    public static float alpha(float now, int start, int end, int fadeTicks) {
        if (now < start || now > end) return 0f;
        float in = clamp01((now - start) / Math.max(1, fadeTicks));
        float out = clamp01((end - now) / Math.max(1, fadeTicks));
        return Math.min(in, out);
    }

    /** Horizontal slide offset in "units" that eases to 0 as the element arrives. */
    public static float slide(float now, int start, int ticks, float distance) {
        return (1f - easeOut((now - start) / Math.max(1, ticks))) * distance;
    }

    public static String typewriter(String s, float progress) {
        int n = Math.max(0, Math.min(s.length(), (int) Math.floor(s.length() * clamp01(progress))));
        return s.substring(0, n);
    }

    public static int lerpColor(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static int argb(int rgb, float alpha) { return ((int) (clamp01(alpha) * 255f) << 24) | (rgb & 0xFFFFFF); }

    public static int parseHex(String hex, int fallback) {
        if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') return fallback;
        try { return Integer.parseInt(hex.substring(1), 16); } catch (NumberFormatException e) { return fallback; }
    }
}
