package de.sibbet.gomme.game;

import de.sibbet.gomme.arena.ArenaManager;
import de.sibbet.gomme.arena.SkywarsArena;
import de.sibbet.gomme.stats.MariaDbStatsService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GameService {
    private final de.sibbet.gomme.Gomme plugin;
    private final ArenaManager arenaManager;
    private final LootService lootService;
    private final MariaDbStatsService statsService;
    private final Map<UUID, SkywarsArena> playerArena = new ConcurrentHashMap<>();
    private final Map<UUID, GamePlayerData> playerData = new ConcurrentHashMap<>();
    private BukkitTask tickerTask;

    public GameService(de.sibbet.gomme.Gomme plugin, ArenaManager arenaManager, LootService lootService, MariaDbStatsService statsService) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.lootService = lootService;
        this.statsService = statsService;
    }

    public void startSchedulers() {
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickArenas, 20L, 20L);
    }

    public Optional<SkywarsArena> getArenaOf(UUID uuid) {
        return Optional.ofNullable(playerArena.get(uuid));
    }

    public GamePlayerData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, GamePlayerData::new);
    }

    public MariaDbStatsService statsService() {
        return statsService;
    }

    public boolean joinArena(Player player, SkywarsArena arena) {
        arena.stateLock().lock();
        try {
            if (arena.phase() != GamePhase.WAITING && arena.phase() != GamePhase.COUNTDOWN) {
                return false;
            }
            if (arena.players().size() >= arena.maxPlayers()) {
                return false;
            }
            arena.players().add(player.getUniqueId());
            playerArena.put(player.getUniqueId(), arena);
        } finally {
            arena.stateLock().unlock();
        }

        prepareLobbyPlayer(player, arena.lobbySpawn());
        Bukkit.broadcastMessage("§a" + player.getName() + " §7hat SkyWars " + arena.name() + " betreten. (" + arena.players().size() + "/" + arena.maxPlayers() + ")");
        return true;
    }

    public void leaveArena(Player player) {
        SkywarsArena arena = playerArena.remove(player.getUniqueId());
        if (arena == null) {
            return;
        }

        arena.stateLock().lock();
        try {
            arena.players().remove(player.getUniqueId());
            if (arena.phase() == GamePhase.COUNTDOWN && arena.players().size() < 2) {
                arena.setPhase(GamePhase.WAITING);
                arena.resetCountdown(30);
            }
        } finally {
            arena.stateLock().unlock();
        }

        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
    }

    public void onDeath(Player dead, Player killer) {
        leaveArena(dead);
        dead.setGameMode(GameMode.SPECTATOR);
        statsService.recordDeath(dead.getUniqueId(), dead.getName());
        if (killer != null) {
            getPlayerData(killer.getUniqueId()).addKill();
            statsService.recordKill(killer.getUniqueId(), killer.getName());
        }
        checkWinCondition();
    }

    public void forceStart(SkywarsArena arena) {
        arena.stateLock().lock();
        try {
            if (arena.players().size() >= 2) {
                arena.setPhase(GamePhase.COUNTDOWN);
                arena.resetCountdown(3);
            }
        } finally {
            arena.stateLock().unlock();
        }
    }

    private void tickArenas() {
        for (SkywarsArena arena : arenaManager.getArenas()) {
            arena.stateLock().lock();
            try {
                switch (arena.phase()) {
                    case WAITING -> {
                        if (arena.players().size() >= 2) {
                            arena.setPhase(GamePhase.COUNTDOWN);
                            arena.resetCountdown(15);
                        }
                    }
                    case COUNTDOWN -> {
                        int left = arena.decrementCountdown();
                        if (left <= 0) {
                            startArena(arena);
                        }
                    }
                    case RUNNING -> checkWin(arena);
                    case ENDING -> {
                    }
                }
            } finally {
                arena.stateLock().unlock();
            }
        }
    }

    private void startArena(SkywarsArena arena) {
        arena.setPhase(GamePhase.RUNNING);
        List<Location> spawns = new ArrayList<>(arena.playerSpawns());
        List<UUID> joined = new ArrayList<>(arena.players());
        joined.sort(Comparator.comparing(UUID::toString));

        for (int i = 0; i < joined.size(); i++) {
            Player player = Bukkit.getPlayer(joined.get(i));
            if (player == null) {
                continue;
            }
            Location spawn = spawns.get(i % Math.max(1, spawns.size()));
            player.teleport(spawn);
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.sendTitle("§6SkyWars", "§fKampf startet!", 5, 30, 10);
            statsService.recordGamePlayed(player.getUniqueId(), player.getName());
        }

        fillArenaChests(arena);
    }

    private void fillArenaChests(SkywarsArena arena) {
        for (Location chestLocation : arena.chestLocations()) {
            lootService.rollChestLootAsync().thenAccept(loot -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (chestLocation.getBlock().getType() != Material.CHEST) {
                    return;
                }
                Inventory inventory = ((org.bukkit.block.Chest) chestLocation.getBlock().getState()).getBlockInventory();
                inventory.clear();
                loot.forEach(item -> inventory.addItem(item));
            }));
        }
    }

    private void checkWinCondition() {
        for (SkywarsArena arena : arenaManager.getArenas()) {
            checkWin(arena);
        }
    }

    private void checkWin(SkywarsArena arena) {
        if (arena.phase() != GamePhase.RUNNING) {
            return;
        }

        List<Player> alivePlayers = arena.players().stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline() && !player.isDead())
                .toList();

        if (alivePlayers.size() == 1) {
            Player winner = alivePlayers.getFirst();
            statsService.recordWin(winner.getUniqueId(), winner.getName());
            Bukkit.broadcastMessage("§6" + winner.getName() + " §ahat SkyWars " + arena.name() + " gewonnen!");
            arena.setPhase(GamePhase.ENDING);
            Bukkit.getScheduler().runTaskLater(plugin, () -> resetArena(arena), 100L);
        }
    }

    private void resetArena(SkywarsArena arena) {
        for (UUID uuid : List.copyOf(arena.players())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.getInventory().clear();
                player.setGameMode(GameMode.ADVENTURE);
                if (arena.lobbySpawn() != null) {
                    player.teleport(arena.lobbySpawn());
                }
            }
            playerArena.remove(uuid);
        }
        arena.players().clear();
        arena.setPhase(GamePhase.WAITING);
        arena.resetCountdown(30);
    }

    private void prepareLobbyPlayer(Player player, Location lobby) {
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);
        if (lobby != null) {
            player.teleport(lobby);
        }
    }
}
