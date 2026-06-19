package com.prismsmp.duels.listeners;

import com.prismsmp.duels.PrismDuels;
import com.prismsmp.duels.arena.Arena;
import com.prismsmp.duels.duel.ActiveDuel;
import com.prismsmp.duels.kit.GameMode;
import com.prismsmp.duels.kit.GameMode.BlockRule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;

public class DuelListener implements Listener {
    private final PrismDuels plugin;

    private static final Set<Material> PROTECTED_MATERIALS = Set.of(
            Material.GRAY_STAINED_GLASS, Material.BEDROCK, Material.BARRIER
    );

    private static final Set<String> BLOCKED_COMMANDS = Set.of(
            "spawn", "home", "sethome", "tp", "tpa", "tpaccept", "tpyes",
            "tpask", "tpno", "rtp", "back", "warp", "wild", "tphere",
            "tpahere", "mvtp", "essentials:tp", "essentials:tpa",
            "essentials:home", "essentials:spawn", "essentials:back"
    );

    public DuelListener(PrismDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        ActiveDuel duel = plugin.getDuelManager().getDuel(dead.getUniqueId());
        if (duel == null || !duel.isStarted() || duel.isEnded()) return;
        event.deathMessage(null);
        if (duel.getType() == com.prismsmp.duels.duel.DuelType.PRACTICE) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepLevel(true);
        }
        plugin.getDuelManager().endDuel(duel, duel.getOpponent(dead.getUniqueId()), dead.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getDuelManager().isInDuel(uuid)) plugin.getDuelManager().handleDisconnect(uuid);
        if (plugin.getDuelManager().isSpectating(uuid)) plugin.getDuelManager().removeSpectator(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getDuelManager().handleJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDuelManager().isInDuel(player.getUniqueId())) return;
        String command = event.getMessage().substring(1).split(" ")[0].toLowerCase();
        if (command.equals("duel") || command.equals("duelstats") || command.equals("practiceduel")) return;
        if (player.hasPermission("prismduel.admin") && command.equals("duels")) return;
        if (BLOCKED_COMMANDS.contains(command)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You can't use that command during a duel!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        ActiveDuel duel = plugin.getDuelManager().getDuel(victim.getUniqueId());
        if (duel != null && !duel.isStarted()) event.setCancelled(true);
    }

    // ==================== BLOCK RULES ====================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            if (player.isOp()) return;
            Arena arena = plugin.getArenaManager().getArenaAt(block.getLocation());
            if (arena != null) event.setCancelled(true);
            return;
        }

        ActiveDuel duel = plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null || !duel.isStarted()) { event.setCancelled(true); return; }

        Arena arena = duel.getArena();
        if (!arena.isInBounds(block.getLocation())) { event.setCancelled(true); return; }

        BlockRule rule = getBlockRule(duel);
        if (rule == BlockRule.NONE) { event.setCancelled(true); return; }
        if (PROTECTED_MATERIALS.contains(block.getType())) { event.setCancelled(true); return; }

        duel.addPlacedBlock(block.getX(), block.getY(), block.getZ());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            if (player.isOp()) return;
            Arena arena = plugin.getArenaManager().getArenaAt(block.getLocation());
            if (arena != null) event.setCancelled(true);
            return;
        }

        ActiveDuel duel = plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null || !duel.isStarted()) { event.setCancelled(true); return; }

        Arena arena = duel.getArena();
        if (!arena.isInBounds(block.getLocation())) { event.setCancelled(true); return; }
        if (PROTECTED_MATERIALS.contains(block.getType())) { event.setCancelled(true); return; }

        BlockRule rule = getBlockRule(duel);
        switch (rule) {
            case NONE -> event.setCancelled(true);
            case PLAYER_BLOCKS_ONLY -> {
                if (!duel.isPlayerPlaced(block.getX(), block.getY(), block.getZ())) {
                    event.setCancelled(true);
                } else {
                    duel.removePlacedBlock(block.getX(), block.getY(), block.getZ());
                }
            }
            case FULL_BREAK -> {
                // Save original block data before it's broken
                duel.addBrokenBlock(block.getX(), block.getY(), block.getZ(), block.getBlockData().getAsString());
                duel.removePlacedBlock(block.getX(), block.getY(), block.getZ());
            }
        }
    }

    /**
     * Track blocks destroyed by entity explosions (end crystals, TNT, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Find if this explosion is in an arena with an active duel
        Arena arena = plugin.getArenaManager().getArenaAt(event.getLocation());
        if (arena == null) return;

        // Find the active duel in this arena
        ActiveDuel duel = findDuelInArena(arena);
        if (duel == null) return;

        // Protect dome/bedrock/barriers from explosions
        event.blockList().removeIf(block -> PROTECTED_MATERIALS.contains(block.getType()));

        // Track all blocks that will be destroyed
        for (Block block : event.blockList()) {
            if (arena.isInBounds(block.getLocation())) {
                duel.addBrokenBlock(block.getX(), block.getY(), block.getZ(), block.getBlockData().getAsString());
            }
        }

        // Remove blocks outside arena bounds from explosion
        event.blockList().removeIf(block -> !arena.isInBounds(block.getLocation()));
    }

    /**
     * Track blocks destroyed by block explosions (respawn anchors, beds)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        Arena arena = plugin.getArenaManager().getArenaAt(event.getBlock().getLocation());
        if (arena == null) return;

        ActiveDuel duel = findDuelInArena(arena);
        if (duel == null) return;

        event.blockList().removeIf(block -> PROTECTED_MATERIALS.contains(block.getType()));

        for (Block block : event.blockList()) {
            if (arena.isInBounds(block.getLocation())) {
                duel.addBrokenBlock(block.getX(), block.getY(), block.getZ(), block.getBlockData().getAsString());
            }
        }

        event.blockList().removeIf(block -> !arena.isInBounds(block.getLocation()));
    }

    private ActiveDuel findDuelInArena(Arena arena) {
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            ActiveDuel duel = plugin.getDuelManager().getDuel(p.getUniqueId());
            if (duel != null && duel.getArena() == arena && duel.isStarted() && !duel.isEnded()) {
                return duel;
            }
        }
        return null;
    }

    private BlockRule getBlockRule(ActiveDuel duel) {
        if (duel.getType() == com.prismsmp.duels.duel.DuelType.REGULAR) return BlockRule.PLAYER_BLOCKS_ONLY;
        GameMode gm = duel.getGameMode();
        if (gm == null) return BlockRule.NONE;
        return gm.getBlockRule();
    }
}
