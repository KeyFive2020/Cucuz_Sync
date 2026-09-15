package br.com.cuscuz.sync.server;

import br.com.cuscuz.multiversion.CuscuzShared;
import br.com.cuscuz.sync.manifest.ManifestCodec;
import br.com.cuscuz.sync.manifest.ManifestException;
import br.com.cuscuz.sync.model.ManifestMod;
import br.com.cuscuz.sync.model.SyncManifest;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ServerManifestRepository {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> EXCLUDED = Set.of("minecraft", "forge", "java", CuscuzShared.MOD_ID);
    private static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve("cuscuz-sync");
    private static final Path MANIFEST_PATH = DIRECTORY.resolve("server-manifest.json");

    private ServerManifestRepository() {
    }

    public static Path manifestPath() {
        return MANIFEST_PATH;
    }

    /** Reconstrói e substitui a lista inteira usando somente mods ativos no servidor. */
    public static synchronized byte[] refresh() throws IOException, ManifestException, InterruptedException {
        Files.createDirectories(DIRECTORY);
        SyncManifest previous = readPrevious();
        SyncManifest refreshed = newManifest(previous);
        String curseForgeApiKey = ServerSettings.loadOrCreate(DIRECTORY).effectiveCurseForgeApiKey();
        Map<String, ManifestMod> oldById = index(previous);
        Map<Path, ArtifactIdentity> artifacts = new HashMap<>();
        Map<Path, ManifestMod> resolvedByArtifact = new HashMap<>();
        int resolved = 0;
        int unresolved = 0;

        for (IModInfo info : ModList.get().getMods().stream()
                .filter(item -> !EXCLUDED.contains(item.getModId()))
                .sorted(Comparator.comparing(IModInfo::getModId))
                .toList()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Atualização do manifesto cancelada.");
            }
            ManifestMod mod = basicMod(info);
            ManifestMod old = oldById.get(mod.modId);
            if (old != null) {
                mod.required = old.required;
                mod.environment = old.environment;
            }
            try {
                Path path = info.getOwningFile().getFile().getFilePath().toAbsolutePath().normalize();
                ArtifactIdentity artifact = artifacts.computeIfAbsent(path, ignored -> inspectUnchecked(path));
                artifact.applyTo(mod);
                ManifestMod sameJar = resolvedByArtifact.get(path);
                if (sameJar != null) {
                    PlatformResolver.copySource(sameJar, mod);
                } else if (canReuse(old, artifact)) {
                    PlatformResolver.copySource(old, mod);
                    resolvedByArtifact.put(path, mod);
                } else if (PlatformResolver.resolve(artifact, mod, curseForgeApiKey)) {
                    resolvedByArtifact.put(path, mod);
                }
                resolvedByArtifact.putIfAbsent(path, mod);
                if (PlatformResolver.hasDownloadSource(mod)) {
                    resolved++;
                } else if (mod.environment != ManifestMod.Environment.SERVER) {
                    unresolved++;
                }
            } catch (UncheckedInspectionException exception) {
                LOGGER.warn("Não foi possível identificar o arquivo de {}: {}", mod.modId, exception.getCause().getMessage());
            } catch (RuntimeException exception) {
                LOGGER.warn("Não foi possível identificar o arquivo de {}: {}", mod.modId, exception.getMessage());
            }
            refreshed.mods.add(mod);
        }

        byte[] bytes = ManifestCodec.encode(refreshed);
        writeAtomically(bytes);
        LOGGER.info("Manifesto atualizado: {} mods, {} com download, {} sem fonte para cliente.",
                refreshed.mods.size(), resolved, unresolved);
        if (unresolved > 0) {
            if (curseForgeApiKey == null || curseForgeApiKey.isBlank()) {
                LOGGER.warn("{} mods não foram encontrados na Modrinth. Para procurar no CurseForge, defina "
                                + "CURSEFORGE_API_KEY/CF_API_KEY ou preencha curseForgeApiKey em {}. "
                                + "A configuração será relida automaticamente em até 5 minutos.",
                        unresolved, DIRECTORY.resolve("server-settings.json").toAbsolutePath());
            } else {
                LOGGER.warn("{} mods não foram encontrados pelo hash na Modrinth nem no CurseForge.", unresolved);
            }
        }
        return bytes;
    }

    private static ArtifactIdentity inspectUnchecked(Path path) {
        try {
            return ArtifactIdentity.inspect(path);
        } catch (IOException exception) {
            throw new UncheckedInspectionException(exception);
        }
    }

    private static SyncManifest readPrevious() {
        if (!Files.isRegularFile(MANIFEST_PATH)) {
            return null;
        }
        try {
            return ManifestCodec.decode(Files.readAllBytes(MANIFEST_PATH));
        } catch (Exception exception) {
            LOGGER.warn("Manifesto anterior inválido; ele será substituído: {}", exception.getMessage());
            return null;
        }
    }

    static synchronized byte[] loadCached() {
        SyncManifest previous = readPrevious();
        if (previous == null) {
            return null;
        }
        try {
            return ManifestCodec.encode(previous);
        } catch (ManifestException impossibleAfterDecode) {
            return null;
        }
    }

    private static SyncManifest newManifest(SyncManifest previous) {
        SyncManifest manifest = new SyncManifest();
        if (previous != null) {
            manifest.profileId = previous.profileId;
            manifest.name = previous.name;
            manifest.allowedClientMods.addAll(previous.allowedClientMods);
        } else {
            manifest.name = "Meu servidor Forge";
            manifest.allowedClientMods.add(CuscuzShared.MOD_ID);
        }
        manifest.generatedAt = Instant.now().toString();
        manifest.manifestVersion = Long.toString(Instant.now().toEpochMilli());
        return manifest;
    }

    private static ManifestMod basicMod(IModInfo info) {
        ManifestMod mod = new ManifestMod();
        mod.modId = info.getModId().toLowerCase(Locale.ROOT);
        mod.name = info.getDisplayName();
        mod.version = info.getVersion().toString();
        mod.environment = ManifestMod.Environment.BOTH;
        return mod;
    }

    private static Map<String, ManifestMod> index(SyncManifest manifest) {
        Map<String, ManifestMod> indexed = new HashMap<>();
        if (manifest != null) {
            manifest.mods.forEach(mod -> indexed.put(mod.modId.toLowerCase(Locale.ROOT), mod));
        }
        return indexed;
    }

    private static boolean canReuse(ManifestMod old, ArtifactIdentity artifact) {
        return old != null && artifact.sha1().equalsIgnoreCase(old.sha1)
                && PlatformResolver.hasDownloadSource(old);
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

    private static final class UncheckedInspectionException extends RuntimeException {
        private UncheckedInspectionException(IOException cause) {
            super(cause);
        }

        @Override
        public synchronized IOException getCause() {
            return (IOException) super.getCause();
        }
    }
}
