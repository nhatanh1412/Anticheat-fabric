package com.secureguard.mixin;

import com.secureguard.SecureGuard;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Inject(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V", at = @At("HEAD"))
    private void secureguard$countMovementPackets(ServerboundMovePlayerPacket packet, CallbackInfo callbackInfo) {
        if (SecureGuard.ANTICHEAT != null) {
            SecureGuard.ANTICHEAT.onMovementPacket(((ServerGamePacketListenerImpl) (Object) this).getPlayer());
        }
    }
}