/*
 * Decompiled with CFR 0.152.
 */
package nguyenxiutai.model;

import java.util.UUID;

public class PlayerStats {
    private final UUID uuid;
    private int totalWins;
    private int totalLosses;
    private long totalWagered;
    private long totalWon;
    private boolean lastWon;
    private long lastWonAmount;
    private String lastSide;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public int getTotalWins() {
        return this.totalWins;
    }

    public int getTotalLosses() {
        return this.totalLosses;
    }

    public long getTotalWagered() {
        return this.totalWagered;
    }

    public long getTotalWon() {
        return this.totalWon;
    }

    public boolean isLastWon() {
        return this.lastWon;
    }

    public long getLastWonAmount() {
        return this.lastWonAmount;
    }

    public String getLastSide() {
        return this.lastSide;
    }

    public void addWin(long amount) {
        ++this.totalWins;
        this.totalWon += amount;
        this.lastWon = true;
        this.lastWonAmount = amount;
    }

    public void addLoss(long amount) {
        ++this.totalLosses;
        this.lastWon = false;
        this.lastWonAmount = 0L;
    }

    public void addWagered(long amount) {
        this.totalWagered += amount;
    }

    public void setLastSide(String side) {
        this.lastSide = side;
    }

    public double getWinRate() {
        int total = this.totalWins + this.totalLosses;
        return total == 0 ? 0.0 : (double)this.totalWins / (double)total * 100.0;
    }

    public void setTotalWins(int v) {
        this.totalWins = v;
    }

    public void setTotalLosses(int v) {
        this.totalLosses = v;
    }

    public void setTotalWagered(long v) {
        this.totalWagered = v;
    }

    public void setTotalWon(long v) {
        this.totalWon = v;
    }

    public void setLastWon(boolean v) {
        this.lastWon = v;
    }

    public void setLastWonAmount(long v) {
        this.lastWonAmount = v;
    }
}

