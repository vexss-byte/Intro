package dev.introcinema.client;

import dev.introcinema.common.network.IntroPayload;
import dev.introcinema.client.render.IntroHudElement;
import dev.introcinema.client.shader.PostProcessor;
import dev.introcinema.client.shader.ShaderManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class IntroCinemaClient implements ClientModInitializer {
    /** Vanilla HUD pieces hidden while the show runs (chat stays available). */
    private static final List<Identifier> HIDDEN = List.of(VanillaHudElements.CROSSHAIR, VanillaHudElements.HOTBAR,
            VanillaHudElements.ARMOR_BAR, VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR,
            VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
            VanillaHudElements.INFO_BAR_FOREGROUND, VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.MOB_EFFECTS,
            VanillaHudElements.BOSS_BAR, VanillaHudElements.SCOREBOARD, VanillaHudElements.PLAYER_LIST, VanillaHudElements.SUBTITLES);

    @Override
    public void onInitializeClient() {
        IntroClientController c = IntroClientController.get();

        ClientPlayNetworking.registerGlobalReceiver(IntroPayload.Start.TYPE, (p, ctx) -> c.onStart(p));
        ClientPlayNetworking.registerGlobalReceiver(IntroPayload.Sync.TYPE, (p, ctx) -> c.onSync(p));
        ClientPlayNetworking.registerGlobalReceiver(IntroPayload.Reveal.TYPE, (p, ctx) -> c.onReveal(p));
        ClientPlayNetworking.registerGlobalReceiver(IntroPayload.End.TYPE, (p, ctx) -> c.onEnd(p));

        ClientTickEvents.END_CLIENT_TICK.register(c::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> c.shutdown("disconnect"));
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ShaderManager.get().ensureReady()); // preload once, never per frame
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> { PostProcessor.get().releaseTargets(); ShaderManager.get().release(); });

        for (Identifier id : HIDDEN)
            HudElementRegistry.replaceElement(id, existing -> (g, dt) -> { if (!c.active()) existing.extractRenderState(g, dt); });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("intro_member", "overlay"), new IntroHudElement());

        LevelRenderEvents.END_MAIN.register(context -> PostProcessor.get().frame());
    }
}
