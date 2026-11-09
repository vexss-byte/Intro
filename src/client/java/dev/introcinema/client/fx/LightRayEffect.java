package dev.introcinema.client.fx;

/** Vertical light beams, drawn as projected gradient columns (cheap, no per-particle cost). */
public final class LightRayEffect extends FxEffect {
    private final float[] p0 = new float[4], p1 = new float[4];

    @Override
    public void update(ParticleController p, float density) { /* beams are drawn in render() */ }

    @Override
    public void render(Painter p, FxProjection pr) {
        float fade = Math.min(1f, age / 10f) * Math.min(1f, (life - age) / 14f);
        int beams = Math.max(1, count);
        for (int i = 0; i < beams; i++) {
            double ox = x + (i - (beams - 1) / 2.0) * radius * 0.9;
            double sway = Math.sin(age * 0.05 + i) * 0.4;
            if (!pr.project(ox + sway, y, z, p0) || !pr.project(ox, y + 14, z, p1)) continue;
            float w = Math.max(3f, p0[2] * radius * 0.35f);
            float pulse = 0.55f + 0.45f * (float) Math.sin(age * 0.11 + i * 1.7);
            int aTop = (int) (fade * pulse * 150), aBot = 0;
            float cx = (p0[0] + p1[0]) / 2f, half = w * 0.4f;
            p.gradient(Math.round(cx - half), Math.round(Math.min(p1[1], p0[1])),
                    Math.round(cx + half), Math.round(Math.max(p1[1], p0[1])),
                    (aBot << 24) | (rgb & 0xFFFFFF), (aTop << 24) | (rgb & 0xFFFFFF));
        }
    }
}
