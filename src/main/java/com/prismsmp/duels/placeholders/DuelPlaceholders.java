package com.prismsmp.duels.placeholders;

import com.prismsmp.duels.PrismDuels;
import com.prismsmp.duels.stats.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DuelPlaceholders extends PlaceholderExpansion {
    private final PrismDuels plugin;

    public DuelPlaceholders(PrismDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "prismduel";
    }

    @Override
    public @NotNull String getAuthor() {
        return "PrismSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());

        return switch (params.toLowerCase()) {
            // Total stats
            case "wins" -> String.valueOf(stats.getTotalWins());
            case "losses" -> String.valueOf(stats.getTotalLosses());
            case "kd" -> String.valueOf(stats.getKD());
            case "winrate" -> stats.getWinRate() + "%";
            case "streak" -> String.valueOf(stats.getBestStreak());

            // Regular stats
            case "regular_wins" -> String.valueOf(stats.getRegularWins());
            case "regular_losses" -> String.valueOf(stats.getRegularLosses());
            case "regular_streak" -> String.valueOf(stats.getRegularStreak());
            case "regular_best_streak" -> String.valueOf(stats.getRegularBestStreak());

            // Practice stats
            case "practice_wins" -> String.valueOf(stats.getPracticeWins());
            case "practice_losses" -> String.valueOf(stats.getPracticeLosses());
            case "practice_streak" -> String.valueOf(stats.getPracticeStreak());
            case "practice_best_streak" -> String.valueOf(stats.getPracticeBestStreak());

            default -> null;
        };
    }
}
