package br.com.cuscuz.sync.server;

import br.com.cuscuz.sync.manifest.ManifestCodec;
import br.com.cuscuz.sync.model.SyncManifest;
import br.com.cuscuz.sync.status.StatusManifestCodec;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusManifestPublisherTest {
    @Test
    void publishesMinimalStatusJson() throws Exception {
        SyncManifest manifest = new SyncManifest();
        manifest.name = "Servidor de teste";
        StatusManifestPublisher.publish(ManifestCodec.encode(manifest));

        String statusJson = StatusManifestPublisher.statusJson();
        JsonObject root = JsonParser.parseString(statusJson).getAsJsonObject();
        assertEquals(1, root.size());
        assertTrue(root.has(StatusManifestCodec.JSON_FIELD));
    }
}
