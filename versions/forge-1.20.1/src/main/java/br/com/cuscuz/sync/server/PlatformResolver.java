package br.com.cuscuz.sync.server;

import br.com.cuscuz.sync.model.ManifestMod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

final class PlatformResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PlatformResolver() {
    }

    static boolean resolve(ArtifactIdentity artifact, ManifestMod mod, String curseForgeApiKey)
            throws InterruptedException {
        artifact.applyTo(mod);
        clearSource(mod);
        try {
            if (ModrinthResolver.resolve(artifact, mod)) {
                return true;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (Exception exception) {
            LOGGER.warn("Falha ao procurar {} na Modrinth: {}", mod.modId, exception.getMessage());
        }
        try {
            return CurseForgeResolver.resolve(artifact, mod, curseForgeApiKey);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (Exception exception) {
            LOGGER.warn("Falha ao procurar {} no CurseForge: {}", mod.modId, exception.getMessage());
            return false;
        }
    }

    static void copySource(ManifestMod source, ManifestMod target) {
        target.environment = source.environment;
        target.platform = source.platform;
        target.projectId = source.projectId;
        target.versionId = source.versionId;
        target.fileId = source.fileId;
        target.fileName = source.fileName;
        target.size = source.size;
        target.sha1 = source.sha1;
        target.sha512 = source.sha512;
        target.downloadUrl = source.downloadUrl;
    }

    static boolean hasDownloadSource(ManifestMod mod) {
        return switch (mod.platform) {
            case MODRINTH -> notBlank(mod.projectId) && notBlank(mod.versionId);
            case CURSEFORGE -> notBlank(mod.projectId) && mod.fileId > 0 && notBlank(mod.downloadUrl)
                    && (notBlank(mod.sha1) || notBlank(mod.sha512));
            case NONE -> false;
        };
    }

    private static void clearSource(ManifestMod mod) {
        mod.platform = ManifestMod.Platform.NONE;
        mod.projectId = "";
        mod.versionId = "";
        mod.fileId = 0;
        mod.downloadUrl = "";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
