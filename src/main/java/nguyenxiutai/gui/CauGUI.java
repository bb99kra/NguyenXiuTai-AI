/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package nguyenxiutai.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import nguyenxiutai.manager.BossBarManager;
import nguyenxiutai.manager.GameManager;
import nguyenxiutai.manager.MessageManager;
import nguyenxiutai.model.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CauGUI {
    private static final ThreadLocal<DecimalFormat> FMT = ThreadLocal.withInitial(() -> new DecimalFormat("#,###"));

    public static void open(Player player, GameManager gm) {
        MessageManager msg = gm.getMessageManager();
        String title = msg.getGuiCauTitle();
        Inventory inv = Bukkit.createInventory(null, (int)54, (String)title);
        inv.setItem(4, CauGUI.make(Material.NETHER_STAR, msg.getGuiCauHeaderName(), msg.getGuiCauHeaderLore()));
        ArrayList<GameSession.SessionResult> history = new ArrayList<GameSession.SessionResult>(gm.getHistory());
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int max = Math.min(history.size(), slots.length);
        DecimalFormat fmt = FMT.get();
        for (int i = 0; i < max; ++i) {
            ItemStack item;
            ItemMeta meta;
            GameSession.SessionResult r = (GameSession.SessionResult)history.get(i);
            int[] d = r.getDice();
            boolean isTai = r.isTai();
            Material mat = isTai ? Material.EMERALD : Material.DIAMOND;
            String side = isTai ? msg.getGuiCauTaiLabel() : msg.getGuiCauXiuLabel();
            String diceStr = BossBarManager.getDice(d[0]) + " + " + BossBarManager.getDice(d[1]) + " + " + BossBarManager.getDice(d[2]);
            ArrayList<String> lore = new ArrayList<>();
            lore.add("\u00a77" + diceStr + " = \u00a7e" + r.getDiceTotal());
            lore.add("\u00a77T\u00e0i: \u00a7a" + fmt.format(r.getTaiTotal()) + " " + gm.getCurrencySymbol());
            lore.add("\u00a77X\u1ec9u: \u00a7c" + fmt.format(r.getXiuTotal()) + " " + gm.getCurrencySymbol());
            if (r.isJackpot()) {
                lore.add("");
                lore.add("\u00a76\u2605 N\u1ed5 h\u0169! " + r.getJackpotPlayer());
            }
            if ((meta = (item = new ItemStack(mat)).getItemMeta()) != null) {
                meta.setDisplayName("\u00a7ePhi\u00ean #" + r.getSessionId() + side);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }
        long taiCount = history.stream().filter(GameSession.SessionResult::isTai).count();
        long xiuCount = (long)history.size() - taiCount;
        ArrayList<String> statsLore = new ArrayList<String>();
        statsLore.add("\u00a77T\u1ed5ng: \u00a7e" + history.size());
        statsLore.add("\u00a7aT\u00e0i: " + taiCount + " (" + (history.isEmpty() ? 0L : taiCount * 100L / (long)history.size()) + "%)");
        statsLore.add("\u00a7cX\u1ec9u: " + xiuCount + " (" + (history.isEmpty() ? 0L : xiuCount * 100L / (long)history.size()) + "%)");
        inv.setItem(49, CauGUI.make(Material.PAPER, msg.getGuiCauStatsName(), statsLore));
        inv.setItem(48, CauGUI.make(Material.ARROW, msg.getGuiCauBackName(), null));
        player.openInventory(inv);
    }

    private static ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                m.setLore(lore);
            }
            item.setItemMeta(m);
        }
        return item;
    }
}

