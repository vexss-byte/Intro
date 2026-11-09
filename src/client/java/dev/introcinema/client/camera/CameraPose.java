package dev.introcinema.client.camera;

/** World-space camera. yaw/pitch/roll in degrees (Minecraft convention: yaw 0 = +Z). */
public record CameraPose(double x, double y, double z, float yaw, float pitch, float roll, float fov) {

    /** Pose at (x,y,z) looking at a target point. */
    public static CameraPose lookAt(double x, double y, double z, double tx, double ty, double tz, float roll, float fov) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
        return new CameraPose(x, y, z, yaw, pitch, roll, fov);
    }
}
