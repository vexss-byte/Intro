package dev.introcinema.mixin.client;

import dev.introcinema.client.animation.PlayerModelAnimator;
import dev.introcinema.client.render.PoseKeys;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the pose right after vanilla setupAnim of the player model; vanilla rebuilds the parts for every state. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void intro$pose(AvatarRenderState state, CallbackInfo ci) {
        PoseKeys.Holder h = ((FabricRenderState) state).getData(PoseKeys.POSE);
        if (h != null && h.pose() != null) PlayerModelAnimator.apply((PlayerModel) (Object) this, h.pose());
    }
}
