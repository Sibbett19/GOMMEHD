package de.sibbet.gomme.game;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class GamePlayerData {
    private final UUID uniqueId;
    private final AtomicInteger kills = new AtomicInteger(0);

    public GamePlayerData(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public UUID uniqueId() {
        return uniqueId;
    }

    public int addKill() {
        return kills.incrementAndGet();
    }

    public int kills() {
        return kills.get();
    }
}
