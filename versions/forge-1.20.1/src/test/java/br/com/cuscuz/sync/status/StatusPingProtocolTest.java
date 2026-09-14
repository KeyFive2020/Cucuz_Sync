package br.com.cuscuz.sync.status;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusPingProtocolTest {
    @Test
    void marksOnlyManifestHandshake() {
        assertFalse(StatusPingProtocol.isManifestRequest("play.example.com"));
        assertTrue(StatusPingProtocol.isManifestRequest(StatusPingProtocol.markedHost("play.example.com")));
    }
}
