package com.gardenprofit.mod.modules;

import com.gardenprofit.mod.GardenProfitConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all HTTP requests to the Coflnet APIs for Bazaar/AH pricing.
 * Replaces the networking code that was previously inlined inside
 * ProfitManager.
 */
public final class BazaarFetcher {

    private static final BazaarFetcher INSTANCE = new BazaarFetcher();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, Double> bazaarPrices = new LinkedHashMap<>();
    private final Map<String, Long> petLvl1Prices = new ConcurrentHashMap<>();
    private final Map<String, Long> petMaxLvlPrices = new ConcurrentHashMap<>();
    private long lastFetchTime = 0;
    private int startupRetryCount = 3;

    // Cofl API item-ID cache
    private final Map<String, String> idByNameCache = new ConcurrentHashMap<>();

    private BazaarFetcher() {}

    public static BazaarFetcher getInstance() { return INSTANCE; }

    public Map<String, Double> getBazaarPrices() { return bazaarPrices; }
    public long getLastFetchTime() { return lastFetchTime; }

    /**
     * Called once at startup: loads cached prices, then fetches fresh ones.
     */
    public void startStartupFetch() {
        ProfitStorage.BazaarCacheData cached = ProfitStorage.getInstance().loadBazaarCache();
        if (cached != null) {
            bazaarPrices.putAll(cached.prices);
            lastFetchTime = cached.fetchTimeMs;
        }
        startupRetryCount = 0;
        fetchBazaarPrices();
    }

    /**
     * Initiates a background fetch of bazaar prices from the Coflnet API.
     */
    public synchronized void fetchBazaarPrices() {
        lastFetchTime = System.currentTimeMillis();
        new Thread(() -> {
            System.out.println("[GardenProfit] Starting bazaar price fetch...");
            HttpClient client = HttpClient.newHttpClient();
            performFetchInternal(client);

            ProfitStorage.getInstance().saveBazaarCache(bazaarPrices, lastFetchTime);
            System.out.println("[GardenProfit] Bazaar price fetch complete. " + bazaarPrices.size() + " prices loaded.");

            // Startup retry logic: if any pet price is missing, retry up to 3 times
            if (startupRetryCount < 3) {
                boolean missingAny = false;
                for (String petConfig : GardenProfitConfig.petTrackerList) {
                    GardenProfitConfig.PetInfo info = new GardenProfitConfig.PetInfo(petConfig);
                    if (!bazaarPrices.containsKey("Pet XP (" + info.name + ")")) {
                        missingAny = true;
                        break;
                    }
                }
                if (missingAny) {
                    startupRetryCount++;
                    System.out.println("[GardenProfit] Pet XP prices not fully fetched, retry "
                            + startupRetryCount + "/3 in 5s...");
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException ignored) {
                    }
                    fetchBazaarPrices();
                } else {
                    startupRetryCount = 3;
                }
            }
        }).start();
    }

    /**
     * Returns the bazaar (or fallback) price for an item.
     */
    public double getItemPrice(String itemName) {
        if (itemName.startsWith("[Visitor] ")) {
            String realName = itemName.substring(10);
            if ("Visitor Cost".equals(realName)) return 1.0;
            if ("Copper".equals(realName)) {
                double greenThumbPrice = bazaarPrices.getOrDefault("ENCHANTMENT_GREEN_THUMB_1", 0.0);
                if (greenThumbPrice <= 0) {
                    greenThumbPrice = ItemConstants.TRACKED_ITEMS.getOrDefault("ENCHANTMENT_GREEN_THUMB_1", 0.0);
                }
                if (greenThumbPrice > 0) {
                    return greenThumbPrice / 1500.0;
                }
                return 0.0;
            }
            return getItemPrice(realName);
        }

        if ("[Spray] Sprayonator".equals(itemName) || "Purse".equals(itemName)) {
            return 1.0;
        }
        double price = ItemConstants.TRACKED_ITEMS.getOrDefault(itemName, 0.0);
        if (price == 0.0) {
            price = bazaarPrices.getOrDefault(itemName, 0.0);
        }
        return price;
    }

    /**
     * Checks if bazaar prices should be refreshed (every hour) and kicks off a fetch.
     */
    public void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime > 3600000L) {
            fetchBazaarPrices();
        }
    }

    /**
     * Looks up a Cofl item tag by display name via the search API.
     */
    public String fetchIdByName(String name) {
        if (name == null || name.isEmpty()) return null;
        if (idByNameCache.containsKey(name)) {
            String cached = idByNameCache.get(name);
            return cached.isEmpty() ? null : cached;
        }

        try {
            String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://sky.coflnet.com/api/items/search/" + encoded + "?limit=1"))
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
                if (!arr.isEmpty()) {
                    String tag = arr.get(0).getAsJsonObject().get("tag").getAsString();
                    idByNameCache.put(name, tag);
                    return tag;
                }
            }
        } catch (Exception e) {
            System.err.println("[GardenProfit] Cofl item ID lookup failed for '" + name + "': " + e.getMessage());
        }
        idByNameCache.put(name, "");
        return null;
    }

    /**
     * Sends the current pet XP price data to the player's chat for debugging.
     */
    public void printPetXpPriceDebug(net.minecraft.client.Minecraft client) {
        if (client.player == null) return;
        client.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("\u00A7b[Pet XP Tracker] \u00A7fCurrently tracking:"), false);

        for (String petConfig : GardenProfitConfig.petTrackerList) {
            GardenProfitConfig.PetInfo info = new GardenProfitConfig.PetInfo(petConfig);
            long lvl1 = petLvl1Prices.getOrDefault(info.name, 0L);
            long lvlMax = petMaxLvlPrices.getOrDefault(info.name, 0L);
            double pricePerXp = bazaarPrices.getOrDefault("Pet XP (" + info.name + ")", 0.0);

            String lvl1Str = lvl1 > 0 ? String.format("%,d", lvl1) : "not found";
            String lvlMaxStr = lvlMax > 0 ? String.format("%,d", lvlMax) : "not found";
            String marginStr = pricePerXp > 0 ? String.format("%.3f", pricePerXp) : "not fetched";

            client.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            " \u00A78> \u00A7e" + info.name + "\u00A7f: \u00A77L1: \u00A76" + lvl1Str
                                    + " \u00A77Max: \u00A76" + lvlMaxStr + " \u00A77-> \u00A7a"
                                    + marginStr + " \u00A77C/XP"),
                    false);
        }
    }

    // ── Internal fetch logic ────────────────────────────────────────────

    private void performFetchInternal(HttpClient client) {
        int fetched = 0;
        int failed = 0;
        for (Map.Entry<String, String> entry : ItemConstants.BAZAAR_MAPPING.entrySet()) {
            String itemName = entry.getKey();
            String itemTag = entry.getValue();

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://sky.coflnet.com/api/item/price/" + itemTag + "/current"))
                        .GET().build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    BazaarApiResponse data = GSON.fromJson(response.body(), BazaarApiResponse.class);
                    if (data != null) {
                        double price = GardenProfitConfig.useBazaarSellPrice ? data.sell : data.buy;
                        if (price > 0) {
                            bazaarPrices.put(itemName, price);
                            fetched++;
                            String priceType = GardenProfitConfig.useBazaarSellPrice ? "sell" : "buy";
                            System.out.println("[GardenProfit] Price (" + priceType + "): "
                                    + itemName + " = " + String.format("%.1f", price));
                        }
                    }
                } else {
                    failed++;
                    System.err.println("[GardenProfit] HTTP " + response.statusCode() + " for " + itemName);
                }
            } catch (Exception e) {
                failed++;
                System.err.println("[GardenProfit] Failed to fetch price for " + itemName + ": " + e.getMessage());
            }
        }
        System.out.println("[GardenProfit] Bazaar items: " + fetched + " fetched, " + failed + " failed.");
        fetchPetXpPrice(client);
    }

    private void fetchPetXpPrice(HttpClient http) {
        for (String petConfig : GardenProfitConfig.petTrackerList) {
            GardenProfitConfig.PetInfo info = new GardenProfitConfig.PetInfo(petConfig);
            long[] table = PetXpTracker.getXpTable(info.rarity, info.maxLevel);
            final long TOTAL_XP = table[info.maxLevel];

            try {
                // Level 1
                long lvl1Price = 0;
                String url1 = "https://sky.coflnet.com/api/auctions/tag/" + info.tag
                        + "/active/overview?query%5BRarity%5D=" + info.rarity.name();

                HttpRequest req1 = HttpRequest.newBuilder().uri(URI.create(url1)).GET().build();
                HttpResponse<String> resp1 = http.send(req1, HttpResponse.BodyHandlers.ofString());

                if (resp1.statusCode() == 200) {
                    Type listType = new TypeToken<List<OverviewEntry>>() {}.getType();
                    List<OverviewEntry> listings = GSON.fromJson(resp1.body(), listType);
                    if (listings != null) {
                        for (OverviewEntry e : listings) {
                            if (e.price > 0 && (lvl1Price == 0 || e.price < lvl1Price)) {
                                lvl1Price = e.price;
                            }
                        }
                    }
                }

                // Max Level
                long lvlMaxPrice = 0;
                String urlMax = "https://sky.coflnet.com/api/auctions/tag/" + info.tag
                        + "/active/overview?query%5BRarity%5D=" + info.rarity.name()
                        + "&query%5BPetLevel%5D=" + info.maxLevel;

                HttpRequest reqMax = HttpRequest.newBuilder().uri(URI.create(urlMax)).GET().build();
                HttpResponse<String> respMax = http.send(reqMax, HttpResponse.BodyHandlers.ofString());

                if (respMax.statusCode() == 200) {
                    Type listType = new TypeToken<List<OverviewEntry>>() {}.getType();
                    List<OverviewEntry> listings = GSON.fromJson(respMax.body(), listType);
                    if (listings != null) {
                        for (OverviewEntry e : listings) {
                            if (e.price > 0 && (lvlMaxPrice == 0 || e.price < lvlMaxPrice)) {
                                lvlMaxPrice = e.price;
                            }
                        }
                    }
                }

                if (lvl1Price > 0) petLvl1Prices.put(info.name, lvl1Price);
                if (lvlMaxPrice > 0) petMaxLvlPrices.put(info.name, lvlMaxPrice);

                if (lvlMaxPrice > lvl1Price && lvl1Price > 0) {
                    double pricePerXp = (double) (lvlMaxPrice - lvl1Price) / TOTAL_XP;
                    if (pricePerXp > 0) {
                        bazaarPrices.put("Pet XP (" + info.name + ")", pricePerXp);
                    }
                }
            } catch (Exception e) {
                System.err.println("[GardenProfit] Failed to fetch Pet XP price for "
                        + info.name + ": " + e.getMessage());
            }
        }
    }

    // ── DTOs ────────────────────────────────────────────────────────────

    private static class BazaarApiResponse {
        double sell;
        double buy;
    }

    private static class OverviewEntry {
        long price;
        String uuid;
    }
}
