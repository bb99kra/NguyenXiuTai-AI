/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.java.JavaPlugin
 */
package nguyenxiutai.manager;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DailyBonusManager {
    private final JavaPlugin plugin;
    private final Map<UUID, String> lastClaimDate = new HashMap<UUID, String>();
    private final Map<UUID, Integer> streak = new HashMap<UUID, Integer>();
    private File bonusFile;
    private long bonusAmount;
    private int streakMultiplierDays;
    private double streakMultiplier;

    public DailyBonusManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(long bonusAmount, int streakDays, double streakMult) {
        this.bonusAmount = bonusAmount;
        this.streakMultiplierDays = streakDays;
        this.streakMultiplier = streakMult;
        this.bonusFile = new File(this.plugin.getDataFolder(), "daily-bonus.yml");
        if (!this.bonusFile.exists()) {
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.save(this.bonusFile);
            }
            catch (Exception config) {
                // empty catch block
            }
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)this.bonusFile);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                this.lastClaimDate.put(uuid, config.getString(key + ".date", ""));
                this.streak.put(uuid, config.getInt(key + ".streak", 0));
            }
            catch (Exception exception) {}
        }
        this.plugin.getLogger().info("[NguyenXiuTai] Loaded " + this.lastClaimDate.size() + " daily bonus records");
    }

    public void save() {
        if (this.bonusFile == null) {
            this.bonusFile = new File(this.plugin.getDataFolder(), "daily-bonus.yml");
        }
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : this.lastClaimDate.entrySet()) {
            String key = e.getKey().toString();
            config.set(key + ".date", (Object)e.getValue());
            config.set(key + ".streak", (Object)this.streak.getOrDefault(e.getKey(), 0));
        }
        try {
            config.save(this.bonusFile);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public boolean canClaim(Player player) {
        String lastClaim;
        UUID uuid = player.getUniqueId();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return !today.equals(lastClaim = this.lastClaimDate.get(uuid));
    }

    public long claim(Player player) {
        UUID uuid = player.getUniqueId();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String yesterday = LocalDate.now().minusDays(1L).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String lastClaim = this.lastClaimDate.get(uuid);
        if (today.equals(lastClaim)) {
            return -1L;
        }
        int currentStreak = this.streak.getOrDefault(uuid, 0);
        currentStreak = yesterday.equals(lastClaim) ? ++currentStreak : 1;
        this.streak.put(uuid, currentStreak);
        long bonus = this.bonusAmount;
        if (currentStreak >= this.streakMultiplierDays) {
            bonus = (long)((double)bonus * this.streakMultiplier);
        }
        this.lastClaimDate.put(uuid, today);
        this.save();
        return bonus;
    }

    public int getStreak(Player player) {
        return this.streak.getOrDefault(player.getUniqueId(), 0);
    }

    public long getBonusAmount() {
        return this.bonusAmount;
    }

    public long getBonusForStreak(int streakCount) {
        long bonus = this.bonusAmount;
        if (streakCount >= this.streakMultiplierDays) {
            bonus = (long)((double)bonus * this.streakMultiplier);
        }
        return bonus;
    }

    public int getStreakMultiplierDays() {
        return this.streakMultiplierDays;
    }

    public double getStreakMultiplier() {
        return this.streakMultiplier;
    }
}

