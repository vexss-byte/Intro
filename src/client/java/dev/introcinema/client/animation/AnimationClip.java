package dev.introcinema.client.animation;

import java.util.ArrayList;
import java.util.List;

/** A named set of channels. Channels not present leave the neutral pose value untouched. */
public final class AnimationClip {
    public final String name;
    public final float duration;
    public final boolean loop;
    public final float speed;
    private final List<AnimationChannel> channels = new ArrayList<>();
    private final float[] bodyYawKeys; // optional {time0,val0,time1,val1,...}

    public AnimationClip(String name, float duration, boolean loop, float speed, List<AnimationChannel> channels) {
        this(name, duration, loop, speed, channels, null);
    }

    public AnimationClip(String name, float duration, boolean loop, float speed, List<AnimationChannel> channels, float[] bodyYawKeys) {
        this.name = name; this.duration = Math.max(1f, duration); this.loop = loop; this.speed = speed;
        this.channels.addAll(channels);
        this.bodyYawKeys = bodyYawKeys;
    }

    /** Writes the clip's pose at {@code time} (ticks since clip start, already including fractional ticks) into out. */
    public void sample(float time, PlayerPose out) {
        float t = time * speed;
        t = loop ? ((t % duration) + duration) % duration : Math.min(t, duration);
        for (AnimationChannel c : channels) out.v[c.target] = c.sample(t);
        if (bodyYawKeys != null) {
            int n = bodyYawKeys.length / 2;
            float yaw = bodyYawKeys[1];
            for (int i = 1; i < n; i++) {
                float t0 = bodyYawKeys[(i - 1) * 2], t1 = bodyYawKeys[i * 2];
                if (t <= t1) {
                    float u = t1 <= t0 ? 1f : Math.max(0f, (t - t0) / (t1 - t0));
                    yaw = AnimationInterpolator.lerp(bodyYawKeys[(i - 1) * 2 + 1], bodyYawKeys[i * 2 + 1], AnimationInterpolator.Easing.SMOOTHSTEP.apply(u));
                    break;
                }
                yaw = bodyYawKeys[i * 2 + 1];
            }
            out.bodyYaw = yaw;
        }
    }
}
