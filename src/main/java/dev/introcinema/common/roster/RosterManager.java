package dev.introcinema.common.roster;

import dev.introcinema.common.config.IntroConfig;

import java.util.*;

/** Tracks join order of online players and produces a frozen, numbered roster for a session. */
public final class RosterManager {
    private final LinkedHashMap<UUID, String> online = new LinkedHashMap<>();

    public synchronized void onJoin(UUID id, String name) { online.putIfAbsent(id, name); }
    public synchronized void onDisconnect(UUID id) { online.remove(id); }
    public synchronized int onlineCount() { return online.size(); }

    /** Result of freezing: the roster plus configured silhouettes that could not be used. */
    public record Frozen(List<RosterEntry> entries, List<String> skippedSilhouettes) {}

    public synchronized Frozen freeze(IntroConfig cfg) {
        Set<UUID> wanted = new HashSet<>();
        List<String> skipped = new ArrayList<>();
        for (String s : cfg.silhouetteMembers) {
            try {
                UUID u = UUID.fromString(s);
                if (online.containsKey(u)) wanted.add(u); else skipped.add(s + " (offline)");
            } catch (IllegalArgumentException e) { skipped.add(s + " (not a UUID)"); }
        }
        List<RosterEntry> out = new ArrayList<>(online.size());
        int n = 0, total = online.size();
        for (Map.Entry<UUID, String> e : online.entrySet()) {
            n++;
            out.add(new RosterEntry(e.getKey(), n, e.getValue(), wanted.contains(e.getKey()),
                    cfg.memberQuotes.getOrDefault(e.getKey().toString(), ""), n == total));
        }
        return new Frozen(Collections.unmodifiableList(out), skipped);
    }
}
