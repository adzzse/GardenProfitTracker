package com.gardenprofit.mod.modules;

import com.gardenprofit.mod.GardenProfitConfig;
import com.gardenprofit.mod.util.ClientUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facade for profit tracking.  Delegates to:
 * <ul>
 *   <li>{@link ProfitState}  -- session/daily/lifetime counts</li>
 *   <li>{@link ProfitStorage} -- JSON persistence and daily resets</li>
 *   <li>{@link BazaarFetcher} -- price lookups and HTTP fetching</li>
 *   <li>{@link ItemConstants} -- shared item data &amp; categories</li>
 * </ul>
 *
 * Methods that the rest of the mod calls (HUD renderer, commands, etc.)
 * are kept here so the public API surface remains unchanged.
 */
public final class ProfitManager {

    private static final ProfitManager INSTANCE = new ProfitManager();

    public enum DropSource {
        SACK,
        PEST,
        RARE_DROP,
        PET_DROP,
        OVERFLOW_DROP,
        PEST_SHARD,
        INVENTORY,
        OTHER
    }

    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");
    private static final long PURSE_BASELINE_STABILIZE_MS = 3_000L;
    private static final long PURSE_BASELINE_MAX_PENDING_MS = 12_000L;

    private long lastPurseBalance = -1;
    private boolean purseBaselinePending = true;
    private long pendingPurseCandidate = -1;
    private int pendingPurseStableReads = 0;
    private long purseBaselinePendingSince = 0L;

    // Spray phase flag
    private volatile boolean isSprayPhaseActive = false;

    private ProfitManager() {}

    public static ProfitManager getInstance() { return INSTANCE; }

    // ── Spray phase (unchanged API) ─────────────────────────────────────

    public void startSprayPhase() { isSprayPhaseActive = true; }
    public void stopSprayPhase() { isSprayPhaseActive = false; }
    public boolean isSprayPhaseActive() { return isSprayPhaseActive; }

    // ── Static convenience wrappers (backwards compatibility) ───────────

    public static void resetSession() {
        ProfitState.getInstance().resetSession();
        PetXpTracker.reset();
        INSTANCE.lastPurseBalance = -1;
        INSTANCE.purseBaselinePending = true;
        INSTANCE.pendingPurseCandidate = -1;
        INSTANCE.pendingPurseStableReads = 0;
        INSTANCE.purseBaselinePendingSince = System.currentTimeMillis();
    }

    /**
     * Called on world join to seed the purse baseline.
     */
    public static void onWorldSwitch(Minecraft client) {
        INSTANCE.lastPurseBalance = -1;
        INSTANCE.purseBaselinePending = true;
        INSTANCE.pendingPurseCandidate = -1;
        INSTANCE.pendingPurseStableReads = 0;
        INSTANCE.purseBaselinePendingSince = System.currentTimeMillis();
        ClientUtils.sendDebugMessage(client, "[ProfitManager] Purse baseline pending on join.");
    }

    public static void loadLifetime() { ProfitStorage.getInstance().loadLifetime(); }
    public static void loadDaily() { ProfitStorage.getInstance().loadDaily(); }

    // ── Drop recording (delegates to ProfitState) ───────────────────────

    /**
     * Core method: normalizes the item name and adds the drop to all tiers.
     */
    public void addDrop(String itemName, long count) {
        addDropFromSource(itemName, count, DropSource.OTHER);
    }

    public void addDropFromSource(String itemName, long count, DropSource source) {
        String processedName = STRIP_COLOR_PATTERN.matcher(itemName).replaceAll("").trim();
        long multiplier = 1;

        Matcher suffixMatcher = Pattern.compile("\\s+[xX](\\d+)$").matcher(processedName);
        if (suffixMatcher.find()) {
            try {
                multiplier = Long.parseLong(suffixMatcher.group(1));
                processedName = processedName.substring(0, suffixMatcher.start()).trim();
            } catch (Exception ignored) {
            }
        }

        long finalCount = count * multiplier;

        // Skip vinyl and rune items
        if (processedName.toLowerCase().endsWith("vinyl")) return;
        if (processedName.toLowerCase().contains("rune")) return;

        // Match against tracked items for pretty formatting
        String matchedName = null;
        for (String tracked : ItemConstants.TRACKED_ITEMS.keySet()) {
            if (tracked.equalsIgnoreCase(processedName)) {
                matchedName = tracked;
                break;
            }
        }

        if (matchedName == null) {
            if (processedName.toLowerCase().startsWith("pet xp (")) {
                matchedName = processedName;
            } else {
                matchedName = normalizeName(processedName);
            }
        }

        if (source != DropSource.SACK && ItemConstants.isCropItem(matchedName)) {
            logIgnoredNonSackCrop(source, matchedName, finalCount);
            return;
        }

        ProfitStorage.getInstance().checkDailyReset();
        ProfitState.getInstance().addDrop(matchedName, finalCount);
    }

    /** Called by SackTracker when items are added to sacks. */
    public void addSackDrop(String itemName, long count) { addDropFromSource(itemName, count, DropSource.SACK); }

    /** Called by InventoryTracker when non-sack items appear in inventory. */
    public void addInventoryDrop(String itemName, long count) { addDropFromSource(itemName, count, DropSource.INVENTORY); }

    public void addVisitorGain(String itemName, long count) {
        String cleanName = STRIP_COLOR_PATTERN.matcher(itemName).replaceAll("").replace("+", "").trim();
        long multiplier = 1;
        Matcher m = Pattern.compile("\\s+[xX](\\d+)$").matcher(cleanName);
        if (m.find()) {
            try {
                multiplier = Long.parseLong(m.group(1));
                cleanName = cleanName.substring(0, m.start()).trim();
            } catch (Exception ignored) {
            }
        }

        String key = cleanName.startsWith(ItemConstants.VISITOR_PREFIX) ? cleanName : ItemConstants.VISITOR_PREFIX + cleanName;
        long totalCount = count * multiplier;

        ProfitStorage.getInstance().checkDailyReset();
        ProfitState.getInstance().addVisitorGain(key, totalCount);
    }

    public void addVisitorCost(long coinsSpent) {
        ProfitStorage.getInstance().checkDailyReset();
        ProfitState.getInstance().addVisitorCost(coinsSpent);
    }

    public void addSprayCost(int quantity, long coins) {
        ProfitStorage.getInstance().checkDailyReset();
        ProfitState.getInstance().addSprayCost(quantity, coins);
    }

    /** Delegates to ChatMessageParser for bazaar purchase ignore checking. */
    public static boolean isBazaarPurchaseIgnored(String itemName) {
        return ChatMessageParser.getInstance().isBazaarPurchaseIgnored(itemName);
    }

    public void addPetXp(String petName, long xpAmount) {
        if (xpAmount <= 0) return;
        addDrop("Pet XP (" + petName + ")", xpAmount);
    }

    // ── Queries (used by HUD renderer and commands) ─────────────────────

    public static long getSprayQuantity(String mode) {
        return ProfitState.getInstance().getSprayQuantity(mode);
    }

    public static long getSprayQuantity(boolean lifetime) {
        return getSprayQuantity(lifetime ? "lifetime" : "session");
    }

    public double getItemPrice(String itemName) {
        return BazaarFetcher.getInstance().getItemPrice(itemName);
    }

    public static boolean isPredefinedTrackedItem(String itemName) {
        return ItemConstants.isPredefinedTrackedItem(itemName);
    }

    public static String getCategorizedName(String name) {
        if (name.equals(ItemConstants.SPRAY_COST_KEY)) {
            return "\u00A7c\u00A7l[COST] \u00A7fSprayonator";
        }
        if (name.equals(ItemConstants.VISITOR_COST_KEY) || name.equals(ItemConstants.VISITOR_COST_KEY_LEGACY)) {
            return "\u00A7c\u00A7l[COST] \u00A7fVisitor Cost";
        }
        if (name.startsWith(ItemConstants.VISITOR_PREFIX)) {
            return "\u00A75\u00A7l[VISITOR] \u00A7f" + name.substring(ItemConstants.VISITOR_PREFIX.length());
        }

        String color = "\u00A77";
        String tag = "OTHER";
        if (ItemConstants.isCropItem(name)) {
            color = "\u00A7a"; tag = "CROP";
        } else if (ItemConstants.PEST_ITEMS.contains(name)) {
            color = "\u00A7d"; tag = "PEST";
        } else if (ItemConstants.PETS.contains(name)) {
            color = "\u00A76"; tag = "PET";
        } else if (ItemConstants.MISC_DROPS.contains(name) || name.toLowerCase().startsWith("pet xp (")) {
            color = "\u00A7b"; tag = "MISC";
        }

        String displayName = name.replace("Enchanted ", "Ench. ");
        if (name.toLowerCase().startsWith("pet xp (")) {
            displayName = name.substring(8, name.length() - 1) + " XP";
        }
        return color + "\u00A7l[" + tag + "] \u00A7f" + displayName;
    }

    public static String getCompactCategoryLabel(String category) {
        switch (category) {
            case "Crops": return "\u00A7a\u00A7l[CROP]";
            case "Pest Items": return "\u00A7d\u00A7l[PEST]";
            case "Pets": return "\u00A76\u00A7l[PET]";
            case "Misc Drops": return "\u00A7b\u00A7l[MISC]";
            case "Visitor": return "\u00A75\u00A7l[VISITOR]";
            case "Costs": return "\u00A7c\u00A7l[COST]";
            default: return "\u00A77\u00A7l[OTHER]";
        }
    }

    public static Map<String, Long> getActiveDrops() { return getActiveDrops("session"); }
    public static Map<String, Long> getActiveDrops(boolean lifetime) { return getActiveDrops(lifetime ? "lifetime" : "session"); }

    public static Map<String, Long> getActiveDrops(String mode) {
        Map<String, Long> counts = ProfitState.getInstance().getCounts(mode);
        List<Map.Entry<String, Long>> rows = new ArrayList<>();
        Map<String, Double> lineProfitCache = new HashMap<>();

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (ItemConstants.isIgnoredItem(entry.getKey())) continue;
            rows.add(entry);
            lineProfitCache.put(entry.getKey(),
                    BazaarFetcher.getInstance().getItemPrice(entry.getKey()) * entry.getValue());
        }

        rows.sort((left, right) -> Double.compare(
                lineProfitCache.getOrDefault(right.getKey(), 0.0),
                lineProfitCache.getOrDefault(left.getKey(), 0.0)));

        Map<String, Long> ordered = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : rows) {
            ordered.put(entry.getKey(), entry.getValue());
        }
        return ordered;
    }

    public static Map<String, Long> getCompactDrops() { return getCompactDrops("session"); }
    public static Map<String, Long> getCompactDrops(boolean lifetime) { return getCompactDrops(lifetime ? "lifetime" : "session"); }

    public static Map<String, Long> getCompactDrops(String mode) {
        Map<String, Long> compact = new LinkedHashMap<>();
        compact.put("Crops", 0L);
        compact.put("Pest Items", 0L);
        compact.put("Pets", 0L);
        compact.put("Misc Drops", 0L);
        compact.put("Visitor", 0L);
        compact.put("Costs", 0L);
        compact.put("Others", 0L);

        Map<String, Long> targetCounts = ProfitState.getInstance().getCounts(mode);

        for (Map.Entry<String, Long> entry : targetCounts.entrySet()) {
            String name = entry.getKey();
            if (ItemConstants.isIgnoredItem(name)) continue;
            long count = entry.getValue();
            double price = BazaarFetcher.getInstance().getItemPrice(name);
            double profit = price * count;

            String category = ItemConstants.getCategory(name);
            if ("Costs".equals(category) || (profit < 0 && "Others".equals(category))) {
                compact.merge("Costs", (long) profit, Long::sum);
            } else {
                compact.merge(category, (long) profit, Long::sum);
            }
        }

        return compact.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (v1, v2) -> v1, LinkedHashMap::new));
    }

    public static void reset() { resetSession(); }

    public static void resetDaily() { ProfitStorage.getInstance().resetDaily(); }

    public static void resetLifetime() {
        ProfitState.getInstance().resetLifetime();
        ProfitStorage.getInstance().saveLifetime();
    }

    public static long getTotalProfit() { return getTotalProfit("session"); }
    public static long getTotalProfit(boolean lifetime) { return getTotalProfit(lifetime ? "lifetime" : "session"); }

    public static long getTotalProfit(String mode) {
        double total = 0;
        Map<String, Long> targetCounts = ProfitState.getInstance().getCounts(mode);
        for (Map.Entry<String, Long> entry : targetCounts.entrySet()) {
            if (ItemConstants.isIgnoredItem(entry.getKey())) continue;
            double price = BazaarFetcher.getInstance().getItemPrice(entry.getKey());
            total += price * entry.getValue();
        }
        return (long) total;
    }

    // ── Tick-based update ───────────────────────────────────────────────

    public static void update(Minecraft client) {
        if (client.player == null) return;

        // Flush dirty state to disk
        ProfitStorage.getInstance().flushIfNeeded();

        // Track purse changes
        long currentPurse = ClientUtils.getPurse(client);
        if (INSTANCE.purseBaselinePending) {
            long pendingForMs = System.currentTimeMillis() - INSTANCE.purseBaselinePendingSince;
            if (currentPurse != -1) {
                if (currentPurse == INSTANCE.pendingPurseCandidate) {
                    INSTANCE.pendingPurseStableReads++;
                } else {
                    INSTANCE.pendingPurseCandidate = currentPurse;
                    INSTANCE.pendingPurseStableReads = 1;
                }

                if (INSTANCE.pendingPurseStableReads >= 2 || pendingForMs >= PURSE_BASELINE_STABILIZE_MS) {
                    INSTANCE.lastPurseBalance = currentPurse;
                    INSTANCE.purseBaselinePending = false;
                    ClientUtils.sendDebugMessage(client,
                            "[ProfitManager] Purse baseline finalized at " + currentPurse + ".");
                }
            } else if (pendingForMs >= PURSE_BASELINE_MAX_PENDING_MS) {
                INSTANCE.purseBaselinePending = false;
                ClientUtils.sendDebugMessage(client,
                        "[ProfitManager] Purse baseline finalized without stable read; waiting for first valid purse value.");
            }
        } else if (currentPurse != -1) {
            if (INSTANCE.lastPurseBalance != -1 && currentPurse > INSTANCE.lastPurseBalance) {
                long delta = currentPurse - INSTANCE.lastPurseBalance;
                if (delta <= 50000) {
                    INSTANCE.addDrop("Purse", delta);
                } else if (GardenProfitConfig.showDebug) {
                    ClientUtils.sendDebugMessage(client, "Dismissed large purse change: +" + delta);
                }
            }
            INSTANCE.lastPurseBalance = currentPurse;
        }

        // Refresh bazaar prices every hour
        BazaarFetcher.getInstance().refreshIfNeeded();
    }

    public static void startStartupPriceFetch() {
        BazaarFetcher.getInstance().startStartupFetch();
    }

    public static synchronized void fetchBazaarPrices() {
        BazaarFetcher.getInstance().fetchBazaarPrices();
    }

    public static void onPriceModeChanged() {
        BazaarFetcher.getInstance().onPriceModeChanged();
    }

    public static void printPetXpPriceDebug(Minecraft client) {
        BazaarFetcher.getInstance().printPetXpPriceDebug(client);
    }

    public static String fetchIdByName(String name) {
        return BazaarFetcher.getInstance().fetchIdByName(name);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static String normalizeName(String name) {
        if (name == null || name.isEmpty()) return "Unknown Item";
        StringBuilder b = new StringBuilder();
        boolean nextUpper = true;
        for (char c : name.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextUpper = true;
                b.append(c);
            } else if (nextUpper) {
                b.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                b.append(Character.toLowerCase(c));
            }
        }
        return b.toString();
    }

    private static void logIgnoredNonSackCrop(DropSource source, String itemName, long count) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || !GardenProfitConfig.showDebug) return;
        ClientUtils.sendDebugMessage(client,
                "[Drop] Ignored non-sack crop from " + source + ": +" + count + " " + itemName);
    }
}
