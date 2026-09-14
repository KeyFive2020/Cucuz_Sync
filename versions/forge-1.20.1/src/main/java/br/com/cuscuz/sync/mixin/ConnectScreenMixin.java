package br.com.cuscuz.sync.mixin;

import br.com.cuscuz.sync.client.ClientSyncController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Inject(method = {"startConnecting", "m_278792_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private static void cuscuzSync$beforeConnecting(Screen parent, Minecraft minecraft,
                                                     ServerAddress address, ServerData serverData,
                                                     boolean quickPlay, CallbackInfo callback) {
        if (ClientSyncController.consumeConnectionPass()) {
            return;
        }
        callback.cancel();
        ClientSyncController.begin(new ClientSyncController.ConnectionRequest(
                parent, address, serverData, quickPlay
        ));
    }
}
