package de.sibbet.gomme.stats;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class PlayerStats {
    private final UUID uniqueId;
    private volatile String lastKnownName;
    private final AtomicInteger wins;
    private final AtomicInteger kills;
    private final AtomicInteger deaths;
    private final AtomicInteger gamesPlayed;

    public PlayerStats(UUID uniqueId, String lastKnownName, int wins, int kills, int deaths, int gamesPlayed) {
        this.uniqueId = uniqueId;
        this.lastKnownName = lastKnownName;
        this.wins = new AtomicInteger(wins);
        this.kills = new AtomicInteger(kills);
        this.deaths = new AtomicInteger(deaths);
        this.gamesPlayed = new AtomicInteger(gamesPlayed);
    }

    public static PlayerStats empty(UUID uniqueId, String name) {
        return new PlayerStats(uniqueId, name, 0, 0, 0, 0);
    }

    public UUID uniqueId() {
        return uniqueId;
    }

    public String lastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public int addWin() {
        return wins.incrementAndGet();
    }

    public int addKill() {
        return kills.incrementAndGet();
    }

    public int addDeath() {
        return deaths.incrementAndGet();
    }

    public int addGamePlayed() {
        return gamesPlayed.incrementAndGet();
    }

    public int wins() {
        return wins.get();
    }

    public int kills() {
        return kills.get();
    }

    public int deaths() {
        return deaths.get();
    }

    public int gamesPlayed() {
        return gamesPlayed.get();
    }
}
