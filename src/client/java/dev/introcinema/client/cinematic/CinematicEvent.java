package dev.introcinema.client.cinematic;

/** Something that happens once at a tick on a track. */
public record CinematicEvent(int tick, String name, Runnable action) {}
