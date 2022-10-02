package dev.u9g.neustoragegui.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import dev.u9g.neustoragegui.StorageManager;
import dev.u9g.neustoragegui.StorageOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.GuiScreen;
import net.minecraft.client.gui.screen.ingame.GuiChest;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.screen.ContainerChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class GameRendererMixin__GuiScreenEvent_DrawScreenEvent_Pre {
	@WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/GuiScreen;render(IIF)V"), method = "render")
	private boolean shouldRender(GuiScreen s, int mouseX, int mouseY, float tickDelta) {
		String containerName = null;
		GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
		if (guiScreen instanceof GuiChest) {
			GuiChest eventGui = (GuiChest) guiScreen;
			ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
			containerName = cc.getLowerChestInventory().method_6344().getUnformattedText();
		}

		boolean storageOverlayActive = StorageManager.getInstance().shouldRenderStorageOverlay(containerName);
		if (storageOverlayActive) {
			StorageOverlay.getInstance().render();
			return false;
		}
		return true;
	}
}
