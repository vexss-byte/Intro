package dev.introcinema.client.animation;

import java.util.Arrays;

/**
 * Additive-free pose override for the six vanilla player parts. Per part: rotX/Y/Z (degrees),
 * posX/Y/Z (model units), scaleX/Y/Z. Pure data: applying it to a model is the renderer's job,
 * so the pose can never touch hitbox, physics or server position.
 */
public final class PlayerPose {
    public enum Part { HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }
    public static final int PROPS = 9; // rot3, pos3, scale3
    public static final int ROT = 0, POS = 3, SCALE = 6;
    public static final int CHANNELS = Part.values().length * PROPS;

    public final float[] v = new float[CHANNELS];
    /** Extra whole-body yaw (degrees) applied around the vertical axis for turning toward the camera. */
    public float bodyYaw;

    public PlayerPose() { reset(); }

    public static int index(Part p, int prop) { return p.ordinal() * PROPS + prop; }

    public PlayerPose reset() {
        Arrays.fill(v, 0f);
        for (Part p : Part.values()) for (int i = 0; i < 3; i++) v[index(p, SCALE + i)] = 1f;
        bodyYaw = 0f;
        return this;
    }

    public PlayerPose set(Part p, float rx, float ry, float rz) {
        v[index(p, ROT)] = rx; v[index(p, ROT + 1)] = ry; v[index(p, ROT + 2)] = rz;
        return this;
    }

    public PlayerPose copyFrom(PlayerPose o) {
        System.arraycopy(o.v, 0, v, 0, CHANNELS);
        bodyYaw = o.bodyYaw;
        return this;
    }

    public float rot(Part p, int axis) { return v[index(p, ROT + axis)]; }
}
