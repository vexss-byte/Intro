package dev.introcinema.client.fx;

/**
 * Fixed-capacity particle pool (structure of arrays): emitting never allocates, a full pool simply drops
 * new particles, and dead particles are removed by swapping with the last one.
 */
public final class ParticleController {
    public static final int SQUARE = 0, STAR = 1, GLOW = 2;

    private final int cap;
    private int n;
    private final double[] x, y, z;
    private final float[] vx, vy, vz, size, gravity, drag;
    private final int[] life, maxLife, rgb;
    private final byte[] kind;
    private long seed = 0x9E3779B97F4A7C15L;
    private final float[] scratch = new float[4];

    public ParticleController(int capacity) {
        cap = Math.max(16, capacity);
        x = new double[cap]; y = new double[cap]; z = new double[cap];
        vx = new float[cap]; vy = new float[cap]; vz = new float[cap];
        size = new float[cap]; gravity = new float[cap]; drag = new float[cap];
        life = new int[cap]; maxLife = new int[cap]; rgb = new int[cap]; kind = new byte[cap];
    }

    public int capacity() { return cap; }
    public int count() { return n; }
    public void clear() { n = 0; }

    /** Allocation-free xorshift in [0,1). */
    public float rand() {
        seed ^= seed << 13; seed ^= seed >>> 7; seed ^= seed << 17;
        return ((seed >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }
    public float rand(float lo, float hi) { return lo + (hi - lo) * rand(); }

    public boolean emit(int kind, double px, double py, double pz, float vx_, float vy_, float vz_,
                        int lifeTicks, float sizeWorld, int color, float grav, float dragK) {
        if (n >= cap) return false;
        int i = n++;
        x[i] = px; y[i] = py; z[i] = pz; vx[i] = vx_; vy[i] = vy_; vz[i] = vz_;
        life[i] = maxLife[i] = Math.max(1, lifeTicks); size[i] = sizeWorld; rgb[i] = color & 0xFFFFFF;
        this.kind[i] = (byte) kind; gravity[i] = grav; drag[i] = dragK;
        return true;
    }

    public void update() {
        for (int i = n - 1; i >= 0; i--) {
            if (--life[i] <= 0) { remove(i); continue; }
            x[i] += vx[i]; y[i] += vy[i]; z[i] += vz[i];
            vx[i] *= drag[i]; vy[i] = vy[i] * drag[i] - gravity[i]; vz[i] *= drag[i];
        }
    }

    private void remove(int i) {
        int l = --n;
        if (i == l) return;
        x[i] = x[l]; y[i] = y[l]; z[i] = z[l]; vx[i] = vx[l]; vy[i] = vy[l]; vz[i] = vz[l];
        life[i] = life[l]; maxLife[i] = maxLife[l]; size[i] = size[l]; rgb[i] = rgb[l]; kind[i] = kind[l];
        gravity[i] = gravity[l]; drag[i] = drag[l];
    }

    public void render(Painter p, FxProjection pr, float maxDistance) {
        float[] o = scratch;
        for (int i = 0; i < n; i++) {
            if (!pr.project(x[i], y[i], z[i], o) || o[3] > maxDistance) continue;
            float t = life[i] / (float) maxLife[i];
            float a = Math.min(1f, t * 2.2f) * Math.min(1f, (1f - t) * 12f + 0.15f);
            float s = Math.max(1.5f, size[i] * o[2]);
            int sx = Math.round(o[0]), sy = Math.round(o[1]);
            if (sx < -40 || sy < -40 || sx > p.width() + 40 || sy > p.height() + 40) continue;
            int col = ((int) (Math.max(0f, Math.min(1f, a)) * 255) << 24) | rgb[i];
            int hs = Math.max(1, Math.round(s / 2f));
            switch (kind[i]) {
                case STAR -> {
                    p.fill(sx - hs, sy - 1, sx + hs, sy + 1, col);
                    p.fill(sx - 1, sy - hs, sx + 1, sy + hs, col);
                    p.fill(sx - 1, sy - 1, sx + 1, sy + 1, (col & 0xFF000000) | 0x00FFFFFF);
                }
                case GLOW -> {
                    int a2 = (int) (((col >>> 24) & 255) * 0.22f) << 24, a3 = (int) (((col >>> 24) & 255) * 0.5f) << 24;
                    p.fill(sx - hs * 2, sy - hs * 2, sx + hs * 2, sy + hs * 2, a2 | rgb[i]);
                    p.fill(sx - hs, sy - hs, sx + hs, sy + hs, a3 | rgb[i]);
                }
                default -> p.fill(sx - hs, sy - hs, sx + hs, sy + hs, col);
            }
        }
    }
}
