package dev.introcinema.client.animation;

import dev.introcinema.client.session.ClientSession;

import java.util.UUID;

/** Decides which clip each real player plays at which moment; the engine does the sampling. */
public final class PlayerPoseController {
    private static final String[] FEATURE_CLIPS = {PoseLibrary.HERO, PoseLibrary.VICTORY, PoseLibrary.WEAPON};
    private final IntroAnimationEngine engine;

    public PlayerPoseController(IntroAnimationEngine engine) { this.engine = engine; }

    public void startAudience(ClientSession s, float now) {
        for (ClientSession.Member m : s.members) engine.play(m.uuid, PoseLibrary.CINEMATIC_IDLE, now, 6f);
    }

    public void feature(ClientSession s, int index, float now) {
        if (index < 0 || index >= s.members.size()) return;
        UUID id = s.members.get(index).uuid;
        engine.play(id, PoseLibrary.LOOK_AT_CAMERA, now, 4f);
        engine.play(id, FEATURE_CLIPS[index % FEATURE_CLIPS.length], now + 4f, 6f);
    }

    public void relax(ClientSession s, int index, float now) {
        if (index < 0 || index >= s.members.size()) return;
        engine.play(s.members.get(index).uuid, PoseLibrary.CINEMATIC_IDLE, now, 10f);
    }

    public void finale(ClientSession s, float now) {
        for (int i = 0; i < s.members.size(); i++) engine.play(s.members.get(i).uuid, PoseLibrary.VICTORY, now + Math.min(40, i * 2), 8f);
    }
}
