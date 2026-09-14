package br.com.cuscuz.sync.status;

public final class StatusPingProtocol {
    // Forge 1.20.1 removes everything after NUL while decoding a handshake.
    // A printable suffix therefore survives until ServerHandshakePacketListenerImpl.
    public static final String HANDSHAKE_SUFFIX = ".cuscuz-sync-status-v1";

    private StatusPingProtocol() {
    }

    public static String markedHost(String host) {
        return host + HANDSHAKE_SUFFIX;
    }

    public static boolean isManifestRequest(String host) {
        return host != null && host.endsWith(HANDSHAKE_SUFFIX);
    }
}
