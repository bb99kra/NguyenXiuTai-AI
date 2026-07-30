package nguyenxiutai.ai;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AI Command handler - delegated from TaiXiuCommand router
 * /taixiu ai status|streak|predict|player|economy|bots|reload|heatmap
 */
public class AICommand {

    private final AIEngine engine;
    private final SmartBot botSystem;
    private final DecimalFormat df = new DecimalFormat("#,###");

    public AICommand(AIEngine engine, SmartBot botSystem) {
        this.engine = engine;
        this.botSystem = botSystem;
    }

    /**
     * Handle /taixiu ai <subcommand> ...
     * Called from TaiXiuCommand router (args[0]="ai", args[1]=sub, ...)
     */
    public boolean handleCommand(CommandSender sender, String[] args) {
        boolean isPlayer = sender instanceof Player;

        if (args.length < 2) {
            showHelp(sender);
            return true;
        }

        String sub = args[1].toLowerCase();

        // Public commands — any player can view
        switch (sub) {
            case "status":
                showStatus(sender);
                return true;
            case "streak":
                if (!isPlayer) { sender.sendMessage("§cChỉ người chơi mới xem được!"); return true; }
                showStreak((Player) sender);
                return true;
            case "predict":
                if (!isPlayer) { sender.sendMessage("§cChỉ người chơi mới xem được!"); return true; }
                showPrediction((Player) sender);
                return true;
            case "heatmap":
                sender.sendMessage("§eSử dụng: /cau để xem heatmap");
                return true;
        }

        // Admin commands — require permission
        if (!sender.hasPermission("nguyenxiutai.admin")) {
            sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }

        switch (sub) {
            case "player":
                if (!isPlayer) { sender.sendMessage("§cChỉ người chơi mới xem được!"); return true; }
                if (args.length >= 3) {
                    showPlayerAnalysis((Player) sender, args[2]);
                } else {
                    showPlayerAnalysis((Player) sender, sender.getName());
                }
                break;
            case "economy":
                showEconomy(sender);
                break;
            case "bots":
                showBots(sender);
                break;
            case "reload":
                reloadConfig();
                sender.sendMessage("§a✅ Đã reload AI config!");
                break;
            default:
                showHelp(sender);
        }
        return true;
    }

    public void reloadConfig() {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("NguyenXiuTai");
        if (plugin != null) {
            engine.getConfig().load(plugin);
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§e§l  🤖 NguyenXiuTai AI Commands");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§a/taixiu ai status §7- Xem trạng thái AI");
        sender.sendMessage("§a/taixiu ai streak §7- Xem cầu hiện tại");
        sender.sendMessage("§a/taixiu ai predict §7- Dự đoán phiên sau");
        sender.sendMessage("§a/taixiu ai player [name] §7- Phân tích người chơi");
        sender.sendMessage("§a/taixiu ai economy §7- Xem kinh tế server");
        sender.sendMessage("§a/taixiu ai bots §7- Xem danh sách bot");
        sender.sendMessage("§a/taixiu ai reload §7- Reload config AI");
        sender.sendMessage("§a/taixiu ai heatmap §7- Xem heatmap");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showStatus(CommandSender sender) {
        AIConfig cfg = engine.getConfig();
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§e§l  🤖 AI Status");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§7AI Enabled: " + (cfg.isEnabled() ? "§a✅" : "§c❌"));
        sender.sendMessage("§7Dynamic Ratio: " + (cfg.isDynamicRatioEnabled() ? "§a✅" : "§c❌"));
        sender.sendMessage("§7Dynamic House Edge: " + (cfg.isDynamicHouseEdgeEnabled() ? "§a✅" : "§c❌"));
        sender.sendMessage("§7Dynamic Bet Limit: " + (cfg.isDynamicBetLimitEnabled() ? "§a✅" : "§c❌"));
        sender.sendMessage("§7Streak Detection: " + (cfg.isStreakDetectionEnabled() ? "§a✅" : "§c❌"));
        sender.sendMessage("§7Prediction: " + (cfg.isPredictionEnabled() ? "§a✅" : "§c❌"));
        sender.sendMessage("§7Smart Bot: " + (cfg.isBotEnabled() ? "§a✅ (" + botSystem.getBotCount() + " bots)" : "§c❌"));
        sender.sendMessage("§7Economy Protection: " + (cfg.isEconomyProtectionEnabled() ? "§a✅" : "§c❌"));
        sender.sendMessage("§7Smart Bonus: " + (cfg.isSmartBonusEnabled() ? "§a✅" : "§c❌"));
        sender.sendMessage("§eCurrent Tai Ratio: §f" + String.format("%.1f%%", engine.getCurrentTaiRatio() * 100));
        sender.sendMessage("§eCurrent House Edge: §f" + String.format("%.1f%%", engine.getCurrentHouseEdge() * 100));
        sender.sendMessage("§7History: §f" + engine.getDataManager().getHistorySize() + " sessions");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showStreak(Player player) {
        AIEngine.StreakInfo streak = engine.getStreakInfo();
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l  📊 Cầu hiện tại");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (streak.count >= 1) {
            String side = streak.side ? "§aTài" : "§cXỉu";
            player.sendMessage("§7Cầu đang ra: " + side + " §7x" + streak.count);

            StringBuilder bar = new StringBuilder("§7[");
            for (int i = 0; i < Math.min(streak.count, 15); i++) {
                bar.append(streak.side ? "§a■" : "§c■");
            }
            bar.append("§7]");
            player.sendMessage(bar.toString());

            if (streak.alert) {
                player.sendMessage("§c⚠️ CẢNH BÁO: Cầu dài bất thường!");
            }
        } else {
            player.sendMessage("§7Không có cầu đặc biệt");
        }

        boolean[] recent = engine.getDataManager().getRecentResults(20);
        if (recent.length > 0) {
            StringBuilder history = new StringBuilder("§7Lịch sử: ");
            for (boolean b : recent) {
                history.append(b ? "§aT" : "§cX");
            }
            player.sendMessage(history.toString());
        }

        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showPrediction(Player player) {
        AIEngine.Prediction pred = engine.getPrediction();
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l  🔮 Dự đoán phiên sau");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§aTài: §f" + String.format("%.0f%%", pred.taiProb * 100) + " §7| §cXỉu: §f" + String.format("%.0f%%", pred.xiuProb * 100));
        player.sendMessage("§7" + pred.analysis);

        int taiBar = (int)(pred.taiProb * 20);
        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < 20; i++) {
            bar.append(i < taiBar ? "§a▓" : "§c▓");
        }
        bar.append("§7]");
        player.sendMessage(bar.toString());
        player.sendMessage("§8(Chỉ mang tính tham khảo)");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showPlayerAnalysis(Player player, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage("§cKhông tìm thấy người chơi: " + targetName);
            return;
        }

        AIEngine.PlayerAnalysis analysis = engine.getPlayerAnalysis(target.getUniqueId());
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l  👤 Phân tích: " + target.getName());
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7" + analysis.summary);
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showEconomy(CommandSender sender) {
        AIEngine.EconomyStatus status = engine.checkEconomy();
        AIDataManager.DailyStats ds = engine.getDataManager().getTodayStats();

        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§e§l  💰 Kinh tế hôm nay");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§7Tổng phiên: §f" + ds.totalSessions);
        sender.sendMessage("§7Tài: §a" + ds.taiCount + " §7| Xỉu: §c" + ds.xiuCount);
        sender.sendMessage("§7Tổng cược Tài: §f" + df.format(ds.totalTaiWagered));
        sender.sendMessage("§7Tổng cược Xỉu: §f" + df.format(ds.totalXiuWagered));
        sender.sendMessage("§7Lãi/lỗ người chơi: §f" + df.format(ds.totalPlayerProfit));

        if (status.needsAction) {
            sender.sendMessage("§c⚠️ " + status.message);
        } else {
            sender.sendMessage("§a✅ Kinh tế ổn định");
        }
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showBots(CommandSender sender) {
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§e§l  🤖 Smart Bots");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        for (String name : botSystem.getBotNames()) {
            sender.sendMessage("§7• §f" + name);
        }
        sender.sendMessage("§7Tổng: §f" + botSystem.getBotCount() + " bots");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
