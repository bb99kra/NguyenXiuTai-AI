package nguyenxiutai.ai;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Economy-wide inflation tracker (Feature #1)
 *
 * Reads total server money supply via Vault periodically,
 * tracks M2 growth rate, and signals when inflation/deflation
 * warrants adjusting house-edge or payout parameters.
 *
 * This goes beyond the plugin's own fund (huAmount) to monitor
 * the ENTIRE server economy — essential for F2P balance.
 */
public class EconomyTracker {

    private final JavaPlugin plugin;
    private final AIConfig config;
    private final File trackerFile;

    // Rolling window of total money supply snapshots (per session)
    private final Deque<MoneySnapshot> supplyHistory = new ArrayDeque<>();
    private static final int MAX_SUPPLY_HISTORY = 500;

    // Daily snapshots for M2 growth rate
    private final Map<String, DailySupply> dailySupply = new HashMap<>();

    // Current state
    private double currentM2GrowthRate = 0.0; // % change per day
    private long lastTotalSupply = 0;
    private long lastPluginFund = 0;
    private boolean inflationAlert = false;
    private boolean deflationAlert = false;

    // Thresholds (configurable via ai-config.yml)
    private double inflationThreshold = 0.15;   // 15% daily growth = inflation alert
    private double deflationThreshold = -0.10;  // -10% daily growth = deflation alert
    private int minSessionsForTrend = 10;        // Need at least 10 sessions to detect trend

    // Smoothing factor for house-edge adjustment (0-1, higher = more responsive)
    private double edgeSmoothingFactor = 0.3;

    public EconomyTracker(JavaPlugin plugin, AIConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.trackerFile = new File(plugin.getDataFolder(), "economy-tracker.yml");
    }

    /**
     * Take a snapshot of total server money supply.
     * Called once per session (in startNewSession).
     */
    public void takeSnapshot(long pluginFund) {
        long totalSupply = readTotalServerSupply();
        this.lastTotalSupply = totalSupply;
        this.lastPluginFund = pluginFund;

        String today = java.time.LocalDate.now().toString();
        MoneySnapshot snapshot = new MoneySnapshot(
            System.currentTimeMillis(),
            totalSupply,
            pluginFund,
            Bukkit.getOnlinePlayers().size()
        );

        supplyHistory.addFirst(snapshot);
        while (supplyHistory.size() > MAX_SUPPLY_HISTORY) {
            supplyHistory.removeLast();
        }

        // Update daily aggregate
        DailySupply ds = dailySupply.computeIfAbsent(today, k -> new DailySupply());
        ds.snapshots++;
        ds.lastTotalSupply = totalSupply;
        ds.lastPluginFund = pluginFund;
        if (ds.firstTotalSupply == 0) {
            ds.firstTotalSupply = totalSupply;
        }

        // Calculate M2 growth rate
        calculateM2GrowthRate();

        // Log if significant
        if (inflationAlert || deflationAlert) {
            plugin.getLogger().warning("[AI Economy] M2 growth rate: " +
                String.format("%.1f%%", currentM2GrowthRate * 100) +
                (inflationAlert ? " ⚠️ INFLATION" : " ⚠️ DEFLATION"));
        }
    }

    /**
     * Read total money supply across online players via Vault.
     * This is expensive — called sparingly (once per session).
     *
     * NOTE: Only reads ONLINE players. Offline players with large balances
     * are not counted, so M2 may be underestimated on servers where most
     * money is held by offline accounts. The TREND (growth rate) still
     * works as a signal — absolute numbers matter less than direction.
     *
     * For EssentialsX: getAccounts() could enumerate all known accounts,
     * but iterating hundreds of offline players each session is too expensive.
     * A future improvement could cache offline balances periodically.
     */
    private long readTotalServerSupply() {
        try {
            // Method 1: Via Vault's getBalance for all offline players
            // Note: Vault doesn't have a direct "total supply" method.
            // We use getAccounts() if available, otherwise track known players.
            net.milkbowl.vault.economy.Economy econ = null;
            org.bukkit.plugin.RegisteredServiceProvider rsp =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (rsp != null) {
                econ = (net.milkbowl.vault.economy.Economy) rsp.getProvider();
            }

            if (econ == null) return lastTotalSupply;

            // Try getAccounts() — not all economy plugins support this
            // For EssentialsX/CMI/etc, this returns all known accounts
            long total = 0;
            int counted = 0;

            // Iterate online players first (cheap)
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                total += (long) econ.getBalance(p);
                counted++;
            }

            // For a more complete picture, we'd need to iterate offline players
            // but that's expensive. Use the online total as a proxy + known offline.
            // The trend (growth rate) matters more than the absolute number.

            return total > 0 ? total : lastTotalSupply;
        } catch (Exception e) {
            plugin.getLogger().fine("[AI Economy] Failed to read total supply: " + e.getMessage());
            return lastTotalSupply;
        }
    }

    /**
     * Calculate M2 growth rate from recent snapshots.
     * Uses simple linear trend over the last N sessions.
     */
    private void calculateM2GrowthRate() {
        if (supplyHistory.size() < minSessionsForTrend) {
            currentM2GrowthRate = 0;
            inflationAlert = false;
            deflationAlert = false;
            return;
        }

        // Compare current supply to supply N sessions ago
        MoneySnapshot[] snapshots = supplyHistory.toArray(new MoneySnapshot[0]);
        int lookback = Math.min(20, snapshots.length - 1);
        if (lookback < 2) return;

        MoneySnapshot current = snapshots[0];
        MoneySnapshot past = snapshots[lookback];

        if (past.totalSupply <= 0) return;

        // Growth rate per session, extrapolated to "per day" assuming ~5 min/session
        double rawGrowth = (double)(current.totalSupply - past.totalSupply) / past.totalSupply;
        double sessionsPerDay = 288.0; // ~5 min per session = 288 sessions/day
        currentM2GrowthRate = rawGrowth * (sessionsPerDay / lookback);

        inflationAlert = currentM2GrowthRate > inflationThreshold;
        deflationAlert = currentM2GrowthRate < deflationThreshold;
    }

    /**
     * Get smoothed house-edge adjustment based on M2 growth.
     * Positive M2 growth → increase house-edge (absorb excess money)
     * Negative M2 growth → decrease house-edge (inject money via payouts)
     *
     * Returns an additive adjustment to the base house-edge.
     */
    public double getM2EdgeAdjustment() {
        if (supplyHistory.size() < minSessionsForTrend) return 0.0;

        // Scale: 10% M2 growth → +2% house-edge adjustment
        double adjustment = currentM2GrowthRate * 0.2;

        // Cap at ±5% to avoid extreme swings
        adjustment = Math.max(-0.05, Math.min(0.05, adjustment));

        // Smooth to avoid sudden jumps
        return adjustment * edgeSmoothingFactor;
    }

    /**
     * Get recommended fund injection amount based on trend.
     * If fund is trending down predictively, inject earlier and smaller amounts.
     */
    public long getPredictiveInjection(long currentFund, long baseInjectAmount) {
        if (supplyHistory.size() < minSessionsForTrend) return baseInjectAmount;

        // Calculate fund trend (linear slope over recent sessions)
        MoneySnapshot[] snapshots = supplyHistory.toArray(new MoneySnapshot[0]);
        int window = Math.min(10, snapshots.length);
        if (window < 3) return baseInjectAmount;

        long sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < window; i++) {
            long x = i;
            long y = snapshots[i].pluginFund;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // Linear regression slope
        double n = window;
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

        // If fund is declining (negative slope), project future state
        if (slope < 0) {
            // How many sessions until fund hits zero at current rate?
            double sessionsToZero = currentFund / Math.abs(slope);

            // If less than 20 sessions away, start injecting proactively
            if (sessionsToZero < 20 && sessionsToZero > 0) {
                // Inject smaller amounts more frequently (smoother experience)
                double urgency = 1.0 - (sessionsToZero / 20.0);
                return (long)(baseInjectAmount * (1.0 + urgency));
            }
        }

        return baseInjectAmount;
    }

    /**
     * Get current state for admin display
     */
    public String getStatusSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("M2 Growth: %.2f%%/day", currentM2GrowthRate * 100));
        if (inflationAlert) sb.append(" ⚠️ INFLATION");
        if (deflationAlert) sb.append(" ⚠️ DEFLATION");
        sb.append(String.format(" | Server Supply: %,d", lastTotalSupply));
        sb.append(String.format(" | Plugin Fund: %,d", lastPluginFund));
        sb.append(" | Snapshots: ").append(supplyHistory.size());
        return sb.toString();
    }

    // === Config loading ===

    public void loadConfig(YamlConfiguration c) {
        inflationThreshold = c.getDouble("economy-tracker.inflation-threshold", 0.15);
        deflationThreshold = c.getDouble("economy-tracker.deflation-threshold", -0.10);
        minSessionsForTrend = c.getInt("economy-tracker.min-sessions", 10);
        edgeSmoothingFactor = c.getDouble("economy-tracker.edge-smoothing", 0.3);
    }

    // === Persistence (own file: economy-tracker.yml) ===

    public void save() {
        try {
            YamlConfiguration c = new YamlConfiguration();
            c.set("economy-tracker.last-total-supply", lastTotalSupply);
            c.set("economy-tracker.last-plugin-fund", lastPluginFund);
            c.set("economy-tracker.m2-growth-rate", currentM2GrowthRate);

            int i = 0;
            for (MoneySnapshot s : supplyHistory) {
                if (i >= 100) break;
                String key = "history." + i;
                c.set(key + ".time", s.timestamp);
                c.set(key + ".supply", s.totalSupply);
                c.set(key + ".fund", s.pluginFund);
                c.set(key + ".players", s.onlinePlayers);
                i++;
            }
            c.save(trackerFile);
        } catch (Exception e) {
            plugin.getLogger().warning("[AI] Failed to save economy-tracker.yml: " + e.getMessage());
        }
    }

    public void load() {
        if (!trackerFile.exists()) return;
        try {
            YamlConfiguration c = YamlConfiguration.loadConfiguration(trackerFile);
            lastTotalSupply = c.getLong("economy-tracker.last-total-supply", 0);
            lastPluginFund = c.getLong("economy-tracker.last-plugin-fund", 0);
            currentM2GrowthRate = c.getDouble("economy-tracker.m2-growth-rate", 0);

            if (c.getConfigurationSection("history") != null) {
                for (String key : c.getConfigurationSection("history").getKeys(false)) {
                    try {
                        String path = "history." + key;
                        MoneySnapshot s = new MoneySnapshot(
                            c.getLong(path + ".time"),
                            c.getLong(path + ".supply"),
                            c.getLong(path + ".fund"),
                            c.getInt(path + ".players")
                        );
                        supplyHistory.addLast(s);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[AI] Failed to load economy-tracker.yml: " + e.getMessage());
        }
    }

    // === Inner classes ===

    private static class MoneySnapshot {
        final long timestamp;
        final long totalSupply;
        final long pluginFund;
        final int onlinePlayers;

        MoneySnapshot(long timestamp, long totalSupply, long pluginFund, int onlinePlayers) {
            this.timestamp = timestamp;
            this.totalSupply = totalSupply;
            this.pluginFund = pluginFund;
            this.onlinePlayers = onlinePlayers;
        }
    }

    private static class DailySupply {
        int snapshots = 0;
        long firstTotalSupply = 0;
        long lastTotalSupply = 0;
        long lastPluginFund = 0;
    }
}
