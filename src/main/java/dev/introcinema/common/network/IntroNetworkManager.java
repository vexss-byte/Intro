package dev.introcinema.common.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/** Registers payload types (both sides) and offers null-safe server sends. */
public final class IntroNetworkManager {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/net");

    private IntroNetworkManager() {}

    public static void registerTypes() {
        PayloadTypeRegistry.clientboundPlay().register(IntroPayload.Start.TYPE, IntroPayload.Start.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(IntroPayload.Sync.TYPE, IntroPayload.Sync.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(IntroPayload.Reveal.TYPE, IntroPayload.Reveal.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(IntroPayload.End.TYPE, IntroPayload.End.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(IntroPayload.ClientStatus.TYPE, IntroPayload.ClientStatus.CODEC);
    }

    /** @param onStatus called on the server thread with the sending player and the status message. */
    public static void registerServerReceivers(BiConsumer<ServerPlayer, IntroPayload.ClientStatus> onStatus) {
        ServerPlayNetworking.registerGlobalReceiver(IntroPayload.ClientStatus.TYPE,
                (payload, context) -> onStatus.accept(context.player(), payload));
    }

    public static void send(ServerPlayer p, IntroPayload payload) {
        try {
            if (p != null && ServerPlayNetworking.canSend(p, payload.type())) ServerPlayNetworking.send(p, payload);
        } catch (Throwable t) {
            LOG.warn("Send {} to {} failed: {}", payload.type().id(), p == null ? "?" : p.getGameProfile().name(), t.toString());
        }
    }
}
