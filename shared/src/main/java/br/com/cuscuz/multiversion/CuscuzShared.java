package br.com.cuscuz.multiversion;

/**
 * Codigo Java puro que pode ser compilado em todas as versoes suportadas.
 * Nao importe classes do Minecraft, Forge ou NeoForge neste diretorio.
 */
public final class CuscuzShared {
    public static final String MOD_ID = "cuscuz_sync";
    public static final String MOD_NAME = "Cuscuz Sync";

    private CuscuzShared() {
    }

    public static String startupMessage(String loader, String minecraftVersion) {
        return MOD_NAME + " iniciado em " + loader + " para Minecraft " + minecraftVersion;
    }
}
