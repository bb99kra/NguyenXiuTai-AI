/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package nguyenxiutai.command;

import nguyenxiutai.gui.CauGUI;
import nguyenxiutai.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CauCommand
implements CommandExecutor {
    private final GameManager gameManager;

    public CauCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ch\u1ec9 ng\u01b0\u1eddi ch\u01a1i m\u1edbi d\u00f9ng l\u1ec7nh n\u00e0y!");
            return true;
        }
        Player p = (Player)sender;
        CauGUI.open(p, this.gameManager);
        return true;
    }
}

