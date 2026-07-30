/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.boss.BarColor
 *  org.bukkit.boss.BarFlag
 *  org.bukkit.boss.BarStyle
 *  org.bukkit.boss.BossBar
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.java.JavaPlugin
 */
package nguyenxiutai.manager;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import nguyenxiutai.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BossBarManager {
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<UUID, BossBar>();
    private BarColor color = BarColor.RED;
    private BarStyle style = BarStyle.SEGMENTED_10;
    private String cur = "\u0111";
    private final ThreadLocal<DecimalFormat> curFmt = ThreadLocal.withInitial(() -> new DecimalFormat("#,###"));
    private String formatPattern = "#,###";
    private MessageManager msg;
    private static final String[] DICE_FACES = new String[]{"\u2680", "\u2681", "\u2682", "\u2683", "\u2684", "\u2685"};

    public BossBarManager(JavaPlugin plugin) {
    }

    public void setMessageManager(MessageManager msg) {
        this.msg = msg;
    }

    public void configure(String c, String s, String cur, String f) {
        try {
            this.color = BarColor.valueOf((String)c.toUpperCase());
        }
        catch (Exception e) {
            this.color = BarColor.RED;
        }
        try {
            this.style = BarStyle.valueOf((String)s.toUpperCase());
        }
        catch (Exception e) {
            this.style = BarStyle.SEGMENTED_10;
        }
        this.cur = cur;
        this.formatPattern = f;
        this.curFmt.remove();
    }

    public void updateBossBar(int sid, long tai, long xiu, long hu, int time, int max, double taiMultiplier, double xiuMultiplier) {
        double p;
        String template = this.msg != null ? this.msg.getBossBarDangCuoc() : "Phi\u00ean #{session} | T\u00e0i ({tai_total}{currency}) | X\u1ec9u ({xiu_total}{currency}) | H\u0169 ({hu}{currency}) | {time}s";
        Object t = this.formatBossBar(template, sid, tai, xiu, hu, time, null);
        if (taiMultiplier > 0.0 && xiuMultiplier > 0.0) {
            DecimalFormat df = this.curFmt.get();
            String taiMul = String.format("%.2f", taiMultiplier);
            String xiuMul = String.format("%.2f", xiuMultiplier);
            t = (String)t + " | " + taiMul + "x/" + xiuMul + "x";
        }
        BarColor c = (p = Math.max(0.0, Math.min(1.0, (double)time / (double)max))) > 0.44 ? BarColor.GREEN : (p > 0.22 ? BarColor.YELLOW : BarColor.RED);
        this.updateAllBars((String)t, c, p);
    }

    public void updateBossBar(int sid, long tai, long xiu, long hu, int time, int max) {
        this.updateBossBar(sid, tai, xiu, hu, time, max, 0.0, 0.0);
    }

    public void showResultPhase(int sid, long tai, long xiu, long hu) {
        String template = this.msg != null ? this.msg.getBossBarDangQuay() : "Phi\u00ean #{session} | \u0110ang quay... | T\u00e0i ({tai_total}{currency}) | X\u1ec9u ({xiu_total}{currency})";
        String t = this.formatBossBar(template, sid, tai, xiu, hu, -1, null);
        this.updateAllBars(t, BarColor.PURPLE, 1.0);
    }

    public void showFinalResult(int sid, long tai, long xiu, long hu, boolean isTai) {
        String template = this.msg != null ? this.msg.getBossBarKetQua() : "Phi\u00ean #{session} | {result} | T\u00e0i: {tai_total}{currency} | X\u1ec9u: {xiu_total}{currency}";
        String result = isTai ? "T\u00e0i" : "X\u1ec9u";
        String t = this.formatBossBar(template, sid, tai, xiu, hu, -1, result);
        BarColor c = isTai ? BarColor.GREEN : BarColor.RED;
        this.updateAllBars(t, c, 1.0);
    }

    private String formatBossBar(String template, int sid, long tai, long xiu, long hu, int time, String result) {
        DecimalFormat fmt = this.curFmt.get();
        String t = template.replace("{session}", String.valueOf(sid)).replace("{tai_total}", fmt.format(tai)).replace("{xiu_total}", fmt.format(xiu)).replace("{hu}", fmt.format(hu)).replace("{currency}", this.cur);
        if (time >= 0) {
            t = t.replace("{time}", String.valueOf(time));
        }
        if (result != null) {
            t = t.replace("{result}", result);
        }
        return t;
    }

    private void updateAllBars(String title, BarColor color, double progress) {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            BossBar b = this.bars.get(pl.getUniqueId());
            if (b == null) {
                b = Bukkit.createBossBar((String)title, (BarColor)color, (BarStyle)this.style, (BarFlag[])new BarFlag[0]);
                b.addPlayer(pl);
                this.bars.put(pl.getUniqueId(), b);
            }
            b.setTitle(title);
            b.setColor(color);
            b.setProgress(progress);
        }
    }

    public void removeAll() {
        for (BossBar b : this.bars.values()) {
            b.removeAll();
        }
        this.bars.clear();
    }

    public void removePlayer(UUID u) {
        BossBar b = this.bars.remove(u);
        if (b != null) {
            b.removeAll();
        }
    }

    public void addPlayer(Player p) {
        if (!this.bars.containsKey(p.getUniqueId())) {
            BossBar b = Bukkit.createBossBar((String)"", (BarColor)this.color, (BarStyle)this.style, (BarFlag[])new BarFlag[0]);
            b.addPlayer(p);
            this.bars.put(p.getUniqueId(), b);
        }
    }

    public static String getDice(int v) {
        return v < 1 || v > 6 ? "?" : DICE_FACES[v - 1];
    }
}

