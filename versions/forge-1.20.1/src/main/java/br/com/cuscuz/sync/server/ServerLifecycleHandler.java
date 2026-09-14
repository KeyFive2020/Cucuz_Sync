package br.com.cuscuz.sync.server;

import br.com.cuscuz.multiversion.CuscuzShared;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = CuscuzShared.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerLifecycleHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ServerLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        byte[] manifest;
        try {
            manifest = ServerManifestRepository.loadOrCreate();
        } catch (Exception exception) {
            LOGGER.error("Não foi possível carregar o manifesto do Cuscuz Sync.", exception);
            return;
        }
        try {
            StatusManifestPublisher.publish(manifest);
        } catch (IOException exception) {
            LOGGER.error("Manifesto grande demais para o Server List Ping; o HTTP continuará disponível.", exception);
        }
        ManifestHttpServer.start(event.getServer().getPort(), manifest);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ManifestHttpServer.stop();
    }
}
