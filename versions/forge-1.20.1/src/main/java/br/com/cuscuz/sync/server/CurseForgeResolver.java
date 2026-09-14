package br.com.cuscuz.sync.server;

import br.com.cuscuz.sync.model.ManifestMod;
import br.com.cuscuz.sync.model.SyncManifest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Resolve metadados do CurseForge somente no servidor; a chave nunca vai para o cliente. */
final class CurseForgeResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private CurseForgeResolver() {
    }

    static void enrichFromOfficialApi(SyncManifest manifest) {
        String apiKey = System.getenv("CURSEFORGE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        for (ManifestMod mod : manifest.mods) {
            if (mod.platform != ManifestMod.Platform.CURSEFORGE || alreadyResolved(mod)) {
                continue;
            }
            try {
                resolve(mod, apiKey);
            } catch (Exception exception) {
                LOGGER.error("Falha ao resolver {} (projeto {}, arquivo {}) na API CurseForge. "
                        + "O item será publicado como bloqueado.", mod.modId, mod.projectId, mod.fileId, exception);
            }
        }
    }

    private static void resolve(ManifestMod mod, String apiKey) throws Exception {
        long projectId = Long.parseLong(mod.projectId);
        URI uri = URI.create("https://api.curseforge.com/v1/mods/" + projectId + "/files/" + mod.fileId);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("x-api-key", apiKey)
                .header("User-Agent", "CuscuzSyncServer/0.1.0")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonObject data = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("data");
        if (data == null || data.get("id").getAsLong() != mod.fileId || data.get("modId").getAsLong() != projectId) {
            throw new IllegalStateException("A API devolveu um arquivo diferente do solicitado.");
        }
        if (!data.has("downloadUrl") || data.get("downloadUrl").isJsonNull()) {
            throw new IllegalStateException("O autor não permite distribuição pela API CurseForge.");
        }
        mod.downloadUrl = data.get("downloadUrl").getAsString();
        mod.fileName = data.get("fileName").getAsString();
        mod.size = data.get("fileLength").getAsLong();
        if (data.has("hashes")) {
            for (JsonElement element : data.getAsJsonArray("hashes")) {
                JsonObject hash = element.getAsJsonObject();
                if (hash.get("algo").getAsInt() == 1) {
                    mod.sha1 = hash.get("value").getAsString();
                }
            }
        }
        if (mod.sha1 == null || mod.sha1.isBlank()) {
            throw new IllegalStateException("A API não forneceu SHA-1 para o arquivo.");
        }
    }

    private static boolean alreadyResolved(ManifestMod mod) {
        return mod.downloadUrl != null && !mod.downloadUrl.isBlank()
                && ((mod.sha1 != null && !mod.sha1.isBlank()) || (mod.sha512 != null && !mod.sha512.isBlank()));
    }
}
