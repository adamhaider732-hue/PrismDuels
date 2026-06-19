package com.prismsmp.duels.duel;

import com.prismsmp.duels.arena.Arena;
import com.prismsmp.duels.kit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ActiveDuel {
    private final UUID player1;
    private final UUID player2;
    private final DuelType type;
    private final GameMode gameMode; // null for regular
    private final Arena arena;
    private final Location player1Origin;
    private final Location player2Origin;
    private final ItemStack[] player1SavedInventory;
    private final ItemStack[] player1SavedArmor;
    private final ItemStack player1SavedOffhand;
    private final ItemStack[] player2SavedInventory;
    private final ItemStack[] player2SavedArmor;
    private final ItemStack player2SavedOffhand;
    private boolean started;
    private boolean ended;
    private long startTime;
    private UUID disconnectedPlayer;
    private long disconnectTime;
    private final Set<Long> playerPlacedBlocks = new HashSet<>();
    private final Map<Long, String> originalBlocks = new HashMap<>(); // blocks broken/exploded during duel

    public ActiveDuel(UUID player1, UUID player2, DuelType type, GameMode gameMode,
                      Arena arena, Location p1Origin, Location p2Origin,
                      ItemStack[] p1Inv, ItemStack[] p1Armor, ItemStack p1Off,
                      ItemStack[] p2Inv, ItemStack[] p2Armor, ItemStack p2Off) {
        this.player1 = player1;
        this.player2 = player2;
        this.type = type;
        this.gameMode = gameMode;
        this.arena = arena;
        this.player1Origin = p1Origin;
        this.player2Origin = p2Origin;
        this.player1SavedInventory = p1Inv;
        this.player1SavedArmor = p1Armor;
        this.player1SavedOffhand = p1Off;
        this.player2SavedInventory = p2Inv;
        this.player2SavedArmor = p2Armor;
        this.player2SavedOffhand = p2Off;
        this.started = false;
        this.ended = false;
    }

    public UUID getPlayer1() { return player1; }
    public UUID getPlayer2() { return player2; }
    public DuelType getType() { return type; }
    public GameMode getGameMode() { return gameMode; }
    public Arena getArena() { return arena; }
    public Location getPlayer1Origin() { return player1Origin.clone(); }
    public Location getPlayer2Origin() { return player2Origin.clone(); }

    public ItemStack[] getPlayer1SavedInventory() { return player1SavedInventory; }
    public ItemStack[] getPlayer1SavedArmor() { return player1SavedArmor; }
    public ItemStack getPlayer1SavedOffhand() { return player1SavedOffhand; }
    public ItemStack[] getPlayer2SavedInventory() { return player2SavedInventory; }
    public ItemStack[] getPlayer2SavedArmor() { return player2SavedArmor; }
    public ItemStack getPlayer2SavedOffhand() { return player2SavedOffhand; }

    public boolean isStarted() { return started; }
    public void setStarted(boolean started) {
        this.started = started;
        if (started) this.startTime = System.currentTimeMillis();
    }
    public boolean isEnded() { return ended; }
    public void setEnded(boolean ended) { this.ended = ended; }
    public long getStartTime() { return startTime; }

    public boolean isParticipant(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        if (player1.equals(uuid)) return player2;
        if (player2.equals(uuid)) return player1;
        return null;
    }

    public Location getOrigin(UUID uuid) {
        if (player1.equals(uuid)) return player1Origin.clone();
        if (player2.equals(uuid)) return player2Origin.clone();
        return null;
    }

    public ItemStack[] getSavedInventory(UUID uuid) {
        if (player1.equals(uuid)) return player1SavedInventory;
        if (player2.equals(uuid)) return player2SavedInventory;
        return null;
    }

    public ItemStack[] getSavedArmor(UUID uuid) {
        if (player1.equals(uuid)) return player1SavedArmor;
        if (player2.equals(uuid)) return player2SavedArmor;
        return null;
    }

    public ItemStack getSavedOffhand(UUID uuid) {
        if (player1.equals(uuid)) return player1SavedOffhand;
        if (player2.equals(uuid)) return player2SavedOffhand;
        return null;
    }

    public UUID getDisconnectedPlayer() { return disconnectedPlayer; }
    public void setDisconnectedPlayer(UUID uuid) {
        this.disconnectedPlayer = uuid;
        this.disconnectTime = System.currentTimeMillis();
    }
    public long getDisconnectTime() { return disconnectTime; }

    // Player-placed block tracking
    public void addPlacedBlock(int x, int y, int z) {
        playerPlacedBlocks.add(packCoords(x, y, z));
    }

    public boolean isPlayerPlaced(int x, int y, int z) {
        return playerPlacedBlocks.contains(packCoords(x, y, z));
    }

    public void removePlacedBlock(int x, int y, int z) {
        playerPlacedBlocks.remove(packCoords(x, y, z));
    }

    public void addBrokenBlock(int x, int y, int z, String blockData) {
        long key = packCoords(x, y, z);
        if (!playerPlacedBlocks.contains(key)) { // Don't save player-placed blocks as "original"
            originalBlocks.putIfAbsent(key, blockData);
        }
    }

    public Map<Long, String> getOriginalBlocks() { return originalBlocks; }
    public Set<Long> getPlacedBlocks() { return playerPlacedBlocks; }
    public boolean hasBlockChanges() { return !playerPlacedBlocks.isEmpty() || !originalBlocks.isEmpty(); }

    private static long packCoords(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
