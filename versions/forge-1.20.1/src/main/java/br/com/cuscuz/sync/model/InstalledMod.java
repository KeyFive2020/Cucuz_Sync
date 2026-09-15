package br.com.cuscuz.sync.model;

import java.nio.file.Path;

public record InstalledMod(
        String modId,
        String displayName,
        String version,
        Path filePath,
        boolean acceptsServerAbsence,
        String sha1,
        String sha512
) {
    public InstalledMod(String modId, String displayName, String version, Path filePath,
                        boolean acceptsServerAbsence) {
        this(modId, displayName, version, filePath, acceptsServerAbsence, "", "");
    }
}
