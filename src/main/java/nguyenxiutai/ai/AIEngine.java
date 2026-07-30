package nguyenxiutai.ai;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI Engine - Brain of the system
 */
public class AIEngine {

    private final AIConfig config;
    private final AIDataManager dataManager;

    // Dynamic values (updated each session)
    private double currentTaiRatio = 0.50;
    private double currentHouseEdge = 0.05;
    private long currentMaxBet = 10000000;

    // FIX #3: Track bonus-given state per player (lossStreak when bonus was given)
    private final Map<UUID, Integer> lastBonusLossStreak = new ConcurrentHashMap<>();

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
     * FIX #13: Calculate Tai probability - single source of truth
     * Used by both rollDice() and getPrediction()
     */
    public double calculateTaiRatio() {
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

        double adjustment = -deviation * 0.5;
        adjustment = Math.max(-maxAdj, Math.min(maxAdj, adjustment));

        currentTaiRatio = baseRatio + adjustment;
        currentTaiRatio = Math.max(0.35, Math.min(0.65, currentTaiRatio));

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

        if (highThreshold <= lowThreshold) {
            // FIX: Prevent division by zero
            currentHouseEdge = minEdge;
            return currentHouseEdge;
        }

        if (fundAmount <= lowThreshold) {
            currentHouseEdge = maxEdge;
        } else if (fundAmount >= highThreshold) {
            currentHouseEdge = minEdge;
        } else {
            double ratio = (double)(fundAmount - lowThreshold) / (highThreshold - lowThreshold);
            currentHouseEdge = maxEdge - ratio * (maxEdge - minEdge);
        }

        return currentHouseEdge;
    }

    /**
     * Calculate dynamic max bet for a player
     * FIX: Uses baseMaxBet from config, not overridden ceiling
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
            reduction = Math.max(0.3, reduction);
            long adjusted = (long)(baseMaxBet * reduction);
            return Math.max(floor, Math.min(ceiling, adjusted));
        }

        // If player is on loss streak, slightly increase their max bet
        if (pd.isOnLossStreak(streakThreshold)) {
            double increase = 1.0 + (pd.currentLossStreak - streakThreshold + 1) * 0.1;
            increase = Math.min(1.5, increase);
            long adjusted = (long)(baseMaxBet * increase);
            return Math.max(floor, Math.min(ceiling, adjusted));
        }

        return Math.max(floor, Math.min(ceiling, baseMaxBet));
    }

    // =====================================================
    // GROUP 2: ANALYTICS
    // =====================================================

    public StreakInfo getStreakInfo() {
        int streak = dataManager.getCurrentStreak();
        boolean side = dataManager.getStreakSide();
        boolean alert = streak >= config.getStreakAlertThreshold();
        return new StreakInfo(streak, side, alert);
    }

    /**
     * FIX #13: Prediction now uses the SAME calculateTaiRatio() as rollDice()
     */
    public Prediction getPrediction() {
        if (!config.isEnabled() || !config.isPredictionEnabled()) {
            return new Prediction(0.5, 0.5, "Disabled");
        }

        // Use the same probability as rollDice
        double taiProb = calculateTaiRatio();
        double xiuProb = 1.0 - taiProb;

        int streak = dataManager.getCurrentStreak();
        boolean streakSide = dataManager.getStreakSide();

        // Streak correction for display (visual hint, doesn't affect roll)
        if (streak >= 5) {
            double reversalBoost = Math.min(0.15, (streak - 4) * 0.03);
            if (streakSide) {
                xiuProb += reversalBoost;
                taiProb -= reversalBoost;
            } else {
                taiProb += reversalBoost;
                xiuProb -= reversalBoost;
            }
            // Normalize
            double total = taiProb + xiuProb;
            taiProb /= total;
            xiuProb /= total;
        }

        String analysis = String.format("Tai: %.0f%% | Xiu: %.0f%% | Streak: %s %d",
            taiProb * 100, xiuProb * 100, streakSide ? "Tài" : "Xỉu", streak);

        return new Prediction(taiProb, xiuProb, analysis);
    }

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

    public boolean shouldInjectFund(long currentFund) {
        return config.isEnabled() &&
               config.isEconomyProtectionEnabled() &&
               currentFund < config.getAutoInjectThreshold();
    }

    public long getInjectAmount() {
        return config.getAutoInjectAmount();
    }

    /**
     * FIX #3: Only give bonus when loss streak JUST hit the threshold
     * Not repeatedly every session
     */
    public boolean shouldGiveBonusOnce(UUID uuid) {
        if (!config.isEnabled() || !config.isSmartBonusEnabled()) return false;
        AIDataManager.PlayerData pd = dataManager.getPlayerData(uuid);
        int threshold = config.getLossStreakForBonus();

        // Must be exactly at threshold (or just passed it this session)
        if (!pd.isOnLossStreak(threshold)) return false;

        // Check if we already gave bonus at this streak level
        Integer lastStreak = lastBonusLossStreak.get(uuid);
        if (lastStreak != null && pd.currentLossStreak <= lastStreak) {
            return false; // Already gave bonus at this or higher streak
        }

        // Mark as given
        lastBonusLossStreak.put(uuid, pd.currentLossStreak);
        return true;
    }

    /**
     * Reset bonus tracking for a player (called when they win)
     */
    public void resetBonusTracking(UUID uuid) {
        lastBonusLossStreak.remove(uuid);
    }

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
        public final boolean side;
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
