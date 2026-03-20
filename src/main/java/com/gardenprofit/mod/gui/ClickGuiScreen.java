package com.gardenprofit.mod.gui;

import com.gardenprofit.mod.GardenProfitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ClickGuiScreen extends Screen {
    private final Screen parent;
    private boolean prevHudHidden;

    public ClickGuiScreen(Screen parent) {
        super(Component.literal("Garden Profit HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        prevHudHidden = GardenProfitConfig.hudHidden;
        // Keep the user's enabled HUD modes unchanged; only ensure HUD is not hidden while editing.
        GardenProfitConfig.hudHidden = false;
    }

    @Override
    public void removed() {
        GardenProfitConfig.hudHidden = prevHudHidden;
        ProfitHudRenderer.endDrag();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Keep parent screen visible (including inventory) and draw only the editable HUD above it.
        if (parent != null) {
            parent.render(g, mouseX, mouseY, partialTick);
        }
        ProfitHudRenderer.renderInEditMode(g, Minecraft.getInstance());
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && ProfitHudRenderer.isHovered(event.x(), event.y())) {
            ProfitHudRenderer.startDrag(event.x(), event.y(), false);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (ProfitHudRenderer.isInteracting()) {
            ProfitHudRenderer.drag(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        ProfitHudRenderer.endDrag();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ProfitHudRenderer.adjustHoveredScale(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
