package br.com.cuscuz.multiversion;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CuscuzShared.MOD_ID)
public final class CuscuzMultiversionMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public CuscuzMultiversionMod() {
        LOGGER.info(CuscuzShared.startupMessage("Forge", "1.18.2"));
    }
}
