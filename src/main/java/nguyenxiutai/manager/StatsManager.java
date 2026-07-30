/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package nguyenxiutai.manager;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import nguyenxiutai.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class StatsManager {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<UUID, PlayerStats>();
    private File statsFile;
    private volatile boolean dirty = false;

    public StatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.statsFile = new File(this.plugin.getDataFolder(), "stats.yml");
        if (!this.statsFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)this.statsFile);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerStats ps = new PlayerStats(uuid);
                ps.setTotalWins(config.getInt(key + ".wins", 0));
                ps.setTotalLosses(config.getInt(key + ".losses", 0));
                ps.setTotalWagered(config.getLong(key + ".wagered", 0L));
                ps.setTotalWon(config.getLong(key + ".won", 0L));
                ps.setLastWon(config.getBoolean(key + ".last-won", false));
                ps.setLastWonAmount(config.getLong(key + ".last-won-amount", 0L));
                ps.setLastSide(config.getString(key + ".last-side", null));
                this.stats.put(uuid, ps);
            }
            catch (Exception exception) {}
        }
        this.plugin.getLogger().info("[NguyenXiuTai] Loaded " + this.stats.size() + " player stats");
    }

    public void save() {
        if (this.statsFile == null) {
            this.statsFile = new File(this.plugin.getDataFolder(), "stats.yml");
        }
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> e : this.stats.entrySet()) {
            String key = e.getKey().toString();
            PlayerStats ps = e.getValue();
            config.set(key + ".wins", (Object)ps.getTotalWins());
            config.set(key + ".losses", (Object)ps.getTotalLosses());
            config.set(key + ".wagered", (Object)ps.getTotalWagered());
            config.set(key + ".won", (Object)ps.getTotalWon());
            config.set(key + ".last-won", (Object)ps.isLastWon());
            config.set(key + ".last-won-amount", (Object)ps.getLastWonAmount());
            config.set(key + ".last-side", (Object)ps.getLastSide());
        }
        try {
            config.save(this.statsFile);
            this.dirty = false;
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[NguyenXiuTai] Failed to save stats: " + e.getMessage());
        }
    }

    public void saveAsync() {
        if (!this.dirty) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, this::save);
    }

    public PlayerStats get(UUID uuid) {
        this.dirty = true;
        return this.stats.computeIfAbsent(uuid, PlayerStats::new);
    }

    public Map<UUID, PlayerStats> getAll() {
        return this.stats;
    }

    public boolean isDirty() {
        return this.dirty;
    }
}

