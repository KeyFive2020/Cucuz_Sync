package br.com.cuscuz.sync.mixin;

import br.com.cuscuz.sync.server.StatusManifestPublisher;
import br.com.cuscuz.sync.status.StatusPingProtocol;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakePacketListenerImpl.class)
public abstract class ServerHandshakePacketListenerImplMixin {
    @Shadow(aliases = "f_9965_", remap = false)
    @Final
    private MinecraftServer server;

    @Shadow(aliases = "f_9966_", remap = false)
    @Final
    private Connection connection;

    @Inject(method = {"handleIntention", "m_7322_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void cuscuzSync$handleManifestStatus(ClientIntentionPacket packet, CallbackInfo callback) {
        if (packet.getIntention() != ConnectionProtocol.STATUS
                || !StatusPingProtocol.isManifestRequest(packet.getHostName())) {
            return;
        }

        String statusJson = StatusManifestPublisher.statusJson();
        if (statusJson == null || !server.repliesToStatus() || server.getStatus() == null) {
            return;
        }

        connection.setProtocol(ConnectionProtocol.STATUS);
        connection.setListener(new ServerStatusPacketListenerImpl(server.getStatus(), connection, statusJson));
        callback.cancel();
    }
}
