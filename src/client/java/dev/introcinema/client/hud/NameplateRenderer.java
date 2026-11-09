package dev.introcinema.client.hud;

import dev.introcinema.IntroCinemaMod;
import dev.introcinema.client.session.ClientSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Big "[NN] Nickname" banner (gold number, gold->orange gradient nickname) plus title and caption overlays. */
public final class NameplateRenderer {
    private NameplateRenderer() {}

    public static void render(GuiGraphicsExtractor g, ClientSession s, HudState hud, float now) {
        Font font = Minecraft.getInstance().font;
        int w = g.guiWidth(), h = g.guiHeight();
        var cfg = IntroCinemaMod.config();
        int c1 = TextAnimator.parseHex(cfg.nameplateColor, 0xFFD24A), c2 = TextAnimator.parseHex(cfg.nameplateGradientEnd, 0xFF8A1F);
        int fade = cfg.nameplateAnimationTicks;

        if (hud.memberIndex >= 0 && hud.memberIndex < s.members.size()) {
            ClientSession.Member m = s.members.get(hud.memberIndex);
            float a = TextAnimator.alpha(now, hud.memberStart, hud.memberEnd, fade);
            if (a > 0.02f) {
                String num = "[" + m.numberLabel() + "] ";
                String name = m.displayName();
                float scale = NameplateLayout.fit(font, num + name, w, NameplateLayout.baseScale(h, cfg.nameplateScale));
                float totalW = font.width(num + name) * scale;
                float x = NameplateLayout.x(cfg.nameplatePosition, w, totalW);
                float y = NameplateLayout.y(cfg.nameplatePosition, h, font.lineHeight * scale);
                y -= TextAnimator.slide(now, hud.memberStart, fade, 14f);
                drawGradientLine(g, font, num, name, x, y, scale, c1, c2, a);
            }
        }

        if (hud.titleStart >= 0) {
            float a = TextAnimator.alpha(now, hud.titleStart, hud.titleEnd, 18);
            if (a > 0.02f) {
                float scale = NameplateLayout.fit(font, hud.titleText, w, NameplateLayout.baseScale(h, 1f) * 1.6f);
                float tw = font.width(hud.titleText) * scale;
                float y = h * 0.36f - TextAnimator.slide(now, hud.titleStart, 18, 10f);
                drawGradientLine(g, font, "", hud.titleText, (w - tw) / 2f, y, scale, c1, c2, a);
                if (!hud.subtitleText.isEmpty()) {
                    float ss = Math.max(1f, scale * 0.38f);
                    float sw = font.width(hud.subtitleText) * ss;
                    drawScaled(g, font, hud.subtitleText, (w - sw) / 2f, y + font.lineHeight * scale + 4f, ss, TextAnimator.argb(0xFFFFFF, a * 0.9f));
                }
            }
        }

        if (hud.captionStart >= 0) {
            float a = TextAnimator.alpha(now, hud.captionStart, hud.captionEnd, 10);
            if (a > 0.02f) {
                float scale = NameplateLayout.fit(font, hud.captionText, w, NameplateLayout.baseScale(h, 1f) * 0.9f);
                float tw = font.width(hud.captionText) * scale;
                drawScaled(g, font, hud.captionText, (w - tw) / 2f, h * 0.22f, scale, TextAnimator.argb(0xFFFFFF, a));
            }
        }
    }

    static void drawGradientLine(GuiGraphicsExtractor g, Font font, String prefix, String text, float x, float y, float scale, int c1, int c2, float a) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale);
        int px = 0;
        if (!prefix.isEmpty()) { g.text(font, prefix, 0, 0, TextAnimator.argb(c1, a), true); px = font.width(prefix); }
        int n = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            g.text(font, ch, px, 0, TextAnimator.argb(TextAnimator.lerpColor(c1, c2, i / (float) n), a), true);
            px += font.width(ch);
        }
        g.pose().popMatrix();
    }

    static void drawScaled(GuiGraphicsExtractor g, Font font, String text, float x, float y, float scale, int argb) {
        if ((argb >>> 24) < 4) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale);
        g.text(font, text, 0, 0, argb, true);
        g.pose().popMatrix();
    }
}
