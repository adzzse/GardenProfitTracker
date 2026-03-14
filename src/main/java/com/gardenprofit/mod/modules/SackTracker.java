package com.gardenprofit.mod.modules;

import com.gardenprofit.mod.GardenProfitConfig;
import com.gardenprofit.mod.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses [Sacks] chat messages from Hypixel SkyBlock to track items
 * going into sacks. The server sends exact item names and counts in
 * the hover text of the chat message siblings.
 *
 * Modeled after SkyHanni's SackApi approach.
 */
public class SackTracker {

    // Regex to parse each line in the hover text: "+128 Wheat (Agronomy Sack)"
    private static final Pattern SACK_CHANGE_PATTERN = Pattern.compile(
            "([+-][\\d,]+)\\s+(.+?)\\s+\\((.+?)\\)");

    // Strip Minecraft color codes
    private static final Pattern STRIP_COLOR = Pattern.compile("(?i)\u00A7[0-9A-FK-ORZ]");

    // Track sack inventory state to ignore manual transfers
    private static boolean inSackInventory = false;
    private static long lastSackInventoryCloseTime = 0;
    private static final long SACK_INVENTORY_COOLDOWN_MS = 10_000;

    /**
     * Called when any GUI/inventory is opened. If the title contains "Sack",
     * we mark that we're inside a sack inventory to suppress tracking.
     */
    public static void onInventoryOpen(String inventoryName) {
        if (inventoryName != null && inventoryName.contains("Sack")) {
            inSackInventory = true;
        }
    }

    /**
     * Called when any GUI/inventory is closed.
     */
    public static void onInventoryClose() {
        if (inSackInventory) {
            inSackInventory = false;
            lastSackInventoryCloseTime = System.currentTimeMillis();
        }
    }

    /**
     * Process a chat message Component looking for [Sacks] messages.
     * Returns true if this message was a sack change event (so other
     * handlers can skip it if needed).
     */
    public static boolean handleChatMessage(Component message) {
        String plainText = message.getString();
        if (plainText == null || !plainText.contains("[Sacks]")) {
            return false;
        }

        // Don't track sack changes while in a sack inventory or within cooldown
        // (manual transfers, not farming)
        if (inSackInventory) {
            return true; // Still consume the message
        }
        long timeSinceClose = System.currentTimeMillis() - lastSackInventoryCloseTime;
        if (timeSinceClose < SACK_INVENTORY_COOLDOWN_MS) {
            return true;
        }

        // Extract hover text from all siblings
        // Use a set to deduplicate: the same hover text can appear on multiple siblings
        List<SackChange> changes = new ArrayList<>();
        Set<String> seenHoverTexts = new HashSet<>();
        extractHoverChanges(message, changes, seenHoverTexts);

        // Net all changes per item name before reporting
        // Auto-crafting causes the same item to appear in both Added (+) and Removed (-)
        // e.g. +113 Nether Wart (Added) then -113 Nether Wart (Removed for crafting)
        Map<String, Integer> netChanges = new LinkedHashMap<>();
        Map<String, String> sackNames = new LinkedHashMap<>();
        for (SackChange change : changes) {
            netChanges.merge(change.itemName, change.delta, Integer::sum);
            sackNames.putIfAbsent(change.itemName, change.sackName);
        }

        // Only report net positive gains
        for (Map.Entry<String, Integer> entry : netChanges.entrySet()) {
            if (entry.getValue() > 0) {
                String itemName = entry.getKey();
                int netDelta = entry.getValue();

                // Skip items recently purchased from Bazaar (they are tracked as costs)
                if (ProfitManager.isBazaarPurchaseIgnored(itemName)) {
                    Minecraft client = Minecraft.getInstance();
                    ClientUtils.sendDebugMessage(client,
                            "[Sack] Ignored bazaar purchase: +" + netDelta + " " + itemName);
                    continue;
                }

                ProfitManager.addSackDrop(itemName, netDelta);

                Minecraft client = Minecraft.getInstance();
                ClientUtils.sendDebugMessage(client,
                        "[Sack] +" + netDelta + " " + itemName
                                + " (" + sackNames.getOrDefault(itemName, "?") + ")");
            }
        }

        return true;
    }

    /**
     * Recursively extract hover text from a Component and all its siblings,
     * looking for "Added" or "Removed" hover text and parsing sack changes.
     */
    private static void extractHoverChanges(Component component, List<SackChange> changes, Set<String> seenHoverTexts) {
        // Check this component's hover event
        extractFromStyle(component.getStyle(), changes, seenHoverTexts);

        // Recurse into all siblings
        for (Component sibling : component.getSiblings()) {
            extractHoverChanges(sibling, changes, seenHoverTexts);
        }
    }

    /**
     * Extract sack changes from a Style's HoverEvent if present.
     * Uses seenHoverTexts to skip hover text we already processed
     * (the same hover can appear on multiple siblings).
     */
    private static void extractFromStyle(Style style, List<SackChange> changes, Set<String> seenHoverTexts) {
        if (style == null) return;

        HoverEvent hover = style.getHoverEvent();
        if (hover == null) return;

        // MC 1.21 uses sealed classes for HoverEvent actions
        if (!(hover instanceof HoverEvent.ShowText showText)) return;

        Component hoverComponent = showText.value();
        if (hoverComponent == null) return;

        String hoverText = hoverComponent.getString();
        if (hoverText == null) return;

        // Remove color codes
        hoverText = STRIP_COLOR.matcher(hoverText).replaceAll("").trim();

        // Skip if we already processed this exact hover text in this message
        if (!seenHoverTexts.add(hoverText)) return;

        // Only process "Added" sections (items going INTO sacks from farming)
        if (!hoverText.startsWith("Added") && !hoverText.startsWith("Removed")) {
            return;
        }

        // Parse each line
        for (String line : hoverText.split("\n")) {
            String cleanLine = line.trim();
            Matcher matcher = SACK_CHANGE_PATTERN.matcher(cleanLine);
            if (matcher.find()) {
                try {
                    String deltaStr = matcher.group(1).replace(",", "");
                    int delta = Integer.parseInt(deltaStr);
                    String itemName = matcher.group(2).trim();
                    String sackName = matcher.group(3).trim();

                    changes.add(new SackChange(delta, itemName, sackName));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /**
     * Represents a single item change in a sack.
     */
    private static class SackChange {
        final int delta;
        final String itemName;
        final String sackName;

        SackChange(int delta, String itemName, String sackName) {
            this.delta = delta;
            this.itemName = itemName;
            this.sackName = sackName;
        }
    }
}
