package dev.introcinema.client.camera;

/** Holds the pose the Camera mixin applies. Cleared to hand control back to vanilla. */
public final class CameraController {
    private volatile CameraPose current;
    private volatile boolean active;

    public void set(CameraPose p) { current = p; active = p != null; }
    public void clear() { current = null; active = false; }
    public boolean active() { return active && current != null; }
    public CameraPose current() { return current; }
}
