package com.prismsmp.duels.kit;

public enum GameMode {
    SWORD("Sword", "sword", ArenaSize.SMALL, BlockRule.NONE),
    CRYSTAL("Crystal", "crystal", ArenaSize.LARGE, BlockRule.FULL_BREAK),
    MACE("Mace", "mace", ArenaSize.LARGE, BlockRule.NONE),
    SPEARMACE("SpearMace", "spearmace", ArenaSize.LARGE, BlockRule.NONE),
    AXE("Axe", "axe", ArenaSize.SMALL, BlockRule.NONE),
    DIAPOT("DiaPot", "diapot", ArenaSize.SMALL, BlockRule.NONE),
    UHC("UHC", "uhc", ArenaSize.LARGE, BlockRule.PLAYER_BLOCKS_ONLY),
    SMP("SMP", "smp", ArenaSize.LARGE, BlockRule.PLAYER_BLOCKS_ONLY),
    DIASMP("DiaSMP", "diasmp", ArenaSize.LARGE, BlockRule.PLAYER_BLOCKS_ONLY);

    public enum ArenaSize {
        SMALL,  // Circular enclosed arena
        LARGE   // Natural terrain with dome
    }

    public enum BlockRule {
        NONE,               // No placing or breaking
        PLAYER_BLOCKS_ONLY, // Can place blocks and only break player-placed blocks
        FULL_BREAK          // Can place and break anything except dome/bedrock
    }

    private final String displayName;
    private final String configKey;
    private final ArenaSize arenaSize;
    private final BlockRule blockRule;

    GameMode(String displayName, String configKey, ArenaSize arenaSize, BlockRule blockRule) {
        this.displayName = displayName;
        this.configKey = configKey;
        this.arenaSize = arenaSize;
        this.blockRule = blockRule;
    }

    public String getDisplayName() { return displayName; }
    public String getConfigKey() { return configKey; }
    public ArenaSize getArenaSize() { return arenaSize; }
    public BlockRule getBlockRule() { return blockRule; }

    public static GameMode fromString(String input) {
        if (input == null) return null;
        String lower = input.toLowerCase().replace(" ", "");
        for (GameMode gm : values()) {
            if (gm.configKey.equals(lower) || gm.displayName.equalsIgnoreCase(input) || gm.name().equalsIgnoreCase(input)) {
                return gm;
            }
        }
        return null;
    }
}
