/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.milkbowl.vault.economy.Economy
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.RegisteredServiceProvider
 */
package nguyenxiutai.manager;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private Economy economy;
    private boolean enabled;
    private final ReentrantLock lock = new ReentrantLock();

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = (Economy)rsp.getProvider();
        this.enabled = this.economy != null;
        return this.enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean withdraw(UUID uuid, double amount) {
        if (!this.enabled) {
            return false;
        }
        this.lock.lock();
        try {
            boolean bl = this.economy.withdrawPlayer(this.resolve(uuid), amount).transactionSuccess();
            return bl;
        }
        finally {
            this.lock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean deposit(UUID uuid, double amount) {
        if (!this.enabled) {
            return false;
        }
        this.lock.lock();
        try {
            boolean bl = this.economy.depositPlayer(this.resolve(uuid), amount).transactionSuccess();
            return bl;
        }
        finally {
            this.lock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public double getBalance(UUID uuid) {
        if (!this.enabled) {
            return 0.0;
        }
        this.lock.lock();
        try {
            double d = this.economy.getBalance(this.resolve(uuid));
            return d;
        }
        finally {
            this.lock.unlock();
        }
    }

    private OfflinePlayer resolve(UUID uuid) {
        Player online = Bukkit.getPlayer((UUID)uuid);
        return online != null ? online : Bukkit.getOfflinePlayer((UUID)uuid);
    }
}

