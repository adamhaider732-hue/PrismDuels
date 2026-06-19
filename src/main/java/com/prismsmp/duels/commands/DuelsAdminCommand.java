package com.prismsmp.duels.commands;

import com.prismsmp.duels.PrismDuels;
import com.prismsmp.duels.arena.Arena;
import com.prismsmp.duels.arena.ArenaGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DuelsAdminCommand implements CommandExecutor, TabCompleter {
    private final PrismDuels plugin;

    public DuelsAdminCommand(PrismDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "enable" -> {
                plugin.getDuelManager().setDuelsEnabled(true);
                sender.sendMessage(Component.text("Duels have been enabled.", NamedTextColor.GREEN));
            }
            case "disable" -> {
                plugin.getDuelManager().setDuelsEnabled(false);
                sender.sendMessage(Component.text("Duels have been disabled. Active duels will continue.", NamedTextColor.RED));
            }
            case "generate" -> {
                handleGenerate(sender, args);
            }
            case "removearena" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /duels removearena <name>", NamedTextColor.RED));
                    return true;
                }
                handleRemoveArena(sender, args[1]);
            }
            case "listarenas", "arenas" -> {
                handleListArenas(sender);
            }
            case "reload" -> {
                plugin.getArenaManager().loadArenas();
                plugin.getKitManager().loadKits();
                sender.sendMessage(Component.text("PrismDuels config reloaded.", NamedTextColor.GREEN));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleGenerate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /duels generate <world_name>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /duels generate duels_world", NamedTextColor.GRAY));
            return;
        }

        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(Component.text("World '" + worldName + "' not found!", NamedTextColor.RED));
            sender.sendMessage(Component.text("Make sure you created it with /mv create " + worldName + " NORMAL -g VoidWorld", NamedTextColor.GRAY));
            return;
        }

        if (plugin.getArenaManager().hasArenas()) {
            sender.sendMessage(Component.text("Arenas already exist! Remove them first with /duels removearena <name> if you want to regenerate.", NamedTextColor.RED));
            sender.sendMessage(Component.text("Current arenas:", NamedTextColor.GRAY));
            for (Arena a : plugin.getArenaManager().getAllArenas()) {
                sender.sendMessage(Component.text(" - " + a.getName() + " (" + a.getTheme().getDisplayName() + ")", NamedTextColor.YELLOW));
            }
            return;
        }

        sender.sendMessage(Component.text("Generating 5 duel arenas in " + worldName + "...", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("This may take a few seconds. The server might lag briefly.", NamedTextColor.GRAY));

        // Run on next tick to let the message send first
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                ArenaGenerator generator = new ArenaGenerator(plugin);
                generator.generateAllArenas(world);
                sender.sendMessage(Component.empty());
                sender.sendMessage(Component.text("All 8 arenas generated!", NamedTextColor.GREEN, TextDecoration.BOLD));
                sender.sendMessage(Component.text("5 small circular arenas (Sword, Axe, DiaPot, UHC)", NamedTextColor.GRAY));
                sender.sendMessage(Component.text("3 large terrain arenas with domes (Crystal, SpearMace, Mace, SMP, DiaSMP, Regular)", NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Players can now duel!", NamedTextColor.GREEN));
            } catch (Exception e) {
                sender.sendMessage(Component.text("Error generating arenas: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().severe("Arena generation failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, 2L);
    }

    private void handleRemoveArena(CommandSender sender, String name) {
        if (name.equalsIgnoreCase("all")) {
            List<String> names = new ArrayList<>();
            for (Arena a : plugin.getArenaManager().getAllArenas()) {
                names.add(a.getName());
            }
            for (String n : names) {
                plugin.getArenaManager().removeArena(n);
            }
            sender.sendMessage(Component.text("Removed all " + names.size() + " arena(s).", NamedTextColor.GREEN));
            return;
        }

        if (plugin.getArenaManager().removeArena(name)) {
            sender.sendMessage(Component.text("Arena '" + name + "' removed.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Arena '" + name + "' not found.", NamedTextColor.RED));
        }
    }

    private void handleListArenas(CommandSender sender) {
        var arenas = plugin.getArenaManager().getAllArenas();
        if (arenas.isEmpty()) {
            sender.sendMessage(Component.text("No arenas configured.", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Use /duels generate <world> to auto-generate 5 arenas.", NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("Duel Arenas (" + arenas.size() + "):", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (Arena arena : arenas) {
            NamedTextColor statusColor = arena.isInUse() ? NamedTextColor.RED : NamedTextColor.GREEN;
            String status = arena.isInUse() ? "IN USE" : "AVAILABLE";
            sender.sendMessage(Component.text(" - " + arena.getName(), NamedTextColor.YELLOW)
                    .append(Component.text(" [" + arena.getTheme().getDisplayName() + "] ", NamedTextColor.AQUA))
                    .append(Component.text("[" + status + "]", statusColor)));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("PrismDuels Admin Commands", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("/duels enable", NamedTextColor.AQUA).append(Component.text(" - Enable duels", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duels disable", NamedTextColor.AQUA).append(Component.text(" - Disable new duels", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duels generate <world>", NamedTextColor.AQUA).append(Component.text(" - Generate 5 arenas in a world", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duels removearena <name|all>", NamedTextColor.AQUA).append(Component.text(" - Remove arena(s)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duels listarenas", NamedTextColor.AQUA).append(Component.text(" - List all arenas", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duels reload", NamedTextColor.AQUA).append(Component.text(" - Reload config", NamedTextColor.GRAY)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List.of("enable", "disable", "generate", "removearena", "listarenas", "reload")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).forEach(completions::add);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("generate")) {
                Bukkit.getWorlds().stream().map(World::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .forEach(completions::add);
            } else if (args[0].equalsIgnoreCase("removearena")) {
                completions.add("all");
                plugin.getArenaManager().getAllArenas().stream()
                        .map(Arena::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .forEach(completions::add);
            }
        }
        return completions;
    }
}
