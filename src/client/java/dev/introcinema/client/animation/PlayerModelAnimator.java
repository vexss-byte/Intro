package dev.introcinema.client.animation;

import dev.introcinema.client.animation.PlayerPose.Part;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;

/**
 * Applies a PlayerPose ON TOP of the vanilla pose of one model, just after vanilla setupAnim.
 * Additive for rotation/translation and multiplicative for scale, so walking, looking and item poses still work.
 * Model parts are rebuilt by vanilla for every render state, so nothing leaks into the next entity or frame.
 */
public final class PlayerModelAnimator {
    private static final float DEG = (float) (Math.PI / 180.0);

    private PlayerModelAnimator() {}

    public static void apply(PlayerModel m, PlayerPose p) {
        applyParts(m.head, m.body, m.leftArm, m.rightArm, m.leftLeg, m.rightLeg, p);
    }

    public static void applyParts(ModelPart head, ModelPart body, ModelPart leftArm, ModelPart rightArm,
                                  ModelPart leftLeg, ModelPart rightLeg, PlayerPose p) {
        part(head, p, Part.HEAD);
        part(body, p, Part.BODY);
        part(leftArm, p, Part.LEFT_ARM);
        part(rightArm, p, Part.RIGHT_ARM);
        part(leftLeg, p, Part.LEFT_LEG);
        part(rightLeg, p, Part.RIGHT_LEG);
    }

    private static void part(ModelPart mp, PlayerPose p, Part part) {
        mp.xRot += p.v[PlayerPose.index(part, PlayerPose.ROT)] * DEG;
        mp.yRot += p.v[PlayerPose.index(part, PlayerPose.ROT + 1)] * DEG;
        mp.zRot += p.v[PlayerPose.index(part, PlayerPose.ROT + 2)] * DEG;
        mp.x += p.v[PlayerPose.index(part, PlayerPose.POS)];
        mp.y += p.v[PlayerPose.index(part, PlayerPose.POS + 1)];
        mp.z += p.v[PlayerPose.index(part, PlayerPose.POS + 2)];
        mp.xScale *= p.v[PlayerPose.index(part, PlayerPose.SCALE)];
        mp.yScale *= p.v[PlayerPose.index(part, PlayerPose.SCALE + 1)];
        mp.zScale *= p.v[PlayerPose.index(part, PlayerPose.SCALE + 2)];
    }
}
