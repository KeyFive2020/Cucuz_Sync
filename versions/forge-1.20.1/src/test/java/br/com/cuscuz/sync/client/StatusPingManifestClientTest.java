package br.com.cuscuz.sync.client;

import br.com.cuscuz.sync.manifest.ManifestCodec;
import br.com.cuscuz.sync.model.SyncManifest;
import br.com.cuscuz.sync.status.StatusManifestCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatusPingManifestClientTest {
    @Test
    void readsManifestFromStatusJson() throws Exception {
        SyncManifest source = new SyncManifest();
        source.name = "Servidor de teste";
        String payload = StatusManifestCodec.encode(ManifestCodec.encode(source));
        SyncManifest decoded = StatusPingManifestClient.decodeStatusJson(
                "{\"description\":\"Teste\",\"cuscuzSync\":\"" + payload + "\"}");
        assertEquals("Servidor de teste", decoded.name);
    }

    @Test
    void rejectsStatusWithoutManifest() {
        assertThrows(java.io.IOException.class,
                () -> StatusPingManifestClient.decodeStatusJson("{\"description\":\"Teste\"}"));
    }
}
