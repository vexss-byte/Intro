package dev.introcinema.client.fx;

/** Base for pooled effects: init -> tick/render each frame -> cleanup. Instances are reused, never reallocated per use. */
public abstract class FxEffect {
    protected int rgb, life, age, count;
    protected double x, y, z;
    protected float radius;
    protected boolean done;

    public FxEffect init(int rgb, double x, double y, double z, float radius, int life, int count) {
        this.rgb = rgb; this.x = x; this.y = y; this.z = z; this.radius = radius;
        this.life = Math.max(1, life); this.count = count; this.age = 0; this.done = false;
        start();
        return this;
    }

    /** One-time setup after init. */
    protected void start() {}
    /** Advance one tick; may emit particles. */
    public abstract void update(ParticleController particles, float density);
    /** Extra geometry beyond particles (beams, etc.). */
    public void render(Painter p, FxProjection pr) {}
    /** Release anything held; the instance goes back to its pool. */
    public void cleanup() {}

    public final boolean tick(ParticleController particles, float density) {
        update(particles, density);
        return ++age >= life || done;
    }
    /** 0..1 progress through the lifetime. */
    protected final float t() { return Math.min(1f, age / (float) life); }
    protected final int scaled(float density, int base) { return Math.max(1, Math.round(base * density)); }
}
