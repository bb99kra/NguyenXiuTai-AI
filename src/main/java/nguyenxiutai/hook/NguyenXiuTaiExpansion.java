/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.entity.Player
 */
package nguyenxiutai.hook;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import nguyenxiutai.manager.GameManager;
import nguyenxiutai.manager.StatsManager;
import nguyenxiutai.model.GameSession;
import nguyenxiutai.model.PlayerStats;
import org.bukkit.entity.Player;

public class NguyenXiuTaiExpansion
extends PlaceholderExpansion {
    private final GameManager gm;
    private final StatsManager stats;
    private static final ThreadLocal<DecimalFormat> FMT = ThreadLocal.withInitial(() -> new DecimalFormat("#,###"));
    private volatile int cachedStreak = 0;
    private volatile String cachedStreakSide = null;
    private volatile double cachedTaiPercent = 0.0;
    private volatile double cachedXiuPercent = 0.0;
    private volatile long cacheTimestamp = 0L;
    private static final long CACHE_DURATION_MS = 1000L;

    public NguyenXiuTaiExpansion(GameManager gm, StatsManager stats) {
        this.gm = gm;
        this.stats = stats;
    }

    public String getIdentifier() {
        return "nxt";
    }

    public String getAuthor() {
        return "NguyenXiuTai";
    }

    public String getVersion() {
        try {
            return nguyenxiutai.NguyenXiuTaiPlugin.class.getPackage().getImplementationVersion() != null
                ? nguyenxiutai.NguyenXiuTaiPlugin.class.getPackage().getImplementationVersion()
                : "1.3.0-AI";
        } catch (Exception e) {
            return "1.3.0-AI";
        }
    }

    public boolean persist() {
        return true;
    }

    private void refreshCacheIfNeeded() {
        GameSession.SessionResult r;
        long now = System.currentTimeMillis();
        if (now - this.cacheTimestamp < 1000L) {
            return;
        }
        this.cacheTimestamp = now;
        ConcurrentLinkedDeque<GameSession.SessionResult> history = this.gm.getHistory();
        if (history.isEmpty()) {
            this.cachedStreak = 0;
            this.cachedStreakSide = null;
            this.cachedTaiPercent = 0.0;
            this.cachedXiuPercent = 0.0;
            return;
        }
        ArrayList<GameSession.SessionResult> list = new ArrayList<GameSession.SessionResult>(history);
        boolean firstIsTai = ((GameSession.SessionResult)list.get(0)).isTai();
        int streak = 0;
        Iterator iterator = list.iterator();
        while (iterator.hasNext() && (r = (GameSession.SessionResult)iterator.next()).isTai() == firstIsTai) {
            ++streak;
        }
        this.cachedStreak = streak;
        this.cachedStreakSide = firstIsTai ? "T\u00e0i" : "X\u1ec9u";
        long taiCount = list.stream().filter(GameSession.SessionResult::isTai).count();
        this.cachedTaiPercent = (double)taiCount / (double)list.size() * 100.0;
        this.cachedXiuPercent = 100.0 - this.cachedTaiPercent;
    }

    public String onPlaceholderRequest(Player player, String id) {
        if (id == null) {
            return "";
        }
        GameSession session = this.gm.getCurrentSession();
        String cur = this.gm.getCurrencySymbol();
        DecimalFormat fmt = FMT.get();
        switch (id) {
            case "session": {
                return session != null ? String.valueOf(session.getSessionId()) : "0";
            }
            case "tai_total": {
                return session != null ? String.valueOf(session.getTaiTotal()) : "0";
            }
            case "xiu_total": {
                return session != null ? String.valueOf(session.getXiuTotal()) : "0";
            }
            case "tai_total_formatted": {
                return session != null ? fmt.format(session.getTaiTotal()) : "0";
            }
            case "xiu_total_formatted": {
                return session != null ? fmt.format(session.getXiuTotal()) : "0";
            }
            case "tai_count": {
                return session != null ? String.valueOf(session.getTaiBets().size()) : "0";
            }
            case "xiu_count": {
                return session != null ? String.valueOf(session.getXiuBets().size()) : "0";
            }
            case "total_bet": {
                return session != null ? String.valueOf(session.getTaiTotal() + session.getXiuTotal()) : "0";
            }
            case "total_bet_formatted": {
                return session != null ? fmt.format(session.getTaiTotal() + session.getXiuTotal()) : "0";
            }
            case "hu": {
                return session != null ? String.valueOf(session.getHuAmount()) : "0";
            }
            case "hu_formatted": {
                return session != null ? fmt.format(session.getHuAmount()) : "0";
            }
            case "time_left": {
                return String.valueOf(this.gm.getTimeLeft());
            }
            case "phase": {
                if (session == null) {
                    return "WAITING";
                }
                if (session.isFinished()) {
                    return "FINISHED";
                }
                return this.gm.isRolling() ? "ROLLING" : "BETTING";
            }
        }
        switch (id) {
            case "dice": {
                if (session == null || session.getDiceResults() == null) {
                    return "---";
                }
                int[] d = session.getDiceResults();
                return this.getDice(d[0]) + " + " + this.getDice(d[1]) + " + " + this.getDice(d[2]);
            }
            case "dice_total": {
                return session != null && session.getDiceResults() != null ? String.valueOf(session.getDiceTotal()) : "0";
            }
            case "result": {
                if (session == null || !session.isFinished()) {
                    return "---";
                }
                return session.isTaiResult() ? "T\u00e0i" : "X\u1ec9u";
            }
            case "result_color": {
                if (session == null || !session.isFinished()) {
                    return "&7---";
                }
                return session.isTaiResult() ? "&aT\u00e0i" : "&cX\u1ec9u";
            }
        }
        if (player != null) {
            UUID uuid = player.getUniqueId();
            switch (id) {
                case "player_bet": {
                    if (session == null) {
                        return "0";
                    }
                    Long taiBet = session.getTaiBets().get(uuid);
                    Long xiuBet = session.getXiuBets().get(uuid);
                    long bet = (taiBet != null ? taiBet : 0L) + (xiuBet != null ? xiuBet : 0L);
                    return String.valueOf(bet);
                }
                case "player_bet_formatted": {
                    if (session == null) {
                        return "0";
                    }
                    Long t = session.getTaiBets().get(uuid);
                    Long x = session.getXiuBets().get(uuid);
                    long b = (t != null ? t : 0L) + (x != null ? x : 0L);
                    return fmt.format(b);
                }
                case "player_side": {
                    if (session == null) {
                        return "Ch\u01b0a \u0111\u1eb7t";
                    }
                    int side = session.getPlayerSide(uuid);
                    return side == 0 ? "T\u00e0i" : (side == 1 ? "X\u1ec9u" : "Ch\u01b0a \u0111\u1eb7t");
                }
                case "player_side_color": {
                    if (session == null) {
                        return "&7Ch\u01b0a \u0111\u1eb7t";
                    }
                    int s = session.getPlayerSide(uuid);
                    return s == 0 ? "&aT\u00e0i" : (s == 1 ? "&cX\u1ec9u" : "&7Ch\u01b0a \u0111\u1eb7t");
                }
                case "player_won": {
                    return this.stats.get(uuid).isLastWon() ? "true" : "false";
                }
                case "player_won_amount": {
                    return String.valueOf(this.stats.get(uuid).getLastWonAmount());
                }
                case "player_won_amount_formatted": {
                    return fmt.format(this.stats.get(uuid).getLastWonAmount());
                }
            }
            PlayerStats st = this.stats.get(uuid);
            switch (id) {
                case "player_total_wins": {
                    return String.valueOf(st.getTotalWins());
                }
                case "player_total_losses": {
                    return String.valueOf(st.getTotalLosses());
                }
                case "player_total_wagered": {
                    return String.valueOf(st.getTotalWagered());
                }
                case "player_total_wagered_formatted": {
                    return fmt.format(st.getTotalWagered());
                }
                case "player_total_won": {
                    return String.valueOf(st.getTotalWon());
                }
                case "player_total_won_formatted": {
                    return fmt.format(st.getTotalWon());
                }
                case "player_winrate": {
                    return String.format("%.1f", st.getWinRate());
                }
            }
        }
        if (id.startsWith("history_")) {
            String numStr = id.replace("history_", "").replace("_color", "");
            try {
                String val;
                int num = Integer.parseInt(numStr);
                boolean withColor = id.endsWith("_color");
                ArrayList<GameSession.SessionResult> list = new ArrayList<GameSession.SessionResult>(this.gm.getHistory());
                if (num < 1 || num > list.size()) {
                    return withColor ? "&7-" : "-";
                }
                GameSession.SessionResult r = (GameSession.SessionResult)list.get(num - 1);
                String string = val = r.isTai() ? "T\u00e0i" : "X\u1ec9u";
                return withColor ? (r.isTai() ? "&a" : "&c") + val : val;
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        this.refreshCacheIfNeeded();
        switch (id) {
            case "streak": {
                return String.valueOf(this.cachedStreak);
            }
            case "streak_side": {
                return this.cachedStreakSide != null ? this.cachedStreakSide : "---";
            }
            case "streak_side_color": {
                if (this.cachedStreakSide == null) {
                    return "&7---";
                }
                return this.cachedStreakSide.equals("T\u00e0i") ? "&a" + this.cachedStreakSide : "&c" + this.cachedStreakSide;
            }
        }
        switch (id) {
            case "tai_percent": {
                return String.format("%.1f", this.cachedTaiPercent);
            }
            case "xiu_percent": {
                return String.format("%.1f", this.cachedXiuPercent);
            }
        }
        switch (id) {
            case "min_bet": {
                return String.valueOf(this.gm.getMinBet());
            }
            case "min_bet_formatted": {
                return fmt.format(this.gm.getMinBet());
            }
            case "max_bet": {
                return String.valueOf(this.gm.getMaxBet());
            }
            case "max_bet_formatted": {
                return fmt.format(this.gm.getMaxBet());
            }
            case "currency": {
                return cur;
            }
            case "bet_time": {
                return String.valueOf(this.gm.getBetTime());
            }
            case "roll_time": {
                return String.valueOf(this.gm.getRollTime());
            }
        }
        return null;
    }

    private String getDice(int v) {
        String[] D = new String[]{"\u2680", "\u2681", "\u2682", "\u2683", "\u2684", "\u2685"};
        return v < 1 || v > 6 ? "?" : D[v - 1];
    }
}

