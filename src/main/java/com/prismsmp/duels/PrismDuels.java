package com.prismsmp.duels;

import com.prismsmp.duels.arena.ArenaManager;
import com.prismsmp.duels.commands.*;
import com.prismsmp.duels.duel.DuelManager;
import com.prismsmp.duels.kit.KitManager;
import com.prismsmp.duels.listeners.DuelListener;
import com.prismsmp.duels.placeholders.DuelPlaceholders;
import com.prismsmp.duels.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PrismDuels extends JavaPlugin {
    private ArenaManager arenaManager;
    private DuelManager duelManager;
    private KitManager kitManager;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // Initialize managers
        statsManager = new StatsManager(this);
        arenaManager = new ArenaManager(this);
        kitManager = new KitManager(this);
        duelManager = new DuelManager(this);

        // Register commands
        var duelCmd = getCommand("duel");
        var duelHandler = new DuelCommand(this);
        if (duelCmd != null) {
            duelCmd.setExecutor(duelHandler);
            duelCmd.setTabCompleter(duelHandler);
        }

        var practiceCmd = getCommand("practiceduel");
        var practiceHandler = new PracticeDuelCommand(this);
        if (practiceCmd != null) {
            practiceCmd.setExecutor(practiceHandler);
            practiceCmd.setTabCompleter(practiceHandler);
        }

        var statsCmd = getCommand("duelstats");
        var statsHandler = new DuelStatsCommand(this);
        if (statsCmd != null) {
            statsCmd.setExecutor(statsHandler);
            statsCmd.setTabCompleter(statsHandler);
        }

        var adminCmd = getCommand("duels");
        var adminHandler = new DuelsAdminCommand(this);
        if (adminCmd != null) {
            adminCmd.setExecutor(adminHandler);
            adminCmd.setTabCompleter(adminHandler);
        }

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new DuelListener(this), this);

        // PlaceholderAPI hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DuelPlaceholders(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        // Auto-save stats every 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> statsManager.saveAllStats(), 6000L, 6000L);

        getLogger().info("PrismDuels v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) duelManager.cleanupAll();
        if (statsManager != null) statsManager.saveAllStats();
        getLogger().info("PrismDuels disabled.");
    }

    public ArenaManager getArenaManager() { return arenaManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public KitManager getKitManager() { return kitManager; }
    public StatsManager getStatsManager() { return statsManager; }
}
