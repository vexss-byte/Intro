package dev.introcinema.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.introcinema.IntroCinemaMod;
import dev.introcinema.common.config.IntroConfig;
import dev.introcinema.common.session.IntroSessionManager;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.function.Supplier;

/** /intro start|stop|status|skip|reload|silhouette. Operators (gamemaster level) by default; permission node intro_member:command/admin. */
public final class IntroCommands {
    private static final Identifier NODE = Identifier.fromNamespaceAndPath("intro_member", "command/admin");

    private IntroCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d, Supplier<IntroSessionManager> mgr) {
        d.register(Commands.literal("intro")
                .requires(PermissionPredicates.require(NODE, PermissionLevel.GAMEMASTERS))
                .then(Commands.literal("start").executes(c -> reply(c, mgr.get().start())))
                .then(Commands.literal("stop").executes(c -> reply(c, mgr.get().stop())))
                .then(Commands.literal("status").executes(c -> reply(c, mgr.get().status())))
                .then(Commands.literal("skip").executes(c -> reply(c, mgr.get().skip())))
                .then(Commands.literal("reload").executes(c -> reply(c, reload(mgr.get()))))
                .then(Commands.literal("silhouette")
                        .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> reply(c, silhouette(EntityArgument.getPlayer(c, "player"), true)))))
                        .then(Commands.literal("remove").then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> reply(c, silhouette(EntityArgument.getPlayer(c, "player"), false)))))
                        .then(Commands.literal("list").executes(c -> reply(c, "Silhouettes: " + IntroCinemaMod.config().silhouetteMembers)))));
    }

    private static int reply(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal("[Intro] " + msg), false);
        return 1;
    }

    private static String reload(IntroSessionManager mgr) {
        IntroConfig cfg = IntroConfig.load(IntroCinemaMod.CONFIG_PATH);
        IntroCinemaMod.setConfig(cfg);
        mgr.setConfig(cfg); // a running session keeps the values it froze at start
        return "Config reloaded" + (mgr.running() ? " (applies to the next session)." : ".");
    }

    private static String silhouette(ServerPlayer p, boolean add) {
        IntroConfig cfg = IntroCinemaMod.config();
        String id = p.getUUID().toString();
        boolean changed = add ? (!cfg.silhouetteMembers.contains(id) && cfg.silhouetteMembers.add(id)) : cfg.silhouetteMembers.remove(id);
        if (changed) cfg.save(IntroCinemaMod.CONFIG_PATH);
        return (add ? "Added " : "Removed ") + p.getGameProfile().name() + (changed ? "" : " (no change)") + ". Silhouettes must be online when the session starts.";
    }
}
