package nguyenxiutai.ai;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI Engine - Brain of the system
 * Analyzes data and makes decisions for dynamic ratios, economy, etc.
 */
public class AIEngine {

    private final AIConfig config;
    private final AIDataManager dataManager;

    // Dynamic values (updated each session)
    private double currentTaiRatio = 0.50;
    private double currentHouseEdge = 0.05;
    private long currentMaxBet = 10000000;

    public AIEngine(AIConfig config, AIDataManager dataManager) {
        this.config = config;
        this.dataManager = dataManager;
    }

    // =====================================================
    // GROUP 1: DYNAMIC RATIOS
    // =====================================================

    /**
     * Roll dice with AI-adjusted probability
     * Returns true = Tai, false = Xiu
     */
    public boolean rollDice() {
        if (!config.isEnabled() || !config.isDynamicRatioEnabled()) {
            return ThreadLocalRandom.current().nextBoolean();
        }

        double ratio = calculateTaiRatio();
        return ThreadLocalRandom.current().nextDouble() < ratio;
    }

    /**
     * Calculate Tai probability based on history
     */
    private double calculateTaiRatio() {
        int window = config.getAnalysisWindowSize();
        double baseRatio = config.getBaseRatio();
        double maxAdj = config.getMaxRatioAdjustment();

        int historySize = dataManager.getHistorySize();
        if (historySize < 10) {
            currentTaiRatio = baseRatio;
            return baseRatio;
        }

        double taiRatio = dataManager.getTaiRatioInLast(window);
        double deviation = taiRatio - baseRatio;

        // If Tai is running hot (>55%), reduce Tai probability
        // If Tai is running cold (<45%), increase Tai probability
        double adjustment = -deviation * 0.5; // Dampened correction
        adjustment = Math.max(-maxAdj, Math.min(maxAdj, adjustment));

        currentTaiRatio = baseRatio + adjustment;
        currentTaiRatio = Math.max(0.35, Math.min(0.65, currentTaiRatio)); // Safety bounds

        return currentTaiRatio;
    }

    /**
     * Calculate dynamic house edge based on fund level
     */
    public double calculateHouseEdge(long fundAmount) {
        if (!config.isEnabled() || !config.isDynamicHouseEdgeEnabled()) {
            return currentHouseEdge;
        }

        double minEdge = config.getHouseEdgeMin();
        double maxEdge = config.getHouseEdgeMax();
        long lowThreshold = config.getQuỹThresholdLow();
        long highThreshold = config.getQuỹThresholdHigh();

        if (fundAmount <= lowThreshold) {
            // Fund is low - increase house edge to protect
            currentHouseEdge = maxEdge;
        } else if (fundAmount >= highThreshold) {
            // Fund is high - decrease house edge (let players win more)
            currentHouseEdge = minEdge;
        } else {
            // Linear interpolation between thresholds
            double ratio = (double)(fundAmount - lowThreshold) / (highThreshold - lowThreshold);
            currentHouseEdge = maxEdge - ratio * (maxEdge - minEdge);
        }

        return currentHouseEdge;
    }

    /**
     * Calculate dynamic max bet for a player
     */
    public long calculateMaxBet(UUID uuid, long baseMaxBet) {
        if (!config.isEnabled() || !config.isDynamicBetLimitEnabled()) {
            return baseMaxBet;
        }

        AIDataManager.PlayerData pd = dataManager.getPlayerData(uuid);
        long floor = config.getMaxBetFloor();
        long ceiling = config.getMaxBetCeiling();
        int streakThreshold = config.getWinStreakThreshold();

        // If player is on win streak, reduce their max bet
        if (pd.isOnWinStreak(streakThreshold)) {
            double reduction = 1.0 - (pd.currentWinStreak - streakThreshold + 1) * 0.15;
            reduction = Math.max(0.3, reduction); // At least 30% of base
            long adjusted = (long)(baseMaxBet * reduction);
            return Math.max(floor, Math.min(ceiling, adjusted));
        }

        // If player is on loss streak, slightly increase their max bet (encourage play)
        if (pd.isOnLossStreak(streakThreshold)) {
            double increase = 1.0 + (pd.currentLossStreak - streakThreshold + 1) * 0.1;
            increase = Math.min(1.5, increase); // Max 150% of base
            long adjusted = (long)(baseMaxBet * increase);
            return Math.max(floor, Math.min(ceiling, adjusted));
        }

        return Math.max(floor, Math.min(ceiling, baseMaxBet));
    }

    // =====================================================
    // GROUP 2: ANALYTICS
    // =====================================================

    /**
     * Get streak analysis
     */
    public StreakInfo getStreakInfo() {
        int streak = dataManager.getCurrentStreak();
        boolean side = dataManager.getStreakSide();
        boolean alert = streak >= config.getStreakAlertThreshold();
        return new StreakInfo(streak, side, alert);
    }

    /**
     * Get prediction for next session
     */
    public Prediction getPrediction() {
        if (!config.isEnabled() || !config.isPredictionEnabled()) {
            return new Prediction(0.5, 0.5, "Disabled");
        }

        int lookback = config.getPredictionLookback();
        double taiRatio = dataManager.getTaiRatioInLast(lookback);
        int streak = dataManager.getCurrentStreak();
        boolean streakSide = dataManager.getStreakSide();

        // Base prediction from history
        double taiProb = taiRatio;
        double xiuProb = 1.0 - taiRatio;

        // Streak correction: if streak is long, predict reversal
        if (streak >= 5) {
            double reversalBoost = Math.min(0.15, (streak - 4) * 0.03);
            if (streakSide) {
                // Tai streak -> predict Xiu more
                xiuProb += reversalBoost;
                taiProb -= reversalBoost;
            } else {
                // Xiu streak -> predict Tai more
                taiProb += reversalBoost;
                xiuProb -= reversalBoost;
            }
        }

        // Normalize
        double total = taiProb + xiuProb;
        taiProb /= total;
        xiuProb /= total;

        String analysis = String.format("Tai: %.0f%% | Xiu: %.0f%% | Streak: %s %d",
            taiProb * 100, xiuProb * 100, streakSide ? "Tài" : "Xỉu", streak);

        return new Prediction(taiProb, xiuProb, analysis);
    }

    /**
     * Get player analysis
     */
    public PlayerAnalysis getPlayerAnalysis(UUID uuid) {
        AIDataManager.PlayerData pd = dataManager.getPlayerData(uuid);
        if (pd.totalSessions < config.getPlayerAnalysisMinSessions()) {
            return new PlayerAnalysis("Not enough data", pd.totalSessions);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Sessions: %d | Win rate: %.1f%%",
            pd.totalSessions, pd.getWinRate() * 100));
        sb.append(String.format(" | Net: %s", formatMoney(pd.getNetProfit())));

        if (pd.isOnWinStreak(config.getWinStreakThreshold())) {
            sb.append(" | &aStreak thắng: ").append(pd.currentWinStreak);
        }
        if (pd.isOnLossStreak(config.getWinStreakThreshold())) {
            sb.append(" | &cStreak thua: ").append(pd.currentLossStreak);
        }

        return new PlayerAnalysis(sb.toString(), pd.totalSessions);
    }

    // =====================================================
    // GROUP 4: ECONOMY
    // =====================================================

    /**
     * Check if economy needs protection
     */
    public EconomyStatus checkEconomy() {
        if (!config.isEnabled() || !config.isEconomyProtectionEnabled()) {
            return new EconomyStatus(false, "Disabled", 0, 0);
        }

        AIDataManager.DailyStats ds = dataManager.getTodayStats();
        long profit = ds.totalPlayerProfit;
        boolean needsAction = false;
        String message = "";

        if (profit >= config.getDailyProfitLimit()) {
            needsAction = true;
            message = "Players winning too much today! Profit: " + formatMoney(profit);
        } else if (profit <= config.getDailyLossLimit()) {
            needsAction = true;
            message = "Players losing too much today! Loss: " + formatMoney(profit);
        }

        return new EconomyStatus(needsAction, message, profit, ds.totalSessions);
    }

    /**
     * Check if auto-inject is needed
     */
    public boolean shouldInjectFund(long currentFund) {
        return config.isEnabled() &&
               config.isEconomyProtectionEnabled() &&
               currentFund < config.getAutoInjectThreshold();
    }

    /**
     * Get auto-inject amount
     */
    public long getInjectAmount() {
        return config.getAutoInjectAmount();
    }

    /**
     * Check if player deserves a smart bonus
     */
    public boolean shouldGiveBonus(UUID uuid) {
        if (!config.isEnabled() || !config.isSmartBonusEnabled()) return false;
        AIDataManager.PlayerData pd = dataManager.getPlayerData(uuid);
        return pd.isOnLossStreak(config.getLossStreakForBonus());
    }

    /**
     * Get bonus amount
     */
    public long getSmartBonusAmount() {
        return config.getBonusAmount();
    }

    // =====================================================
    // GETTERS
    // =====================================================

    public double getCurrentTaiRatio() { return currentTaiRatio; }
    public double getCurrentHouseEdge() { return currentHouseEdge; }
    public long getCurrentMaxBet() { return currentMaxBet; }
    public AIConfig getConfig() { return config; }
    public AIDataManager getDataManager() { return dataManager; }

    private String formatMoney(long amount) {
        return String.format("%,d", amount);
    }

    // =====================================================
    // INNER CLASSES
    // =====================================================

    public static class StreakInfo {
        public final int count;
        public final boolean side; // true=Tai
        public final boolean alert;

        public StreakInfo(int count, boolean side, boolean alert) {
            this.count = count;
            this.side = side;
            this.alert = alert;
        }

        public String getDisplay() {
            return (side ? "Tài" : "Xỉu") + " x" + count;
        }
    }

    public static class Prediction {
        public final double taiProb;
        public final double xiuProb;
        public final String analysis;

        public Prediction(double taiProb, double xiuProb, String analysis) {
            this.taiProb = taiProb;
            this.xiuProb = xiuProb;
            this.analysis = analysis;
        }
    }

    public static class PlayerAnalysis {
        public final String summary;
        public final int sessions;

        public PlayerAnalysis(String summary, int sessions) {
            this.summary = summary;
            this.sessions = sessions;
        }
    }

    public static class EconomyStatus {
        public final boolean needsAction;
        public final String message;
        public final long dailyProfit;
        public final int totalSessions;

        public EconomyStatus(boolean needsAction, String message, long dailyProfit, int totalSessions) {
            this.needsAction = needsAction;
            this.message = message;
            this.dailyProfit = dailyProfit;
            this.totalSessions = totalSessions;
        }
    }
}
