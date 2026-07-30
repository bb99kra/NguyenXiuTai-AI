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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nguyenxiutai.gui.MainGUI;
import nguyenxiutai.manager.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PersonalHistory {
    private static final Map<UUID, List<BetRecord>> playerHistory = new HashMap<UUID, List<BetRecord>>();
    private static final int MAX_HISTORY = 50;
    private static final int[] HISTORY_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int STATS_SLOT = 4;
    private static final int BACK_SLOT = 49;

    public static void recordBet(UUID uuid, int sessionId, boolean isTai, long amount, boolean won, long payout, boolean jackpot) {
        List<BetRecord> history = playerHistory.computeIfAbsent(uuid, k -> new ArrayList<>());
        history.add(0, new BetRecord(sessionId, isTai, amount, won, payout, jackpot));
        while (history.size() > 50) {
            history.remove(history.size() - 1);
        }
    }

    public static void open(Player player, GameManager gm) {
        UUID uuid = player.getUniqueId();
        DecimalFormat fmt = gm.getCurrencyFormat();
        String cur = gm.getCurrencySymbol();
        Inventory inv = Bukkit.createInventory(null, (int)54, (String)"\u00a76\u00a7lL\u1ecbch s\u1efd c\u01b0\u1ee3c");
        List<BetRecord> history = playerHistory.getOrDefault(uuid, new ArrayList<>());
        int totalBets = history.size();
        int wins = (int)history.stream().filter(r -> r.won).count();
        int losses = totalBets - wins;
        long totalWagered = history.stream().mapToLong(r -> r.amount).sum();
        long totalProfit = history.stream().mapToLong(r -> r.won ? r.payout - r.amount : -r.amount).sum();
        ArrayList<String> statsLore = new ArrayList<String>();
        statsLore.add("\u00a77T\u1ed5ng phi\u00ean: \u00a7e" + totalBets);
        statsLore.add("\u00a77Th\u1eafng: \u00a7a" + wins + " | Thua: \u00a7c" + losses);
        statsLore.add("\u00a77T\u1ef7 l\u1ec7: \u00a7e" + (totalBets > 0 ? String.format("%.1f%%", (double)wins / (double)totalBets * 100.0) : "0%"));
        statsLore.add("\u00a77T\u1ed5ng c\u01b0\u1ee3c: \u00a76" + fmt.format(totalWagered) + " " + cur);
        statsLore.add("");
        String profitColor = totalProfit >= 0L ? "\u00a7a" : "\u00a7c";
        String profitSign = totalProfit >= 0L ? "+" : "";
        statsLore.add(profitColor + profitSign + fmt.format(totalProfit) + " " + cur);
        inv.setItem(4, PersonalHistory.makeItem(Material.BOOK, "\u00a76\u00a7lTh\u1ed1ng k\u00ea c\u00e1 nh\u00e2n", statsLore));
        int limit = Math.min(history.size(), HISTORY_SLOTS.length);
        for (int i = 0; i < limit; ++i) {
            BetRecord record = (BetRecord)history.get(i);
            Material mat = record.jackpot ? Material.NETHER_STAR : (record.won ? (record.isTai ? Material.EMERALD : Material.DIAMOND) : Material.GRAY_DYE);
            String side = record.isTai ? "\u00a7aT\u00e0i" : "\u00a7cX\u1ec9u";
            String result = record.won ? "\u00a7a\u2714 Th\u1eafng" : "\u00a7c\u2716 Thua";
            ArrayList<String> lore = new ArrayList<String>();
            lore.add("\u00a77B\u00ean: " + side);
            lore.add("\u00a77C\u01b0\u1ee3c: \u00a7e" + fmt.format(record.amount) + " " + cur);
            lore.add("\u00a77K\u1ebft qu\u1ea3: " + result);
            if (record.won) {
                long profit = record.payout - record.amount;
                lore.add("\u00a77L\u00e3i: \u00a7a+" + fmt.format(profit) + " " + cur);
            } else {
                lore.add("\u00a77L\u1ed7: \u00a7c-" + fmt.format(record.amount) + " " + cur);
            }
            if (record.jackpot) {
                lore.add("");
                lore.add("\u00a76\u2605 N\u1ed5 H\u0169!");
            }
            String name = "\u00a7ePhi\u00ean #" + record.sessionId + " " + side;
            inv.setItem(HISTORY_SLOTS[i], PersonalHistory.makeItem(mat, name, lore));
        }
        if (history.isEmpty()) {
            inv.setItem(22, PersonalHistory.makeItem(Material.PAPER, "\u00a77Ch\u01b0a c\u00f3 l\u1ecbch s\u1efd", List.of("\u00a77H\u00e3y \u0111\u1eb7t c\u01b0\u1ee3c \u0111\u1ec3 b\u1eaft \u0111\u1ea7u!")));
        }
        inv.setItem(49, PersonalHistory.makeItem(Material.ARROW, "\u00a7e\u2190 Quay l\u1ea1i", null));
        player.openInventory(inv);
    }

    public static boolean handleClick(Player player, int slot, GameManager gm) {
        if (slot == 49) {
            MainGUI.open(player, gm);
            return true;
        }
        return false;
    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
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

    public static class BetRecord {
        public final int sessionId;
        public final boolean isTai;
        public final long amount;
        public final boolean won;
        public final long payout;
        public final boolean jackpot;

        public BetRecord(int sessionId, boolean isTai, long amount, boolean won, long payout, boolean jackpot) {
            this.sessionId = sessionId;
            this.isTai = isTai;
            this.amount = amount;
            this.won = won;
            this.payout = payout;
            this.jackpot = jackpot;
        }
    }
}

