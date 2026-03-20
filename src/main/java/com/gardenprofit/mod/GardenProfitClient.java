package com.gardenprofit.mod;

import com.gardenprofit.mod.gui.ProfitHudRenderer;
import com.gardenprofit.mod.modules.ChatMessageParser;
// import com.gardenprofit.mod.modules.InventoryTracker;
import com.gardenprofit.mod.modules.LocationTracker;
import com.gardenprofit.mod.modules.PetXpTracker;
import com.gardenprofit.mod.modules.ProfitManager;
import com.gardenprofit.mod.modules.SackTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

public class GardenProfitClient implements ClientModInitializer {

    private static int tickCounter = 0;

    private static boolean openConfigScreenNextTick = false;
    private static Screen lastScreen = null;

    @Override
    public void onInitializeClient() {
        GardenProfitConfig.load();
        ProfitManager.loadLifetime();
        ProfitManager.loadDaily();

        // Cache inventory/purse on world join
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
        //    InventoryTracker.onWorldSwitch();
            LocationTracker.onWorldSwitch();
            ProfitManager.onWorldSwitch(client);
            lastScreen = client.screen;
        });

        // Register /gardenprofit and /gp commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher2, buildContext) -> {
            dispatcher2.register(buildCommandTree("gardenprofit"));
            dispatcher2.register(buildCommandTree("gp"));
        });

        // Register HUD renderer
        ProfitHudRenderer.register();
        ProfitHudRenderer.startSession();

        // Register chat message listener (sack parsing first, then generic parsing)
        ClientReceiveMessageEvents.GAME.register((message, isOverlay) -> {
            if (isOverlay) return;
            // Only track drops/sacks while in the Garden
            if (!LocationTracker.isInGarden()) return;
            // Fixed order: sack parsing first, then generic chat parsing.
            SackTracker.handleChatMessage(message);
            ChatMessageParser.getInstance().handleChatMessage(message);
        });

        // Register tick event for profit updates and inventory tracking
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfigScreenNextTick) {
                openConfigScreenNextTick = false;
                net.minecraft.client.gui.screens.Screen screen = com.gardenprofit.mod.gui.HudEditScreen.create(client.screen);
                client.setScreen(screen);
            }

            if (client.player == null) return;

            Screen currentScreen = client.screen;
            if (currentScreen != lastScreen) {
                if (lastScreen instanceof AbstractContainerScreen<?>) {
                    SackTracker.getInstance().onInventoryClose();
                }
                if (currentScreen instanceof AbstractContainerScreen<?> handledScreen) {
                    SackTracker.getInstance().onInventoryOpen(handledScreen.getTitle().getString());
                }
                lastScreen = currentScreen;
            }

            tickCounter++;

            // Update location detection every 20 ticks
            if (tickCounter % 20 == 0) {
                LocationTracker.tick(client);
            }

            // Only run tracking modules while in the Garden
            if (!LocationTracker.isInGarden()) return;

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

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> buildCommandTree(String name) {
        return ClientCommandManager.literal(name)
            .executes(context -> {
                openConfigScreenNextTick = true;
                return 1;
            })
            .then(ClientCommandManager.literal("reset")
                .executes(context -> {
                    runResetAction();
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("toggle")
                .executes(context -> {
                    runToggleAction();
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("pricemode")
                .executes(context -> {
                    runPriceModeAction();
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("fetch")
                .executes(context -> {
                    runFetchAction();
                    return 1;
                })
            );
    }

    public static void runResetAction() {
        ProfitManager.resetSession();
        ProfitHudRenderer.startSession();
        LocationTracker.resetUptime();
        com.gardenprofit.mod.util.ClientUtils.sendDebugMessage(Minecraft.getInstance(), "Session reset.");
    }

    public static void runToggleAction() {
        GardenProfitConfig.hudHidden = !GardenProfitConfig.hudHidden;
        GardenProfitConfig.save();
        String status = GardenProfitConfig.hudHidden ? "\u00A7cHidden" : "\u00A7aVisible";
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("\u00A7a[GardenProfit] HUD: " + status), false);
        }
    }

    public static void runPriceModeAction() {
        GardenProfitConfig.useBazaarSellPrice = !GardenProfitConfig.useBazaarSellPrice;
        GardenProfitConfig.save();
        String mode = GardenProfitConfig.useBazaarSellPrice ? "Insta-Sell" : "Insta-Buy";
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("\u00A7a[GardenProfit] Bazaar price mode: \u00A7e" + mode), false);
        }
        ProfitManager.onPriceModeChanged();
    }

    public static void runFetchAction() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("\u00A7a[GardenProfit] \u00A7eManually fetching prices..."), false);
        }
        ProfitManager.fetchBazaarPrices();
    }
}
