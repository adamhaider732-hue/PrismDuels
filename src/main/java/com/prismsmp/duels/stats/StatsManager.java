package com.prismsmp.duels.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prismsmp.duels.PrismDuels;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager {
    private final PrismDuels plugin;
    private final File statsDir;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public StatsManager(PrismDuels plugin) {
        this.plugin = plugin;
        this.statsDir = new File(plugin.getDataFolder(), "stats");
        if (!statsDir.exists()) statsDir.mkdirs();
    }

    public PlayerStats getStats(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadStats);
    }

    private PlayerStats loadStats(UUID uuid) {
        File file = new File(statsDir, uuid.toString() + ".json");
        if (!file.exists()) return new PlayerStats();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            return PlayerStats.fromJson(obj);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load stats for " + uuid + ": " + e.getMessage());
            return new PlayerStats();
        }
    }

    public void saveStats(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats == null) return;

        if (!statsDir.exists()) statsDir.mkdirs();
        File file = new File(statsDir, uuid.toString() + ".json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(stats.toJson(), writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save stats for " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAllStats() {
        for (UUID uuid : cache.keySet()) {
            saveStats(uuid);
        }
    }

    /**
     * Get sorted leaderboard entries for a stat type.
     * Returns list of [UUID, value] pairs sorted descending.
     */
    public List<Map.Entry<UUID, Integer>> getLeaderboard(String statType, int limit) {
        // Load all stat files
        Map<UUID, Integer> values = new HashMap<>();

        // First add cached entries
        for (Map.Entry<UUID, PlayerStats> entry : cache.entrySet()) {
            int val = getStatValue(entry.getValue(), statType);
            if (val > 0) values.put(entry.getKey(), val);
        }

        // Then check files not in cache
        File[] files = statsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String uuidStr = file.getName().replace(".json", "");
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    if (!cache.containsKey(uuid)) {
                        PlayerStats stats = loadStats(uuid);
                        int val = getStatValue(stats, statType);
                        if (val > 0) values.put(uuid, val);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(values.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (sorted.size() > limit) sorted = sorted.subList(0, limit);
        return sorted;
    }

    private int getStatValue(PlayerStats stats, String type) {
        return switch (type) {
            case "wins" -> stats.getTotalWins();
            case "regular_wins" -> stats.getRegularWins();
            case "practice_wins" -> stats.getPracticeWins();
            case "streak" -> stats.getBestStreak();
            case "regular_streak" -> stats.getRegularBestStreak();
            case "practice_streak" -> stats.getPracticeBestStreak();
            default -> 0;
        };
    }
}
