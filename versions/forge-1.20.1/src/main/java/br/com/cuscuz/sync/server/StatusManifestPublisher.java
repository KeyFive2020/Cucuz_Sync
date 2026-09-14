package br.com.cuscuz.sync.server;

import br.com.cuscuz.sync.manifest.ManifestException;
import br.com.cuscuz.sync.status.StatusManifestCodec;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;

public final class StatusManifestPublisher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_STATUS_CHARACTERS = 32_767;
    private static volatile String statusJson;

    private StatusManifestPublisher() {
    }

    public static synchronized void reload() throws IOException, ManifestException {
        publish(ServerManifestRepository.loadOrCreate());
    }

    static synchronized void publish(byte[] manifest) throws IOException {
        statusJson = null;
        String payload = StatusManifestCodec.encode(manifest);
        JsonObject root = new JsonObject();
        root.addProperty(StatusManifestCodec.JSON_FIELD, payload);
        String encodedStatus = root.toString();
        if (encodedStatus.length() > MAX_STATUS_CHARACTERS) {
            throw new IOException("O status especial excede " + MAX_STATUS_CHARACTERS + " caracteres.");
        }
        statusJson = encodedStatus;
        LOGGER.info("Manifesto Cuscuz Sync publicado pelo Server List Ping ({} bytes, {} caracteres comprimidos).",
                manifest.length, payload.length());
    }

    public static String statusJson() {
        return statusJson;
    }
}
