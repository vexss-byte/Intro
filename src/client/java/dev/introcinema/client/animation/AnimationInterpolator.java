package dev.introcinema.client.animation;

/** Easing + interpolation helpers shared by animation and camera. */
public final class AnimationInterpolator {
    public enum Easing {
        LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, SMOOTHSTEP, CUBIC;

        public float apply(float t) {
            t = Math.max(0f, Math.min(1f, t));
            return switch (this) {
                case LINEAR -> t;
                case EASE_IN -> t * t;
                case EASE_OUT -> 1f - (1f - t) * (1f - t);
                case EASE_IN_OUT -> t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
                case SMOOTHSTEP -> t * t * (3f - 2f * t);
                case CUBIC -> t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
            };
        }
    }

    private AnimationInterpolator() {}

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Interpolates angles in degrees along the shortest arc. */
    public static float lerpAngle(float a, float b, float t) {
        float d = ((b - a) % 360f + 540f) % 360f - 180f;
        return a + d * t;
    }
}
