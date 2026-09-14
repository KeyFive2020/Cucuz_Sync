package br.com.cuscuz.sync.plan;

import br.com.cuscuz.sync.model.InstalledMod;
import br.com.cuscuz.sync.model.ManifestMod;
import br.com.cuscuz.sync.model.PlanAction;
import br.com.cuscuz.sync.model.SyncManifest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncPlannerTest {
    @Test
    void plansInstallUpdateAndUnknownExtraQuarantine() {
        SyncManifest manifest = new SyncManifest();
        manifest.mods.add(modrinth("missing", "1.0.0"));
        manifest.mods.add(modrinth("outdated", "2.0.0"));

        var plan = SyncPlanner.compare(manifest, List.of(
                installed("outdated", "1.0.0", false),
                installed("unknown_extra", "3.0.0", false),
                installed("client_only", "4.0.0", true)
        ));

        assertEquals(1, plan.count(PlanAction.Type.INSTALL));
        assertEquals(1, plan.count(PlanAction.Type.UPDATE));
        assertEquals(1, plan.count(PlanAction.Type.QUARANTINE));
        assertEquals(0, plan.count(PlanAction.Type.BLOCKED));
        assertTrue(plan.actions().stream().noneMatch(action -> action.modId().equals("client_only")));
    }

    @Test
    void blocksRequiredModWithoutAPlatformSource() {
        SyncManifest manifest = new SyncManifest();
        ManifestMod target = new ManifestMod();
        target.modId = "without_source";
        target.name = "Without Source";
        target.version = "1.0.0";
        manifest.mods.add(target);

        var plan = SyncPlanner.compare(manifest, List.of());

        assertTrue(plan.hasBlockedActions());
        assertEquals(1, plan.count(PlanAction.Type.BLOCKED));
    }

    private static ManifestMod modrinth(String id, String version) {
        ManifestMod mod = new ManifestMod();
        mod.modId = id;
        mod.name = id;
        mod.version = version;
        mod.platform = ManifestMod.Platform.MODRINTH;
        mod.projectId = "project_" + id;
        mod.versionId = "version_" + id;
        return mod;
    }

    private static InstalledMod installed(String id, String version, boolean acceptsAbsence) {
        return new InstalledMod(id, id, version, Path.of("mods", id + ".jar"), acceptsAbsence);
    }
}
