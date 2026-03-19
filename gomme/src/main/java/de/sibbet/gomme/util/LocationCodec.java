package de.sibbet.gomme.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class LocationCodec {

    private LocationCodec() {
    }

    public static String serialize(Location location) {
        return location.getWorld().getName() + ";" + location.getX() + ";" + location.getY() + ";" + location.getZ() + ";" + location.getYaw() + ";" + location.getPitch();
    }

    public static Location deserialize(String value) {
        String[] parts = value.split(";");
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        return new Location(world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5]));
    }
}
