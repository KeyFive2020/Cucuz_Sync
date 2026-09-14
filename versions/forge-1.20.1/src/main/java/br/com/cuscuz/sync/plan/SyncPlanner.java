package br.com.cuscuz.sync.plan;

import br.com.cuscuz.sync.model.InstalledMod;
import br.com.cuscuz.sync.model.ManifestMod;
import br.com.cuscuz.sync.model.PlanAction;
import br.com.cuscuz.sync.model.SyncManifest;
import br.com.cuscuz.sync.model.SyncPlan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SyncPlanner {
    private static final Set<String> BUILTIN_IDS = Set.of("minecraft", "forge", "java", "cuscuz_sync");

    private SyncPlanner() {
    }

    public static SyncPlan compare(SyncManifest manifest, List<InstalledMod> installedMods) {
        Map<String, InstalledMod> installed = new HashMap<>();
        installedMods.forEach(mod -> installed.putIfAbsent(normalize(mod.modId()), mod));

        Map<String, ManifestMod> expected = new HashMap<>();
        manifest.mods.forEach(mod -> expected.put(normalize(mod.modId), mod));
        Set<String> allowedClient = new HashSet<>();
        manifest.allowedClientMods.forEach(id -> allowedClient.add(normalize(id)));

        List<PlanAction> actions = new ArrayList<>();
        for (ManifestMod target : manifest.mods) {
            if (target.environment == ManifestMod.Environment.SERVER) {
                continue;
            }
            InstalledMod current = installed.get(normalize(target.modId));
            if (current == null && target.required) {
                actions.add(targetAction(PlanAction.Type.INSTALL, target, null));
            } else if (current != null && !target.version.equals(current.version())) {
                actions.add(targetAction(PlanAction.Type.UPDATE, target, current));
            }
        }

        for (InstalledMod local : installedMods) {
            String id = normalize(local.modId());
            if (BUILTIN_IDS.contains(id) || expected.containsKey(id) || allowedClient.contains(id)
                    || local.acceptsServerAbsence()) {
                continue;
            }
            actions.add(new PlanAction(
                    PlanAction.Type.QUARANTINE,
                    local.modId(),
                    local.displayName(),
                    local.version(),
                    "-",
                    null,
                    local,
                    "Mod extra não declarado e não identificado pelo Forge como seguro apenas no cliente."
            ));
        }

        actions.sort((left, right) -> {
            int type = left.type().compareTo(right.type());
            return type != 0 ? type : left.modId().compareTo(right.modId());
        });
        return new SyncPlan(actions);
    }

    private static PlanAction targetAction(PlanAction.Type requested, ManifestMod target, InstalledMod current) {
        boolean downloadable = switch (target.platform) {
            case MODRINTH -> notBlank(target.projectId) && notBlank(target.versionId);
            case CURSEFORGE -> notBlank(target.projectId) && target.fileId > 0
                    && notBlank(target.downloadUrl) && (notBlank(target.sha1) || notBlank(target.sha512));
            case NONE -> false;
        };
        PlanAction.Type type = downloadable ? requested : PlanAction.Type.BLOCKED;
        String reason = downloadable
                ? (requested == PlanAction.Type.INSTALL ? "Mod obrigatório ausente." : "Versão diferente da exigida pelo servidor.")
                : "O administrador ainda não informou uma fonte de download verificável.";
        return new PlanAction(
                type,
                target.modId,
                target.name,
                current == null ? "-" : current.version(),
                target.version,
                target,
                current,
                reason
        );
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
