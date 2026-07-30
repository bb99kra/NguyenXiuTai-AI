package nguyenxiutai;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import nguyenxiutai.ai.AICommand;
import nguyenxiutai.ai.AIConfig;
import nguyenxiutai.ai.AIDataManager;
import nguyenxiutai.ai.AIEngine;
import nguyenxiutai.ai.HeatMapGUI;
import nguyenxiutai.ai.SmartBot;
import nguyenxiutai.command.CauCommand;
import nguyenxiutai.command.TaiCommand;
import nguyenxiutai.command.TaiXiuCommand;
import nguyenxiutai.command.XiuCommand;
import nguyenxiutai.gui.BetGUI;
import nguyenxiutai.gui.Leaderboard;
import nguyenxiutai.gui.MainGUI;
import nguyenxiutai.gui.PersonalHistory;
import nguyenxiutai.hook.NguyenXiuTaiExpansion;
import nguyenxiutai.manager.BossBarManager;
import nguyenxiutai.manager.DailyBonusManager;
import nguyenxiutai.manager.DiscordManager;
import nguyenxiutai.manager.EconomyManager;
import nguyenxiutai.manager.GameManager;
import nguyenxiutai.manager.MessageManager;
import nguyenxiutai.manager.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class NguyenXiuTaiPlugin extends JavaPlugin implements Listener {

    private GameManager gameManager;
    private BossBarManager bossBarManager;
    private StatsManager statsManager;
    private final Map<UUID, Boolean> waitingBet = new ConcurrentHashMap<>();
    // Anti-spam: track last bet message time per player
    private final Map<UUID, Long> lastBetMsgTime = new ConcurrentHashMap<>();
    private static final long BET_CHAT_COOLDOWN_MS = 1000; // 1 second debounce

    // AI Components
    private AIConfig aiConfig;
    private AIDataManager aiDataManager;
    private AIEngine aiEngine;
    private SmartBot smartBot;
    private HeatMapGUI heatMapGUI;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        // === Original components ===
        MessageManager msgManager = new MessageManager(this);
        msgManager.load();

        this.statsManager = new StatsManager(this);
        this.statsManager.load();

        EconomyManager eco = new EconomyManager();

        // === FIX #7: Check Vault/economy before proceeding ===
        if (!eco.setup()) {
            this.getLogger().severe("========================================");
            this.getLogger().severe("Vault not found! No economy provider.");
            this.getLogger().severe("This plugin requires Vault + an economy");
            this.getLogger().severe("plugin (e.g. EssentialsX). Disabling...");
            this.getLogger().severe("========================================");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.bossBarManager = new BossBarManager(this);
        this.bossBarManager.setMessageManager(msgManager);

        DiscordManager discord = new DiscordManager(this.getLogger(), this);
        discord.setMessageManager(msgManager);

        // === AI Components ===
        this.aiConfig = new AIConfig();
        this.aiConfig.load(this);

        this.aiDataManager = new AIDataManager(this);
        this.aiDataManager.load();

        this.aiEngine = new AIEngine(this.aiConfig, this.aiDataManager);

        this.smartBot = new SmartBot(this.aiConfig);
        if (this.aiConfig.isBotEnabled()) {
            this.smartBot.initBots();
            this.getLogger().info("[AI] Initialized " + this.smartBot.getBotCount() + " smart bots");
        }

        this.heatMapGUI = new HeatMapGUI(this.aiEngine);

        // === Game Manager with AI ===
        this.gameManager = new GameManager(this, eco, this.bossBarManager, discord, msgManager, this.statsManager);
        this.gameManager.setAI(this.aiEngine, this.smartBot, this.aiDataManager);

        this.gameManager.loadConfig();

        // === FIX #1: Single command executor with router ===
        TaiXiuCommand taiXiuCmd = new TaiXiuCommand(this.gameManager, this);
        AICommand aiCmd = new AICommand(this.aiEngine, this.smartBot);
        taiXiuCmd.setAICommand(aiCmd);

        this.getCommand("tai").setExecutor(new TaiCommand(this.gameManager));
        this.getCommand("xiu").setExecutor(new XiuCommand(this.gameManager));
        this.getCommand("taixiu").setExecutor(taiXiuCmd);
        this.getCommand("cau").setExecutor(new CauCommand(this.gameManager));

        this.getServer().getPluginManager().registerEvents(this, this);

        // HeatMap GUI click handler
        this.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onHeatMapClick(InventoryClickEvent e) {
                if (e.getView().getTitle().contains("AI Cầu Analysis")) {
                    e.setCancelled(true);
                }
            }
        }, this);

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NguyenXiuTaiExpansion(this.gameManager, this.statsManager).register();
            this.getLogger().info("[NguyenXiuTai] PlaceholderAPI hooked!");
        }

        this.gameManager.startNewSession();

        // AI status log
        if (this.aiConfig.isEnabled()) {
            this.getLogger().info("§a[AI] AI Engine enabled!");
            this.getLogger().info("§a[AI] - Dynamic Ratio: " + this.aiConfig.isDynamicRatioEnabled());
            this.getLogger().info("§a[AI] - Dynamic House Edge: " + this.aiConfig.isDynamicHouseEdgeEnabled());
            this.getLogger().info("§a[AI] - Smart Bot: " + this.aiConfig.isBotEnabled());
            this.getLogger().info("§a[AI] - Economy Protection: " + this.aiConfig.isEconomyProtectionEnabled());
        }

        this.getLogger().info("NguyenXiuTai v" + this.getDescription().getVersion() + " enabled! (AI Enhanced)");
    }

    @Override
    public void onDisable() {
        if (this.gameManager != null) {
            this.gameManager.shutdown();
        }
        if (this.aiDataManager != null) {
            this.aiDataManager.save();
        }
    }

    // === Getters for AI ===
    public AIEngine getAiEngine() { return aiEngine; }
    public SmartBot getSmartBot() { return smartBot; }
    public HeatMapGUI getHeatMapGUI() { return heatMapGUI; }

    // === Original Event Handlers ===

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (this.gameManager.getCurrentSession() != null && !this.gameManager.getCurrentSession().isFinished()) {
            this.bossBarManager.addPlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        this.bossBarManager.removePlayer(e.getPlayer().getUniqueId());
        this.waitingBet.remove(e.getPlayer().getUniqueId());
        // FIX #4: Cleanup selected amount on quit
        BetGUI.cleanupPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) return;
        Player p = (Player) humanEntity;
        if (e.getClickedInventory() == null) return;

        String title = e.getView().getTitle();

        if (title.equals(this.gameManager.getMessageManager().getGuiMainTitle())) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot < 0 || slot >= 27) return;

            if (slot == 11) {
                p.closeInventory();
                this.waitingBet.put(p.getUniqueId(), true);
                p.sendMessage(this.gameManager.getMsgNhap());
                return;
            }
            if (slot == 15) {
                p.closeInventory();
                this.waitingBet.put(p.getUniqueId(), false);
                p.sendMessage(this.gameManager.getMsgNhap());
                return;
            }
            if (slot == 20) {
                p.closeInventory();
                Leaderboard.open(p, this.gameManager, Leaderboard.Tab.WINS);
                return;
            }
            if (slot == 21) {
                p.closeInventory();
                PersonalHistory.open(p, this.gameManager);
                return;
            }
            if (slot == 23) {
                this.handleDailyBonus(p);
                return;
            }
            if (slot == 24) {
                p.closeInventory();
                BetGUI.open(p, this.gameManager);
                return;
            }
            return;
        }

        if (title.equals("§6§lTài Xỉu - Chọn tiền")) {
            e.setCancelled(true);
            BetGUI.handleClick(p, e.getRawSlot(), this.gameManager);
            return;
        }

        if (title.equals(this.gameManager.getMessageManager().getGuiCauTitle())) {
            e.setCancelled(true);
            if (e.getRawSlot() == 48) {
                p.closeInventory();
                MainGUI.open(p, this.gameManager);
            }
            return;
        }

        if (title.contains("Bảng xếp hạng")) {
            e.setCancelled(true);
            Leaderboard.handleClick(p, e.getRawSlot(), this.gameManager);
            return;
        }

        if (title.equals("§6§lLịch sử cược")) {
            e.setCancelled(true);
            PersonalHistory.handleClick(p, e.getRawSlot(), this.gameManager);
        }
    }

    private void handleDailyBonus(Player p) {
        DailyBonusManager dbm = this.gameManager.getDailyBonusManager();
        if (dbm == null) {
            p.sendMessage("§cDaily bonus không khả dụng!");
            return;
        }
        if (!dbm.canClaim(p)) {
            p.sendMessage("§cBạn đã nhận thưởng hôm nay rồi!");
            p.closeInventory();
            return;
        }
        // Calculate bonus first, deposit, THEN mark as claimed
        long bonus = dbm.calculateBonus(p);
        if (bonus <= 0) {
            p.closeInventory();
            return;
        }
        boolean ok = this.gameManager.getEconomyManager().deposit(p.getUniqueId(), bonus);
        if (!ok) {
            p.sendMessage("§cLỗi khi nhận thưởng! Thử lại sau.");
            p.closeInventory();
            return;
        }
        // Only mark claimed AFTER successful deposit
        dbm.markClaimed(p);
        String cur = this.gameManager.getCurrencySymbol();
        String formatted = this.gameManager.getCurrencyFormat().format(bonus);
        p.sendMessage("§a✅ Nhận thưởng hàng ngày: §e" + formatted + " " + cur);
        int streak = dbm.getStreak(p);
        p.sendMessage("§7Streak: §e" + streak + " ngày");
        if (streak >= dbm.getStreakMultiplierDays()) {
            p.sendMessage("§a✔ Bonus x" + String.format("%.1f", dbm.getStreakMultiplier()) + " streak!");
        }
        p.closeInventory();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!this.waitingBet.containsKey(uuid)) return;

        e.setCancelled(true);
        String msg = e.getMessage().trim();

        if (msg.equalsIgnoreCase("huy") || msg.equalsIgnoreCase("cancel")) {
            this.waitingBet.remove(uuid);
            p.sendMessage(this.gameManager.getMsgHuy());
            return;
        }

        // Anti-spam: debounce bet messages
        long now = System.currentTimeMillis();
        Long lastTime = lastBetMsgTime.get(uuid);
        if (lastTime != null && (now - lastTime) < BET_CHAT_COOLDOWN_MS) {
            p.sendMessage("§7Đợi chút...");
            return;
        }
        lastBetMsgTime.put(uuid, now);

        boolean isTai = this.waitingBet.remove(uuid);
        long amount = GameManager.parseAmount(msg);
        if (amount <= 0) {
            p.sendMessage("§cSố tiền không hợp lệ! (10k, 1M, 500k...)");
            return;
        }
        this.getServer().getScheduler().runTask(this, () -> this.gameManager.placeBet(p, isTai, amount));
    }
}
