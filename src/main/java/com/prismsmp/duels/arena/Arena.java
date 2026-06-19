package com.prismsmp.duels.arena;

import com.prismsmp.duels.kit.GameMode.ArenaSize;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Arena {
    private final String name;
    private final Location spawn1;
    private final Location spawn2;
    private final ArenaTheme theme;
    private final ArenaSize size;
    // Region bounds for snapshot reset
    private final int minX, minZ, maxX, maxZ, minY, maxY;
    private boolean inUse;

    public Arena(String name, Location spawn1, Location spawn2, ArenaTheme theme,
                 int minX, int minZ, int maxX, int maxZ, int minY, int maxY) {
        this(name, spawn1, spawn2, theme, ArenaSize.SMALL, minX, minZ, maxX, maxZ, minY, maxY);
    }

    public Arena(String name, Location spawn1, Location spawn2, ArenaTheme theme, ArenaSize size,
                 int minX, int minZ, int maxX, int maxZ, int minY, int maxY) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.theme = theme;
        this.size = size;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.minY = minY;
        this.maxY = maxY;
        this.inUse = false;
    }

    public String getName() { return name; }
    public Location getSpawn1() { return spawn1.clone(); }
    public Location getSpawn2() { return spawn2.clone(); }
    public ArenaTheme getTheme() { return theme; }
    public ArenaSize getSize() { return size; }
    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }

    public int getMinX() { return minX; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxZ() { return maxZ; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }

    /**
     * Check if a location is inside this arena's bounds.
     */
    public boolean isInBounds(Location loc) {
        if (!loc.getWorld().equals(spawn1.getWorld())) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
    }

    public String serialize() {
        return serializeLoc(spawn1) + "|" + serializeLoc(spawn2) + "|"
                + theme.name() + "|"
                + size.name() + "|"
                + minX + "," + minZ + "," + maxX + "," + maxZ + "," + minY + "," + maxY;
    }

    private String serializeLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + ","
                + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    public static Arena deserialize(String name, String data) {
        String[] parts = data.split("\\|");
        if (parts.length < 4) return null;
        Location s1 = parseLoc(parts[0]);
        Location s2 = parseLoc(parts[1]);
        if (s1 == null || s2 == null) return null;

        ArenaTheme theme;
        try { theme = ArenaTheme.valueOf(parts[2]); } catch (IllegalArgumentException e) { theme = ArenaTheme.STONE; }

        ArenaSize size = ArenaSize.SMALL;
        String boundsStr;
        if (parts.length == 5) {
            try { size = ArenaSize.valueOf(parts[3]); } catch (IllegalArgumentException ignored) {}
            boundsStr = parts[4];
        } else {
            boundsStr = parts[3];
        }

        String[] bounds = boundsStr.split(",");
        if (bounds.length != 6) return null;

        return new Arena(name, s1, s2, theme, size,
                Integer.parseInt(bounds[0]), Integer.parseInt(bounds[1]),
                Integer.parseInt(bounds[2]), Integer.parseInt(bounds[3]),
                Integer.parseInt(bounds[4]), Integer.parseInt(bounds[5]));
    }

    private static Location parseLoc(String s) {
        String[] p = s.split(",");
        if (p.length != 6) return null;
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        return new Location(world, Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                Double.parseDouble(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5]));
    }
}
