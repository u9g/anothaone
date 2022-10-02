package dev.u9g.neustoragegui.mixin;

import net.minecraft.client.gui.screen.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class GuiScreen_sendMessageOverride {
    @Shadow public abstract void sendMessage(String text, boolean toHud);

    @Inject(method = "sendMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    public void sendMessage(String input, CallbackInfo ci) {
        ci.cancel();
        if (input.toLowerCase().startsWith("/pv") && input.length() > 3) {
            input = "/pv";
        }
        this.sendMessage(input, true);
    }
}
