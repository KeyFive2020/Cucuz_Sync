package br.com.cuscuz.multiversion;

import br.com.cuscuz.sync.client.ClientBootstrap;
import br.com.cuscuz.sync.install.PendingPlanLauncher;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CuscuzShared.MOD_ID)
public final class CuscuzMultiversionMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public CuscuzMultiversionMod() {
        LOGGER.info(CuscuzShared.startupMessage("Forge", "1.20.1"));
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientBootstrap::initialize);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> PendingPlanLauncher::installShutdownHook);
    }
}
