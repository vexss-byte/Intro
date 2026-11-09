package dev.introcinema.client.hud;

import dev.introcinema.IntroCinemaMod;
import dev.introcinema.client.session.ClientSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small member card at the bottom-left ("#NN Name / SERVER / MEMBER NN OF N") and the quote line at the bottom centre. */
public final class MemberInfoRenderer {
    private MemberInfoRenderer() {}

    public static void render(GuiGraphicsExtractor g, ClientSession s, HudState hud, float now) {
        if (hud.memberIndex < 0 || hud.memberIndex >= s.members.size()) return;
        ClientSession.Member m = s.members.get(hud.memberIndex);
        if (m.silhouette && !m.revealed) return; // no card while the identity is hidden
        int fade = IntroCinemaMod.config().nameplateAnimationTicks;
        float a = TextAnimator.alpha(now, hud.memberStart + 4, hud.memberEnd, fade);
        if (a <= 0.02f) return;

        Font font = Minecraft.getInstance().font;
        int w = g.guiWidth(), h = g.guiHeight();
        int gold = TextAnimator.parseHex(IntroCinemaMod.config().nameplateColor, 0xFFD24A);
        String l1 = "#" + m.numberLabel() + " " + m.displayName();
        String l2 = s.title;
        String l3 = m.last ? s.lastLabel : "MEMBER " + m.numberLabel() + " OF " + String.format("%02d", s.members.size());

        float sc = Math.max(0.9f, Math.min(2.2f, h / 270f * 1.1f));
        int textW = Math.max(font.width(l1), Math.max(font.width(l2) * 3 / 4, font.width(l3) * 3 / 4));
        int cw = textW + 16, ch = 10 + 8 + 7 + 7 + 10;
        float maxW = w * 0.45f;
        if (cw * sc > maxW) sc = maxW / cw;
        float x = 8f - TextAnimator.slide(now, hud.memberStart + 4, fade, 40f);
        float y = h - 8f - ch * sc - 18f * sc;

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(sc);
        g.fill(0, 0, cw, ch, TextAnimator.argb(0x14110A, a * 0.82f));
        g.outline(0, 0, cw, ch, TextAnimator.argb(gold, a));
        g.fill(1, 1, cw - 1, 2, TextAnimator.argb(gold, a * 0.6f));
        g.text(font, trim(font, l1, cw - 12), 8, 8, TextAnimator.argb(0xFFFFFF, a), true);
        g.pose().pushMatrix(); g.pose().translate(8, 8 + 10); g.pose().scale(0.75f);
        g.text(font, l2, 0, 0, TextAnimator.argb(gold, a), true);
        g.pose().translate(0, 9);
        g.text(font, l3, 0, 0, TextAnimator.argb(0xC8B98A, a), true);
        g.pose().popMatrix();
        g.pose().popMatrix();

        if (m.quote != null && !m.quote.isEmpty()) {
            float qa = TextAnimator.alpha(now, hud.memberStart + 14, hud.memberEnd - 6, 8);
            if (qa > 0.02f) {
                float qs = Math.max(0.9f, h / 270f);
                qs = Math.min(qs, w * 0.8f / Math.max(1, font.width(m.quote)));
                float qw = font.width(m.quote) * qs;
                NameplateRenderer.drawScaled(g, font, m.quote, (w - qw) / 2f, h - 14f * qs - 4f, qs, TextAnimator.argb(0xFFFFFF, qa));
            }
        }
    }

    private static String trim(Font font, String s, int maxPx) {
        while (s.length() > 1 && font.width(s) > maxPx) s = s.substring(0, s.length() - 1);
        return s;
    }
}
