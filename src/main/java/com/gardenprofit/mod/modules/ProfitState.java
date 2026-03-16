package com.gardenprofit.mod.modules;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates session, daily, and lifetime drop counts with atomic
 * update methods.  Replaces the three separate Maps that used to live
 * inside ProfitManager.
 */
public final class ProfitState {

    private static final ProfitState INSTANCE = new ProfitState();

    private final Map<String, Long> sessionCounts = new LinkedHashMap<>();
    private final Map<String, Long> dailyCounts = new LinkedHashMap<>();
    private final Map<String, Long> lifetimeCounts = new LinkedHashMap<>();

    // Spray quantity is tracked separately from coin value
    private long spraySessionQuantity = 0;
    private long sprayDailyQuantity = 0;
    private long sprayLifetimeQuantity = 0;

    // Dirty flag + debounce for file writes
    private boolean needsSave = false;
    private long lastSaveTime = 0;

    private ProfitState() {}

    public static ProfitState getInstance() { return INSTANCE; }

    // ── Atomic add methods ──────────────────────────────────────────────

    /**
     * Adds a drop to all three tiers (session, daily, lifetime) atomically.
     */
    public void addDrop(String itemName, long count) {
        sessionCounts.merge(itemName, count, Long::sum);
        dailyCounts.merge(itemName, count, Long::sum);
        lifetimeCounts.merge(itemName, count, Long::sum);
        needsSave = true;
    }

    /**
     * Adds a visitor reward (prefixed with [Visitor]).
     */
    public void addVisitorGain(String key, long count) {
        sessionCounts.merge(key, count, Long::sum);
        dailyCounts.merge(key, count, Long::sum);
        lifetimeCounts.merge(key, count, Long::sum);
    }

    /**
     * Subtracts visitor cost from all tiers.
     */
    public void addVisitorCost(long coinsSpent) {
        String key = "[Visitor] Visitor Cost";
        sessionCounts.merge(key, -coinsSpent, Long::sum);
        dailyCounts.merge(key, -coinsSpent, Long::sum);
        lifetimeCounts.merge(key, -coinsSpent, Long::sum);
    }

    /**
     * Subtracts spray cost from all tiers and increments quantity counters.
     */
    public void addSprayCost(int quantity, long coins) {
        String key = "[Spray] Sprayonator";
        spraySessionQuantity += quantity;
        sprayDailyQuantity += quantity;
        sprayLifetimeQuantity += quantity;
        sessionCounts.merge(key, -coins, Long::sum);
        dailyCounts.merge(key, -coins, Long::sum);
        lifetimeCounts.merge(key, -coins, Long::sum);
    }

    // ── Getters ─────────────────────────────────────────────────────────

    /** Returns the counts map for the given mode ("session", "daily", "lifetime"). */
    public Map<String, Long> getCounts(String mode) {
        if ("daily".equals(mode)) return dailyCounts;
        if ("lifetime".equals(mode)) return lifetimeCounts;
        return sessionCounts;
    }

    public long getSprayQuantity(String mode) {
        if ("daily".equals(mode)) return sprayDailyQuantity;
        if ("lifetime".equals(mode)) return sprayLifetimeQuantity;
        return spraySessionQuantity;
    }

    public boolean needsSave() { return needsSave; }
    public long getLastSaveTime() { return lastSaveTime; }
    public void markSaved() { needsSave = false; lastSaveTime = System.currentTimeMillis(); }

    // ── Reset methods ───────────────────────────────────────────────────

    public void resetSession() {
        sessionCounts.clear();
        spraySessionQuantity = 0;
    }

    public void resetDaily() {
        dailyCounts.clear();
        sprayDailyQuantity = 0;
    }

    public void resetLifetime() {
        lifetimeCounts.clear();
        sprayLifetimeQuantity = 0;
    }

    // ── Bulk setters (for loading from disk) ────────────────────────────

    public void setDailyCounts(Map<String, Long> counts) {
        dailyCounts.clear();
        if (counts != null) dailyCounts.putAll(counts);
    }

    public void setLifetimeCounts(Map<String, Long> counts) {
        lifetimeCounts.clear();
        if (counts != null) lifetimeCounts.putAll(counts);
    }

    public void setSprayDailyQuantity(long qty) { sprayDailyQuantity = qty; }
    public void setSprayLifetimeQuantity(long qty) { sprayLifetimeQuantity = qty; }
}
