package dev.introcinema.client.animation;

import dev.introcinema.client.animation.AnimationInterpolator.Easing;
import dev.introcinema.client.animation.PlayerPose.Part;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Built-in clips. All angles in degrees; arms: X negative lifts forward, Z spreads outward. */
public final class PoseLibrary {
    public static final String IDLE = "idle", CINEMATIC_IDLE = "cinematic_idle", HERO = "hero", VICTORY = "victory",
            WEAPON = "weapon", LOOK_AROUND = "look_around", LOOK_AT_CAMERA = "look_at_camera";
    private static final Map<String, AnimationClip> CLIPS = new HashMap<>();

    static {
        register(clip(IDLE, 60, true, c -> {
            c.add(ch(Part.HEAD, 0).key(0, 0).key(30, 1.5f).key(60, 0));
            c.add(ch(Part.RIGHT_ARM, 2).key(0, 2).key(30, 4).key(60, 2));
            c.add(ch(Part.LEFT_ARM, 2).key(0, -2).key(30, -4).key(60, -2));
        }));
        register(clip(CINEMATIC_IDLE, 80, true, c -> {
            c.add(ch(Part.BODY, 0).key(0, 0).key(40, 1f).key(80, 0));
            c.add(ch(Part.RIGHT_ARM, 2).key(0, 6).key(40, 9).key(80, 6));
            c.add(ch(Part.LEFT_ARM, 2).key(0, -6).key(40, -9).key(80, -6));
            c.add(ch(Part.HEAD, 0).key(0, -2).key(40, 0).key(80, -2));
        }));
        register(clip(HERO, 50, false, c -> {
            c.add(ch(Part.RIGHT_ARM, 0).key(0, 0).key(18, -150, Easing.EASE_OUT).key(50, -160));
            c.add(ch(Part.RIGHT_ARM, 2).key(0, 2).key(18, 12).key(50, 14));
            c.add(ch(Part.LEFT_ARM, 2).key(0, -2).key(20, -25, Easing.EASE_OUT).key(50, -25));
            c.add(ch(Part.HEAD, 0).key(0, 0).key(20, -12, Easing.EASE_OUT).key(50, -12));
            c.add(ch(Part.BODY, 0).key(0, 0).key(20, -4).key(50, -4));
        }));
        register(clip(VICTORY, 60, false, c -> {
            c.add(ch(Part.RIGHT_ARM, 0).key(0, 0).key(15, -170, Easing.EASE_OUT).key(60, -175));
            c.add(ch(Part.LEFT_ARM, 0).key(0, 0).key(15, -170, Easing.EASE_OUT).key(60, -175));
            c.add(ch(Part.HEAD, 0).key(0, 0).key(20, -18, Easing.EASE_OUT).key(60, -18));
        }));
        register(clip(WEAPON, 40, false, c -> {
            c.add(ch(Part.RIGHT_ARM, 0).key(0, 0).key(14, -75, Easing.EASE_OUT).key(40, -80));
            c.add(ch(Part.RIGHT_ARM, 1).key(0, 0).key(14, -15).key(40, -15));
            c.add(ch(Part.LEFT_ARM, 0).key(0, 0).key(14, -40, Easing.EASE_OUT).key(40, -45));
            c.add(ch(Part.BODY, 1).key(0, 0).key(14, 12).key(40, 12));
        }));
        register(new AnimationClip(LOOK_AROUND, 120, true, 1f, listOf(
                ch(Part.HEAD, 1).key(0, 0).key(30, 35).key(60, 0).key(90, -35).key(120, 0))));
        register(new AnimationClip(LOOK_AT_CAMERA, 30, false, 1f, listOf(
                ch(Part.HEAD, 1).key(0, 0).key(30, 0)), new float[]{0, 0, 30, 0}));
    }

    private PoseLibrary() {}

    public static AnimationClip get(String name) { return CLIPS.getOrDefault(name, CLIPS.get(IDLE)); }
    public static void register(AnimationClip c) { CLIPS.put(c.name, c); }

    private static AnimationChannel ch(Part p, int axis) { return new AnimationChannel(p, PlayerPose.ROT + axis); }

    private static List<AnimationChannel> listOf(AnimationChannel... cs) { return new ArrayList<>(List.of(cs)); }

    private static AnimationClip clip(String name, float dur, boolean loop, java.util.function.Consumer<List<AnimationChannel>> fill) {
        List<AnimationChannel> list = new ArrayList<>();
        fill.accept(list);
        return new AnimationClip(name, dur, loop, 1f, list);
    }
}
