package com.prismsmp.duels.commands;

import com.prismsmp.duels.PrismDuels;
import com.prismsmp.duels.kit.GameMode;
import com.prismsmp.duels.stats.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DuelStatsCommand implements CommandExecutor, TabCompleter {
    private final PrismDuels plugin;

    public DuelStatsCommand(PrismDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        UUID targetUUID;
        String targetName;

        if (args.length > 0) {
            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
            if (target == null) {
                // Try online player
                Player online = Bukkit.getPlayer(args[0]);
                if (online == null) {
                    player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                targetUUID = online.getUniqueId();
                targetName = online.getName();
            } else {
                targetUUID = target.getUniqueId();
                targetName = target.getName() != null ? target.getName() : args[0];
            }
        } else {
            targetUUID = player.getUniqueId();
            targetName = player.getName();
        }

        PlayerStats stats = plugin.getStatsManager().getStats(targetUUID);
        openStatsGUI(player, targetName, targetUUID, stats);
        return true;
    }

    private void openStatsGUI(Player viewer, String targetName, UUID targetUUID, PlayerStats stats) {
        Inventory gui = Bukkit.createInventory(null, 45,
                Component.text(targetName + "'s Duel Stats", NamedTextColor.GOLD, TextDecoration.BOLD));

        // Player head in center top
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUUID));
        skullMeta.displayName(Component.text(targetName, NamedTextColor.GOLD, TextDecoration.BOLD));
        skullMeta.lore(List.of(
                Component.empty(),
                Component.text("Total Wins: ", NamedTextColor.GRAY).append(Component.text(stats.getTotalWins(), NamedTextColor.GREEN)),
                Component.text("Total Losses: ", NamedTextColor.GRAY).append(Component.text(stats.getTotalLosses(), NamedTextColor.RED)),
                Component.text("KD Ratio: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getKD()), NamedTextColor.AQUA)),
                Component.text("Win Rate: ", NamedTextColor.GRAY).append(Component.text(stats.getWinRate() + "%", NamedTextColor.YELLOW)),
                Component.text("Best Streak: ", NamedTextColor.GRAY).append(Component.text(stats.getBestStreak(), NamedTextColor.LIGHT_PURPLE))
        ));
        head.setItemMeta(skullMeta);
        gui.setItem(4, head);

        // Regular duel stats (left side)
        gui.setItem(19, createStatItem(Material.NETHERITE_SWORD, "Regular Duels",
                "Wins: " + stats.getRegularWins(),
                "Losses: " + stats.getRegularLosses(),
                "Current Streak: " + stats.getRegularStreak(),
                "Best Streak: " + stats.getRegularBestStreak()));

        // Practice duel stats (right side)
        gui.setItem(25, createStatItem(Material.DIAMOND_SWORD, "Practice Duels",
                "Wins: " + stats.getPracticeWins(),
                "Losses: " + stats.getPracticeLosses(),
                "Current Streak: " + stats.getPracticeStreak(),
                "Best Streak: " + stats.getPracticeBestStreak()));

        // Per-gamemode stats (bottom row)
        Material[] modeMaterials = {
                Material.IRON_SWORD, Material.END_CRYSTAL, Material.MACE,
                Material.TRIDENT, Material.NETHERITE_AXE, Material.DIAMOND_SWORD,
                Material.BOW, Material.NETHERITE_CHESTPLATE, Material.DIAMOND_CHESTPLATE
        };

        int slot = 28;
        for (int i = 0; i < GameMode.values().length; i++) {
            GameMode gm = GameMode.values()[i];
            int wins = stats.getPracticeWinsForMode(gm);
            int losses = stats.getPracticeLossesForMode(gm);
            gui.setItem(slot, createStatItem(modeMaterials[i], gm.getDisplayName(),
                    "Wins: " + wins, "Losses: " + losses));
            slot++;
        }

        // Fill empty slots with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }

        viewer.openInventory(gui);
    }

    private ItemStack createStatItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (String line : loreLines) {
            lore.add(Component.text(line, NamedTextColor.GRAY));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
            completions.removeIf(s -> !s.toLowerCase().startsWith(args[0].toLowerCase()));
        }
        return completions;
    }
}
