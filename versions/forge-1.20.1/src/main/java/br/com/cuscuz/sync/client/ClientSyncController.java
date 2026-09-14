package br.com.cuscuz.sync.client;

import br.com.cuscuz.sync.client.screen.SyncErrorScreen;
import br.com.cuscuz.sync.client.screen.SyncPreparedScreen;
import br.com.cuscuz.sync.client.screen.SyncProgressScreen;
import br.com.cuscuz.sync.client.screen.SyncReviewScreen;
import br.com.cuscuz.sync.install.SyncInstaller;
import br.com.cuscuz.sync.inventory.ModInventory;
import br.com.cuscuz.sync.model.SyncManifest;
import br.com.cuscuz.sync.model.SyncPlan;
import br.com.cuscuz.sync.plan.SyncPlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientSyncController {
    private static final AtomicBoolean ANALYZING = new AtomicBoolean();
    private static final AtomicBoolean PREPARING = new AtomicBoolean();
    private static final AtomicBoolean CONNECTION_PASS = new AtomicBoolean();

    private ClientSyncController() {
    }

    public static boolean consumeConnectionPass() {
        return CONNECTION_PASS.compareAndSet(true, false);
    }

    public static void begin(ConnectionRequest request) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ANALYZING.compareAndSet(false, true)) {
            return;
        }
        minecraft.setScreen(new SyncProgressScreen(Component.literal("Analisando o perfil do servidor...")));
        CompletableFuture.supplyAsync(() -> analyze(request.serverData())).whenComplete((analysis, failure) ->
                minecraft.execute(() -> {
                    ANALYZING.set(false);
                    if (failure != null) {
                        minecraft.setScreen(new SyncErrorScreen(request, readable(failure)));
                        return;
                    }
                    if (analysis.plan().isCompatible()) {
                        connect(request);
                        return;
                    }
                    minecraft.setScreen(new SyncReviewScreen(request, analysis.manifest(), analysis.plan()));
                }));
    }

    public static void prepare(ConnectionRequest request, SyncManifest manifest, SyncPlan plan) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!PREPARING.compareAndSet(false, true)) {
            return;
        }
        minecraft.setScreen(new SyncProgressScreen(Component.literal("Baixando e verificando os mods...")));
        CompletableFuture.supplyAsync(() -> {
            try {
                return SyncInstaller.prepare(minecraft.gameDirectory.toPath(), manifest, plan);
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }).whenComplete((prepared, failure) -> minecraft.execute(() -> {
            PREPARING.set(false);
            if (failure != null) {
                minecraft.setScreen(new SyncErrorScreen(request, readable(failure)));
                return;
            }
            minecraft.setScreen(new SyncPreparedScreen(request.parent(), prepared.operationCount()));
        }));
    }

    public static void connect(ConnectionRequest request) {
        CONNECTION_PASS.set(true);
        Minecraft minecraft = Minecraft.getInstance();
        ConnectScreen.startConnecting(request.parent(), minecraft, request.address(),
                request.serverData(), request.quickPlay());
    }

    private static Analysis analyze(ServerData serverData) {
        try {
            SyncManifest manifest = ManifestClient.fetch(serverData);
            return new Analysis(manifest, SyncPlanner.compare(manifest, ModInventory.scan()));
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }

    private static String readable(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current.getMessage() == null) && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record Analysis(SyncManifest manifest, SyncPlan plan) {
    }

    public record ConnectionRequest(Screen parent, ServerAddress address,
                                    ServerData serverData, boolean quickPlay) {
    }
}
