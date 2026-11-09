package dev.introcinema.client.hud;

import net.minecraft.client.gui.Font;

/** Layout maths: keeps long nicknames on screen at any resolution / GUI scale. */
public final class NameplateLayout {
    private NameplateLayout() {}

    public static float baseScale(int guiHeight, float userScale) { return Math.max(1.4f, guiHeight / 100f) * userScale; }

    /** Largest scale <= wanted such that textWidth*scale fits within 90% of the screen width. */
    public static float fit(Font font, String text, int guiWidth, float wanted) {
        int w = Math.max(1, font.width(text));
        return Math.min(wanted, guiWidth * 0.9f / w);
    }

    /** Left x (unscaled GUI units) for an element of the given pixel width, honouring TOP_LEFT / TOP_RIGHT / other = centre. */
    public static float x(String position, int guiWidth, float pixelWidth) {
        String p = position == null ? "" : position.toUpperCase();
        if (p.endsWith("LEFT")) return 8f;
        if (p.endsWith("RIGHT")) return guiWidth - pixelWidth - 8f;
        return (guiWidth - pixelWidth) / 2f;
    }

    public static float y(String position, int guiHeight, float pixelHeight) {
        String p = position == null ? "" : position.toUpperCase();
        if (p.startsWith("BOTTOM")) return guiHeight - pixelHeight - 8f;
        if (p.startsWith("CENTER")) return (guiHeight - pixelHeight) / 2f;
        return 6f;
    }
}
