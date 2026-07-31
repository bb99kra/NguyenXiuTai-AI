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

import nguyenxiutai.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class XiuCommand
implements CommandExecutor {
    private final GameManager gameManager;

    public XiuCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ch\u1ec9 ng\u01b0\u1eddi ch\u01a1i m\u1edbi d\u00f9ng l\u1ec7nh n\u00e0y!");
            return true;
        }
        Player p = (Player)sender;
        if (args.length < 1) {
            p.sendMessage("\u00a7eS\u1eed d\u1ee5ng: /xiu <s\u1ed1 ti\u1ec1n> (10k, 1M)");
            return true;
        }
        long amount = GameManager.parseAmount(args[0]);
        if (amount <= 0L) {
            p.sendMessage("\u00a7cS\u1ed1 ti\u1ec1n kh\u00f4ng h\u1ee3p l\u1ec7! (10k, 1M, 500k...)");
            return true;
        }
        this.gameManager.placeBet(p, false, amount);
        return true;
    }
}

