package br.com.cuscuz.multiversion;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CuscuzShared.MOD_ID)
public final class CuscuzMultiversionMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public CuscuzMultiversionMod(IEventBus modEventBus) {
        LOGGER.info(CuscuzShared.startupMessage("NeoForge", "26.2"));
    }
}
