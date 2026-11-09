package dev.introcinema.client.shader;

import dev.introcinema.client.IntroClientController;
import dev.introcinema.client.camera.CameraPose;
import dev.introcinema.client.cinematic.CinematicSequence;
import dev.introcinema.client.fx.FxParams;
import dev.introcinema.client.fx.FxProjection;
import dev.introcinema.client.session.ClientSession;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.backend.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional GLSL layer drawn at the end of the main world pass: nebula sky, energy, portal, glow, impact, motion
 * streaks, bloom, vignette and grading. Everything is additive or multiplicative on top of the finished scene,
 * so the scene stays sharp and a failing pass can never blank the screen. All GL state is saved and restored.
 * Repeated GL errors permanently switch this layer off.
 */
public final class PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/post");
    private static final PostProcessor INSTANCE = new PostProcessor();
    public static PostProcessor get() { return INSTANCE; }

    private final FxParams params = new FxParams();
    private final FxProjection projection = new FxProjection();
    private final float[] pt = new float[4];
    private int vao, bloomFbo, bloomTex, bloomW, bloomH, errors;

    private PostProcessor() {}

    /** Hook for LevelRenderEvents.END_MAIN. */
    public void frame() {
        IntroClientController c = IntroClientController.get();
        ShaderManager sm = ShaderManager.get();
        if (!c.active() || !sm.ready()) return;
        ClientSession s = c.session();
        if (s == null || !s.shaderOn) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
            float now = c.now();
            fillParams(c, s, now, w, h);
            GL11.glGetError(); // drain errors that are not ours
            draw(sm, mc, now, w, h);
            int err = GL11.glGetError();
            if (err != 0 && ++errors >= 3) sm.markFailed("repeated GL error 0x" + Integer.toHexString(err));
        } catch (Throwable t) {
            sm.markFailed(t.toString());
        }
    }

    private void fillParams(IntroClientController c, ClientSession s, float now, int w, int h) {
        params.reset();
        c.fx.fill(params);
        c.screen.fill(params, now);
        params.cosmic = 0.55f;
        params.bloom = s.bloomOn ? 0.45f : 0f;
        params.grade = 1f;
        int idx = c.hud.memberIndex;
        int col = idx >= 0 && idx < s.members.size() ? CinematicSequence.color(s.members.get(idx).number) : 0xFFC83C;
        params.colorA = col; params.colorB = 0xFF7A1A;
        CameraPose p = c.camera.current();
        params.centerX = 0.5f; params.centerY = 0.5f;
        if (p != null) {
            projection.set(p.x(), p.y(), p.z(), p.yaw(), p.pitch(), p.fov(), w, h);
            if (projection.project(0.5, 66.3, 0.5, pt)) { params.centerX = pt[0] / w; params.centerY = pt[1] / h; }
        }
    }

    private void draw(ShaderManager sm, Minecraft mc, float now, int w, int h) {
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int[] vp = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
        try {
            if (vao == 0) vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);
            GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE); GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDepthMask(false);
            GL11.glViewport(0, 0, w, h);
            GL11.glEnable(GL11.GL_BLEND);

            GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE);   // screen blend: nebula only shows on dark pixels
            pass(sm, "intro_cosmic", now, w, h, params.cosmic);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);                     // additive
            pass(sm, "intro_energy", now, w, h, params.energy);
            pass(sm, "intro_portal", now, w, h, params.portal);
            pass(sm, "intro_glow", now, w, h, params.glow);
            pass(sm, "intro_impact", now, w, h, params.impact);
            pass(sm, "intro_transition", now, w, h, params.transition);

            if (params.bloom > 0.01f) bloom(sm, mc, now, w, h, prevDraw);

            GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_COLOR);              // multiply: vignette + grading
            ShaderManager.Program fin = sm.program("intro_final");
            if (fin != null) {
                fin.use(); common(fin, now, w, h, 1f);
                fin.f("uVignette", params.vignette); fin.f("uGrade", params.grade);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            }
        } finally {
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcA, dstA);
            GL11.glViewport(vp[0], vp[1], vp[2], vp[3]);
            GL11.glDepthMask(depthMask);
            if (blend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (cull) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
            GL13.glActiveTexture(prevActive);
            GL30.glBindVertexArray(prevVao);
            GL20.glUseProgram(prevProgram);
        }
    }

    private void pass(ShaderManager sm, String name, float now, int w, int h, float intensity) {
        if (intensity < 0.01f) return;
        ShaderManager.Program p = sm.program(name);
        if (p == null) return;
        p.use(); common(p, now, w, h, intensity);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    private void common(ShaderManager.Program p, float now, int w, int h, float intensity) {
        p.f("uTime", now / 20f); p.v2("uRes", w, h); p.f("uIntensity", intensity);
        p.rgb("uColorA", params.colorA); p.rgb("uColorB", params.colorB); p.v2("uCenter", params.centerX, params.centerY);
    }

    /** Half-resolution bright pass + blur into our own target, then added on top of the main target. */
    private void bloom(ShaderManager sm, Minecraft mc, float now, int w, int h, int mainFbo) {
        ShaderManager.Program p = sm.program("intro_bloom");
        if (p == null) return;
        int sceneTex;
        try {
            RenderTarget target = mc.gameRenderer.mainRenderTarget();
            sceneTex = ((GlTexture) target.getColorTexture()).glId();
        } catch (Throwable t) { return; } // backend without a GL texture: skip bloom, everything else still works
        ensureBloomTarget(Math.max(2, w / 2), Math.max(2, h / 2));
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, bloomFbo);
        GL11.glViewport(0, 0, bloomW, bloomH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneTex);
        p.use(); common(p, now, bloomW, bloomH, params.bloom); p.i("uScene", 0); p.i("uMode", 0);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mainFbo);
        GL11.glViewport(0, 0, w, h);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, bloomTex);
        common(p, now, w, h, params.bloom); p.i("uMode", 1);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    private void ensureBloomTarget(int w, int h) {
        if (bloomFbo != 0 && bloomW == w && bloomH == h) return;
        releaseTargets();
        bloomW = w; bloomH = h;
        bloomTex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, bloomTex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        bloomFbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, bloomFbo);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, bloomTex, 0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("bloom framebuffer incomplete");
    }

    /** Frees GL resources owned by this class (call when the session ends). */
    public void releaseTargets() {
        try {
            if (bloomFbo != 0) GL30.glDeleteFramebuffers(bloomFbo);
            if (bloomTex != 0) GL11.glDeleteTextures(bloomTex);
            if (vao != 0) GL30.glDeleteVertexArrays(vao);
        } catch (Throwable t) { LOG.warn("Release problem: {}", t.toString()); }
        bloomFbo = bloomTex = vao = bloomW = bloomH = 0;
    }
}
