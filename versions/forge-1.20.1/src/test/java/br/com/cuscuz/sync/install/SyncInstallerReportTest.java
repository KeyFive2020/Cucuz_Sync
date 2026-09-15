package br.com.cuscuz.sync.install;

import br.com.cuscuz.sync.model.PlanAction;
import br.com.cuscuz.sync.model.SyncManifest;
import br.com.cuscuz.sync.model.SyncPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncInstallerReportTest {
    @TempDir
    Path gameDirectory;

    @Test
    void exportsOnlyBlockedModsAsUtf8Text() throws Exception {
        SyncManifest manifest = new SyncManifest();
        manifest.name = "Servidor Cuscuz";
        manifest.profileId = "perfil/teste";
        manifest.manifestVersion = "42";
        SyncPlan plan = new SyncPlan(List.of(
                action(PlanAction.Type.BLOCKED, "sem_fonte", "Mod Sem Fonte", "1.2.3"),
                action(PlanAction.Type.INSTALL, "disponivel", "Mod Disponível", "4.5.6")
        ));

        Path report = SyncInstaller.exportBlockedReport(gameDirectory, manifest, plan);
        String text = Files.readString(report);

        assertTrue(Files.isRegularFile(report));
        assertTrue(report.getFileName().toString().endsWith(".txt"));
        assertTrue(text.contains("Mod Sem Fonte"));
        assertTrue(text.contains("sem_fonte"));
        assertTrue(text.contains("1.2.3"));
        assertFalse(text.contains("Mod Disponível"));
    }

    private static PlanAction action(PlanAction.Type type, String id, String name, String version) {
        return new PlanAction(type, id, name, "-", version, null, null, "Sem fonte verificável.");
    }
}
