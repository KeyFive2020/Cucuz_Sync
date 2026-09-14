package br.com.cuscuz.sync.server;

import br.com.cuscuz.sync.manifest.ManifestException;
import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.Executors;

public final class ManifestHttpServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile HttpServer server;
    private static volatile byte[] manifestBytes = new byte[0];

    private ManifestHttpServer() {
    }

    public static synchronized void start(int minecraftPort) {
        try {
            start(minecraftPort, ServerManifestRepository.loadOrCreate());
        } catch (IOException | ManifestException exception) {
            LOGGER.error("Não foi possível preparar o endpoint do Cuscuz Sync.", exception);
        }
    }

    public static synchronized void start(int minecraftPort, byte[] manifest) {
        if (server != null || !Boolean.parseBoolean(System.getProperty("cuscuzSync.httpEnabled", "true"))) {
            return;
        }
        try {
            manifestBytes = manifest.clone();
            int port = Integer.getInteger("cuscuzSync.httpPort", minecraftPort + 1);
            String bindAddress = System.getProperty("cuscuzSync.bindAddress", "0.0.0.0");
            HttpServer created = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            created.createContext("/cuscuz-sync/manifest", ManifestHttpServer::serveManifest);
            created.createContext("/cuscuz-sync/health", ManifestHttpServer::serveHealth);
            created.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "Cuscuz Sync HTTP");
                thread.setDaemon(true);
                return thread;
            }));
            created.start();
            server = created;
            LOGGER.info("Manifesto Cuscuz Sync disponível em {}:{} (arquivo: {})",
                    bindAddress, created.getAddress().getPort(), ServerManifestRepository.manifestPath());
        } catch (IOException exception) {
            LOGGER.error("Não foi possível iniciar o endpoint do Cuscuz Sync.", exception);
        }
    }

    public static synchronized void stop() {
        HttpServer running = server;
        server = null;
        if (running != null) {
            running.stop(1);
        }
    }

    public static synchronized void reload() throws IOException, ManifestException {
        manifestBytes = ServerManifestRepository.loadOrCreate();
    }

    private static void serveManifest(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "Somente GET.", "text/plain; charset=utf-8");
            return;
        }
        byte[] snapshot = manifestBytes;
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Cuscuz-SHA256", sha256(snapshot));
        exchange.sendResponseHeaders(200, snapshot.length);
        try (var body = exchange.getResponseBody()) {
            body.write(snapshot);
        }
    }

    private static void serveHealth(HttpExchange exchange) throws IOException {
        respond(exchange, 200, "ok", "text/plain; charset=utf-8");
    }

    private static void respond(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
