package dev.introcinema.client.camera;

import dev.introcinema.client.animation.AnimationInterpolator;
import dev.introcinema.client.animation.AnimationInterpolator.Easing;

public final class CameraInterpolator {
    private CameraInterpolator() {}

    public static CameraPose lerp(CameraPose a, CameraPose b, float t) {
        return new CameraPose(
                a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t, a.z() + (b.z() - a.z()) * t,
                AnimationInterpolator.lerpAngle(a.yaw(), b.yaw(), t),
                AnimationInterpolator.lerp(a.pitch(), b.pitch(), t),
                AnimationInterpolator.lerp(a.roll(), b.roll(), t),
                AnimationInterpolator.lerp(a.fov(), b.fov(), t));
    }

    public static CameraPose lerp(CameraPose a, CameraPose b, float t, Easing e) { return lerp(a, b, e.apply(t)); }
}
