package de.sibbet.gomme.command;

import de.sibbet.gomme.arena.ArenaManager;
import de.sibbet.gomme.config.ArenaConfigRepository;
import de.sibbet.gomme.game.ChestTier;
import de.sibbet.gomme.game.GameService;
import de.sibbet.gomme.stats.PlayerStats;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SkywarsCommand implements CommandExecutor {
    private final GameService gameService;
    private final ArenaManager arenaManager;
    private final ArenaConfigRepository repository;

    public SkywarsCommand(GameService gameService, ArenaManager arenaManager, ArenaConfigRepository repository) {
        this.gameService = gameService;
        this.arenaManager = arenaManager;
        this.repository = repository;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler.");
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> create(player, args);
            case "join" -> join(player, args);
            case "leave" -> gameService.leaveArena(player);
            case "setlobby" -> setLobby(player, args);
            case "addspawn" -> addSpawn(player, args);
            case "addchest" -> addChest(player, args);
            case "forcestart" -> forceStart(player, args);
            case "stats" -> stats(player);
            case "save" -> {
                repository.saveArenas(arenaManager.getArenas());
                player.sendMessage("§aArenen gespeichert.");
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void create(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c/skywars create <arena>");
            return;
        }
        arenaManager.createArena(args[1]);
        player.sendMessage("§aArena erstellt: " + args[1]);
    }

    private void join(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c/skywars join <arena>");
            return;
        }

        arenaManager.getArena(args[1]).ifPresentOrElse(arena -> {
            if (!gameService.joinArena(player, arena)) {
                player.sendMessage("§cArena voll oder bereits gestartet.");
            }
        }, () -> player.sendMessage("§cArena nicht gefunden."));
    }

    private void stats(Player player) {
        PlayerStats stats = gameService.statsService().getStats(player.getUniqueId(), player.getName());
        player.sendMessage("§6Deine SkyWars Stats:");
        player.sendMessage("§7Wins: §f" + stats.wins());
        player.sendMessage("§7Kills: §f" + stats.kills());
        player.sendMessage("§7Deaths: §f" + stats.deaths());
        player.sendMessage("§7Gespielte Runden: §f" + stats.gamesPlayed());
    }

    private void setLobby(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c/skywars setlobby <arena>");
            return;
        }
        arenaManager.getArena(args[1]).ifPresentOrElse(arena -> {
            arena.setLobbySpawn(player.getLocation());
            player.sendMessage("§aLobby gesetzt.");
        }, () -> player.sendMessage("§cArena nicht gefunden."));
    }

    private void addSpawn(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c/skywars addspawn <arena>");
            return;
        }
        arenaManager.getArena(args[1]).ifPresentOrElse(arena -> {
            arena.addSpawn(player.getLocation());
            player.sendMessage("§aSpawn hinzugefügt.");
        }, () -> player.sendMessage("§cArena nicht gefunden."));
    }

    private void addChest(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c/skywars addchest <arena> <insel|mitte>");
        if (args.length < 2) {
            player.sendMessage("§c/skywars addchest <arena>");
            return;
        }
        if (player.getTargetBlockExact(5) == null || player.getTargetBlockExact(5).getType() != Material.CHEST) {
            player.sendMessage("§cBitte auf eine Kiste schauen.");
            return;
        }

        ChestTier chestTier = parseChestTier(args[2]);
        if (chestTier == null) {
            player.sendMessage("§cLoot-Typ muss 'insel' oder 'mitte' sein.");
            return;
        }

        arenaManager.getArena(args[1]).ifPresentOrElse(arena -> {
            arena.addChest(player.getTargetBlockExact(5).getLocation(), chestTier);
            player.sendMessage("§aKiste als " + (chestTier == ChestTier.CENTER ? "Mitte" : "Insel") + "-Loot registriert.");
        arenaManager.getArena(args[1]).ifPresentOrElse(arena -> {
            arena.chestLocations().add(player.getTargetBlockExact(5).getLocation());
            player.sendMessage("§aKiste registriert.");
        }, () -> player.sendMessage("§cArena nicht gefunden."));
    }

    private void forceStart(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c/skywars forcestart <arena>");
            return;
        }
        arenaManager.getArena(args[1]).ifPresentOrElse(arena -> {
            if (gameService.forceStart(arena)) {
                player.sendMessage("§aForcestart ausgelöst (ab 1 Spieler möglich).");
                return;
            }
            player.sendMessage("§cFür den Forcestart muss mindestens 1 Spieler in der Arena sein.");
            gameService.forceStart(arena);
            player.sendMessage("§aForcestart ausgelöst.");
        }, () -> player.sendMessage("§cArena nicht gefunden."));
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6SkyWars Commands:");
        player.sendMessage("§e/skywars create <arena>");
        player.sendMessage("§e/skywars setlobby <arena>");
        player.sendMessage("§e/skywars addspawn <arena>");
        player.sendMessage("§e/skywars addchest <arena> <insel|mitte>");
        player.sendMessage("§e/skywars addchest <arena>");
        player.sendMessage("§e/skywars join <arena>");
        player.sendMessage("§e/skywars leave");
        player.sendMessage("§e/skywars stats");
        player.sendMessage("§e/skywars forcestart <arena>");
        player.sendMessage("§e/skywars save");
    }

    private ChestTier parseChestTier(String input) {
        return switch (input.toLowerCase()) {
            case "insel", "island" -> ChestTier.ISLAND;
            case "mitte", "center", "mid" -> ChestTier.CENTER;
            default -> null;
        };
    }
}
