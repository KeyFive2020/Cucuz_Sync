package br.com.cuscuz.sync.server;

import br.com.cuscuz.multiversion.CuscuzShared;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CuscuzShared.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerLifecycleHandler {
    private ServerLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ManifestRefreshService.start(event.getServer().getPort());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ManifestRefreshService.stop();
        ManifestHttpServer.stop();
    }
}
