package de.sibbet.gomme.arena;

import de.sibbet.gomme.game.ChestTier;
import de.sibbet.gomme.game.GamePhase;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public final class SkywarsArena {
    private final String name;
    private volatile Location lobbySpawn;
    private final List<Location> playerSpawns;
    private final Map<Location, ChestTier> chestLocations;
    private final Set<UUID> players;
    private final AtomicReference<GamePhase> phase;
    private final AtomicInteger countdown;
    private final ReentrantLock stateLock;

    public SkywarsArena(String name, Location lobbySpawn, List<Location> playerSpawns, Map<Location, ChestTier> chestLocations) {
        this.name = name;
        this.lobbySpawn = lobbySpawn;
        this.playerSpawns = new ArrayList<>(playerSpawns);
        this.chestLocations = new ConcurrentHashMap<>(chestLocations);
        this.players = ConcurrentHashMap.newKeySet();
        this.phase = new AtomicReference<>(GamePhase.WAITING);
        this.countdown = new AtomicInteger(30);
        this.stateLock = new ReentrantLock();
    }

    public String name() {
        return name;
    }

    public Location lobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public List<Location> playerSpawns() {
        return new ArrayList<>(playerSpawns);
    }

    public void addSpawn(Location location) {
        playerSpawns.add(location);
    }

    public Map<Location, ChestTier> chestLocations() {
        return chestLocations;
    }

    public void addChest(Location location, ChestTier chestTier) {
        chestLocations.put(location, chestTier);
    }

    public Set<UUID> players() {
        return players;
    }

    public GamePhase phase() {
        return phase.get();
    }

    public void setPhase(GamePhase gamePhase) {
        phase.set(gamePhase);
    }

    public int countdown() {
        return countdown.get();
    }

    public int decrementCountdown() {
        return countdown.decrementAndGet();
    }

    public void resetCountdown(int seconds) {
        countdown.set(seconds);
    }

    public ReentrantLock stateLock() {
        return stateLock;
    }

    public int maxPlayers() {
        return Math.max(2, playerSpawns.size());
    }
}
