package dev.introcinema.client.animation;

import dev.introcinema.client.animation.AnimationInterpolator.Easing;

/** A value at a time (ticks). {@code easing} shapes the segment that ENDS at this keyframe. */
public record Keyframe(float time, float value, Easing easing) {
    public Keyframe(float time, float value) { this(time, value, Easing.SMOOTHSTEP); }
}
