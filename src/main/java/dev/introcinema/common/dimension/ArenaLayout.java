package dev.introcinema.common.dimension;

/**
 * Pure geometry of the built-in arena. Server (teleport targets) and client (camera anchors) both use these
 * numbers, so every player sees the same scene. Units are blocks. Floor top is FLOOR_Y; entities stand on FLOOR_Y + 1.
 *
 * Stage at the origin facing the camera on the -Z side. The audience stands behind the camera (arcs at -Z facing +Z).
 * The featured member is placed on the gold stage plate facing -Z (toward the camera).
 */
public final class ArenaLayout {
    public static final int FLOOR_Y = 64;
    public static final double STAGE_X = 0.5, STAGE_Z = 0.5;
    public static final double STAND_Y = FLOOR_Y + 1;
    public static final int ARENA_RADIUS = 56;

    /** Yaw that makes a player look toward -Z (towards the camera): Minecraft yaw 180 = facing -Z. */
    public static final float FACE_NEG_Z = 180f;
    /** Yaw that makes a player look toward +Z (towards the stage). */
    public static final float FACE_POS_Z = 0f;

    public record Slot(double x, double y, double z, float yaw, float pitch) {}

    private ArenaLayout() {}

    public static Slot stage() { return new Slot(STAGE_X, STAND_Y, STAGE_Z, FACE_NEG_Z, 0f); }

    /**
     * Audience formation: concentric arcs centred on the stage, behind the camera, facing the stage.
     * Capacity grows with ring count so there is no hard player limit.
     */
    public static Slot audience(int index, int total) {
        int ring = 0, start = 0, cap = ringCapacity(0);
        while (index >= start + cap) { start += cap; ring++; cap = ringCapacity(ring); }
        int posInRing = index - start;
        int inThisRing = Math.min(cap, total - start);
        double radius = 16 + ring * 3.0;
        double spread = Math.toRadians(150);               // arc width
        double t = inThisRing <= 1 ? 0.5 : (double) posInRing / (inThisRing - 1);
        double ang = -spread / 2 + spread * t;             // 0 = straight behind the camera (-Z)
        double x = STAGE_X + Math.sin(ang) * radius;
        double z = STAGE_Z - Math.cos(ang) * radius;
        float yaw = yawToward(x, z, STAGE_X, STAGE_Z);
        return new Slot(x, STAND_Y, z, yaw, 0f);
    }

    private static int ringCapacity(int ring) { return 10 + ring * 6; }

    /** Finale formation: staggered rows in front of the stage, everyone faces the camera at -Z. */
    public static Slot finale(int index, int total) {
        int perRow = 11;
        int row = index / perRow, col = index % perRow;
        int inRow = Math.min(perRow, total - row * perRow);
        double spacing = 1.9;
        double x = STAGE_X + (col - (inRow - 1) / 2.0) * spacing + ((row & 1) == 1 ? spacing / 2 : 0);
        double z = STAGE_Z + 2.0 + row * 2.2;
        return new Slot(x, STAND_Y, z, FACE_NEG_Z, 0f);
    }

    /** Minecraft yaw so that an entity at (x,z) looks at (tx,tz). Yaw 0 looks along +Z. */
    public static float yawToward(double x, double z, double tx, double tz) {
        return (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
    }

    /** Rows needed by the finale formation (used to place the wide camera far enough back). */
    public static int finaleRows(int total) { return Math.max(1, (total + 10) / 11); }
}
