package com.gardenprofit.mod.modules;

import com.gardenprofit.mod.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.Collections;
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
 */
public class SackTracker {

    private static final SackTracker INSTANCE = new SackTracker();

    private SackTracker() {}

    public static SackTracker getInstance() { return INSTANCE; }

    // Regex to parse each line in the hover text: "+128 Wheat (Agronomy Sack)"
    private static final Pattern SACK_CHANGE_PATTERN = Pattern.compile(
            "([+-][\\d,]+)\\s+(.+?)\\s+\\((.+?)\\)");

    // Strip Minecraft color codes
    private static final Pattern STRIP_COLOR = Pattern.compile("(?i)\u00A7[0-9A-FK-ORZ]");

    // Track sack inventory state to ignore manual transfers
    private static boolean inSackInventory = false;
    private static long lastSackInventoryCloseTime = 0;
    private static final long SACK_INVENTORY_COOLDOWN_MS = 10_000;
    private static final long STASH_TRANSFER_COOLDOWN_MS = 6_000;
    private static final long DUPLICATE_FINGERPRINT_WINDOW_MS = 4_000;
    private static final Map<String, Long> recentFingerprints = new LinkedHashMap<>();
    private static long lastManualTransferTime = 0;

    /**
     * Called when any GUI/inventory is opened. If the title contains "Sack",
     * we mark that we're inside a sack inventory to suppress tracking.
     */
    public void onInventoryOpen(String inventoryName) {
        if (inventoryName != null && (inventoryName.contains("Sack") || inventoryName.contains("Material Stash"))) {
            inSackInventory = true;
            ClientUtils.sendDebugMessage(Minecraft.getInstance(),
                    "[Sack] Manual transfer suppression active: opened '" + inventoryName + "'.");
        }
    }

    /**
     * Called when any GUI/inventory is closed.
     */
    public void onInventoryClose() {
        if (inSackInventory) {
            inSackInventory = false;
            lastSackInventoryCloseTime = System.currentTimeMillis();
            ClientUtils.sendDebugMessage(Minecraft.getInstance(), "[Sack] Manual transfer suppression cooldown started.");
        }
    }

    /**
     * Process a chat message Component looking for [Sacks] messages.
     * Returns true if this message was a sack change event (so other
     * handlers can skip it if needed).
     */
    public static boolean handleChatMessage(Component message) {
        String plainText = message.getString();
        if (plainText == null) {
            return false;
        }

        String strippedText = STRIP_COLOR.matcher(plainText).replaceAll("").trim();
        if (strippedText.startsWith("From stash:") || strippedText.contains("items from your material stash")) {
            lastManualTransferTime = System.currentTimeMillis();
            ClientUtils.sendDebugMessage(Minecraft.getInstance(),
                    "[Sack] Manual transfer suppression active: stash pickup detected.");
        }

        if (!plainText.contains("[Sacks]")) {
            return false;
        }

        purgeExpiredFingerprints();

        // Don't track sack changes while in a sack inventory or within cooldown
        // (manual transfers, not farming)
        if (inSackInventory) {
            return true; // Still consume the message
        }
        long timeSinceClose = System.currentTimeMillis() - lastSackInventoryCloseTime;
        if (timeSinceClose < SACK_INVENTORY_COOLDOWN_MS) {
            return true;
        }
        long timeSinceManualTransfer = System.currentTimeMillis() - lastManualTransferTime;
        if (timeSinceManualTransfer < STASH_TRANSFER_COOLDOWN_MS) {
            return true;
        }

        // Extract hover text from all siblings
        // Use a set to deduplicate: the same hover text can appear on multiple siblings
        List<String> hoverTexts = new ArrayList<>();
        extractHoverTexts(message, hoverTexts, new HashSet<>());
        if (hoverTexts.isEmpty()) {
            return true;
        }

        String fingerprint = buildFingerprint(hoverTexts);
        if (recentFingerprints.containsKey(fingerprint)) {
            ClientUtils.sendDebugMessage(Minecraft.getInstance(),
                    "[Sack] Ignored duplicated sack payload.");
            return true;
        }
        recentFingerprints.put(fingerprint, System.currentTimeMillis() + DUPLICATE_FINGERPRINT_WINDOW_MS);

        List<SackChange> changes = parseHoverTexts(hoverTexts);

        // Net all changes per item name before reporting
        // Auto-crafting causes the same item to appear in both Added (+) and Removed (-)
        // e.g. +113 Nether Wart (Added) then -113 Nether Wart (Removed for crafting)
        Map<String, Integer> netChanges = new LinkedHashMap<>();
        Map<String, String> sackNames = new LinkedHashMap<>();
        for (SackChange change : changes) {
            netChanges.merge(change.itemName, change.delta, Integer::sum);
            sackNames.putIfAbsent(change.itemName, change.sackName);
        }

        // Report non-zero net changes (positive + negative).
        // This allows visitor fulfillment and other sack removals to be tracked.
        for (Map.Entry<String, Integer> entry : netChanges.entrySet()) {
            if (entry.getValue() != 0) {
                String itemName = entry.getKey();
                int netDelta = entry.getValue();

                // Skip recent Bazaar purchases only for positive adds
                // (we still want to track negative removals, e.g. visitor usage).
                if (netDelta > 0 && ProfitManager.isBazaarPurchaseIgnored(itemName)) {
                    Minecraft client = Minecraft.getInstance();
                    ClientUtils.sendDebugMessage(client,
                            "[Sack] Ignored bazaar purchase: +" + netDelta + " " + itemName);
                    continue;
                }

                ProfitManager.getInstance().addSackDrop(itemName, netDelta);

                Minecraft client = Minecraft.getInstance();
                String signedDelta = (netDelta > 0 ? "+" : "") + netDelta;
                ClientUtils.sendDebugMessage(client,
                        "[Sack] " + signedDelta + " " + itemName
                                + " (" + sackNames.getOrDefault(itemName, "?") + ")");
            }
        }

        return true;
    }

    /**
     * Recursively extract hover text from a Component and all its siblings.
     */
    private static void extractHoverTexts(Component component, List<String> hoverTexts, Set<String> seenHoverTexts) {
        // Check this component's hover event
        extractFromStyle(component.getStyle(), hoverTexts, seenHoverTexts);

        // Recurse into all siblings
        for (Component sibling : component.getSiblings()) {
            extractHoverTexts(sibling, hoverTexts, seenHoverTexts);
        }
    }

    /**
     * Extract sack hover text from a Style's HoverEvent if present.
     * Uses seenHoverTexts to skip hover text we already processed
     * (the same hover can appear on multiple siblings).
     */
    private static void extractFromStyle(Style style, List<String> hoverTexts, Set<String> seenHoverTexts) {
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

        // Process both Added and Removed sections.
        if (!hoverText.startsWith("Added") && !hoverText.startsWith("Removed")) {
            return;
        }

        hoverTexts.add(hoverText);
    }

    private static List<SackChange> parseHoverTexts(List<String> hoverTexts) {
        List<SackChange> changes = new ArrayList<>();

        for (String hoverText : hoverTexts) {
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

        return changes;
    }

    private static String buildFingerprint(List<String> hoverTexts) {
        List<String> sorted = new ArrayList<>(hoverTexts);
        Collections.sort(sorted);
        return String.join("\n---\n", sorted);
    }

    private static void purgeExpiredFingerprints() {
        long now = System.currentTimeMillis();
        recentFingerprints.entrySet().removeIf(entry -> entry.getValue() < now);
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
