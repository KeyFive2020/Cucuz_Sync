package br.com.cuscuz.sync.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public final class SyncReportSavedScreen extends Screen {
    private final Screen parent;
    private final Path report;

    public SyncReportSavedScreen(Screen parent, Path report) {
        super(Component.literal("Cuscuz Sync: lista salva"));
        this.parent = parent;
        this.report = report;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Voltar"), button -> minecraft.setScreen(parent))
                .bounds(width / 2 - 50, height / 2 + 38, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 34, 0x77FF77);
        graphics.drawCenteredString(font, Component.literal("Arquivo criado:"),
                width / 2, height / 2 - 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(report.getFileName().toString()),
                width / 2, height / 2 + 8, 0xD0D0D0);
        graphics.drawCenteredString(font, Component.literal("Pasta: .cuscuz-sync/reports"),
                width / 2, height / 2 + 22, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
