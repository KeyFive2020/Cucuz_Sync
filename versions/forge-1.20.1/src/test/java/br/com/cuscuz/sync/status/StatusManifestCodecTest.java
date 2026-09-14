package br.com.cuscuz.sync.status;

import br.com.cuscuz.sync.manifest.ManifestCodec;
import br.com.cuscuz.sync.manifest.ManifestException;
import br.com.cuscuz.sync.model.SyncManifest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatusManifestCodecTest {
    @Test
    void roundTripsCompressedManifest() throws Exception {
        byte[] manifest = validManifest();
        assertArrayEquals(manifest, StatusManifestCodec.decode(StatusManifestCodec.encode(manifest)));
    }

    @Test
    void rejectsChangedPayload() throws Exception {
        String encoded = StatusManifestCodec.encode(validManifest());
        String[] parts = encoded.split("\\.", 3);
        char replacement = parts[1].charAt(0) == '0' ? '1' : '0';
        String changed = parts[0] + '.' + replacement + parts[1].substring(1) + '.' + parts[2];
        assertThrows(IOException.class, () -> StatusManifestCodec.decode(changed));
    }

    @Test
    void rejectsDecompressionBomb() throws Exception {
        byte[] oversized = new byte[ManifestCodec.MAX_MANIFEST_BYTES + 1];
        String encoded = StatusManifestCodec.encode(validManifest());
        assertThrows(IOException.class, () -> StatusManifestCodec.encode(oversized));
        StatusManifestCodec.decode(encoded);
    }

    private static byte[] validManifest() throws ManifestException {
        SyncManifest manifest = new SyncManifest();
        manifest.name = "Teste";
        return ManifestCodec.encode(manifest);
    }
}
