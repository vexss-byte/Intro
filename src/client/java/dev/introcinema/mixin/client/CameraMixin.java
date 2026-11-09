package dev.introcinema.mixin.client;

import dev.introcinema.client.IntroClientController;
import dev.introcinema.client.camera.CameraPose;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the cinematic pose to the vanilla camera. Injection points mirror a mod known to run on 26.3. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void intro$apply(float partial, CallbackInfo ci) {
        CameraPose p = IntroClientController.get().cameraFrame(partial);
        if (p == null) return;
        setPosition(new Vec3(p.x(), p.y(), p.z()));
        setRotation(p.yaw(), p.pitch());
    }

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void intro$fov(float partial, CallbackInfoReturnable<Float> cir) {
        IntroClientController c = IntroClientController.get();
        if (c.camera.active()) cir.setReturnValue(c.camera.current().fov());
    }

    /** Detached = third-person style: the local player model is rendered and the first-person hand is skipped. */
    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void intro$detached(CallbackInfoReturnable<Boolean> cir) {
        if (IntroClientController.get().camera.active()) cir.setReturnValue(true);
    }
}
