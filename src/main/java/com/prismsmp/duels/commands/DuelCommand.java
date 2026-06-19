package com.prismsmp.duels.commands;

import com.prismsmp.duels.PrismDuels;
import com.prismsmp.duels.duel.ActiveDuel;
import com.prismsmp.duels.duel.DuelType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private final PrismDuels plugin;

    public DuelCommand(PrismDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /duel <player> | /duel accept | /duel deny | /duel spectate <player>", NamedTextColor.RED));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("accept")) {
            plugin.getDuelManager().acceptRequest(player);
            return true;
        }

        if (sub.equals("deny") || sub.equals("decline")) {
            plugin.getDuelManager().denyRequest(player);
            return true;
        }

        if (sub.equals("spectate") || sub.equals("spec") || sub.equals("watch")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Usage: /duel spectate <player>", NamedTextColor.RED));
                return true;
            }
            handleSpectate(player, args[1]);
            return true;
        }

        if (sub.equals("leave") || sub.equals("unspectate")) {
            handleLeaveSpectate(player);
            return true;
        }

        // Challenge a player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player not found or not online.", NamedTextColor.RED));
            return true;
        }

        plugin.getDuelManager().sendRequest(player, target, DuelType.REGULAR, null);
        return true;
    }

    private void handleSpectate(Player spectator, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            spectator.sendMessage(Component.text("Player not found or not online.", NamedTextColor.RED));
            return;
        }

        ActiveDuel duel = plugin.getDuelManager().getDuel(target.getUniqueId());
        if (duel == null) {
            spectator.sendMessage(Component.text(target.getName() + " is not in a duel.", NamedTextColor.RED));
            return;
        }

        if (plugin.getDuelManager().isInDuel(spectator.getUniqueId())) {
            spectator.sendMessage(Component.text("You can't spectate while in a duel!", NamedTextColor.RED));
            return;
        }

        // Save spectator state
        plugin.getDuelManager().addSpectator(spectator, duel);

        // Set spectator mode and teleport
        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.teleport(duel.getArena().getSpawn1().add(0, 10, 0));
        spectator.sendMessage(Component.text("Spectating duel. Use /duel leave to stop spectating.", NamedTextColor.GREEN));
    }

    private void handleLeaveSpectate(Player spectator) {
        if (plugin.getDuelManager().removeSpectator(spectator)) {
            spectator.sendMessage(Component.text("Stopped spectating.", NamedTextColor.GREEN));
        } else {
            spectator.sendMessage(Component.text("You're not spectating a duel.", NamedTextColor.RED));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("accept");
            completions.add("deny");
            completions.add("spectate");
            completions.add("leave");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender)) completions.add(p.getName());
            }
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spectate")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getDuelManager().isInDuel(p.getUniqueId())) {
                    completions.add(p.getName());
                }
            }
            completions.removeIf(s -> !s.toLowerCase().startsWith(args[1].toLowerCase()));
        }
        return completions;
    }
}
