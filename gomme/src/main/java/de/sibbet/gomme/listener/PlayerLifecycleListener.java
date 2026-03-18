package de.sibbet.gomme.listener;

import de.sibbet.gomme.game.GameService;
import de.sibbet.gomme.scoreboard.ScoreboardService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {
    private final GameService gameService;
    private final ScoreboardService scoreboardService;

    public PlayerLifecycleListener(GameService gameService, ScoreboardService scoreboardService) {
        this.gameService = gameService;
        this.scoreboardService = scoreboardService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        gameService.statsService().loadPlayer(event.getPlayer());
        scoreboardService.update(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gameService.statsService().savePlayer(event.getPlayer());
        gameService.leaveArena(event.getPlayer());
        gameService.statsService().removeFromCache(event.getPlayer().getUniqueId());
    }
}
