package br.com.cuscuz.sync.client.screen;

import br.com.cuscuz.sync.client.ClientSyncController;
import br.com.cuscuz.sync.model.PlanAction;
import br.com.cuscuz.sync.model.SyncManifest;
import br.com.cuscuz.sync.model.SyncPlan;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class SyncReviewScreen extends Screen {
    private final ClientSyncController.ConnectionRequest request;
    private final SyncManifest manifest;
    private final SyncPlan plan;
    private final List<String> rows;
    private int page;

    public SyncReviewScreen(ClientSyncController.ConnectionRequest request,
                            SyncManifest manifest, SyncPlan plan) {
        super(Component.literal("Cuscuz Sync: alterações encontradas"));
        this.request = request;
        this.manifest = manifest;
        this.plan = plan;
        this.rows = buildRows(plan);
    }

    @Override
    protected void init() {
        int y = height - 28;
        Button apply = Button.builder(Component.literal(plan.hasBlockedActions()
                                ? "Há itens sem download" : "Permitir e preparar"),
                        button -> ClientSyncController.prepare(request, manifest, plan))
                .bounds(width / 2 - 100, y, 145, 20).build();
        apply.active = !plan.hasBlockedActions();
        addRenderableWidget(apply);
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), button -> minecraft.setScreen(request.parent()))
                .bounds(width / 2 + 49, y, 70, 20).build());

        int pageSize = pageSize();
        int pages = Math.max(1, (rows.size() + pageSize - 1) / pageSize);
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
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        String summary = plan.count(PlanAction.Type.INSTALL) + " instalar, "
                + plan.count(PlanAction.Type.UPDATE) + " atualizar, "
                + plan.count(PlanAction.Type.QUARANTINE) + " colocar em quarentena, "
                + plan.count(PlanAction.Type.BLOCKED) + " bloqueados";
        graphics.drawCenteredString(font, Component.literal(summary), width / 2, 36, 0xD0D0D0);
        graphics.drawCenteredString(font,
                Component.literal("Nada será alterado sem sua confirmação. Use < e > para ver todos os mods."),
                width / 2, 50, 0xA0A0A0);

        int pageSize = pageSize();
        int start = page * pageSize;
        int end = Math.min(rows.size(), start + pageSize);
        int y = 70;
        for (int index = start; index < end; index++) {
            graphics.drawString(font, rows.get(index), 20, y, 0xFFFFFF, false);
            y += 12;
        }
        int pages = Math.max(1, (rows.size() + pageSize - 1) / pageSize);
        graphics.drawString(font, "Página " + (page + 1) + '/' + pages, 72, height - 22, 0xA0A0A0, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(request.parent());
    }

    private int pageSize() {
        return Math.max(4, (height - 116) / 12);
    }

    private static List<String> buildRows(SyncPlan plan) {
        List<String> result = new ArrayList<>();
        for (PlanAction action : plan.actions()) {
            String row = switch (action.type()) {
                case INSTALL -> "+ INSTALAR  " + action.displayName() + "  " + action.targetVersion();
                case UPDATE -> "~ ATUALIZAR " + action.displayName() + "  "
                        + action.currentVersion() + " -> " + action.targetVersion();
                case QUARANTINE -> "- QUARENTENA " + action.displayName() + "  " + action.currentVersion();
                case BLOCKED -> "! SEM DOWNLOAD " + action.displayName() + "  " + action.targetVersion();
            };
            result.add(row);
        }
        return List.copyOf(result);
    }
}
