package com.prismsmp.duels.duel;

import com.prismsmp.duels.kit.GameMode;

import java.util.UUID;

public class DuelRequest {
    private final UUID sender;
    private final UUID target;
    private final DuelType type;
    private final GameMode gameMode; // null for regular duels
    private final long timestamp;

    public DuelRequest(UUID sender, UUID target, DuelType type, GameMode gameMode) {
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.gameMode = gameMode;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getSender() { return sender; }
    public UUID getTarget() { return target; }
    public DuelType getType() { return type; }
    public GameMode getGameMode() { return gameMode; }
    public long getTimestamp() { return timestamp; }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 30_000; // 30 seconds
    }
}
