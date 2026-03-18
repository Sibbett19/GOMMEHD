package de.sibbet.gomme.config;

import de.sibbet.gomme.arena.SkywarsArena;
import de.sibbet.gomme.util.LocationCodec;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArenaConfigRepository {
    private final de.sibbet.gomme.Gomme plugin;

    public ArenaConfigRepository(de.sibbet.gomme.Gomme plugin) {
        this.plugin = plugin;
    }

    public List<SkywarsArena> loadArenas() {
        FileConfiguration config = plugin.getConfig();
        List<SkywarsArena> arenas = new ArrayList<>();

        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section == null) {
            return arenas;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection arenaSection = section.getConfigurationSection(key);
            if (arenaSection == null) {
                continue;
            }

            Location lobby = decodeNullable(arenaSection.getString("lobby"));
            List<Location> spawns = arenaSection.getStringList("spawns").stream()
                    .map(LocationCodec::deserialize)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            Set<Location> chests = new HashSet<>(arenaSection.getStringList("chests").stream()
                    .map(LocationCodec::deserialize)
                    .filter(java.util.Objects::nonNull)
                    .toList());

            arenas.add(new SkywarsArena(key, lobby, spawns, chests));
        }
        return arenas;
    }

    public void saveArenas(Collection<SkywarsArena> arenas) {
        FileConfiguration config = plugin.getConfig();
        config.set("arenas", null);
        for (SkywarsArena arena : arenas) {
            String base = "arenas." + arena.name() + ".";
            config.set(base + "lobby", arena.lobbySpawn() == null ? null : LocationCodec.serialize(arena.lobbySpawn()));
            config.set(base + "spawns", arena.playerSpawns().stream().map(LocationCodec::serialize).toList());
            config.set(base + "chests", arena.chestLocations().stream().map(LocationCodec::serialize).toList());
        }
        plugin.saveConfig();
    }

    private Location decodeNullable(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        return LocationCodec.deserialize(encoded);
    }
}
