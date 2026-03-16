package com.gardenprofit.mod.modules;

import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for all item names, prices, and bazaar mappings
 * used across the mod. No other module should hardcode item data.
 */
public final class ItemConstants {

    private ItemConstants() {} // Utility class

    // ── Item Category Sets ──────────────────────────────────────────────

    public static final Set<String> CROPS = Set.of(
            "Wheat", "Enchanted Wheat", "Enchanted Hay Bale",
            "Seeds", "Enchanted Seeds", "Box of Seeds",
            "Potato", "Enchanted Potato", "Enchanted Baked Potato",
            "Carrot", "Enchanted Carrot", "Enchanted Golden Carrot",
            "Melon Slice", "Melon Block", "Enchanted Melon Slice", "Enchanted Melon",
            "Pumpkin", "Enchanted Pumpkin", "Polished Pumpkin",
            "Sugar Cane", "Enchanted Sugar", "Enchanted Sugar Cane",
            "Cactus", "Enchanted Cactus Green", "Enchanted Cactus",
            "Red Mushroom", "Enchanted Red Mushroom", "Enchanted Red Mushroom Block",
            "Brown Mushroom", "Enchanted Brown Mushroom", "Enchanted Brown Mushroom Block",
            "Cocoa Beans", "Enchanted Cocoa Beans", "Enchanted Cookie",
            "Nether Wart", "Enchanted Nether Wart", "Mutant Nether Wart",
            "Sunflower", "Enchanted Sunflower", "Compacted Sunflower",
            "Moonflower", "Enchanted Moonflower", "Compacted Moonflower",
            "Wild Rose", "Enchanted Wild Rose", "Compacted Wild Rose");

    public static final Set<String> BASE_CROPS = Set.of(
            "Wheat", "Potato", "Carrot", "Melon Slice", "Pumpkin",
            "Sugar Cane", "Cactus", "Nether Wart", "Cocoa Beans",
            "Red Mushroom", "Brown Mushroom",
            "Sunflower", "Moonflower", "Wild Rose", "Seeds");

    public static final Set<String> PEST_ITEMS = Set.of(
            "Beady Eyes", "Chirping Stereo", "Sunder VI Book", "Clipped Wings",
            "Bookworm's Favorite Book", "Atmospheric Filter", "Wriggling Larva",
            "Pesterminator I Book", "Squeaky Toy", "Squeaky Mousemat",
            "Fire in a Bottle", "Vermin Vaporizer Chip", "Mantid Claw",
            "Overclocker 3000",
            "Dung", "Honey Jar", "Plant Matter", "Tasty Cheese", "Compost", "Jelly",
            "Pest Shard");

    public static final Set<String> PETS = Set.of("Epic Slug", "Legendary Slug", "Rat");

    public static final Set<String> MISC_DROPS = Set.of(
            "Cropie", "Squash", "Fermento", "Helianthus",
            "Tool EXP Capsule", "Pet XP", "Purse");

    /**
     * Items that InventoryTracker should diff-track (non-sack items
     * that appear directly in the player's inventory).
     */
    public static final Set<String> TRACKED_INVENTORY_ITEMS = Set.of(
            "Squeaky Toy", "Squeaky Mousemat", "Fire in a Bottle",
            "Mantid Claw", "Overclocker 3000",
            "Dung", "Honey Jar", "Plant Matter", "Tasty Cheese", "Compost", "Jelly",
            "Pest Shard",
            "Cropie", "Squash", "Fermento",
            "Tool EXP Capsule");

    // ── Fallback Prices (NPC / pre-bazaar values) ───────────────────────

    public static final Map<String, Double> TRACKED_ITEMS = Map.ofEntries(
            // Crops
            Map.entry("Wheat", 6.0), Map.entry("Enchanted Wheat", 960.0), Map.entry("Enchanted Hay Bale", 153600.0),
            Map.entry("Seeds", 3.0), Map.entry("Enchanted Seeds", 480.0), Map.entry("Box of Seeds", 76800.0),
            Map.entry("Potato", 3.0), Map.entry("Enchanted Potato", 480.0),
            Map.entry("Enchanted Baked Potato", 76800.0),
            Map.entry("Carrot", 3.0), Map.entry("Enchanted Carrot", 480.0),
            Map.entry("Enchanted Golden Carrot", 76800.0),
            Map.entry("Melon Slice", 2.0), Map.entry("Melon Block", 18.0), Map.entry("Enchanted Melon Slice", 320.0),
            Map.entry("Enchanted Melon", 51200.0),
            Map.entry("Pumpkin", 10.0), Map.entry("Enchanted Pumpkin", 1600.0), Map.entry("Polished Pumpkin", 256000.0),
            Map.entry("Sugar Cane", 4.0), Map.entry("Enchanted Sugar", 640.0),
            Map.entry("Enchanted Sugar Cane", 102400.0),
            Map.entry("Cactus", 4.0), Map.entry("Enchanted Cactus Green", 640.0),
            Map.entry("Enchanted Cactus", 102400.0),
            Map.entry("Mushroom", 10.0), Map.entry("Red Mushroom", 10.0), Map.entry("Brown Mushroom", 10.0),
            Map.entry("Enchanted Red Mushroom", 1600.0), Map.entry("Enchanted Brown Mushroom", 1600.0),
            Map.entry("Enchanted Red Mushroom Block", 256000.0), Map.entry("Enchanted Brown Mushroom Block", 256000.0),
            Map.entry("Cocoa Beans", 3.0), Map.entry("Enchanted Cocoa Beans", 480.0),
            Map.entry("Enchanted Cookie", 76800.0),
            Map.entry("Nether Wart", 4.0), Map.entry("Enchanted Nether Wart", 640.0),
            Map.entry("Mutant Nether Wart", 102400.0),
            Map.entry("Sunflower", 4.0), Map.entry("Enchanted Sunflower", 640.0),
            Map.entry("Compacted Sunflower", 102400.0),
            Map.entry("Moonflower", 4.0), Map.entry("Enchanted Moonflower", 640.0),
            Map.entry("Compacted Moonflower", 102400.0),
            Map.entry("Wild Rose", 4.0), Map.entry("Enchanted Wild Rose", 640.0),
            Map.entry("Compacted Wild Rose", 102400.0),
            // Pest Items
            Map.entry("Beady Eyes", 25000.0), Map.entry("Chirping Stereo", 100000.0), Map.entry("Sunder VI Book", 0.0),
            Map.entry("Clipped Wings", 25000.0), Map.entry("Bookworm's Favorite Book", 10000.0),
            Map.entry("Atmospheric Filter", 100000.0),
            Map.entry("Wriggling Larva", 250000.0), Map.entry("Pesterminator I Book", 0.0),
            Map.entry("Squeaky Toy", 10000.0),
            Map.entry("Squeaky Mousemat", 1000000.0), Map.entry("Fire in a Bottle", 100000.0),
            Map.entry("Vermin Vaporizer Chip", 0.0),
            Map.entry("Mantid Claw", 75000.0),
            Map.entry("Overclocker 3000", 250000.0),
            Map.entry("Dung", 0.0), Map.entry("Honey Jar", 0.0), Map.entry("Plant Matter", 0.0),
            Map.entry("Tasty Cheese", 0.0), Map.entry("Compost", 0.0), Map.entry("Jelly", 0.0),
            // Pets
            Map.entry("Epic Slug", 500000.0), Map.entry("Legendary Slug", 5000000.0), Map.entry("Rat", 5000.0),
            // Misc Drops
            Map.entry("Cropie", 25000.0), Map.entry("Squash", 75000.0), Map.entry("Fermento", 250000.0),
            Map.entry("Helianthus", 0.0), Map.entry("Tool EXP Capsule", 100000.0),
            // Pet XP (price per XP point, will be fetched)
            Map.entry("Pet XP", 0.0),
            Map.entry("Pest Shard", 0.0),
            // AH Items
            Map.entry("Biofuel", 0.0), Map.entry("Farming Exp Boost", 0.0),
            Map.entry("Harvest Harbinger V Potion", 0.0),
            Map.entry("Velvet Top Hat", 0.0), Map.entry("Cashmere Hat", 0.0), Map.entry("Satin Trousers", 0.0),
            Map.entry("Oxford Shoes", 0.0), Map.entry("Space Helmet", 0.0), Map.entry("Wild Strawberry Dye", 0.0),
            Map.entry("Copper Dye", 0.0),
            // Visitor / Rare Drops
            Map.entry("Overgrown Grass", 0.0), Map.entry("Flowering Bouqet", 0.0), Map.entry("Green Bandana", 0.0),
            Map.entry("Hypercharge Chip", 0.0), Map.entry("Quickdraw Chip", 0.0), Map.entry("Superboom TNT", 0.0),
            Map.entry("Green Candy", 0.0), Map.entry("Purple Candy", 0.0), Map.entry("Ice Essence", 0.0),
            Map.entry("Diamond Essence", 0.0), Map.entry("Gold Essence", 0.0), Map.entry("Jacob's Ticket", 0.0),
            Map.entry("Turbo-Cacti I Book", 0.0), Map.entry("Turbo-Cane I Book", 0.0),
            Map.entry("Turbo-Carrot I Book", 0.0),
            Map.entry("Turbo-Cocoa I Book", 0.0), Map.entry("Turbo-Melon I Book", 0.0),
            Map.entry("Turbo-Moonflower I Book", 0.0),
            Map.entry("Turbo-Mushrooms I Book", 0.0), Map.entry("Turbo-Potato I Book", 0.0),
            Map.entry("Turbo-Pumpkin I Book", 0.0),
            Map.entry("Turbo-Rose I Book", 0.0), Map.entry("Turbo-Sunflower I Book", 0.0),
            Map.entry("Turbo-Warts I Book", 0.0),
            Map.entry("Turbo-Wheat I Book", 0.0), Map.entry("Cultivating I Book", 0.0),
            Map.entry("Delicate V Book", 0.0),
            Map.entry("Replenish I Book", 0.0), Map.entry("Dedication IV Book", 0.0), Map.entry("Jungle Key", 0.0),
            Map.entry("Pet Cake", 0.0), Map.entry("Fine Flour", 0.0), Map.entry("Arachne Fragment", 0.0),
            Map.entry("Purse", 1.0));

    // ── Bazaar API Mapping (item display name -> Coflnet tag) ────────────

    public static final Map<String, String> BAZAAR_MAPPING = Map.ofEntries(
            Map.entry("Sunder VI Book", "ENCHANTMENT_SUNDER_6"),
            Map.entry("Pesterminator I Book", "ENCHANTMENT_PESTERMINATOR_1"),
            Map.entry("Dung", "DUNG"),
            Map.entry("Honey Jar", "HONEY_JAR"),
            Map.entry("Plant Matter", "PLANT_MATTER"),
            Map.entry("Tasty Cheese", "CHEESE_FUEL"),
            Map.entry("Compost", "COMPOST"),
            Map.entry("Jelly", "JELLY"),
            Map.entry("Helianthus", "HELIANTHUS"),
            Map.entry("Vermin Vaporizer Chip", "VERMIN_VAPORIZER_GARDEN_CHIP"),
            Map.entry("ENCHANTMENT_GREEN_THUMB_1", "ENCHANTMENT_GREEN_THUMB_1"),
            Map.entry("Pest Shard", "SHARD_PEST"),
            // Visitor / Rare Drops Mappings
            Map.entry("Overgrown Grass", "OVERGROWN_GRASS"),
            Map.entry("Flowering Bouqet", "FLOWERING_BOUQUET"),
            Map.entry("Green Bandana", "GREEN_BANDANA"),
            Map.entry("Hypercharge Chip", "HYPERCHARGE_GARDEN_CHIP"),
            Map.entry("Quickdraw Chip", "QUICKDRAW_GARDEN_CHIP"),
            Map.entry("Superboom TNT", "SUPERBOOM_TNT"),
            Map.entry("Green Candy", "GREEN_CANDY"),
            Map.entry("Purple Candy", "PURPLE_CANDY"),
            Map.entry("Ice Essence", "ESSENCE_ICE"),
            Map.entry("Diamond Essence", "ESSENCE_DIAMOND"),
            Map.entry("Gold Essence", "ESSENCE_GOLD"),
            Map.entry("Jacob's Ticket", "JACOBS_TICKET"),
            Map.entry("Turbo-Cacti I Book", "ENCHANTMENT_TURBO_CACTUS_1"),
            Map.entry("Turbo-Cane I Book", "ENCHANTMENT_TURBO_CANE_1"),
            Map.entry("Turbo-Carrot I Book", "ENCHANTMENT_TURBO_CARROT_1"),
            Map.entry("Turbo-Cocoa I Book", "ENCHANTMENT_TURBO_COCO_1"),
            Map.entry("Turbo-Melon I Book", "ENCHANTMENT_TURBO_MELON_1"),
            Map.entry("Turbo-Moonflower I Book", "ENCHANTMENT_TURBO_MOONFLOWER_1"),
            Map.entry("Turbo-Mushrooms I Book", "ENCHANTMENT_TURBO_MUSHROOMS_1"),
            Map.entry("Turbo-Potato I Book", "ENCHANTMENT_TURBO_POTATO_1"),
            Map.entry("Turbo-Pumpkin I Book", "ENCHANTMENT_TURBO_PUMPKIN_1"),
            Map.entry("Turbo-Rose I Book", "ENCHANTMENT_TURBO_ROSE_1"),
            Map.entry("Turbo-Sunflower I Book", "ENCHANTMENT_TURBO_SUNFLOWER_1"),
            Map.entry("Turbo-Warts I Book", "ENCHANTMENT_TURBO_WARTS_1"),
            Map.entry("Turbo-Wheat I Book", "ENCHANTMENT_TURBO_WHEAT_1"),
            Map.entry("Cultivating I Book", "ENCHANTMENT_CULTIVATING_1"),
            Map.entry("Delicate V Book", "ENCHANTMENT_DELICATE_5"),
            Map.entry("Replenish I Book", "ENCHANTMENT_REPLENISH_1"),
            Map.entry("Dedication IV Book", "ENCHANTMENT_DEDICATION_4"),
            Map.entry("Jungle Key", "JUNGLE_KEY"),
            Map.entry("Pet Cake", "PET_CAKE"),
            Map.entry("Fine Flour", "FINE_FLOUR"),
            Map.entry("Arachne Fragment", "ARACHNE_FRAGMENT"),
            // AH Items Mappings
            Map.entry("Biofuel", "BIOFUEL"),
            Map.entry("Farming Exp Boost", "PET_ITEM_FARMING_SKILL_BOOST_UNCOMMON"),
            Map.entry("Harvest Harbinger V Potion", "POTION_harvest_harbinger"),
            Map.entry("Velvet Top Hat", "VELVET_TOP_HAT"),
            Map.entry("Cashmere Hat", "CASHMERE_JACKET"),
            Map.entry("Satin Trousers", "SATIN_TROUSERS"),
            Map.entry("Oxford Shoes", "OXFORD_SHOES"),
            Map.entry("Space Helmet", "DCTR_SPACE_HELM"),
            Map.entry("Wild Strawberry Dye", "DYE_WILD_STRAWBERRY"),
            Map.entry("Copper Dye", "DYE_COPPER"));

    // ── Helper Methods ──────────────────────────────────────────────────

    /**
     * Returns true if this item should be hidden from rendering (runes, vinyl).
     */
    public static boolean isIgnoredItem(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("rune") || lower.endsWith("vinyl") || lower.equals("vinyl");
    }

    /**
     * Returns true if the given item name matches any predefined tracked item
     * (case-insensitive) or is a Pet XP entry.
     */
    public static boolean isPredefinedTrackedItem(String itemName) {
        if (itemName == null) return false;
        if (itemName.toLowerCase().startsWith("pet xp (")) return true;
        for (String tracked : TRACKED_ITEMS.keySet()) {
            if (tracked.equalsIgnoreCase(itemName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the item category for display/grouping purposes.
     */
    public static String getCategory(String name) {
        if (name.equals("[Spray] Sprayonator") || name.equals("[Visitor] Visitor Cost")) return "Costs";
        if (name.startsWith("[Visitor] ")) return "Visitor";
        if (CROPS.contains(name)) return "Crops";
        if (PEST_ITEMS.contains(name)) return "Pest Items";
        if (PETS.contains(name)) return "Pets";
        if (MISC_DROPS.contains(name) || name.toLowerCase().startsWith("pet xp (")) return "Misc Drops";
        return "Others";
    }
}
