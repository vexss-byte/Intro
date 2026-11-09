package dev.introcinema.client.render;

import dev.introcinema.client.IntroClientController;
import dev.introcinema.client.camera.CameraPose;
import dev.introcinema.client.fx.FxProjection;
import dev.introcinema.client.hud.MemberInfoRenderer;
import dev.introcinema.client.hud.NameplateRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** CinematicFXRenderer for the GUI layer: particles/beams -> names + card -> screen effects (fade is always last). */
public final class IntroHudElement implements HudElement {
    private final FxProjection projection = new FxProjection();

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        IntroClientController c = IntroClientController.get();
        if (!c.active() || c.session() == null) return;
        try {
            float now = c.now();
            GuiPainter painter = new GuiPainter(g);
            CameraPose p = c.camera.current();
            if (p != null) projection.set(p.x(), p.y(), p.z(), p.yaw(), p.pitch(), p.fov(), g.guiWidth(), g.guiHeight());
            else projection.invalidate();
            c.fx.render(painter, projection);
            NameplateRenderer.render(g, c.session(), c.hud, now);
            MemberInfoRenderer.render(g, c.session(), c.hud, now);
            c.screen.render(painter, now);
        } catch (Throwable t) {
            c.shutdown("hud error: " + t);
        }
    }
}
