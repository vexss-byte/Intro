package dev.introcinema.client.fx;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Timeline-driven effect hub. Owns the particle pool and pooled effect instances. Every effect has
 * start (init) / update (tick) / render / cleanup, and a hard particle cap keeps cost bounded.
 */
public final class CinematicFXManager {
    private ParticleController particles = new ParticleController(560);
    private float density = 1f;
    private final List<FxEffect> active = new ArrayList<>(32);
    private final ArrayDeque<FxEffect> poolEnergy = new ArrayDeque<>(), poolGlow = new ArrayDeque<>(),
            poolPortal = new ArrayDeque<>(), poolRays = new ArrayDeque<>();
    private float impactLevel, glowLevel, energyLevel, portalLevel;
    private int portalUntil = -1;

    /** @param cap max live particles; also scales emission density. */
    public void configure(int cap) {
        particles = new ParticleController(cap);
        density = cap <= 260 ? 0.35f : cap >= 1000 ? 1.6f : 1f;
    }

    public ParticleController particles() { return particles; }

    private <T extends FxEffect> T take(ArrayDeque<FxEffect> pool, Supplier<T> make) {
        @SuppressWarnings("unchecked") T e = (T) pool.pollFirst();
        return e != null ? e : make.get();
    }

    private void launch(FxEffect e, ArrayDeque<FxEffect> pool) {
        if (active.size() >= 48) { e.cleanup(); pool.addLast(e); return; } // hard cap on simultaneous effects
        active.add(e);
    }

    public void floatingGlow(int rgb, double x, double y, double z, float radius, int life, int perSecond) {
        GlowEffect e = take(poolGlow, GlowEffect::new); e.init(rgb, x, y, z, radius, life, perSecond); launch(e, poolGlow);
        glowLevel = Math.max(glowLevel, 0.5f);
    }
    public void groundCircle(int rgb, double x, double y, double z, float radius, int life) {
        EnergyEffect e = take(poolEnergy, EnergyEffect::new); e.mode(EnergyEffect.Mode.GROUND_CIRCLE).init(rgb, x, y, z, radius, life, 28); launch(e, poolEnergy);
        energyLevel = Math.max(energyLevel, 0.5f);
    }
    public void energyRing(int rgb, double x, double y, double z, float radius, int life) {
        EnergyEffect e = take(poolEnergy, EnergyEffect::new); e.mode(EnergyEffect.Mode.RING).init(rgb, x, y, z, radius, life, 36); launch(e, poolEnergy);
        energyLevel = 1f;
    }
    public void energyWaves(int rgb, double x, double y, double z, float radius, int life) {
        EnergyEffect e = take(poolEnergy, EnergyEffect::new); e.mode(EnergyEffect.Mode.WAVES).init(rgb, x, y, z, radius, life, 30); launch(e, poolEnergy);
    }
    public void energyTrail(int rgb, double x, double y, double z, float height, int life) {
        EnergyEffect e = take(poolEnergy, EnergyEffect::new); e.mode(EnergyEffect.Mode.TRAIL).init(rgb, x, y, z, height, life, 2); launch(e, poolEnergy);
    }
    public void portal(int rgb, double x, double y, double z, float radius, int life, int nowTick) {
        PortalEffect e = take(poolPortal, PortalEffect::new); e.init(rgb, x, y, z, radius, life, 24); launch(e, poolPortal);
        portalUntil = nowTick + life; portalLevel = 1f;
    }
    public void lightRays(int rgb, double x, double y, double z, int beams, int life) {
        LightRayEffect e = take(poolRays, LightRayEffect::new); e.init(rgb, x, y, z, 2.2f, life, beams); launch(e, poolRays);
    }

    /** Star + square burst around a point (the "member revealed" moment). */
    public void burst(int rgb, double x, double y, double z, float power) {
        int n = Math.max(8, Math.round(70 * power * density));
        for (int i = 0; i < n; i++) {
            double a = particles.rand() * Math.PI * 2, el = (particles.rand() - 0.3) * Math.PI;
            float sp = particles.rand(0.04f, 0.16f) * power;
            float vx = (float) (Math.cos(a) * Math.cos(el) * sp), vy = (float) (Math.sin(el) * sp), vz = (float) (Math.sin(a) * Math.cos(el) * sp);
            int kind = particles.rand() < 0.45f ? ParticleController.STAR : ParticleController.SQUARE;
            int col = particles.rand() < 0.25f ? 0xFFFFFF : rgb;
            particles.emit(kind, x, y, z, vx, vy, vz, (int) particles.rand(20, 46), particles.rand(0.08f, 0.2f), col, 0.003f, 0.94f);
        }
        impactLevel = Math.min(1f, 0.6f * power);
    }

    public void impact(int rgb, double x, double y, double z) {
        energyRing(rgb, x, y, z, 5f, 14);
        burst(rgb, x, y + 0.2, z, 0.8f);
        impactLevel = 1f;
    }

    public void update(int nowTick) {
        particles.update();
        for (int i = active.size() - 1; i >= 0; i--) {
            FxEffect e = active.get(i);
            if (e.tick(particles, density)) {
                active.remove(i); e.cleanup();
                if (e instanceof EnergyEffect) poolEnergy.addLast(e);
                else if (e instanceof GlowEffect) poolGlow.addLast(e);
                else if (e instanceof PortalEffect) poolPortal.addLast(e);
                else poolRays.addLast(e);
            }
        }
        impactLevel *= 0.90f; energyLevel *= 0.985f; glowLevel *= 0.99f;
        portalLevel = nowTick <= portalUntil ? 1f : portalLevel * 0.9f;
    }

    public void render(Painter p, FxProjection pr) {
        if (!pr.valid()) return;
        for (FxEffect e : active) e.render(p, pr);
        particles.render(p, pr, 140f);
    }

    public void fill(FxParams out) {
        out.impact = impactLevel; out.energy = Math.min(1f, energyLevel); out.glow = glowLevel; out.portal = portalLevel;
    }

    /** Return every effect to its pool and drop all particles. */
    public void cleanup() {
        for (FxEffect e : active) {
            e.cleanup();
            if (e instanceof EnergyEffect) poolEnergy.addLast(e);
            else if (e instanceof GlowEffect) poolGlow.addLast(e);
            else if (e instanceof PortalEffect) poolPortal.addLast(e);
            else poolRays.addLast(e);
        }
        active.clear(); particles.clear();
        impactLevel = glowLevel = energyLevel = portalLevel = 0f; portalUntil = -1;
    }
}
