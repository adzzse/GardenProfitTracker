package com.gardenprofit.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GardenProfitConfig {

    public enum PetRarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
    }

    // Profit HUD Defaults
    public static final boolean DEFAULT_COMPACT_PROFIT_CALCULATOR = false;
    public static final boolean DEFAULT_SHOW_DEBUG = false;

    // Pet Tracker Defaults
    public static final java.util.List<String> DEFAULT_PET_TRACKER_LIST = java.util.Arrays.asList(
            "PET_ROSE_DRAGON:Rose Dragon:200:LEGENDARY");

    // Session Profit HUD
    public static final int DEFAULT_SESSION_PROFIT_HUD_X = 10;
    public static final int DEFAULT_SESSION_PROFIT_HUD_Y = 150;
    public static final float DEFAULT_SESSION_PROFIT_HUD_SCALE = 1.0f;
    public static final boolean DEFAULT_SHOW_SESSION_PROFIT_HUD = true;

    // Daily HUD
    public static final int DEFAULT_DAILY_HUD_X = 10;
    public static final int DEFAULT_DAILY_HUD_Y = 290;
    public static final float DEFAULT_DAILY_HUD_SCALE = 1.0f;
    public static final boolean DEFAULT_SHOW_DAILY_HUD = true;

    // Lifetime HUD
    public static final int DEFAULT_LIFETIME_HUD_X = 10;
    public static final int DEFAULT_LIFETIME_HUD_Y = 430;
    public static final float DEFAULT_LIFETIME_HUD_SCALE = 1.0f;
    public static final boolean DEFAULT_SHOW_LIFETIME_HUD = true;

    // Active settings
    public static boolean compactProfitCalculator = DEFAULT_COMPACT_PROFIT_CALCULATOR;
    public static boolean showDebug = DEFAULT_SHOW_DEBUG;

    public static java.util.List<String> petTrackerList = new java.util.ArrayList<>(DEFAULT_PET_TRACKER_LIST);

    public static int sessionProfitHudX = DEFAULT_SESSION_PROFIT_HUD_X;
    public static int sessionProfitHudY = DEFAULT_SESSION_PROFIT_HUD_Y;
    public static float sessionProfitHudScale = DEFAULT_SESSION_PROFIT_HUD_SCALE;
    public static boolean showSessionProfitHud = DEFAULT_SHOW_SESSION_PROFIT_HUD;

    public static int dailyHudX = DEFAULT_DAILY_HUD_X;
    public static int dailyHudY = DEFAULT_DAILY_HUD_Y;
    public static float dailyHudScale = DEFAULT_DAILY_HUD_SCALE;
    public static boolean showDailyHud = DEFAULT_SHOW_DAILY_HUD;

    public static int lifetimeHudX = DEFAULT_LIFETIME_HUD_X;
    public static int lifetimeHudY = DEFAULT_LIFETIME_HUD_Y;
    public static float lifetimeHudScale = DEFAULT_LIFETIME_HUD_SCALE;
    public static boolean showLifetimeHud = DEFAULT_SHOW_LIFETIME_HUD;

    public static class PetInfo {
        public String tag;
        public String name;
        public int maxLevel;
        public PetRarity rarity;

        public PetInfo(String config) {
            String[] parts = config.split(":");
            if (parts.length >= 4) {
                this.tag = parts[0].trim();
                this.name = capitalizeWords(parts[1].trim());
                try {
                    this.maxLevel = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException e) {
                    this.maxLevel = 100;
                }
                try {
                    this.rarity = PetRarity.valueOf(parts[3].trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    this.rarity = PetRarity.LEGENDARY;
                }
            } else {
                this.tag = "UNKNOWN";
                this.name = "Unknown Pet";
                this.maxLevel = 100;
                this.rarity = PetRarity.LEGENDARY;
            }
        }

        private String capitalizeWords(String input) {
            if (input == null || input.isEmpty())
                return input;
            String[] words = input.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    sb.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        sb.append(word.substring(1).toLowerCase());
                    }
                    sb.append(" ");
                }
            }
            return sb.toString().trim();
        }

        @Override
        public String toString() {
            return tag + ":" + name + ":" + maxLevel + ":" + rarity;
        }
    }

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("garden_profit_config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save() {
        ConfigData data = new ConfigData();
        data.compactProfitCalculator = compactProfitCalculator;
        data.showDebug = showDebug;
        data.petTrackerList = new java.util.ArrayList<>(petTrackerList);

        data.sessionProfitHudX = sessionProfitHudX;
        data.sessionProfitHudY = sessionProfitHudY;
        data.sessionProfitHudScale = sessionProfitHudScale;
        data.showSessionProfitHud = showSessionProfitHud;

        data.dailyHudX = dailyHudX;
        data.dailyHudY = dailyHudY;
        data.dailyHudScale = dailyHudScale;
        data.showDailyHud = showDailyHud;

        data.lifetimeHudX = lifetimeHudX;
        data.lifetimeHudY = lifetimeHudY;
        data.lifetimeHudScale = lifetimeHudScale;
        data.showLifetimeHud = showLifetimeHud;

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                compactProfitCalculator = data.compactProfitCalculator;
                showDebug = data.showDebug;

                if (data.petTrackerList != null) {
                    petTrackerList = new java.util.ArrayList<>(data.petTrackerList);
                }

                sessionProfitHudX = data.sessionProfitHudX;
                sessionProfitHudY = data.sessionProfitHudY;
                sessionProfitHudScale = data.sessionProfitHudScale > 0 ? data.sessionProfitHudScale
                        : DEFAULT_SESSION_PROFIT_HUD_SCALE;
                showSessionProfitHud = data.showSessionProfitHud;

                dailyHudX = data.dailyHudX;
                dailyHudY = data.dailyHudY;
                dailyHudScale = data.dailyHudScale > 0 ? data.dailyHudScale : DEFAULT_DAILY_HUD_SCALE;
                showDailyHud = data.showDailyHud;

                lifetimeHudX = data.lifetimeHudX;
                lifetimeHudY = data.lifetimeHudY;
                lifetimeHudScale = data.lifetimeHudScale > 0 ? data.lifetimeHudScale : DEFAULT_LIFETIME_HUD_SCALE;
                showLifetimeHud = data.showLifetimeHud;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        boolean compactProfitCalculator = DEFAULT_COMPACT_PROFIT_CALCULATOR;
        boolean showDebug = DEFAULT_SHOW_DEBUG;
        java.util.List<String> petTrackerList = new java.util.ArrayList<>(DEFAULT_PET_TRACKER_LIST);

        int sessionProfitHudX = DEFAULT_SESSION_PROFIT_HUD_X;
        int sessionProfitHudY = DEFAULT_SESSION_PROFIT_HUD_Y;
        float sessionProfitHudScale = DEFAULT_SESSION_PROFIT_HUD_SCALE;
        boolean showSessionProfitHud = DEFAULT_SHOW_SESSION_PROFIT_HUD;

        int dailyHudX = DEFAULT_DAILY_HUD_X;
        int dailyHudY = DEFAULT_DAILY_HUD_Y;
        float dailyHudScale = DEFAULT_DAILY_HUD_SCALE;
        boolean showDailyHud = DEFAULT_SHOW_DAILY_HUD;

        int lifetimeHudX = DEFAULT_LIFETIME_HUD_X;
        int lifetimeHudY = DEFAULT_LIFETIME_HUD_Y;
        float lifetimeHudScale = DEFAULT_LIFETIME_HUD_SCALE;
        boolean showLifetimeHud = DEFAULT_SHOW_LIFETIME_HUD;
    }
}
