package br.com.cuscuz.sync.client.screen;

import br.com.cuscuz.sync.client.ClientSyncController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SyncErrorScreen extends Screen {
    private final ClientSyncController.ConnectionRequest request;
    private final String error;

    public SyncErrorScreen(ClientSyncController.ConnectionRequest request, String error) {
        super(Component.literal("Cuscuz Sync: não foi possível analisar"));
        this.request = request;
        this.error = error;
    }

    @Override
    protected void init() {
        int y = height - 52;
        addRenderableWidget(Button.builder(Component.literal("Tentar novamente"), button ->
                ClientSyncController.begin(request)).bounds(width / 2 - 154, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Entrar sem sincronizar"), button ->
                ClientSyncController.connect(request)).bounds(width / 2 - 50, y, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Voltar"), button -> minecraft.setScreen(request.parent()))
                .bounds(width / 2 + 94, y, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 34, 0xFF7777);
        graphics.drawCenteredString(font,
                Component.literal("O servidor pode estar sem o endpoint do Cuscuz Sync ou com a porta bloqueada."),
                width / 2, 62, 0xD0D0D0);
        int y = 84;
        for (var line : font.split(Component.literal(error), Math.max(200, width - 60))) {
            graphics.drawCenteredString(font, line, width / 2, y, 0xFFFFFF);
            y += 11;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(request.parent());
    }
}
