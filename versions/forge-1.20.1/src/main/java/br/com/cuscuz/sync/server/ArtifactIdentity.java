package br.com.cuscuz.sync.server;

import br.com.cuscuz.sync.model.ManifestMod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

record ArtifactIdentity(Path path, String fileName, long size, String sha1, String sha512, long curseForgeFingerprint) {
    private static final Map<Path, CachedIdentity> CACHE = new ConcurrentHashMap<>();

    static ArtifactIdentity inspect(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IOException("O arquivo do mod não é um JAR regular: " + normalized);
        }
        long size = Files.size(normalized);
        long modified = Files.getLastModifiedTime(normalized).toMillis();
        CachedIdentity cached = CACHE.get(normalized);
        if (cached != null && cached.size == size && cached.modified == modified) {
            return cached.identity;
        }
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            byte[] buffer = new byte[64 * 1024];
            try (InputStream input = Files.newInputStream(normalized)) {
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    sha1.update(buffer, 0, read);
                    sha512.update(buffer, 0, read);
                }
            }
            ArtifactIdentity identity = new ArtifactIdentity(normalized, normalized.getFileName().toString(), size,
                    HexFormat.of().formatHex(sha1.digest()), HexFormat.of().formatHex(sha512.digest()),
                    CurseForgeFingerprint.compute(normalized));
            CACHE.put(normalized, new CachedIdentity(size, modified, identity));
            return identity;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    void applyTo(ManifestMod mod) {
        mod.fileName = fileName;
        mod.size = size;
        mod.sha1 = sha1;
        mod.sha512 = sha512;
    }

    private record CachedIdentity(long size, long modified, ArtifactIdentity identity) {
    }
}
