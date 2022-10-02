package dev.u9g.neustoragegui.mixin;

import dev.u9g.neustoragegui.StorageManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.C0EPacketClickWindow;
import net.minecraft.network.packet.s2c.play.S2DPacketOpenWindow;
import net.minecraft.network.packet.s2c.play.S2EPacketCloseWindow;
import net.minecraft.network.packet.s2c.play.S2FPacketSetSlot;
import net.minecraft.network.packet.s2c.play.S30PacketWindowItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    public void handleSetSlot(S2FPacketSetSlot packet, CallbackInfo ci) {
//        EnchantingSolvers.processInventoryContents(false);
        StorageManager.getInstance().setSlotPacket(packet);
    }

    @Inject(method = "onOpenScreen", at = @At("RETURN"))
    public void handleOpenWindow(S2DPacketOpenWindow packet, CallbackInfo ci) {
        StorageManager.getInstance().openWindowPacket(packet);
    }

    @Inject(method = "onCloseScreen", at = @At("RETURN"))
    public void handleCloseWindow(S2EPacketCloseWindow packet, CallbackInfo ci) {
        StorageManager.getInstance().closeWindowPacket(packet);
    }

    @Inject(method = "onInventory", at = @At("RETURN"))
    public void handleOpenWindow(S30PacketWindowItems packet, CallbackInfo ci) {
        StorageManager.getInstance().setItemsPacket(packet);
    }

    @Inject(method = "addToSendQueue", at = @At("HEAD"))
    public void addToSendQueue(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof C0EPacketClickWindow) {
            StorageManager.getInstance().clientSendWindowClick((C0EPacketClickWindow) packet);
        }
//        if (packet instanceof C01PacketChatMessage) {
//            NewApiKeyHelper.getInstance().hookPacketChatMessage((C01PacketChatMessage) packet);
//        }
    }
}
