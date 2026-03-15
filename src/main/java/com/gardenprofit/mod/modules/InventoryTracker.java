package com.gardenprofit.mod.modules;

import com.gardenprofit.mod.GardenProfitConfig;
import com.gardenprofit.mod.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Per-tick inventory diff scanner that detects items appearing in the
 * player's inventory. This catches items that bypass sacks and go
 * directly into inventory (pest drops, hunting box items, etc.).
 *
 * Crops are excluded since they are tracked precisely by SackTracker.
 *
 * Modeled after SkyHanni's OwnInventoryData approach.
 */
public class InventoryTracker {

    private static final Map<String, Integer> prevSnapshot = new LinkedHashMap<>();
    private static boolean initialized = false;
    private static long lastWorldSwitchTime = 0;

    // Grace period: don't track until this many ms after initialization
    private static final long WARMUP_MS = 5_000;
    private static long initTime = 0;

    // Items to ignore temporarily (e.g. after sack-to-inventory transfer)
    private static final Map<String, Long> ignoredItems = new LinkedHashMap<>();
    private static final long IGNORE_DURATION_MS = 3_000;

    // Crops are tracked by SackTracker, don't double-count them here
    private static final Set<String> CROP_ITEMS = Set.of(
            "Wheat", "Enchanted Wheat", "Enchanted Hay Bale",
            "Seeds", "Enchanted Seeds", "Box of Seeds",
            "Potato", "Enchanted Potato", "Enchanted Baked Potato",
            "Carrot", "Enchanted Carrot", "Enchanted Golden Carrot",
            "Melon Slice", "Melon Block", "Enchanted Melon Slice", "Enchanted Melon",
            "Pumpkin", "Enchanted Pumpkin", "Polished Pumpkin",
            "Sugar Cane", "Enchanted Sugar", "Enchanted Sugar Cane",
            "Cactus", "Enchanted Cactus Green", "Enchanted Cactus",
            "Red Mushroom", "Brown Mushroom",
            "Enchanted Red Mushroom", "Enchanted Brown Mushroom",
            "Enchanted Red Mushroom Block", "Enchanted Brown Mushroom Block",
            "Cocoa Beans", "Enchanted Cocoa Beans", "Enchanted Cookie",
            "Nether Wart", "Enchanted Nether Wart", "Mutant Nether Wart",
            "Sunflower", "Enchanted Sunflower", "Compacted Sunflower",
            "Moonflower", "Enchanted Moonflower", "Compacted Moonflower",
            "Wild Rose", "Enchanted Wild Rose", "Compacted Wild Rose");

    // Items that we care about tracking via inventory diff
    // (pest drops, misc garden items that go to inventory)
    private static final Set<String> TRACKED_INVENTORY_ITEMS = Set.of(
            "Squeaky Toy", "Squeaky Mousemat", "Fire in a Bottle",
            "Mantid Claw", "Overclocker 3000",
            "Dung", "Honey Jar", "Plant Matter", "Tasty Cheese", "Compost", "Jelly",
            "Pest Shard",
            "Cropie", "Squash", "Fermento",
            "Tool EXP Capsule");

    /**
     * Called when the player switches worlds / joins a server.
     */
    public static void onWorldSwitch() {
        lastWorldSwitchTime = System.currentTimeMillis();
        prevSnapshot.clear();
        initialized = false;
    }

    /**
     * Temporarily ignore an item name (e.g. after sack-to-inventory chat).
     */
    public static void ignoreItem(String itemName, long durationMs) {
        ignoredItems.put(itemName, System.currentTimeMillis() + durationMs);
    }

    /**
     * Called every tick from the client tick callback.
     */
    public static void tick(Minecraft client) {
        if (client.player == null) return;

        // Don't track for 3 seconds after a world switch
        if (System.currentTimeMillis() - lastWorldSwitchTime < 3_000) {
            return;
        }

        // Build current inventory snapshot
        Map<String, Integer> currentSnapshot = new LinkedHashMap<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack == null || stack.isEmpty()) continue;

            String name = stack.getHoverName().getString()
                    .replaceAll("(?i)\u00A7[0-9A-FK-ORZ]", "").trim();

            if (name.isEmpty()) continue;

            currentSnapshot.merge(name, stack.getCount(), Integer::sum);
        }

        // On first tick or after world switch, take initial snapshot and start warmup
        if (!initialized) {
            prevSnapshot.clear();
            prevSnapshot.putAll(currentSnapshot);
            initialized = true;
            initTime = System.currentTimeMillis();
            return;
        }

        // During warmup, only update snapshot without tracking diffs
        // This prevents pre-existing inventory items from being counted
        if (System.currentTimeMillis() - initTime < WARMUP_MS) {
            prevSnapshot.clear();
            prevSnapshot.putAll(currentSnapshot);
            return;
        }

        // Clean up expired ignores
        long now = System.currentTimeMillis();
        ignoredItems.entrySet().removeIf(e -> e.getValue() < now);

        // Diff against previous snapshot
        for (Map.Entry<String, Integer> entry : currentSnapshot.entrySet()) {
            String name = entry.getKey();
            int current = entry.getValue();
            int previous = prevSnapshot.getOrDefault(name, 0);

            if (current > previous) {
                int diff = current - previous;

                // Skip crops (tracked by SackTracker)
                if (CROP_ITEMS.contains(name)) continue;

                // Skip ignored items
                if (ignoredItems.containsKey(name)) continue;

                // Only track items we care about
                if (TRACKED_INVENTORY_ITEMS.contains(name)) {
                    ProfitManager.addInventoryDrop(name, diff);

                    ClientUtils.sendDebugMessage(client,
                            "[Inv] +" + diff + " " + name);
                }
            }
        }

        // Update snapshot
        prevSnapshot.clear();
        prevSnapshot.putAll(currentSnapshot);
    }
}
