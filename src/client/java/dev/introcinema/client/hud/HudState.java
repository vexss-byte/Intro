package dev.introcinema.client.hud;

/** What the HUD should show; set by timeline events, read by the renderers. Plain data, no GL. */
public final class HudState {
    public int memberIndex = -1, memberStart, memberEnd;
    public String titleText = "", subtitleText = ""; public int titleStart = -1, titleEnd;
    public String captionText = ""; public int captionStart = -1, captionEnd;

    public void showMember(int idx, int start, int end) { memberIndex = idx; memberStart = start; memberEnd = end; }
    public void hideMember() { memberIndex = -1; }
    public void title(String t, String sub, int start, int end) { titleText = t; subtitleText = sub; titleStart = start; titleEnd = end; }
    public void caption(String t, int start, int end) { captionText = t; captionStart = start; captionEnd = end; }
    public void reset() { memberIndex = -1; titleStart = -1; captionStart = -1; titleText = subtitleText = captionText = ""; }
}
