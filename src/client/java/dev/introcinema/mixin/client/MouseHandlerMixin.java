package dev.introcinema.mixin.client;

import dev.introcinema.client.IntroClientController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The mouse cannot turn the player (and thus the model/slot) while the cinematic runs. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void intro$turn(LocalPlayer player, double yaw, double pitch) {
        if (!IntroClientController.get().active()) player.turn(yaw, pitch);
    }
}
