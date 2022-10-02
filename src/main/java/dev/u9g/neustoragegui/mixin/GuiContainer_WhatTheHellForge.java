package dev.u9g.neustoragegui.mixin;

import net.minecraft.client.gui.screen.ingame.GuiContainer;
import net.minecraft.inventory.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Thank you so much forge for adding patches without a comment
 */
@Mixin(GuiContainer.class)
public abstract class GuiContainer_WhatTheHellForge {
    private Slot lastSlot = null;

    @Inject(method = "getSlotAt(II)Lnet/minecraft/inventory/slot/Slot;", at = @At(value = "RETURN"))
    private void getVariables(int x, int y, CallbackInfoReturnable<Slot> cir) {
        lastSlot = cir.getReturnValue();
    }

    @ModifyVariable(method = "mouseClicked(III)V", at = @At("STORE"), ordinal = 1)
    private boolean isDragging(boolean x) {
        return x && lastSlot == null;
    }
}
