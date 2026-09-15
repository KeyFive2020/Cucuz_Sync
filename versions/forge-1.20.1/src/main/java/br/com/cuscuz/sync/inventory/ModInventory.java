package br.com.cuscuz.sync.inventory;

import br.com.cuscuz.sync.model.InstalledMod;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkRegistry;

import java.nio.file.Path;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class ModInventory {
    private ModInventory() {
    }

    public static List<InstalledMod> scan() {
        List<InstalledMod> result = new ArrayList<>();
        Map<Path, LocalHashes> hashesByArtifact = new HashMap<>();
        ModList.get().getMods().forEach(info -> {
            Path path = info.getOwningFile().getFile().getFilePath().toAbsolutePath().normalize();
            LocalHashes hashes = hashesByArtifact.computeIfAbsent(path, ModInventory::hash);
            boolean acceptsAbsence = ModList.get().getModContainerById(info.getModId())
                    .flatMap(container -> container.getCustomExtension(IExtensionPoint.DisplayTest.class))
                    .map(test -> test.remoteVersionTest().test(NetworkRegistry.ABSENT.version(), true))
                    .orElse(false);
            result.add(new InstalledMod(
                    info.getModId(),
                    info.getDisplayName(),
                    info.getVersion().toString(),
                    path,
                    acceptsAbsence,
                    hashes.sha1(),
                    hashes.sha512()
            ));
        });
        result.sort(Comparator.comparing(InstalledMod::modId));
        return List.copyOf(result);
    }

    private static LocalHashes hash(Path path) {
        if (!Files.isRegularFile(path)) {
            return LocalHashes.EMPTY;
        }
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    sha1.update(buffer, 0, read);
                    sha512.update(buffer, 0, read);
                }
            }
            return new LocalHashes(
                    HexFormat.of().formatHex(sha1.digest()),
                    HexFormat.of().formatHex(sha512.digest())
            );
        } catch (java.io.IOException | NoSuchAlgorithmException ignored) {
            return LocalHashes.EMPTY;
        }
    }

    private record LocalHashes(String sha1, String sha512) {
        private static final LocalHashes EMPTY = new LocalHashes("", "");
    }
}
