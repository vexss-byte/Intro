package dev.introcinema.client.shader;

import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Own GLSL manager. Programs are compiled once (preload), cached with their uniform locations and released
 * explicitly. Any compile/link error disables the GLSL path (the CPU-drawn effects keep working) instead of crashing.
 */
public final class ShaderManager {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/shader");
    private static final ShaderManager INSTANCE = new ShaderManager();
    public static ShaderManager get() { return INSTANCE; }

    public static final String[] NAMES = {"intro_cosmic", "intro_energy", "intro_portal", "intro_glow",
            "intro_impact", "intro_transition", "intro_bloom", "intro_final"};
    private static final String VERT = """
            #version 330 core
            out vec2 vUv;
            void main() {
                vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
                vUv = p;
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    public static final class Program {
        public final int id;
        private final Map<String, Integer> uniforms = new HashMap<>();
        Program(int id) { this.id = id; }
        public void use() { GL20.glUseProgram(id); }
        private int loc(String n) { return uniforms.computeIfAbsent(n, k -> GL20.glGetUniformLocation(id, k)); }
        public void f(String n, float v) { int l = loc(n); if (l >= 0) GL20.glUniform1f(l, v); }
        public void i(String n, int v) { int l = loc(n); if (l >= 0) GL20.glUniform1i(l, v); }
        public void v2(String n, float x, float y) { int l = loc(n); if (l >= 0) GL20.glUniform2f(l, x, y); }
        public void rgb(String n, int rgb) {
            int l = loc(n);
            if (l >= 0) GL20.glUniform3f(l, ((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
        }
    }

    private final Map<String, Program> programs = new HashMap<>();
    private boolean ready, failed;
    private String lastError = "";

    private ShaderManager() {}

    public synchronized boolean ready() { return ready; }
    public synchronized String lastError() { return lastError; }
    public Program program(String name) { return programs.get(name); }

    /** Compiles everything once. Safe to call every session start. @return true when GLSL effects can be used. */
    public synchronized boolean ensureReady() {
        if (ready) return true;
        if (failed) return false;
        try {
            String common = read("common.glsl");
            for (String n : NAMES) {
                String body = read(n + ".fsh");
                int vs = compile(GL20.GL_VERTEX_SHADER, VERT, "vertex");
                int fs;
                try { fs = compile(GL20.GL_FRAGMENT_SHADER, "#version 330 core\n" + common + "\n" + body, n); }
                catch (RuntimeException e) { GL20.glDeleteShader(vs); throw e; }
                int prog = GL20.glCreateProgram();
                GL20.glAttachShader(prog, vs); GL20.glAttachShader(prog, fs);
                GL20.glLinkProgram(prog);
                GL20.glDeleteShader(vs); GL20.glDeleteShader(fs);
                if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == 0) {
                    String log = GL20.glGetProgramInfoLog(prog);
                    GL20.glDeleteProgram(prog);
                    throw new IllegalStateException("link " + n + ": " + log);
                }
                programs.put(n, new Program(prog));
            }
            ready = true;
            LOG.info("Intro shaders ready ({} programs)", programs.size());
        } catch (Throwable t) {
            lastError = t.toString();
            LOG.error("Shader init failed, falling back to CPU effects: {}", lastError);
            release();
            failed = true;
        }
        return ready;
    }

    /** Called when the GL path misbehaves at runtime. */
    public synchronized void markFailed(String why) {
        lastError = why; failed = true;
        LOG.error("GLSL path disabled: {}", why);
        release();
    }

    /** Frees GL programs. */
    public synchronized void release() {
        for (Program p : programs.values()) { try { GL20.glDeleteProgram(p.id); } catch (Throwable ignored) {} }
        programs.clear();
        ready = false;
    }

    /** Re-reads and recompiles from resources (preload/reload support). */
    public synchronized boolean reload() { release(); failed = false; lastError = ""; return ensureReady(); }

    public void releaseFrameResources() { PostProcessor.get().releaseTargets(); }

    private static int compile(int type, String src, String name) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(s);
            GL20.glDeleteShader(s);
            throw new IllegalStateException("compile " + name + ": " + log);
        }
        return s;
    }

    private static String read(String file) throws IOException {
        try (InputStream in = ShaderManager.class.getResourceAsStream("/assets/intro_member/shaders/" + file)) {
            if (in == null) throw new IOException("missing shader resource " + file);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
