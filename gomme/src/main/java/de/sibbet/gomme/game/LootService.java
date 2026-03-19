package de.sibbet.gomme.game;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public final class LootService {

    private final List<ItemStack> islandLootPool = List.of(
            new ItemStack(Material.STONE_SWORD),
            new ItemStack(Material.IRON_SWORD),
            new ItemStack(Material.BOW),
            new ItemStack(Material.ARROW, 12),
            new ItemStack(Material.GOLDEN_APPLE, 1),
            new ItemStack(Material.CHAINMAIL_CHESTPLATE),
            new ItemStack(Material.IRON_BOOTS),
            new ItemStack(Material.COOKED_BEEF, 10),
            new ItemStack(Material.OAK_PLANKS, 32),
            new ItemStack(Material.COBBLESTONE, 24),
            new ItemStack(Material.WATER_BUCKET),
            new ItemStack(Material.LAVA_BUCKET)
    );
    private final List<ItemStack> centerLootPool = List.of(
            new ItemStack(Material.DIAMOND_SWORD),
            new ItemStack(Material.IRON_SWORD),
            new ItemStack(Material.BOW),
            new ItemStack(Material.ARROW, 24),
            new ItemStack(Material.GOLDEN_APPLE, 3),
            new ItemStack(Material.DIAMOND_HELMET),
            new ItemStack(Material.DIAMOND_CHESTPLATE),
            new ItemStack(Material.DIAMOND_LEGGINGS),
            new ItemStack(Material.DIAMOND_BOOTS),
            new ItemStack(Material.IRON_CHESTPLATE),
            new ItemStack(Material.ENDER_PEARL, 2),
            new ItemStack(Material.SNOWBALL, 16),
            new ItemStack(Material.TNT, 2),
            new ItemStack(Material.OBSIDIAN, 8),
            new ItemStack(Material.COOKED_BEEF, 16),
            new ItemStack(Material.OAK_PLANKS, 48)
    );

    public CompletableFuture<List<ItemStack>> rollChestLootAsync(ChestTier chestTier) {
        return CompletableFuture.supplyAsync(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            List<ItemStack> lootPool = chestTier == ChestTier.CENTER ? centerLootPool : islandLootPool;
            int amount = chestTier == ChestTier.CENTER ? random.nextInt(4, 8) : random.nextInt(3, 6);
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
