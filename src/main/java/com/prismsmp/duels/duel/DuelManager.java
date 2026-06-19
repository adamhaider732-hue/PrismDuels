package com.prismsmp.duels.duel;

import com.prismsmp.duels.PrismDuels;
import com.prismsmp.duels.arena.Arena;
import com.prismsmp.duels.kit.GameMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {
    private final PrismDuels plugin;
    private final Map<UUID, DuelRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveDuel> activeDuels = new ConcurrentHashMap<>();
    private final Map<UUID, Location> spectatorOrigins = new ConcurrentHashMap<>();
    private final Map<UUID, org.bukkit.GameMode> spectatorGameModes = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveDuel> spectatorDuels = new ConcurrentHashMap<>();
    private final Map<UUID, PendingRestore> pendingRestores = new ConcurrentHashMap<>();
    private final Set<UUID> lootCollectors = ConcurrentHashMap.newKeySet(); // players collecting loot after regular duel
    private boolean duelsEnabled = true;

    // Stores inventory data for players who disconnected during a practice duel
    private static class PendingRestore {
        final ItemStack[] inventory;
        final ItemStack[] armor;
        final ItemStack offhand;
        final Location origin;
        PendingRestore(ItemStack[] inv, ItemStack[] armor, ItemStack off, Location origin) {
            this.inventory = inv; this.armor = armor; this.offhand = off; this.origin = origin;
        }
    }

    public DuelManager(PrismDuels plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    public boolean isDuelsEnabled() { return duelsEnabled; }
    public void setDuelsEnabled(boolean enabled) { this.duelsEnabled = enabled; }
    public boolean isInDuel(UUID uuid) { return activeDuels.containsKey(uuid); }
    public ActiveDuel getDuel(UUID uuid) { return activeDuels.get(uuid); }
    public boolean hasPendingRequest(UUID uuid) { return pendingRequests.containsKey(uuid); }

    // ==================== SPECTATOR MANAGEMENT ====================

    public void addSpectator(Player spectator, ActiveDuel duel) {
        spectatorOrigins.put(spectator.getUniqueId(), spectator.getLocation().clone());
        spectatorGameModes.put(spectator.getUniqueId(), spectator.getGameMode());
        spectatorDuels.put(spectator.getUniqueId(), duel);
    }

    public boolean removeSpectator(Player spectator) {
        UUID uuid = spectator.getUniqueId();
        if (!spectatorOrigins.containsKey(uuid)) return false;
        Location origin = spectatorOrigins.remove(uuid);
        org.bukkit.GameMode gm = spectatorGameModes.remove(uuid);
        spectatorDuels.remove(uuid);
        if (gm != null) spectator.setGameMode(gm);
        if (origin != null) spectator.teleport(origin);
        return true;
    }

    private void removeAllSpectators(ActiveDuel duel) {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, ActiveDuel> entry : spectatorDuels.entrySet()) {
            if (entry.getValue() == duel) toRemove.add(entry.getKey());
        }
        for (UUID uuid : toRemove) {
            Player spec = Bukkit.getPlayer(uuid);
            if (spec != null && spec.isOnline()) {
                removeSpectator(spec);
                spec.sendMessage(Component.text("The duel has ended.", NamedTextColor.GRAY));
            } else {
                spectatorOrigins.remove(uuid);
                spectatorGameModes.remove(uuid);
                spectatorDuels.remove(uuid);
            }
        }
    }

    public boolean isSpectating(UUID uuid) { return spectatorDuels.containsKey(uuid); }

    // ==================== DUEL REQUESTS ====================

    public void sendRequest(Player sender, Player target, DuelType type, GameMode gameMode) {
        if (!duelsEnabled) {
            sender.sendMessage(Component.text("Duels are currently disabled.", NamedTextColor.RED));
            return;
        }
        if (isInDuel(sender.getUniqueId())) {
            sender.sendMessage(Component.text("You are already in a duel!", NamedTextColor.RED));
            return;
        }
        if (isInDuel(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is already in a duel!", NamedTextColor.RED));
            return;
        }
        if (lootCollectors.contains(sender.getUniqueId())) {
            sender.sendMessage(Component.text("You're still collecting loot! Wait for the timer to end.", NamedTextColor.RED));
            return;
        }
        if (lootCollectors.contains(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is collecting loot from a duel!", NamedTextColor.RED));
            return;
        }
        if (sender.equals(target)) {
            sender.sendMessage(Component.text("You can't duel yourself!", NamedTextColor.RED));
            return;
        }

        // Check arena availability for the required size
        GameMode.ArenaSize requiredSize = (type == DuelType.REGULAR)
                ? GameMode.ArenaSize.LARGE
                : (gameMode != null ? gameMode.getArenaSize() : GameMode.ArenaSize.LARGE);

        Arena arena = plugin.getArenaManager().getAvailableArena(requiredSize);
        if (arena == null) {
            sender.sendMessage(Component.text("No arenas are available right now. Try again shortly.", NamedTextColor.RED));
            return;
        }

        DuelRequest request = new DuelRequest(sender.getUniqueId(), target.getUniqueId(), type, gameMode);
        pendingRequests.put(target.getUniqueId(), request);

        String typeLabel = type == DuelType.PRACTICE
                ? "practice duel (" + gameMode.getDisplayName() + ")"
                : "duel (real inventory)";

        sender.sendMessage(Component.text("Duel request sent to " + target.getName() + "!", NamedTextColor.GREEN));

        // Use unique command format to avoid InteractiveChat conflicts
        Component acceptBtn = Component.text("[ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/duel accept"));
        Component denyBtn = Component.text("[DENY]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/duel deny"));

        target.sendMessage(Component.empty());
        target.sendMessage(Component.text(sender.getName(), NamedTextColor.GOLD)
                .append(Component.text(" has challenged you to a ", NamedTextColor.YELLOW))
                .append(Component.text(typeLabel, NamedTextColor.GOLD))
                .append(Component.text("!", NamedTextColor.YELLOW)));
        target.sendMessage(Component.text("   ").append(acceptBtn).append(Component.text("   ")).append(denyBtn));
        target.sendMessage(Component.text("Request expires in 30 seconds.", NamedTextColor.GRAY));
        target.sendMessage(Component.empty());

        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelRequest pending = pendingRequests.get(target.getUniqueId());
            if (pending != null && pending.getSender().equals(sender.getUniqueId())) {
                pendingRequests.remove(target.getUniqueId());
                Player s = Bukkit.getPlayer(sender.getUniqueId());
                Player t = Bukkit.getPlayer(target.getUniqueId());
                if (s != null) s.sendMessage(Component.text("Duel request to " + target.getName() + " expired.", NamedTextColor.GRAY));
                if (t != null) t.sendMessage(Component.text("Duel request from " + sender.getName() + " expired.", NamedTextColor.GRAY));
            }
        }, 600L);
    }

    public void acceptRequest(Player target) {
        DuelRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null || request.isExpired()) {
            target.sendMessage(Component.text("You don't have any pending duel requests.", NamedTextColor.RED));
            return;
        }
        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(Component.text("The challenger is no longer online.", NamedTextColor.RED));
            return;
        }
        if (isInDuel(sender.getUniqueId()) || isInDuel(target.getUniqueId())) {
            target.sendMessage(Component.text("One of you is already in a duel!", NamedTextColor.RED));
            return;
        }
        if (lootCollectors.contains(sender.getUniqueId()) || lootCollectors.contains(target.getUniqueId())) {
            target.sendMessage(Component.text("One of you is still collecting loot!", NamedTextColor.RED));
            return;
        }

        GameMode.ArenaSize requiredSize = (request.getType() == DuelType.REGULAR)
                ? GameMode.ArenaSize.LARGE
                : (request.getGameMode() != null ? request.getGameMode().getArenaSize() : GameMode.ArenaSize.LARGE);

        Arena arena = plugin.getArenaManager().getAvailableArena(requiredSize);
        if (arena == null) {
            target.sendMessage(Component.text("No arenas are available right now.", NamedTextColor.RED));
            sender.sendMessage(Component.text("No arenas are available right now.", NamedTextColor.RED));
            return;
        }

        startDuel(sender, target, request.getType(), request.getGameMode(), arena);
    }

    public void denyRequest(Player target) {
        DuelRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(Component.text("You don't have any pending duel requests.", NamedTextColor.RED));
            return;
        }
        target.sendMessage(Component.text("Duel request denied.", NamedTextColor.GRAY));
        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender != null) sender.sendMessage(Component.text(target.getName() + " denied your duel request.", NamedTextColor.RED));
    }

    // ==================== DUEL LIFECYCLE ====================

    private void startDuel(Player p1, Player p2, DuelType type, GameMode gameMode, Arena arena) {
        arena.setInUse(true);

        var p1Origin = p1.getLocation().clone();
        var p2Origin = p2.getLocation().clone();

        ItemStack[] p1Inv = cloneContents(p1.getInventory().getContents());
        ItemStack[] p1Armor = cloneContents(p1.getInventory().getArmorContents());
        ItemStack p1Off = p1.getInventory().getItemInOffHand().clone();
        ItemStack[] p2Inv = cloneContents(p2.getInventory().getContents());
        ItemStack[] p2Armor = cloneContents(p2.getInventory().getArmorContents());
        ItemStack p2Off = p2.getInventory().getItemInOffHand().clone();

        ActiveDuel duel = new ActiveDuel(p1.getUniqueId(), p2.getUniqueId(), type, gameMode,
                arena, p1Origin, p2Origin, p1Inv, p1Armor, p1Off, p2Inv, p2Armor, p2Off);

        activeDuels.put(p1.getUniqueId(), duel);
        activeDuels.put(p2.getUniqueId(), duel);

        p1.teleport(arena.getSpawn1());
        p2.teleport(arena.getSpawn2());

        if (type == DuelType.PRACTICE && gameMode != null) {
            plugin.getKitManager().applyKit(p1, gameMode);
            plugin.getKitManager().applyKit(p2, gameMode);
        }

        // Heal and prep both players
        prepPlayer(p1);
        prepPlayer(p2);

        String modeLabel = type == DuelType.PRACTICE ? gameMode.getDisplayName() : "Regular";
        runCountdown(p1, p2, duel, modeLabel);
    }

    private void prepPlayer(Player p) {
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
    }

    private void runCountdown(Player p1, Player p2, ActiveDuel duel, String modeLabel) {
        p1.setInvulnerable(true);
        p2.setInvulnerable(true);

        // Glowing so players can see each other across the arena
        p1.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.GLOWING, 100, 0, false, false, false));
        p2.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.GLOWING, 100, 0, false, false, false));

        new BukkitRunnable() {
            int count = 3;
            @Override
            public void run() {
                if (duel.isEnded()) { p1.setInvulnerable(false); p2.setInvulnerable(false); cancel(); return; }
                if (count > 0) {
                    NamedTextColor color = count == 3 ? NamedTextColor.RED : count == 2 ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
                    Title title = Title.title(Component.text(String.valueOf(count), color, TextDecoration.BOLD),
                            Component.text(modeLabel + " Duel", NamedTextColor.GRAY),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO));
                    showTitle(p1, title); showTitle(p2, title);
                    playSound(p1, Sound.BLOCK_NOTE_BLOCK_HAT); playSound(p2, Sound.BLOCK_NOTE_BLOCK_HAT);
                    count--;
                } else {
                    Title title = Title.title(Component.text("FIGHT!", NamedTextColor.GREEN, TextDecoration.BOLD),
                            Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(300)));
                    showTitle(p1, title); showTitle(p2, title);
                    playSound(p1, Sound.ENTITY_ENDER_DRAGON_GROWL); playSound(p2, Sound.ENTITY_ENDER_DRAGON_GROWL);
                    p1.setInvulnerable(false); p2.setInvulnerable(false);
                    // Remove glowing
                    p1.removePotionEffect(org.bukkit.potion.PotionEffectType.GLOWING);
                    p2.removePotionEffect(org.bukkit.potion.PotionEffectType.GLOWING);
                    duel.setStarted(true);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void endDuel(ActiveDuel duel, UUID winnerUUID, UUID loserUUID) {
        if (duel.isEnded()) return;
        duel.setEnded(true);

        Arena arena = duel.getArena();
        activeDuels.remove(duel.getPlayer1());
        activeDuels.remove(duel.getPlayer2());

        Player winner = Bukkit.getPlayer(winnerUUID);
        Player loser = Bukkit.getPlayer(loserUUID);

        // Stats
        var winnerStats = plugin.getStatsManager().getStats(winnerUUID);
        var loserStats = plugin.getStatsManager().getStats(loserUUID);
        if (duel.getType() == DuelType.REGULAR) { winnerStats.addRegularWin(); loserStats.addRegularLoss(); }
        else { winnerStats.addPracticeWin(duel.getGameMode()); loserStats.addPracticeLoss(duel.getGameMode()); }
        plugin.getStatsManager().saveStats(winnerUUID);
        plugin.getStatsManager().saveStats(loserUUID);

        // Lightning effect
        if (loser != null && loser.isOnline()) loser.getWorld().strikeLightningEffect(loser.getLocation());

        String winnerName = winner != null ? winner.getName() : "Unknown";
        String loserName = loser != null ? loser.getName() : "Unknown";
        String duelLabel = duel.getType() == DuelType.PRACTICE ? duel.getGameMode().getDisplayName() + " practice duel" : "duel";

        Component msg = Component.text(winnerName, NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text(" defeated ", NamedTextColor.GRAY))
                .append(Component.text(loserName, NamedTextColor.RED))
                .append(Component.text(" in a ", NamedTextColor.GRAY))
                .append(Component.text(duelLabel, NamedTextColor.GOLD))
                .append(Component.text("!", NamedTextColor.GRAY));
        Bukkit.broadcast(msg);

        // Remove spectators
        removeAllSpectators(duel);

        // Clear combat tags immediately
        if (winner != null && winner.isOnline()) clearCombatTag(winner);
        if (loser != null && loser.isOnline()) clearCombatTag(loser);

        if (duel.getType() == DuelType.PRACTICE) {
            // Practice: instant return, restore inventory
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                restoreAndReturn(winner, winnerUUID, duel, true);
                restoreAndReturn(loser, loserUUID, duel, true);
                resetArenaDelayed(arena, duel);
            }, 40L);
        } else {
            // Regular: loser goes to spawn immediately, winner gets 60s to loot
            lootCollectors.add(winnerUUID);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Send loser to their respawn point
                if (loser != null && loser.isOnline()) {
                    prepPlayer(loser);
                    clearCombatTag(loser);
                    Location respawn = loser.getRespawnLocation();
                    if (respawn == null) {
                        respawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                    }
                    safeTeleport(loser, respawn);
                    loser.sendMessage(Component.text("You lost the duel.", NamedTextColor.RED));
                }

                // Tell winner they have 60s
                if (winner != null && winner.isOnline()) {
                    winner.sendMessage(Component.text("You won! You have 60 seconds to collect loot.", NamedTextColor.GREEN));
                }
            }, 40L);

            // After 60 seconds, return winner and reset
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                lootCollectors.remove(winnerUUID);
                if (winner != null && winner.isOnline()) {
                    prepPlayer(winner);
                    clearCombatTag(winner);
                    safeTeleport(winner, duel.getOrigin(winnerUUID));
                    winner.sendMessage(Component.text("Loot time over! You've been teleported back.", NamedTextColor.YELLOW));
                }
                arena.setInUse(false);
                resetArenaDelayed(arena, duel);
            }, 40L + 1200L); // 2s + 60s
        }
    }

    private void restoreAndReturn(Player player, UUID uuid, ActiveDuel duel, boolean restoreInv) {
        if (restoreInv && duel.getType() == DuelType.PRACTICE) {
            if (player == null || !player.isOnline()) {
                // Player is offline - save pending restore for when they rejoin
                pendingRestores.put(uuid, new PendingRestore(
                        duel.getSavedInventory(uuid), duel.getSavedArmor(uuid),
                        duel.getSavedOffhand(uuid), duel.getOrigin(uuid)));
                return;
            }
            player.getInventory().clear();
            player.getInventory().setContents(duel.getSavedInventory(uuid));
            player.getInventory().setArmorContents(duel.getSavedArmor(uuid));
            player.getInventory().setItemInOffHand(duel.getSavedOffhand(uuid));
        }
        if (player == null || !player.isOnline()) return;
        prepPlayer(player);
        clearCombatTag(player);
        safeTeleport(player, duel.getOrigin(uuid));
    }

    /**
     * Called on PlayerJoinEvent - restores inventory if player disconnected during a practice duel.
     */
    public void handleJoin(UUID uuid) {
        // Check for pending inventory restores first
        PendingRestore restore = pendingRestores.remove(uuid);
        if (restore != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.getInventory().clear();
                    player.getInventory().setContents(restore.inventory);
                    player.getInventory().setArmorContents(restore.armor);
                    player.getInventory().setItemInOffHand(restore.offhand);
                    prepPlayer(player);
                    clearCombatTag(player);
                    safeTeleport(player, restore.origin);
                    player.sendMessage(Component.text("Your inventory has been restored from your last duel.", NamedTextColor.GREEN));
                }, 10L); // small delay to let login finish
            }
            return;
        }

        // Check for active duel reconnect
        if (isInDuel(uuid)) {
            handleReconnect(uuid);
        }
    }

    /**
     * Clear EternalCombat combat tag so players can RTP/logout safely after duels.
     */
    private void clearCombatTag(Player player) {
        if (player == null || !player.isOnline()) return;
        try {
            // EternalCombat uses /combatlog untag
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "combatlog untag " + player.getName());
        } catch (Exception e) {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ec untag " + player.getName());
            } catch (Exception ignored) {}
        }
    }

    private void resetArenaDelayed(Arena arena) {
        resetArenaDelayed(arena, null);
    }

    private void resetArenaDelayed(Arena arena, ActiveDuel duel) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (duel != null && duel.hasBlockChanges()) {
                // Targeted restore - only fix blocks that actually changed
                org.bukkit.World world = arena.getSpawn1().getWorld();
                if (world != null) {
                    // Restore broken blocks to their original state
                    for (var entry : duel.getOriginalBlocks().entrySet()) {
                        int[] coords = unpackCoords(entry.getKey());
                        int x = coords[0], y = coords[1], z = coords[2];
                        try {
                            org.bukkit.block.data.BlockData data = Bukkit.createBlockData(entry.getValue());
                            world.getBlockAt(x, y, z).setBlockData(data, false);
                        } catch (Exception e) {
                            world.getBlockAt(x, y, z).setType(org.bukkit.Material.AIR, false);
                        }
                    }
                    // Remove player-placed blocks (set to air)
                    for (Long key : duel.getPlacedBlocks()) {
                        int[] coords = unpackCoords(key);
                        world.getBlockAt(coords[0], coords[1], coords[2]).setType(org.bukkit.Material.AIR, false);
                    }
                }
            }
            // Always clean entities (dropped items, crystals, arrows, etc.)
            plugin.getArenaManager().cleanEntities(arena);
            arena.setInUse(false);
        }, 20L);
    }

    private static int[] unpackCoords(long packed) {
        int x = (int) ((packed >> 38) & 0x3FFFFFF);
        if (x >= 0x2000000) x -= 0x4000000; // sign extend
        int z = (int) ((packed >> 12) & 0x3FFFFFF);
        if (z >= 0x2000000) z -= 0x4000000;
        int y = (int) (packed & 0xFFF);
        return new int[]{x, y, z};
    }

    // ==================== DISCONNECT / RECONNECT ====================

    public void handleDisconnect(UUID uuid) {
        ActiveDuel duel = activeDuels.get(uuid);
        if (duel == null || duel.isEnded()) return;
        if (!duel.isStarted()) { cancelDuel(duel, "Player disconnected."); return; }

        UUID opponent = duel.getOpponent(uuid);
        Player opp = Bukkit.getPlayer(opponent);

        // Regular duels: drop items immediately, opponent wins instantly
        if (duel.getType() == DuelType.REGULAR) {
            // Drop the disconnected player's items at the arena
            Player disconnected = Bukkit.getPlayer(uuid);
            if (disconnected != null) {
                Location dropLoc = disconnected.getLocation();
                for (ItemStack item : disconnected.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                    }
                }
                for (ItemStack item : disconnected.getInventory().getArmorContents()) {
                    if (item != null && !item.getType().isAir()) {
                        dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                    }
                }
                ItemStack offhand = disconnected.getInventory().getItemInOffHand();
                if (!offhand.getType().isAir()) {
                    dropLoc.getWorld().dropItemNaturally(dropLoc, offhand);
                }
            }
            endDuel(duel, opponent, uuid);
            return;
        }

        // Practice duels: 30 second reconnect window
        duel.setDisconnectedPlayer(uuid);
        if (opp != null) opp.sendMessage(Component.text("Your opponent disconnected. 30 seconds to reconnect.", NamedTextColor.YELLOW));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (duel.isEnded()) return;
            Player reconnected = Bukkit.getPlayer(uuid);
            if (reconnected == null || !reconnected.isOnline()) {
                endDuel(duel, opponent, uuid);
            } else {
                duel.setDisconnectedPlayer(null);
                if (opp != null && opp.isOnline()) opp.sendMessage(Component.text("Opponent reconnected!", NamedTextColor.GREEN));
            }
        }, 600L);
    }

    public void handleReconnect(UUID uuid) {
        ActiveDuel duel = activeDuels.get(uuid);
        if (duel == null || duel.isEnded()) return;
        if (uuid.equals(duel.getDisconnectedPlayer())) {
            duel.setDisconnectedPlayer(null);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleport(duel.getArena().getSpawn1());
                player.sendMessage(Component.text("Reconnected to your duel!", NamedTextColor.GREEN));
            }
        }
    }

    public void cancelDuel(ActiveDuel duel, String reason) {
        if (duel.isEnded()) return;
        duel.setEnded(true);
        Arena arena = duel.getArena();
        activeDuels.remove(duel.getPlayer1());
        activeDuels.remove(duel.getPlayer2());
        removeAllSpectators(duel);
        for (UUID uuid : List.of(duel.getPlayer1(), duel.getPlayer2())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                restoreAndReturn(p, uuid, duel, duel.getType() == DuelType.PRACTICE);
                clearCombatTag(p);
                p.sendMessage(Component.text("Duel cancelled: " + reason, NamedTextColor.RED));
            }
        }
        resetArenaDelayed(arena);
    }

    // ==================== UTIL ====================

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] cloned = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) cloned[i] = contents[i] != null ? contents[i].clone() : null;
        return cloned;
    }

    private void showTitle(Player p, Title t) { if (p != null && p.isOnline()) p.showTitle(t); }
    private void playSound(Player p, Sound s) { if (p != null && p.isOnline()) p.playSound(p.getLocation(), s, 1f, 1f); }

    /**
     * Teleport player to a safe location. Searches upward if the target is inside solid blocks.
     */
    private void safeTeleport(Player player, Location loc) {
        if (player == null || !player.isOnline() || loc == null) return;
        Location safe = loc.clone();
        org.bukkit.World world = safe.getWorld();
        if (world == null) { player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()); return; }

        // Check if location is in void
        if (safe.getY() < world.getMinHeight()) {
            safe.setY(world.getSpawnLocation().getY());
        }

        // Search upward for safe spot (feet block and head block must be non-solid)
        for (int i = 0; i < 20; i++) {
            org.bukkit.block.Block feet = world.getBlockAt(safe.getBlockX(), safe.getBlockY() + i, safe.getBlockZ());
            org.bukkit.block.Block head = world.getBlockAt(safe.getBlockX(), safe.getBlockY() + i + 1, safe.getBlockZ());
            if (!feet.getType().isSolid() && !head.getType().isSolid()) {
                safe.setY(safe.getBlockY() + i + 0.1);
                player.teleport(safe);
                return;
            }
        }

        // If no safe spot found within 20 blocks up, teleport to world spawn
        player.teleport(world.getSpawnLocation());
    }

    private void startCleanupTask() {
        // Clean expired requests every 10 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, () -> pendingRequests.entrySet().removeIf(e -> e.getValue().isExpired()), 200L, 200L);

        // Safeguard: auto-TP non-staff players out of arenas when no duel is happening
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Arena arena : plugin.getArenaManager().getAllArenas()) {
                if (arena.isInUse()) continue; // Duel active, skip
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                    if (player.hasPermission("prismduel.admin")) continue;
                    if (isSpectating(player.getUniqueId())) continue;
                    if (lootCollectors.contains(player.getUniqueId())) continue;
                    if (arena.isInBounds(player.getLocation())) {
                        safeTeleport(player, Bukkit.getWorlds().get(0).getSpawnLocation());
                        player.sendMessage(Component.text("You've been teleported out of the duel arena.", NamedTextColor.YELLOW));
                    }
                }
            }
        }, 100L, 100L); // Check every 5 seconds
    }

    public void cleanupAll() {
        for (ActiveDuel duel : new HashSet<>(activeDuels.values())) cancelDuel(duel, "Server shutting down.");
        // Return all spectators
        for (UUID uuid : new HashSet<>(spectatorOrigins.keySet())) {
            Player spec = Bukkit.getPlayer(uuid);
            if (spec != null) removeSpectator(spec);
        }
        pendingRequests.clear();
        lootCollectors.clear();
    }
}
