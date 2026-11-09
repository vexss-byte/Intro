package dev.introcinema.common.session;

import dev.introcinema.common.config.IntroConfig;
import dev.introcinema.common.dimension.ArenaBuilder;
import dev.introcinema.common.dimension.ArenaLayout;
import dev.introcinema.common.dimension.IntroDimension;
import dev.introcinema.common.network.IntroNetworkManager;
import dev.introcinema.common.network.IntroPayload;
import dev.introcinema.common.roster.RosterEntry;
import dev.introcinema.common.roster.RosterManager;
import dev.introcinema.common.teleport.TeleportService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Server-authoritative cinematic session. The server owns roster, clock, placements and restore.
 * Clients only render. A session ends with every participant verified restored or snapshots kept for retry.
 */
public final class IntroSessionManager {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/session");
    private static final int SYNC_EVERY = 5;
    private static final int ENFORCE_EVERY = 4;
    private static final int PLACE_LEAD = 6;       // move the featured member this many ticks before their slot starts
    private static final int MAX_RESTORE_TRIES = 10;

    private final RosterManager roster;
    private final SnapshotStore store;
    private IntroConfig config;

    private MinecraftServer server;
    private ArenaBuilder builder;
    private IntroSessionState state = IntroSessionState.IDLE;
    private String sessionId = "";
    private List<RosterEntry> entries = List.of();
    private SessionPlan plan;
    private int tick;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Map<UUID, ArenaLayout.Slot> assigned = new HashMap<>();
    private final Set<UUID> revealed = new HashSet<>();
    private int featured = -2;           // -2 none placed yet, -1 finale/audience-only, >=0 member index
    private boolean finalePlaced;
    private final Map<UUID, Integer> restoreTries = new HashMap<>();
    private final ArrayDeque<UUID> pendingRecovery = new ArrayDeque<>();
    private int lastEndServerTick = Integer.MIN_VALUE / 2;
    private int autoStartTimer = -1;
    private boolean startupAutoDone;

    public IntroSessionManager(RosterManager roster, SnapshotStore store, IntroConfig config) {
        this.roster = roster; this.store = store; this.config = config;
    }

    public void setConfig(IntroConfig c) { this.config = c; }
    public IntroSessionState state() { return state; }
    public boolean running() { return state != IntroSessionState.IDLE; }
    public boolean isParticipant(UUID id) { return running() && participants.contains(id); }

    // ---- lifecycle events ------------------------------------------------------------------

    public void onServerStarted(MinecraftServer s) {
        this.server = s;
        ServerLevel arena = IntroDimension.get(s);
        if (arena == null) LOG.error("Dimension {} is not registered. Check the data pack files; /intro start will refuse to run.", IntroDimension.ID);
        else builder = new ArenaBuilder(arena);
        List<PlayerSnapshot> pending = store.pending();
        if (!pending.isEmpty()) LOG.warn("{} player(s) have an unrestored intro snapshot (crash recovery on join)", pending.size());
    }

    public void onServerStopping() {
        // keep snapshots as they are: any session interrupted by shutdown is recovered on the next join
        if (running()) LOG.warn("Server stopping during an intro session; snapshots stay for recovery");
    }

    public void onJoin(ServerPlayer p) {
        roster.onJoin(p.getUUID(), p.getGameProfile().name());
        if (store.load(p.getUUID().toString()).filter(s -> !s.restored()).isPresent()) pendingRecovery.add(p.getUUID());
    }

    public void onDisconnect(ServerPlayer p) {
        roster.onDisconnect(p.getUUID());
        // snapshot is intentionally kept; participants entry stays so the slot is reserved for the session.
    }

    // ---- commands --------------------------------------------------------------------------

    public String start() {
        if (server == null) return "Server not ready.";
        if (running()) return "A session is already running (" + state + ").";
        if (builder == null) return "Dimension " + IntroDimension.ID + " is not registered; see the server log.";
        RosterManager.Frozen frozen = roster.freeze(config);
        if (frozen.entries().isEmpty()) return "No players online.";
        frozen.skippedSilhouettes().forEach(s -> LOG.warn("Silhouette skipped: {}", s));
        entries = frozen.entries();
        plan = SessionPlan.of(config, entries.size());
        sessionId = UUID.randomUUID().toString().substring(0, 8);
        participants.clear(); assigned.clear(); revealed.clear(); restoreTries.clear();
        featured = -2; finalePlaced = false; tick = 0;

        // snapshot everyone BEFORE anything moves; refuse to start if any snapshot cannot be saved
        for (RosterEntry e : entries) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.uuid());
            if (p == null || !p.isAlive()) continue;
            if (!store.save(TeleportService.snapshot(p, sessionId))) {
                participants.forEach(u -> store.delete(u.toString()));
                participants.clear();
                return "Could not save a recovery snapshot for " + p.getGameProfile().name() + "; nothing was moved.";
            }
            participants.add(e.uuid());
        }
        if (participants.isEmpty()) return "No living players to introduce.";
        builder.begin();
        state = IntroSessionState.PREPARING;
        broadcast(Component.literal("[Intro] Preparing the arena..."));
        return "Session " + sessionId + " preparing for " + participants.size() + " player(s)"
                + (frozen.skippedSilhouettes().isEmpty() ? "." : " (skipped silhouettes: " + frozen.skippedSilhouettes() + ").");
    }

    public String stop() {
        if (!running()) return "No session running.";
        beginRestore("stopped");
        return "Stopping; restoring " + participants.size() + " player(s).";
    }

    public String skip() {
        if (state != IntroSessionState.RUNNING) return "Nothing to skip.";
        int idx = plan.memberIndexAt(tick);
        int target;
        switch (plan.sceneAt(tick)) {
            case OPENING -> target = plan.memberStart(0);
            case MEMBER_REVEAL -> target = idx + 1 < plan.members() ? plan.memberStart(idx + 1) : plan.finaleStart();
            case FINALE -> target = plan.returnStart();
            default -> { return "Already ending."; }
        }
        tick = target;
        syncAll();
        return "Skipped to tick " + tick + ".";
    }

    public String status() {
        if (!running()) return "Idle. Online: " + roster.onlineCount() + ", unrestored snapshots: " + store.pending().size();
        return "State=" + state + " session=" + sessionId + " tick=" + tick + "/" + (plan == null ? 0 : plan.total())
                + " participants=" + participants.size() + " scene=" + (plan == null ? "-" : plan.sceneAt(tick));
    }

    // ---- tick --------------------------------------------------------------------------------

    public void tick(MinecraftServer s) {
        this.server = s;
        processRecovery();
        switch (state) {
            case IDLE -> autoStartCheck();
            case PREPARING -> tickPreparing();
            case RUNNING -> tickRunning();
            case RESTORING -> tickRestoring();
            default -> {}
        }
    }

    private void tickPreparing() {
        if (builder.building()) { builder.tick(); return; }
        if (!builder.isReady()) { builder.begin(); return; }
        state = IntroSessionState.TELEPORTING;
        List<RosterEntry> reachable = new ArrayList<>();
        int i = 0;
        for (RosterEntry e : entries) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.uuid());
            if (p == null || !participants.contains(e.uuid())) continue;
            ArenaLayout.Slot slot = ArenaLayout.audience(i++, participants.size());
            if (TeleportService.toSlot(server, p, slot)) { assigned.put(e.uuid(), slot); reachable.add(e); }
            else LOG.warn("Teleport to arena failed for {}", p.getGameProfile().name());
        }
        if (reachable.isEmpty()) { LOG.error("No player reached the arena; aborting"); beginRestore("teleport_failed"); return; }
        IntroPayload.Start start = buildStart();
        for (UUID id : participants) IntroNetworkManager.send(server.getPlayerList().getPlayer(id), start);
        state = IntroSessionState.RUNNING;
        LOG.info("Session {} running: {} members, {} ticks", sessionId, entries.size(), plan.total());
    }

    private IntroPayload.Start buildStart() {
        List<IntroPayload.Member> ms = new ArrayList<>();
        for (RosterEntry e : entries)
            ms.add(new IntroPayload.Member(e.uuid(), e.number(), e.silhouette() ? "" : e.name(), e.silhouette(), e.quote(), e.last()));
        return new IntroPayload.Start(sessionId, 0, config.serverTitle, config.subtitle, config.lastMemberLabel,
                plan.opening(), plan.memberTicks(), plan.finale(), plan.returnTicks(), config.particleQuality,
                config.cameraEnabled, config.animationEnabled, config.shaderEnabled, config.bloomEnabled,
                config.screenEffectsEnabled, config.cameraSpeed, ms);
    }

    private void tickRunning() {
        tick++;
        // 1) formation / stage placement derived from the clock (idempotent, so skip/catch-up just works)
        SessionPlan.Scene scene = plan.sceneAt(tick);
        boolean finaleNow = scene == SessionPlan.Scene.FINALE || scene == SessionPlan.Scene.RETURN;
        int want = (finaleNow || tick + PLACE_LEAD < plan.opening()) ? -1 : plan.memberIndexAtClamped(tick + PLACE_LEAD);
        applyPlacement(want, finaleNow);
        if (tick % ENFORCE_EVERY == 0) enforceSlots();

        // 2) reveals for silhouettes (name leaves the server only now)
        for (int i = 0; i < entries.size(); i++) {
            RosterEntry e = entries.get(i);
            if (!e.silhouette() || revealed.contains(e.uuid())) continue;
            if (tick >= plan.memberStart(i) + (int) (plan.memberTicks() * 0.4f)) {
                revealed.add(e.uuid());
                IntroPayload.Reveal r = new IntroPayload.Reveal(sessionId, e.uuid(), e.name());
                for (UUID id : participants) IntroNetworkManager.send(server.getPlayerList().getPlayer(id), r);
            }
        }

        // 3) heartbeat + end
        if (tick % SYNC_EVERY == 0) syncAll();
        if (tick >= plan.total()) beginRestore("finished");
    }

    private void applyPlacement(int wantFeatured, boolean finale) {
        if (finale) {
            if (finalePlaced) return;
            int i = 0;
            for (RosterEntry e : entries) {
                if (!participants.contains(e.uuid())) continue;
                ServerPlayer p = server.getPlayerList().getPlayer(e.uuid());
                ArenaLayout.Slot s = ArenaLayout.finale(i++, participants.size());
                assigned.put(e.uuid(), s);
                if (p != null) TeleportService.toSlot(server, p, s);
            }
            finalePlaced = true; featured = -1;
            return;
        }
        if (wantFeatured == featured) return;
        int audienceIdx = 0;
        // return previous featured to their audience slot, move new featured onto the stage
        List<RosterEntry> present = new ArrayList<>();
        for (RosterEntry e : entries) if (participants.contains(e.uuid())) present.add(e);
        for (int idx = 0; idx < present.size(); idx++) {
            RosterEntry e = present.get(idx);
            boolean isNewFeatured = wantFeatured >= 0 && wantFeatured < entries.size() && entries.get(wantFeatured).uuid().equals(e.uuid());
            ArenaLayout.Slot s = isNewFeatured ? ArenaLayout.stage() : ArenaLayout.audience(audienceIdx, present.size());
            audienceIdx++;
            ArenaLayout.Slot old = assigned.get(e.uuid());
            if (old == null || !old.equals(s)) {
                assigned.put(e.uuid(), s);
                ServerPlayer p = server.getPlayerList().getPlayer(e.uuid());
                if (p != null) TeleportService.toSlot(server, p, s);
            }
        }
        featured = wantFeatured;
    }

    /** Players cannot walk or turn away from their slot during the show; the server pulls drift back. */
    private void enforceSlots() {
        ServerLevel arena = IntroDimension.get(server);
        for (UUID id : participants) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            ArenaLayout.Slot s = assigned.get(id);
            if (p == null || s == null || arena == null) continue;
            boolean wrongLevel = !p.level().dimension().equals(IntroDimension.LEVEL_KEY);
            double dx = p.getX() - s.x(), dz = p.getZ() - s.z();
            float dyaw = Math.abs(((p.getYRot() - s.yaw()) % 360f + 540f) % 360f - 180f);
            if (wrongLevel || dx * dx + dz * dz > 0.36 || Math.abs(p.getY() - s.y()) > 1.0 || dyaw > 8f)
                TeleportService.toSlot(server, p, s);
        }
    }

    private void syncAll() {
        IntroPayload.Sync sync = new IntroPayload.Sync(sessionId, tick);
        for (UUID id : participants) IntroNetworkManager.send(server.getPlayerList().getPlayer(id), sync);
    }

    // ---- restore -----------------------------------------------------------------------------

    private void beginRestore(String reason) {
        IntroPayload.End end = new IntroPayload.End(sessionId, reason);
        for (UUID id : participants) IntroNetworkManager.send(server.getPlayerList().getPlayer(id), end);
        state = IntroSessionState.RESTORING;
    }

    private void tickRestoring() {
        Iterator<UUID> it = participants.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Optional<PlayerSnapshot> snap = store.load(id.toString());
            if (snap.isEmpty() || snap.get().restored()) { it.remove(); continue; }
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) { it.remove(); continue; } // offline: snapshot stays, restored on next join
            if (TeleportService.restore(server, p, snap.get())) {
                store.delete(id.toString()); it.remove();
            } else {
                int n = restoreTries.merge(id, 1, Integer::sum);
                if (n >= MAX_RESTORE_TRIES) {
                    LOG.error("Restore of {} failed {} times; snapshot kept for the next join", p.getGameProfile().name(), n);
                    p.sendSystemMessage(Component.literal("[Intro] Could not restore your position automatically. Ask an admin."));
                    it.remove();
                }
            }
        }
        if (participants.isEmpty()) finish();
    }

    private void finish() {
        state = IntroSessionState.IDLE;
        lastEndServerTick = server.getTickCount();
        assigned.clear(); revealed.clear(); entries = List.of();
        LOG.info("Session {} ended", sessionId);
    }

    /** One recovery per tick keeps joins cheap; used for crash/restart/disconnect recovery. */
    private void processRecovery() {
        UUID id = pendingRecovery.poll();
        if (id == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p == null) return;
        if (isParticipant(id) && state == IntroSessionState.RUNNING) {
            // rejoined mid-session: put them back in the show instead of restoring
            ArenaLayout.Slot s = assigned.getOrDefault(id, ArenaLayout.audience(0, Math.max(1, participants.size())));
            TeleportService.toSlot(server, p, s);
            IntroNetworkManager.send(p, buildStart());
            for (RosterEntry e : entries)
                if (revealed.contains(e.uuid())) IntroNetworkManager.send(p, new IntroPayload.Reveal(sessionId, e.uuid(), e.name()));
            IntroNetworkManager.send(p, new IntroPayload.Sync(sessionId, tick));
            return;
        }
        store.load(id.toString()).filter(s -> !s.restored()).ifPresent(snap -> {
            if (TeleportService.restore(server, p, snap)) {
                store.delete(id.toString());
                p.sendSystemMessage(Component.literal("[Intro] You were returned to your previous location."));
            } else pendingRecovery.add(id);
        });
    }

    // ---- auto start / helpers ----------------------------------------------------------------

    private void autoStartCheck() {
        if (!config.autoStart || server == null) { autoStartTimer = -1; return; }
        if (config.autoStartOnServerStartup && startupAutoDone) return;
        boolean eligible = roster.onlineCount() >= config.minimumPlayers
                && server.getTickCount() - lastEndServerTick >= config.introCooldown;
        if (!eligible) { autoStartTimer = -1; return; }
        if (autoStartTimer < 0) autoStartTimer = config.autoStartDelay;
        if (autoStartTimer-- > 0) return;
        autoStartTimer = -1;
        startupAutoDone = true;
        LOG.info("Auto-start: {}", start());
    }

    private void broadcast(Component c) {
        for (UUID id : participants) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.sendSystemMessage(c);
        }
    }

    public void onClientStatus(ServerPlayer p, IntroPayload.ClientStatus st) {
        if (!st.ok()) LOG.warn("Client {} reports renderer problem in session {}: {} (continuing; restore is unaffected)",
                p.getGameProfile().name(), st.sessionId(), st.message());
    }
}
