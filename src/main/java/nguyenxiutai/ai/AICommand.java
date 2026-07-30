package nguyenxiutai.ai;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * AI Command - /taixiu ai [subcommand]
 * Shows AI analysis, predictions, streak info
 */
public class AICommand implements CommandExecutor {

    private final AIEngine engine;
    private final SmartBot botSystem;
    private final DecimalFormat df = new DecimalFormat("#,###");

    public AICommand(AIEngine engine, SmartBot botSystem) {
        this.engine = engine;
        this.botSystem = botSystem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nguyenxiutai.admin")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }

        if (args.length < 2) {
            showHelp(player);
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "status":
                showStatus(player);
                break;
            case "streak":
                showStreak(player);
                break;
            case "predict":
                showPrediction(player);
                break;
            case "player":
                if (args.length >= 3) {
                    showPlayerAnalysis(player, args[2]);
                } else {
                    showPlayerAnalysis(player, player.getName());
                }
                break;
            case "economy":
                showEconomy(player);
                break;
            case "bots":
                showBots(player);
                break;
            case "reload":
                engine.getConfig().load((org.bukkit.plugin.java.JavaPlugin) Bukkit.getPluginManager().getPlugin("NguyenXiuTai"));
                player.sendMessage("§a✅ Đã reload AI config!");
                break;
            default:
                showHelp(player);
        }
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l  🤖 NguyenXiuTai AI Commands");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§a/taixiu ai status §7- Xem trạng thái AI");
        player.sendMessage("§a/taixiu ai streak §7- Xem cầu hiện tại");
        player.sendMessage("§a/taixiu ai predict §7- Dự đoán phiên sau");
        player.sendMessage("§a/taixiu ai player [name] §7- Phân tích người chơi");
        player.sendMessage("§a/taixiu ai economy §7- Xem kinh tế server");
        player.sendMessage("§a/taixiu ai bots §7- Xem danh sách bot");
        player.sendMessage("§a/taixiu ai reload §7- Reload config AI");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showStatus(Player player) {
        AIConfig cfg = engine.getConfig();
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l  🤖 AI Status");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7AI Enabled: " + (cfg.isEnabled() ? "§a✅" : "§c❌"));
        player.sendMessage("§7Dynamic Ratio: " + (cfg.isDynamicRatioEnabled() ? "§a✅" : "§c❌"));
        player.sendMessage("§7Dynamic House Edge: " + (cfg.isDynamicHouseEdgeEnabled() ? "§a✅" : "§c❌"));
        player.sendMessage("§7Dynamic Bet Limit: " + (cfg.isDynamicBetLimitEnabled() ? "§a✅" : "§c❌"));
        player.sendMessage("§7Streak Detection: " + (cfg.isStreakDetectionEnabled() ? "§a✅" : "§c❌"));
        player.sendMessage("§7Prediction: " + (cfg.isPredictionEnabled() ? "§a✅" : "§c❌"));
        player.sendMessage("§7Smart Bot: " + (cfg.isBotEnabled() ? "§a✅ (" + botSystem.getBotCount() + " bots)" : "§c❌"));
        player.sendMessage("§7Economy Protection: " + (cfg.isEconomyProtectionEnabled() ? "§a✅" : "§c❌"));
        player.sendMessage("§7Smart Bonus: " + (cfg.isSmartBonusEnabled() ? "§a✅" : "§c❌"));
        player.sendMessage("§eCurrent Tai Ratio: §f" + String.format("%.1f%%", engine.getCurrentTaiRatio() * 100));
        player.sendMessage("§eCurrent House Edge: §f" + String.format("%.1f%%", engine.getCurrentHouseEdge() * 100));
        player.sendMessage("§7History: §f" + engine.getDataManager().getHistorySize() + " sessions");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showStreak(Player player) {
        AIEngine.StreakInfo streak = engine.getStreakInfo();
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l  📊 Cầu hiện tại");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (streak.count >= 1) {
            String side = streak.side ? "§aTài" : "§cXỉu";
            player.sendMessage("§7Cầu đang ra: " + side + " §7x" + streak.count);

            // Visual streak bar
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

        // Show last 20 results
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

        // Visual bar
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

    private void showEconomy(Player player) {
        AIEngine.EconomyStatus status = engine.checkEconomy();
        AIDataManager.DailyStats ds = engine.getDataManager().getTodayStats();

        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l  💰 Kinh tế hôm nay");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7Tổng phiên: §f" + ds.totalSessions);
        player.sendMessage("§7Tài: §a" + ds.taiCount + " §7| Xỉu: §c" + ds.xiuCount);
        player.sendMessage("§7Tổng cược Tài: §f" + df.format(ds.totalTaiWagered));
        player.sendMessage("§7Tổng cược Xỉu: §f" + df.format(ds.totalXiuWagered));
        player.sendMessage("§7Lãi/lỗ người chơi: §f" + df.format(ds.totalPlayerProfit));

        if (status.needsAction) {
            player.sendMessage("§c⚠️ " + status.message);
        } else {
            player.sendMessage("§a✅ Kinh tế ổn định");
        }
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showBots(Player player) {
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l  🤖 Smart Bots");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        for (String name : botSystem.getBotNames()) {
            player.sendMessage("§7• §f" + name);
        }
        player.sendMessage("§7Tổng: §f" + botSystem.getBotCount() + " bots");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
