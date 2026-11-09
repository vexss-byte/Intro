package dev.introcinema.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Pulls the camera toward the focus when a block would sit between them, so it never ends up inside walls. */
public final class CameraCollision {
    private CameraCollision() {}

    public static CameraPose resolve(CameraPose pose, Vec3 focus) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || focus == null) return pose;
        Vec3 want = new Vec3(pose.x(), pose.y(), pose.z());
        BlockHitResult hit = mc.level.clip(new ClipContext(focus, want, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() == HitResult.Type.MISS) return pose;
        Vec3 dir = want.subtract(focus);
        double len = dir.length();
        if (len < 1e-4) return pose;
        Vec3 p = hit.getLocation().subtract(dir.scale(0.25 / len)); // 0.25 block margin
        return new CameraPose(p.x, p.y, p.z, pose.yaw(), pose.pitch(), pose.roll(), pose.fov());
    }
}
