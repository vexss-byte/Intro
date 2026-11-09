package dev.introcinema.common.session;

/** Server-side snapshot taken before a player is moved to the arena. Persisted as JSON. */
public record PlayerSnapshot(String uuid, String sessionId, String dimension,
                             double x, double y, double z, float yaw, float pitch,
                             double vx, double vy, double vz, boolean restored) {
    public PlayerSnapshot asRestored() {
        return new PlayerSnapshot(uuid, sessionId, dimension, x, y, z, yaw, pitch, vx, vy, vz, true);
    }
}
