package com.gardenprofit.mod;

import com.gardenprofit.mod.gui.ProfitHudRenderer;
import com.gardenprofit.mod.modules.InventoryTracker;
import com.gardenprofit.mod.modules.LocationTracker;
import com.gardenprofit.mod.modules.PetXpTracker;
import com.gardenprofit.mod.modules.ProfitManager;
import com.gardenprofit.mod.modules.SackTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

public class GardenProfitClient implements ClientModInitializer {

    private static int tickCounter = 0;

    private static boolean openConfigScreenNextTick = false;

    @Override
    public void onInitializeClient() {
        GardenProfitConfig.load();

        // Register /gardenprofit command to open the HUD edit screen
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommandManager.literal("gardenprofit")
                .executes(context -> {
                    openConfigScreenNextTick = true;
                    return 1;
                })
                .then(ClientCommandManager.literal("reset")
                    .executes(context -> {
                        ProfitManager.resetSession();
                        ProfitHudRenderer.startSession();
                        LocationTracker.resetUptime();
                        com.gardenprofit.mod.util.ClientUtils.sendDebugMessage(Minecraft.getInstance(), "Session reset.");
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("hide")
                    .executes(context -> {
                        GardenProfitConfig.showSessionProfitHud = false;
                        GardenProfitConfig.showDailyHud = false;
                        GardenProfitConfig.showLifetimeHud = false;
                        GardenProfitConfig.save();
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("\u00A7a[GardenProfit] HUD hidden."), false);
                        }
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("show")
                    .executes(context -> {
                        GardenProfitConfig.showSessionProfitHud = true;
                        GardenProfitConfig.showDailyHud = true;
                        GardenProfitConfig.showLifetimeHud = true;
                        GardenProfitConfig.save();
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("\u00A7a[GardenProfit] HUD visible."), false);
                        }
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("pricemode")
                    .executes(context -> {
                        GardenProfitConfig.useBazaarSellPrice = !GardenProfitConfig.useBazaarSellPrice;
                        GardenProfitConfig.save();
                        String mode = GardenProfitConfig.useBazaarSellPrice ? "Insta-Sell" : "Insta-Buy";
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("\u00A7a[GardenProfit] Bazaar price mode: \u00A7e" + mode + "\u00A7a. Re-fetching prices..."), false);
                        }
                        ProfitManager.fetchBazaarPrices();
                        return 1;
                    })
                )
            );
        });

        // Register HUD renderer
        ProfitHudRenderer.register();
        ProfitHudRenderer.startSession();

        // Register chat message listener for profit tracking + sack tracking
        ClientReceiveMessageEvents.GAME.register((message, isOverlay) -> {
            if (isOverlay) return;
            // Only track drops/sacks while in the Garden
            if (!LocationTracker.isInGarden()) return;
            // Try sack tracking first (precise crop data from hover text)
            SackTracker.handleChatMessage(message);
            // Then handle other chat-based tracking (pest drops, rare drops, etc.)
            ProfitManager.handleChatMessage(message);
        });

        // Register tick event for profit updates and inventory tracking
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfigScreenNextTick) {
                openConfigScreenNextTick = false;
                net.minecraft.client.gui.screens.Screen screen = com.gardenprofit.mod.gui.HudEditScreen.create(client.screen);
                client.setScreen(screen);
            }

            if (client.player == null) return;

            tickCounter++;

            // Update location detection every 20 ticks
            if (tickCounter % 20 == 0) {
                LocationTracker.tick(client);
            }

            // Only run tracking modules while in the Garden
            if (!LocationTracker.isInGarden()) return;

            // Inventory diff tracking every tick for non-sack items
            InventoryTracker.tick(client);

            // Update profit (purse tracking) every 5 ticks
            if (tickCounter % 5 == 0) {
                ProfitManager.update(client);
            }
            // Update pet XP every 20 ticks
            if (tickCounter % 20 == 0) {
                PetXpTracker.update(client);
            }
        });
    }
}