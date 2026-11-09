package dev.introcinema.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.introcinema.client.IntroClientController;
import dev.introcinema.client.animation.PlayerPose;
import dev.introcinema.client.render.PoseKeys;
import dev.introcinema.client.render.SilhouetteRender;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Puts the pose (and silhouette flag) for THIS player onto THIS render state; always overwritten, so nothing leaks between frames. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void intro$extract(Avatar entity, AvatarRenderState state, float partial, CallbackInfo ci) {
        IntroClientController c = IntroClientController.get();
        FabricRenderState fs = (FabricRenderState) state;
        if (!c.active()) { fs.setData(PoseKeys.POSE, PoseKeys.NONE); return; }
        PlayerPose pose = c.poseCopyFor(entity.getUUID());
        boolean sil = c.isSilhouette(entity.getUUID());
        if (sil) SilhouetteRender.apply(state);
        fs.setData(PoseKeys.POSE, pose == null && !sil ? PoseKeys.NONE : new PoseKeys.Holder(pose, sil));
    }

    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void intro$rotate(AvatarRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
        PoseKeys.Holder h = ((FabricRenderState) state).getData(PoseKeys.POSE);
        if (h != null && h.pose() != null && h.pose().bodyYaw != 0f) poseStack.rotateDegrees(Axis.YP, h.pose().bodyYaw);
    }
}
