package de.sibbet.gomme.scoreboard;

import de.sibbet.gomme.game.GamePhase;
import de.sibbet.gomme.game.GameService;
import de.sibbet.gomme.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class ScoreboardService {
    private final de.sibbet.gomme.Gomme plugin;
    private final GameService gameService;
    private BukkitTask updateTask;

    public ScoreboardService(de.sibbet.gomme.Gomme plugin, GameService gameService) {
        this.plugin = plugin;
        this.gameService = gameService;
    }

    public void start() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    public void update(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("skywars", "dummy", "§6§lSkyWars");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerStats stats = gameService.statsService().getStats(player.getUniqueId(), player.getName());

        objective.getScore("§7Spieler: §f" + Bukkit.getOnlinePlayers().size()).setScore(9);
        objective.getScore(" ").setScore(8);

        gameService.getArenaOf(player.getUniqueId()).ifPresentOrElse(arena -> {
            objective.getScore("§7Arena: §f" + arena.name()).setScore(7);
            objective.getScore("§7Phase: §f" + mapPhase(arena.phase())).setScore(6);
            objective.getScore("§7Verbleibend: §f" + arena.players().size()).setScore(5);
        }, () -> {
            objective.getScore("§7Arena: §f-").setScore(7);
            objective.getScore("§7Status: §fLobby").setScore(6);
            objective.getScore("§7Wartend...").setScore(5);
        });

        objective.getScore("§7Wins: §f" + stats.wins()).setScore(4);
        objective.getScore("§7Kills: §f" + stats.kills()).setScore(3);
        objective.getScore("§7Deaths: §f" + stats.deaths()).setScore(2);
        objective.getScore("§eplay.example.net").setScore(1);
        player.setScoreboard(scoreboard);
    }

    private void updateAll() {
        Bukkit.getOnlinePlayers().forEach(this::update);
    }

    private String mapPhase(GamePhase phase) {
        return switch (phase) {
            case WAITING -> "Warten";
            case COUNTDOWN -> "Countdown";
            case RUNNING -> "Running";
            case ENDING -> "Ending";
        };
    }
}
