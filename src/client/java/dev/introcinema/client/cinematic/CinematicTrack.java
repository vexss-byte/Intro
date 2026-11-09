package dev.introcinema.client.cinematic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** One lane (camera, animation, particle, shader, nameplate, lighting, sound) of events. */
public final class CinematicTrack {
    public final String name;
    private final List<CinematicEvent> events = new ArrayList<>();
    private int cursor;

    public CinematicTrack(String name) { this.name = name; }

    public CinematicTrack at(int tick, String eventName, Runnable action) {
        events.add(new CinematicEvent(tick, eventName, action));
        events.sort(Comparator.comparingInt(CinematicEvent::tick)); // stable: same-tick events keep insertion order
        return this;
    }

    public int size() { return events.size(); }
    void rewind() { cursor = 0; }

    /** Fires every not-yet-fired event with tick <= upTo. A failing event is isolated and reported. */
    void advance(int upTo, java.util.function.BiConsumer<CinematicEvent, Throwable> onError) {
        while (cursor < events.size() && events.get(cursor).tick() <= upTo) {
            CinematicEvent e = events.get(cursor++);
            try { e.action().run(); } catch (Throwable t) { onError.accept(e, t); }
        }
    }
}
