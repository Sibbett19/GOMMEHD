package de.sibbet.gomme.game;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public final class LootService {

    private final List<ItemStack> lootPool = List.of(
            new ItemStack(Material.STONE_SWORD),
            new ItemStack(Material.IRON_SWORD),
            new ItemStack(Material.BOW),
            new ItemStack(Material.ARROW, 16),
            new ItemStack(Material.GOLDEN_APPLE, 2),
            new ItemStack(Material.IRON_CHESTPLATE),
            new ItemStack(Material.COOKED_BEEF, 12),
            new ItemStack(Material.OAK_PLANKS, 32),
            new ItemStack(Material.ENDER_PEARL, 1),
            new ItemStack(Material.DIAMOND_HELMET)
    );

    public CompletableFuture<List<ItemStack>> rollChestLootAsync() {
        return CompletableFuture.supplyAsync(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int amount = random.nextInt(3, 7);
            List<ItemStack> selected = new ArrayList<>();
            for (int i = 0; i < amount; i++) {
                ItemStack template = lootPool.get(random.nextInt(lootPool.size()));
                selected.add(template.clone());
            }
            return selected;
        });
    }
}
