package net.fabricmc.example.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import dev.u9g.configlib.M;
import dev.u9g.configlib.config.MyModConfigEditor;
import dev.u9g.configlib.config.ScreenElementWrapper;
import net.fabricmc.example.PrisonsModConfig;
import net.fabricmc.example.StorageManager;
import net.fabricmc.example.StorageOverlay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screen.GuiScreen;
import net.minecraft.client.gui.screen.ingame.GuiChest;
import net.minecraft.client.util.ScaledResolution;
import net.minecraft.screen.ContainerChest;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiScreen.class)
public class ScreenMixin__GuiScreenEvent_MouseOrKeyboardInputEvent_Pre {
    @WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/GuiScreen;handleMouse()V"), method = "Lnet/minecraft/client/gui/screen/GuiScreen;handleInput()V")
    private boolean shouldHandleMouse(GuiScreen s) {
        if (Mouse.getEventButtonState() && StorageManager.getInstance().onAnyClick()) {
            return false;
        }
        String containerName = null;
        Gui guiScreen = M.C.currentScreen;
        if (guiScreen instanceof GuiChest) {
            GuiChest eventGui = (GuiChest) guiScreen;
            ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
            containerName = cc.getLowerChestInventory().method_6344().getUnformattedText();
        }
        boolean storageOverlayActive = StorageManager.getInstance().shouldRenderStorageOverlay(containerName);
        if (storageOverlayActive) {
            final ScaledResolution scaledresolution = new ScaledResolution(M.C);
            final int scaledWidth = scaledresolution.getScaledWidth();
            final int scaledHeight = scaledresolution.getScaledHeight();
            int mouseX = Mouse.getX() * scaledWidth / M.C.displayWidth;
            int mouseY = scaledHeight - Mouse.getY() * scaledHeight / M.C.displayHeight - 1;
            if (StorageOverlay.getInstance().mouseInput(mouseX, mouseY)) {
                return false;
            }
        }
        return true;
    }

    @WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/GuiScreen;handleKeyboard()V"), method = "Lnet/minecraft/client/gui/screen/GuiScreen;handleInput()V")
    private boolean shouldHandleKeyboard(GuiScreen s) {
        if (Mouse.getEventButtonState() && StorageManager.getInstance().onAnyClick()) {
            return false;
        }
        String containerName = null;
        Gui guiScreen = M.C.currentScreen;
        if (guiScreen instanceof GuiChest) {
            GuiChest eventGui = (GuiChest) guiScreen;
            ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
            containerName = cc.getLowerChestInventory().method_6344().getUnformattedText();
        }
        boolean storageOverlayActive = StorageManager.getInstance().shouldRenderStorageOverlay(containerName);
        if (storageOverlayActive) {
            if (StorageOverlay.getInstance().keyboardInput()) {
                return false;
            }
        }
        return true;
    }
}
