package dev.introcinema.client.fx;

/**
 * World -> screen projection for the current camera, in plain doubles (no JOML, no GL).
 * Minecraft convention: yaw 0 looks along +Z, pitch positive looks down, FOV is vertical.
 */
public final class FxProjection {
    private double cx, cy, cz;
    private double fx, fy, fz, rx, ry, rz, ux, uy, uz;
    private double focal;       // 1 / tan(fov/2)
    private int w = 1, h = 1;
    private boolean valid;

    public void set(double camX, double camY, double camZ, float yawDeg, float pitchDeg, float fovDeg, int width, int height) {
        cx = camX; cy = camY; cz = camZ; w = Math.max(1, width); h = Math.max(1, height);
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double cp = Math.cos(pitch);
        fx = -Math.sin(yaw) * cp; fy = -Math.sin(pitch); fz = Math.cos(yaw) * cp;
        rx = -Math.cos(yaw); ry = 0; rz = -Math.sin(yaw);
        ux = ry * fz - rz * fy; uy = rz * fx - rx * fz; uz = rx * fy - ry * fx; // right x forward
        focal = 1.0 / Math.tan(Math.toRadians(Math.max(1f, Math.min(170f, fovDeg))) / 2.0);
        valid = true;
    }

    public void invalidate() { valid = false; }
    public boolean valid() { return valid; }
    public int width() { return w; }
    public int height() { return h; }

    /** @param out out[0]=screen x, out[1]=screen y, out[2]=pixels per world unit at that depth, out[3]=depth. */
    public boolean project(double x, double y, double z, float[] out) {
        if (!valid) return false;
        double vx = x - cx, vy = y - cy, vz = z - cz;
        double depth = vx * fx + vy * fy + vz * fz;
        if (depth < 0.08) return false;
        double px = vx * rx + vy * ry + vz * rz, py = vx * ux + vy * uy + vz * uz;
        double aspect = (double) w / h;
        double ndcX = px * focal / (aspect * depth), ndcY = py * focal / depth;
        out[0] = (float) ((ndcX * 0.5 + 0.5) * w);
        out[1] = (float) ((0.5 - ndcY * 0.5) * h);
        out[2] = (float) (focal / depth * h * 0.5);
        out[3] = (float) depth;
        return true;
    }
}
