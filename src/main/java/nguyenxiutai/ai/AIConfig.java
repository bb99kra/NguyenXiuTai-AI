package nguyenxiutai.ai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AI Configuration - loads from ai-config.yml
 */
public class AIConfig {

    private boolean enabled = true;

    // === Group 1: Dynamic Ratios ===
    private boolean dynamicRatioEnabled = true;
    private int analysisWindowSize = 50;       // So phien gan nhat de phan tich
    private double maxRatioAdjustment = 0.10;  // Dieu chinh toi da 10%
    private double baseRatio = 0.50;           // 50/50 mac dinh

    private boolean dynamicHouseEdgeEnabled = true;
    private double houseEdgeMin = 0.02;        // 2%
    private double houseEdgeMax = 0.12;        // 12%
    private long quỹThresholdLow = 200000;     // Quy thap
    private long quỹThresholdHigh = 5000000;   // Quy cao

    private boolean dynamicBetLimitEnabled = true;
    private long maxBetFloor = 500000;         // Max bet toi thieu
    private long maxBetCeiling = 15000000;     // Max bet toi da
    private int winStreakThreshold = 5;        // Chuoi thang de giam max bet

    // === Group 2: Analytics ===
    private boolean streakDetectionEnabled = true;
    private int streakAlertThreshold = 8;      // Canh bao khi streak >= 8

    private boolean playerAnalysisEnabled = true;
    private int playerAnalysisMinSessions = 10; // So phien toi thieu de phan tich

    private boolean predictionEnabled = true;
    private int predictionLookback = 20;       // So phien de du doan

    // === Group 3: Smart Bot ===
    private boolean botEnabled = true;
    private int botCount = 5;                  // So luong bot
    private int botMinBet = 5000;
    private int botMaxBet = 500000;
    private double botReactionToStreak = 0.6;  // Bot dat nguoc streak 60%
    private double botFundBalanceRatio = 0.3;  // Bot dieu tiet 30% quỹ

    // === Group 4: Economy ===
    private boolean economyProtectionEnabled = true;
    private long dailyProfitLimit = 2000000;   // Gioi han loi/ngay
    private long dailyLossLimit = -3000000;    // Gioi han lo/ngay
    private long playerDailyLossLimit = -500000; // Per-player daily loss limit
    private long autoInjectThreshold = 100000; // Quy thap thi inject
    private long autoInjectAmount = 200000;    // So tien inject

    private boolean smartBonusEnabled = true;
    private int lossStreakForBonus = 5;        // Thua 5 lan lien tiep -> bonus
    private long bonusAmount = 50000;          // So tien bonus

    public void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "ai-config.yml");
        if (!file.exists()) {
            saveDefault(plugin, file);
        }
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);

        enabled = c.getBoolean("ai.enabled", true);

        // Group 1
        dynamicRatioEnabled = c.getBoolean("ratio.dynamic-enabled", true);
        analysisWindowSize = c.getInt("ratio.window-size", 50);
        maxRatioAdjustment = c.getDouble("ratio.max-adjustment", 0.10);

        dynamicHouseEdgeEnabled = c.getBoolean("house-edge.dynamic-enabled", true);
        houseEdgeMin = c.getDouble("house-edge.min", 0.02);
        houseEdgeMax = c.getDouble("house-edge.max", 0.12);
        quỹThresholdLow = c.getLong("house-edge.quy-threshold-low", 200000);
        quỹThresholdHigh = c.getLong("house-edge.quy-threshold-high", 5000000);

        dynamicBetLimitEnabled = c.getBoolean("bet-limit.dynamic-enabled", true);
        maxBetFloor = c.getLong("bet-limit.floor", 500000);
        maxBetCeiling = c.getLong("bet-limit.ceiling", 15000000);
        winStreakThreshold = c.getInt("bet-limit.win-streak-threshold", 5);

        // Group 2
        streakDetectionEnabled = c.getBoolean("analytics.streak-detection", true);
        streakAlertThreshold = c.getInt("analytics.streak-alert-threshold", 8);
        playerAnalysisEnabled = c.getBoolean("analytics.player-analysis", true);
        playerAnalysisMinSessions = c.getInt("analytics.player-min-sessions", 10);
        predictionEnabled = c.getBoolean("analytics.prediction", true);
        predictionLookback = c.getInt("analytics.prediction-lookback", 20);

        // Group 3
        botEnabled = c.getBoolean("bot.enabled", true);
        botCount = c.getInt("bot.count", 5);
        botMinBet = c.getInt("bot.min-bet", 5000);
        botMaxBet = c.getInt("bot.max-bet", 500000);
        botReactionToStreak = c.getDouble("bot.streak-reaction", 0.6);
        botFundBalanceRatio = c.getDouble("bot.fund-balance-ratio", 0.3);

        // Group 4
        economyProtectionEnabled = c.getBoolean("economy.protection-enabled", true);
        dailyProfitLimit = c.getLong("economy.daily-profit-limit", 2000000);
        dailyLossLimit = c.getLong("economy.daily-loss-limit", -3000000);
        playerDailyLossLimit = c.getLong("economy.player-daily-loss-limit", -500000);
        autoInjectThreshold = c.getLong("economy.auto-inject-threshold", 100000);
        autoInjectAmount = c.getLong("economy.auto-inject-amount", 200000);

        smartBonusEnabled = c.getBoolean("economy.smart-bonus-enabled", true);
        lossStreakForBonus = c.getInt("economy.loss-streak-for-bonus", 5);
        bonusAmount = c.getLong("economy.bonus-amount", 50000);
    }

    private void saveDefault(JavaPlugin plugin, File file) {
        YamlConfiguration c = new YamlConfiguration();
        c.set("ai.enabled", true);

        // Group 1
        c.set("ratio.dynamic-enabled", true);
        c.set("ratio.window-size", 50);
        c.set("ratio.max-adjustment", 0.10);
        c.set("house-edge.dynamic-enabled", true);
        c.set("house-edge.min", 0.02);
        c.set("house-edge.max", 0.12);
        c.set("house-edge.quy-threshold-low", 200000);
        c.set("house-edge.quy-threshold-high", 5000000);
        c.set("bet-limit.dynamic-enabled", true);
        c.set("bet-limit.floor", 500000);
        c.set("bet-limit.ceiling", 15000000);
        c.set("bet-limit.win-streak-threshold", 5);

        // Group 2
        c.set("analytics.streak-detection", true);
        c.set("analytics.streak-alert-threshold", 8);
        c.set("analytics.player-analysis", true);
        c.set("analytics.player-min-sessions", 10);
        c.set("analytics.prediction", true);
        c.set("analytics.prediction-lookback", 20);

        // Group 3
        c.set("bot.enabled", true);
        c.set("bot.count", 5);
        c.set("bot.min-bet", 5000);
        c.set("bot.max-bet", 500000);
        c.set("bot.streak-reaction", 0.6);
        c.set("bot.fund-balance-ratio", 0.3);

        // Group 4
        c.set("economy.protection-enabled", true);
        c.set("economy.daily-profit-limit", 2000000);
        c.set("economy.daily-loss-limit", -3000000);
        c.set("economy.player-daily-loss-limit", -500000);
        c.set("economy.auto-inject-threshold", 100000);
        c.set("economy.auto-inject-amount", 200000);
        c.set("economy.smart-bonus-enabled", true);
        c.set("economy.loss-streak-for-bonus", 5);
        c.set("economy.bonus-amount", 50000);

        try {
            c.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // === Getters ===
    public boolean isEnabled() { return enabled; }
    public boolean isDynamicRatioEnabled() { return dynamicRatioEnabled; }
    public int getAnalysisWindowSize() { return analysisWindowSize; }
    public double getMaxRatioAdjustment() { return maxRatioAdjustment; }
    public double getBaseRatio() { return baseRatio; }
    public boolean isDynamicHouseEdgeEnabled() { return dynamicHouseEdgeEnabled; }
    public double getHouseEdgeMin() { return houseEdgeMin; }
    public double getHouseEdgeMax() { return houseEdgeMax; }
    public long getQuỹThresholdLow() { return quỹThresholdLow; }
    public long getQuỹThresholdHigh() { return quỹThresholdHigh; }
    public boolean isDynamicBetLimitEnabled() { return dynamicBetLimitEnabled; }
    public long getMaxBetFloor() { return maxBetFloor; }
    public long getMaxBetCeiling() { return maxBetCeiling; }
    public int getWinStreakThreshold() { return winStreakThreshold; }
    public boolean isStreakDetectionEnabled() { return streakDetectionEnabled; }
    public int getStreakAlertThreshold() { return streakAlertThreshold; }
    public boolean isPlayerAnalysisEnabled() { return playerAnalysisEnabled; }
    public int getPlayerAnalysisMinSessions() { return playerAnalysisMinSessions; }
    public boolean isPredictionEnabled() { return predictionEnabled; }
    public int getPredictionLookback() { return predictionLookback; }
    public boolean isBotEnabled() { return botEnabled; }
    public int getBotCount() { return botCount; }
    public int getBotMinBet() { return botMinBet; }
    public int getBotMaxBet() { return botMaxBet; }
    public double getBotReactionToStreak() { return botReactionToStreak; }
    public double getBotFundBalanceRatio() { return botFundBalanceRatio; }
    public boolean isEconomyProtectionEnabled() { return economyProtectionEnabled; }
    public long getDailyProfitLimit() { return dailyProfitLimit; }
    public long getDailyLossLimit() { return dailyLossLimit; }
    public long getPlayerDailyLossLimit() { return playerDailyLossLimit; }
    public long getAutoInjectThreshold() { return autoInjectThreshold; }
    public long getAutoInjectAmount() { return autoInjectAmount; }
    public boolean isSmartBonusEnabled() { return smartBonusEnabled; }
    public int getLossStreakForBonus() { return lossStreakForBonus; }
    public long getBonusAmount() { return bonusAmount; }
}
