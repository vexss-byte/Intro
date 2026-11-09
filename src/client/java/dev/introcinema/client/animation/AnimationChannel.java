package dev.introcinema.client.animation;

import java.util.ArrayList;
import java.util.List;

/** Keyframes for one pose value (one part + one property). Keys must be added in time order. */
public final class AnimationChannel {
    public final int target;
    private final List<Keyframe> keys = new ArrayList<>();

    public AnimationChannel(PlayerPose.Part part, int prop) { this.target = PlayerPose.index(part, prop); }

    public AnimationChannel key(float time, float value) { return key(new Keyframe(time, value)); }
    public AnimationChannel key(float time, float value, AnimationInterpolator.Easing e) { return key(new Keyframe(time, value, e)); }

    public AnimationChannel key(Keyframe k) {
        if (!keys.isEmpty() && k.time() < keys.get(keys.size() - 1).time())
            throw new IllegalArgumentException("Keyframes must be in ascending time order");
        keys.add(k);
        return this;
    }

    public float duration() { return keys.isEmpty() ? 0f : keys.get(keys.size() - 1).time(); }

    /** Value at {@code t}; clamps before the first and after the last key. */
    public float sample(float t) {
        int n = keys.size();
        if (n == 0) return 0f;
        if (t <= keys.get(0).time()) return keys.get(0).value();
        if (t >= keys.get(n - 1).time()) return keys.get(n - 1).value();
        int hi = 1;
        while (keys.get(hi).time() < t) hi++;
        Keyframe a = keys.get(hi - 1), b = keys.get(hi);
        float span = b.time() - a.time();
        float u = span <= 0f ? 1f : (t - a.time()) / span;
        return AnimationInterpolator.lerp(a.value(), b.value(), b.easing().apply(u));
    }
}
