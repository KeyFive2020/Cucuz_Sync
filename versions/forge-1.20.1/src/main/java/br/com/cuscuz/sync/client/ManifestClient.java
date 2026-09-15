package br.com.cuscuz.sync.client;

import br.com.cuscuz.sync.manifest.ManifestCodec;
import br.com.cuscuz.sync.manifest.ManifestException;
import br.com.cuscuz.sync.model.SyncManifest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ManifestClient {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ManifestClient() {
    }

    public static SyncManifest fetch(ServerData serverData) throws IOException, InterruptedException, ManifestException {
        URI override = resolveOverride(serverData.ip);
        if (override != null) {
            return fetchHttp(override);
        }

        Exception pingFailure;
        try {
            return StatusPingManifestClient.fetch(serverData.ip);
        } catch (IOException exception) {
            pingFailure = exception;
        }

        URI endpoint = defaultEndpoint(serverData.ip);
        try {
            return fetchHttp(endpoint);
        } catch (IOException | ManifestException httpFailure) {
            IOException combined = new IOException("Não foi possível obter o manifesto pela porta Minecraft nem por "
                    + endpoint + ". Ping: " + message(pingFailure) + "; HTTP: " + message(httpFailure), httpFailure);
            combined.addSuppressed(pingFailure);
            throw combined;
        }
    }

    private static SyncManifest fetchHttp(URI endpoint)
            throws IOException, InterruptedException, ManifestException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "CuscuzSync/0.3.3 (Minecraft 1.20.1; Forge)")
                .GET()
                .build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("O endpoint respondeu HTTP " + response.statusCode() + '.');
        }
        return ManifestCodec.decode(readLimited(response.body()));
    }

    private static URI resolveOverride(String serverAddress) throws IOException {
        Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve(".cuscuz-sync");
        Path settingsPath = directory.resolve("servers.json");
        ClientServerSettings settings = loadOrCreateSettings(directory, settingsPath);
        String override = settings.overrides.get(serverAddress);
        if (override == null || override.isBlank()) {
            return null;
        }
        try {
            URI endpoint = new URI(override);
            validateEndpoint(endpoint);
            return endpoint;
        } catch (URISyntaxException exception) {
            throw new IOException("Endereço do manifesto inválido.", exception);
        }
    }

    private static URI defaultEndpoint(String serverAddress) throws IOException {
        ServerAddress parsed = ServerAddress.parseString(serverAddress);
        ResolvedServerAddress resolved = ServerNameResolver.DEFAULT.resolveAddress(parsed)
                .orElseThrow(() -> new IOException("Não foi possível resolver o endereço do servidor."));
        if (resolved.getPort() == 65_535) {
            throw new IOException("Não existe porta HTTP automática depois da porta 65535.");
        }
        try {
            URI endpoint = new URI("http", null, resolved.getHostIp(), resolved.getPort() + 1,
                    "/cuscuz-sync/manifest", null, null);
            validateEndpoint(endpoint);
            return endpoint;
        } catch (URISyntaxException exception) {
            throw new IOException("Endereço do manifesto inválido.", exception);
        }
    }

    private static ClientServerSettings loadOrCreateSettings(Path directory, Path settingsPath) throws IOException {
        Files.createDirectories(directory);
        if (!Files.exists(settingsPath)) {
            ClientServerSettings initial = new ClientServerSettings();
            Files.writeString(settingsPath, GSON.toJson(initial), StandardCharsets.UTF_8);
            return initial;
        }
        String json = Files.readString(settingsPath, StandardCharsets.UTF_8);
        ClientServerSettings loaded = GSON.fromJson(json, ClientServerSettings.class);
        return loaded == null || loaded.overrides == null ? new ClientServerSettings() : loaded;
    }

    private static void validateEndpoint(URI endpoint) throws IOException {
        if (!("https".equalsIgnoreCase(endpoint.getScheme()) || "http".equalsIgnoreCase(endpoint.getScheme()))
                || endpoint.getHost() == null || endpoint.getUserInfo() != null || endpoint.getFragment() != null) {
            throw new IOException("O endpoint precisa ser HTTP(S), sem credenciais ou fragmentos.");
        }
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > ManifestCodec.MAX_MANIFEST_BYTES) {
                    throw new IOException("O manifesto recebido excede 2 MiB.");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String message(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private static final class ClientServerSettings {
        String note = "Use overrides para servidores com HTTPS ou porta de manifesto personalizada.";
        Map<String, String> overrides = new LinkedHashMap<>();
    }
}
