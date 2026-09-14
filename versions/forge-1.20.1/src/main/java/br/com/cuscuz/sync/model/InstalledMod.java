package br.com.cuscuz.sync.model;

import java.nio.file.Path;

public record InstalledMod(
        String modId,
        String displayName,
        String version,
        Path filePath,
        boolean acceptsServerAbsence
) {
}
