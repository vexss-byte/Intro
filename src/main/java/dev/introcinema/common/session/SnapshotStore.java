package dev.introcinema.common.session;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** One JSON file per player, written atomically so a crash can never leave half a snapshot. */
public final class SnapshotStore {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/snapshots");
    private static final Gson GSON = new Gson();
    private final Path dir;

    public SnapshotStore(Path dir) {
        this.dir = dir;
        try { Files.createDirectories(dir); } catch (IOException e) { LOG.error("Cannot create {}: {}", dir, e.toString()); }
    }

    public boolean save(PlayerSnapshot s) {
        Path target = dir.resolve(s.uuid() + ".json");
        Path tmp = dir.resolve(s.uuid() + ".json.tmp");
        try {
            Files.writeString(tmp, GSON.toJson(s), StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            LOG.error("Snapshot save failed for {}: {}", s.uuid(), e.toString());
            return false;
        }
    }

    public Optional<PlayerSnapshot> load(String uuid) {
        Path f = dir.resolve(uuid + ".json");
        if (!Files.exists(f)) return Optional.empty();
        try (Reader r = Files.newBufferedReader(f)) {
            return Optional.ofNullable(GSON.fromJson(r, PlayerSnapshot.class));
        } catch (Exception e) {
            LOG.error("Snapshot {} unreadable: {}", f, e.toString());
            return Optional.empty();
        }
    }

    /** Snapshots whose player has not been restored yet (used for crash/restart recovery). */
    public List<PlayerSnapshot> pending() {
        List<PlayerSnapshot> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                String name = p.getFileName().toString();
                load(name.substring(0, name.length() - 5)).filter(s -> !s.restored()).ifPresent(out::add);
            });
        } catch (IOException e) { LOG.error("Cannot list {}: {}", dir, e.toString()); }
        return out;
    }

    /** Only call after the restore was verified. */
    public void delete(String uuid) {
        try { Files.deleteIfExists(dir.resolve(uuid + ".json")); } catch (IOException e) { LOG.warn("Delete failed {}: {}", uuid, e.toString()); }
    }
}
