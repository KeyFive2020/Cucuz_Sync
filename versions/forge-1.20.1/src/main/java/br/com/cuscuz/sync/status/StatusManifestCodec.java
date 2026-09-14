package br.com.cuscuz.sync.status;

import br.com.cuscuz.sync.manifest.ManifestCodec;
import br.com.cuscuz.sync.manifest.ManifestException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class StatusManifestCodec {
    public static final String JSON_FIELD = "cuscuzSync";
    private static final String PREFIX = "CS1";
    private static final int MAX_ENCODED_CHARS = 32_767;

    private StatusManifestCodec() {
    }

    public static String encode(byte[] manifestBytes) throws IOException {
        if (manifestBytes.length == 0 || manifestBytes.length > ManifestCodec.MAX_MANIFEST_BYTES) {
            throw new IOException("O manifesto está vazio ou excede 2 MiB.");
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(manifestBytes);
        }
        String payload = PREFIX + '.' + sha256(manifestBytes) + '.'
                + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed.toByteArray());
        if (payload.length() > MAX_ENCODED_CHARS) {
            throw new IOException("O manifesto comprimido excede o limite do Server List Ping.");
        }
        return payload;
    }

    public static byte[] decode(String payload) throws IOException, ManifestException {
        if (payload == null || payload.isBlank() || payload.length() > MAX_ENCODED_CHARS) {
            throw new IOException("Payload do manifesto ausente ou grande demais.");
        }
        String[] parts = payload.split("\\.", 3);
        if (parts.length != 3 || !PREFIX.equals(parts[0]) || !parts[1].matches("[a-f0-9]{64}")) {
            throw new IOException("Payload do manifesto possui formato inválido.");
        }

        byte[] compressed;
        try {
            compressed = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Payload do manifesto possui Base64 inválido.", exception);
        }

        byte[] manifestBytes;
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                if (read == 0) {
                    continue;
                }
                total += read;
                if (total > ManifestCodec.MAX_MANIFEST_BYTES) {
                    throw new IOException("Manifesto descompactado excede 2 MiB.");
                }
                output.write(buffer, 0, read);
            }
            manifestBytes = output.toByteArray();
        }

        if (!MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.US_ASCII),
                sha256(manifestBytes).getBytes(StandardCharsets.US_ASCII))) {
            throw new IOException("SHA-256 do manifesto recebido não confere.");
        }
        ManifestCodec.decode(manifestBytes);
        return manifestBytes;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
