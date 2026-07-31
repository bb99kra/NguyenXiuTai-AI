package nguyenxiutai.command;

import java.text.DecimalFormat;
import nguyenxiutai.ai.AICommand;
import nguyenxiutai.gui.MainGUI;
import nguyenxiutai.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Unified /taixiu command router
 * /taixiu              -> MainGUI
 * /taixiu reload       -> reload config
 * /taixiu ai <sub>     -> AI subcommands
 */
public class TaiXiuCommand implements CommandExecutor {

    private final GameManager gameManager;
    private final JavaPlugin plugin;
    private AICommand aiCommand;

    public TaiXiuCommand(GameManager gameManager, JavaPlugin plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    public void setAICommand(AICommand aiCommand) {
        this.aiCommand = aiCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /taixiu -> open MainGUI
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cChỉ người chơi mới dùng được lệnh này!");
                return true;
            }
            MainGUI.open((Player) sender, this.gameManager);
            return true;
        }

        // /taixiu reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nguyenxiutai.admin")) {
                sender.sendMessage("§cBạn không có quyền!");
                return true;
            }
            this.gameManager.loadConfig();
            // Reload AI config too
            if (this.aiCommand != null) {
                this.aiCommand.reloadConfig();
            }
            sender.sendMessage("§aNguyenXiuTai - Reload thành công!");
            this.plugin.getLogger().info("[NguyenXiuTai] Reloaded by " + sender.getName());
            return true;
        }

        // /taixiu ai <subcommand> ... -> delegate to AICommand
        if (args[0].equalsIgnoreCase("ai")) {
            if (this.aiCommand != null) {
                return this.aiCommand.handleCommand(sender, args);
            }
            sender.sendMessage("§cAI chưa được khởi tạo!");
            return true;
        }

        // Unknown subcommand -> show help or GUI
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cUsage: /taixiu [reload|ai <subcommand>]");
            return true;
        }
        MainGUI.open((Player) sender, this.gameManager);
        return true;
    }
}
