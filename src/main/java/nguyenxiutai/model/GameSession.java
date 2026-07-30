package nguyenxiutai.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameSession {
    private final int sessionId;
    private final Map<UUID, Long> taiBets = new ConcurrentHashMap<>();
    private final Map<UUID, Long> xiuBets = new ConcurrentHashMap<>();
    // FIX #2: Separate bot bets (decorative only, no economy impact)
    private final Map<UUID, Long> botTaiBets = new ConcurrentHashMap<>();
    private final Map<UUID, Long> botXiuBets = new ConcurrentHashMap<>();
    private volatile long huAmount;
    private volatile boolean finished;
    private volatile int[] diceResults;
    private volatile boolean isTaiResult;
    private volatile boolean jackpot;
    private volatile long cachedTaiTotal = 0L;
    private volatile long cachedXiuTotal = 0L;
    private volatile long cachedBotTaiTotal = 0L;
    private volatile long cachedBotXiuTotal = 0L;

    public GameSession(int id, long hu) {
        this.sessionId = id;
        this.huAmount = hu;
    }

    public int getSessionId() {
        return this.sessionId;
    }

    // === Real player bets ===
    public void addTaiBet(UUID p, long a) {
        this.taiBets.merge(p, a, Long::sum);
        this.cachedTaiTotal += a;
    }

    public void addXiuBet(UUID p, long a) {
        this.xiuBets.merge(p, a, Long::sum);
        this.cachedXiuTotal += a;
    }

    // FIX #2: Bot bets are separate
    public void addBotTaiBet(UUID p, long a) {
        this.botTaiBets.merge(p, a, Long::sum);
        this.cachedBotTaiTotal += a;
    }

    public void addBotXiuBet(UUID p, long a) {
        this.botXiuBets.merge(p, a, Long::sum);
        this.cachedBotXiuTotal += a;
    }

    // FIX #4: Get current bet for a player on a specific side
    public long getTaiBet(UUID p) {
        return this.taiBets.getOrDefault(p, 0L);
    }

    public long getXiuBet(UUID p) {
        return this.xiuBets.getOrDefault(p, 0L);
    }

    public int getPlayerSide(UUID p) {
        if (this.taiBets.containsKey(p)) {
            return 0;
        }
        if (this.xiuBets.containsKey(p)) {
            return 1;
        }
        return -1;
    }

    public long getTaiTotal() {
        return this.cachedTaiTotal;
    }

    public long getXiuTotal() {
        return this.cachedXiuTotal;
    }

    // Bot totals (for display)
    public long getBotTaiTotal() { return this.cachedBotTaiTotal; }
    public long getBotXiuTotal() { return this.cachedBotXiuTotal; }

    public Map<UUID, Long> getTaiBets() {
        return this.taiBets;
    }

    public Map<UUID, Long> getXiuBets() {
        return this.xiuBets;
    }

    public Map<UUID, Long> getBotTaiBets() { return this.botTaiBets; }
    public Map<UUID, Long> getBotXiuBets() { return this.botXiuBets; }

    // FIX: Real player count (excluding bots)
    public int getRealTaiCount() { return this.taiBets.size(); }
    public int getRealXiuCount() { return this.xiuBets.size(); }
    public int getBotTaiCount() { return this.botTaiBets.size(); }
    public int getBotXiuCount() { return this.botXiuBets.size(); }

    public long getHuAmount() {
        return this.huAmount;
    }

    public void setHuAmount(long h) {
        this.huAmount = h;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void setFinished(boolean f) {
        this.finished = f;
    }

    public int[] getDiceResults() {
        return this.diceResults;
    }

    public void setDiceResults(int[] d) {
        this.diceResults = d;
    }

    public boolean isTaiResult() {
        return this.isTaiResult;
    }

    public void setIsTaiResult(boolean t) {
        this.isTaiResult = t;
    }

    public boolean isJackpot() {
        return this.jackpot;
    }

    public void setJackpot(boolean j) {
        this.jackpot = j;
    }

    public int getDiceTotal() {
        return this.diceResults == null ? 0 : this.diceResults[0] + this.diceResults[1] + this.diceResults[2];
    }

    public static class SessionResult {
        private final int sessionId;
        private final int[] dice;
        private final boolean isTai;
        private final long taiTotal;
        private final long xiuTotal;
        private final boolean jackpot;
        private final String jackpotPlayer;

        public SessionResult(int id, int[] d, boolean t, long tt, long xt, boolean jp, String jpn) {
            this.sessionId = id;
            this.dice = d.clone();
            this.isTai = t;
            this.taiTotal = tt;
            this.xiuTotal = xt;
            this.jackpot = jp;
            this.jackpotPlayer = jpn;
        }

        public int getSessionId() { return this.sessionId; }
        public int[] getDice() { return this.dice.clone(); }
        public boolean isTai() { return this.isTai; }
        public long getTaiTotal() { return this.taiTotal; }
        public long getXiuTotal() { return this.xiuTotal; }
        public boolean isJackpot() { return this.jackpot; }
        public String getJackpotPlayer() { return this.jackpotPlayer; }
        public int getDiceTotal() { return this.dice[0] + this.dice[1] + this.dice[2]; }
    }
}
