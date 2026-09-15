package br.com.cuscuz.sync.server;

import br.com.cuscuz.sync.model.ManifestMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class ModrinthResolver {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private ModrinthResolver() {
    }

    static boolean resolve(ArtifactIdentity artifact, ManifestMod mod) throws IOException, InterruptedException {
        URI uri = URI.create("https://api.modrinth.com/v2/version_file/" + artifact.sha1() + "?algorithm=sha1");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "CuscuzSyncServer/0.3.3 (Minecraft 1.20.1; Forge)")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return false;
        }
        if (response.statusCode() != 200) {
            throw new IOException("A API Modrinth respondeu HTTP " + response.statusCode());
        }
        JsonObject version = JsonParser.parseString(response.body()).getAsJsonObject();
        boolean matched = apply(version, artifact, mod);
        if (matched && (!version.has("environment") || version.get("environment").isJsonNull())) {
            applyProjectEnvironment(fetchProject(mod.projectId), mod);
        }
        return matched;
    }

    static boolean apply(JsonObject version, ArtifactIdentity artifact, ManifestMod mod) throws IOException {
        if (!contains(version.getAsJsonArray("game_versions"), "1.20.1")
                || !contains(version.getAsJsonArray("loaders"), "forge")) {
            throw new IOException("A versão Modrinth encontrada não declara Forge 1.20.1.");
        }
        JsonObject selected = findExactFile(version.getAsJsonArray("files"), artifact.sha1());
        if (selected == null) {
            throw new IOException("A resposta Modrinth não contém o arquivo do hash consultado.");
        }
        mod.platform = ManifestMod.Platform.MODRINTH;
        mod.projectId = version.get("project_id").getAsString();
        mod.versionId = version.get("id").getAsString();
        mod.fileId = 0;
        mod.downloadUrl = selected.get("url").getAsString();
        mod.fileName = selected.get("filename").getAsString();
        mod.size = selected.get("size").getAsLong();
        JsonObject hashes = selected.getAsJsonObject("hashes");
        mod.sha1 = hashes.get("sha1").getAsString();
        mod.sha512 = hashes.has("sha512") ? hashes.get("sha512").getAsString() : artifact.sha512();
        applyEnvironment(version, mod);
        return true;
    }

    private static JsonObject findExactFile(JsonArray files, String sha1) {
        if (files == null) {
            return null;
        }
        for (JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();
            JsonObject hashes = file.getAsJsonObject("hashes");
            if (hashes != null && hashes.has("sha1") && sha1.equalsIgnoreCase(hashes.get("sha1").getAsString())) {
                return file;
            }
        }
        return null;
    }

    private static boolean contains(JsonArray values, String expected) {
        if (values == null) {
            return false;
        }
        for (JsonElement value : values) {
            if (expected.equalsIgnoreCase(value.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static void applyEnvironment(JsonObject version, ManifestMod mod) {
        if (!version.has("environment") || version.get("environment").isJsonNull()) {
            return;
        }
        String environment = version.get("environment").getAsString();
        if (environment.contains("server_only") || environment.equals("dedicated_server_only")) {
            mod.environment = ManifestMod.Environment.SERVER;
        } else if (environment.contains("client_only") || environment.equals("singleplayer_only")) {
            mod.environment = ManifestMod.Environment.CLIENT;
        }
    }

    private static JsonObject fetchProject(String projectId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/" + projectId))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "CuscuzSyncServer/0.3.3 (Minecraft 1.20.1; Forge)")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("A API Modrinth respondeu HTTP " + response.statusCode() + " ao consultar o projeto.");
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    static void applyProjectEnvironment(JsonObject project, ManifestMod mod) {
        if (project.has("client_side") && project.has("server_side")) {
            String client = project.get("client_side").getAsString();
            String server = project.get("server_side").getAsString();
            if ("unsupported".equals(client) && !"unsupported".equals(server)) {
                mod.environment = ManifestMod.Environment.SERVER;
                return;
            }
            if ("unsupported".equals(server) && !"unsupported".equals(client)) {
                mod.environment = ManifestMod.Environment.CLIENT;
                return;
            }
        }
        JsonArray environments = project.getAsJsonArray("environment");
        if (environments != null && environments.size() == 1) {
            JsonObject wrapper = new JsonObject();
            wrapper.add("environment", environments.get(0));
            applyEnvironment(wrapper, mod);
        }
    }
}
