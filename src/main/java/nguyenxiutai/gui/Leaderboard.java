package nguyenxiutai.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import nguyenxiutai.manager.GameManager;
import nguyenxiutai.manager.StatsManager;
import nguyenxiutai.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class Leaderboard {
    private static final int[] PLAYER_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30};

    public static void open(Player player, GameManager gm, Tab tab) {
        StatsManager stats = gm.getStatsManager();
        DecimalFormat fmt = gm.getCurrencyFormat();
        String cur = gm.getCurrencySymbol();

        String title;
        switch (tab) {
            case WINS: title = "§6§lBảng xếp hạng - Thắng nhiều"; break;
            case WINRATE: title = "§6§lBảng xếp hạng - Tỷ lệ thắng"; break;
            case WAGERED: title = "§6§lBảng xếp hạng - Cược nhiều"; break;
            default: title = "§6§lBảng xếp hạng"; break;
        }

        Inventory inv = Bukkit.createInventory(null, 54, title);
        inv.setItem(1, makeTabItem(Material.DIAMOND_SWORD, "§aThắng nhiều", tab == Tab.WINS));
        inv.setItem(4, makeTabItem(Material.GOLDEN_APPLE, "§eTỷ lệ thắng", tab == Tab.WINRATE));
        inv.setItem(7, makeTabItem(Material.GOLD_BLOCK, "§6Cược nhiều", tab == Tab.WAGERED));

        List<Map.Entry<UUID, PlayerStats>> sorted = getSorted(stats.getAll(), tab);
        int limit = Math.min(sorted.size(), PLAYER_SLOTS.length);
        for (int i = 0; i < limit; i++) {
            Map.Entry<UUID, PlayerStats> entry = sorted.get(i);
            UUID uuid = entry.getKey();
            PlayerStats ps = entry.getValue();

            Material mat;
            String prefix;
            switch (i) {
                case 0: mat = Material.DIAMOND_BLOCK; prefix = "§b§l#1 "; break;
                case 1: mat = Material.GOLD_BLOCK; prefix = "§6§l#2 "; break;
                case 2: mat = Material.IRON_BLOCK; prefix = "§f§l#3 "; break;
                default: mat = Material.PLAYER_HEAD; prefix = "§7#" + (i + 1) + " "; break;
            }

            String name = getPlayerName(uuid);
            ItemStack item;
            if (mat == Material.PLAYER_HEAD) {
                item = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta != null) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    meta.setOwningPlayer(op);
                    meta.setDisplayName(prefix + name);
                    meta.setLore(getStatLore(ps, tab, fmt, cur));
                    item.setItemMeta(meta);
                }
            } else {
                item = makeItem(mat, prefix + name, getStatLore(ps, tab, fmt, cur));
            }
            inv.setItem(PLAYER_SLOTS[i], item);
        }

        inv.setItem(49, makeItem(Material.ARROW, "§e← Quay lại", null));
        player.openInventory(inv);
    }

    public static boolean handleClick(Player player, int slot, GameManager gm) {
        if (slot == 1) { open(player, gm, Tab.WINS); return true; }
        if (slot == 4) { open(player, gm, Tab.WINRATE); return true; }
        if (slot == 7) { open(player, gm, Tab.WAGERED); return true; }
        if (slot == 49) { MainGUI.open(player, gm); return true; }
        return false;
    }

    private static List<Map.Entry<UUID, PlayerStats>> getSorted(Map<UUID, PlayerStats> all, Tab tab) {
        return all.entrySet().stream()
            .filter(e -> e.getValue().getTotalWins() + e.getValue().getTotalLosses() > 0)
            .sorted(getComparator(tab))
            .limit(15)
            .collect(Collectors.toList());
    }

    private static Comparator<Map.Entry<UUID, PlayerStats>> getComparator(Tab tab) {
        switch (tab) {
            case WINS:
                return Comparator.comparingInt((Map.Entry<UUID, PlayerStats> e) -> e.getValue().getTotalWins()).reversed();
            case WINRATE:
                return Comparator.comparingDouble((Map.Entry<UUID, PlayerStats> e) -> e.getValue().getWinRate()).reversed();
            case WAGERED:
                return Comparator.comparingLong((Map.Entry<UUID, PlayerStats> e) -> e.getValue().getTotalWagered()).reversed();
            default:
                return Comparator.comparingInt((Map.Entry<UUID, PlayerStats> e) -> e.getValue().getTotalWins()).reversed();
        }
    }

    private static List<String> getStatLore(PlayerStats ps, Tab tab, DecimalFormat fmt, String cur) {
        ArrayList<String> lore = new ArrayList<>();
        lore.add("§7Thắng: §a" + ps.getTotalWins() + " | Thua: §c" + ps.getTotalLosses());
        lore.add("§7Tỷ lệ: §e" + String.format("%.1f%%", ps.getWinRate()));
        lore.add("§7Tổng cược: §6" + fmt.format(ps.getTotalWagered()) + " " + cur);
        lore.add("§7Tổng thắng: §a" + fmt.format(ps.getTotalWon()) + " " + cur);
        return lore;
    }

    private static String getPlayerName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }

    private static ItemStack makeTabItem(Material mat, String name, boolean active) {
        ArrayList<String> lore = new ArrayList<>();
        lore.add(active ? "§a✔ Đang xem" : "§7Nhấn để xem");
        return makeItem(mat, (active ? "§a" : "§7") + name, lore);
    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) m.setLore(lore);
            item.setItemMeta(m);
        }
        return item;
    }

    public enum Tab { WINS, WINRATE, WAGERED }
}
