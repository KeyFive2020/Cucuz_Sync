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

/** Descobre um arquivo no CurseForge pelo fingerprint do JAR local. */
final class CurseForgeResolver {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private CurseForgeResolver() {
    }

    static boolean resolve(ArtifactIdentity artifact, ManifestMod mod, String apiKey)
            throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        JsonObject body = new JsonObject();
        JsonArray fingerprints = new JsonArray();
        fingerprints.add(artifact.curseForgeFingerprint());
        body.add("fingerprints", fingerprints);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.curseforge.com/v1/fingerprints"))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("User-Agent", "CuscuzSyncServer/0.3.1")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("A API CurseForge respondeu HTTP " + response.statusCode());
        }
        JsonObject data = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("data");
        return data != null && apply(data.getAsJsonArray("exactMatches"), artifact, mod);
    }

    static boolean apply(JsonArray exactMatches, ArtifactIdentity artifact, ManifestMod mod) throws IOException {
        if (exactMatches == null) {
            return false;
        }
        for (JsonElement element : exactMatches) {
            JsonObject match = element.getAsJsonObject();
            JsonObject file = match.getAsJsonObject("file");
            if (file == null || !file.has("fileFingerprint") || file.get("fileFingerprint").isJsonNull()
                    || file.get("fileFingerprint").getAsLong() != artifact.curseForgeFingerprint()) {
                continue;
            }
            if (!file.has("downloadUrl") || file.get("downloadUrl").isJsonNull()
                    || file.get("downloadUrl").getAsString().isBlank()) {
                throw new IOException("O autor bloqueou a distribuição automática deste arquivo no CurseForge.");
            }
            mod.platform = ManifestMod.Platform.CURSEFORGE;
            mod.projectId = Long.toString(file.get("modId").getAsLong());
            mod.versionId = "";
            mod.fileId = file.get("id").getAsLong();
            mod.downloadUrl = file.get("downloadUrl").getAsString();
            mod.fileName = file.get("fileName").getAsString();
            mod.size = file.get("fileLength").getAsLong();
            JsonArray hashes = file.getAsJsonArray("hashes");
            if (hashes != null) {
                for (JsonElement hashElement : hashes) {
                    JsonObject hash = hashElement.getAsJsonObject();
                    if (hash.get("algo").getAsInt() == 1) {
                        mod.sha1 = hash.get("value").getAsString();
                    }
                }
            }
            if (!artifact.sha1().equalsIgnoreCase(mod.sha1)) {
                throw new IOException("O SHA-1 do CurseForge difere do JAR instalado.");
            }
            mod.sha512 = artifact.sha512();
            return true;
        }
        return false;
    }
}
