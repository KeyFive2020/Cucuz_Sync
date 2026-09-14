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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SyncInstaller {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private SyncInstaller() {
    }

    public static PreparedPlan prepare(Path gameDirectory, SyncManifest manifest, SyncPlan plan)
            throws IOException, InterruptedException {
        if (plan.hasBlockedActions()) {
            throw new IOException("Há mods sem uma fonte de download verificável.");
        }
        Path gameRoot = gameDirectory.toAbsolutePath().normalize();
        Path syncRoot = gameRoot.resolve(".cuscuz-sync");
        String stamp = STAMP.format(Instant.now());
        Path staging = syncRoot.resolve("staging").resolve(manifest.profileId + '-' + stamp).toAbsolutePath().normalize();
        Path quarantine = syncRoot.resolve("quarantine").resolve(stamp).toAbsolutePath().normalize();
        Path modsDirectory = gameRoot.resolve("mods").toAbsolutePath().normalize();
        Files.createDirectories(staging);
        Files.createDirectories(quarantine);

        List<Operation> operations = new ArrayList<>();
        Set<Path> quarantinedSources = new HashSet<>();
        Set<Path> installTargets = new HashSet<>();
        for (PlanAction action : plan.actions()) {
            if (action.type() == PlanAction.Type.QUARANTINE || action.type() == PlanAction.Type.UPDATE) {
                Path source = requireRegularModFile(action.installed().filePath(), modsDirectory);
                if (quarantinedSources.add(source)) {
                    Path target = uniqueTarget(quarantine, source.getFileName().toString(), quarantinedSources.size());
                    operations.add(new Operation("QUARANTINE", source.toString(), target.toString(), action.modId()));
                }
            }
            if (action.type() == PlanAction.Type.INSTALL || action.type() == PlanAction.Type.UPDATE) {
                Path downloaded = DownloadService.download(action.target(), staging);
                Path target = modsDirectory.resolve(downloaded.getFileName()).toAbsolutePath().normalize();
                if (!target.getParent().equals(modsDirectory) || !installTargets.add(target)) {
                    throw new IOException("Dois mods tentaram usar o mesmo nome de arquivo: " + downloaded.getFileName());
                }
                operations.add(new Operation("INSTALL", downloaded.toString(), target.toString(), action.modId()));
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
        return new PreparedPlan(pending, script, operations.size());
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

    private static void copyHelper(Path target) throws IOException {
        try (InputStream input = SyncInstaller.class.getResourceAsStream("/assets/cuscuz_sync/apply-cuscuz-sync.ps1")) {
            if (input == null) {
                throw new IOException("Helper de aplicação não foi empacotado.");
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record PreparedPlan(Path pendingPlan, Path helperScript, int operationCount) {
    }

    private record Operation(String kind, String source, String target, String modId) {
    }

    private record PendingPlan(String gameDirectory, String createdAt, String profileId,
                               String manifestVersion, List<Operation> operations) {
    }
}
