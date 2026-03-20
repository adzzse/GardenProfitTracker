package com.gardenprofit.mod.gui;

import com.gardenprofit.mod.GardenProfitClient;
import com.gardenprofit.mod.GardenProfitConfig;
import com.gardenprofit.mod.modules.ProfitManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        general.addEntry(new ActionButtonEntry(Component.literal("Toggle HUD"), GardenProfitClient::runToggleAction));
        general.addEntry(new ActionButtonEntry(Component.literal("Price Mode"), GardenProfitClient::runPriceModeAction));
        general.addEntry(new ActionButtonEntry(Component.literal("Fetch"), GardenProfitClient::runFetchAction));

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("HUD Visible"),
                        !GardenProfitConfig.hudHidden)
                .setDefaultValue(!GardenProfitConfig.DEFAULT_HUD_HIDDEN)
                .setTooltip(Component.literal("If off, all profit HUD overlays are hidden"))
                .setSaveConsumer(val -> GardenProfitConfig.hudHidden = !val)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("Bazaar Price Mode: Insta-Sell"),
                        GardenProfitConfig.useBazaarSellPrice)
                .setDefaultValue(GardenProfitConfig.DEFAULT_USE_BAZAAR_SELL_PRICE)
                .setTooltip(Component.literal("On = Insta-Sell price, Off = Insta-Buy price"))
                .setSaveConsumer(val -> {
                    GardenProfitConfig.useBazaarSellPrice = val;
                    ProfitManager.onPriceModeChanged();
                })
                .build());

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

        general.addEntry(entryBuilder.startIntSlider(
                        Component.literal("HUD Opacity"),
                        (int) (GardenProfitConfig.hudOpacity * 100),
                        10, 100)
                .setDefaultValue((int) (GardenProfitConfig.DEFAULT_HUD_OPACITY * 100))
                .setTooltip(Component.literal("Transparency of the HUD background (10-100%)"))
                .setSaveConsumer(val -> GardenProfitConfig.hudOpacity = val / 100f)
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

        sessionHud.addEntry(new ActionButtonEntry(Component.literal("Reset Session"), GardenProfitClient::runResetAction));

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

        dailyHud.addEntry(new ActionButtonEntry(Component.literal("Reset Daily"), GardenProfitClient::runResetDailyAction));

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

        lifetimeHud.addEntry(new ActionButtonEntry(Component.literal("Reset Lifetime"), GardenProfitClient::runResetLifetimeAction));

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

    private static final class ActionButtonEntry extends TooltipListEntry<Object> {
        private static final int BUTTON_WIDTH = 170;
        private static final int BUTTON_HEIGHT = 20;

        private final Button button;

        private ActionButtonEntry(Component label, Runnable action) {
            super(Component.empty(), () -> Optional.empty());
            this.button = Button.builder(label, b -> action.run())
                    .bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int index,
                int y,
                int x,
                int entryWidth,
                int entryHeight,
                int mouseX,
                int mouseY,
                boolean hovered,
                float delta
        ) {
            super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
            int buttonX = x + (entryWidth - BUTTON_WIDTH) / 2;
            this.button.setPosition(buttonX, y);
            this.button.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public int getItemHeight() {
            return BUTTON_HEIGHT;
        }

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public Optional<Object> getDefaultValue() {
            return Optional.empty();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(button);
        }
    }
}
