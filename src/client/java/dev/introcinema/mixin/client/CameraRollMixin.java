package dev.introcinema.mixin.client;

import dev.introcinema.client.IntroClientController;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** OPTIONAL config: camera roll. Needs the private rotation quaternion; if it is named differently this mixin is skipped. */
@Mixin(Camera.class)
public abstract class CameraRollMixin {
    @Shadow private Quaternionf rotation;

    @Inject(method = "setRotation(FF)V", at = @At("TAIL"))
    private void intro$roll(float yRot, float xRot, CallbackInfo ci) {
        var c = IntroClientController.get();
        if (c.camera.active() && c.camera.current().roll() != 0f)
            rotation.rotateZ((float) Math.toRadians(c.camera.current().roll()));
    }
}
