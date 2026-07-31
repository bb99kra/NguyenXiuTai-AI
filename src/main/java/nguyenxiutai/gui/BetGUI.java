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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nguyenxiutai.manager.GameManager;
import nguyenxiutai.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BetGUI {
    private static final long[] AMOUNTS = new long[]{10000L, 50000L, 100000L, 500000L, 1000000L, 5000000L, 10000000L};
    private static final Material[] MATERIALS = new Material[]{Material.GOLD_NUGGET, Material.GOLD_INGOT, Material.GOLD_BLOCK, Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR};
    private static final int[] AMOUNT_SLOTS = new int[]{1, 2, 3, 4, 5, 6, 7};
    private static final int TAI_SLOT = 13;
    private static final int XIU_SLOT = 13;
    private static final int CONFIRM_TAI_SLOT = 11;
    private static final int CONFIRM_XIU_SLOT = 15;
    private static final int INFO_SLOT = 0;
    private static final int CLEAR_SLOT = 8;
    private static final Map<UUID, Long> selectedAmount = new HashMap<UUID, Long>();

    public static void open(Player player, GameManager gm) {
        MessageManager msg = gm.getMessageManager();
        DecimalFormat fmt = gm.getCurrencyFormat();
        String cur = gm.getCurrencySymbol();
        Inventory inv = Bukkit.createInventory(null, (int)27, (String)"\u00a76\u00a7lT\u00e0i X\u1ec9u - Ch\u1ecdn ti\u1ec1n");
        ArrayList<String> infoLore = new ArrayList<String>();
        infoLore.add("\u00a77Min: \u00a7e" + fmt.format(gm.getMinBet()));
        infoLore.add("\u00a77Max: \u00a7e" + fmt.format(gm.getMaxBet()));
        infoLore.add("");
        infoLore.add("\u00a77S\u1ed1 d\u01b0: \u00a7e" + fmt.format(BetGUI.getPlayerBalance(player, gm)));
        inv.setItem(0, BetGUI.makeItem(Material.BOOK, "\u00a76\u00a7lTh\u00f4ng tin", infoLore));
        for (int i = 0; i < AMOUNTS.length; ++i) {
            long amount = AMOUNTS[i];
            ArrayList<String> lore = new ArrayList<String>();
            lore.add("\u00a77Nh\u1ea5n \u0111\u1ec3 ch\u1ecdn");
            lore.add("\u00a7e" + fmt.format(amount) + " " + cur);
            inv.setItem(AMOUNT_SLOTS[i], BetGUI.makeItem(MATERIALS[i], "\u00a76" + fmt.format(amount), lore));
        }
        inv.setItem(8, BetGUI.makeItem(Material.BARRIER, "\u00a7cX\u00f3a l\u1ef1a ch\u1ecdn", null));
        BetGUI.updateConfirmButtons(inv, gm, 0L);
        player.openInventory(inv);
    }

    public static void updateConfirmButtons(Inventory inv, GameManager gm, long amount) {
        DecimalFormat fmt = gm.getCurrencyFormat();
        String cur = gm.getCurrencySymbol();
        if (amount > 0L) {
            ArrayList<String> taiLore = new ArrayList<String>();
            taiLore.add("\u00a77C\u01b0\u1ee3c: \u00a7e" + fmt.format(amount) + " " + cur);
            taiLore.add("");
            taiLore.add("\u00a7a\u25b6 Nh\u1ea5n \u0111\u1ec3 \u0111\u1eb7t T\u00e0i");
            inv.setItem(11, BetGUI.makeItem(Material.LIME_STAINED_GLASS_PANE, "\u00a7a\u00a7lT\u00c0I", taiLore));
            ArrayList<String> xiuLore = new ArrayList<String>();
            xiuLore.add("\u00a77C\u01b0\u1ee3c: \u00a7e" + fmt.format(amount) + " " + cur);
            xiuLore.add("");
            xiuLore.add("\u00a7c\u25b6 Nh\u1ea5n \u0111\u1ec3 \u0111\u1eb7t X\u1ec9u");
            inv.setItem(15, BetGUI.makeItem(Material.RED_STAINED_GLASS_PANE, "\u00a7c\u00a7lX\u1ec8U", xiuLore));
        } else {
            ArrayList<String> disabledLore = new ArrayList<String>();
            disabledLore.add("\u00a77Ch\u1ecdn s\u1ed1 ti\u1ec1n tr\u01b0\u1edbc");
            inv.setItem(11, BetGUI.makeItem(Material.GRAY_STAINED_GLASS_PANE, "\u00a77\u00a7lT\u00c0I", disabledLore));
            inv.setItem(15, BetGUI.makeItem(Material.GRAY_STAINED_GLASS_PANE, "\u00a77\u00a7lX\u1ec8U", disabledLore));
        }
    }

    public static boolean handleClick(Player player, int slot, GameManager gm) {
        UUID uuid = player.getUniqueId();
        for (int i = 0; i < AMOUNT_SLOTS.length; ++i) {
            if (slot != AMOUNT_SLOTS[i]) continue;
            long amount = AMOUNTS[i];
            selectedAmount.put(uuid, amount);
            player.closeInventory();
            BetGUI.open(player, gm);
            player.getOpenInventory().getTopInventory().setItem(AMOUNT_SLOTS[i], BetGUI.makeItem(MATERIALS[i], "\u00a7a\u2714 " + gm.getCurrencyFormat().format(amount), Arrays.asList("\u00a77\u0110\u00e3 ch\u1ecdn", "\u00a7eNh\u1ea5n l\u1ea1i \u0111\u1ec3 b\u1ecf ch\u1ecdn")));
            BetGUI.updateConfirmButtons(player.getOpenInventory().getTopInventory(), gm, amount);
            return true;
        }
        if (slot == 8) {
            selectedAmount.remove(uuid);
            player.closeInventory();
            BetGUI.open(player, gm);
            return true;
        }
        if (slot == 11) {
            Long amount = selectedAmount.get(uuid);
            if (amount == null || amount <= 0L) {
                player.sendMessage("\u00a7cCh\u1ecdn s\u1ed1 ti\u1ec1n tr\u01b0\u1edbc!");
                return true;
            }
            selectedAmount.remove(uuid);
            player.closeInventory();
            gm.placeBet(player, true, amount);
            return true;
        }
        if (slot == 15) {
            Long amount = selectedAmount.get(uuid);
            if (amount == null || amount <= 0L) {
                player.sendMessage("\u00a7cCh\u1ecdn s\u1ed1 ti\u1ec1n tr\u01b0\u1edbc!");
                return true;
            }
            selectedAmount.remove(uuid);
            player.closeInventory();
            gm.placeBet(player, false, amount);
            return true;
        }
        return false;
    }

    // FIX #8: Actually return player balance from economy
    private static double getPlayerBalance(Player player, GameManager gm) {
        return gm.getEconomyManager().getBalance(player.getUniqueId());
    }

    // FIX: Cleanup selected amount on quit
    public static void cleanupPlayer(UUID uuid) {
        selectedAmount.remove(uuid);
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
}

