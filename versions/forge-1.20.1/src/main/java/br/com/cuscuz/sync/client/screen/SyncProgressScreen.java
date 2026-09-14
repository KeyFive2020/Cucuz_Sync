package br.com.cuscuz.sync.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SyncProgressScreen extends Screen {
    private final Component message;

    public SyncProgressScreen(Component message) {
        super(Component.literal("Cuscuz Sync"));
        this.message = message;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 30, 0xFFFFFF);
        graphics.drawCenteredString(font, message, width / 2, height / 2, 0xD0D0D0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
