package com.prismsmp.duels.arena;

import org.bukkit.Material;

public enum ArenaTheme {
    DESERT(
            "Desert Colosseum",
            Material.SAND,                   // ground
            Material.SMOOTH_SANDSTONE,       // floor main
            Material.CUT_SANDSTONE,          // floor accent
            Material.CUT_SANDSTONE,          // walls
            Material.SANDSTONE_WALL,         // wall top
            Material.SANDSTONE_STAIRS,       // stands
            Material.CHISELED_SANDSTONE,     // pillars
            Material.RED_TERRACOTTA,         // pillar cap
            Material.SMOOTH_SANDSTONE_SLAB,  // floor trim
            Material.TORCH                   // lighting
    ),
    STONE(
            "Stone Fortress",
            Material.GRASS_BLOCK,            // ground
            Material.STONE_BRICKS,           // floor main
            Material.POLISHED_ANDESITE,      // floor accent
            Material.STONE_BRICKS,           // walls
            Material.STONE_BRICK_WALL,       // wall top
            Material.STONE_BRICK_STAIRS,     // stands
            Material.MOSSY_STONE_BRICKS,     // pillars
            Material.DARK_OAK_LOG,           // pillar cap
            Material.STONE_BRICK_SLAB,       // floor trim
            Material.LANTERN                 // lighting
    ),
    DARK(
            "Shadow Arena",
            Material.PODZOL,                       // ground
            Material.DEEPSLATE_TILES,              // floor main
            Material.POLISHED_BLACKSTONE_BRICKS,   // floor accent
            Material.DEEPSLATE_BRICKS,             // walls
            Material.DEEPSLATE_BRICK_WALL,         // wall top
            Material.DEEPSLATE_BRICK_STAIRS,       // stands
            Material.POLISHED_BLACKSTONE,          // pillars
            Material.CRYING_OBSIDIAN,              // pillar cap
            Material.DEEPSLATE_TILE_SLAB,          // floor trim
            Material.SOUL_LANTERN                  // lighting
    );

    private final String displayName;
    private final Material ground;
    private final Material floorMain;
    private final Material floorAccent;
    private final Material wall;
    private final Material wallTop;
    private final Material stands;
    private final Material pillar;
    private final Material pillarCap;
    private final Material floorTrim;
    private final Material lighting;

    ArenaTheme(String displayName, Material ground, Material floorMain, Material floorAccent,
               Material wall, Material wallTop, Material stands, Material pillar,
               Material pillarCap, Material floorTrim, Material lighting) {
        this.displayName = displayName;
        this.ground = ground;
        this.floorMain = floorMain;
        this.floorAccent = floorAccent;
        this.wall = wall;
        this.wallTop = wallTop;
        this.stands = stands;
        this.pillar = pillar;
        this.pillarCap = pillarCap;
        this.floorTrim = floorTrim;
        this.lighting = lighting;
    }

    public String getDisplayName() { return displayName; }
    public Material getGround() { return ground; }
    public Material getFloorMain() { return floorMain; }
    public Material getFloorAccent() { return floorAccent; }
    public Material getWall() { return wall; }
    public Material getWallTop() { return wallTop; }
    public Material getStands() { return stands; }
    public Material getPillar() { return pillar; }
    public Material getPillarCap() { return pillarCap; }
    public Material getFloorTrim() { return floorTrim; }
    public Material getLighting() { return lighting; }

    public static ArenaTheme fromIndex(int index) {
        ArenaTheme[] themes = { DESERT, STONE, DARK };
        return themes[index % themes.length];
    }
}
