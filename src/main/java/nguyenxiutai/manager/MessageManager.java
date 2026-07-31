/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package nguyenxiutai.manager;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageManager {
    private final JavaPlugin plugin;
    private File messageFile;
    private FileConfiguration messageConfig;
    private final Map<String, String> cache = new ConcurrentHashMap<String, String>();
    private final Map<String, List<String>> listCache = new ConcurrentHashMap<String, List<String>>();

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.messageFile = new File(this.plugin.getDataFolder(), "Message.yml");
        if (!this.messageFile.exists()) {
            this.plugin.saveResource("Message.yml", false);
        }
        this.messageConfig = YamlConfiguration.loadConfiguration((File)this.messageFile);
        this.cache.clear();
        this.listCache.clear();
    }

    public String get(String path, String def) {
        return this.cache.computeIfAbsent(path, p -> this.clr(this.messageConfig.getString(p, def)));
    }

    public List<String> getList(String path) {
        return this.listCache.computeIfAbsent(path, p -> {
            List<String> raw = this.messageConfig.getStringList(p);
            raw.replaceAll(this::clr);
            return raw;
        });
    }

    public String get(String path, String def, Map<String, String> placeholders) {
        String msg = this.get(path, def);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            msg = msg.replace(e.getKey(), e.getValue());
        }
        return msg;
    }

    private String clr(String s) {
        return s == null ? "" : s.replace('&', '\u00a7');
    }

    public String getMsgTai() {
        return this.get("messages.dat-tai-thanh-cong", "&a\u2705 \u0110\u1eb7t t\u00e0i &a{amount} &fth\u00e0nh c\u00f4ng!");
    }

    public String getMsgXiu() {
        return this.get("messages.dat-xiu-thanh-cong", "&a\u2705 \u0110\u1eb7t x\u1ec9u &a{amount} &fth\u00e0nh c\u00f4ng!");
    }

    public String getMsgNoMoney() {
        return this.get("messages.khong-du-tien", "&c\u274c Kh\u00f4ng \u0111\u1ee7 ti\u1ec1n!");
    }

    public String getMsgLimit() {
        return this.get("messages.vuot-qua-gioi-han", "&c\u274c Ti\u1ec1n t\u1eeb {min} \u0111\u1ebfn {max}!");
    }

    public String getMsgRolling() {
        return this.get("messages.dang-quay", "&e\ud83c\udfb2 \u0110ang quay k\u1ebft qu\u1ea3...");
    }

    public String getMsgJP() {
        return this.get("messages.no-hu", "&6\ud83c\udf89 N\u1ed5 h\u0169! &f{player} &6tr\u00fang &f{amount}!");
    }

    public String getMsgNoSession() {
        return this.get("messages.khong-co-cuoc", "&c\u274c Kh\u00f4ng c\u00f3 phi\u00ean n\u00e0o m\u1edf!");
    }

    public String getMsgNhap() {
        return this.get("messages.nhap-so-tien", "&e\ud83d\udcdd Nh\u1eadp s\u1ed1 ti\u1ec1n mu\u1ed1n c\u01b0\u1ee3c (10k, 1M...). G\u00f5 'huy' \u0111\u1ec3 h\u1ee7y.");
    }

    public String getMsgHuy() {
        return this.get("messages.huy-cuoc", "&c\u274c \u0110\u00e3 h\u1ee7y \u0111\u1eb7t c\u01b0\u1ee3c.");
    }

    public String getMsgDaDatTai() {
        return this.get("messages.da-dat-ben-kia", "&c\u274c B\u1ea1n \u0111\u00e3 \u0111\u1eb7t t\u00e0i r\u1ed3i!");
    }

    public String getMsgDaDatXiu() {
        return this.get("messages.da-dat-ben-nay", "&c\u274c B\u1ea1n \u0111\u00e3 \u0111\u1eb7t x\u1ec9u r\u1ed3i!");
    }

    public String getMsgThang() {
        return this.get("messages.thang-cuoc", "&a\u2705 Th\u1eafng! +{amount} {currency}");
    }

    public String getMsgKetQua() {
        return this.get("messages.ket-qua", "&6\ud83c\udfb2 &f{dice} = &e{total} &f\u2192 {result}");
    }

    public String getMsgPhienMoi() {
        return this.get("messages.phien-moi", "&a&l\u2726 Phi\u00ean #{session} \u0111\u00e3 b\u1eaft \u0111\u1ea7u!");
    }

    public String getMsgHoanTien() {
        return this.get("messages.hoan-tien-shutdown", "&e\u26a0 Server \u0111\u00f3ng l\u1ea1i, \u0111\u00e3 ho\u00e0n ti\u1ec1n: {amount} {currency}");
    }

    public String getMsgPayoutInfo() {
        return this.get("messages.payout-info", "&7Payout: &e{multiplier}x &7(T\u00e0i:{tai_total} | X\u1ec9u:{xiu_total})");
    }

    public String getBossBarDangCuoc() {
        return this.get("bossbar.dang-cuoc", "Phi\u00ean #{session} | T\u00e0i ({tai_total}{currency}) | X\u1ec9u ({xiu_total}{currency}) | H\u0169 ({hu}{currency}) | {time}s");
    }

    public String getBossBarDangQuay() {
        return this.get("bossbar.dang-quay", "Phi\u00ean #{session} | \u0110ang quay... | T\u00e0i ({tai_total}{currency}) | X\u1ec9u ({xiu_total}{currency})");
    }

    public String getBossBarKetQua() {
        return this.get("bossbar.ket-qua", "Phi\u00ean #{session} | {result} | T\u00e0i: {tai_total}{currency} | X\u1ec9u: {xiu_total}{currency}");
    }

    public String getGuiMainTitle() {
        return this.get("gui.main-title", "t\u00e0i x\u1ec9u");
    }

    public String getGuiCauTitle() {
        return this.get("gui.cau-title", "K\u1ebft qu\u1ea3 c\u1ea7u");
    }

    public String getGuiMainInfoName() {
        return this.get("gui.main-info-name", "&6\ud83c\udfb2 Phi\u00ean #{session}");
    }

    public List<String> getGuiMainInfoLore() {
        return this.getList("gui.main-info-lore");
    }

    public String getGuiMainTaiName() {
        return this.get("gui.main-tai-name", "&a\ud83c\udfc6 T\u00e0i");
    }

    public List<String> getGuiMainTaiLore() {
        return this.getList("gui.main-tai-lore");
    }

    public String getGuiMainXiuName() {
        return this.get("gui.main-xiu-name", "&c\ud83c\udfc6 X\u1ec9u");
    }

    public List<String> getGuiMainXiuLore() {
        return this.getList("gui.main-xiu-lore");
    }

    public String getGuiCauHeaderName() {
        return this.get("gui.cau-header-name", "&6\ud83d\udcc8 K\u1ebft qu\u1ea3 c\u1ea7u");
    }

    public List<String> getGuiCauHeaderLore() {
        return this.getList("gui.cau-header-lore");
    }

    public String getGuiCauStatsName() {
        return this.get("gui.cau-stats-name", "&6\ud83d\udcca Th\u1ed1ng k\u00ea");
    }

    public String getGuiCauBackName() {
        return this.get("gui.cau-back-name", "&e\u2190 Quay l\u1ea1i");
    }

    public String getGuiCauTaiLabel() {
        return this.get("gui.cau-tai-label", " &aT\u00e0i");
    }

    public String getGuiCauXiuLabel() {
        return this.get("gui.cau-xiu-label", " &cX\u1ec9u");
    }

    public String getDiscordEmbedTitle() {
        return this.get("discord.embed-title", "\ud83c\udfb2 Phi\u00ean #{session}");
    }

    public int getDiscordColorTai() {
        return this.messageConfig.getInt("discord.embed-color-tai", 65280);
    }

    public int getDiscordColorXiu() {
        return this.messageConfig.getInt("discord.embed-color-xiu", 0xFF0000);
    }

    public String getDiscordFieldDice() {
        return this.get("discord.field-dice", "X\u00fac x\u1eafc");
    }

    public String getDiscordFieldKetQua() {
        return this.get("discord.field-ketqua", "K\u1ebft qu\u1ea3");
    }

    public String getDiscordFieldTongCuoc() {
        return this.get("discord.field-tongcuoc", "T\u1ed5ng c\u01b0\u1ee3c");
    }

    public String getDiscordFieldHu() {
        return this.get("discord.field-hu", "H\u0169");
    }

    public String getDiscordFieldNguoiChoi() {
        return this.get("discord.field-nguoichoi", "Ng\u01b0\u1eddi ch\u01a1i");
    }

    public String getDiscordFieldJackpot() {
        return this.get("discord.field-jackpot", "\ud83c\udf89 N\u1ed5 h\u0169");
    }

    public String getDiscordFooter() {
        return this.get("discord.footer-text", "NguyenXiuTai v1.2.0");
    }
}

