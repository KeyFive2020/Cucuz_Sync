package br.com.cuscuz.sync.install;

import br.com.cuscuz.sync.manifest.ManifestCodec;
import br.com.cuscuz.sync.model.ManifestMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class DownloadService {
    private static final long MAX_ARTIFACT_BYTES = 512L * 1024L * 1024L;
    private static final Pattern PLATFORM_ID = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final Set<String> MODRINTH_CDN = Set.of("cdn.modrinth.com");
    private static final Set<String> CURSEFORGE_CDN = Set.of("edge.forgecdn.net", "mediafilez.forgecdn.net");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private DownloadService() {
    }

    static Path download(ManifestMod mod, Path stagingDirectory) throws IOException, InterruptedException {
        RemoteArtifact artifact = resolve(mod);
        if (!ManifestCodec.isSafeJarName(artifact.fileName())) {
            throw new IOException("A plataforma devolveu um nome de JAR inseguro para " + mod.modId + '.');
        }
        Files.createDirectories(stagingDirectory);
        Path target = stagingDirectory.resolve(artifact.fileName()).normalize();
        if (!target.getParent().equals(stagingDirectory.toAbsolutePath().normalize())) {
            throw new IOException("Destino de staging inválido.");
        }
        Path temporary = stagingDirectory.resolve(artifact.fileName() + ".part").normalize();

        HttpRequest request = HttpRequest.newBuilder(artifact.uri())
                .timeout(Duration.ofMinutes(3))
                .header("Accept", "application/java-archive, application/octet-stream")
                .header("User-Agent", "CuscuzSync/0.3.2 (Minecraft 1.20.1; Forge)")
                .GET()
                .build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("Falha ao baixar " + mod.modId + ": HTTP " + response.statusCode());
        }
        validateDownloadHost(mod.platform, response.uri());
        long headerSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (headerSize > MAX_ARTIFACT_BYTES || artifact.size() > MAX_ARTIFACT_BYTES) {
            response.body().close();
            throw new IOException("O arquivo de " + mod.modId + " excede 512 MiB.");
        }

        long received = copyLimited(response.body(), temporary);
        if (artifact.size() > 0 && received != artifact.size()) {
            Files.deleteIfExists(temporary);
            throw new IOException("Tamanho inesperado para " + mod.modId + '.');
        }
        verifyHash(temporary, "SHA-512", artifact.sha512());
        verifyHash(temporary, "SHA-1", artifact.sha1());
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return target;
    }

    private static RemoteArtifact resolve(ManifestMod mod) throws IOException, InterruptedException {
        return switch (mod.platform) {
            case MODRINTH -> resolveModrinth(mod);
            case CURSEFORGE -> resolveCurseForge(mod);
            case NONE -> throw new IOException("Não há fonte configurada para " + mod.modId + '.');
        };
    }

    private static RemoteArtifact resolveModrinth(ManifestMod mod) throws IOException, InterruptedException {
        if (!PLATFORM_ID.matcher(mod.versionId).matches() || !PLATFORM_ID.matcher(mod.projectId).matches()) {
            throw new IOException("IDs Modrinth inválidos em " + mod.modId + '.');
        }
        URI uri = URI.create("https://api.modrinth.com/v2/version/" + mod.versionId);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "CuscuzSync/0.3.2 (Minecraft 1.20.1; Forge)")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("A API Modrinth respondeu HTTP " + response.statusCode() + " para " + mod.modId + '.');
        }
        JsonObject version = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!mod.projectId.equals(version.get("project_id").getAsString())) {
            throw new IOException("O versionId Modrinth não pertence ao projectId declarado em " + mod.modId + '.');
        }
        JsonArray files = version.getAsJsonArray("files");
        if (files == null || files.isEmpty()) {
            throw new IOException("A versão Modrinth não contém arquivos.");
        }
        JsonObject selected = selectExactFile(files, mod);
        if (selected == null) {
            throw new IOException("A versão Modrinth não contém o arquivo exato declarado para " + mod.modId + '.');
        }
        String fileName = selected.get("filename").getAsString();
        if (mod.fileName != null && !mod.fileName.isBlank() && !mod.fileName.equals(fileName)) {
            throw new IOException("O nome devolvido pela Modrinth difere do manifesto em " + mod.modId + '.');
        }
        long size = selected.get("size").getAsLong();
        if (mod.size > 0 && mod.size != size) {
            throw new IOException("O tamanho devolvido pela Modrinth difere do manifesto em " + mod.modId + '.');
        }
        JsonObject hashes = selected.getAsJsonObject("hashes");
        String sha1 = hashes.has("sha1") ? hashes.get("sha1").getAsString() : "";
        String sha512 = hashes.has("sha512") ? hashes.get("sha512").getAsString() : "";
        crossCheckHash(mod.sha1, sha1, mod.modId);
        crossCheckHash(mod.sha512, sha512, mod.modId);
        URI downloadUri = URI.create(selected.get("url").getAsString());
        validateHost(downloadUri, MODRINTH_CDN, "Modrinth");
        return new RemoteArtifact(fileName, downloadUri, size, sha1, sha512);
    }

    static JsonObject selectExactFile(JsonArray files, ManifestMod mod) {
        for (JsonElement element : files) {
            JsonObject candidate = element.getAsJsonObject();
            JsonObject candidateHashes = candidate.getAsJsonObject("hashes");
            boolean sha1Matches = candidateHashes != null && candidateHashes.has("sha1")
                    && mod.sha1 != null && !mod.sha1.isBlank()
                    && mod.sha1.equalsIgnoreCase(candidateHashes.get("sha1").getAsString());
            boolean sha512Matches = candidateHashes != null && candidateHashes.has("sha512")
                    && mod.sha512 != null && !mod.sha512.isBlank()
                    && mod.sha512.equalsIgnoreCase(candidateHashes.get("sha512").getAsString());
            if (sha1Matches || sha512Matches) {
                return candidate;
            }
        }
        if (mod.fileName != null && !mod.fileName.isBlank()) {
            for (JsonElement element : files) {
                JsonObject candidate = element.getAsJsonObject();
                if (mod.fileName.equals(candidate.get("filename").getAsString())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static RemoteArtifact resolveCurseForge(ManifestMod mod) throws IOException {
        URI uri;
        try {
            uri = URI.create(mod.downloadUrl);
        } catch (IllegalArgumentException exception) {
            throw new IOException("URL CurseForge inválida em " + mod.modId + '.', exception);
        }
        validateHost(uri, CURSEFORGE_CDN, "CurseForge");
        if ((mod.sha1 == null || mod.sha1.isBlank()) && (mod.sha512 == null || mod.sha512.isBlank())) {
            throw new IOException("CurseForge exige hash publicado pelo servidor em " + mod.modId + '.');
        }
        String fileName = mod.fileName;
        if (!ManifestCodec.isSafeJarName(fileName)) {
            Path urlPath = Path.of(uri.getPath());
            fileName = urlPath.getFileName() == null ? "" : urlPath.getFileName().toString();
        }
        return new RemoteArtifact(fileName, uri, mod.size, safe(mod.sha1), safe(mod.sha512));
    }

    private static long copyLimited(InputStream input, Path temporary) throws IOException {
        long total = 0;
        byte[] buffer = new byte[64 * 1024];
        try (input; var output = Files.newOutputStream(temporary)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > MAX_ARTIFACT_BYTES) {
                    throw new IOException("Download interrompido por exceder 512 MiB.");
                }
                output.write(buffer, 0, read);
            }
        } catch (IOException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }
        return total;
    }

    private static void verifyHash(Path path, String algorithm, String expected) throws IOException {
        if (expected == null || expected.isBlank()) {
            return;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equalsIgnoreCase(expected)) {
                throw new IOException("Hash " + algorithm + " divergente para " + path.getFileName() + '.');
            }
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void crossCheckHash(String declared, String platform, String modId) throws IOException {
        if (declared != null && !declared.isBlank() && !declared.equalsIgnoreCase(platform)) {
            throw new IOException("O hash da plataforma difere do manifesto em " + modId + '.');
        }
    }

    private static void validateDownloadHost(ManifestMod.Platform platform, URI uri) throws IOException {
        switch (platform) {
            case MODRINTH -> validateHost(uri, MODRINTH_CDN, "Modrinth");
            case CURSEFORGE -> validateHost(uri, CURSEFORGE_CDN, "CurseForge");
            case NONE -> throw new IOException("Plataforma ausente.");
        }
    }

    private static void validateHost(URI uri, Set<String> allowed, String platform) throws IOException {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || !allowed.contains(host)) {
            throw new IOException("A URL de " + platform + " não pertence a uma CDN HTTPS permitida.");
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
