package dev.introcinema.client.fx;

/** Energy ring, ground energy circle, rising energy trail (helix) and magical waves, all from one pooled class. */
public final class EnergyEffect extends FxEffect {
    public enum Mode { RING, GROUND_CIRCLE, TRAIL, WAVES }
    private Mode mode = Mode.RING;

    public EnergyEffect mode(Mode m) { this.mode = m; return this; }

    @Override
    public void update(ParticleController p, float density) {
        switch (mode) {
            case RING -> ring(p, density, radius * (0.15f + 0.85f * t()), 1.0f);
            case WAVES -> { if (age % 12 == 0) ring(p, density, radius * 0.25f, 0.6f); }
            case GROUND_CIRCLE -> {
                int n = scaled(density, count);
                double spin = age * 0.07;
                for (int i = 0; i < n; i++) {
                    double a = spin + i * (Math.PI * 2 / n);
                    double r = radius * (i % 2 == 0 ? 1.0 : 0.62);
                    p.emit(ParticleController.SQUARE, x + Math.cos(a) * r, y + 0.05, z + Math.sin(a) * r, 0, 0.004f, 0, 12, 0.07f, rgb, 0, 1f);
                }
                if (age % 3 == 0) { // spokes: the animated pattern inside the circle
                    double a = spin * 2;
                    for (int k = 1; k <= 5; k++) {
                        double r = radius * k / 6.0;
                        p.emit(ParticleController.STAR, x + Math.cos(a) * r, y + 0.06, z + Math.sin(a) * r, 0, 0.01f, 0, 10, 0.09f, rgb, 0, 1f);
                        p.emit(ParticleController.STAR, x - Math.cos(a) * r, y + 0.06, z - Math.sin(a) * r, 0, 0.01f, 0, 10, 0.09f, rgb, 0, 1f);
                    }
                }
            }
            case TRAIL -> {
                double a = age * 0.35, h = (age * 0.06) % (radius * 2);
                for (int k = 0; k < 2; k++) {
                    double aa = a + k * Math.PI;
                    p.emit(ParticleController.GLOW, x + Math.cos(aa) * radius * 0.5, y + h, z + Math.sin(aa) * radius * 0.5, 0, 0.012f, 0, 18, 0.12f, rgb, 0, 0.98f);
                }
            }
        }
    }

    private void ring(ParticleController p, float density, float r, float speed) {
        int n = scaled(density, count);
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2 / n);
            p.emit(ParticleController.SQUARE, x + Math.cos(a) * r, y + 0.1, z + Math.sin(a) * r,
                    (float) Math.cos(a) * 0.05f * speed, 0.01f, (float) Math.sin(a) * 0.05f * speed, 14, 0.08f, rgb, 0, 0.95f);
        }
    }
}
