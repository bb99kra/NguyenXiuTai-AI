package nguyenxiutai.ai;

import java.text.DecimalFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import java.util.Arrays;

/**
 * Heat Map GUI - Visual streak/pattern display for /cau command
 */
public class HeatMapGUI {

    private final AIEngine engine;
    private final DecimalFormat df = new DecimalFormat("#,###");

    public HeatMapGUI(AIEngine engine) {
        this.engine = engine;
    }

    /**
     * Open heat map GUI for player
     */
    public void openHeatMap(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§l🔥 AI Cầu Analysis");

        // Row 1: Last 30 results (slots 0-29)
        boolean[] recent = engine.getDataManager().getRecentResults(30);
        for (int i = 0; i < 30 && i < recent.length; i++) {
            gui.setItem(i, createResultItem(recent[i], i));
        }

        // Row 3-4: Streak info (slot 31-33)
        AIEngine.StreakInfo streak = engine.getStreakInfo();
        gui.setItem(31, createStreakItem(streak));

        // Row 4: Prediction (slot 32)
        AIEngine.Prediction pred = engine.getPrediction();
        gui.setItem(32, createPredictionItem(pred));

        // Row 4: Tai/Xiu ratio (slot 33)
        gui.setItem(33, createRatioItem());

        // Row 5: Stats (slot 36-44)
        gui.setItem(36, createStatItem("§aTài Count", engine.getDataManager().countTaiInLast(50), 50));
        gui.setItem(37, createStatItem("§cXỉu Count", 50 - engine.getDataManager().countTaiInLast(50), 50));
        gui.setItem(38, createInfoItem("§eHouse Edge", String.format("%.1f%%", engine.getCurrentHouseEdge() * 100)));
        gui.setItem(39, createInfoItem("§bTai Ratio", String.format("%.1f%%", engine.getCurrentTaiRatio() * 100)));

        // Fill empty slots with glass panes
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName(" ");
        glass.setItemMeta(gm);
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createResultItem(boolean isTai, int index) {
        Material mat = isTai ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String side = isTai ? "§aTài" : "§cXỉu";
        meta.setDisplayName(side + " §7#" + (index + 1));
        meta.setLore(Arrays.asList(
            "§7Phiên thứ " + (engine.getDataManager().getHistorySize() - index),
            isTai ? "§aKết quả: Tài (11-17)" : "§cKết quả: Xỉu (4-10)"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStreakItem(AIEngine.StreakInfo streak) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l🔥 Cầu hiện tại");

        if (streak.count >= 1) {
            String side = streak.side ? "§aTài" : "§cXỉu";
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < Math.min(streak.count, 10); i++) {
                bar.append(streak.side ? "§a■" : "§c■");
            }
            meta.setLore(Arrays.asList(
                "§7" + side + " §7x" + streak.count,
                bar.toString(),
                streak.alert ? "§c⚠️ Cầu dài!" : "§a✅ Bình thường"
            ));
        } else {
            meta.setLore(Arrays.asList("§7Không có cầu đặc biệt"));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPredictionItem(AIEngine.Prediction pred) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l🔮 Dự đoán");
        meta.setLore(Arrays.asList(
            "§aTài: §f" + String.format("%.0f%%", pred.taiProb * 100),
            "§cXỉu: §f" + String.format("%.0f%%", pred.xiuProb * 100),
            "§8(Tham khảo)"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRatioItem() {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l⚖️ Tỷ lệ AI");
        meta.setLore(Arrays.asList(
            "§7Tài Ratio: §f" + String.format("%.1f%%", engine.getCurrentTaiRatio() * 100),
            "§7House Edge: §f" + String.format("%.1f%%", engine.getCurrentHouseEdge() * 100),
            "§7Max Bet: §f" + df.format(engine.getCurrentMaxBet())
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatItem(String name, int value, int max) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        double pct = max == 0 ? 0 : (double) value / max * 100;
        meta.setLore(Arrays.asList(
            "§7Số lần: §f" + value + "/" + max,
            "§7Tỷ lệ: §f" + String.format("%.1f%%", pct)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(String name, String value) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("§7" + value));
        item.setItemMeta(meta);
        return item;
    }
}
