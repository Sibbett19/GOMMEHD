package de.sibbet.gomme;

import de.sibbet.gomme.arena.ArenaManager;
import de.sibbet.gomme.command.SkywarsCommand;
import de.sibbet.gomme.config.ArenaConfigRepository;
import de.sibbet.gomme.game.GameService;
import de.sibbet.gomme.game.LootService;
import de.sibbet.gomme.listener.GameListener;
import de.sibbet.gomme.listener.PlayerLifecycleListener;
import de.sibbet.gomme.scoreboard.ScoreboardService;
import de.sibbet.gomme.stats.MariaDbStatsService;
import org.bukkit.plugin.java.JavaPlugin;

public final class Gomme extends JavaPlugin {

    private ArenaConfigRepository arenaConfigRepository;
    private ArenaManager arenaManager;
    private GameService gameService;
    private LootService lootService;
    private ScoreboardService scoreboardService;
    private MariaDbStatsService mariaDbStatsService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.arenaConfigRepository = new ArenaConfigRepository(this);
        this.arenaManager = new ArenaManager(arenaConfigRepository.loadArenas());
        this.lootService = new LootService();
        this.mariaDbStatsService = new MariaDbStatsService(this);
        this.gameService = new GameService(this, arenaManager, lootService, mariaDbStatsService);
        this.scoreboardService = new ScoreboardService(this, gameService);

        mariaDbStatsService.start();

        getCommand("skywars").setExecutor(new SkywarsCommand(gameService, arenaManager, arenaConfigRepository));
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(gameService, scoreboardService), this);
        getServer().getPluginManager().registerEvents(new GameListener(gameService), this);

        gameService.startSchedulers();
        scoreboardService.start();
        getLogger().info("SkyWars plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (scoreboardService != null) {
            scoreboardService.stop();
        }
        if (mariaDbStatsService != null) {
            mariaDbStatsService.stop();
        }
        if (arenaConfigRepository != null && arenaManager != null) {
            arenaConfigRepository.saveArenas(arenaManager.getArenas());
        }
        getLogger().info("SkyWars plugin disabled.");
    }
}
