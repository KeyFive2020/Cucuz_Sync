package br.com.cuscuz.sync.install;

import br.com.cuscuz.sync.model.PlanAction;
import br.com.cuscuz.sync.model.SyncManifest;
import br.com.cuscuz.sync.model.SyncPlan;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SyncInstaller {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private SyncInstaller() {
    }

    public static PreparedPlan prepare(Path gameDirectory, SyncManifest manifest, SyncPlan plan)
            throws IOException, InterruptedException {
        return prepare(gameDirectory, manifest, plan, false);
    }

    public static PreparedPlan prepare(Path gameDirectory, SyncManifest manifest, SyncPlan plan,
                                       boolean exportMissingReport)
            throws IOException, InterruptedException {
        List<PlanAction> actionable = plan.actionableActions();
        if (actionable.isEmpty()) {
            throw new IOException("Nenhuma alteração possui fonte de download verificável.");
        }
        Path gameRoot = gameDirectory.toAbsolutePath().normalize();
        Path syncRoot = gameRoot.resolve(".cuscuz-sync");
        String stamp = STAMP.format(Instant.now());
        Path staging = syncRoot.resolve("staging").resolve(manifest.profileId + '-' + stamp).toAbsolutePath().normalize();
        Path quarantine = syncRoot.resolve("quarantine").resolve(stamp).toAbsolutePath().normalize();
        Path modsDirectory = gameRoot.resolve("mods").toAbsolutePath().normalize();
        Path blockedReport = exportMissingReport && plan.hasBlockedActions()
                ? exportBlockedReport(gameRoot, manifest, plan) : null;
        Files.createDirectories(staging);
        Files.createDirectories(quarantine);

        List<Operation> operations = new ArrayList<>();
        Set<Path> quarantinedSources = new HashSet<>();
        Map<Path, String> installTargets = new HashMap<>();
        Map<String, Path> downloadedArtifacts = new HashMap<>();
        for (PlanAction action : actionable) {
            if (action.type() == PlanAction.Type.QUARANTINE || action.type() == PlanAction.Type.UPDATE) {
                Path source = requireRegularModFile(action.installed().filePath(), modsDirectory);
                if (quarantinedSources.add(source)) {
                    Path target = uniqueTarget(quarantine, source.getFileName().toString(), quarantinedSources.size());
                    operations.add(new Operation("QUARANTINE", source.toString(), target.toString(), action.modId()));
                }
            }
            if (action.type() == PlanAction.Type.INSTALL || action.type() == PlanAction.Type.UPDATE) {
                String artifactKey = artifactKey(action);
                Path downloaded = downloadedArtifacts.get(artifactKey);
                if (downloaded == null) {
                    downloaded = DownloadService.download(action.target(), staging);
                    downloadedArtifacts.put(artifactKey, downloaded);
                }
                Path target = modsDirectory.resolve(downloaded.getFileName()).toAbsolutePath().normalize();
                if (!target.getParent().equals(modsDirectory)) {
                    throw new IOException("Destino de instalação inválido: " + downloaded.getFileName());
                }
                String previousArtifact = installTargets.putIfAbsent(target, artifactKey);
                if (previousArtifact == null) {
                    operations.add(new Operation("INSTALL", downloaded.toString(), target.toString(), action.modId()));
                } else if (!previousArtifact.equals(artifactKey)) {
                    throw new IOException("Dois arquivos diferentes tentaram usar o mesmo nome: " + downloaded.getFileName());
                }
            }
        }

        Files.createDirectories(syncRoot);
        Path script = syncRoot.resolve("apply-cuscuz-sync.ps1");
        copyHelper(script);
        Path pending = syncRoot.resolve("pending-plan.json");
        PendingPlan pendingPlan = new PendingPlan(gameRoot.toString(), stamp, manifest.profileId,
                manifest.manifestVersion, operations);
        Path temporary = syncRoot.resolve("pending-plan.json.tmp");
        Files.writeString(temporary, GSON.toJson(pendingPlan), StandardCharsets.UTF_8);
        Files.move(temporary, pending, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return new PreparedPlan(pending, script, operations.size(),
                (int) plan.count(PlanAction.Type.BLOCKED), blockedReport);
    }

    public static Path exportBlockedReport(Path gameDirectory, SyncManifest manifest, SyncPlan plan)
            throws IOException {
        List<PlanAction> blocked = plan.actions().stream()
                .filter(action -> action.type() == PlanAction.Type.BLOCKED)
                .toList();
        if (blocked.isEmpty()) {
            throw new IOException("Não existem mods sem fonte para exportar.");
        }

        Path gameRoot = gameDirectory.toAbsolutePath().normalize();
        Path reports = gameRoot.resolve(".cuscuz-sync").resolve("reports");
        Files.createDirectories(reports);
        String profile = safeFilePart(manifest.profileId);
        Path target = reports.resolve("mods-sem-fonte-" + profile + ".txt");
        Path temporary = reports.resolve(target.getFileName() + ".tmp");

        String newline = System.lineSeparator();
        StringBuilder text = new StringBuilder()
                .append("Cuscuz Sync - Mods sem fonte").append(newline)
                .append("Servidor: ").append(safeText(manifest.name)).append(newline)
                .append("Perfil: ").append(safeText(manifest.profileId)).append(newline)
                .append("Manifesto: ").append(safeText(manifest.manifestVersion)).append(newline)
                .append("Gerado em: ").append(Instant.now()).append(newline)
                .append("Total: ").append(blocked.size()).append(newline).append(newline);
        for (int index = 0; index < blocked.size(); index++) {
            PlanAction action = blocked.get(index);
            text.append(index + 1).append(". ").append(action.displayName()).append(newline)
                    .append("   Mod ID: ").append(action.modId()).append(newline)
                    .append("   Versão: ").append(action.targetVersion()).append(newline)
                    .append("   Motivo: ").append(action.reason()).append(newline).append(newline);
        }

        Files.writeString(temporary, text, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveUnsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static String safeFilePart(String value) {
        String safe = safeText(value).replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank()) {
            return "servidor";
        }
        return safe.substring(0, Math.min(safe.length(), 48));
    }

    private static String safeText(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ');
    }

    private static Path requireRegularModFile(Path source, Path modsDirectory) throws IOException {
        Path normalized = source.toAbsolutePath().normalize();
        if (!normalized.getParent().equals(modsDirectory) || !Files.isRegularFile(normalized)) {
            throw new IOException("O mod extra não é um JAR regular dentro de mods: " + normalized);
        }
        return normalized;
    }

    private static Path uniqueTarget(Path directory, String fileName, int suffix) {
        Path direct = directory.resolve(fileName);
        return Files.exists(direct) ? directory.resolve(suffix + "-" + fileName) : direct;
    }

    private static String artifactKey(PlanAction action) {
        if (action.target().sha1 != null && !action.target().sha1.isBlank()) {
            return "sha1:" + action.target().sha1.toLowerCase(java.util.Locale.ROOT);
        }
        if (action.target().sha512 != null && !action.target().sha512.isBlank()) {
            return "sha512:" + action.target().sha512.toLowerCase(java.util.Locale.ROOT);
        }
        return action.target().platform + ":" + action.target().projectId + ':'
                + action.target().versionId + ':' + action.target().fileId;
    }

    private static void copyHelper(Path target) throws IOException {
        try (InputStream input = SyncInstaller.class.getResourceAsStream("/assets/cuscuz_sync/apply-cuscuz-sync.ps1")) {
            if (input == null) {
                throw new IOException("Helper de aplicação não foi empacotado.");
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record PreparedPlan(Path pendingPlan, Path helperScript, int operationCount,
                               int blockedCount, Path blockedReport) {
    }

    private record Operation(String kind, String source, String target, String modId) {
    }

    private record PendingPlan(String gameDirectory, String createdAt, String profileId,
                               String manifestVersion, List<Operation> operations) {
    }
}
