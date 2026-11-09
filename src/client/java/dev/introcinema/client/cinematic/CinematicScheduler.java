package dev.introcinema.client.cinematic;

import java.util.function.BiConsumer;

/**
 * Advances a timeline to a target tick. The target comes from the server clock, so a client that
 * lags or receives a late payload simply catches up: skipped events fire in order, exactly once.
 * Repeated or older targets are ignored.
 */
public final class CinematicScheduler {
    private final CinematicTimeline timeline;
    private final BiConsumer<CinematicEvent, Throwable> onError;
    private int lastTick = -1;
    private boolean finished;

    public CinematicScheduler(CinematicTimeline timeline, BiConsumer<CinematicEvent, Throwable> onError) {
        this.timeline = timeline; this.onError = onError;
    }

    /** @return true once the timeline end has been reached. */
    public boolean advanceTo(int tick) {
        if (finished) return true;
        if (tick > lastTick) {
            int t = Math.min(tick, timeline.duration);
            for (CinematicTrack tr : timeline.tracks()) tr.advance(t, onError);
            lastTick = tick;
        }
        if (tick >= timeline.duration) finished = true;
        return finished;
    }

    public int lastTick() { return lastTick; }
    public boolean finished() { return finished; }

    public void reset() { lastTick = -1; finished = false; for (CinematicTrack t : timeline.tracks()) t.rewind(); }
}
