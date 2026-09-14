package br.com.cuscuz.sync.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class ClientBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientBootstrap() {
    }

    public static void initialize() {
        LOGGER.info("Interceptador de pré-conexão do Cuscuz Sync inicializado.");
    }
}
