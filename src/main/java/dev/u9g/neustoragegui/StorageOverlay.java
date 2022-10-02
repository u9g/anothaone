package dev.u9g.neustoragegui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.OpenGlHelper;
import dev.u9g.configlib.config.ChromaColour;
import dev.u9g.configlib.config.elements.GuiElement;
import dev.u9g.configlib.config.elements.GuiElementTextField;
import dev.u9g.configlib.util.GlScissorStack;
import dev.u9g.configlib.util.render.BackgroundBlur;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.font.FontRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screen.ingame.GuiChest;
import net.minecraft.client.gui.screen.ingame.GuiContainer;
import net.minecraft.client.render.item.RenderItem;
import net.minecraft.client.util.ScaledResolution;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ContainerChest;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.NotImplementedException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StorageOverlay extends GuiElement {
	public static final ResourceLocation[] STORAGE_PREVIEW_TEXTURES = new ResourceLocation[4];
	private static final int CHEST_TOP_OFFSET = 17;
	private static final int CHEST_SLOT_SIZE = 18;
	private static final int CHEST_BOTTOM_OFFSET = 215;
	public static final ResourceLocation[] STORAGE_TEXTURES = new ResourceLocation[4];
	private static final ResourceLocation STORAGE_ICONS_TEXTURE = new ResourceLocation(
		"prisons:storage_gui/storage_icons.png");
	private static final ResourceLocation STORAGE_PANE_CTM_TEXTURE = new ResourceLocation(
		"prisons:storage_gui/storage_gui_pane_ctm.png");
	private static final ResourceLocation[] LOAD_CIRCLE_SEQ = new ResourceLocation[11];
	private static final ResourceLocation[] NOT_RICKROLL_SEQ = new ResourceLocation[19];
	private static final StorageOverlay INSTANCE = new StorageOverlay();
	private static final String CHROMA_STR = "230:255:255:0:0";

	static {
		for (int i = 0; i < STORAGE_TEXTURES.length; i++) {
			STORAGE_TEXTURES[i] = new ResourceLocation("prisons:storage_gui/storage_gui_" + i + ".png");
		}
		for (int i = 0; i < STORAGE_PREVIEW_TEXTURES.length; i++) {
			STORAGE_PREVIEW_TEXTURES[i] = new ResourceLocation("prisons:storage_gui/storage_preview_" + i + ".png");
		}

		for (int i = 0; i < NOT_RICKROLL_SEQ.length; i++) {
			NOT_RICKROLL_SEQ[i] = new ResourceLocation("prisons:storage_gui/we_do_a_little_rolling/" + i + ".jpg");
		}

		LOAD_CIRCLE_SEQ[0] = new ResourceLocation("prisons:loading_circle_seq/1.png");
		LOAD_CIRCLE_SEQ[1] = new ResourceLocation("prisons:loading_circle_seq/1.png");
		LOAD_CIRCLE_SEQ[2] = new ResourceLocation("prisons:loading_circle_seq/2.png");
		for (int i = 2; i <= 7; i++) {
			LOAD_CIRCLE_SEQ[i + 1] = new ResourceLocation("prisons:loading_circle_seq/" + i + ".png");
		}
		LOAD_CIRCLE_SEQ[9] = new ResourceLocation("prisons:loading_circle_seq/7.png");
		LOAD_CIRCLE_SEQ[10] = new ResourceLocation("prisons:loading_circle_seq/1.png");
	}

	public final Set<Vector2f> enchantGlintRenderLocations = new HashSet<>();
	private final GuiElementTextField searchBar = new GuiElementTextField("", 88, 10,
		GuiElementTextField.SCALE_TEXT | GuiElementTextField.DISABLE_BG
	);
	private final GuiElementTextField renameStorageField = new GuiElementTextField("", 100, 13,
		GuiElementTextField.COLOUR
	);
	private final int[][] isPaneCaches = new int[40][];
	private final int[][] ctmIndexCaches = new int[40][];
	private final LerpingInteger scroll = new LerpingInteger(0, 200);
	private Framebuffer framebuffer = null;
	private int editingNameId = -1;
	private int guiLeft;
	private int guiTop;
	private boolean fastRender = false;
	private int loadCircleIndex = 0;
	private int rollIndex = 0;
	private int loadCircleRotation = 0;
	private long millisAccumIndex = 0;
	private long millisAccumRoll = 0;
	private long millisAccumRotation = 0;
	private long lastMillis = 0;
	private int scrollVelocity = 0;
	private long lastScroll = 0;
	private int desiredHeightSwitch = -1;
	private int desiredHeightMX = -1;
	private int desiredHeightMY = -1;
	private boolean dirty = false;
	public boolean allowTypingInSearchBar = true;
	private int scrollGrabOffset = -1;

	public static StorageOverlay getInstance() {
		return INSTANCE;
	}

	private static boolean shouldConnect(int paneIndex1, int paneIndex2) {
		if (paneIndex1 == 16 || paneIndex2 == 16) return false;
		if (paneIndex1 < 1 || paneIndex2 < 1) return false;
		return paneIndex1 == paneIndex2;

	}

	public static int getCTMIndex(StorageManager.StoragePage page, int index, int[] isPaneCache, int[] ctmIndexCache) {
		if (page.items[index] == null) {
			ctmIndexCache[index] = -1;
			return -1;
		}

		int paneType = getPaneType(page.items[index], index, isPaneCache);

		int upIndex = index - 9;
		int leftIndex = index % 9 > 0 ? index - 1 : -1;
		int rightIndex = index % 9 < 8 ? index + 1 : -1;
		int downIndex = index + 9;
		int upleftIndex = index % 9 > 0 ? index - 10 : -1;
		int uprightIndex = index % 9 < 8 ? index - 8 : -1;
		int downleftIndex = index % 9 > 0 ? index + 8 : -1;
		int downrightIndex = index % 9 < 8 ? index + 10 : -1;

		boolean up = upIndex >= 0 && upIndex < isPaneCache.length && shouldConnect(getPaneType(
			page.items[upIndex],
			upIndex,
			isPaneCache
		), paneType);
		boolean left = leftIndex >= 0 && leftIndex < isPaneCache.length && shouldConnect(getPaneType(
			page.items[leftIndex],
			leftIndex,
			isPaneCache
		), paneType);
		boolean down = downIndex >= 0 && downIndex < isPaneCache.length && shouldConnect(getPaneType(
			page.items[downIndex],
			downIndex,
			isPaneCache
		), paneType);
		boolean right = rightIndex >= 0 && rightIndex < isPaneCache.length && shouldConnect(getPaneType(
			page.items[rightIndex],
			rightIndex,
			isPaneCache
		), paneType);
		boolean upleft = upleftIndex >= 0 && upleftIndex < isPaneCache.length && shouldConnect(getPaneType(
			page.items[upleftIndex],
			upleftIndex,
			isPaneCache
		), paneType);
		boolean upright = uprightIndex >= 0 && uprightIndex < isPaneCache.length && shouldConnect(getPaneType(
			page.items[uprightIndex],
			uprightIndex,
			isPaneCache
		), paneType);
		boolean downleft = downleftIndex >= 0 && downleftIndex < isPaneCache.length && shouldConnect(getPaneType(
			page.items[downleftIndex],
			downleftIndex,
			isPaneCache
		), paneType);
		boolean downright = downrightIndex >= 0 && downrightIndex < isPaneCache.length &&
			shouldConnect(getPaneType(page.items[downrightIndex], downrightIndex, isPaneCache), paneType);

		int ctmIndex = BetterContainers.getCTMIndex(up, right, down, left, upleft, upright, downright, downleft);
		ctmIndexCache[index] = ctmIndex;
		return ctmIndex;
	}

	public static int getRGBFromPane(int paneType) {
		int rgb = -1;
		EnumChatFormatting formatting = EnumChatFormatting.WHITE;
		switch (paneType) {
			case 0:
				formatting = EnumChatFormatting.WHITE;
				break;
			case 1:
				formatting = EnumChatFormatting.GOLD;
				break;
			case 2:
				formatting = EnumChatFormatting.LIGHT_PURPLE;
				break;
			case 3:
				formatting = EnumChatFormatting.BLUE;
				break;
			case 4:
				formatting = EnumChatFormatting.YELLOW;
				break;
			case 5:
				formatting = EnumChatFormatting.GREEN;
				break;
			case 6:
				rgb = 0xfff03c96;
				break;
			case 7:
				formatting = EnumChatFormatting.DARK_GRAY;
				break;
			case 8:
				formatting = EnumChatFormatting.GRAY;
				break;
			case 9:
				formatting = EnumChatFormatting.DARK_AQUA;
				break;
			case 10:
				formatting = EnumChatFormatting.DARK_PURPLE;
				break;
			case 11:
				formatting = EnumChatFormatting.DARK_BLUE;
				break;
			case 12:
				rgb = 0xffA0522D;
				break;
			case 13:
				formatting = EnumChatFormatting.DARK_GREEN;
				break;
			case 14:
				formatting = EnumChatFormatting.DARK_RED;
				break;
			case 15:
				rgb = 0x00000000;
				break;
			case 16:
				rgb = SpecialColour.specialToChromaRGB(CHROMA_STR);
				break;
		}
		if (rgb != -1) return rgb;
		return 0xff000000 | Minecraft.getMinecraft().fontRendererObj.getColorCode(formatting.toString().charAt(1));
	}

	public static int getPaneType(ItemStack stack, int index, int[] cache) {
		if (cache != null && cache[index] != 0) return cache[index];

		if (PrisonsModConfig.INSTANCE.storageGUI.fancyPanes == 2) {
			if (cache != null) cache[index] = -1;
			return -1;
		}

		if (stack != null &&
			(stack.getItem() == Item.getItemFromBlock(Blocks.STAINED_GLASS_PANE) || stack.getItem() == Item.getItemFromBlock(Blocks.GLASS_PANE))) {
			// TODO: fix
			String internalName = null;//NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack);
			if (internalName != null) {
				if (internalName.startsWith("STAINED_GLASS_PANE")) {
					if (cache != null) cache[index] = stack.getItemDamage() + 1;
					return stack.getItemDamage() + 1;
				} else if (internalName.startsWith("THIN_GLASS")) {
					if (cache != null) cache[index] = 17;
					return 17;
				}
			}
		}
		if (cache != null) cache[index] = -1;
		return -1;
	}

	private int getMaximumScroll() {
		synchronized (StorageManager.getInstance().storageConfig.displayToStorageIdMap_TO_RENDER) {

			int maxH = 0;

			for (int i = 0; i < 3; i++) {
				int lastDisplayId = StorageManager.getInstance().storageConfig.displayToStorageIdMap_TO_RENDER.size() - 1;
				int coords = (int) Math.ceil(lastDisplayId / 3f) * 3 + 1 + i;

				int h = getPageCoords(coords).y + scroll.getValue() - getStorageViewSize() - 14;

				if (h > maxH) maxH = h;
			}

			return maxH;
		}
	}

	public void markDirty() {
		dirty = true;
	}

	private void scrollToY(int y) {
		int target = y;
		if (target < 0) target = 0;

		int maxY = getMaximumScroll();
		if (target > maxY) target = maxY;

		float factor = (scroll.getValue() - target) / (float) (scroll.getValue() - y + 1E-5);

		scroll.setTarget(target);
		scroll.setTimeToReachTarget(Math.min(200, Math.max(20, (int) (200 * factor))));
		scroll.resetTimer();
	}

	public void scrollToStorage(int displayId, boolean forceScroll) {
		if (displayId < 0) return;

		int y = getPageCoords(displayId).y - 17;
		if (y < 3) {
			scrollToY(y + scroll.getValue());
		} else {
			int storageViewSize = getStorageViewSize();
			int y2 = getPageCoords(displayId + 3).y - 17 - storageViewSize;
			if (y2 > 3) {
				if (forceScroll) {
					scrollToY(y + scroll.getValue());
				} else {
					scrollToY(y2 + scroll.getValue());
				}
			}
		}
	}

	private int getStorageViewSize() {
		return PrisonsModConfig.INSTANCE.storageGUI.storageHeight;
	}

	public int getScrollBarHeight() {
		return getStorageViewSize() - 21;
	}

	private void createOrClearFrameBuffer(int fw, int fh) {
		if (framebuffer == null) {
			framebuffer = new Framebuffer(fw, fh, true);
		} else if (framebuffer.framebufferWidth != fw || framebuffer.framebufferHeight != fh) {
			framebuffer.createBindFramebuffer(fw, fh);
		}
		framebuffer.framebufferClear();
	}

	@Override
	public void render() {
		if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) return;

		GuiChest guiChest = (GuiChest) Minecraft.getMinecraft().currentScreen;
		ContainerChest containerChest = (ContainerChest) guiChest.inventorySlots;

		ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		int width = scaledResolution.getScaledWidth();
		int height = scaledResolution.getScaledHeight();
		int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
		int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;
		FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRendererObj;

		scroll.tick();

		int displayStyle = PrisonsModConfig.INSTANCE.storageGUI.displayStyle;
		ResourceLocation storageTexture = STORAGE_TEXTURES[displayStyle];
		int textColour = 0x404040;
		int searchTextColour = 0xe0e0e0;
		if (displayStyle == 2) {
			textColour = 0x000000;
			searchTextColour = 0xa0a0a0;
		} else if (displayStyle == 3) {
			textColour = 0xFBCC6C;
		} else if (displayStyle == 0) {
			textColour = 0x909090;
			searchTextColour = 0xa0a0a0;
		}

		long currentTime = System.currentTimeMillis();
		if (lastMillis > 0) {
			long deltaTime = currentTime - lastMillis;
			millisAccumIndex += deltaTime;
			loadCircleIndex += millisAccumIndex / (1000 / 15);
			millisAccumIndex %= (1000 / 15);

			millisAccumRotation += deltaTime;
			loadCircleRotation += millisAccumRotation / (1000 / 107);
			millisAccumRotation %= (1000 / 107);

			millisAccumRoll += deltaTime;
			rollIndex += millisAccumRoll / 100;
			millisAccumRoll %= 100;
		}

		lastMillis = currentTime;
		loadCircleIndex %= LOAD_CIRCLE_SEQ.length;
		rollIndex %= NOT_RICKROLL_SEQ.length * 2;
		loadCircleRotation %= 360;

		Color loadCircleColour = Color.getHSBColor(loadCircleRotation / 360f, 0.3f, 0.9f);
		ItemStack stackOnMouse = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
		if (stackOnMouse != null) {
			String stackDisplay = Utils.cleanColour(stackOnMouse.getDisplayName());
			if (stackDisplay.startsWith("Backpack Slot ") || stackDisplay.startsWith("Empty Backpack Slot ") ||
				stackDisplay.startsWith("Ender Chest Page ")) {
				stackOnMouse = null;
			}
		}

		List<String> tooltipToDisplay = null;
		int slotPreview = -1;

		int storageViewSize = getStorageViewSize();

		int sizeX = 540;
		int sizeY = 100 + storageViewSize;
		int searchNobX = 18;

		int itemHoverX = -1;
		int itemHoverY = -1;

		guiLeft = width / 2 - (sizeX - searchNobX) / 2;
		guiTop = height / 2 - sizeY / 2;

		if (displayStyle == 0) {
			BackgroundBlur.renderBlurredBackground(7, width, height, guiLeft, guiTop, sizeX, storageViewSize);
			BackgroundBlur.renderBlurredBackground(
				7,
				width,
				height,
				guiLeft + 5,
				guiTop + storageViewSize,
				sizeX - searchNobX - 10,
				sizeY - storageViewSize - 4
			);
		}

		Utils.drawGradientRect(0, 0, width, height, 0xc0101010, 0xd0101010);

		GL11.glPushMatrix();
		GlStateManager.translate(guiLeft, guiTop, 0);

		boolean hoveringOtherBackpack = false;

		//Gui
		Minecraft.getMinecraft().getTextureManager().bindTexture(storageTexture);
		GlStateManager.color(1, 1, 1, 1);
		// upper lip of storage gui https://i.imgur.com/yQLf3CV.png
		Utils.drawTexturedRect(0, 0, sizeX, 10, 0, sizeX / 600f, 0, 10 / 400f, GL11.GL_NEAREST);
		// middle of storage gui: https://i.imgur.com/Lw57V3D.png
		Utils.drawTexturedRect(0, 10, sizeX, storageViewSize - 20, 0, sizeX / 600f, 10 / 400f, 94 / 400f, GL11.GL_NEAREST);
		// bottom of storage gui https://i.imgur.com/YnUAMDj.png => https://i.imgur.com/nLX57sp.png
		Utils.drawTexturedRect(
			0,
			storageViewSize - 10,
			sizeX,
			110,
			0,
			sizeX / 600f,
			94 / 400f,
			204 / 400f,
			GL11.GL_NEAREST
		);

		StorageOverlayRenderHelper.drawScrollKnob(getMaximumScroll(), scroll, scrollGrabOffset);

		int currentPage = StorageManager.getInstance().getCurrentPageId();

		boolean mouseInsideStorages = mouseY > guiTop + 3 && mouseY < guiTop + 3 + storageViewSize;

		//Storages
		boolean doItemRender = true;
		boolean doRenderFramebuffer = false;
		int startY = getPageCoords(0).y;
		if (OpenGlHelper.isFramebufferEnabled()) {
			int h = StorageOverlayRenderHelper.heightToRender(scroll);
			int w = sizeX;
			markDirty(); // TODO: clearing framebuffer, remove later

			//Render from framebuffer
			if (framebuffer != null) {
				StorageOverlayRenderHelper.renderFromFramebuffer(w, h, guiTop, width, storageViewSize, scaledResolution, startY, framebuffer);
			}

			if (dirty || framebuffer == null) {
				dirty = false;
				createOrClearFrameBuffer(w * scaledResolution.getScaleFactor(), h * scaledResolution.getScaleFactor());
				// Render to framebuffer
				framebuffer.bindFramebuffer(true);
				GlStateManager.matrixMode(GL11.GL_PROJECTION);
				GlStateManager.loadIdentity();
				GlStateManager.ortho(0.0D, w, h, 0.0D, 1000.0D, 3000.0D);
				GlStateManager.matrixMode(GL11.GL_MODELVIEW);
				GlStateManager.pushMatrix();
				GlStateManager.translate(-guiLeft, -guiTop - startY, 0);
				doRenderFramebuffer = true;
			} else {
				doItemRender = false;
			}
		}

		if (doItemRender) {
			enchantGlintRenderLocations.clear();
			for (Map.Entry<Integer, Integer> entry : StorageManager.getInstance().storageConfig.displayToStorageIdMap_TO_RENDER.entrySet()) {
				int displayId = entry.getKey();
				int storageId = entry.getValue();

				IntPair coords = getPageCoords(displayId);
				int storageX = coords.x;
				int storageY = coords.y;

				if (!doRenderFramebuffer) {
					if (coords.y - 11 > 3 + storageViewSize || coords.y + 90 < 3) continue;
				}

				StorageManager.StoragePage page = StorageManager.getInstance().getPage(storageId, false);
				if (page != null && page.rows > 0) {
					int rows = page.rows;

					isPaneCaches[storageId] = new int[page.rows * 9];
					ctmIndexCaches[storageId] = new int[page.rows * 9];
					int[] isPaneCache = isPaneCaches[storageId];
					int[] ctmIndexCache = ctmIndexCaches[storageId];

					for (int k = 0; k < rows * 9; k++) {
						ItemStack stack;

						if (storageId == currentPage) {
							stack = containerChest.getSlot(k).getStack();
						} else {
							stack = page.items[k];
						}

						int itemX = storageX + 1 + 18 * (k % 9);
						int itemY = storageY + 1 + 18 * (k / 9);

						//Render fancy glass
						if (stack != null) {
							int paneType = getPaneType(stack, k, isPaneCache);
							if (paneType > 0) {
								GlStateManager.disableAlpha();
								Gui.drawRect(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0x01000000);
								GlStateManager.enableAlpha();

								int ctmIndex = getCTMIndex(page, k, isPaneCache, ctmIndexCache);
								int startCTMX = (ctmIndex % 12) * 19;
								int startCTMY = (ctmIndex / 12) * 19;

								ctmIndexCache[k] = ctmIndex;

								if (paneType != 17) {
									int rgb = getRGBFromPane(paneType - 1);
									{
										int a = (rgb >> 24) & 0xFF;
										int r = (rgb >> 16) & 0xFF;
										int g = (rgb >> 8) & 0xFF;
										int b = rgb & 0xFF;
										Minecraft.getMinecraft().getTextureManager().bindTexture(STORAGE_PANE_CTM_TEXTURE);
										GlStateManager.color(r / 255f, g / 255f, b / 255f, a / 255f);
										Utils.drawTexturedRect(
											itemX - 1,
											itemY - 1,
											18,
											18,
											startCTMX / 227f,
											(startCTMX + 18) / 227f,
											startCTMY / 75f,
											(startCTMY + 18) / 75f,
											GL11.GL_NEAREST
										);
									}

									RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();
									itemRender.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRendererObj, stack, itemX, itemY, null);
									GlStateManager.disableLighting();
								}

								page.shouldDarkenIfNotSelected[k] = false;
								continue;
							}
						}
						page.shouldDarkenIfNotSelected[k] = true;

						//Render item
						GlStateManager.translate(0, 0, 20);
						if (doRenderFramebuffer) {
							GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
							GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE, GL11.GL_ZERO);

							if (storageId == currentPage) {
								StorageOverlayRenderHelper.doItemStackDraw(storageX, storageY, guiChest, containerChest.getSlot(k));
							} else {
								Utils.drawItemStackWithoutGlint(stack, itemX, itemY);
							}

							GL14.glBlendFuncSeparate(770, 771, 1, 0);

							if (stack != null && (stack.hasEffect() || stack.getItem() == Items.ENCHANTED_BOOK)) {
								enchantGlintRenderLocations.add(new Vector2f(itemX, itemY - startY));
							}
						} else if (storageId == currentPage) {
							StorageOverlayRenderHelper.doItemStackDraw(storageX, storageY, guiChest, containerChest.getSlot(k));
						} else {
							Utils.drawItemStack(stack, itemX, itemY);
						}
						GlStateManager.disableLighting();
						GlStateManager.translate(0, 0, -20);
					}

					GlStateManager.disableLighting();
					GlStateManager.enableDepth();
				}
			}
		}

		if (OpenGlHelper.isFramebufferEnabled() && doRenderFramebuffer) {
			GlStateManager.popMatrix();
			Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);

			GlStateManager.matrixMode(GL11.GL_PROJECTION);
			GlStateManager.loadIdentity();
			GlStateManager.ortho(0.0D, scaledResolution.getScaledWidth_double(), scaledResolution.getScaledHeight_double(),
				0.0D, 1000.0D, 3000.0D
			);
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		}

		GlScissorStack.push(0, guiTop + 3, width, guiTop + 3 + storageViewSize, scaledResolution);
		for (Map.Entry<Integer, Integer> entry : StorageManager.getInstance().storageConfig.displayToStorageIdMap_TO_RENDER.entrySet()) {
			int displayId = entry.getKey();
			int storageId = entry.getValue();

			IntPair coords = getPageCoords(displayId);
			int storageX = coords.x;
			int storageY = coords.y;

			if (coords.y - 11 > 3 + storageViewSize || coords.y + 90 < 3) continue;

			StorageManager.StoragePage page = StorageManager.getInstance().getPage(storageId, false);

			// Draw names of pages
			if (editingNameId == storageId) {
				int len = fontRendererObj.getStringWidth(renameStorageField.getTextDisplay()) + 10;
				renameStorageField.setSize(len, 12);
				renameStorageField.render(storageX, storageY - 13);
			} else {
				String pageTitle;
				if (page != null && page.customTitle != null && !page.customTitle.isEmpty()) {
					pageTitle = Utils.chromaStringByColourCode(page.customTitle);
				} else if (entry.getValue() < 9) {
					pageTitle = "Ender Chest Page " + (entry.getValue() + 1);
				} else {
					pageTitle = "Backpack Slot " + (storageId - 8);
				}
				int titleLen = fontRendererObj.getStringWidth(pageTitle);

				if (mouseX >= guiLeft + storageX && mouseX <= guiLeft + storageX + titleLen + 15 &&
					mouseY >= guiTop + storageY - 14 && mouseY <= guiTop + storageY + 1) {
					pageTitle += " \u270E";
				}
				fontRendererObj.drawString(pageTitle, storageX, storageY - 11, textColour);
			}

			if (page == null) {
				Minecraft.getMinecraft().getTextureManager().bindTexture(storageTexture);
				GlStateManager.color(1, 1, 1, 1);
				int h = 18 * 3;

				Utils.drawTexturedRect(
					storageX,
					storageY,
					162,
					h,
					0,
					162 / 600f,
					265 / 400f,
					(265 + h) / 400f,
					GL11.GL_NEAREST
				);

				Gui.drawRect(storageX, storageY, storageX + 162, storageY + h, 0x80000000);

				if (storageId < 9) {
					Utils.drawStringCenteredScaledMaxWidth("Locked Page", fontRendererObj,
						storageX + 81, storageY + h / 2, true, 150, 0xd94c00
					);
				} else {
					Utils.drawStringCenteredScaledMaxWidth("Empty Backpack Slot", fontRendererObj,
						storageX + 81, storageY + h / 2, true, 150, 0xd94c00
					);
				}
			} else if (page.rows <= 0) {
				Minecraft.getMinecraft().getTextureManager().bindTexture(storageTexture);
				GlStateManager.color(1, 1, 1, 1);
				int h = 18 * 3;

				Utils.drawTexturedRect(
					storageX,
					storageY,
					162,
					h,
					0,
					162 / 600f,
					265 / 400f,
					(265 + h) / 400f,
					GL11.GL_NEAREST
				);

				Gui.drawRect(storageX, storageY, storageX + 162, storageY + h, 0x80000000);

				Utils.drawStringCenteredScaledMaxWidth("Click to load items", fontRendererObj,
					storageX + 81, storageY + h / 2, true, 150, 0xffdf00
				);
			} else {
				int rows = page.rows;

				int storageW = 162;
				int storageH = 18 * rows;

				GlStateManager.enableDepth();

				boolean[] shouldLimitBorder = new boolean[rows * 9];
				boolean hasCaches = isPaneCaches[storageId] != null && isPaneCaches[storageId].length == rows * 9 &&
					ctmIndexCaches[storageId] != null && ctmIndexCaches[storageId].length == rows * 9;

				//Render item connections
				for (int k = 0; k < rows * 9; k++) {
					ItemStack stack = page.items[k];

					if (stack != null && hasCaches) {
						int itemX = storageX + 1 + 18 * (k % 9);
						int itemY = storageY + 1 + 18 * (k / 9);

						int[] isPaneCache = isPaneCaches[storageId];
						int[] ctmIndexCache = ctmIndexCaches[storageId];

						if (isPaneCache[k] == 17) {
							int ctmIndex = getCTMIndex(page, k, isPaneCache, ctmIndexCache);
							int startCTMX = (ctmIndex % 12) * 19;
							int startCTMY = (ctmIndex / 12) * 19;

							int rgb = getRGBFromPane(isPaneCache[k] - 1);
							int a = (rgb >> 24) & 0xFF;
							int r = (rgb >> 16) & 0xFF;
							int g = (rgb >> 8) & 0xFF;
							int b = rgb & 0xFF;
							Minecraft.getMinecraft().getTextureManager().bindTexture(STORAGE_PANE_CTM_TEXTURE);
							GlStateManager.color(r / 255f, g / 255f, b / 255f, a / 255f);
							GlStateManager.translate(0, 0, 110);
							Utils.drawTexturedRect(itemX - 1, itemY - 1, 18, 18,
								startCTMX / 227f, (startCTMX + 18) / 227f, startCTMY / 75f, (startCTMY + 18) / 75f, GL11.GL_NEAREST
							);
							GlStateManager.translate(0, 0, -110);

							RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();
							itemRender.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRendererObj, stack, itemX, itemY, null);
							GlStateManager.enableDepth();
						} else if (isPaneCache[k] < 0) {
							boolean hasConnection = false;

							int upIndex = k - 9;
							int leftIndex = k % 9 > 0 ? k - 1 : -1;
							int rightIndex = k % 9 < 8 ? k + 1 : -1;
							int downIndex = k + 9;

							int[] indexArr = {rightIndex, downIndex, leftIndex, upIndex};

							for (int j = 0; j < 4; j++) {
								int index = indexArr[j];
								int type = index >= 0 && index < isPaneCache.length
									? getPaneType(page.items[index], index, isPaneCache)
									: -1;
								if (type > 0) {
									int ctmIndex = getCTMIndex(page, index, isPaneCache, ctmIndexCache);
									if (ctmIndex < 0) continue;

									boolean renderConnection;
									boolean horizontal = ctmIndex == 1 || ctmIndex == 2 || ctmIndex == 3;
									boolean vertical = ctmIndex == 12 || ctmIndex == 24 || ctmIndex == 36;
									if ((k % 9 == 0 && index % 9 == 0) || (k % 9 == 8 && index % 9 == 8)) {
										renderConnection = horizontal || vertical;
									} else if (index == leftIndex || index == rightIndex) {
										renderConnection = horizontal;
									} else {
										renderConnection = vertical;
									}

									if (renderConnection) {
										shouldLimitBorder[k] = true;
										hasConnection = true;

										Minecraft.getMinecraft().getTextureManager().bindTexture(STORAGE_PANE_CTM_TEXTURE);
										int rgb = getRGBFromPane(type - 1);
										int a = (rgb >> 24) & 0xFF;
										int r = (rgb >> 16) & 0xFF;
										int g = (rgb >> 8) & 0xFF;
										int b = rgb & 0xFF;
										GlStateManager.color(r / 255f, g / 255f, b / 255f, a / 255f);

										GlStateManager.pushMatrix();
										GlStateManager.translate(itemX - 1 + 9, itemY - 1 + 9, 10);
										GlStateManager.rotate(j * 90, 0, 0, 1);
										GlStateManager.enableAlpha();
										GlStateManager.disableLighting();

										boolean horzFlip = false;
										boolean vertFlip = false;

										if (index == leftIndex) {
											vertFlip = true;
										} else if (index == downIndex) {
											vertFlip = true;
										}

										GlStateManager.enableDepth();
										Utils.drawTexturedRect(0, -9, 8, 18,
											!horzFlip ? 209 / 227f : 219 / 227f, horzFlip ? 227 / 227f : 217 / 227f,
											!vertFlip ? 57 / 75f : 75f / 75f, vertFlip ? 57 / 75f : 75f / 75f, GL11.GL_NEAREST
										);
										GlStateManager.translate(0, 0, 120);
										Utils.drawTexturedRect(8, -9, 10, 18,
											!horzFlip ? 217 / 227f : 209 / 227f, horzFlip ? 219 / 227f : 227 / 227f,
											!vertFlip ? 57 / 75f : 75f / 75f, vertFlip ? 57 / 75f : 75f / 75f, GL11.GL_NEAREST
										);
										GlStateManager.translate(0, 0, -120);

										GlStateManager.popMatrix();
									}
								}
							}

							if (hasConnection) {
								page.shouldDarkenIfNotSelected[k] = false;

								GlStateManager.disableAlpha();
								GlStateManager.translate(0, 0, 10);
								Gui.drawRect(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0x01000000);
								GlStateManager.translate(0, 0, -10);
								GlStateManager.enableAlpha();
							}
						}
					}
				}

				StorageOverlayRenderHelper.renderInventoryLines(storageX, storageY, storageW, storageH, rows);

				boolean whiteOverlay = false;

				for (int k = 0; k < rows * 9; k++) {
					ItemStack stack = page.items[k];
					int itemX = storageX + 1 + 18 * (k % 9);
					int itemY = storageY + 1 + 18 * (k / 9);

					if (!searchBar.getText().isEmpty()) {
						if (stack == null || !ExtraUtils.doesStackMatchSearch(stack, searchBar.getText())) {
							GlStateManager.disableDepth();
//							Gui.drawRect(itemX, itemY, itemX + 16, itemY + 16, 0xffff0000);
							Gui.drawRect(itemX, itemY, itemX + 16, itemY + 16, 0x80000000);
							GlStateManager.enableDepth();
						}
					}

					GlStateManager.disableLighting();

					if (mouseInsideStorages && mouseX >= guiLeft + itemX && mouseX < guiLeft + itemX + 18 &&
						mouseY >= guiTop + itemY && mouseY < guiTop + itemY + 18) {
						boolean allowHover = PrisonsModConfig.INSTANCE.storageGUI.fancyPanes != 1 || !hasCaches ||
							isPaneCaches[storageId][k] <= 0;

						if (storageId != StorageManager.getInstance().getCurrentPageId()) {
							hoveringOtherBackpack = true;
							whiteOverlay = stackOnMouse == null;
						} else if (stack == null || allowHover) {
							itemHoverX = itemX;
							itemHoverY = itemY;
						}

						if (stack != null && allowHover) {
							tooltipToDisplay = stack.getTooltip(
								Minecraft.getMinecraft().thePlayer,
								Minecraft.getMinecraft().gameSettings.advancedItemTooltips
							);
						}
					}
				}

				GlStateManager.disableDepth();
				if (storageId == currentPage) {
					if (isPaneCaches[storageId] != null && isPaneCaches[storageId].length == rows * 9 &&
						ctmIndexCaches[storageId] != null && ctmIndexCaches[storageId].length == rows * 9) {
						int[] isPaneCache = isPaneCaches[storageId];

						int borderStartY = 0;
						int borderEndY = storageH;
						int borderStartX = 0;
						int borderEndX = storageW;

						boolean allChroma = true;
						for (int y = 0; y < page.rows; y++) {
							for (int x = 0; x < 9; x++) {
								int index = x + y * 9;
								if (isPaneCache[index] != 17) {
									allChroma = false;
									break;
								}
							}
						}

						out:
						for (int y = 0; y < page.rows; y++) {
							for (int x = 0; x < 9; x++) {
								int index = x + y * 9;
								if (isPaneCache[index] <= 0 && !shouldLimitBorder[index]) {
									borderStartY = y * 18;
									break out;
								}
							}
						}
						out:
						for (int y = page.rows - 1; y >= 0; y--) {
							for (int x = 0; x < 9; x++) {
								int index = x + y * 9;
								if (isPaneCache[index] <= 0 && !shouldLimitBorder[index]) {
									borderEndY = y * 18 + 18; //Bottom
									break out;
								}
							}
						}
						out:
						for (int x = 0; x < 9; x++) {
							for (int y = 0; y < page.rows; y++) {
								int index = x + y * 9;
								if (isPaneCache[index] <= 0 && !shouldLimitBorder[index]) {
									borderStartX = x * 18;
									break out;
								}
							}
						}
						out:
						for (int x = 8; x >= 0; x--) {
							for (int y = 0; y < page.rows; y++) {
								int index = x + y * 9;
								if (isPaneCache[index] <= 0 && !shouldLimitBorder[index]) {
									borderEndX = x * 18 + 18; //Bottom
									break out;
								}
							}
						}
						StorageOverlayRenderHelper.renderSelectedStorageOutline(storageX, storageY, borderStartX, borderStartY, borderEndX, borderEndY);

						if (allChroma) {
							ResourceLocation loc;
							if (rollIndex < NOT_RICKROLL_SEQ.length) {
								loc = NOT_RICKROLL_SEQ[rollIndex];
							} else {
								loc = NOT_RICKROLL_SEQ[NOT_RICKROLL_SEQ.length * 2 - rollIndex - 1];
							}
							Minecraft.getMinecraft().getTextureManager().bindTexture(loc);
							GlStateManager.color(1, 1, 1, 1);
							Utils.drawTexturedRect(storageX, storageY, storageW, storageH, GL11.GL_LINEAR);
						}
					} else {
						int borderColour =
							ChromaColour.specialToChromaRGB(PrisonsModConfig.INSTANCE.storageGUI.selectedStorageColour);
						Gui.drawRect(storageX + 1, storageY, storageX, storageY + storageH, borderColour); //Left
						Gui.drawRect(
							storageX + storageW - 1,
							storageY,
							storageX + storageW,
							storageY + storageH,
							borderColour
						); //Right
						Gui.drawRect(storageX, storageY - 1, storageX + storageW, storageY, borderColour); //Top
						Gui.drawRect(
							storageX,
							storageY + storageH - 1,
							storageX + storageW,
							storageY + storageH,
							borderColour
						); //Bottom
					}
				} else if (currentTime - StorageManager.getInstance().storageOpenSwitchMillis < 1000 &&
					StorageManager.getInstance().desiredStoragePage == storageId &&
					StorageManager.getInstance().getCurrentPageId() != storageId) {
					Gui.drawRect(storageX, storageY, storageX + storageW, storageY + storageH, 0x30000000);

					Minecraft.getMinecraft().getTextureManager().bindTexture(LOAD_CIRCLE_SEQ[loadCircleIndex]);
					GlStateManager.color(loadCircleColour.getRed() / 255f, loadCircleColour.getGreen() / 255f,
						loadCircleColour.getBlue() / 255f, 1
					);

					GlStateManager.pushMatrix();
					GlStateManager.translate(storageX + storageW / 2, storageY + storageH / 2, 0);
					GlStateManager.rotate(loadCircleRotation, 0, 0, 1);
					Utils.drawTexturedRect(-10, -10, 20, 20, GL11.GL_LINEAR);
					GlStateManager.popMatrix();
				} else if (whiteOverlay) {
					Gui.drawRect(storageX, storageY, storageX + storageW, storageY + storageH, 0x80ffffff);
				} else {
					if (page.rows <= 0) {
						Gui.drawRect(storageX, storageY, storageX + storageW, storageY + storageH, 0x40000000);
					} else {
						for (int i = 0; i < page.rows * 9; i++) {
							if (page.items[i] == null || page.shouldDarkenIfNotSelected[i]) {
								int x = storageX + 18 * (i % 9);
								int y = storageY + 18 * (i / 9);
								Gui.drawRect(x, y, x + 18, y + 18, 0x40000000);
							}
						}
					}
				}

				if (StorageManager.getInstance().desiredStoragePage == storageId &&
					StorageManager.getInstance().onGeneralVaultsPage) {
					Utils.drawStringCenteredScaledMaxWidth("Please click again to load...", fontRendererObj,
						storageX + 81 - 1, storageY + storageH / 2 - 5, false, 150, 0x111111
					);
					Utils.drawStringCenteredScaledMaxWidth("Please click again to load...", fontRendererObj,
						storageX + 81 + 1, storageY + storageH / 2 - 5, false, 150, 0x111111
					);
					Utils.drawStringCenteredScaledMaxWidth("Please click again to load...", fontRendererObj,
						storageX + 81, storageY + storageH / 2 - 5 - 1, false, 150, 0x111111
					);
					Utils.drawStringCenteredScaledMaxWidth("Please click again to load...", fontRendererObj,
						storageX + 81, storageY + storageH / 2 - 5 + 1, false, 150, 0x111111
					);
					Utils.drawStringCenteredScaledMaxWidth("Please click again to load...", fontRendererObj,
						storageX + 81, storageY + storageH / 2 - 5, false, 150, 0xffdf00
					);

					Utils.drawStringCenteredScaledMaxWidth("Use /neustwhy for more info", fontRendererObj,
						storageX + 81 - 1, storageY + storageH / 2 + 5, false, 150, 0x111111
					);
					Utils.drawStringCenteredScaledMaxWidth("Use /neustwhy for more info", fontRendererObj,
						storageX + 81 + 1, storageY + storageH / 2 + 5, false, 150, 0x111111
					);
					Utils.drawStringCenteredScaledMaxWidth("Use /neustwhy for more info", fontRendererObj,
						storageX + 81, storageY + storageH / 2 + 5 - 1, false, 150, 0x111111
					);
					Utils.drawStringCenteredScaledMaxWidth("Use /neustwhy for more info", fontRendererObj,
						storageX + 81, storageY + storageH / 2 + 5 + 1, false, 150, 0x111111
					);
					Utils.drawStringCenteredScaledMaxWidth("Use /neustwhy for more info", fontRendererObj,
						storageX + 81, storageY + storageH / 2 + 5, false, 150, 0xffdf00
					);
				}

				GlStateManager.enableDepth();
			}
		}
		GlScissorStack.pop(scaledResolution);

		if (fastRender) {
			fontRendererObj.drawString(
				"Fast render and antialiasing do not work with Storage overlay.",
				sizeX / 2 - fontRendererObj.getStringWidth("Fast render and antialiasing do not work with Storage overlay.") / 2,
				-10,
				0xFFFF0000
			);
		}

		//Inventory Text
		fontRendererObj.drawString("Inventory", 180, storageViewSize + 6, textColour);
		searchBar.setCustomTextColour(searchTextColour);
		searchBar.render(252, storageViewSize + 5);

		//Player Inventory
		ItemStack[] playerItems = Minecraft.getMinecraft().thePlayer.inventory.mainInventory;
		int inventoryStartIndex = containerChest.getLowerChestInventory().getSizeInventory();
		GlStateManager.enableDepth();
		for (int i = 0; i < 9; i++) {
			int itemX = 181 + 18 * i;
			int itemY = storageViewSize + 76;

			GlStateManager.pushMatrix();
			GlStateManager.translate(181 - 8, storageViewSize + 18 - (inventoryStartIndex / 9 * 18 + 31), 0);
			guiChest.drawSlot(containerChest.inventorySlots.get(inventoryStartIndex + i));
			GlStateManager.popMatrix();

			if (!searchBar.getText().isEmpty()) {
				if (playerItems[i] == null || !ExtraUtils.doesStackMatchSearch(
						playerItems[i],
						searchBar.getText()
				)) {
					GlStateManager.disableDepth();
					Gui.drawRect(itemX, itemY, itemX + 16, itemY + 16, 0x80000000);
					GlStateManager.enableDepth();
				}
			}

			if (mouseX >= guiLeft + itemX && mouseX < guiLeft + itemX + 18 && mouseY >= guiTop + itemY &&
					mouseY < guiTop + itemY + 18) {
				itemHoverX = itemX;
				itemHoverY = itemY;

				if (playerItems[i] != null) {
					tooltipToDisplay = playerItems[i].getTooltip(
							Minecraft.getMinecraft().thePlayer,
							Minecraft.getMinecraft().gameSettings.advancedItemTooltips
					);
				}
			}
		}
		for (int i = 0; i < 27; i++) {
			int itemX = 181 + 18 * (i % 9);
			int itemY = storageViewSize + 18 + 18 * (i / 9);
			StorageOverlayRenderHelper.renderInnerInventorySearchGraying(itemX, itemY, i, storageViewSize, containerChest, inventoryStartIndex, playerItems, searchBar);

			// Update highlighted item tooltip to render
			if (mouseX >= guiLeft + itemX && mouseX < guiLeft + itemX + 18 && mouseY >= guiTop + itemY && mouseY < guiTop + itemY + 18) {
				itemHoverX = itemX;
				itemHoverY = itemY;

				if (playerItems[i + 9] != null) {
					tooltipToDisplay = playerItems[i + 9].getTooltip(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
				}
			}
		}

		//Backpack Selector
		fontRendererObj.drawString("Ender Chest Pages", 9, storageViewSize + 12, textColour);
		fontRendererObj.drawString("Storage Pages", 9, storageViewSize + 44, textColour);
		if (StorageManager.getInstance().onGeneralVaultsPage) {
			for (int i = 0; i < 9; i++) {
				int itemX = 10 + i * 18;
				int itemY = storageViewSize + 24;
				ItemStack stack = containerChest.getLowerChestInventory().getStackInSlot(i + 9);
				Utils.drawItemStack(stack, itemX, itemY);

				if (mouseX >= guiLeft + itemX && mouseX < guiLeft + itemX + 18 && mouseY >= guiTop + itemY &&
					mouseY < guiTop + itemY + 18) {
					itemHoverX = itemX;
					itemHoverY = itemY;

					if (stack != null) {
						if (/*PrisonsModConfig.INSTANCE.storageGUI.enderchestPreview*/true) slotPreview = i;
						tooltipToDisplay = stack.getTooltip(
							Minecraft.getMinecraft().thePlayer,
							Minecraft.getMinecraft().gameSettings.advancedItemTooltips
						);
					}
				}
			}
			for (int i = 0; i < 18; i++) {
				int itemX = 10 + 18 * (i % 9);
				int itemY = storageViewSize + 56 + 18 * (i / 9);
				ItemStack stack = containerChest.getLowerChestInventory().getStackInSlot(i + 27);
				Utils.drawItemStack(stack, itemX, itemY);

				if (mouseX >= guiLeft + itemX && mouseX < guiLeft + itemX + 18 && mouseY >= guiTop + itemY &&
					mouseY < guiTop + itemY + 18) {
					itemHoverX = itemX;
					itemHoverY = itemY;

					if (stack != null) {
						if (/*PrisonsModConfig.INSTANCE.storageGUI.backpackPreview*/false)
							slotPreview = i + StorageManager.MAX_ENDER_CHEST_PAGES;
						tooltipToDisplay = stack.getTooltip(
							Minecraft.getMinecraft().thePlayer,
							Minecraft.getMinecraft().gameSettings.advancedItemTooltips
						);
					}
				}
			}
		} else {
			for (int i = 0; i < StorageManager.getInstance().numberOfPvs(); i++) {
				StorageManager.StoragePage page = StorageManager.getInstance().getPage(i, false);
				int itemX = 10 + (i % 9) * 18;
				int itemY = storageViewSize + 24 + (i / 9) * 18;

				ItemStack stack;
				if (page != null && page.backpackDisplayStack != null) {
					stack = page.backpackDisplayStack;
				} else {
					stack = StorageManager.LOCKED_ENDERCHEST_STACK;
				}

				if (stack != null) {
					Utils.drawItemStack(stack, itemX, itemY);

					if (mouseX >= guiLeft + itemX && mouseX < guiLeft + itemX + 18 && mouseY >= guiTop + itemY &&
						mouseY < guiTop + itemY + 18) {
						itemHoverX = itemX;
						itemHoverY = itemY;
						if (/*PrisonsModConfig.INSTANCE.storageGUI.enderchestPreview*/true) slotPreview = i;
						tooltipToDisplay = stack.getTooltip(
							Minecraft.getMinecraft().thePlayer,
							Minecraft.getMinecraft().gameSettings.advancedItemTooltips
						);
					}
				}
			}
			// render backpacks
//			for (int i = 0; i < 18; i++) {
//				StorageManager.StoragePage page = StorageManager.getInstance().getPage(
//					i + StorageManager.MAX_ENDER_CHEST_PAGES,
//					false
//				);
//				int itemX = 10 + (i % 9) * 18;
//				int itemY = storageViewSize + 56 + (i / 9) * 18;
//
//				ItemStack stack;
//				if (page != null && page.backpackDisplayStack != null) {
//					stack = page.backpackDisplayStack;
//				} else {
//					stack = StorageManager.getInstance().getMissingBackpackStack(i);
//				}
//
//				if (stack != null) {
//					Utils.drawItemStack(stack, itemX, itemY);
//
//					if (mouseX >= guiLeft + itemX && mouseX < guiLeft + itemX + 18 && mouseY >= guiTop + itemY &&
//						mouseY < guiTop + itemY + 18) {
//						itemHoverX = itemX;
//						itemHoverY = itemY;
//						if (/*PrisonsModConfig.INSTANCE.storageGUI.backpackPreview*/false)
//							slotPreview = i + StorageManager.MAX_ENDER_CHEST_PAGES;
//						tooltipToDisplay = stack.getTooltip(
//							Minecraft.getMinecraft().thePlayer,
//							Minecraft.getMinecraft().gameSettings.advancedItemTooltips
//						);
//
//						if (!StorageManager.getInstance().onGeneralVaultsPage) {
//							List<String> tooltip = new ArrayList<>();
//							for (String line : tooltipToDisplay) {
//								tooltip.add(line.replace("Right-click to remove", "Click \"Edit\" to manage"));
//							}
//							tooltipToDisplay = tooltip;
//						}
//					}
//				}
//			}
		}

		//Buttons
		Minecraft.getMinecraft().getTextureManager().bindTexture(STORAGE_ICONS_TEXTURE);
		GlStateManager.color(1, 1, 1, 1);
		for (int i = 0; i < 10; i++) {
			int buttonX = 388 + (i % 5) * 18;
			int buttonY = getStorageViewSize() + 35 + (i / 5) * 18;

			float minU = (i * 16) / 256f;
			float maxU = (i * 16 + 16) / 256f;

			int vIndex = 0;

			switch (i) {
				case 2:
					vIndex = PrisonsModConfig.INSTANCE.storageGUI.displayStyle;
					break;
				case 3:
					vIndex = /*PrisonsModConfig.INSTANCE.storageGUI.backpackPreview*/false ? 1 : 0;
					break;
				case 4:
					vIndex = /*PrisonsModConfig.INSTANCE.storageGUI.enderchestPreview*/true ? 1 : 0;
					break;
				case 5:
					vIndex = PrisonsModConfig.INSTANCE.storageGUI.compactVertically ? 1 : 0;
					break;
				case 6:
					vIndex = PrisonsModConfig.INSTANCE.storageGUI.fancyPanes == 2
						? 0
						: PrisonsModConfig.INSTANCE.storageGUI.fancyPanes + 1;
					break;
				case 7:
					vIndex = PrisonsModConfig.INSTANCE.storageGUI.searchBarAutofocus ? 1 : 0;
					break;
				case 8:
					/*vIndex = PrisonsModConfig.INSTANCE.storageGUI.showEnchantGlint ? 1 : 0;*/
					break;
			}

			Utils.drawTexturedRect(
				buttonX,
				buttonY,
				16,
				16,
				minU,
				maxU,
				(vIndex * 16) / 256f,
				(vIndex * 16 + 16) / 256f,
				GL11.GL_NEAREST
			);

			if (mouseX >= guiLeft + buttonX && mouseX < guiLeft + buttonX + 18 &&
				mouseY >= guiTop + buttonY && mouseY < guiTop + buttonY + 18) {
				switch (i) {
					case 0:
						tooltipToDisplay = createTooltip(
							"Enable GUI",
							0,
							"On",
							"Off"
						);
						break;
					case 1:
						int tooltipStorageHeight = desiredHeightSwitch != -1 ? desiredHeightSwitch :
								PrisonsModConfig.INSTANCE.storageGUI.storageHeight;
						tooltipToDisplay = createTooltip(
							"Storage View Height",
							Math.round((tooltipStorageHeight - 104) / 52f),
							"Tiny",
							"Small",
							"Medium",
							"Large",
							"Huge"
						);
						if (desiredHeightSwitch != -1) {
							tooltipToDisplay.add("");
							tooltipToDisplay.add(EnumChatFormatting.YELLOW + "* Move mouse to apply changes *");
						}
						break;
					case 2:
						tooltipToDisplay = createTooltip(
							"Overlay Style",
								PrisonsModConfig.INSTANCE.storageGUI.displayStyle,
							"Transparent",
							"Minecraft",
							"Dark",
							"Custom"
						);
						break;
					case 3:
						tooltipToDisplay = createTooltip(
							"Backpack Preview",
								/*PrisonsModConfig.INSTANCE.storageGUI.backpackPreview*/false ? 0 : 1,
							"On",
							"Off"
						);
						break;
					case 4:
						tooltipToDisplay = createTooltip(
							"Enderchest Preview",
								/*PrisonsModConfig.INSTANCE.storageGUI.enderchestPreview*/true ? 0 : 1,
							"On",
							"Off"
						);
						break;
					case 5:
						tooltipToDisplay = createTooltip(
							"Compact Vertically",
								PrisonsModConfig.INSTANCE.storageGUI.compactVertically ? 0 : 1,
							"On",
							"Off"
						);
						break;
					case 6:
						tooltipToDisplay = createTooltip(
							"Fancy Glass Panes",
								PrisonsModConfig.INSTANCE.storageGUI.fancyPanes,
							"On",
							"Locked",
							"Off"
						);
						tooltipToDisplay.add(1, "\u00a7eReplace the glass pane textures");
						tooltipToDisplay.add(2, "\u00a7ein your storage containers with");
						tooltipToDisplay.add(3, "\u00a7ea fancy connected texture");
						break;
					case 7:
						tooltipToDisplay = createTooltip(
							"Search Bar Autofocus",
								PrisonsModConfig.INSTANCE.storageGUI.searchBarAutofocus ? 0 : 1,
							"On",
							"Off"
						);
						break;
					case 8:
						tooltipToDisplay = createTooltip(
							"Show Enchant Glint",
								/*PrisonsModConfig.INSTANCE.storageGUI.showEnchantGlint ? 0 :*/ 1,
							"On",
							"Off"
						);
						break;
					case 9:
						tooltipToDisplay = createTooltip(
							"Open Full Settings",
							0,
							"Click To Open"
						);
						break;
				}
			}
		}

		if (!StorageManager.getInstance().onGeneralVaultsPage) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(storageTexture);
			GlStateManager.color(1, 1, 1, 1);
			Utils.drawTexturedRect(
				171 - 36,
				41 + storageViewSize,
				36,
				14,
				24 / 600f,
				60 / 600f,
				251 / 400f,
				265 / 400f,
				GL11.GL_NEAREST
			);
		}

		if (itemHoverX >= 0 && itemHoverY >= 0) {
			GlStateManager.disableDepth();
			GlStateManager.colorMask(true, true, true, false);
			Gui.drawRect(itemHoverX, itemHoverY, itemHoverX + 16, itemHoverY + 16, 0x80ffffff);
			GlStateManager.colorMask(true, true, true, true);
			GlStateManager.enableDepth();
		}

		GlStateManager.popMatrix();
		GlStateManager.translate(0, 0, 300);
		allowTypingInSearchBar = false;
		StorageOverlayRenderHelper.drawMouseHovered(stackOnMouse, hoveringOtherBackpack, mouseX, mouseY, slotPreview, width, height, tooltipToDisplay);
		GlStateManager.translate(0, 0, -300);
	}

	private List<String> createTooltip(String title, int selectedOption, String... options) {
		String selPrefix = EnumChatFormatting.DARK_AQUA + " \u25b6 ";
		String unselPrefix = EnumChatFormatting.GRAY.toString();

		for (int i = 0; i < options.length; i++) {
			if (i == selectedOption) {
				options[i] = selPrefix + options[i];
			} else {
				options[i] = unselPrefix + options[i];
			}
		}

		List<String> list = Lists.newArrayList(options);
		list.add(0, "");
		list.add(0, EnumChatFormatting.GREEN + title);
		return list;
	}

	public IntPair getPageCoords(int displayId) {
		if (displayId < 0) displayId = 0;

		int y;
		if (PrisonsModConfig.INSTANCE.storageGUI.compactVertically) {
			y = -scroll.getValue() + 18 + 108 * (displayId / 3);
		} else {
			y = -scroll.getValue() + 17 + 104 * (displayId / 3);
		}
		for (int i = 0; i <= displayId - 3; i += 3) {
			int maxRows = 1;
			for (int j = i; j < i + 3; j++) {
				if (PrisonsModConfig.INSTANCE.storageGUI.compactVertically && displayId % 3 != j % 3) continue;

				if (!StorageManager.getInstance().storageConfig.displayToStorageIdMap_TO_RENDER.containsKey(j)) {
					continue;
				}
				int storageId = StorageManager.getInstance().storageConfig.displayToStorageIdMap_TO_RENDER.get(j);
				StorageManager.StoragePage page = StorageManager.getInstance().getPage(storageId, false);
				if (page == null || page.rows <= 0) {
					maxRows = Math.max(maxRows, 3);
				} else {
					maxRows = Math.max(maxRows, page.rows);
				}
			}
			y -= (5 - maxRows) * 18;
		}

		return new IntPair(8 + 172 * (displayId % 3), y);
	}

	@Override
	public boolean mouseInput(int mouseX, int mouseY) {
		if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) return false;

		int dWheel = Mouse.getEventDWheel();
		// scrolling with scrollwheel
		if (dWheel != 0) {
			if (dWheel < 0) {
				dWheel = -1;
				if (scrollVelocity > 0) scrollVelocity = 0;
			}
			if (dWheel > 0) {
				dWheel = 1;
				if (scrollVelocity < 0) scrollVelocity = 0;
			}

			long currentTime = System.currentTimeMillis();
			if (currentTime - lastScroll > 200) {
				scrollVelocity = 0;
			} else {
				scrollVelocity = (int) (scrollVelocity / 1.3f);
			}
			lastScroll = currentTime;

			scrollVelocity += dWheel * 10;
			scrollToY(scroll.getTarget() - scrollVelocity);

			return true;
		}

		if (Mouse.getEventButtonState()) {
			editingNameId = -1;
		}

		if (Mouse.getEventButton() == 0) {
			if (!Mouse.getEventButtonState()) {
				scrollGrabOffset = -1;
			} else if (mouseX >= guiLeft + 519 && mouseX <= guiLeft + 519 + 14 &&
				mouseY >= guiTop + 8 && mouseY <= guiTop + 2 + getStorageViewSize()) {
				int scrollMouseY = mouseY - (guiTop + 8);
				int scrollBarY = Math.round(getScrollBarHeight() * scroll.getValue() / (float) getMaximumScroll());

				if (scrollMouseY >= scrollBarY && scrollMouseY < scrollBarY + 12) {
					scrollGrabOffset = scrollMouseY - scrollBarY;
				}
			}
		}
		if (scrollGrabOffset >= 0 && Mouse.getEventButton() == -1 && !Mouse.getEventButtonState()) {
			int scrollMouseY = mouseY - (guiTop + 8);
			int scrollBarY = scrollMouseY - scrollGrabOffset;

			scrollToY(Math.round(scrollBarY * getMaximumScroll() / (float) getScrollBarHeight()));
			scroll.setTimeToReachTarget(10);
		}

		ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		int width = scaledResolution.getScaledWidth();
		int height = scaledResolution.getScaledHeight();

		int storageViewSize = getStorageViewSize();

		int sizeX = 540;
		int sizeY = 100 + storageViewSize;
		int searchNobX = 18;

		guiLeft = width / 2 - (sizeX - searchNobX) / 2;
		guiTop = height / 2 - sizeY / 2;

		if (Mouse.getEventButtonState() && !StorageManager.getInstance().onGeneralVaultsPage) {
			if (mouseX > guiLeft + 171 - 36 && mouseX < guiLeft + 171 && mouseY > guiTop + 41 + storageViewSize && mouseY < guiTop + 41 + storageViewSize + 14) {
//				NotEnoughUpdates.INSTANCE.sendChatMessage("/storage");
				// TODO: Fix
				StorageManager.getInstance().sendToPage(0);
				searchBar.setFocus(false);
				return true;
			}
		}

		if (Mouse.getEventButtonState()) {
			if (mouseX >= guiLeft + 252 && mouseX <= guiLeft + 252 + searchBar.getWidth() &&
				mouseY >= guiTop + storageViewSize + 5 && mouseY <= guiTop + storageViewSize + 5 + searchBar.getHeight()) {
				if (searchBar.getFocus()) {
					searchBar.mouseClicked(mouseX - guiLeft, mouseY - guiTop, Mouse.getEventButton());
					StorageManager.getInstance().searchDisplay(searchBar.getText());
					dirty = true;
				} else {
					searchBar.setFocus(true);
					if (Mouse.getEventButton() == 1) {
						searchBar.setText("");
						StorageManager.getInstance().searchDisplay(searchBar.getText());
						dirty = true;
					}
				}
			} else {
				searchBar.setFocus(false);
			}
		}

		if (mouseX > guiLeft + 181 && mouseX < guiLeft + 181 + 162 && mouseY > guiTop + storageViewSize + 18 && mouseY < guiTop + storageViewSize + 94) {
			if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
				dirty = true;
			return false;
		}

		if (mouseY > guiTop + 3 && mouseY < guiTop + storageViewSize + 3) {
			int currentPage = StorageManager.getInstance().getCurrentPageId();
			for (Map.Entry<Integer, Integer> entry : StorageManager.getInstance().storageConfig.displayToStorageIdMap_TO_RENDER.entrySet()) {
				IntPair pageCoords = getPageCoords(entry.getKey());

				if (pageCoords.y > storageViewSize + 3 || pageCoords.y + 90 < 3) continue;

				StorageManager.StoragePage page = StorageManager.getInstance().getPage(entry.getValue(), false);
				int rows = page == null ? 3 : page.rows <= 0 ? 3 : page.rows;

				// page name editing
				if (page != null) {
					String pageTitle;
					if (page.customTitle != null && !page.customTitle.isEmpty()) {
						pageTitle = page.customTitle;
					} else/* if (entry.getValue() < 9)*/ {
						pageTitle = "Ender Chest Page " + (entry.getValue() + 1);
					}/* else {
						pageTitle = "Backpack Slot " + (entry.getValue() - 8);
					}*/
					int titleLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(pageTitle);

					if (mouseX >= guiLeft + pageCoords.x && mouseX <= guiLeft + pageCoords.x + titleLen + 15 &&
						mouseY >= guiTop + pageCoords.y - 14 && mouseY <= guiTop + pageCoords.y + 1) {
						if (Mouse.getEventButtonState() && (Mouse.getEventButton() == 0 || Mouse.getEventButton() == 1)) {
							if (editingNameId != entry.getValue()) {
								editingNameId = entry.getValue();
								if (!renameStorageField.getText().equalsIgnoreCase(pageTitle)) {
									renameStorageField.setText(pageTitle);
								}
							}
							if (!renameStorageField.getFocus()) {
								renameStorageField.setFocus(true);
							} else {
								renameStorageField.mouseClicked(mouseX - guiLeft, mouseY - guiTop, Mouse.getEventButton());
							}
						} else if (Mouse.getEventButton() < 0 && Mouse.isButtonDown(0)) {
							renameStorageField.mouseClickMove(mouseX - guiLeft, mouseY - guiTop, 0, 0);
						}
						return true;
					}
				}

				// clicking into a different page
				if (mouseX > guiLeft + pageCoords.x && mouseX < guiLeft + pageCoords.x + 162 &&
					mouseY > guiTop + pageCoords.y && mouseY < guiTop + pageCoords.y + rows * 18) {
					if (currentPage >= 0 && entry.getValue() == currentPage) {
						dirty = true;
						return false;
					} else {
						if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0 &&
							Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null &&
							page != null) {
							scrollToStorage(entry.getKey(), false);
							StorageManager.getInstance().sendToPage(entry.getValue());
							return true;
						}
					}
				}
			}
		}

		// buttons?
		for (int i = 0; i < 10; i++) {
			int buttonX = 388 + (i % 5) * 18;
			int buttonY = getStorageViewSize() + 35 + (i / 5) * 18;

			float minU = (i * 16) / 256f;
			float maxU = (i * 16 + 16) / 256f;

			int vIndex = 0;

			switch (i) {
				case 2:
					vIndex = PrisonsModConfig.INSTANCE.storageGUI.displayStyle;
					break;
                /*case 3:
                    vIndex = */
			}

			Utils.drawTexturedRect(buttonX, buttonY, 16, 16, minU, maxU, (vIndex * 16) / 256f, (vIndex * 16 + 16) / 256f, GL11.GL_NEAREST);
		}
		// buttons?
		if (desiredHeightSwitch != -1 && Mouse.getEventButton() == -1 && !Mouse.getEventButtonState()) {
			int delta = Math.abs(desiredHeightMX - mouseX) + Math.abs(desiredHeightMY - mouseY);
			if (delta > 3) {
				PrisonsModConfig.INSTANCE.storageGUI.storageHeight = desiredHeightSwitch;
				desiredHeightSwitch = -1;
			}
		}
		// buttons?
		if (Mouse.getEventButtonState() && mouseX >= guiLeft + 388 && mouseX < guiLeft + 388 + 90 &&
			mouseY >= guiTop + storageViewSize + 35 && mouseY < guiTop + storageViewSize + 35 + 36) {
			int xN = mouseX - (guiLeft + 388);
			int yN = mouseY - (guiTop + storageViewSize + 35);

			int xIndex = xN / 18;
			int yIndex = yN / 18;

			int buttonIndex = xIndex + 5 * yIndex;

			switch (buttonIndex) {
				case 0:
					PrisonsModConfig.INSTANCE.storageGUI.storageGuiEnabled = false;
					break;
				case 1:
					int size =
						desiredHeightSwitch != -1 ? desiredHeightSwitch : PrisonsModConfig.INSTANCE.storageGUI.storageHeight;
					int sizeIndex = Math.round((size - 104) / 54f);
					if (Mouse.getEventButton() == 0) {
						sizeIndex--;
					} else {
						sizeIndex++;
					}
					size = sizeIndex * 54 + 104;
					if (size < 104) size = 312;
					if (size > 320) size = 104;
					desiredHeightMX = mouseX;
					desiredHeightMY = mouseY;
					desiredHeightSwitch = size;
					break;
				case 2:
					int displayStyle = PrisonsModConfig.INSTANCE.storageGUI.displayStyle;
					if (Mouse.getEventButton() == 0) {
						displayStyle++;
					} else {
						displayStyle--;
					}
					if (displayStyle < 0) displayStyle = STORAGE_TEXTURES.length - 1;
					if (displayStyle >= STORAGE_TEXTURES.length) displayStyle = 0;

					PrisonsModConfig.INSTANCE.storageGUI.displayStyle = displayStyle;
					break;
				case 3:
//					PrisonsModConfig.INSTANCE.storageGUI.backpackPreview =
//						!PrisonsModConfig.INSTANCE.storageGUI.backpackPreview;
					break;
				case 4:
//					PrisonsModConfig.INSTANCE.storageGUI.enderchestPreview =
//						!PrisonsModConfig.INSTANCE.storageGUI.enderchestPreview;
					break;
				case 5:
					PrisonsModConfig.INSTANCE.storageGUI.compactVertically =
						!PrisonsModConfig.INSTANCE.storageGUI.compactVertically;
					break;
				case 6:
					int fancyPanes = PrisonsModConfig.INSTANCE.storageGUI.fancyPanes;
					if (Mouse.getEventButton() == 0) {
						fancyPanes++;
					} else {
						fancyPanes--;
					}
					if (fancyPanes < 0) fancyPanes = 2;
					if (fancyPanes >= 3) fancyPanes = 0;

					PrisonsModConfig.INSTANCE.storageGUI.fancyPanes = fancyPanes;
					break;
				case 7:
					PrisonsModConfig.INSTANCE.storageGUI.searchBarAutofocus =
						!PrisonsModConfig.INSTANCE.storageGUI.searchBarAutofocus;
					break;
				case 8:
					/*PrisonsModConfig.INSTANCE.storageGUI.showEnchantGlint =
						!PrisonsModConfig.INSTANCE.storageGUI.showEnchantGlint;*/
					break;
				case 9:
					throw new NotImplementedException("full neu settings");
//					ClientCommandHandler.instance.executeCommand(Minecraft.getMinecraft().thePlayer, "/neu storage gui");
//					break;
			}
			dirty = true;
		}

		// load new storages
		if (mouseX >= guiLeft + 10 && mouseX <= guiLeft + 171 && mouseY >= guiTop + storageViewSize + 23 && mouseY <= guiTop + storageViewSize + 91) {
			if (StorageManager.getInstance().onGeneralVaultsPage) {
				return false;
			} else if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
				for (int i = 0; i < StorageManager.getInstance().numberOfPvs(); i++) {
					int storageId = i;
					int displayId = StorageManager.getInstance().getDisplayIdForStorageIdRender(i);

					StorageManager.StoragePage page = StorageManager.getInstance().getPage(storageId, false);
					if (page != null) {
						int itemX = 10 + (i % 9) * 18;
						int itemY = storageViewSize + 24 + (i / 9) * 18;

						if (mouseX >= guiLeft + itemX && mouseX < guiLeft + itemX + 18 &&
							mouseY >= guiTop + itemY && mouseY < guiTop + itemY + 18) {
							StorageManager.getInstance().sendToPage(storageId);
							scrollToStorage(displayId, true);
							return true;
						}
					}
				}
				for (int i = 0; i < 18; i++) {
					int storageId = i + StorageManager.MAX_ENDER_CHEST_PAGES;
					int displayId = StorageManager.getInstance().getDisplayIdForStorageIdRender(i);

					StorageManager.StoragePage page = StorageManager.getInstance().getPage(storageId, false);
					if (page != null) {
						int itemX = 10 + (i % 9) * 18;
						int itemY = storageViewSize + 56 + (i / 9) * 18;

						if (mouseX >= guiLeft + itemX && mouseX < guiLeft + itemX + 18 &&
							mouseY >= guiTop + itemY && mouseY < guiTop + itemY + 18) {
							StorageManager.getInstance().sendToPage(storageId);
							scrollToStorage(displayId, true);
							return true;
						}
					}
				}
			}
		}

		return true;
	}

	public void overrideIsMouseOverSlot(Slot slot, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
		if (StorageManager.getInstance().shouldRenderStorageOverlayFast()) {
			boolean playerInv = slot.inventory == Minecraft.getMinecraft().thePlayer.inventory;
			int slotId = slot.slotIndex;
			if (playerInv) {
				if (isMouseInPlayersInventory(slot, mouseX, mouseY, cir)) {
					cir.setReturnValue(true);
					return;
				}
			} else {
				/*if (StorageManager.getInstance().onStorageMenu) {
					if (slotId >= 9 && slotId < 18) {
						if (mouseY >= guiTop + storageViewSize + 24 && mouseY < guiTop + storageViewSize + 24 + 18) {
							int xN = mouseX - (guiLeft + 10);

							int xClicked = xN / 18;

							if (xClicked == slotId % 9) {
								cir.setReturnValue(true);
								return;
							}
						}
					} else if (slotId >= 27 && slotId < 45) {
						int xN = mouseX - (guiLeft + 10);
						int yN = mouseY - (guiTop + storageViewSize + 56);

						int xClicked = xN / 18;
						int yClicked = yN / 18;

						if (xClicked == slotId % 9 &&
							yClicked >= 0 && yClicked == slotId / 9 - 3) {
							cir.setReturnValue(true);
							return;
						}
					}
				} else*/
				int currentPage = StorageManager.getInstance().getCurrentPageId();
				int displayId = StorageManager.getInstance().getDisplayIdForStorageIdRender(currentPage);
				if (displayId >= 0) {
					IntPair pageCoords = getPageCoords(displayId);

					int xN = mouseX - (guiLeft + pageCoords.x);
					int yN = mouseY - (guiTop + pageCoords.y);

					int xClicked = xN / 18;
					int yClicked = yN / 18;

					if (xClicked >= 0 && xClicked <= 8 && yClicked >= 0 && yClicked <= 6) {
						if (xClicked + yClicked * 9 == slotId) {
							if (PrisonsModConfig.INSTANCE.storageGUI.fancyPanes == 1 && slot.getHasStack() && getPaneType(slot.getStack(), -1, null) > 0) {
								cir.setReturnValue(false);
								return;
							}
							cir.setReturnValue(true);
							return;
						}
					}
				}
			}
			cir.setReturnValue(false);
		}
	}

	private boolean isMouseInPlayersInventory(Slot slot, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
		int currentWindowSlotSize = StorageManager.getInstance().getCurrentPage().rows * 9;
		int slotId = slot.slotIndex;
		int storageViewSize = getStorageViewSize();
		int xN = mouseX - (guiLeft + 181);
		int xClicked = xN / 18;

		if (slotId - currentWindowSlotSize - 27 < 9 && slotId - currentWindowSlotSize - 27 >= 0) { // hotbar
			if (mouseY >= guiTop + storageViewSize + 76 && mouseY <= guiTop + storageViewSize + 92) {
				return xClicked == slotId - currentWindowSlotSize - 27;
			}
		} else { // inner inventory
			int yN = mouseY - (guiTop + storageViewSize + 18);

			int yClicked = yN / 18;

			if (xClicked >= 0 && xClicked <= 8 && yClicked >= 0 && yClicked <= 2) {
				return xClicked + yClicked * 9 == slotId - currentWindowSlotSize;
			}
		}
		return false;
	}

	public void clearSearch() {
		searchBar.setFocus(false);
		searchBar.setText("");
		StorageManager.getInstance().searchDisplay(searchBar.getText());
	}

	@Override
	public boolean keyboardInput() {
		if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
			clearSearch();
			return false;
		}
		if (Keyboard.getEventKey() == Minecraft.getMinecraft().gameSettings.keyBindScreenshot.getKeyCode()) {
			return false;
		}
		if (Keyboard.getEventKey() == Minecraft.getMinecraft().gameSettings.keyBindFullscreen.getKeyCode()) {
			return false;
		}

		if (Keyboard.getEventKeyState()) {
			if (/*PrisonsModConfig.INSTANCE.slotLocking.enableSlotLocking &&
				KeybindHelper.isKeyPressed(PrisonsModConfig.INSTANCE.slotLocking.slotLockKey)*/false && !searchBar.getFocus()) {
				if (!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) return true;
				GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

				ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
				int width = scaledResolution.getScaledWidth();
				int height = scaledResolution.getScaledHeight();
				int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
				int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

				for (Slot slot : container.inventorySlots.inventorySlots) {
					if (slot != null &&
						slot.inventory == Minecraft.getMinecraft().thePlayer.inventory && container.isMouseOverSlot(slot, mouseX, mouseY)) {
//						SlotLocking.getInstance().toggleLock(slot.slotIndex);
						return true;
					}
				}
			}

			if (editingNameId >= 0) {
				if (Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
					editingNameId = -1;
					return true;
				}

				String prevText = renameStorageField.getText();
				renameStorageField.setFocus(true);
				searchBar.setFocus(false);
				renameStorageField.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
				if (!prevText.equals(renameStorageField.getText())) {
					StorageManager.StoragePage page = StorageManager.getInstance().getPage(editingNameId, false);
					if (page != null) {
						page.customTitle = renameStorageField.getText();
					}
				}
			} else if (searchBar.getFocus() ||
				(allowTypingInSearchBar && PrisonsModConfig.INSTANCE.storageGUI.searchBarAutofocus)) {
				String prevText = searchBar.getText();
				searchBar.setFocus(true);
				renameStorageField.setFocus(false);
				searchBar.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
				if (!prevText.equals(searchBar.getText())) {
					StorageManager.getInstance().searchDisplay(searchBar.getText());
					dirty = true;
				}
				if (PrisonsModConfig.INSTANCE.storageGUI.searchBarAutofocus &&
					searchBar.getText().isEmpty()) {
					searchBar.setFocus(false);
				}
			} else return Keyboard.getEventKey() != Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode();

		}

		return true;
	}

	public void fastRenderCheck() {
		if (!OpenGlHelper.isFramebufferEnabled() && /*PrisonsModConfig.INSTANCE.notifications.doFastRenderNotif*/true &&
			PrisonsModConfig.INSTANCE.storageGUI.storageGuiEnabled) {
			this.fastRender = true;
//			NotificationHandler.displayNotification(Lists.newArrayList(
//				"\u00a74Warning",
//				"\u00a77Due to the way fast render and antialiasing work, they're not compatible with NEU.",
//				"\u00a77Please disable fast render and antialiasing in your options under",
//				"\u00a77ESC > Options > Video Settings > Performance > \u00A7cFast Render",
//				"\u00a77ESC > Options > Video Settings > Quality > \u00A7cAntialiasing",
//				"\u00a77This can't be fixed.",
//				"\u00a77",
//				"\u00a77Press X on your keyboard to close this notification"
//			), true, true);
			return;
		}

		this.fastRender = false;
	}

	public static class IntPair {
		public int x;
		public int y;

		public IntPair(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

}