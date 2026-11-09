package dev.introcinema.common.teleport;

import dev.introcinema.common.dimension.ArenaLayout;
import dev.introcinema.common.dimension.IntroDimension;
import dev.introcinema.common.session.PlayerSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/** Moves players only: never touches inventory, equipment, health, XP or any other player data. */
public final class TeleportService {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/teleport");

    private TeleportService() {}

    public static PlayerSnapshot snapshot(ServerPlayer p, String sessionId) {
        Vec3 v = p.getDeltaMovement();
        return new PlayerSnapshot(p.getUUID().toString(), sessionId, p.level().dimension().identifier().toString(),
                p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(), v.x, v.y, v.z, false);
    }

    /** Moves a player to an arena slot (same-dimension moves are used for stage changes and formation re-sync). */
    public static boolean toSlot(MinecraftServer server, ServerPlayer p, ArenaLayout.Slot s) {
        ServerLevel arena = IntroDimension.get(server);
        if (arena == null) return false;
        if (p.isPassenger()) p.stopRiding();
        arena.getChunk((int) Math.floor(s.x()) >> 4, (int) Math.floor(s.z()) >> 4, ChunkStatus.FULL, true);
        boolean ok = p.teleportTo(arena, s.x(), s.y(), s.z(), Set.of(), s.yaw(), s.pitch(), true);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        return ok;
    }

    /**
     * Restores a player from a snapshot and verifies the result (dimension + distance).
     * @return true only when the player is verified to be back; the caller deletes the snapshot then.
     */
    public static boolean restore(MinecraftServer server, ServerPlayer p, PlayerSnapshot s) {
        ServerLevel target = server.getLevel(IntroDimension.keyFromString(s.dimension()));
        if (target == null || target.dimension().equals(IntroDimension.LEVEL_KEY)) {
            LOG.warn("Snapshot dimension '{}' unavailable for {}; using overworld", s.dimension(), p.getGameProfile().name());
            target = server.overworld();
        }
        if (p.isPassenger()) p.stopRiding();
        target.getChunk((int) Math.floor(s.x()) >> 4, (int) Math.floor(s.z()) >> 4, ChunkStatus.FULL, true);
        double y = safeY(target, s.x(), s.y(), s.z());
        boolean ok = p.teleportTo(target, s.x(), y, s.z(), Set.of(), s.yaw(), s.pitch(), true);
        p.setDeltaMovement(Vec3.ZERO); // safe movement state: no leftover fall speed from before the intro
        p.resetFallDistance();
        boolean verified = ok && p.level().dimension().equals(target.dimension())
                && p.distanceToSqr(s.x(), y, s.z()) < 4.0;
        if (!verified) LOG.error("Restore verification failed for {} (teleport ok={})", p.getGameProfile().name(), ok);
        return verified;
    }

    /** Finds a y where feet and head are free of collision, scanning up first then down. */
    static double safeY(ServerLevel level, double x, double y, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z), by = (int) Math.floor(y);
        for (int dy = 0; dy <= 24; dy++) if (free(level, bx, by + dy, bz)) return by + dy;
        for (int dy = 1; dy <= 8; dy++) if (free(level, bx, by - dy, bz)) return by - dy;
        LOG.warn("No free space found near {} {} {}; restoring to the saved coordinates", bx, by, bz);
        return y;
    }

    private static boolean free(ServerLevel level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z), head = feet.above();
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }
}
