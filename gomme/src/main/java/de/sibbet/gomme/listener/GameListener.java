package de.sibbet.gomme.listener;

import de.sibbet.gomme.game.GamePhase;
import de.sibbet.gomme.game.GameService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class GameListener implements Listener {
    private final GameService gameService;

    public GameListener(GameService gameService) {
        this.gameService = gameService;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        gameService.onDeath(dead, killer);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        gameService.getArenaOf(victim.getUniqueId()).ifPresent(arena -> {
            if (arena.phase() != GamePhase.RUNNING) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.getGameMode() == GameMode.ADVENTURE) {
            event.setCancelled(true);
        }
    }
}
