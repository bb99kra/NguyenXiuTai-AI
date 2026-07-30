package nguyenxiutai.manager;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import nguyenxiutai.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class StatsManager {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();
    private File statsFile;
    private volatile boolean dirty = false;
    // FIX: Prevent concurrent save operations
    private final AtomicBoolean saveInProgress = new AtomicBoolean(false);

    public StatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.statsFile = new File(this.plugin.getDataFolder(), "stats.yml");
        if (!this.statsFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(this.statsFile);
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
            } catch (Exception ignored) {}
        }
        this.plugin.getLogger().info("[NguyenXiuTai] Loaded " + this.stats.size() + " player stats");
    }

    public void save() {
        if (this.statsFile == null) {
            this.statsFile = new File(this.plugin.getDataFolder(), "stats.yml");
        }
        // FIX: Snapshot current state atomically to avoid writing partial data
        if (!saveInProgress.compareAndSet(false, true)) {
            return; // Another save is in progress, skip
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            // Take a snapshot of current stats
            Map<UUID, PlayerStats> snapshot = new ConcurrentHashMap<>(this.stats);
            for (Map.Entry<UUID, PlayerStats> e : snapshot.entrySet()) {
                String key = e.getKey().toString();
                PlayerStats ps = e.getValue();
                config.set(key + ".wins", ps.getTotalWins());
                config.set(key + ".losses", ps.getTotalLosses());
                config.set(key + ".wagered", ps.getTotalWagered());
                config.set(key + ".won", ps.getTotalWon());
                config.set(key + ".last-won", ps.isLastWon());
                config.set(key + ".last-won-amount", ps.getLastWonAmount());
                config.set(key + ".last-side", ps.getLastSide());
            }
            // Write to temp file then atomic move
            File tempFile = new File(this.statsFile.getParent(), "stats.yml.tmp");
            config.save(tempFile);
            if (this.statsFile.exists()) {
                this.statsFile.delete();
            }
            tempFile.renameTo(this.statsFile);
            this.dirty = false;
        } catch (Exception e) {
            this.plugin.getLogger().warning("[NguyenXiuTai] Failed to save stats: " + e.getMessage());
        } finally {
            saveInProgress.set(false);
        }
    }

    public void saveAsync() {
        if (!this.dirty) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously((Plugin) this.plugin, this::save);
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
