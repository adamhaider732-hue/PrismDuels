package com.prismsmp.duels.arena;

import com.prismsmp.duels.PrismDuels;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {
    private final PrismDuels plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<String, ArenaSnapshot> snapshots = new HashMap<>();
    private final File arenaFile;
    private final File snapshotDir;

    public ArenaManager(PrismDuels plugin) {
        this.plugin = plugin;
        this.arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        this.snapshotDir = new File(plugin.getDataFolder(), "snapshots");
        if (!snapshotDir.exists()) snapshotDir.mkdirs();
        loadArenas();
        loadSnapshots();
    }

    public void loadArenas() {
        arenas.clear();
        if (!arenaFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(arenaFile);
        for (String name : config.getKeys(false)) {
            String data = config.getString(name);
            if (data == null) continue;
            Arena arena = Arena.deserialize(name, data);
            if (arena != null) {
                arenas.put(name.toLowerCase(), arena);
            } else {
                plugin.getLogger().warning("Failed to load arena: " + name);
            }
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arena(s).");
    }

    public void saveArenas() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            config.set(entry.getKey(), entry.getValue().serialize());
        }
        try {
            config.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas: " + e.getMessage());
        }
    }

    private void loadSnapshots() {
        File[] files = snapshotDir.listFiles((dir, name) -> name.endsWith(".snapshot"));
        if (files == null) return;
        for (File file : files) {
            String arenaName = file.getName().replace(".snapshot", "");
            try {
                ArenaSnapshot snapshot = ArenaSnapshot.loadFromFile(file);
                snapshots.put(arenaName.toLowerCase(), snapshot);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load snapshot for " + arenaName + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + snapshots.size() + " arena snapshot(s).");
    }

    /**
     * Save a block snapshot of an arena for reset purposes.
     */
    public void saveSnapshot(Arena arena, World world) {
        ArenaSnapshot snapshot = ArenaSnapshot.capture(world,
                arena.getMinX(), arena.getMinY(), arena.getMinZ(),
                arena.getMaxX(), arena.getMaxY(), arena.getMaxZ());
        snapshots.put(arena.getName().toLowerCase(), snapshot);

        File file = new File(snapshotDir, arena.getName().toLowerCase() + ".snapshot");
        try {
            snapshot.saveToFile(file);
            plugin.getLogger().info("Saved snapshot for arena " + arena.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save snapshot for " + arena.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Reset an arena to its saved snapshot state.
     */
    public void resetArena(Arena arena) {
        ArenaSnapshot snapshot = snapshots.get(arena.getName().toLowerCase());
        if (snapshot == null) {
            plugin.getLogger().warning("No snapshot found for arena " + arena.getName() + ", cannot reset.");
            return;
        }
        snapshot.restore(plugin);
        plugin.getLogger().info("Arena " + arena.getName() + " reset to snapshot.");
    }

    /**
     * Lightweight cleanup - just remove entities without restoring blocks.
     * Used when no blocks were modified during the duel.
     */
    public void cleanEntities(Arena arena) {
        org.bukkit.World world = arena.getSpawn1().getWorld();
        if (world == null) return;
        world.getEntities().stream()
                .filter(e -> !(e instanceof org.bukkit.entity.Player))
                .filter(e -> arena.isInBounds(e.getLocation()))
                .forEach(org.bukkit.entity.Entity::remove);
    }

    public boolean addArena(Arena arena) {
        String key = arena.getName().toLowerCase();
        if (arenas.containsKey(key)) return false;
        arenas.put(key, arena);
        saveArenas();
        return true;
    }

    public boolean removeArena(String name) {
        Arena removed = arenas.remove(name.toLowerCase());
        if (removed != null) {
            snapshots.remove(name.toLowerCase());
            new File(snapshotDir, name.toLowerCase() + ".snapshot").delete();
            saveArenas();
            return true;
        }
        return false;
    }

    public Arena getAvailableArena() {
        return getAvailableArena(null);
    }

    public Arena getAvailableArena(com.prismsmp.duels.kit.GameMode.ArenaSize size) {
        List<Arena> available = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (!arena.isInUse()) {
                if (size == null || arena.getSize() == size) {
                    available.add(arena);
                }
            }
        }
        if (available.isEmpty()) return null;
        Collections.shuffle(available);
        return available.get(0);
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    /**
     * Find which arena a location is inside of, or null.
     */
    public Arena getArenaAt(org.bukkit.Location loc) {
        for (Arena arena : arenas.values()) {
            if (arena.isInBounds(loc)) return arena;
        }
        return null;
    }

    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public int getArenaCount() {
        return arenas.size();
    }

    public boolean hasArenas() {
        return !arenas.isEmpty();
    }
}
