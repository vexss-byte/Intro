package dev.introcinema.client.animation;

import dev.introcinema.client.animation.AnimationInterpolator.Easing;

/** Crossfades from the previous pose to the current clip over a fixed number of ticks. */
public final class AnimationBlender {
    private final PlayerPose from = new PlayerPose();
    private final PlayerPose scratch = new PlayerPose();
    private AnimationClip clip;
    private float clipStart;
    private float fadeTicks;
    private boolean hasFrom;

    /** Starts {@code next} at {@code now}; the currently shown pose becomes the fade source. */
    public void transitionTo(AnimationClip next, float now, float fadeTicks, PlayerPose currentShown) {
        if (currentShown != null) { from.copyFrom(currentShown); hasFrom = true; }
        this.clip = next; this.clipStart = now; this.fadeTicks = Math.max(0f, fadeTicks);
    }

    public boolean active() { return clip != null; }
    public void clear() { clip = null; hasFrom = false; }

    public static void blend(PlayerPose a, PlayerPose b, float w, PlayerPose out) {
        for (int i = 0; i < PlayerPose.CHANNELS; i++) out.v[i] = AnimationInterpolator.lerp(a.v[i], b.v[i], w);
        out.bodyYaw = AnimationInterpolator.lerp(a.bodyYaw, b.bodyYaw, w);
    }

    public void sample(float now, PlayerPose out) {
        if (clip == null) { out.reset(); return; }
        scratch.reset();
        clip.sample(now - clipStart, scratch);
        float elapsed = now - clipStart;
        if (hasFrom && fadeTicks > 0f && elapsed < fadeTicks) {
            blend(from, scratch, Easing.SMOOTHSTEP.apply(elapsed / fadeTicks), out);
        } else out.copyFrom(scratch);
    }
}
