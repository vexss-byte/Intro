package dev.introcinema.client.fx;

/** Floating glow motes drifting upward inside a volume (also used for ambient dust and star particles). */
public final class GlowEffect extends FxEffect {
    @Override
    public void update(ParticleController p, float density) {
        int n = Math.max(0, Math.round(count * density / 20f)); // `count` is motes per second
        for (int i = 0; i < n + (p.rand() < (count * density / 20f - n) ? 1 : 0); i++) {
            double px = x + p.rand(-radius, radius), pz = z + p.rand(-radius, radius), py = y + p.rand(0f, 3f);
            int kind = p.rand() < 0.35f ? ParticleController.STAR : ParticleController.GLOW;
            p.emit(kind, px, py, pz, p.rand(-0.004f, 0.004f), p.rand(0.006f, 0.02f), p.rand(-0.004f, 0.004f),
                    (int) p.rand(35, 80), p.rand(0.06f, 0.14f), rgb, 0, 0.99f);
        }
    }
}
