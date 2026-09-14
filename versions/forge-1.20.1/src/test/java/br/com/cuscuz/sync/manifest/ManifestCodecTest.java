package br.com.cuscuz.sync.manifest;

import br.com.cuscuz.sync.model.ManifestMod;
import br.com.cuscuz.sync.model.SyncManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManifestCodecTest {
    @Test
    void roundTripsAValidManifest() throws Exception {
        SyncManifest manifest = new SyncManifest();
        ManifestMod mod = new ManifestMod();
        mod.modId = "example_mod";
        mod.name = "Example Mod";
        mod.version = "1.2.3";
        manifest.mods.add(mod);

        SyncManifest decoded = ManifestCodec.decode(ManifestCodec.encode(manifest));

        assertEquals("example_mod", decoded.mods.get(0).modId);
        assertEquals("1.2.3", decoded.mods.get(0).version);
    }

    @Test
    void rejectsDuplicateModIds() {
        SyncManifest manifest = new SyncManifest();
        manifest.mods.add(mod("duplicate"));
        manifest.mods.add(mod("duplicate"));

        assertThrows(ManifestException.class, () -> ManifestCodec.encode(manifest));
    }

    @Test
    void rejectsUnsafeFileNames() {
        SyncManifest manifest = new SyncManifest();
        ManifestMod mod = mod("unsafe_mod");
        mod.fileName = "../unsafe.jar";
        manifest.mods.add(mod);

        assertThrows(ManifestException.class, () -> ManifestCodec.encode(manifest));
    }

    private static ManifestMod mod(String id) {
        ManifestMod mod = new ManifestMod();
        mod.modId = id;
        mod.name = id;
        mod.version = "1.0.0";
        return mod;
    }
}
