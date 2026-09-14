package br.com.cuscuz.sync.server;

import br.com.cuscuz.multiversion.CuscuzShared;
import br.com.cuscuz.sync.manifest.ManifestCodec;
import br.com.cuscuz.sync.manifest.ManifestException;
import br.com.cuscuz.sync.model.ManifestMod;
import br.com.cuscuz.sync.model.SyncManifest;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.Set;

public final class ServerManifestRepository {
    private static final Set<String> EXCLUDED = Set.of("minecraft", "forge", "java", CuscuzShared.MOD_ID);
    private static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve("cuscuz-sync");
    private static final Path MANIFEST_PATH = DIRECTORY.resolve("server-manifest.json");

    private ServerManifestRepository() {
    }

    public static Path manifestPath() {
        return MANIFEST_PATH;
    }

    public static byte[] loadOrCreate() throws IOException, ManifestException {
        Files.createDirectories(DIRECTORY);
        if (!Files.exists(MANIFEST_PATH)) {
            writeAtomically(ManifestCodec.encode(createTemplate()));
        }
        byte[] bytes = Files.readAllBytes(MANIFEST_PATH);
        SyncManifest manifest = ManifestCodec.decode(bytes);
        CurseForgeResolver.enrichFromOfficialApi(manifest);
        return ManifestCodec.encode(manifest);
    }

    private static SyncManifest createTemplate() {
        SyncManifest manifest = new SyncManifest();
        manifest.name = "Meu servidor Forge";
        manifest.generatedAt = Instant.now().toString();
        manifest.allowedClientMods.add(CuscuzShared.MOD_ID);

        ModList.get().getMods().stream()
                .filter(info -> !EXCLUDED.contains(info.getModId()))
                .sorted(Comparator.comparing(info -> info.getModId()))
                .forEach(info -> {
                    ManifestMod mod = new ManifestMod();
                    mod.modId = info.getModId();
                    mod.name = info.getDisplayName();
                    mod.version = info.getVersion().toString();
                    mod.environment = ManifestMod.Environment.BOTH;
                    mod.platform = ManifestMod.Platform.NONE;
                    manifest.mods.add(mod);
                });
        return manifest;
    }

    private static void writeAtomically(byte[] bytes) throws IOException {
        Path temporary = DIRECTORY.resolve("server-manifest.json.tmp");
        Files.write(temporary, bytes);
        try {
            Files.move(temporary, MANIFEST_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveUnsupported) {
            Files.move(temporary, MANIFEST_PATH, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
