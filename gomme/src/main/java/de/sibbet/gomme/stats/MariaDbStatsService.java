package de.sibbet.gomme.stats;

import de.sibbet.gomme.Gomme;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class MariaDbStatsService {
    private final Gomme plugin;
    private final ExecutorService dbExecutor;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();

    private boolean enabled;
    private String jdbcUrl;
    private String username;
    private String password;

    public MariaDbStatsService(Gomme plugin) {
        this.plugin = plugin;
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "skywars-mariadb-stats");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        this.enabled = plugin.getConfig().getBoolean("mariadb.enabled", false);
        this.jdbcUrl = "jdbc:mariadb://"
                + plugin.getConfig().getString("mariadb.host", "localhost") + ":"
                + plugin.getConfig().getInt("mariadb.port", 3306) + "/"
                + plugin.getConfig().getString("mariadb.database", "skywars")
                + "?useUnicode=true&characterEncoding=utf8";
        this.username = plugin.getConfig().getString("mariadb.username", "root");
        this.password = plugin.getConfig().getString("mariadb.password", "");

        if (!enabled) {
            plugin.getLogger().warning("MariaDB stats are disabled in config.yml (mariadb.enabled=false).");
            return;
        }

        CompletableFuture.runAsync(this::createSchema, dbExecutor)
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Could not initialize MariaDB schema: " + throwable.getMessage());
                    return null;
                });
    }

    public void stop() {
        if (enabled) {
            Bukkit.getOnlinePlayers().forEach(this::savePlayer);
        }
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            dbExecutor.shutdownNow();
        }
    }

    public void loadPlayer(Player player) {
        if (!enabled) {
            cache.put(player.getUniqueId(), PlayerStats.empty(player.getUniqueId(), player.getName()));
            return;
        }

        UUID uuid = player.getUniqueId();
        String name = player.getName();
        CompletableFuture.supplyAsync(() -> loadStats(uuid, name), dbExecutor)
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Could not load stats for " + name + ": " + throwable.getMessage());
                    return PlayerStats.empty(uuid, name);
                })
                .thenAccept(stats -> cache.put(uuid, stats));
    }

    public PlayerStats getStats(UUID uuid, String name) {
        return cache.computeIfAbsent(uuid, key -> PlayerStats.empty(key, name));
    }

    public void savePlayer(Player player) {
        PlayerStats stats = cache.get(player.getUniqueId());
        if (stats == null) {
            return;
        }
        stats.setLastKnownName(player.getName());
        persistAsync(stats);
    }

    public void removeFromCache(UUID uuid) {
        cache.remove(uuid);
    }

    public void recordKill(UUID uuid, String name) {
        PlayerStats stats = getStats(uuid, name);
        stats.addKill();
        persistAsync(stats);
    }

    public void recordDeath(UUID uuid, String name) {
        PlayerStats stats = getStats(uuid, name);
        stats.addDeath();
        persistAsync(stats);
    }

    public void recordWin(UUID uuid, String name) {
        PlayerStats stats = getStats(uuid, name);
        stats.addWin();
        persistAsync(stats);
    }

    public void recordGamePlayed(UUID uuid, String name) {
        PlayerStats stats = getStats(uuid, name);
        stats.addGamePlayed();
        persistAsync(stats);
    }

    private PlayerStats loadStats(UUID uuid, String name) {
        String sql = "SELECT player_name, wins, kills, deaths, games_played FROM skywars_stats WHERE player_uuid = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return PlayerStats.empty(uuid, name);
                }
                String loadedName = resultSet.getString("player_name");
                return new PlayerStats(
                        uuid,
                        loadedName == null || loadedName.isBlank() ? name : loadedName,
                        resultSet.getInt("wins"),
                        resultSet.getInt("kills"),
                        resultSet.getInt("deaths"),
                        resultSet.getInt("games_played")
                );
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void persistAsync(PlayerStats stats) {
        if (!enabled) {
            return;
        }

        CompletableFuture.runAsync(() -> persist(stats), dbExecutor)
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Could not save stats for " + stats.lastKnownName() + ": " + throwable.getMessage());
                    return null;
                });
    }

    private void persist(PlayerStats stats) {
        String sql = "INSERT INTO skywars_stats (player_uuid, player_name, wins, kills, deaths, games_played) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), wins = VALUES(wins), "
                + "kills = VALUES(kills), deaths = VALUES(deaths), games_played = VALUES(games_played)";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, stats.uniqueId().toString());
            statement.setString(2, stats.lastKnownName());
            statement.setInt(3, stats.wins());
            statement.setInt(4, stats.kills());
            statement.setInt(5, stats.deaths());
            statement.setInt(6, stats.gamesPlayed());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private void createSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS skywars_stats ("
                + "player_uuid VARCHAR(36) PRIMARY KEY,"
                + "player_name VARCHAR(16) NOT NULL,"
                + "wins INT NOT NULL DEFAULT 0,"
                + "kills INT NOT NULL DEFAULT 0,"
                + "deaths INT NOT NULL DEFAULT 0,"
                + "games_played INT NOT NULL DEFAULT 0"
                + ")";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
            plugin.getLogger().info("MariaDB stats table is ready.");
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
