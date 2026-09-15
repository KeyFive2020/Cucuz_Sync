package br.com.cuscuz.sync.server;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class ManifestRefreshService {
    static final long REFRESH_MINUTES = 5;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static ScheduledExecutorService executor;
    private static int minecraftPort;

    private ManifestRefreshService() {
    }

    static synchronized void start(int minecraftPort) {
        stop();
        ManifestRefreshService.minecraftPort = minecraftPort;
        byte[] cached = ServerManifestRepository.loadCached();
        if (cached != null) {
            publish(cached);
            ManifestHttpServer.start(minecraftPort, cached);
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Cuscuz Sync manifest refresh");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(ManifestRefreshService::refreshSafely,
                0, REFRESH_MINUTES, TimeUnit.MINUTES);
        LOGGER.info("Verificação automática do manifesto agendada a cada {} minutos.", REFRESH_MINUTES);
    }

    static synchronized void stop() {
        ScheduledExecutorService running = executor;
        executor = null;
        if (running != null) {
            running.shutdownNow();
        }
    }

    private static void refreshSafely() {
        try {
            refreshAndPublish();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            LOGGER.error("Falha ao atualizar o manifesto; a última versão válida continua publicada.", exception);
        }
    }

    private static byte[] refreshAndPublish() throws Exception {
        byte[] manifest = ServerManifestRepository.refresh();
        publish(manifest);
        ManifestHttpServer.update(manifest);
        ManifestHttpServer.start(minecraftPort, manifest);
        return manifest;
    }

    private static void publish(byte[] manifest) {
        try {
            StatusManifestPublisher.publish(manifest);
        } catch (IOException exception) {
            LOGGER.error("Manifesto grande demais para o Server List Ping; o HTTP continuará disponível.", exception);
        }
    }
}
