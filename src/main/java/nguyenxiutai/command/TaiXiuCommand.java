/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.java.JavaPlugin
 */
package nguyenxiutai.command;

import nguyenxiutai.gui.MainGUI;
import nguyenxiutai.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TaiXiuCommand
implements CommandExecutor {
    private final GameManager gameManager;
    private final JavaPlugin plugin;

    public TaiXiuCommand(GameManager gameManager, JavaPlugin plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nguyenxiutai.admin")) {
                sender.sendMessage("\u00a7cB\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n!");
                return true;
            }
            this.gameManager.loadConfig();
            sender.sendMessage("\u00a7aNguyenXiuTai - Reload th\u00e0nh c\u00f4ng!");
            this.plugin.getLogger().info("[NguyenXiuTai] Reloaded by " + sender.getName());
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ch\u1ec9 ng\u01b0\u1eddi ch\u01a1i m\u1edbi d\u00f9ng l\u1ec7nh n\u00e0y!");
            return true;
        }
        Player p = (Player)sender;
        MainGUI.open(p, this.gameManager);
        return true;
    }
}

