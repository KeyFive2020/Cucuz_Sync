package br.com.cuscuz.multiversion;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CuscuzShared.MOD_ID)
public final class CuscuzMultiversionMod {
    private static final Logger LOGGER = LogManager.getLogger();

    public CuscuzMultiversionMod() {
        LOGGER.info(CuscuzShared.startupMessage("Forge", "1.16.5"));
    }
}
