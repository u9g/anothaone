package net.fabricmc.example;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.u9g.configlib.config.ChromaColour;
import dev.u9g.configlib.config.elements.GuiElementTextField;
import dev.u9g.configlib.util.GlScissorStack;
import dev.u9g.configlib.util.render.BackgroundBlur;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screen.ingame.GuiChest;
import net.minecraft.client.util.ScaledResolution;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ContainerChest;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Set;

import static net.fabricmc.example.StorageOverlay.STORAGE_PREVIEW_TEXTURES;

public class StorageOverlayRenderHelper {
    public static void doItemStackDraw(int storageX, int storageY, GuiChest guiChest, Slot slot) {
        Utils.hasEffectOverride = true;
        GlStateManager.translate(storageX - 7, storageY - 18, 0);
        guiChest.drawSlot(slot);
        GlStateManager.translate(-storageX + 7, -storageY + 18, 0);
        Utils.hasEffectOverride = false;
    }

    public static void drawScrollKnob(int maxScroll, LerpingInteger scroll, int scrollGrabOffset) {
        if (scroll.getValue() > maxScroll) {
            scroll.setValue(maxScroll);
        }
        if (scroll.getValue() < 0) {
            scroll.setValue(0);
        }

        //Scroll bar
        int scrollBarY = Math.round(StorageOverlay.getInstance().getScrollBarHeight() * scroll.getValue() / (float) maxScroll);
        float uMin = scrollGrabOffset >= 0 ? 12 / 600f : 0;
        // draw scrollbar knob https://i.imgur.com/KLBGEEO.png
        Utils.drawTexturedRect(
                520,
                8 + scrollBarY,
                12,
                15,
                uMin,
                uMin + 12 / 600f,
                250 / 400f,
                265 / 400f,
                GL11.GL_NEAREST
        );
    }

    public static int heightToRender(LerpingInteger scroll) {
        int h;
        synchronized (StorageManager.getInstance().storageConfig.displayToStorageIdMap_TO_RENDER) {
            int lastDisplayId = StorageManager.getInstance().storageConfig.displayToStorageIdMap_TO_RENDER.size() - 1;
            int coords = (int) Math.ceil(lastDisplayId / 3f) * 3 + 3;

            h = StorageOverlay.getInstance().getPageCoords(coords).y + scroll.getValue();
        }
        return h;
    }

    public static void renderFromFramebuffer(int w, int h, int guiTop, int width, int storageViewSize, ScaledResolution scaledResolution, int startY, Framebuffer framebuffer) {
        GlScissorStack.push(0, guiTop + 3, width, guiTop + 3 + storageViewSize, scaledResolution);
        GlStateManager.enableDepth();
        GlStateManager.translate(0, startY, 107.0001f);
        framebuffer.bindFramebufferTexture();

        GlStateManager.color(1, 1, 1, 1);

        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0F);
        Utils.drawTexturedRect(0, 0, w, h, 0, 1, 1, 0, GL11.GL_NEAREST);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        renderEnchOverlay(StorageOverlay.getInstance().enchantGlintRenderLocations);

        GlStateManager.translate(0, -startY, -107.0001f);
        GlScissorStack.pop(scaledResolution);
    }

    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    private static void renderEnchOverlay(Set<Vector2f> locations) {
        float f = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F;
        float f1 = (float) (Minecraft.getSystemTime() % 4873L) / 4873.0F / 8.0F;
        if (/*PrisonsModConfig.INSTANCE.storageGUI.showEnchantGlint*/true) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(RES_ITEM_GLINT);
        }

        GL11.glPushMatrix();
        for (Vector2f loc : locations) {
            GlStateManager.pushMatrix();
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableBlend();

            GlStateManager.disableLighting();

            GlStateManager.translate(loc.x, loc.y, 0);

            GlStateManager.depthMask(false);
            GlStateManager.depthFunc(GL11.GL_EQUAL);
            GlStateManager.blendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
            GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(8.0F, 8.0F, 8.0F);
            GlStateManager.translate(f, 0.0F, 0.0F);
            GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);

            GlStateManager.color(0x80 / 255f, 0x40 / 255f, 0xCC / 255f, 1);
            Utils.drawTexturedRectNoBlend(0, 0, 16, 16, 0, 1 / 16f, 0, 1 / 16f, GL11.GL_NEAREST);

            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(8.0F, 8.0F, 8.0F);
            GlStateManager.translate(-f1, 0.0F, 0.0F);
            GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);

            GlStateManager.color(0x80 / 255f, 0x40 / 255f, 0xCC / 255f, 1);
            Utils.drawTexturedRectNoBlend(0, 0, 16, 16, 0, 1 / 16f, 0, 1 / 16f, GL11.GL_NEAREST);

            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.blendFunc(770, 771);
            GlStateManager.depthFunc(515);
            GlStateManager.depthMask(true);

            GlStateManager.popMatrix();
        }
        GlStateManager.disableRescaleNormal();
        GL11.glPopMatrix();

        GlStateManager.bindTexture(0);
    }

    public static void renderSelectedStorageOutline(int storageX, int storageY, int borderStartX, int borderStartY, int borderEndX, int borderEndY) {
        int borderColour = ChromaColour.specialToChromaRGB(PrisonsModConfig.INSTANCE.storageGUI.selectedStorageColour);
        Gui.drawRect(storageX + borderStartX + 1, storageY + borderStartY, storageX + borderStartX, storageY + borderEndY, borderColour); //Left
        Gui.drawRect(storageX + borderEndX - 1, storageY + borderStartY, storageX + borderEndX, storageY + borderEndY, borderColour); //Right
        Gui.drawRect(storageX + borderStartX, storageY + borderStartY, storageX + borderEndX, storageY + borderStartY + 1, borderColour); //Top
        Gui.drawRect( storageX + borderStartX, storageY + borderEndY - 1, storageX + borderEndX, storageY + borderEndY, borderColour); //Bottom
    }


    public static void drawMouseHovered(ItemStack stackOnMouse, boolean hoveringOtherBackpack, int mouseX, int mouseY, int hoveredPage, int tooltipWidth, int tooltipHeight, List<String> tooltipToDisplay) {
        ResourceLocation storagePreviewTexture = STORAGE_PREVIEW_TEXTURES[PrisonsModConfig.INSTANCE.storageGUI.displayStyle];
        if (stackOnMouse != null) {
            GlStateManager.enableDepth();
            if (hoveringOtherBackpack) {
                Utils.drawItemStack(new ItemStack(Item.getItemFromBlock(Blocks.BARRIER)), mouseX - 8, mouseY - 8);
            } else {
                Utils.drawItemStack(stackOnMouse, mouseX - 8, mouseY - 8);
            }
        } else if (hoveredPage >= 0) {
            StorageManager.StoragePage page = StorageManager.getInstance().getPage(hoveredPage, false);
            if (page != null && page.rows > 0) {
                int rows = page.rows;

                GlStateManager.translate(0, 0, 100);
                GlStateManager.disableDepth();
                BackgroundBlur.renderBlurredBackground(7, tooltipWidth, tooltipHeight, mouseX + 2, mouseY + 2, 172, 10 + 18 * rows);
                Utils.drawGradientRect(mouseX + 2, mouseY + 2, mouseX + 174, mouseY + 12 + 18 * rows, 0xc0101010, 0xd0101010);

                Minecraft.getMinecraft().getTextureManager().bindTexture(storagePreviewTexture);
                GlStateManager.color(1, 1, 1, 1);
                Utils.drawTexturedRect(mouseX, mouseY, 176, 7, 0, 1, 0, 7 / 32f, GL11.GL_NEAREST);
                for (int i = 0; i < rows; i++) {
                    Utils.drawTexturedRect(mouseX, mouseY + 7 + 18 * i, 176, 18, 0, 1, 7 / 32f, 25 / 32f, GL11.GL_NEAREST);
                }
                Utils.drawTexturedRect(mouseX, mouseY + 7 + 18 * rows, 176, 7, 0, 1, 25 / 32f, 1, GL11.GL_NEAREST);
                GlStateManager.enableDepth();

                for (int i = 0; i < rows * 9; i++) {
                    ItemStack stack = page.items[i];
                    if (stack != null) {
                        GlStateManager.enableDepth();
                        Utils.drawItemStack(stack, mouseX + 8 + 18 * (i % 9), mouseY + 8 + 18 * (i / 9));
                        GlStateManager.disableDepth();
                    }
                }
                GlStateManager.translate(0, 0, -100);
            } else {
                Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, tooltipWidth, tooltipHeight, -1, Minecraft.getMinecraft().fontRendererObj);
            }
        } else if (tooltipToDisplay != null) {
            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, tooltipWidth, tooltipHeight, -1, Minecraft.getMinecraft().fontRendererObj);
        } else {
            StorageOverlay.getInstance().allowTypingInSearchBar = true;
        }
    }

    // these: https://i.imgur.com/lCNu05O.png
    public static void renderInventoryLines(int storageX, int storageY, int storageW, int storageH, int rows) {
        ResourceLocation storageTexture = StorageOverlay.STORAGE_TEXTURES[PrisonsModConfig.INSTANCE.storageGUI.displayStyle];
        Minecraft.getMinecraft().getTextureManager().bindTexture(storageTexture);
        GlStateManager.color(1, 1, 1, 1);
        Utils.drawTexturedRect(storageX, storageY, storageW, storageH, 0, storageW / 600f, 265 / 400f, (265 + storageH) / 400f, GL11.GL_NEAREST);
        if (rows == 6) {
            Utils.drawTexturedRect(storageX, storageY+(18*5), storageW, 18, 0, storageW / 600f, 265 / 400f, (265 + 18) / 400f, GL11.GL_NEAREST);
        }
    }

    public static void renderInnerInventorySearchGraying(int itemX, int itemY, int slotNum, int storageViewSize, ContainerChest containerChest, int inventoryStartIndex, ItemStack[] playerItems, GuiElementTextField searchBar) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(181 - 8, storageViewSize + 18 - (inventoryStartIndex / 9 * 18 + 31), 0);
        Slot slot = containerChest.inventorySlots.get(inventoryStartIndex + 9 + slotNum);
        // TODO: "Fixed" the item rendering by just doing the call myself?
        // 		 will this cause issues later?
        if (slot.getHasStack()) Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(slot.getStack(), slot.x, slot.y);
        // instead of:
        // guiChest.drawSlot(containerChest.inventorySlots.get(inventoryStartIndex + 9 + slotNum));
        GlStateManager.popMatrix();

        if (!searchBar.getText().isEmpty() && (playerItems[slotNum + 9] == null || !ExtraUtils.doesStackMatchSearch(playerItems[slotNum + 9], searchBar.getText()))) {
            GlStateManager.disableDepth();
            Gui.drawRect(itemX, itemY, itemX + 16, itemY + 16, 0x80000000);
            GlStateManager.enableDepth();
        }
    }
}
