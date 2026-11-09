package dev.introcinema.client;

import dev.introcinema.IntroCinemaMod;
import dev.introcinema.client.animation.IntroAnimationEngine;
import dev.introcinema.client.animation.PlayerPose;
import dev.introcinema.client.animation.PlayerPoseController;
import dev.introcinema.client.camera.CameraController;
import dev.introcinema.client.camera.CameraPose;
import dev.introcinema.client.camera.CameraTimeline;
import dev.introcinema.client.camera.IntroDirector;
import dev.introcinema.client.cinematic.CinematicScheduler;
import dev.introcinema.client.cinematic.CinematicSequence;
import dev.introcinema.client.cinematic.CinematicTimeline;
import dev.introcinema.client.fx.CinematicFXManager;
import dev.introcinema.client.fx.ScreenEffectController;
import dev.introcinema.client.hud.HudState;
import dev.introcinema.client.session.ClientSession;
import dev.introcinema.client.shader.ShaderManager;
import dev.introcinema.common.network.IntroPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Client brain: receives server messages, runs the shared cinematic clock and owns every client-side system.
 * Any failure shuts the visuals down and gives control back; it can never block the server-side restore.
 */
public final class IntroClientController implements CinematicSequence.Ctx {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/client");
    private static final IntroClientController INSTANCE = new IntroClientController();
    public static IntroClientController get() { return INSTANCE; }

    public final IntroAnimationEngine anim = new IntroAnimationEngine();
    public final CameraController camera = new CameraController();
    public final HudState hud = new HudState();
    public final CinematicFXManager fx = new CinematicFXManager();
    public final ScreenEffectController screen = new ScreenEffectController();
    private final PlayerPoseController poses = new PlayerPoseController(anim);

    private ClientSession session;
    private CinematicScheduler scheduler;
    private IntroDirector director;
    private volatile boolean active;
    private int clientTick, serverTick, ticksSinceSync;
    private float frameTick;
    private int holdBlack;   // ticks to keep the screen black after a normal ending, until the restore teleport lands

    private IntroClientController() {}

    public boolean active() { return active; }
    public ClientSession session() { return session; }

    // ---- Ctx ---------------------------------------------------------------------------------
    @Override public float now() { return frameTick; }
    @Override public HudState hud() { return hud; }
    @Override public IntroAnimationEngine anim() { return anim; }
    @Override public PlayerPoseController poses() { return poses; }
    @Override public CinematicFXManager fx() { return fx; }
    @Override public ScreenEffectController screen() { return screen; }

    // ---- network handlers (called on the client main thread) -----------------------------------
    public void onStart(IntroPayload.Start p) {
        if (active) shutdown("restarted");
        try {
            session = new ClientSession(p);
            anim.setEnabled(session.animationOn);
            screen.setEnabled(session.screenFxOn);
            fx.configure(session.qualityCap());
            boolean shaders = session.shaderOn && ShaderManager.get().ensureReady();
            if (session.shaderOn && !shaders) report(false, "shaders unavailable: " + ShaderManager.get().lastError());

            CinematicSequence.Built b = CinematicSequence.build(session, this);
            CinematicTimeline timeline = b.timeline();
            CameraTimeline camTl = b.camera();
            scheduler = new CinematicScheduler(timeline, (e, t) -> LOG.warn("Event '{}' failed: {}", e.name(), t.toString()));
            director = new IntroDirector(session, camTl, camera);
            clientTick = serverTick = p.startTick(); ticksSinceSync = 0; frameTick = clientTick;
            screen.fade(clientTick, clientTick + 24, 1f, 0f); // open from black, never from a raw cut
            active = true;
            report(true, "ok");
            LOG.info("Cinematic {} started: {} members, {} ticks", session.id, session.members.size(), session.plan.total());
        } catch (Throwable t) {
            fail("start failed", t);
        }
    }

    public void onSync(IntroPayload.Sync p) {
        if (!active || !p.sessionId().equals(session.id)) return;       // wrong/stale session
        if (p.tick() < serverTick) return;                                // late or duplicate payload
        serverTick = p.tick(); ticksSinceSync = 0;
    }

    public void onReveal(IntroPayload.Reveal p) {
        if (active && p.sessionId().equals(session.id)) session.reveal(p.uuid(), p.name());
    }

    public void onEnd(IntroPayload.End p) {
        if (session == null || !p.sessionId().equals(session.id)) return;
        if (p.reason().equals("finished") && active) {
            screen.fade((int) frameTick, (int) frameTick + 1, 1f, 1f); // stay black: no glimpse of the arena before the teleport
            holdBlack = 24;
        } else shutdown(p.reason());
    }

    // ---- per-tick / per-frame ------------------------------------------------------------------
    public void tick(Minecraft mc) {
        if (!active) return;
        try {
            if (mc.isPaused()) return;
            ticksSinceSync++;
            clientTick++;
            int expected = serverTick + ticksSinceSync;
            if (clientTick < expected) clientTick = expected;             // lagging client catches up
            if (clientTick > expected + 4) clientTick = expected + 4;     // never run far ahead of the server clock
            frameTick = clientTick;
            scheduler.advanceTo(clientTick);
            fx.update(clientTick);
            if (mc.gui.screen() == null) KeyMapping.releaseAll();               // no walking/attacking while the show runs
            if (holdBlack > 0 && --holdBlack == 0) { shutdown("finished"); return; }
            if (clientTick >= session.plan.total() + 60) shutdown("timeout"); // server end message lost: stop anyway
        } catch (Throwable t) {
            fail("tick failed", t);
        }
    }

    /** Called from the Camera mixin each frame; returns the pose to apply, or null for vanilla. */
    public CameraPose cameraFrame(float partial) {
        if (!active) return null;
        try {
            frameTick = clientTick + Math.max(0f, Math.min(1f, partial));
            director.frame(frameTick);
            return camera.current();
        } catch (Throwable t) {
            fail("camera failed", t);
            return null;
        }
    }

    /** Pose override for one player (null = vanilla). Returns a copy because the engine reuses its instance. */
    public PlayerPose poseCopyFor(UUID id) {
        if (!active) return null;
        PlayerPose p = anim.poseFor(id, frameTick);
        return p == null ? null : new PlayerPose().copyFrom(p);
    }

    public boolean isSilhouette(UUID id) {
        if (!active || session == null) return false;
        int i = session.indexOf(id);
        return i >= 0 && session.members.get(i).silhouette && !session.members.get(i).revealed;
    }

    // ---- shutdown --------------------------------------------------------------------------------
    /** Restores normal rendering, camera and controls. Safe to call repeatedly. */
    public void shutdown(String reason) {
        boolean was = active;
        active = false; holdBlack = 0;
        try {
            anim.stopAll(); camera.clear(); hud.reset(); fx.cleanup(); screen.reset();
            if (director != null) director.reset();
            ShaderManager.get().releaseFrameResources();
        } catch (Throwable t) { LOG.warn("Cleanup problem: {}", t.toString()); }
        session = null; scheduler = null; director = null;
        if (was) LOG.info("Cinematic ended ({})", reason);
    }

    private void fail(String what, Throwable t) {
        LOG.error("Cinematic {}: {}. Visuals disabled; the server still restores you.", what, t.toString(), t);
        String id = session == null ? "" : session.id;
        shutdown("error");
        sendStatus(id, false, what + ": " + t);
    }

    private void report(boolean ok, String msg) {
        if (session != null) sendStatus(session.id, ok, msg);
    }

    private static void sendStatus(String id, boolean ok, String msg) {
        try {
            if (ClientPlayNetworking.canSend(IntroPayload.ClientStatus.TYPE))
                ClientPlayNetworking.send(new IntroPayload.ClientStatus(id, ok, msg.length() > 200 ? msg.substring(0, 200) : msg));
        } catch (Throwable ignored) { /* reporting is best effort */ }
    }
}
