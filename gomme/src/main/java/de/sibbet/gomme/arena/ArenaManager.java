package de.sibbet.gomme.arena;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ArenaManager {
    private final Map<String, SkywarsArena> arenas = new ConcurrentHashMap<>();

    public ArenaManager(Collection<SkywarsArena> loadedArenas) {
        for (SkywarsArena arena : loadedArenas) {
            arenas.put(arena.name().toLowerCase(), arena);
        }
    }

    public Collection<SkywarsArena> getArenas() {
        return arenas.values();
    }

    public Optional<SkywarsArena> getArena(String name) {
        return Optional.ofNullable(arenas.get(name.toLowerCase()));
    }

    public SkywarsArena createArena(String name) {
        SkywarsArena arena = new SkywarsArena(name, null, java.util.List.of(), java.util.Set.of());
        arenas.put(name.toLowerCase(), arena);
        return arena;
    }
}
