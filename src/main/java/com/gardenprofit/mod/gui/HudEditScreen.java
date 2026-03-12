package com.gardenprofit.mod.gui;

import com.gardenprofit.mod.GardenProfitConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration screen built with Cloth Config API.
 * Opened via /gardenprofit command.
 */
public class HudEditScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Garden Profit Tracker Settings"))
                .setSavingRunnable(GardenProfitConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ============================================================
        // General Settings
        // ============================================================
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("Compact Profit Display"),
                        GardenProfitConfig.compactProfitCalculator)
                .setDefaultValue(GardenProfitConfig.DEFAULT_COMPACT_PROFIT_CALCULATOR)
                .setTooltip(Component.literal("Groups individual crop drops into summarized categories"))
                .setSaveConsumer(val -> GardenProfitConfig.compactProfitCalculator = val)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("Debug Messages"),
                        GardenProfitConfig.showDebug)
                .setDefaultValue(GardenProfitConfig.DEFAULT_SHOW_DEBUG)
                .setTooltip(Component.literal("Show debug messages in chat"))
                .setSaveConsumer(val -> GardenProfitConfig.showDebug = val)
                .build());

        general.addEntry(entryBuilder.startStrList(
                        Component.literal("Pet Tracker List"),
                        GardenProfitConfig.petTrackerList)
                .setDefaultValue(new ArrayList<>(GardenProfitConfig.DEFAULT_PET_TRACKER_LIST))
                .setTooltip(Component.literal("Format: PET_ID:Display Name:MaxLevel:RARITY"))
                .setSaveConsumer(val -> GardenProfitConfig.petTrackerList = new ArrayList<>(val))
                .build());

        // ============================================================
        // Session Profit HUD
        // ============================================================
        ConfigCategory sessionHud = builder.getOrCreateCategory(Component.literal("Session HUD"));

        sessionHud.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("Show Session Profit HUD"),
                        GardenProfitConfig.showSessionProfitHud)
                .setDefaultValue(GardenProfitConfig.DEFAULT_SHOW_SESSION_PROFIT_HUD)
                .setSaveConsumer(val -> GardenProfitConfig.showSessionProfitHud = val)
                .build());

        sessionHud.addEntry(entryBuilder.startIntField(
                        Component.literal("X Position"),
                        GardenProfitConfig.sessionProfitHudX)
                .setDefaultValue(GardenProfitConfig.DEFAULT_SESSION_PROFIT_HUD_X)
                .setMin(0).setMax(3840)
                .setSaveConsumer(val -> GardenProfitConfig.sessionProfitHudX = val)
                .build());

        sessionHud.addEntry(entryBuilder.startIntField(
                        Component.literal("Y Position"),
                        GardenProfitConfig.sessionProfitHudY)
                .setDefaultValue(GardenProfitConfig.DEFAULT_SESSION_PROFIT_HUD_Y)
                .setMin(0).setMax(2160)
                .setSaveConsumer(val -> GardenProfitConfig.sessionProfitHudY = val)
                .build());

        sessionHud.addEntry(entryBuilder.startFloatField(
                        Component.literal("Scale"),
                        GardenProfitConfig.sessionProfitHudScale)
                .setDefaultValue(GardenProfitConfig.DEFAULT_SESSION_PROFIT_HUD_SCALE)
                .setMin(0.5f).setMax(2.5f)
                .setSaveConsumer(val -> GardenProfitConfig.sessionProfitHudScale = val)
                .build());

        // ============================================================
        // Daily Profit HUD
        // ============================================================
        ConfigCategory dailyHud = builder.getOrCreateCategory(Component.literal("Daily HUD"));

        dailyHud.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("Show Daily Profit HUD"),
                        GardenProfitConfig.showDailyHud)
                .setDefaultValue(GardenProfitConfig.DEFAULT_SHOW_DAILY_HUD)
                .setSaveConsumer(val -> GardenProfitConfig.showDailyHud = val)
                .build());

        dailyHud.addEntry(entryBuilder.startIntField(
                        Component.literal("X Position"),
                        GardenProfitConfig.dailyHudX)
                .setDefaultValue(GardenProfitConfig.DEFAULT_DAILY_HUD_X)
                .setMin(0).setMax(3840)
                .setSaveConsumer(val -> GardenProfitConfig.dailyHudX = val)
                .build());

        dailyHud.addEntry(entryBuilder.startIntField(
                        Component.literal("Y Position"),
                        GardenProfitConfig.dailyHudY)
                .setDefaultValue(GardenProfitConfig.DEFAULT_DAILY_HUD_Y)
                .setMin(0).setMax(2160)
                .setSaveConsumer(val -> GardenProfitConfig.dailyHudY = val)
                .build());

        dailyHud.addEntry(entryBuilder.startFloatField(
                        Component.literal("Scale"),
                        GardenProfitConfig.dailyHudScale)
                .setDefaultValue(GardenProfitConfig.DEFAULT_DAILY_HUD_SCALE)
                .setMin(0.5f).setMax(2.5f)
                .setSaveConsumer(val -> GardenProfitConfig.dailyHudScale = val)
                .build());

        // ============================================================
        // Lifetime Profit HUD
        // ============================================================
        ConfigCategory lifetimeHud = builder.getOrCreateCategory(Component.literal("Lifetime HUD"));

        lifetimeHud.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("Show Lifetime Profit HUD"),
                        GardenProfitConfig.showLifetimeHud)
                .setDefaultValue(GardenProfitConfig.DEFAULT_SHOW_LIFETIME_HUD)
                .setSaveConsumer(val -> GardenProfitConfig.showLifetimeHud = val)
                .build());

        lifetimeHud.addEntry(entryBuilder.startIntField(
                        Component.literal("X Position"),
                        GardenProfitConfig.lifetimeHudX)
                .setDefaultValue(GardenProfitConfig.DEFAULT_LIFETIME_HUD_X)
                .setMin(0).setMax(3840)
                .setSaveConsumer(val -> GardenProfitConfig.lifetimeHudX = val)
                .build());

        lifetimeHud.addEntry(entryBuilder.startIntField(
                        Component.literal("Y Position"),
                        GardenProfitConfig.lifetimeHudY)
                .setDefaultValue(GardenProfitConfig.DEFAULT_LIFETIME_HUD_Y)
                .setMin(0).setMax(2160)
                .setSaveConsumer(val -> GardenProfitConfig.lifetimeHudY = val)
                .build());

        lifetimeHud.addEntry(entryBuilder.startFloatField(
                        Component.literal("Scale"),
                        GardenProfitConfig.lifetimeHudScale)
                .setDefaultValue(GardenProfitConfig.DEFAULT_LIFETIME_HUD_SCALE)
                .setMin(0.5f).setMax(2.5f)
                .setSaveConsumer(val -> GardenProfitConfig.lifetimeHudScale = val)
                .build());

        return builder.build();
    }
}
