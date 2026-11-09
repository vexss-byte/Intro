package dev.introcinema.common.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Builds the dark-fantasy arena once, incrementally (a few chunks per tick), from code shipped in the mod.
 * No commands are used and the survival world is never touched: everything lives in the intro dimension.
 * {@link #isReady} checks real blocks, so a deleted or partially built arena is rebuilt automatically.
 */
public final class ArenaBuilder {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/arena");
    private static final int FLAGS = 2; // Block.UPDATE_CLIENTS
    private static final int CHUNKS_PER_TICK = 2;

    private final ServerLevel level;
    private final Deque<ChunkPos> queue = new ArrayDeque<>();
    private int total;
    private boolean started;

    public ArenaBuilder(ServerLevel level) { this.level = level; }

    private static BlockPos stageSentinel() { return new BlockPos(0, ArenaLayout.FLOOR_Y, 0); }
    private static BlockPos edgeSentinel() { return new BlockPos(ArenaLayout.ARENA_RADIUS - 6, ArenaLayout.FLOOR_Y, 0); }

    /** True when the stage and an edge floor block are present and their chunks are loaded. */
    public boolean isReady() {
        if (!queue.isEmpty()) return false;
        level.getChunk(0, 0, ChunkStatus.FULL, true);
        BlockPos e = edgeSentinel();
        level.getChunk(e.getX() >> 4, e.getZ() >> 4, ChunkStatus.FULL, true);
        return level.getBlockState(stageSentinel()).is(Blocks.GOLD_BLOCK)
                && !level.getBlockState(edgeSentinel()).isAir();
    }

    /** Plans the work. Safe to call again; does nothing if already ready or already building. */
    public void begin() {
        if (started && !queue.isEmpty()) return;
        if (isReady()) return;
        int cr = (ArenaLayout.ARENA_RADIUS >> 4) + 1;
        queue.clear();
        for (int cx = -cr; cx <= cr; cx++) for (int cz = -cr; cz <= cr; cz++) queue.add(new ChunkPos(cx, cz));
        total = queue.size();
        started = true;
        LOG.info("Building intro arena: {} chunks", total);
    }

    public boolean building() { return !queue.isEmpty(); }
    public float progress() { return total == 0 ? 1f : 1f - (float) queue.size() / total; }

    /** Call once per server tick while {@link #building()}. */
    public void tick() {
        for (int i = 0; i < CHUNKS_PER_TICK && !queue.isEmpty(); i++) buildChunk(queue.poll());
        if (queue.isEmpty() && started) { started = false; LOG.info("Intro arena build finished"); }
    }

    // ---------------------------------------------------------------------------------------------

    private void buildChunk(ChunkPos cp) {
        level.getChunk(cp.x(), cp.z(), ChunkStatus.FULL, true);
        int minX = cp.getMinBlockX(), minZ = cp.getMinBlockZ();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double d = Math.sqrt((double) x * x + (double) z * z);
                if (d > ArenaLayout.ARENA_RADIUS) continue;
                // clear 40 blocks of headroom so rebuilding always starts from a clean column
                for (int y = ArenaLayout.FLOOR_Y + 1; y < ArenaLayout.FLOOR_Y + 40; y++) {
                    p.set(x, y, z);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
                }
                p.set(x, ArenaLayout.FLOOR_Y, z);
                level.setBlock(p, floorBlock(x, z, d), FLAGS);
                for (int y = ArenaLayout.FLOOR_Y - 3; y < ArenaLayout.FLOOR_Y; y++) {
                    p.set(x, y, z);
                    level.setBlock(p, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), FLAGS);
                }
            }
        }
        decorate(cp);
    }

    private BlockState floorBlock(int x, int z, double d) {
        // gold stage plate 5x5 at the origin
        if (Math.abs(x) <= 2 && Math.abs(z) <= 2) return Blocks.GOLD_BLOCK.defaultBlockState();
        if (Math.abs(x) <= 3 && Math.abs(z) <= 3) return Blocks.SHROOMLIGHT.defaultBlockState();
        // glowing runway strips pointing at the stage from the audience side (video: light-blue strips)
        if ((Math.abs(x) % 8 == 0) && z < 0 && z > -40 && ((z & 3) == 0)) return Blocks.SEA_LANTERN.defaultBlockState();
        if (d > ArenaLayout.ARENA_RADIUS - 3) return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        if (((x + z) & 1) == 0) return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        return Blocks.DEEPSLATE_TILES.defaultBlockState();
    }

    private void decorate(ChunkPos cp) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // lantern pillars on a ring
        int ringR = ArenaLayout.ARENA_RADIUS - 8;
        for (int k = 0; k < 24; k++) {
            double a = Math.PI * 2 * k / 24;
            int px = (int) Math.round(Math.cos(a) * ringR), pz = (int) Math.round(Math.sin(a) * ringR);
            if ((px >> 4) != cp.x() || (pz >> 4) != cp.z()) continue;
            for (int y = 1; y <= 9; y++) set(p, px, ArenaLayout.FLOOR_Y + y, pz, y % 3 == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(p, px, ArenaLayout.FLOOR_Y + 10, pz, Blocks.GOLD_BLOCK);
            set(p, px, ArenaLayout.FLOOR_Y + 11, pz, Blocks.LANTERN);
        }
        // stage gate arch behind the stage (+Z side), like the frame in the reference video
        int gz = 14;
        for (int x = -9; x <= 9; x++) for (int y = 1; y <= 14; y++) {
            boolean frame = Math.abs(x) >= 8 || y >= 13;
            if (!frame) continue;
            if ((x >> 4) != cp.x() || (gz >> 4) != cp.z()) continue;
            Block b = (y == 14 || Math.abs(x) == 9) ? Blocks.GILDED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS;
            set(p, x, ArenaLayout.FLOOR_Y + y, gz, b);
        }
        for (int x : new int[]{-8, 8}) for (int y = 3; y <= 11; y += 4) {
            if ((x >> 4) == cp.x() && ((gz - 1) >> 4) == cp.z()) set(p, x, ArenaLayout.FLOOR_Y + y, gz - 1, Blocks.SEA_LANTERN);
        }
    }

    private void set(BlockPos.MutableBlockPos p, int x, int y, int z, Block b) {
        p.set(x, y, z);
        level.setBlock(p, b.defaultBlockState(), FLAGS);
    }
}
