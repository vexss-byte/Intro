package dev.introcinema.client.render;

import dev.introcinema.client.animation.PlayerPose;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

/** Carries the pose through the render pipeline on the render state, so it can never leak to another entity or frame. */
public final class PoseKeys {
    public record Holder(PlayerPose pose, boolean silhouette) {}
    public static final Holder NONE = new Holder(null, false);
    public static final RenderStateDataKey<Holder> POSE = RenderStateDataKey.create(() -> "intro_member:pose");
    private PoseKeys() {}
}
