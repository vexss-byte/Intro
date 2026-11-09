package dev.introcinema;

import dev.introcinema.common.command.IntroCommands;
import dev.introcinema.common.config.IntroConfig;
import dev.introcinema.common.network.IntroNetworkManager;
import dev.introcinema.common.roster.RosterManager;
import dev.introcinema.common.session.IntroSessionManager;
import dev.introcinema.common.session.SnapshotStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class IntroCinemaMod implements ModInitializer {
    public static final String MOD_ID = "intro_member";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("intro_member.json");

    private static volatile IntroConfig config = new IntroConfig();
    private static IntroSessionManager sessions;

    public static IntroConfig config() { return config; }
    public static void setConfig(IntroConfig c) { config = c; }

    @Override
    public void onInitialize() {
        config = IntroConfig.load(CONFIG_PATH);
        IntroNetworkManager.registerTypes();
        RosterManager roster = new RosterManager();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                IntroCommands.register(dispatcher, () -> sessions));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SnapshotStore store = new SnapshotStore(server.getWorldPath(LevelResource.ROOT).resolve("intro_member").resolve("snapshots"));
            sessions = new IntroSessionManager(roster, store, config);
            sessions.onServerStarted(server);
            IntroNetworkManager.registerServerReceivers(sessions::onClientStatus);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { if (sessions != null) sessions.onServerStopping(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (sessions != null) sessions.tick(server); });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> { if (sessions != null) sessions.onJoin(handler.getPlayer()); });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { if (sessions != null) sessions.onDisconnect(handler.getPlayer()); });

        // participants cannot be hurt while the show runs (health itself is never modified)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer p && sessions != null && sessions.isParticipant(p.getUUID())));

        LOGGER.info("Intro Member loaded (title='{}', autoStart={})", config.serverTitle, config.autoStart);
    }
}
