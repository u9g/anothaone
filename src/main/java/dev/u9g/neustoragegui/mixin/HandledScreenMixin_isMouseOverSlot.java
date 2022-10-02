package dev.u9g.neustoragegui.mixin;

import dev.u9g.neustoragegui.StorageOverlay;
import net.minecraft.client.gui.screen.ingame.GuiContainer;
import net.minecraft.inventory.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiContainer.class)
public class HandledScreenMixin_isMouseOverSlot {
    @Inject(method = "isMouseOverSlot", at = @At("HEAD"), cancellable = true)
    public void isMouseOverSlot(Slot slot, int pointX, int pointY, CallbackInfoReturnable<Boolean> cir) {
        StorageOverlay.getInstance().overrideIsMouseOverSlot(slot, pointX, pointY, cir);
    }
}
