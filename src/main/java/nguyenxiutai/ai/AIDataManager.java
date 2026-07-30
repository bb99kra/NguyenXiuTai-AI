package nguyenxiutai.ai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AI Data Manager - collects and stores game data for analysis
 */
public class AIDataManager {

    private final JavaPlugin plugin;
    private File dataFile;

    // Game history (ring buffer of last N results)
    private final Deque<Boolean> resultHistory = new ArrayDeque<>();
    private final int MAX_HISTORY = 200;

    // Player tracking
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    // Daily stats
    private final Map<String, DailyStats> dailyStatsMap = new HashMap<>();
    private String today;

    // Streak tracking
    private int currentStreak = 0;
    private boolean streakSide = true;

    public AIDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "ai-data.yml");
        this.today = java.time.LocalDate.now().toString();
    }

    /**
     * Record a game result
     */
    public void recordResult(boolean isTai, long taiTotal, long xiuTotal) {
        resultHistory.addFirst(isTai);
        while (resultHistory.size() > MAX_HISTORY) {
            resultHistory.removeLast();
        }

        // FIX #5: Correct streak calculation (was off by +1)
        updateStreak();

        // Update daily stats
        DailyStats ds = getTodayStats();
        ds.totalSessions++;
        if (isTai) ds.taiCount++;
        else ds.xiuCount++;
        ds.totalTaiWagered += taiTotal;
        ds.totalXiuWagered += xiuTotal;
    }

    /**
     * FIX #5: Correct streak calculation
     * The old code initialized streak=1 then counted from the first element again,
     * causing off-by-one. Now we count consecutive same results starting from index 0.
     */
    private void updateStreak() {
        if (resultHistory.isEmpty()) {
            currentStreak = 0;
            return;
        }
        boolean lastSide = resultHistory.peekFirst();
        int streak = 0;
        for (Boolean b : resultHistory) {
            if (b == lastSide) streak++;
            else break;
        }
        currentStreak = streak;
        streakSide = lastSide;
    }

    /**
     * Record a player's bet result
     */
    public void recordPlayerResult(UUID uuid, boolean won, long amount, long payout) {
        PlayerData pd = playerDataMap.computeIfAbsent(uuid, k -> new PlayerData());
        pd.totalSessions++;
        pd.totalWagered += amount;
        if (won) {
            pd.wins++;
            pd.totalWon += payout;
            pd.currentWinStreak++;
            pd.currentLossStreak = 0;
        } else {
            pd.losses++;
            pd.totalLost += amount;
            pd.currentLossStreak++;
            pd.currentWinStreak = 0;
        }
        pd.lastBetTime = System.currentTimeMillis();

        // Update daily player stats
        DailyStats ds = getTodayStats();
        if (won) {
            ds.totalPlayerProfit += (payout - amount);
        } else {
            ds.totalPlayerProfit -= amount;
        }
    }

    /**
     * Get recent N results as boolean array
     */
    public boolean[] getRecentResults(int count) {
        boolean[] results = new boolean[Math.min(count, resultHistory.size())];
        int i = 0;
        for (Boolean b : resultHistory) {
            if (i >= results.length) break;
            results[i++] = b;
        }
        return results;
    }

    /**
     * Count Tai results in last N sessions
     */
    public int countTaiInLast(int n) {
        int count = 0;
        int checked = 0;
        for (Boolean b : resultHistory) {
            if (checked >= n) break;
            if (b) count++;
            checked++;
        }
        return count;
    }

    public int getCurrentStreak() { return currentStreak; }
    public boolean getStreakSide() { return streakSide; }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> new PlayerData());
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        return playerDataMap;
    }

    public int getHistorySize() { return resultHistory.size(); }

    public double getTaiRatioInLast(int n) {
        if (resultHistory.isEmpty()) return 0.5;
        int tai = countTaiInLast(n);
        int total = Math.min(n, resultHistory.size());
        return (double) tai / total;
    }

    public DailyStats getTodayStats() {
        String now = java.time.LocalDate.now().toString();
        if (!now.equals(today)) {
            today = now;
        }
        return dailyStatsMap.computeIfAbsent(today, k -> new DailyStats());
    }

    /**
     * Save data to file
     */
    public void save() {
        try {
            YamlConfiguration c = new YamlConfiguration();

            // Save result history
            int i = 0;
            for (Boolean b : resultHistory) {
                c.set("history." + i, b);
                i++;
            }

            // Save streak
            c.set("streak.count", currentStreak);
            c.set("streak.side", streakSide);

            // FIX #6: Save daily stats (was missing)
            for (Map.Entry<String, DailyStats> entry : dailyStatsMap.entrySet()) {
                String key = "daily." + entry.getKey();
                DailyStats ds = entry.getValue();
                c.set(key + ".sessions", ds.totalSessions);
                c.set(key + ".tai-count", ds.taiCount);
                c.set(key + ".xiu-count", ds.xiuCount);
                c.set(key + ".tai-wagered", ds.totalTaiWagered);
                c.set(key + ".xiu-wagered", ds.totalXiuWagered);
                c.set(key + ".player-profit", ds.totalPlayerProfit);
            }

            // FIX #6: Save player data (was missing: streaks, lastBetTime)
            // Save top 50 by sessions
            List<Map.Entry<UUID, PlayerData>> sorted = new ArrayList<>(playerDataMap.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getValue().totalSessions, a.getValue().totalSessions));
            int limit = Math.min(sorted.size(), 50);
            for (int j = 0; j < limit; j++) {
                Map.Entry<UUID, PlayerData> entry = sorted.get(j);
                String key = "players." + entry.getKey().toString();
                PlayerData pd = entry.getValue();
                c.set(key + ".sessions", pd.totalSessions);
                c.set(key + ".wins", pd.wins);
                c.set(key + ".losses", pd.losses);
                c.set(key + ".wagered", pd.totalWagered);
                c.set(key + ".won", pd.totalWon);
                c.set(key + ".lost", pd.totalLost);
                c.set(key + ".win-streak", pd.currentWinStreak);
                c.set(key + ".loss-streak", pd.currentLossStreak);
                c.set(key + ".last-bet-time", pd.lastBetTime);
            }

            c.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[AI] Failed to save ai-data.yml: " + e.getMessage());
        }
    }

    /**
     * FIX #6: Load data from file - now loads ALL saved data
     */
    public void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration c = YamlConfiguration.loadConfiguration(dataFile);

        // Load history (FIX #9: sort keys numerically)
        if (c.getConfigurationSection("history") != null) {
            List<String> keys = new ArrayList<>(c.getConfigurationSection("history").getKeys(false));
            keys.sort((a, b) -> {
                try {
                    return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
                } catch (NumberFormatException e) {
                    return a.compareTo(b);
                }
            });
            for (String key : keys) {
                resultHistory.addLast(c.getBoolean("history." + key));
            }
        }

        // Load streak
        currentStreak = c.getInt("streak.count", 0);
        streakSide = c.getBoolean("streak.side", true);

        // FIX #6: Load daily stats (was missing)
        if (c.getConfigurationSection("daily") != null) {
            for (String dateKey : c.getConfigurationSection("daily").getKeys(false)) {
                String key = "daily." + dateKey;
                DailyStats ds = new DailyStats();
                ds.totalSessions = c.getInt(key + ".sessions", 0);
                ds.taiCount = c.getInt(key + ".tai-count", 0);
                ds.xiuCount = c.getInt(key + ".xiu-count", 0);
                ds.totalTaiWagered = c.getLong(key + ".tai-wagered", 0);
                ds.totalXiuWagered = c.getLong(key + ".xiu-wagered", 0);
                ds.totalPlayerProfit = c.getLong(key + ".player-profit", 0);
                dailyStatsMap.put(dateKey, ds);
            }
        }

        // FIX #6: Load player data (was missing: streaks, lastBetTime)
        if (c.getConfigurationSection("players") != null) {
            for (String uuidStr : c.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String key = "players." + uuidStr;
                    PlayerData pd = new PlayerData();
                    pd.totalSessions = c.getInt(key + ".sessions", 0);
                    pd.wins = c.getInt(key + ".wins", 0);
                    pd.losses = c.getInt(key + ".losses", 0);
                    pd.totalWagered = c.getLong(key + ".wagered", 0);
                    pd.totalWon = c.getLong(key + ".won", 0);
                    pd.totalLost = c.getLong(key + ".lost", 0);
                    pd.currentWinStreak = c.getInt(key + ".win-streak", 0);
                    pd.currentLossStreak = c.getInt(key + ".loss-streak", 0);
                    pd.lastBetTime = c.getLong(key + ".last-bet-time", 0);
                    playerDataMap.put(uuid, pd);
                } catch (Exception e) {
                    // Skip invalid UUIDs
                }
            }
        }

        plugin.getLogger().info("[AI] Loaded " + resultHistory.size() + " historical results, "
            + playerDataMap.size() + " players, " + dailyStatsMap.size() + " daily stats");
    }

    // === Inner Classes ===

    public static class PlayerData {
        public int totalSessions = 0;
        public int wins = 0;
        public int losses = 0;
        public long totalWagered = 0;
        public long totalWon = 0;
        public long totalLost = 0;
        public int currentWinStreak = 0;
        public int currentLossStreak = 0;
        public long lastBetTime = 0;

        public double getWinRate() {
            return totalSessions == 0 ? 0.5 : (double) wins / totalSessions;
        }

        public long getNetProfit() {
            return totalWon - totalLost;
        }

        public boolean isOnWinStreak(int threshold) {
            return currentWinStreak >= threshold;
        }

        public boolean isOnLossStreak(int threshold) {
            return currentLossStreak >= threshold;
        }
    }

    public static class DailyStats {
        public int totalSessions = 0;
        public int taiCount = 0;
        public int xiuCount = 0;
        public long totalTaiWagered = 0;
        public long totalXiuWagered = 0;
        public long totalPlayerProfit = 0;
    }
}
