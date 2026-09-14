package br.com.cuscuz.sync.install;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PendingPlanLauncher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    private PendingPlanLauncher() {
    }

    public static void installShutdownHook() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(PendingPlanLauncher::launchIfPending,
                "Cuscuz Sync shutdown helper"));
    }

    private static void launchIfPending() {
        Path root = Minecraft.getInstance().gameDirectory.toPath().resolve(".cuscuz-sync").toAbsolutePath().normalize();
        Path plan = root.resolve("pending-plan.json");
        Path script = root.resolve("apply-cuscuz-sync.ps1");
        if (!Files.isRegularFile(plan) || !Files.isRegularFile(script)) {
            return;
        }
        try {
            new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-WindowStyle", "Hidden",
                    "-ExecutionPolicy", "Bypass",
                    "-File", script.toString(),
                    "-PlanPath", plan.toString()
            ).start();
        } catch (IOException exception) {
            LOGGER.error("Não foi possível iniciar o aplicador do Cuscuz Sync.", exception);
        }
    }
}
