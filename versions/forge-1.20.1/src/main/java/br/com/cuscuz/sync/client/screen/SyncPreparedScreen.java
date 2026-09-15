package br.com.cuscuz.sync.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public final class SyncPreparedScreen extends Screen {
    private final Screen parent;
    private final int operationCount;
    private final int blockedCount;
    private final Path blockedReport;

    public SyncPreparedScreen(Screen parent, int operationCount, int blockedCount, Path blockedReport) {
        super(Component.literal("Cuscuz Sync: alterações preparadas"));
        this.parent = parent;
        this.operationCount = operationCount;
        this.blockedCount = blockedCount;
        this.blockedReport = blockedReport;
    }

    @Override
    protected void init() {
        int y = height / 2 + 44;
        addRenderableWidget(Button.builder(Component.literal("Fechar e instalar agora"), button -> minecraft.stop())
                .bounds(width / 2 - 102, y, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Aplicar depois"), button -> minecraft.setScreen(parent))
                .bounds(width / 2 + 52, y, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 45, 0x77FF77);
        graphics.drawCenteredString(font,
                Component.literal(operationCount + " operações foram baixadas/verificadas e colocadas na fila."),
                width / 2, height / 2 - 18, 0xFFFFFF);
        if (blockedCount > 0) {
            graphics.drawCenteredString(font,
                    Component.literal(blockedCount + " mods continuam sem fonte."),
                    width / 2, height / 2 - 4, 0xFFAA55);
            if (blockedReport != null) {
                graphics.drawCenteredString(font,
                        Component.literal("Lista salva em .cuscuz-sync/reports/" + blockedReport.getFileName()),
                        width / 2, height / 2 + 8, 0xFFAA55);
            }
        }
        graphics.drawCenteredString(font,
                Component.literal("Ao fechar, o helper moverá extras para a quarentena e instalará os JARs."),
                width / 2, height / 2 + 22, 0xD0D0D0);
        graphics.drawCenteredString(font,
                Component.literal("Depois, abra o Minecraft novamente e conecte ao servidor."),
                width / 2, height / 2 + 36, 0xD0D0D0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
