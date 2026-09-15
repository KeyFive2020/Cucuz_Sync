package br.com.cuscuz.sync.client;

import br.com.cuscuz.multiversion.CuscuzShared;
import br.com.cuscuz.sync.install.PendingPlanLauncher;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CuscuzShared.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientLifecycleHandler {
    private ClientLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientBootstrap.initialize();
        PendingPlanLauncher.installShutdownHook();
    }
}
