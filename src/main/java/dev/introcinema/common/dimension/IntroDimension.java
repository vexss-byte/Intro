package dev.introcinema.common.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Registry keys and lookup for intro_member:intro_arena. The dimension itself is data-driven (data/intro_member/...). */
public final class IntroDimension {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("intro_member", "intro_arena");
    public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, ID);

    private IntroDimension() {}

    /** @return the arena level, or null if the data pack did not register it (validation failure). */
    public static ServerLevel get(MinecraftServer server) { return server.getLevel(LEVEL_KEY); }

    public static ResourceKey<Level> keyFromString(String s) {
        int i = s.indexOf(':');
        Identifier id = i < 0 ? Identifier.fromNamespaceAndPath("minecraft", s)
                : Identifier.fromNamespaceAndPath(s.substring(0, i), s.substring(i + 1));
        return ResourceKey.create(Registries.DIMENSION, id);
    }
}
