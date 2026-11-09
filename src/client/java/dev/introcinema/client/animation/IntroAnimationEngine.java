package dev.introcinema.client.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player animation state, driven by the shared cinematic time. The renderer asks
 * {@link #poseFor} once per player per frame; players without an active clip return null so the
 * vanilla pose stays completely untouched.
 */
public final class IntroAnimationEngine {
    private final Map<UUID, AnimationBlender> blenders = new HashMap<>();
    private final Map<UUID, PlayerPose> shown = new HashMap<>();
    private final PlayerPose out = new PlayerPose();
    private boolean enabled = true;

    public void setEnabled(boolean e) { enabled = e; if (!e) stopAll(); }

    public void play(UUID id, String clipName, float now, float fadeTicks) {
        if (!enabled) return;
        AnimationBlender b = blenders.computeIfAbsent(id, k -> new AnimationBlender());
        b.transitionTo(PoseLibrary.get(clipName), now, fadeTicks, shown.get(id));
    }

    /** Returns a reused PlayerPose instance (valid until the next call), or null for "use vanilla". */
    public PlayerPose poseFor(UUID id, float now) {
        if (!enabled) return null;
        AnimationBlender b = blenders.get(id);
        if (b == null || !b.active()) return null;
        b.sample(now, out);
        shown.computeIfAbsent(id, k -> new PlayerPose()).copyFrom(out);
        return out;
    }

    public void stop(UUID id) { AnimationBlender b = blenders.remove(id); if (b != null) b.clear(); shown.remove(id); }

    /** Full reset after the cutscene: no clip survives, no pose leaks into normal rendering. */
    public void stopAll() { blenders.clear(); shown.clear(); out.reset(); }
}
