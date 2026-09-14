package br.com.cuscuz.sync.inventory;

import br.com.cuscuz.sync.model.InstalledMod;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ModInventory {
    private ModInventory() {
    }

    public static List<InstalledMod> scan() {
        List<InstalledMod> result = new ArrayList<>();
        ModList.get().getMods().forEach(info -> {
            Path path = info.getOwningFile().getFile().getFilePath().toAbsolutePath().normalize();
            boolean acceptsAbsence = ModList.get().getModContainerById(info.getModId())
                    .flatMap(container -> container.getCustomExtension(IExtensionPoint.DisplayTest.class))
                    .map(test -> test.remoteVersionTest().test(NetworkRegistry.ABSENT.version(), true))
                    .orElse(false);
            result.add(new InstalledMod(
                    info.getModId(),
                    info.getDisplayName(),
                    info.getVersion().toString(),
                    path,
                    acceptsAbsence
            ));
        });
        result.sort(Comparator.comparing(InstalledMod::modId));
        return List.copyOf(result);
    }
}
