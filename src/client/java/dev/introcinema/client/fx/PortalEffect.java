package dev.introcinema.client.fx;

/** Vertical swirling portal ring (spiralling inward) facing the audience side (-Z). */
public final class PortalEffect extends FxEffect {
    @Override
    public void update(ParticleController p, float density) {
        int n = scaled(density, count);
        float open = Math.min(1f, age / 12f) * Math.min(1f, (life - age) / 12f);
        for (int i = 0; i < n; i++) {
            double a = age * 0.12 + i * (Math.PI * 2 / n);
            double r = radius * open * (0.55 + 0.45 * ((i % 3) / 2.0));
            double px = x + Math.cos(a) * r, py = y + radius + Math.sin(a) * r;
            p.emit(ParticleController.GLOW, px, py, z, (float) (-Math.cos(a) * 0.02), (float) (-Math.sin(a) * 0.02), 0, 16, 0.13f, rgb, 0, 0.97f);
        }
    }
}
