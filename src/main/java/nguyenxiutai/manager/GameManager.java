package nguyenxiutai.manager;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import nguyenxiutai.ai.AIDataManager;
import nguyenxiutai.ai.AIEngine;
import nguyenxiutai.ai.SmartBot;
import nguyenxiutai.gui.PersonalHistory;
import nguyenxiutai.model.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GameManager {
    private final JavaPlugin plugin;
    private final EconomyManager eco;
    private final BossBarManager boss;
    private final DiscordManager discord;
    private final MessageManager msgManager;
    private final StatsManager statsManager;
    private DailyBonusManager dailyBonusManager;
    private GameSession session;
    private int sessionCounter;
    private int betTime;
    private int rollTime;
    private int timeLeft;
    private int jpChance;
    private long huAmount;
    private long initHu;
    private long minBet;
    private long maxBet;
    private long jpAmount;
    private String cur;
    private boolean balancedPayout;
    private double houseEdge;
    private final ThreadLocal<DecimalFormat> curFmt = ThreadLocal.withInitial(() -> new DecimalFormat("#,###"));
    private BukkitTask timer;
    private BukkitTask autoSaveTask;
    private volatile boolean rolling;
    private final ConcurrentLinkedDeque<GameSession.SessionResult> history = new ConcurrentLinkedDeque<>();
    private final ReentrantLock betLock = new ReentrantLock();

    // === AI Components ===
    private AIEngine aiEngine;
    private SmartBot smartBot;
    private AIDataManager aiDataManager;

    public GameManager(JavaPlugin plugin, EconomyManager eco, BossBarManager boss, DiscordManager discord, MessageManager msgManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.eco = eco;
        this.boss = boss;
        this.discord = discord;
        this.msgManager = msgManager;
        this.statsManager = statsManager;
    }

    /**
     * Set AI components (called from plugin onEnable)
     */
    public void setAI(AIEngine aiEngine, SmartBot smartBot, AIDataManager aiDataManager) {
        this.aiEngine = aiEngine;
        this.smartBot = smartBot;
        this.aiDataManager = aiDataManager;
    }

    public void loadConfig() {
        File configFile = new File(this.plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            this.plugin.saveDefaultConfig();
            try {
                YamlConfiguration def = new YamlConfiguration();
                def.set("thoi-gian-dat-cuoc", 45);
                def.set("thoi-gian-quay", 5);
                def.set("so-tien-ban-dau-hu", 500000);
                def.set("cuoc-toi-thieu", 1000);
                def.set("cuoc-toi-da", 10000000);
                def.set("ti-le-no-hu", 100);
                def.set("so-tien-no-hu", 5000000);
                def.set("tien-te", "đ");
                def.set("format-tien", "#,###");
                def.set("bossbar-mau", "RED");
                def.set("bossbar-kieu", "SEGMENTED_10");
                def.set("auto-save-interval", 300);
                def.set("can-bang-payout", true);
                def.set("house-edge", 5);
                def.save(configFile);
                this.plugin.getLogger().info("[NguyenXiuTai] Created default config.yml");
            } catch (Exception ex) {
                this.plugin.getLogger().warning("[NguyenXiuTai] Failed to create config.yml: " + ex.getMessage());
            }
        }
        YamlConfiguration c = YamlConfiguration.loadConfiguration(configFile);
        this.betTime = c.getInt("thoi-gian-dat-cuoc", 45);
        this.rollTime = c.getInt("thoi-gian-quay", 5);
        this.initHu = c.getLong("so-tien-ban-dau-hu", 500000L);
        this.minBet = c.getLong("cuoc-toi-thieu", 1000L);
        this.maxBet = c.getLong("cuoc-toi-da", 10000000L);
        this.jpChance = c.getInt("ti-le-no-hu", 100);
        this.jpAmount = c.getLong("so-tien-no-hu", 5000000L);
        this.cur = c.getString("tien-te", "đ");
        String f = c.getString("format-tien", "#,###");
        try { this.curFmt.remove(); } catch (Exception e) {}
        this.balancedPayout = c.getBoolean("can-bang-payout", true);
        this.houseEdge = c.getDouble("house-edge", 5.0) / 100.0;
        this.huAmount = c.getLong("current-hu", this.initHu);
        this.sessionCounter = c.getInt("current-session", 0);

        try {
            c.set("thoi-gian-dat-cuoc", this.betTime);
            c.set("thoi-gian-quay", this.rollTime);
            c.set("so-tien-ban-dau-hu", this.initHu);
            c.set("cuoc-toi-thieu", this.minBet);
            c.set("cuoc-toi-da", this.maxBet);
            c.set("ti-le-no-hu", this.jpChance);
            c.set("so-tien-no-hu", this.jpAmount);
            c.set("tien-te", this.cur);
            c.set("format-tien", f);
            c.set("bossbar-mau", c.getString("bossbar-mau", "RED"));
            c.set("bossbar-kieu", c.getString("bossbar-kieu", "SEGMENTED_10"));
            c.set("current-hu", this.huAmount);
            c.set("current-session", this.sessionCounter);
            c.set("auto-save-interval", c.getInt("auto-save-interval", 300));
            c.set("can-bang-payout", this.balancedPayout);
            c.set("house-edge", this.houseEdge * 100.0);
            c.save(configFile);
        } catch (Exception ex) {
            this.plugin.getLogger().warning("[NguyenXiuTai] Failed to save config: " + ex.getMessage());
        }

        this.boss.configure(c.getString("bossbar-mau", "RED"), c.getString("bossbar-kieu", "SEGMENTED_10"), this.cur, f);

        try {
            File discordFile = new File(this.plugin.getDataFolder(), "discord.yml");
            if (!discordFile.exists()) {
                YamlConfiguration dc = new YamlConfiguration();
                dc.set("enabled", false);
                dc.set("url", "");
                dc.save(discordFile);
            }
            YamlConfiguration dc = YamlConfiguration.loadConfiguration(discordFile);
            this.discord.configure(dc.getBoolean("enabled", false), dc.getString("url", ""), this.cur);
        } catch (Exception ex) {
            this.plugin.getLogger().warning("[Discord] Failed to load discord.yml: " + ex.getMessage());
            this.discord.configure(false, "", this.cur);
        }

        this.msgManager.load();
        int autoSaveInterval = c.getInt("auto-save-interval", 300);
        this.startAutoSave(autoSaveInterval);

        long dailyBonusAmount = c.getLong("daily-bonus-amount", 10000L);
        int streakDays = c.getInt("daily-bonus-streak-days", 7);
        double streakMult = c.getDouble("daily-bonus-streak-multiplier", 2.0);
        this.dailyBonusManager = new DailyBonusManager(this.plugin);
        this.dailyBonusManager.load(dailyBonusAmount, streakDays, streakMult);
    }

    private void startAutoSave(int intervalSeconds) {
        if (this.autoSaveTask != null) this.autoSaveTask.cancel();
        if (intervalSeconds <= 0) return;
        this.autoSaveTask = new BukkitRunnable() {
            public void run() {
                if (GameManager.this.statsManager.isDirty()) {
                    GameManager.this.statsManager.save();
                    GameManager.this.savePersistentData();
                }
                // Save AI data periodically
                if (GameManager.this.aiDataManager != null) {
                    GameManager.this.aiDataManager.save();
                }
            }
        }.runTaskTimer(this.plugin, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    public void startNewSession() {
        ++this.sessionCounter;
        this.session = new GameSession(this.sessionCounter, this.huAmount);
        this.rolling = false;
        this.timeLeft = this.betTime;

        // === AI: Update dynamic values each session ===
        if (aiEngine != null && aiEngine.getConfig().isEnabled()) {
            // Update house edge based on fund
            this.houseEdge = aiEngine.calculateHouseEdge(this.huAmount);
            // Update max bet (base, per-player adjustment happens in placeBet)
            this.maxBet = aiEngine.getConfig().getMaxBetCeiling();
        }

        // === AI: Inject bots ===
        if (smartBot != null && aiEngine != null && aiEngine.getConfig().isBotEnabled()) {
            List<SmartBot.BotBet> botBets = smartBot.generateBotBets(aiEngine);
            for (SmartBot.BotBet bet : botBets) {
                if (bet.isTai) {
                    this.session.addTaiBet(bet.uuid, bet.amount);
                } else {
                    this.session.addXiuBet(bet.uuid, bet.amount);
                }
            }
            if (!botBets.isEmpty()) {
                this.plugin.getLogger().info("[AI] " + botBets.size() + " bots placed bets this session");
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            this.boss.addPlayer(p);
        }

        String phienMsg = this.msgManager.getMsgPhienMoi().replace("{session}", String.valueOf(this.sessionCounter));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(phienMsg);
        }

        // === AI: Show streak alert if needed ===
        if (aiEngine != null && aiEngine.getConfig().isStreakDetectionEnabled()) {
            AIEngine.StreakInfo streak = aiEngine.getStreakInfo();
            if (streak.alert) {
                String alertMsg = "§c⚠️ [AI] Cầu đang ra " + streak.getDisplay() + " - Dài bất thường!";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(alertMsg);
                }
            }
        }

        this.startTimer();
    }

    private void startTimer() {
        if (this.timer != null) this.timer.cancel();
        this.timer = new BukkitRunnable() {
            public void run() {
                --GameManager.this.timeLeft;
                if (GameManager.this.timeLeft < 0) {
                    this.cancel();
                    GameManager.this.startRoll();
                    return;
                }
                GameManager.this.boss.updateBossBar(
                    GameManager.this.session.getSessionId(),
                    GameManager.this.session.getTaiTotal(),
                    GameManager.this.session.getXiuTotal(),
                    GameManager.this.session.getHuAmount(),
                    GameManager.this.timeLeft,
                    GameManager.this.betTime,
                    GameManager.this.getPayoutMultiplier(true),
                    GameManager.this.getPayoutMultiplier(false)
                );
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
    }

    private void startRoll() {
        this.rolling = true;
        this.boss.showResultPhase(this.session.getSessionId(), this.session.getTaiTotal(), this.session.getXiuTotal(), this.session.getHuAmount());
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(this.msgManager.getMsgRolling());
        }
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (this.t >= GameManager.this.rollTime * 2) {
                    this.cancel();
                    GameManager.this.rollDice();
                    return;
                }
                int[] d = new int[]{GameManager.this.rnd(), GameManager.this.rnd(), GameManager.this.rnd()};
                String ab = "§6🎲 §f" + BossBarManager.getDice(d[0]) + " + " + BossBarManager.getDice(d[1]) + " + " + BossBarManager.getDice(d[2]) + " = §e" + (d[0] + d[1] + d[2]) + " §f→ ?";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ab));
                }
                ++this.t;
            }
        }.runTaskTimer(this.plugin, 0L, 10L);
    }

    private void rollDice() {
        // === AI: Use AI engine for dice roll ===
        boolean isTai;
        if (aiEngine != null && aiEngine.getConfig().isEnabled() && aiEngine.getConfig().isDynamicRatioEnabled()) {
            isTai = aiEngine.rollDice();
        } else {
            int total = rnd() + rnd() + rnd();
            isTai = total >= 11;
        }

        // Generate visual dice (must match result)
        int d1, d2, d3;
        do {
            d1 = rnd(); d2 = rnd(); d3 = rnd();
        } while ((d1 + d2 + d3 >= 11) != isTai);
        int total = d1 + d2 + d3;

        this.session.setDiceResults(new int[]{d1, d2, d3});
        this.session.setIsTaiResult(isTai);
        this.session.setFinished(true);

        boolean jp = false;
        String jpName = null;
        if (ThreadLocalRandom.current().nextInt(this.jpChance) == 0) {
            jp = true;
            this.session.setJackpot(true);
            Map<UUID, Long> winners = isTai ? this.session.getTaiBets() : this.session.getXiuBets();
            if (!winners.isEmpty()) {
                Map.Entry<UUID, Long> top = null;
                for (Map.Entry<UUID, Long> e : winners.entrySet()) {
                    if (top != null && e.getValue() <= top.getValue()) continue;
                    top = e;
                }
                if (top != null) {
                    UUID u = top.getKey();
                    Player w = Bukkit.getPlayer(u);
                    jpName = w != null ? w.getName() : u.toString();
                    this.eco.deposit(u, this.jpAmount);
                    this.huAmount = this.initHu;
                }
            }
        }

        long taiTotal = this.session.getTaiTotal();
        long xiuTotal = this.session.getXiuTotal();

        if (isTai) {
            this.payWinners(this.session.getTaiBets(), this.session.getXiuBets());
        } else {
            this.payWinners(this.session.getXiuBets(), this.session.getTaiBets());
        }

        // === AI: Record data ===
        if (aiDataManager != null) {
            aiDataManager.recordResult(isTai, taiTotal, xiuTotal);

            // Record player results
            Map<UUID, Long> winnerBets = isTai ? this.session.getTaiBets() : this.session.getXiuBets();
            Map<UUID, Long> loserBets = isTai ? this.session.getXiuBets() : this.session.getTaiBets();

            for (Map.Entry<UUID, Long> entry : winnerBets.entrySet()) {
                // Skip bots
                if (smartBot != null && smartBot.isBot(entry.getKey())) continue;
                long payout = calculatePayout(entry.getValue(), winnerBets, loserBets);
                aiDataManager.recordPlayerResult(entry.getKey(), true, entry.getValue(), payout);
            }
            for (Map.Entry<UUID, Long> entry : loserBets.entrySet()) {
                if (smartBot != null && smartBot.isBot(entry.getKey())) continue;
                aiDataManager.recordPlayerResult(entry.getKey(), false, entry.getValue(), 0);
            }
        }

        Map<UUID, Long> loserBets2 = isTai ? this.session.getXiuBets() : this.session.getTaiBets();
        for (Map.Entry<UUID, Long> entry : loserBets2.entrySet()) {
            this.statsManager.get(entry.getKey()).addLoss(entry.getValue());
            PersonalHistory.recordBet(entry.getKey(), this.session.getSessionId(), !isTai, entry.getValue(), false, 0L, false);
        }
        this.statsManager.saveAsync();

        long losers = isTai ? xiuTotal : taiTotal;
        this.huAmount += (long)((double)losers * this.houseEdge);

        DecimalFormat df = this.curFmt.get();
        String dice = BossBarManager.getDice(d1) + " + " + BossBarManager.getDice(d2) + " + " + BossBarManager.getDice(d3);
        String resultStr = isTai ? "§aTài" : "§cXỉu";
        double multiplier = this.getPayoutMultiplier(isTai);
        String multiplierStr = String.format("%.2f", multiplier);

        String msg = this.msgManager.getMsgKetQua().replace("{dice}", dice).replace("{total}", String.valueOf(total)).replace("{result}", resultStr);
        String payoutMsg = this.msgManager.getMsgPayoutInfo().replace("{multiplier}", multiplierStr).replace("{tai_total}", df.format(taiTotal) + this.cur).replace("{xiu_total}", df.format(xiuTotal) + this.cur);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
            if (this.balancedPayout) p.sendMessage(payoutMsg);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            // === AI: Smart bonus for losing streak players ===
            if (aiEngine != null && smartBot != null && !smartBot.isBot(p.getUniqueId())) {
                if (aiEngine.shouldGiveBonus(p.getUniqueId())) {
                    long bonus = aiEngine.getSmartBonusAmount();
                    this.eco.deposit(p.getUniqueId(), bonus);
                    p.sendMessage("§d🎁 [AI] Bạn đang đen quá! Thưởng an ủi: §e" + df.format(bonus) + " " + this.cur);
                }
            }
        }

        if (jp && jpName != null) {
            String jmsg = this.msgManager.getMsgJP().replace("{player}", jpName).replace("{amount}", df.format(this.jpAmount));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(jmsg);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        this.history.addFirst(new GameSession.SessionResult(this.session.getSessionId(), this.session.getDiceResults(), isTai, taiTotal, xiuTotal, jp, jpName));
        while (this.history.size() > 100) this.history.removeLast();

        this.discord.sendSessionResult(this.history.peekFirst());
        this.savePersistentData();
        this.boss.removeAll();

        // === AI: Auto-inject fund if needed ===
        if (aiEngine != null && aiEngine.shouldInjectFund(this.huAmount)) {
            long inject = aiEngine.getInjectAmount();
            this.huAmount += inject;
            this.plugin.getLogger().info("[AI] Auto-injected " + inject + " to fund. New fund: " + this.huAmount);
        }

        // === AI: Economy alert ===
        if (aiEngine != null) {
            AIEngine.EconomyStatus ecoStatus = aiEngine.checkEconomy();
            if (ecoStatus.needsAction) {
                this.plugin.getLogger().warning("[AI] Economy alert: " + ecoStatus.message);
                this.plugin.getLogger().warning("[AI Economy Alert] " + ecoStatus.message);
            }
        }

        new BukkitRunnable() {
            public void run() {
                GameManager.this.startNewSession();
            }
        }.runTaskLater(this.plugin, 60L);
    }

    /**
     * Calculate payout for a winner (used for AI tracking)
     */
    private long calculatePayout(long bet, Map<UUID, Long> winnerBets, Map<UUID, Long> loserBets) {
        long winnerTotal = winnerBets.values().stream().mapToLong(Long::longValue).sum();
        long loserTotal = loserBets.values().stream().mapToLong(Long::longValue).sum();
        if (this.balancedPayout && loserTotal > 0 && winnerTotal > 0) {
            long netPot = (long)((double)loserTotal * (1.0 - this.houseEdge));
            long share = (long)((double)bet / (double)winnerTotal * (double)netPot);
            return bet + share;
        }
        return bet * 2;
    }

    private void payWinners(Map<UUID, Long> winnerBets, Map<UUID, Long> loserBets) {
        if (winnerBets.isEmpty()) return;
        DecimalFormat df = this.curFmt.get();
        long winnerTotal = winnerBets.values().stream().mapToLong(Long::longValue).sum();
        long loserTotal = loserBets.values().stream().mapToLong(Long::longValue).sum();

        for (Map.Entry<UUID, Long> e : winnerBets.entrySet()) {
            long payout;
            UUID uuid = e.getKey();
            long bet = e.getValue();

            if (this.balancedPayout && loserTotal > 0 && winnerTotal > 0) {
                long netPot = (long)((double)loserTotal * (1.0 - this.houseEdge));
                long share = (long)((double)bet / (double)winnerTotal * (double)netPot);
                payout = bet + share;
            } else {
                payout = bet * 2;
            }

            boolean ok = this.eco.deposit(uuid, payout);
            Player p = Bukkit.getPlayer(uuid);

            // Skip bot messages
            if (smartBot != null && smartBot.isBot(uuid)) continue;

            if (p != null && ok) {
                p.sendMessage(this.msgManager.getMsgThang().replace("{amount}", df.format(payout)).replace("{currency}", this.cur));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            this.statsManager.get(uuid).addWin(payout);
            PersonalHistory.recordBet(uuid, this.session.getSessionId(), winnerBets == this.session.getTaiBets(), bet, true, payout, false);
        }
    }

    public boolean placeBet(Player player, boolean isTai, long amount) {
        this.betLock.lock();
        try {
            if (this.session == null || this.session.isFinished() || this.rolling) {
                player.sendMessage(this.msgManager.getMsgNoSession());
                return false;
            }
            UUID uuid = player.getUniqueId();
            int side = this.session.getPlayerSide(uuid);
            if (side == 0 && !isTai) {
                player.sendMessage(this.msgManager.getMsgDaDatTai());
                return false;
            }
            if (side == 1 && isTai) {
                player.sendMessage(this.msgManager.getMsgDaDatXiu());
                return false;
            }

            // === AI: Dynamic max bet per player ===
            long playerMaxBet = this.maxBet;
            if (aiEngine != null && aiEngine.getConfig().isEnabled() && aiEngine.getConfig().isDynamicBetLimitEnabled()) {
                playerMaxBet = aiEngine.calculateMaxBet(uuid, this.maxBet);
            }

            if (amount < this.minBet || amount > playerMaxBet) {
                DecimalFormat df = this.curFmt.get();
                player.sendMessage(this.msgManager.getMsgLimit().replace("{min}", df.format(this.minBet)).replace("{max}", df.format(playerMaxBet)));
                return false;
            }

            if (!this.eco.withdraw(uuid, amount)) {
                player.sendMessage(this.msgManager.getMsgNoMoney());
                return false;
            }
            this.statsManager.get(uuid).addWagered(amount);
            this.statsManager.get(uuid).setLastSide(isTai ? "tai" : "xiu");
            DecimalFormat df = this.curFmt.get();
            if (isTai) {
                this.session.addTaiBet(uuid, amount);
                player.sendMessage(this.msgManager.getMsgTai().replace("{amount}", df.format(amount) + " " + this.cur));
            } else {
                this.session.addXiuBet(uuid, amount);
                player.sendMessage(this.msgManager.getMsgXiu().replace("{amount}", df.format(amount) + " " + this.cur));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            return true;
        } finally {
            this.betLock.unlock();
        }
    }

    public void shutdown() {
        if (this.timer != null) this.timer.cancel();
        if (this.autoSaveTask != null) this.autoSaveTask.cancel();
        if (this.session != null && !this.session.isFinished()) {
            this.refundAllBets(this.session.getTaiBets());
            this.refundAllBets(this.session.getXiuBets());
        }
        this.boss.removeAll();
        this.statsManager.save();
        this.savePersistentData();
        // Save AI data
        if (this.aiDataManager != null) this.aiDataManager.save();
    }

    private void savePersistentData() {
        try {
            File configFile = new File(this.plugin.getDataFolder(), "config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("current-hu", this.huAmount);
            config.set("current-session", this.sessionCounter);
            config.save(configFile);
        } catch (Exception e) {
            this.plugin.getLogger().warning("[NguyenXiuTai] Failed to save: " + e.getMessage());
        }
    }

    private void refundAllBets(Map<UUID, Long> bets) {
        DecimalFormat df = this.curFmt.get();
        for (Map.Entry<UUID, Long> e : bets.entrySet()) {
            UUID uuid = e.getKey();
            long amount = e.getValue();
            boolean ok = this.eco.deposit(uuid, amount);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && ok) {
                p.sendMessage(this.msgManager.getMsgHoanTien().replace("{amount}", df.format(amount)).replace("{currency}", this.cur));
            }
        }
    }

    private int rnd() {
        return ThreadLocalRandom.current().nextInt(1, 7);
    }

    public static long parseAmount(String input) {
        input = input.trim().toUpperCase().replace(",", "").replace(".", "");
        double m = 1.0;
        if (input.endsWith("T")) { m = 1.0E12; input = input.substring(0, input.length() - 1); }
        else if (input.endsWith("B")) { m = 1.0E9; input = input.substring(0, input.length() - 1); }
        else if (input.endsWith("M")) { m = 1000000.0; input = input.substring(0, input.length() - 1); }
        else if (input.endsWith("K")) { m = 1000.0; input = input.substring(0, input.length() - 1); }
        try { return (long)(Double.parseDouble(input) * m); }
        catch (NumberFormatException e) { return -1; }
    }

    public double getPayoutMultiplier(boolean isTai) {
        if (!this.balancedPayout) return 2.0;
        if (this.session == null) return 2.0;
        long winnerTotal = isTai ? this.session.getTaiTotal() : this.session.getXiuTotal();
        long loserTotal = isTai ? this.session.getXiuTotal() : this.session.getTaiTotal();
        if (winnerTotal <= 0 || loserTotal <= 0) return 2.0;
        return 1.0 + (double)loserTotal * (1.0 - this.houseEdge) / (double)winnerTotal;
    }

    // === Getters ===
    public GameSession getCurrentSession() { return this.session; }
    public boolean isRolling() { return this.rolling; }
    public long getMinBet() { return this.minBet; }
    public long getMaxBet() { return this.maxBet; }
    public DecimalFormat getCurrencyFormat() { return this.curFmt.get(); }
    public String getCurrencySymbol() { return this.cur; }
    public int getTimeLeft() { return this.timeLeft; }
    public int getBetTime() { return this.betTime; }
    public int getRollTime() { return this.rollTime; }
    public String getMsgNhap() { return this.msgManager.getMsgNhap(); }
    public String getMsgHuy() { return this.msgManager.getMsgHuy(); }
    public ConcurrentLinkedDeque<GameSession.SessionResult> getHistory() { return this.history; }
    public MessageManager getMessageManager() { return this.msgManager; }
    public StatsManager getStatsManager() { return this.statsManager; }
    public DailyBonusManager getDailyBonusManager() { return this.dailyBonusManager; }
    public EconomyManager getEconomyManager() { return this.eco; }
    public AIEngine getAiEngine() { return this.aiEngine; }
}
