package com.prismsmp.duels.arena;

import com.prismsmp.duels.PrismDuels;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Saves and restores a rectangular region of blocks.
 * Used to reset arenas after duels (cleans up placed crystals, obsidian, broken blocks, etc.)
 */
public class ArenaSnapshot {
    private final Map<Long, String> blocks; // packed coords -> block data string
    private final String worldName;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    private ArenaSnapshot(String worldName, int minX, int minY, int minZ,
                          int maxX, int maxY, int maxZ, Map<Long, String> blocks) {
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.blocks = blocks;
    }

    /**
     * Capture a snapshot of all blocks in the given region.
     */
    public static ArenaSnapshot capture(World world, int minX, int minY, int minZ,
                                         int maxX, int maxY, int maxZ) {
        Map<Long, String> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        long key = packCoords(x - minX, y - minY, z - minZ);
                        blocks.put(key, block.getBlockData().getAsString());
                    }
                }
            }
        }
        return new ArenaSnapshot(world.getName(), minX, minY, minZ, maxX, maxY, maxZ, blocks);
    }

    /**
     * Restore the snapshot - resets all blocks in the region to their saved state.
     * Runs synchronously on the main thread.
     */
    public void restore(PrismDuels plugin) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Cannot restore snapshot: world '" + worldName + "' not found.");
            return;
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;

        // First pass: clear everything to air
        for (int x = minX; x <= maxX; x++) {
            for (int y = maxY; y >= minY; y--) { // top-down for proper physics
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }

        // Second pass: place saved blocks
        for (Map.Entry<Long, String> entry : blocks.entrySet()) {
            int[] coords = unpackCoords(entry.getKey());
            int x = coords[0] + minX;
            int y = coords[1] + minY;
            int z = coords[2] + minZ;

            try {
                BlockData data = Bukkit.createBlockData(entry.getValue());
                world.getBlockAt(x, y, z).setBlockData(data, false);
            } catch (Exception e) {
                // Fallback: try setting just the material type
                try {
                    String matName = entry.getValue().split("\\[")[0].replace("minecraft:", "").toUpperCase();
                    Material mat = Material.valueOf(matName);
                    world.getBlockAt(x, y, z).setType(mat, false);
                } catch (Exception ignored) {}
            }
        }

        // Remove any entities in the arena (dropped items, crystals, etc.)
        world.getEntities().stream()
                .filter(e -> !(e instanceof org.bukkit.entity.Player))
                .filter(e -> {
                    var loc = e.getLocation();
                    return loc.getBlockX() >= minX && loc.getBlockX() <= maxX
                            && loc.getBlockY() >= minY && loc.getBlockY() <= maxY
                            && loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
                })
                .forEach(org.bukkit.entity.Entity::remove);
    }

    /**
     * Save snapshot to a compressed file.
     */
    public void saveToFile(File file) throws IOException {
        try (var out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
            // Header
            byte[] worldBytes = worldName.getBytes(StandardCharsets.UTF_8);
            out.writeInt(worldBytes.length);
            out.write(worldBytes);
            out.writeInt(minX);
            out.writeInt(minY);
            out.writeInt(minZ);
            out.writeInt(maxX);
            out.writeInt(maxY);
            out.writeInt(maxZ);

            // Block data
            out.writeInt(blocks.size());
            for (Map.Entry<Long, String> entry : blocks.entrySet()) {
                out.writeLong(entry.getKey());
                byte[] dataBytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
                out.writeShort(dataBytes.length);
                out.write(dataBytes);
            }
        }
    }

    /**
     * Load snapshot from a compressed file.
     */
    public static ArenaSnapshot loadFromFile(File file) throws IOException {
        try (var in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            // Header
            int worldNameLen = in.readInt();
            byte[] worldBytes = new byte[worldNameLen];
            in.readFully(worldBytes);
            String worldName = new String(worldBytes, StandardCharsets.UTF_8);
            int minX = in.readInt();
            int minY = in.readInt();
            int minZ = in.readInt();
            int maxX = in.readInt();
            int maxY = in.readInt();
            int maxZ = in.readInt();

            // Block data
            int count = in.readInt();
            Map<Long, String> blocks = new HashMap<>(count);
            for (int i = 0; i < count; i++) {
                long key = in.readLong();
                int dataLen = in.readUnsignedShort();
                byte[] dataBytes = new byte[dataLen];
                in.readFully(dataBytes);
                blocks.put(key, new String(dataBytes, StandardCharsets.UTF_8));
            }

            return new ArenaSnapshot(worldName, minX, minY, minZ, maxX, maxY, maxZ, blocks);
        }
    }

    // Pack relative x, y, z into a single long for efficient storage
    private static long packCoords(int x, int y, int z) {
        return ((long) x & 0xFFFFF) | (((long) y & 0xFFF) << 20) | (((long) z & 0xFFFFF) << 32);
    }

    private static int[] unpackCoords(long packed) {
        int x = (int) (packed & 0xFFFFF);
        int y = (int) ((packed >> 20) & 0xFFF);
        int z = (int) ((packed >> 32) & 0xFFFFF);
        return new int[]{x, y, z};
    }
}
