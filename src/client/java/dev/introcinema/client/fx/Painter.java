package dev.introcinema.client.fx;

/** Minimal 2D drawing surface so FX logic stays independent of Minecraft's GUI classes. */
public interface Painter {
    int width();
    int height();
    void fill(int x1, int y1, int x2, int y2, int argb);
    void gradient(int x1, int y1, int x2, int y2, int argbTop, int argbBottom);
}
