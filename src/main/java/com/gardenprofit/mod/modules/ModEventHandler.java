package com.gardenprofit.mod.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Observer interface for mod event handling.
 * Implementations are dispatched in priority order by {@link EventDispatcher}.
 */
public interface ModEventHandler {

    /**
     * Processing priority. Lower values are processed first.
     * T0 = 0 (SackTracker), T1 = 1 (ChatTracker), T2 = 2 (InventoryTracker).
     */
    int getPriority();

    /**
     * Called when a game chat message is received (non-overlay, in Garden).
     */
    default void onChatMessage(Component message) {}

    /**
     * Called every client tick while in the Garden.
     */
    default void onTick(Minecraft client) {}

    /**
     * Called when the player opens an inventory/GUI.
     */
    default void onInventoryOpen(String inventoryName) {}

    /**
     * Called when the player closes an inventory/GUI.
     */
    default void onInventoryClose() {}
}