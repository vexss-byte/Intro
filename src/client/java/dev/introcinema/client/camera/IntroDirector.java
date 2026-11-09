package dev.introcinema.client.camera;

import dev.introcinema.client.session.ClientSession;
import dev.introcinema.common.dimension.ArenaLayout;
import dev.introcinema.common.session.SessionPlan;
import net.minecraft.world.phys.Vec3;

/** Turns the shared cinematic time into a camera pose each frame (timeline -> collision -> smoothing). */
public final class IntroDirector {
    private final ClientSession session;
    private final CameraTimeline timeline;
    private final CameraController controller;
    private final CinematicCamera smoother = new CinematicCamera();
    private long lastNanos;

    public IntroDirector(ClientSession s, CameraTimeline t, CameraController c) { session = s; timeline = t; controller = c; }

    public void frame(float cinematicTick) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : (now - lastNanos) / 1e9f;
        lastNanos = now;
        if (!session.cameraOn || timeline.isEmpty()) { controller.clear(); return; }
        CameraPose p = timeline.sample(cinematicTick);
        if (p == null) { controller.clear(); return; }
        p = CameraCollision.resolve(p, focus((int) cinematicTick));
        controller.set(smoother.smooth(p, dt));
    }

    private Vec3 focus(int tick) {
        SessionPlan plan = session.plan;
        return switch (plan.sceneAt(tick)) {
            case MEMBER_REVEAL -> new Vec3(ArenaLayout.STAGE_X, ArenaLayout.STAND_Y + 1.2, ArenaLayout.STAGE_Z);
            case FINALE, RETURN -> new Vec3(ArenaLayout.STAGE_X, ArenaLayout.STAND_Y + 1.0, ArenaLayout.STAGE_Z + 4);
            default -> new Vec3(ArenaLayout.STAGE_X, ArenaLayout.STAND_Y + 2.0, ArenaLayout.STAGE_Z);
        };
    }

    public void reset() { smoother.reset(); controller.clear(); }
}
