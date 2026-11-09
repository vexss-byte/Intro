package dev.introcinema.client.session;

import dev.introcinema.common.network.IntroPayload;
import dev.introcinema.common.session.SessionPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Client copy of the session. Silhouette names are unknown until a Reveal arrives. */
public final class ClientSession {
    public static final class Member {
        public final UUID uuid; public final int number; public final boolean silhouette, last; public final String quote;
        public String name; public boolean revealed;
        Member(IntroPayload.Member m) {
            uuid = m.uuid(); number = m.number(); silhouette = m.silhouette(); last = m.last(); quote = m.quote();
            name = m.name(); revealed = !m.silhouette();
        }
        public String displayName() { return revealed ? name : "???"; }
        public String numberLabel() { return String.format("%02d", number); }
    }

    public final String id, title, subtitle, lastLabel, quality;
    public final SessionPlan plan;
    public final boolean cameraOn, animationOn, shaderOn, bloomOn, screenFxOn;
    public final float cameraSpeed;
    public final List<Member> members = new ArrayList<>();

    public ClientSession(IntroPayload.Start s) {
        id = s.sessionId(); title = s.title(); subtitle = s.subtitle(); lastLabel = s.lastLabel(); quality = s.quality();
        plan = new SessionPlan(s.members().size(), s.opening(), s.memberTicks(), s.finale(), s.returnTicks());
        cameraOn = s.cameraOn(); animationOn = s.animationOn(); shaderOn = s.shaderOn(); bloomOn = s.bloomOn();
        screenFxOn = s.screenFxOn(); cameraSpeed = s.cameraSpeed();
        for (IntroPayload.Member m : s.members()) members.add(new Member(m));
    }

    public int indexOf(UUID id) {
        for (int i = 0; i < members.size(); i++) if (members.get(i).uuid.equals(id)) return i;
        return -1;
    }

    public void reveal(UUID id, String name) {
        int i = indexOf(id);
        if (i >= 0) { members.get(i).name = name; members.get(i).revealed = true; }
    }

    public int qualityCap() { return switch (quality) { case "LOW" -> 220; case "HIGH" -> 1200; default -> 560; }; }
}
