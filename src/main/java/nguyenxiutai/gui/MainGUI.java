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
import nguyenxiutai.manager.GameManager;
import nguyenxiutai.manager.MessageManager;
import nguyenxiutai.model.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MainGUI {
    private static final ThreadLocal<DecimalFormat> FMT = ThreadLocal.withInitial(() -> new DecimalFormat("#,###"));
    public static final int LEADERBOARD_SLOT = 20;
    public static final int HISTORY_SLOT = 21;
    public static final int DAILY_SLOT = 23;
    public static final int BET_GUI_SLOT = 24;

    public static void open(Player player, GameManager gm) {
        MessageManager msg = gm.getMessageManager();
        String title = msg.getGuiMainTitle();
        Inventory inv = Bukkit.createInventory(null, (int)27, (String)title);
        GameSession s = gm.getCurrentSession();
        String c = gm.getCurrencySymbol();
        DecimalFormat fmt = FMT.get();
        String infoName = msg.getGuiMainInfoName().replace("{session}", String.valueOf(s.getSessionId()));
        ArrayList<String> infoLore = new ArrayList<String>();
        for (String line : msg.getGuiMainInfoLore()) {
            infoLore.add(line.replace("{total}", fmt.format(s.getTaiTotal() + s.getXiuTotal())).replace("{hu}", fmt.format(s.getHuAmount())).replace("{time}", String.valueOf(gm.getTimeLeft())).replace("{currency}", c));
        }
        inv.setItem(4, MainGUI.make(Material.NETHER_STAR, infoName, infoLore));
        String taiName = msg.getGuiMainTaiName();
        ArrayList<String> taiLore = new ArrayList<String>();
        for (String line : msg.getGuiMainTaiLore()) {
            taiLore.add(line.replace("{amount}", fmt.format(s.getTaiTotal())).replace("{count}", String.valueOf(s.getTaiBets().size())).replace("{currency}", c));
        }
        inv.setItem(11, MainGUI.make(Material.EMERALD, taiName, taiLore));
        String xiuName = msg.getGuiMainXiuName();
        ArrayList<String> xiuLore = new ArrayList<String>();
        for (String line : msg.getGuiMainXiuLore()) {
            xiuLore.add(line.replace("{amount}", fmt.format(s.getXiuTotal())).replace("{count}", String.valueOf(s.getXiuBets().size())).replace("{currency}", c));
        }
        inv.setItem(15, MainGUI.make(Material.DIAMOND, xiuName, xiuLore));
        ArrayList<String> lbLore = new ArrayList<String>();
        lbLore.add("\u00a77Xem top players");
        lbLore.add("\u00a77Th\u1eafng, t\u1ef7 l\u1ec7, c\u01b0\u1ee3c");
        lbLore.add("");
        lbLore.add("\u00a7e\u25b6 Nh\u1ea5n \u0111\u1ec3 xem");
        inv.setItem(20, MainGUI.make(Material.DIAMOND_SWORD, "\u00a7b\u00a7lB\u1ea3ng x\u1ebfp h\u1ea1ng", lbLore));
        ArrayList<String> histLore = new ArrayList<String>();
        histLore.add("\u00a77Xem l\u1ecbch s\u1efd c\u01b0\u1ee3c c\u00e1 nh\u00e2n");
        histLore.add("\u00a77Th\u1eafng/thua, l\u00e3i/l\u1ed7");
        histLore.add("");
        histLore.add("\u00a7e\u25b6 Nh\u1ea5n \u0111\u1ec3 xem");
        inv.setItem(21, MainGUI.make(Material.BOOK, "\u00a76\u00a7lL\u1ecbch s\u1efd c\u00e1 nh\u00e2n", histLore));
        boolean canClaim = gm.getDailyBonusManager() != null && gm.getDailyBonusManager().canClaim(player);
        Material dailyMat = canClaim ? Material.GOLD_BLOCK : Material.BEDROCK;
        String dailyName = canClaim ? "\u00a7a\u00a7lNh\u1eadn th\u01b0\u1edfng h\u00e0ng ng\u00e0y" : "\u00a77\u00a7l\u0110\u00e3 nh\u1eadn h\u00f4m nay";
        ArrayList<String> dailyLore = new ArrayList<String>();
        if (gm.getDailyBonusManager() != null) {
            int streak = gm.getDailyBonusManager().getStreak(player);
            long bonus = gm.getDailyBonusManager().getBonusForStreak(streak);
            dailyLore.add("\u00a77Th\u01b0\u1edfng: \u00a7e" + fmt.format(bonus) + " " + c);
            dailyLore.add("\u00a77Streak: \u00a7e" + streak + " ng\u00e0y");
            if (streak >= gm.getDailyBonusManager().getStreakMultiplierDays()) {
                dailyLore.add("\u00a7a\u2714 Bonus x" + String.format("%.1f", gm.getDailyBonusManager().getStreakMultiplier()));
            }
            dailyLore.add("");
            if (canClaim) {
                dailyLore.add("\u00a7a\u25b6 Nh\u1ea5n \u0111\u1ec3 nh\u1eadn");
            } else {
                dailyLore.add("\u00a77Quay l\u1ea1i ng\u00e0y mai!");
            }
        }
        inv.setItem(23, MainGUI.make(dailyMat, dailyName, dailyLore));
        ArrayList<String> betLore = new ArrayList<String>();
        betLore.add("\u00a77M\u1edf GUI ch\u1ecdn ti\u1ec1n");
        betLore.add("\u00a77Click \u0111\u1ec3 \u0111\u1eb7t c\u01b0\u1ee3c nhanh");
        betLore.add("");
        betLore.add("\u00a7e\u25b6 Nh\u1ea5n \u0111\u1ec3 m\u1edf");
        inv.setItem(24, MainGUI.make(Material.GOLD_INGOT, "\u00a76\u00a7l\u0110\u1eb7t c\u01b0\u1ee3c nhanh", betLore));
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

