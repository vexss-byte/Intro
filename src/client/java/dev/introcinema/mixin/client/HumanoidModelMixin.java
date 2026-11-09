package dev.introcinema.mixin.client;

import dev.introcinema.client.animation.PlayerPose;
import dev.introcinema.client.animation.PlayerModelAnimator;
import dev.introcinema.client.render.PoseKeys;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** OPTIONAL config: makes armor models follow the same pose. Failing to apply only logs a warning. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void intro$armor(HumanoidRenderState state, CallbackInfo ci) {
        if ((Object) this instanceof PlayerModel) return; // the player model is handled by PlayerModelMixin
        PoseKeys.Holder h = ((FabricRenderState) state).getData(PoseKeys.POSE);
        if (h == null || h.pose() == null) return;
        HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
        PlayerPose p = h.pose();
        PlayerModelAnimator.applyParts(m.head, m.body, m.leftArm, m.rightArm, m.leftLeg, m.rightLeg, p);
    }
}
