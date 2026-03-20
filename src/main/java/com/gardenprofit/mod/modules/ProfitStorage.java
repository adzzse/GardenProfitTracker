package com.gardenprofit.mod.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Handles all JSON file persistence (load/save) and daily-reset logic
 * for profit data.  Replaces the file-IO code that was scattered across
 * ProfitManager.
 */
public final class ProfitStorage {

    private static final ProfitStorage INSTANCE = new ProfitStorage();

    private static final File CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getConfigDir().resolve("gardenprofit").toFile();
    private static final File LIFETIME_FILE = new File(CONFIG_DIR, "lifetime.json");
    private static final File DAILY_FILE = new File(CONFIG_DIR, "daily.json");
    private static final File BAZAAR_CACHE_FILE = new File(CONFIG_DIR, "bazaar_cache.json");

    // Old file paths for one-time migration
    private static final File OLD_LIFETIME_FILE = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getConfigDir().resolve("pest_macro_profit_lifetime.json").toFile();
    private static final File OLD_DAILY_FILE = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getConfigDir().resolve("pest_macro_profit_daily.json").toFile();
    private static final File OLD_BAZAAR_CACHE_FILE = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getConfigDir().resolve("gardenprofit_bazaar_cache.json").toFile();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String lastDailyResetDate = getCurrentDateString();

    private ProfitStorage() {}

    public static ProfitStorage getInstance() { return INSTANCE; }

    // ── Lifetime ────────────────────────────────────────────────────────

    public void saveLifetime() {
        ensureConfigDir();
        Map<String, Long> copy = new LinkedHashMap<>(ProfitState.getInstance().getCounts("lifetime"));
        CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(LIFETIME_FILE)) {
                GSON.toJson(copy, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void loadLifetime() {
        migrateOldFile(OLD_LIFETIME_FILE, LIFETIME_FILE);
        if (!LIFETIME_FILE.exists()) return;
        try (FileReader reader = new FileReader(LIFETIME_FILE)) {
            Type type = new TypeToken<Map<String, Long>>() {}.getType();
            Map<String, Long> data = GSON.fromJson(reader, type);
            ProfitState.getInstance().setLifetimeCounts(data);
        } catch (Exception e) {
            System.err.println("[GardenProfit] Failed to load lifetime profit data: " + e.getMessage());
        }
    }

    // ── Daily ───────────────────────────────────────────────────────────

    public void saveDaily() {
        ensureConfigDir();
        DailyData data = new DailyData(
                new LinkedHashMap<>(ProfitState.getInstance().getCounts("daily")),
                ProfitState.getInstance().getSprayQuantity("daily"),
                lastDailyResetDate
        );
        CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(DAILY_FILE)) {
                GSON.toJson(data, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void loadDaily() {
        migrateOldFile(OLD_DAILY_FILE, DAILY_FILE);
        if (!DAILY_FILE.exists()) return;
        try (FileReader reader = new FileReader(DAILY_FILE)) {
            DailyData data = GSON.fromJson(reader, DailyData.class);
            if (data != null) {
                lastDailyResetDate = data.resetDate != null ? data.resetDate : getCurrentDateString();
                if (!lastDailyResetDate.equals(getCurrentDateString())) {
                    ProfitState.getInstance().resetDaily();
                    lastDailyResetDate = getCurrentDateString();
                    saveDaily();
                } else {
                    ProfitState.getInstance().setDailyCounts(data.counts);
                    ProfitState.getInstance().setSprayDailyQuantity(data.sprayQuantity);
                }
            }
        } catch (Exception e) {
            System.err.println("[GardenProfit] Failed to load daily profit data: " + e.getMessage());
        }
    }

    public void resetDaily() {
        ProfitState.getInstance().resetDaily();
        lastDailyResetDate = getCurrentDateString();
        saveDaily();
    }

    /**
     * Checks if a new day has started and resets daily counts if needed.
     */
    public void checkDailyReset() {
        String currentDate = getCurrentDateString();
        if (!currentDate.equals(lastDailyResetDate)) {
            resetDaily();
        }
    }

    // ── Bazaar Cache ────────────────────────────────────────────────────

    public void saveBazaarCache(Map<String, Double> buyPrices, Map<String, Double> sellPrices, long fetchTimeMs) {
        ensureConfigDir();
        BazaarCacheData data = new BazaarCacheData(
                new LinkedHashMap<>(buyPrices),
                new LinkedHashMap<>(sellPrices),
                fetchTimeMs
        );
        try (FileWriter writer = new FileWriter(BAZAAR_CACHE_FILE)) {
            GSON.toJson(data, writer);
            System.out.println("[GardenProfit] Bazaar cache saved (buy="
                    + data.buyPrices.size() + ", sell=" + data.sellPrices.size() + ")");
        } catch (IOException e) {
            System.err.println("[GardenProfit] Failed to save bazaar cache: " + e.getMessage());
        }
    }

    /**
     * Loads the bazaar price cache from disk.
     * Returns cached data or null if not found.
     */
    public BazaarCacheData loadBazaarCache() {
        migrateOldFile(OLD_BAZAAR_CACHE_FILE, BAZAAR_CACHE_FILE);
        if (!BAZAAR_CACHE_FILE.exists()) {
            System.out.println("[GardenProfit] No bazaar cache file found, will fetch fresh prices.");
            return null;
        }
        try (FileReader reader = new FileReader(BAZAAR_CACHE_FILE)) {
            BazaarCacheData data = GSON.fromJson(reader, BazaarCacheData.class);
            if (data != null) {
                // Backward compatibility: old cache used a single 'prices' field.
                if (data.buyPrices == null && data.sellPrices == null && data.prices != null) {
                    data.buyPrices = new LinkedHashMap<>(data.prices);
                    data.sellPrices = new LinkedHashMap<>(data.prices);
                } else if (data.buyPrices == null && data.sellPrices != null) {
                    data.buyPrices = new LinkedHashMap<>(data.sellPrices);
                } else if (data.sellPrices == null && data.buyPrices != null) {
                    data.sellPrices = new LinkedHashMap<>(data.buyPrices);
                }
            }
            if (data != null && data.buyPrices != null && data.sellPrices != null) {
                long ageMinutes = (System.currentTimeMillis() - data.fetchTimeMs) / 60_000;
                System.out.println("[GardenProfit] Loaded cached bazaar prices (buy="
                        + data.buyPrices.size() + ", sell=" + data.sellPrices.size()
                        + ", age: " + ageMinutes + " min)");
                return data;
            }
        } catch (Exception e) {
            System.err.println("[GardenProfit] Failed to load bazaar cache: " + e.getMessage());
        }
        return null;
    }

    // ── Periodic save debounce ──────────────────────────────────────────

    /**
     * Called from ProfitManager.update() to flush dirty state to disk.
     */
    public void flushIfNeeded() {
        ProfitState state = ProfitState.getInstance();
        if (state.needsSave() && System.currentTimeMillis() - state.getLastSaveTime() > 5000) {
            saveLifetime();
            saveDaily();
            state.markSaved();
        }
    }

    // ── Record-style DTOs ───────────────────────────────────────────────

    public static class DailyData {
        public Map<String, Long> counts;
        public long sprayQuantity;
        public String resetDate;

        public DailyData() {}

        public DailyData(Map<String, Long> counts, long sprayQuantity, String resetDate) {
            this.counts = counts;
            this.sprayQuantity = sprayQuantity;
            this.resetDate = resetDate;
        }
    }

    public static class BazaarCacheData {
        // Preferred dual-side cache fields
        public Map<String, Double> buyPrices;
        public Map<String, Double> sellPrices;
        // Legacy cache field for backward compatibility
        public Map<String, Double> prices;
        public long fetchTimeMs;

        public BazaarCacheData() {}

        public BazaarCacheData(Map<String, Double> buyPrices, Map<String, Double> sellPrices, long fetchTimeMs) {
            this.buyPrices = buyPrices;
            this.sellPrices = sellPrices;
            this.fetchTimeMs = fetchTimeMs;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static String getCurrentDateString() {
        return LocalDate.now().toString();
    }

    private static void ensureConfigDir() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
    }

    private static void migrateOldFile(File oldFile, File newFile) {
        if (!newFile.exists() && oldFile.exists()) {
            ensureConfigDir();
            if (oldFile.renameTo(newFile)) {
                System.out.println("[GardenProfit] Migrated " + oldFile.getName() + " -> " + newFile.getPath());
            }
        }
    }
}
