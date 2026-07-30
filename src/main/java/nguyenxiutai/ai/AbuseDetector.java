package nguyenxiutai.ai;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Abuse / Anti-Farming Detector (Feature #3)
 *
 * Detects suspicious patterns in bonus claiming and betting behavior:
 * - Alt accounts farming loss-streak bonuses at minimum bet
 * - Sybil accounts (many accounts, same IP/pattern)
 * - Abnormal bonus-to-session ratios
 * - New accounts targeting loss-streak threshold
 *
 * For F2P servers, this is critical to prevent inflation
 * from automated bonus farming.
 */
public class AbuseDetector {

    private final JavaPlugin plugin;
    private final AIConfig config;

    // Per-player abuse tracking
    private final Map<UUID, PlayerAbuseProfile> profiles = new HashMap<>();

    // Global statistics
    private int totalBonusClaims = 0;
    private int totalFlaggedPlayers = 0;
    private int totalBlockedBonuses = 0;

    // Thresholds (configurable)
    private double bonusToSessionRatioThreshold = 0.4; // Flag if >40% of sessions result in bonus
    private int minSessionsForDetection = 5;            // Need 5+ sessions before flagging
    private long maxAccountAgeForSuspicion = 3600000;   // Accounts < 1 hour old are suspicious
    private int minBetAsFarmingThreshold = 3;           // 3+ consecutive minimum bets = suspicious
    private boolean autoBlockEnabled = false;            // Auto-block or just flag?
    private double ipClusterThreshold = 3;              // 3+ accounts from same IP = suspicious

    // IP tracking (if available)
    private final Map<String, List<UUID>> ipToAccounts = new HashMap<>();

    public AbuseDetector(JavaPlugin plugin, AIConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Record a bonus claim for abuse analysis.
     * Called when shouldGiveBonusOnce() returns true and bonus is about to be given.
     *
     * Returns true if the bonus should be ALLOWED, false if it should be BLOCKED.
     */
    public boolean recordAndCheckBonusClaim(UUID uuid, long betAmount, long minBet) {
        PlayerAbuseProfile profile = profiles.computeIfAbsent(uuid, k -> new PlayerAbuseProfile());

        totalBonusClaims++;
        profile.bonusClaims++;
        profile.lastBonusTime = System.currentTimeMillis();

        // Check 1: Is this player betting minimum to farm bonuses?
        if (betAmount <= minBet * 1.1) { // Within 10% of minimum
            profile.minBetBonusCount++;
        } else {
            profile.minBetBonusCount = 0; // Reset on non-min bet
        }

        // Check 2: Calculate bonus-to-session ratio
        double ratio = profile.totalSessions > 0
            ? (double) profile.bonusClaims / profile.totalSessions
            : 0;

        // Check 3: Is the account very new and already claiming bonuses?
        boolean isNewAccount = (System.currentTimeMillis() - profile.firstSeen) < maxAccountAgeForSuspicion;
        boolean newAccountBonus = isNewAccount && profile.bonusClaims >= 2;

        // Check 4: Abnormal bonus ratio after enough sessions
        boolean abnormalRatio = profile.totalSessions >= minSessionsForDetection
            && ratio > bonusToSessionRatioThreshold;

        // Check 5: Consecutive minimum bet farming
        boolean minBetFarming = profile.minBetBonusCount >= minBetAsFarmingThreshold;

        // Check 6: IP cluster (if tracked)
        boolean ipCluster = false;
        String playerIp = getPlayerIp(uuid);
        if (playerIp != null) {
            List<UUID> sameIp = ipToAccounts.get(playerIp);
            if (sameIp != null && sameIp.size() >= ipClusterThreshold) {
                ipCluster = true;
            }
        }

        // Determine if suspicious
        boolean suspicious = abnormalRatio || newAccountBonus || minBetFarming || ipCluster;

        if (suspicious) {
            profile.flagged = true;
            profile.flagReason = buildFlagReason(abnormalRatio, newAccountBonus, minBetFarming, ipCluster, ratio);
            totalFlaggedPlayers++;

            plugin.getLogger().warning("[AI Abuse] Suspicious bonus claim by " + uuid);
            plugin.getLogger().warning("[AI Abuse] Reason: " + profile.flagReason);
            plugin.getLogger().warning("[AI Abuse] Sessions: " + profile.totalSessions +
                " | Bonuses: " + profile.bonusClaims +
                " | Ratio: " + String.format("%.1f%%", ratio * 100) +
                " | MinBetBonuses: " + profile.minBetBonusCount);

            if (autoBlockEnabled) {
                totalBlockedBonuses++;
                return false; // Block the bonus
            }
            // Just flag for admin review
        }

        return true; // Allow the bonus
    }

    /**
     * Record a session start for abuse tracking.
     */
    public void recordSession(UUID uuid, long betAmount, long minBet) {
        PlayerAbuseProfile profile = profiles.computeIfAbsent(uuid, k -> new PlayerAbuseProfile());
        profile.totalSessions++;
        if (profile.firstSeen == 0) {
            profile.firstSeen = System.currentTimeMillis();
        }

        // Track minimum bet pattern
        if (betAmount <= minBet * 1.1) {
            profile.consecutiveMinBets++;
        } else {
            profile.consecutiveMinBets = 0;
        }
        profile.lastBetTime = System.currentTimeMillis();
    }

    /**
     * Track player IP for cluster detection.
     * Call from PlayerJoinEvent if IP is available.
     */
    public void trackPlayerIp(UUID uuid, String ip) {
        if (ip == null || ip.isEmpty()) return;
        List<UUID> accounts = ipToAccounts.computeIfAbsent(ip, k -> new ArrayList<>());
        if (!accounts.contains(uuid)) {
            accounts.add(uuid);
        }
    }

    /**
     * Get list of flagged players for admin review.
     */
    public List<FlaggedPlayer> getFlaggedPlayers() {
        List<FlaggedPlayer> flagged = new ArrayList<>();
        for (Map.Entry<UUID, PlayerAbuseProfile> entry : profiles.entrySet()) {
            if (entry.getValue().flagged) {
                PlayerAbuseProfile p = entry.getValue();
                flagged.add(new FlaggedPlayer(
                    entry.getKey(),
                    p.flagReason,
                    p.totalSessions,
                    p.bonusClaims,
                    p.totalSessions > 0 ? (double) p.bonusClaims / p.totalSessions : 0,
                    p.minBetBonusCount
                ));
            }
        }
        return flagged;
    }

    /**
     * Get status summary for admin display
     */
    public String getStatusSummary() {
        int flagged = (int) profiles.values().stream().filter(p -> p.flagged).count();
        return String.format("Tracked: %d players | Flagged: %d | Total bonuses: %d | Blocked: %d",
            profiles.size(), flagged, totalBonusClaims, totalBlockedBonuses);
    }

    /**
     * Get detailed report for a specific player
     */
    public String getPlayerReport(UUID uuid) {
        PlayerAbuseProfile p = profiles.get(uuid);
        if (p == null) return "No data for this player.";

        double ratio = p.totalSessions > 0 ? (double) p.bonusClaims / p.totalSessions : 0;
        long accountAge = System.currentTimeMillis() - p.firstSeen;
        String ageStr = accountAge < 3600000 ? (accountAge / 60000) + " min" : (accountAge / 3600000) + " hours";

        StringBuilder sb = new StringBuilder();
        sb.append("Sessions: ").append(p.totalSessions);
        sb.append(" | Bonus claims: ").append(p.bonusClaims);
        sb.append(" | Ratio: ").append(String.format("%.1f%%", ratio * 100));
        sb.append(" | Min-bet bonuses: ").append(p.minBetBonusCount);
        sb.append(" | Account age: ").append(ageStr);
        sb.append(" | Consecutive min-bets: ").append(p.consecutiveMinBets);
        if (p.flagged) {
            sb.append("\n§c⚠ FLAGGED: ").append(p.flagReason);
        }
        return sb.toString();
    }

    // === Config ===

    public void loadConfig(YamlConfiguration c) {
        bonusToSessionRatioThreshold = c.getDouble("abuse-detection.bonus-ratio-threshold", 0.4);
        minSessionsForDetection = c.getInt("abuse-detection.min-sessions", 5);
        maxAccountAgeForSuspicion = c.getLong("abuse-detection.new-account-ms", 3600000);
        minBetAsFarmingThreshold = c.getInt("abuse-detection.min-bet-farming-count", 3);
        autoBlockEnabled = c.getBoolean("abuse-detection.auto-block", false);
        ipClusterThreshold = c.getDouble("abuse-detection.ip-cluster-threshold", 3);
    }

    // === Persistence ===

    public void save(YamlConfiguration c) {
        c.set("abuse-detection.total-bonus-claims", totalBonusClaims);
        c.set("abuse-detection.total-flagged", totalFlaggedPlayers);
        c.set("abuse-detection.total-blocked", totalBlockedBonuses);

        int i = 0;
        for (Map.Entry<UUID, PlayerAbuseProfile> entry : profiles.entrySet()) {
            if (i >= 200) break; // Save top 200
            PlayerAbuseProfile p = entry.getValue();
            if (p.totalSessions < 2 && !p.flagged) continue; // Skip trivial entries
            String key = "abuse-detection.players." + entry.getKey().toString();
            c.set(key + ".sessions", p.totalSessions);
            c.set(key + ".bonuses", p.bonusClaims);
            c.set(key + ".min-bet-bonuses", p.minBetBonusCount);
            c.set(key + ".first-seen", p.firstSeen);
            c.set(key + ".flagged", p.flagged);
            c.set(key + ".flag-reason", p.flagReason);
            i++;
        }
    }

    public void load(YamlConfiguration c) {
        totalBonusClaims = c.getInt("abuse-detection.total-bonus-claims", 0);
        totalFlaggedPlayers = c.getInt("abuse-detection.total-flagged", 0);
        totalBlockedBonuses = c.getInt("abuse-detection.total-blocked", 0);

        if (c.getConfigurationSection("abuse-detection.players") != null) {
            for (String uuidStr : c.getConfigurationSection("abuse-detection.players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String key = "abuse-detection.players." + uuidStr;
                    PlayerAbuseProfile p = new PlayerAbuseProfile();
                    p.totalSessions = c.getInt(key + ".sessions", 0);
                    p.bonusClaims = c.getInt(key + ".bonuses", 0);
                    p.minBetBonusCount = c.getInt(key + ".min-bet-bonuses", 0);
                    p.firstSeen = c.getLong(key + ".first-seen", 0);
                    p.flagged = c.getBoolean(key + ".flagged", false);
                    p.flagReason = c.getString(key + ".flag-reason", "");
                    profiles.put(uuid, p);
                } catch (Exception ignored) {}
            }
        }
    }

    // === Helpers ===

    private String getPlayerIp(UUID uuid) {
        org.bukkit.entity.Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.getAddress() != null) {
            return p.getAddress().getAddress().getHostAddress();
        }
        return null;
    }

    private String buildFlagReason(boolean ratio, boolean newAcc, boolean minBet, boolean ip, double actualRatio) {
        StringBuilder sb = new StringBuilder();
        if (ratio) sb.append(String.format("High bonus ratio (%.0f%%); ", actualRatio * 100));
        if (newAcc) sb.append("New account rapid bonus claims; ");
        if (minBet) sb.append("Minimum-bet bonus farming; ");
        if (ip) sb.append("IP cluster (possible Sybil); ");
        return sb.toString().trim();
    }

    // === Inner classes ===

    private static class PlayerAbuseProfile {
        int totalSessions = 0;
        int bonusClaims = 0;
        int minBetBonusCount = 0;
        int consecutiveMinBets = 0;
        long firstSeen = 0;
        long lastBonusTime = 0;
        long lastBetTime = 0;
        boolean flagged = false;
        String flagReason = "";
    }

    public static class FlaggedPlayer {
        public final UUID uuid;
        public final String reason;
        public final int sessions;
        public final int bonusClaims;
        public final double bonusRatio;
        public final int minBetBonuses;

        public FlaggedPlayer(UUID uuid, String reason, int sessions, int bonusClaims, double bonusRatio, int minBetBonuses) {
            this.uuid = uuid;
            this.reason = reason;
            this.sessions = sessions;
            this.bonusClaims = bonusClaims;
            this.bonusRatio = bonusRatio;
            this.minBetBonuses = minBetBonuses;
        }
    }
}
