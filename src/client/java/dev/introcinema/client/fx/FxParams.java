package dev.introcinema.client.fx;

/** Per-frame intensities handed to the (optional) GLSL post-processing pass. All 0..1. */
public final class FxParams {
    public float cosmic, energy, portal, glow, impact, transition, bloom, vignette, grade;
    public float centerX = 0.5f, centerY = 0.5f;           // screen-space focus (0..1, y down)
    public int colorA = 0xFFC83C, colorB = 0xFF7A1A;       // rgb
    public void reset() { cosmic = energy = portal = glow = impact = transition = bloom = vignette = grade = 0f; }
}
