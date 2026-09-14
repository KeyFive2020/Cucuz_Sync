package br.com.cuscuz.sync.model;

public record PlanAction(
        Type type,
        String modId,
        String displayName,
        String currentVersion,
        String targetVersion,
        ManifestMod target,
        InstalledMod installed,
        String reason
) {
    public enum Type {
        INSTALL,
        UPDATE,
        QUARANTINE,
        BLOCKED
    }
}
