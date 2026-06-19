package com.prismsmp.duels.commands;

import com.prismsmp.duels.PrismDuels;
import com.prismsmp.duels.duel.DuelType;
import com.prismsmp.duels.kit.GameMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PracticeDuelCommand implements CommandExecutor, TabCompleter {
    private final PrismDuels plugin;

    public PracticeDuelCommand(PrismDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /practiceduel <player> <gamemode>", NamedTextColor.RED));
            player.sendMessage(Component.text("Gamemodes: Sword, Crystal, Mace, SpearMace, Axe, DiaPot, UHC, SMP, DiaSMP", NamedTextColor.GRAY));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player not found or not online.", NamedTextColor.RED));
            return true;
        }

        GameMode gameMode = GameMode.fromString(args[1]);
        if (gameMode == null) {
            player.sendMessage(Component.text("Unknown gamemode: " + args[1], NamedTextColor.RED));
            player.sendMessage(Component.text("Available: Sword, Crystal, Mace, SpearMace, Axe, DiaPot, UHC, SMP, DiaSMP", NamedTextColor.GRAY));
            return true;
        }

        plugin.getDuelManager().sendRequest(player, target, DuelType.PRACTICE, gameMode);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender)) completions.add(p.getName());
            }
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        } else if (args.length == 2) {
            Arrays.stream(GameMode.values())
                    .map(GameMode::getDisplayName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .forEach(completions::add);
        }
        return completions;
    }
}
