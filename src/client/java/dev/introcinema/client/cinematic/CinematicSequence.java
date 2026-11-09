package dev.introcinema.client.cinematic;

import dev.introcinema.IntroCinemaMod;
import dev.introcinema.client.animation.IntroAnimationEngine;
import dev.introcinema.client.animation.PlayerPoseController;
import dev.introcinema.client.camera.CameraPose;
import dev.introcinema.client.camera.CameraTimeline;
import dev.introcinema.client.animation.AnimationInterpolator.Easing;
import dev.introcinema.client.fx.CinematicFXManager;
import dev.introcinema.client.fx.ScreenEffectController;
import dev.introcinema.client.hud.HudState;
import dev.introcinema.client.session.ClientSession;
import dev.introcinema.common.dimension.ArenaLayout;
import dev.introcinema.common.session.SessionPlan;

/**
 * The director's script: builds the ONE timeline (all tracks) and the camera shot list from the roster and plan.
 * Scene 1 opening, scene 2 member reveals (nobody skipped), scene 3 grand finale, scene 4 return/fade.
 */
public final class CinematicSequence {
    public interface Ctx {
        float now();
        HudState hud();
        IntroAnimationEngine anim();
        PlayerPoseController poses();
        CinematicFXManager fx();
        ScreenEffectController screen();
    }

    public record Built(CinematicTimeline timeline, CameraTimeline camera) {}

    private static final int[] PALETTE = {0xFFC83C, 0xFF5FA2, 0x6BE04E, 0xA066FF, 0xFF8A1F, 0xFF3B3B, 0xFFFFFF, 0x4FD8FF};
    private static final int GOLD = 0xFFC83C, ICE = 0x9CE6FF;

    private CinematicSequence() {}

    public static int color(int memberNumber) { return PALETTE[Math.floorMod(memberNumber - 1, PALETTE.length)]; }

    public static Built build(ClientSession s, Ctx c) {
        SessionPlan plan = s.plan;
        int total = plan.total();
        CinematicTimeline tl = new CinematicTimeline(total);
        CameraTimeline cam = new CameraTimeline(8);
        final double sx = ArenaLayout.STAGE_X, sy = ArenaLayout.STAND_Y, sz = ArenaLayout.STAGE_Z, floor = ArenaLayout.FLOOR_Y + 1.02;
        final float k = s.cameraSpeed;

        CinematicTrack tAnim = tl.track("animation"), tPart = tl.track("particle"), tShader = tl.track("shader"),
                tName = tl.track("nameplate"), tLight = tl.track("lighting");
        tl.track("sound"); // lane exists on the shared clock; no sound assets are shipped yet

        // ---------------- SCENE 1: opening ----------------
        int op = plan.opening();
        tAnim.at(0, "audience-idle", () -> c.poses().startAudience(s, c.now()));
        tPart.at(0, "ambient", () -> c.fx().floatingGlow(GOLD, sx, floor, sz, 22f, total, 26));
        tPart.at(20, "stage-circle", () -> c.fx().groundCircle(GOLD, sx, floor, sz, 5.5f, op + 20));
        tLight.at(40, "stage-rays", () -> c.fx().lightRays(ICE, sx, floor, sz, 3, op));
        tName.at(35, "server-title", () -> c.hud().title(s.title, s.subtitle, 35, op - 14));
        tShader.at(Math.max(0, op - 8), "to-members", () -> c.screen().pixelTransition(op - 8, op + 8));

        int op1 = (int) (op * 0.55f);
        cam.add(0, op1, dolly(new double[]{sx, 92, -64}, scaleTo(new double[]{sx, 92, -64}, new double[]{sx, 78, -40}, k), new double[]{sx, 68, sz}, 70f, 64f, 0f, 0f), Easing.SMOOTHSTEP);
        cam.add(op1, op, dolly(new double[]{8, 72, -28}, scaleTo(new double[]{8, 72, -28}, new double[]{sx, 66.8, -9}, k), new double[]{sx, 66.5, sz}, 62f, 56f, 0f, 0f), Easing.EASE_IN_OUT);

        // ---------------- SCENE 2: member reveal ----------------
        int mt = plan.memberTicks();
        for (int i = 0; i < plan.members(); i++) {
            final int idx = i;
            final ClientSession.Member m = s.members.get(i);
            final int col = color(m.number);
            final int s0 = plan.memberStart(i);
            final int reveal = s0 + (int) (mt * (m.silhouette ? 0.40f : 0.12f));

            if (i > 0) tShader.at(s0 - 8, "transition-" + m.number, () -> c.screen().pixelTransition(s0 - 8, s0 + 8));
            tName.at(s0 + 1, "plate-" + m.number, () -> c.hud().showMember(idx, s0 + 1, s0 + mt - 8));
            tName.at(s0 + mt - 8, "plate-hide-" + m.number, () -> c.hud().hideMember());
            if (m.silhouette && m.last) {
                String caption = IntroCinemaMod.config().lastCaption;
                if (!caption.isEmpty()) tName.at(s0 + 2, "caption-last", () -> c.hud().caption(caption, s0 + 2, reveal));
            }
            tAnim.at(s0 + 2, "feature-" + m.number, () -> c.poses().feature(s, idx, c.now()));
            tAnim.at(s0 + (int) (mt * 0.72f), "relax-" + m.number, () -> c.poses().relax(s, idx, c.now()));
            tPart.at(s0 + 3, "aura-" + m.number, () -> {
                c.fx().groundCircle(col, sx, floor, sz, 3.2f, (int) (mt * 0.85f));
                c.fx().floatingGlow(col, sx, floor, sz, 4f, (int) (mt * 0.9f), 34);
            });
            tPart.at(reveal, "burst-" + m.number, () -> {
                c.fx().burst(col, sx, sy + 1.2, sz, 1.3f);
                c.fx().impact(col, sx, floor, sz);
                c.fx().energyTrail(col, sx, floor, sz, 2.4f, 70);
                c.screen().flash(reveal, 8, 0.45f, 0xFFF2C8);
            });

            int a1 = s0 + (int) (mt * 0.35f), a2 = s0 + (int) (mt * 0.70f), a3 = s0 + mt;
            double headY = sy + 1.3;
            cam.add(s0, a1, dolly(new double[]{sx + 2.4, headY + 0.5, sz - 4.6}, scaleTo(new double[]{sx + 2.4, headY + 0.5, sz - 4.6}, new double[]{sx + 1.2, headY + 0.1, sz - 3.4}, k),
                    new double[]{sx, headY, sz}, 66f, 58f, 0f, 0f), Easing.SMOOTHSTEP);
            cam.add(a1, a2, CameraTimeline.orbit(sx, headY - 0.3, sz, 3.3, 0.5, -40 * k, 45 * k, 58f, 54f, 0f), Easing.EASE_IN_OUT);
            cam.add(a2, a3, dolly(new double[]{sx - 1.4, sy + 0.4, sz - 3.2}, scaleTo(new double[]{sx - 1.4, sy + 0.4, sz - 3.2}, new double[]{sx - 0.5, sy + 0.6, sz - 2.5}, k),
                    new double[]{sx, sy + 1.6, sz}, 74f, 68f, 0f, 0f), Easing.EASE_OUT);
        }

        // ---------------- SCENE 3: grand finale ----------------
        int f0 = plan.finaleStart(), rs = plan.returnStart();
        int rows = ArenaLayout.finaleRows(plan.members());
        double zc = sz + 2 + (rows - 1) * 1.1;
        tName.at(f0, "plate-clear", () -> c.hud().hideMember());
        tShader.at(f0 - 8, "to-finale", () -> c.screen().pixelTransition(f0 - 8, f0 + 8));
        tAnim.at(f0 + 8, "finale-pose", () -> c.poses().finale(s, c.now()));
        tPart.at(f0 + 10, "finale-portal", () -> c.fx().portal(GOLD, sx, floor, zc + 6, 5f, plan.finale() - 30, f0 + 10));
        tPart.at(f0 + 12, "finale-burst", () -> {
            c.fx().burst(GOLD, sx, sy + 3, zc, 2.4f);
            c.fx().energyRing(0xFF8A1F, sx, floor, zc, 16f, 24);
            c.fx().energyWaves(ICE, sx, floor, zc, 20f, plan.finale() - 40);
            c.screen().flash(f0 + 12, 16, 0.7f, 0xFFF2C8);
        });
        tLight.at(f0 + 14, "finale-rays", () -> c.fx().lightRays(GOLD, sx, floor, zc, 5, plan.finale() - 20));
        tName.at(f0 + 36, "finale-title", () -> c.hud().title(s.title, "", f0 + 36, rs - 16));
        int f1 = f0 + (int) (plan.finale() * 0.30f), f2 = f0 + (int) (plan.finale() * 0.70f);
        double wideY = sy + 4.0;
        cam.add(f0, f1, dolly(new double[]{sx, sy + 1.6, -6}, scaleTo(new double[]{sx, sy + 1.6, -6}, new double[]{sx, sy + 5, -20}, k), new double[]{sx, sy + 1.2, zc}, 62f, 70f, 0f, 0f), Easing.SMOOTHSTEP);
        cam.add(f1, f2, CameraTimeline.orbit(sx, wideY + 0.5, zc, 26 + rows * 2, 5, -28 * k, 28 * k, 68f, 62f, 0f), Easing.EASE_IN_OUT);
        cam.add(f2, total, dolly(new double[]{sx, sy + 8, -30 - rows}, scaleTo(new double[]{sx, sy + 8, -30 - rows}, new double[]{sx, sy + 22, -52 - rows * 2}, k), new double[]{sx, sy + 1, zc}, 76f, 72f, 0f, 0f), Easing.EASE_IN_OUT);

        // ---------------- SCENE 4: return ----------------
        tShader.at(Math.max(0, rs - 10), "fade-out", () -> c.screen().fade(rs - 10, total - 2, 0f, 1f));
        return new Built(tl, cam);
    }

    private static double[] scaleTo(double[] from, double[] to, float k) {
        return new double[]{from[0] + (to[0] - from[0]) * k, from[1] + (to[1] - from[1]) * k, from[2] + (to[2] - from[2]) * k};
    }

    private static CameraTimeline.Shot dolly(double[] from, double[] to, double[] target, float fov0, float fov1, float roll0, float roll1) {
        return p -> CameraPose.lookAt(from[0] + (to[0] - from[0]) * p, from[1] + (to[1] - from[1]) * p, from[2] + (to[2] - from[2]) * p,
                target[0], target[1], target[2], roll0 + (roll1 - roll0) * p, fov0 + (fov1 - fov0) * p);
    }
}
