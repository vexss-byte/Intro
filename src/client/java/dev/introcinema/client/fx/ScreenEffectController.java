package dev.introcinema.client.fx;

import java.util.ArrayList;
import java.util.List;

/**
 * Timed full-screen effects (fade, flash, pixel-dissolve transition, vignette). Everything is a pure function
 * of the shared cinematic tick, so skipping or catching up never leaves a stuck overlay. Drawn with plain
 * fills, so it keeps working when the GLSL path is unavailable. Nothing here blurs the scene.
 */
public final class ScreenEffectController {
    private record Fade(int from, int to, float a0, float a1) {}
    private record Flash(int start, int ticks, float strength, int rgb) {}
    private record Transition(int start, int end) {}

    private final List<Fade> fades = new ArrayList<>(4);
    private final List<Flash> flashes = new ArrayList<>(8);
    private final List<Transition> transitions = new ArrayList<>(8);
    private boolean enabled = true;
    private float vignetteStrength = 0.35f;

    public void setEnabled(boolean e) { enabled = e; }
    public void fade(int from, int to, float a0, float a1) { fades.add(new Fade(from, Math.max(from + 1, to), a0, a1)); }
    public void flash(int start, int ticks, float strength, int rgb) { flashes.add(new Flash(start, Math.max(1, ticks), strength, rgb)); }
    public void pixelTransition(int start, int end) { transitions.add(new Transition(start, Math.max(start + 2, end))); }
    public void reset() { fades.clear(); flashes.clear(); transitions.clear(); }

    public float fadeAlpha(float now) {
        float a = 0f;
        for (Fade f : fades) {
            if (now < f.from()) continue;
            float t = Math.min(1f, (now - f.from()) / (f.to() - f.from()));
            a = f.a0() + (f.a1() - f.a0()) * t;
        }
        return a;
    }

    public float flashAmount(float now) {
        float m = 0f;
        for (Flash f : flashes) {
            float t = (now - f.start()) / f.ticks();
            if (t >= 0f && t <= 1f) m = Math.max(m, f.strength() * (1f - t) * (1f - t));
        }
        return m;
    }

    /** 0 -> 1 -> 0 across the transition window (screen fully covered at the midpoint, when the stage swap happens). */
    public float transitionAmount(float now) {
        float m = 0f;
        for (Transition tr : transitions) {
            if (now < tr.start() || now > tr.end()) continue;
            float t = (now - tr.start()) / (tr.end() - tr.start());
            m = Math.max(m, 1f - Math.abs(t * 2f - 1f));
        }
        return m;
    }

    public void fill(FxParams p, float now) {
        p.transition = enabled ? transitionAmount(now) : 0f;
        p.vignette = enabled ? vignetteStrength : 0f;
    }

    public void render(Painter p, float now) {
        int w = p.width(), h = p.height();
        if (enabled) {
            float fl = flashAmount(now);
            if (fl > 0.01f) p.fill(0, 0, w, h, ((int) (Math.min(1f, fl) * 200) << 24) | 0xFFF2C8);
            float tr = transitionAmount(now);
            if (tr > 0.01f) pixelDissolve(p, tr, w, h);
            vignette(p, w, h);
        }
        float fa = fadeAlpha(now);
        if (fa > 0.003f) p.fill(0, 0, w, h, ((int) (Math.min(1f, fa) * 255) << 24));
    }

    private static void pixelDissolve(Painter p, float amount, int w, int h) {
        int cell = Math.max(8, h / 22);
        for (int cy = 0; cy * cell < h; cy++) {
            for (int cx = 0; cx * cell < w; cx++) {
                int hash = (cx * 73856093) ^ (cy * 19349663);
                float r = ((hash >>> 8) & 0xFFFF) / 65535f;
                if (r < amount) p.fill(cx * cell, cy * cell, Math.min(w, (cx + 1) * cell), Math.min(h, (cy + 1) * cell), 0xE6120C06);
            }
        }
    }

    private void vignette(Painter p, int w, int h) {
        int band = Math.max(8, h / 5);
        int edge = (int) (vignetteStrength * 150) << 24;
        p.gradient(0, 0, w, band, edge, 0);
        p.gradient(0, h - band, w, h, 0, edge);
    }
}
