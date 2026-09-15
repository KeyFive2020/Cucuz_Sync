package br.com.cuscuz.sync.client.screen;

import br.com.cuscuz.sync.client.ClientSyncController;
import br.com.cuscuz.sync.model.PlanAction;
import br.com.cuscuz.sync.model.SyncManifest;
import br.com.cuscuz.sync.model.SyncPlan;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class MissingSourcesScreen extends Screen {
    private static final int RED = 0xFF5555;

    private final Screen parent;
    private final ClientSyncController.ConnectionRequest request;
    private final SyncManifest manifest;
    private final SyncPlan plan;
    private final List<PlanAction> missing;
    private int page;

    public MissingSourcesScreen(Screen parent, ClientSyncController.ConnectionRequest request,
                                SyncManifest manifest, SyncPlan plan) {
        super(Component.literal("Cuscuz Sync: mods sem fonte"));
        this.parent = parent;
        this.request = request;
        this.manifest = manifest;
        this.plan = plan;
        this.missing = plan.actions().stream()
                .filter(action -> action.type() == PlanAction.Type.BLOCKED)
                .toList();
    }

    @Override
    protected void init() {
        int y = height - 28;
        addRenderableWidget(Button.builder(Component.literal("Salvar lista .txt"),
                        button -> ClientSyncController.exportMissingReport(request, this, manifest, plan))
                .bounds(width / 2 - 105, y, 130, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Voltar"), button -> minecraft.setScreen(parent))
                .bounds(width / 2 + 29, y, 76, 20).build());

        int pages = pageCount();
        Button previous = Button.builder(Component.literal("<"), button -> {
            page--;
            rebuildWidgets();
        }).bounds(12, y, 24, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), button -> {
            page++;
            rebuildWidgets();
        }).bounds(40, y, 24, 20).build();
        next.active = page + 1 < pages;
        addRenderableWidget(next);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 18, RED);
        graphics.drawCenteredString(font,
                Component.literal(missing.size() + " mods não foram encontrados em nenhuma fonte disponível."),
                width / 2, 36, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal("Configure CurseForge no servidor ou obtenha estes arquivos manualmente."),
                width / 2, 50, 0xA0A0A0);

        int start = page * pageSize();
        int end = Math.min(missing.size(), start + pageSize());
        int y = 70;
        for (int index = start; index < end; index++) {
            PlanAction action = missing.get(index);
            graphics.drawString(font,
                    "! " + action.displayName() + "  [" + action.modId() + "]  " + action.targetVersion(),
                    20, y, RED, false);
            y += 12;
        }
        graphics.drawString(font, "Página " + (page + 1) + '/' + pageCount(),
                72, height - 22, 0xA0A0A0, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private int pageSize() {
        return Math.max(4, (height - 116) / 12);
    }

    private int pageCount() {
        return Math.max(1, (missing.size() + pageSize() - 1) / pageSize());
    }
}
