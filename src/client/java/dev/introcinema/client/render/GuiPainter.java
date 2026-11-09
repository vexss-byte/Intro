package dev.introcinema.client.render;

import dev.introcinema.client.fx.Painter;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Adapts Minecraft's GUI drawing surface to the FX Painter interface. */
public final class GuiPainter implements Painter {
    private final GuiGraphicsExtractor g;
    public GuiPainter(GuiGraphicsExtractor g) { this.g = g; }
    @Override public int width() { return g.guiWidth(); }
    @Override public int height() { return g.guiHeight(); }
    @Override public void fill(int x1, int y1, int x2, int y2, int argb) { if (x2 > x1 && y2 > y1) g.fill(x1, y1, x2, y2, argb); }
    @Override public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) { if (x2 > x1 && y2 > y1) g.fillGradient(x1, y1, x2, y2, top, bottom); }
}
