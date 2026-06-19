package com.prismsmp.duels.arena;

import com.prismsmp.duels.PrismDuels;
import com.prismsmp.duels.kit.GameMode.ArenaSize;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;

import java.util.Random;

public class ArenaGenerator {
    private final PrismDuels plugin;
    private static final int FLOOR_Y = 64;
    private static final int SMALL_RADIUS = 20; // 40 diameter
    private static final int SMALL_SPACING = 250;
    private static final int LARGE_RADIUS = 150; // 300 diameter
    private static final int LARGE_SPACING = 500;
    private static final int DOME_HEIGHT = 200;
    private final Random random = new Random(42); // Seed for consistent generation

    public ArenaGenerator(PrismDuels plugin) {
        this.plugin = plugin;
    }

    public void generateAllArenas(World world) {
        plugin.getLogger().info("Generating arenas in " + world.getName() + "...");

        // 5 small circular arenas
        for (int i = 0; i < 5; i++) {
            int cx = i * SMALL_SPACING;
            ArenaTheme theme = ArenaTheme.fromIndex(i);
            String name = "arena" + (i + 1);
            plugin.getLogger().info("Building small arena " + name + " (" + theme.getDisplayName() + ")...");
            loadChunks(world, cx, 0, SMALL_RADIUS + 15);
            generateSmallArena(world, cx, 0, theme);
            Location s1 = new Location(world, cx - SMALL_RADIUS + 5 + 0.5, FLOOR_Y + 1, 0.5, -90, 0);
            Location s2 = new Location(world, cx + SMALL_RADIUS - 5 + 0.5, FLOOR_Y + 1, 0.5, 90, 0);
            int bound = SMALL_RADIUS + 3;
            Arena arena = new Arena(name, s1, s2, theme, ArenaSize.SMALL,
                    cx - bound, -bound, cx + bound, bound, FLOOR_Y - 1, FLOOR_Y + 110);
            plugin.getArenaManager().addArena(arena);
            plugin.getArenaManager().saveSnapshot(arena, world);
            plugin.getLogger().info(name + " complete! (" + (i + 1) + "/8)");
        }

        // 3 large natural terrain arenas
        int largeStart = 5 * SMALL_SPACING + 500;
        for (int i = 0; i < 3; i++) {
            int cx = largeStart + i * LARGE_SPACING;
            String name = "terrain" + (i + 1);
            plugin.getLogger().info("Building large terrain arena " + name + "... (this takes a while)");
            loadChunks(world, cx, 0, LARGE_RADIUS + 20);
            generateLargeArena(world, cx, 0);
            Location s1 = new Location(world, cx - 40 + 0.5, FLOOR_Y + 1, 0.5, -90, 0);
            Location s2 = new Location(world, cx + 40 + 0.5, FLOOR_Y + 1, 0.5, 90, 0);
            int bound = LARGE_RADIUS + 5;
            Arena arena = new Arena(name, s1, s2, ArenaTheme.STONE, ArenaSize.LARGE,
                    cx - bound, -bound, cx + bound, bound, FLOOR_Y - 16, FLOOR_Y + DOME_HEIGHT + 5);
            plugin.getArenaManager().addArena(arena);
            plugin.getArenaManager().saveSnapshot(arena, world);
            plugin.getLogger().info(name + " complete! (" + (6 + i) + "/8)");
        }

        plugin.getLogger().info("All 8 arenas generated! (5 small + 3 large)");
    }

    // ==================== SMALL CIRCULAR ARENA ====================

    private void generateSmallArena(World world, int cx, int cz, ArenaTheme theme) {
        int r = SMALL_RADIUS;

        // Ground platform (circular, slightly larger than arena)
        int groundR = r + 12;
        for (int x = -groundR; x <= groundR; x++) {
            for (int z = -groundR; z <= groundR; z++) {
                if (x * x + z * z <= groundR * groundR) {
                    setBlock(world, cx + x, FLOOR_Y - 1, cz + z, Material.BEDROCK);
                    setBlock(world, cx + x, FLOOR_Y, cz + z, theme.getGround());
                }
            }
        }

        // Circular fighting floor
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z <= r * r) {
                    setBlock(world, cx + x, FLOOR_Y, cz + z, theme.getFloorMain());
                }
            }
        }

        // Floor accent pattern - cross + ring
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int dist2 = x * x + z * z;
                if (dist2 > r * r) continue;
                // Cross through center (2 blocks wide)
                if (Math.abs(x) <= 1 || Math.abs(z) <= 1) {
                    setBlock(world, cx + x, FLOOR_Y, cz + z, theme.getFloorAccent());
                }
                // Inner ring at r/2
                int innerR = r / 2;
                if (Math.abs(dist2 - innerR * innerR) < innerR * 3) {
                    setBlock(world, cx + x, FLOOR_Y, cz + z, theme.getFloorAccent());
                }
                // Outer border ring
                if (dist2 > (r - 2) * (r - 2) && dist2 <= r * r) {
                    setBlock(world, cx + x, FLOOR_Y, cz + z, theme.getFloorAccent());
                }
            }
        }

        // Circular walls (4 blocks high)
        int wallR = r + 1;
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 4; y++) {
            for (int x = -wallR - 1; x <= wallR + 1; x++) {
                for (int z = -wallR - 1; z <= wallR + 1; z++) {
                    int dist2 = x * x + z * z;
                    if (dist2 >= wallR * wallR && dist2 <= (wallR + 1) * (wallR + 1)) {
                        setBlock(world, cx + x, y, cz + z, theme.getWall());
                    }
                }
            }
        }
        // Wall top decoration
        for (int x = -wallR - 1; x <= wallR + 1; x++) {
            for (int z = -wallR - 1; z <= wallR + 1; z++) {
                int dist2 = x * x + z * z;
                if (dist2 >= wallR * wallR && dist2 <= (wallR + 1) * (wallR + 1)) {
                    setBlock(world, cx + x, FLOOR_Y + 5, cz + z, theme.getWallTop());
                }
            }
        }

        // 8 evenly spaced pillars around the circle
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int px = cx + (int) (wallR * Math.cos(angle));
            int pz = cz + (int) (wallR * Math.sin(angle));
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 7; y++) {
                setBlock(world, px, y, pz, theme.getPillar());
            }
            setBlock(world, px, FLOOR_Y + 8, pz, theme.getPillarCap());
            // Lighting on top
            setBlock(world, px, FLOOR_Y + 9, pz, theme.getLighting());
        }

        // Barrier blocks on top of walls (100 blocks tall)
        for (int y = FLOOR_Y + 5; y <= FLOOR_Y + 100; y++) {
            for (int x = -wallR - 1; x <= wallR + 1; x++) {
                for (int z = -wallR - 1; z <= wallR + 1; z++) {
                    int dist2 = x * x + z * z;
                    if (dist2 >= wallR * wallR && dist2 <= (wallR + 2) * (wallR + 2)) {
                        setBlock(world, cx + x, y, cz + z, Material.BARRIER);
                    }
                }
            }
        }

        // Hidden floor lighting
        for (int x = -r + 4; x < r; x += 6) {
            for (int z = -r + 4; z < r; z += 6) {
                if (x * x + z * z < (r - 2) * (r - 2)) {
                    setBlock(world, cx + x, FLOOR_Y - 1, cz + z, Material.SEA_LANTERN);
                }
            }
        }
    }

    // ==================== LARGE TERRAIN ARENA ====================

    private void generateLargeArena(World world, int cx, int cz) {
        int r = LARGE_RADIUS;

        // Terrain layers (circular) - 15 blocks thick for crystal PvP
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r) continue;
                setBlock(world, cx + x, FLOOR_Y, cz + z, Material.GRASS_BLOCK);
                setBlock(world, cx + x, FLOOR_Y - 1, cz + z, Material.DIRT);
                setBlock(world, cx + x, FLOOR_Y - 2, cz + z, Material.DIRT);
                setBlock(world, cx + x, FLOOR_Y - 3, cz + z, Material.DIRT);
                for (int y = FLOOR_Y - 4; y >= FLOOR_Y - 14; y--) {
                    setBlock(world, cx + x, y, cz + z, Material.STONE);
                }
                setBlock(world, cx + x, FLOOR_Y - 15, cz + z, Material.BEDROCK);
            }
        }

        // Bedrock below
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r) continue;
                setBlock(world, cx + x, FLOOR_Y - 16, cz + z, Material.BEDROCK);
            }
        }

        // Scatter oak trees (30 trees)
        random.setSeed(cx * 31L + cz);
        for (int i = 0; i < 30; i++) {
            int tx = random.nextInt(r * 2 - 40) - r + 20;
            int tz = random.nextInt(r * 2 - 40) - r + 20;
            if (tx * tx + tz * tz > (r - 20) * (r - 20)) continue;
            // Don't place near spawn points
            if (Math.abs(tx) < 30 && Math.abs(tz) < 10) continue;
            placeOakTree(world, cx + tx, FLOOR_Y + 1, cz + tz);
        }

        // Scatter tall grass and flowers
        for (int i = 0; i < 200; i++) {
            int gx = random.nextInt(r * 2) - r;
            int gz = random.nextInt(r * 2) - r;
            if (gx * gx + gz * gz > (r - 5) * (r - 5)) continue;
            Block block = world.getBlockAt(cx + gx, FLOOR_Y + 1, cz + gz);
            if (block.getType() == Material.AIR && world.getBlockAt(cx + gx, FLOOR_Y, cz + gz).getType() == Material.GRASS_BLOCK) {
                Material plant = random.nextInt(5) == 0
                        ? (random.nextBoolean() ? Material.DANDELION : Material.POPPY)
                        : Material.SHORT_GRASS;
                block.setType(plant, false);
            }
        }

        // Gray glass dome
        plugin.getLogger().info("Building dome...");
        buildDome(world, cx, cz, r, DOME_HEIGHT);

        // Barrier ring at the base (prevent going under dome)
        for (int y = FLOOR_Y - 16; y <= FLOOR_Y; y++) {
            for (int x = -r - 1; x <= r + 1; x++) {
                for (int z = -r - 1; z <= r + 1; z++) {
                    int dist2 = x * x + z * z;
                    if (dist2 >= r * r && dist2 <= (r + 2) * (r + 2)) {
                        setBlock(world, cx + x, y, cz + z, Material.BEDROCK);
                    }
                }
            }
        }
    }

    private void buildDome(World world, int cx, int cz, int radius, int height) {
        double a = radius;
        double b = height;

        // Build solid dome shell - 3 blocks thick, no gaps
        for (int x = -radius - 2; x <= radius + 2; x++) {
            for (int z = -radius - 2; z <= radius + 2; z++) {
                for (int y = 0; y <= height + 2; y++) {
                    double nx = (double) x / a;
                    double nz = (double) z / a;
                    double ny = (double) y / b;
                    double dist = nx * nx + nz * nz + ny * ny;

                    // Shell: between inner and outer surface (3 block thick)
                    double inner = 0.93;
                    double outer = 1.07;
                    if (dist >= inner && dist <= outer) {
                        setBlock(world, cx + x, FLOOR_Y + y, cz + z, Material.GRAY_STAINED_GLASS);
                    }
                }
            }
        }

        // Seal the very top cap (fill any remaining gaps)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                setBlock(world, cx + x, FLOOR_Y + height, cz + z, Material.GRAY_STAINED_GLASS);
                setBlock(world, cx + x, FLOOR_Y + height - 1, cz + z, Material.GRAY_STAINED_GLASS);
            }
        }
    }

    private void placeOakTree(World world, int x, int y, int z) {
        int height = 4 + random.nextInt(3); // 4-6 blocks
        // Trunk
        for (int i = 0; i < height; i++) {
            setBlock(world, x, y + i, z, Material.OAK_LOG);
        }
        // Leaves (3x3 at top 2 layers, 5x5 at layer below)
        int leafBase = y + height - 2;
        for (int dy = 0; dy < 3; dy++) {
            int leafR = dy < 2 ? 2 : 1;
            for (int lx = -leafR; lx <= leafR; lx++) {
                for (int lz = -leafR; lz <= leafR; lz++) {
                    if (lx == 0 && lz == 0 && dy < 2) continue; // trunk
                    if (Math.abs(lx) == leafR && Math.abs(lz) == leafR && random.nextInt(3) == 0) continue; // corners
                    Block block = world.getBlockAt(x + lx, leafBase + dy, z + lz);
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.OAK_LEAVES, false);
                    }
                }
            }
        }
    }

    // ==================== HELPERS ====================

    private void loadChunks(World world, int cx, int cz, int radius) {
        int chunkMinX = (cx - radius) >> 4;
        int chunkMaxX = (cx + radius) >> 4;
        int chunkMinZ = (cz - radius) >> 4;
        int chunkMaxZ = (cz + radius) >> 4;
        for (int x = chunkMinX; x <= chunkMaxX; x++) {
            for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                world.getChunkAt(x, z).load(true);
            }
        }
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
    }
}
