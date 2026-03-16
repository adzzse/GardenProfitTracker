package com.gardenprofit.mod.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Maintains a priority-sorted list of {@link ModEventHandler} instances and
 * dispatches chat/inventory/tick events to them in order (T0 first, T2 last).
 */
public final class EventDispatcher {

    private static final EventDispatcher INSTANCE = new EventDispatcher();

    private final List<ModEventHandler> handlers = new ArrayList<>();

    private EventDispatcher() {}

    public static EventDispatcher getInstance() {
        return INSTANCE;
    }

    /**
     * Register a handler. Handlers are kept sorted by priority (ascending).
     */
    public void register(ModEventHandler handler) {
        handlers.add(handler);
        handlers.sort(Comparator.comparingInt(ModEventHandler::getPriority));
    }

    /**
     * Dispatch a chat message to all registered handlers in priority order.
     */
    public void dispatchChatMessage(Component message) {
        for (ModEventHandler handler : handlers) {
            handler.onChatMessage(message);
        }
    }

    /**
     * Dispatch a tick event to all registered handlers in priority order.
     */
    public void dispatchTick(Minecraft client) {
        for (ModEventHandler handler : handlers) {
            handler.onTick(client);
        }
    }

    /**
     * Dispatch an inventory open event to all registered handlers in priority order.
     */
    public void dispatchInventoryOpen(String inventoryName) {
        for (ModEventHandler handler : handlers) {
            handler.onInventoryOpen(inventoryName);
        }
    }

    /**
     * Dispatch an inventory close event to all registered handlers in priority order.
     */
    public void dispatchInventoryClose() {
        for (ModEventHandler handler : handlers) {
            handler.onInventoryClose();
        }
    }
}

