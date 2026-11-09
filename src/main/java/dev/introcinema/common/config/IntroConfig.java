package dev.introcinema.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** config/intro_member.json. Invalid or broken files fall back to safe defaults and never crash the server. */
public final class IntroConfig {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String serverTitle = "LAPER HARDCORE";
    public String subtitle = "PERKENALAN MEMBER";
    public String lastMemberLabel = "THE LAST MEMBER";
    public String lastCaption = "DAN YANG TERAKHIR...";

    public boolean autoStart = false;
    public int minimumPlayers = 2;
    public int autoStartDelay = 600;      // ticks
    public int introCooldown = 72000;     // ticks
    public boolean autoStartOnServerStartup = false;

    public int openingDuration = 200;     // ticks
    public int memberDuration = 140;      // ticks per member
    public int finaleDuration = 260;      // ticks
    public int returnDuration = 40;       // ticks (fade-out / restore window)

    public boolean cameraEnabled = true;
    public boolean animationEnabled = true;
    public boolean shaderEnabled = true;
    public String particleQuality = "MEDIUM"; // LOW | MEDIUM | HIGH
    public boolean bloomEnabled = true;
    public boolean screenEffectsEnabled = true;
    public float cameraSpeed = 1.0f;

    public String nameplateColor = "#FFD24A";
    public String nameplateGradientEnd = "#FF8A1F";
    public String nameplatePosition = "TOP_CENTER";
    public float nameplateScale = 1.0f;
    public int nameplateAnimationTicks = 12;

    public boolean restorePlayersOnStop = true;

    /** UUID strings of online players shown as "[NN] ???" silhouettes before being revealed. */
    public List<String> silhouetteMembers = new ArrayList<>();
    /** UUID string -> short quote shown under the member card. */
    public Map<String, String> memberQuotes = new HashMap<>();

    public static IntroConfig load(Path file) {
        IntroConfig cfg = new IntroConfig();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                IntroConfig parsed = GSON.fromJson(r, IntroConfig.class);
                if (parsed != null) cfg = parsed;
            } catch (Exception e) {
                LOG.error("Config {} is invalid ({}). Using safe defaults; the file was left untouched.", file, e.toString());
                cfg = new IntroConfig();
            }
        } else {
            cfg.save(file);
        }
        cfg.sanitize();
        return cfg;
    }

    public void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) { GSON.toJson(this, w); }
        } catch (IOException e) {
            LOG.error("Could not write default config {}: {}", file, e.toString());
        }
    }

    /** Clamp every value into a range that cannot break the timeline. */
    public void sanitize() {
        if (serverTitle == null || serverTitle.isBlank()) serverTitle = "LAPER HARDCORE";
        if (subtitle == null) subtitle = "";
        if (lastMemberLabel == null) lastMemberLabel = "THE LAST MEMBER";
        if (lastCaption == null) lastCaption = "";
        minimumPlayers = clamp(minimumPlayers, 1, 1000);
        autoStartDelay = clamp(autoStartDelay, 0, 1_728_000);
        introCooldown = clamp(introCooldown, 0, 17_280_000);
        openingDuration = clamp(openingDuration, 20, 2400);
        memberDuration = clamp(memberDuration, 40, 1200);
        finaleDuration = clamp(finaleDuration, 40, 2400);
        returnDuration = clamp(returnDuration, 10, 200);
        cameraSpeed = Math.max(0.25f, Math.min(4f, cameraSpeed));
        nameplateScale = Math.max(0.5f, Math.min(3f, nameplateScale));
        nameplateAnimationTicks = clamp(nameplateAnimationTicks, 1, 60);
        String q = String.valueOf(particleQuality).toUpperCase(Locale.ROOT);
        if (!Set.of("LOW", "MEDIUM", "HIGH").contains(q)) {
            LOG.warn("particleQuality '{}' invalid, using MEDIUM", particleQuality);
            particleQuality = "MEDIUM";
        } else particleQuality = q;
        if (!isHex(nameplateColor)) nameplateColor = "#FFD24A";
        if (!isHex(nameplateGradientEnd)) nameplateGradientEnd = "#FF8A1F";
        if (nameplatePosition == null) nameplatePosition = "TOP_CENTER";
        if (silhouetteMembers == null) silhouetteMembers = new ArrayList<>();
        if (memberQuotes == null) memberQuotes = new HashMap<>();
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static boolean isHex(String s) { return s != null && s.matches("#[0-9a-fA-F]{6}"); }
}
