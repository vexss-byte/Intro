package dev.introcinema.common.session;

import dev.introcinema.common.config.IntroConfig;

/**
 * Single source of truth for the cinematic layout in ticks. Server and every client compute the
 * same numbers from the same config values + roster size, so all players see the same scene.
 */
public record SessionPlan(int members, int opening, int memberTicks, int finale, int returnTicks) {
    public enum Scene { OPENING, MEMBER_REVEAL, FINALE, RETURN, DONE }

    public static SessionPlan of(IntroConfig c, int members) {
        return new SessionPlan(members, c.openingDuration, c.memberDuration, c.finaleDuration, c.returnDuration);
    }

    public int memberStart(int index) { return opening + index * memberTicks; }
    public int finaleStart() { return opening + members * memberTicks; }
    public int returnStart() { return finaleStart() + finale; }
    public int total() { return returnStart() + returnTicks; }

    public Scene sceneAt(int tick) {
        if (tick < opening) return Scene.OPENING;
        if (tick < finaleStart()) return Scene.MEMBER_REVEAL;
        if (tick < returnStart()) return Scene.FINALE;
        if (tick < total()) return Scene.RETURN;
        return Scene.DONE;
    }

    /** Member index for any tick, clamped into 0..members-1 (used for placement lead time). */
    public int memberIndexAtClamped(int tick) {
        if (tick < opening) return 0;
        return Math.max(0, Math.min(members - 1, (tick - opening) / memberTicks));
    }

    /** Zero-based member index at this tick, or -1 outside the reveal scene. */
    public int memberIndexAt(int tick) {
        if (sceneAt(tick) != Scene.MEMBER_REVEAL) return -1;
        return Math.min(members - 1, (tick - opening) / memberTicks);
    }
}
