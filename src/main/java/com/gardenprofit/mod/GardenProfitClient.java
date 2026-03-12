package com.gardenprofit.mod;

import com.gardenprofit.mod.gui.ProfitHudRenderer;
import com.gardenprofit.mod.modules.PetXpTracker;
import com.gardenprofit.mod.modules.ProfitManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

public class GardenProfitClient implements ClientModInitializer {

    private static int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        GardenProfitConfig.load();

        // Register /gardenprofit command to open the HUD edit screen
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommandManager.literal("gardenprofit")
                .executes(context -> {
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().setScreen(com.gardenprofit.mod.gui.HudEditScreen.create(null));
                    });
                    return 1;
                })
            );
        });

        // Register HUD renderer
        ProfitHudRenderer.register();
        ProfitHudRenderer.startSession();

        // Register chat message listener for profit tracking
        ClientReceiveMessageEvents.GAME.register((message, isOverlay) -> {
            if (isOverlay) return;
            ProfitManager.handleChatMessage(message);
        });

        // Register tick event for profit updates
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            tickCounter++;
            // Update profit every 5 ticks
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