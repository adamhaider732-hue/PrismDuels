package com.prismsmp.duels.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.prismsmp.duels.kit.GameMode;

import java.util.HashMap;
import java.util.Map;

public class PlayerStats {
    // Regular duel stats
    private int regularWins;
    private int regularLosses;
    private int regularStreak;
    private int regularBestStreak;

    // Practice duel stats (overall)
    private int practiceWins;
    private int practiceLosses;
    private int practiceStreak;
    private int practiceBestStreak;

    // Practice wins per gamemode
    private final Map<String, Integer> practiceWinsPerMode = new HashMap<>();
    private final Map<String, Integer> practiceLossesPerMode = new HashMap<>();

    public void addRegularWin() {
        regularWins++;
        regularStreak++;
        if (regularStreak > regularBestStreak) regularBestStreak = regularStreak;
    }

    public void addRegularLoss() {
        regularLosses++;
        regularStreak = 0;
    }

    public void addPracticeWin(GameMode mode) {
        practiceWins++;
        practiceStreak++;
        if (practiceStreak > practiceBestStreak) practiceBestStreak = practiceStreak;
        practiceWinsPerMode.merge(mode.getConfigKey(), 1, Integer::sum);
    }

    public void addPracticeLoss(GameMode mode) {
        practiceLosses++;
        practiceStreak = 0;
        practiceLossesPerMode.merge(mode.getConfigKey(), 1, Integer::sum);
    }

    public int getTotalWins() { return regularWins + practiceWins; }
    public int getTotalLosses() { return regularLosses + practiceLosses; }
    public int getRegularWins() { return regularWins; }
    public int getRegularLosses() { return regularLosses; }
    public int getRegularStreak() { return regularStreak; }
    public int getRegularBestStreak() { return regularBestStreak; }
    public int getPracticeWins() { return practiceWins; }
    public int getPracticeLosses() { return practiceLosses; }
    public int getPracticeStreak() { return practiceStreak; }
    public int getPracticeBestStreak() { return practiceBestStreak; }

    public int getBestStreak() {
        return Math.max(regularBestStreak, practiceBestStreak);
    }

    public double getKD() {
        int totalLosses = getTotalLosses();
        if (totalLosses == 0) return getTotalWins();
        return Math.round((double) getTotalWins() / totalLosses * 100.0) / 100.0;
    }

    public double getWinRate() {
        int total = getTotalWins() + getTotalLosses();
        if (total == 0) return 0;
        return Math.round((double) getTotalWins() / total * 1000.0) / 10.0;
    }

    public int getPracticeWinsForMode(GameMode mode) {
        return practiceWinsPerMode.getOrDefault(mode.getConfigKey(), 0);
    }

    public int getPracticeLossesForMode(GameMode mode) {
        return practiceLossesPerMode.getOrDefault(mode.getConfigKey(), 0);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("regularWins", regularWins);
        obj.addProperty("regularLosses", regularLosses);
        obj.addProperty("regularStreak", regularStreak);
        obj.addProperty("regularBestStreak", regularBestStreak);
        obj.addProperty("practiceWins", practiceWins);
        obj.addProperty("practiceLosses", practiceLosses);
        obj.addProperty("practiceStreak", practiceStreak);
        obj.addProperty("practiceBestStreak", practiceBestStreak);

        JsonObject modeWins = new JsonObject();
        for (Map.Entry<String, Integer> e : practiceWinsPerMode.entrySet()) {
            modeWins.addProperty(e.getKey(), e.getValue());
        }
        obj.add("practiceWinsPerMode", modeWins);

        JsonObject modeLosses = new JsonObject();
        for (Map.Entry<String, Integer> e : practiceLossesPerMode.entrySet()) {
            modeLosses.addProperty(e.getKey(), e.getValue());
        }
        obj.add("practiceLossesPerMode", modeLosses);

        return obj;
    }

    public static PlayerStats fromJson(JsonObject obj) {
        PlayerStats stats = new PlayerStats();
        stats.regularWins = getInt(obj, "regularWins");
        stats.regularLosses = getInt(obj, "regularLosses");
        stats.regularStreak = getInt(obj, "regularStreak");
        stats.regularBestStreak = getInt(obj, "regularBestStreak");
        stats.practiceWins = getInt(obj, "practiceWins");
        stats.practiceLosses = getInt(obj, "practiceLosses");
        stats.practiceStreak = getInt(obj, "practiceStreak");
        stats.practiceBestStreak = getInt(obj, "practiceBestStreak");

        if (obj.has("practiceWinsPerMode")) {
            JsonObject mw = obj.getAsJsonObject("practiceWinsPerMode");
            for (Map.Entry<String, JsonElement> e : mw.entrySet()) {
                stats.practiceWinsPerMode.put(e.getKey(), e.getValue().getAsInt());
            }
        }
        if (obj.has("practiceLossesPerMode")) {
            JsonObject ml = obj.getAsJsonObject("practiceLossesPerMode");
            for (Map.Entry<String, JsonElement> e : ml.entrySet()) {
                stats.practiceLossesPerMode.put(e.getKey(), e.getValue().getAsInt());
            }
        }
        return stats;
    }

    private static int getInt(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsInt() : 0;
    }
}
