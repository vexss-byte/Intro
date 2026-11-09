package dev.introcinema.client.cinematic;

import java.util.LinkedHashMap;
import java.util.Map;

/** All tracks of one cutscene plus its total length. One timeline = one clock for everything. */
public final class CinematicTimeline {
    private final Map<String, CinematicTrack> tracks = new LinkedHashMap<>();
    public final int duration;

    public CinematicTimeline(int duration) { this.duration = duration; }

    public CinematicTrack track(String name) { return tracks.computeIfAbsent(name, CinematicTrack::new); }
    Iterable<CinematicTrack> tracks() { return tracks.values(); }
}
