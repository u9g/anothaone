package net.fabricmc.example;

import com.google.gson.annotations.Expose;
import dev.u9g.configlib.config.Config;
import dev.u9g.configlib.config.GuiTextures;
import dev.u9g.configlib.config.annotations.Category;
import dev.u9g.configlib.config.annotations.ConfigEditorBoolean;
import dev.u9g.configlib.config.annotations.ConfigEditorColour;
import dev.u9g.configlib.config.annotations.ConfigEditorDropdown;
import dev.u9g.configlib.config.annotations.ConfigEditorKeybind;
import dev.u9g.configlib.config.annotations.ConfigEditorSlider;
import dev.u9g.configlib.config.annotations.ConfigOption;

public class PrisonsModConfig implements Config {
    public static PrisonsModConfig INSTANCE = new PrisonsModConfig();

    @Expose
    @Category(name = "Storage GUI Options", desc = "Options about the storage gui.")
    public StorageGUI storageGUI = new StorageGUI();

    @Expose
    @Category(name = "Tooltips", desc = "Options about tooltips.")
    public ToolTips toolTips = new ToolTips();

    @Expose
    @Category(name = "Improved Menus", desc = "Options to change way chests look.")
    public ImprovedMenus improvedMenus = new ImprovedMenus();

    @Expose
    @Category(name = "Misc", desc = "Not specific.")
    public Misc misc = new Misc();

    public static class ImprovedMenus {
        @Expose
        @ConfigOption(
                name = "Enable Improved SB Menus",
                desc = "Change the way that skyblock menus (eg. /sbmenu) look"
        )
        @ConfigEditorBoolean
        public boolean enableSbMenus = true;

        @Expose
        @ConfigOption(
                name = "Menu Background Style",
                desc = "Change the style of the background of skyblock menus"
        )
        @ConfigEditorDropdown(
                values = {
                        "Dark 1", "Dark 2", "Transparent", "Light 1", "Light 2", "Light 3",
                        "Unused 1", "Unused 2", "Unused 3", "Unused 4"
                }
        )
        public int backgroundStyle = 0;

        @Expose
        @ConfigOption(
                name = "Button Background Style",
                desc = "Change the style of the foreground elements in skyblock menus"
        )
        @ConfigEditorDropdown(
                values = {
                        "Dark 1", "Dark 2", "Transparent", "Light 1", "Light 2", "Light 3",
                        "Unused 1", "Unused 2", "Unused 3", "Unused 4"
                }
        )
        public int buttonStyle = 0;
    }

    public static class Misc {
        @Expose
        @ConfigOption(
                name = "Chroma Text Speed",
                desc = "Change the speed of chroma text for items names (/neucustomize) and enchant colours (/neuec) with the chroma colour code (&z)"
        )
        @ConfigEditorSlider(
                minValue = 10,
                maxValue = 500,
                minStep = 10
        )
        public int chromaSpeed = 100;
    }

    public static class ToolTips {
        @Expose
        @ConfigOption(
                name = "Tooltip Border Colours",
                desc = "Make the borders of tooltips match the rarity of the item (Prisons Mod Tooltips Only)"
        )
        @ConfigEditorBoolean
        public boolean tooltipBorderColours = true;

        @Expose
        @ConfigOption(
                name = "Tooltip Border Opacity",
                desc = "Change the opacity of the rarity highlight (Prisons Mod Tooltips Only)"
        )
        @ConfigEditorSlider(
                minValue = 0f,
                maxValue = 255f,
                minStep = 1f
        )
        public int tooltipBorderOpacity = 200;

        @Expose
        @ConfigOption(
                name = "Scrollable Tooltips",
                desc = "Support for scrolling tooltips for users with small monitors\n" +
                        "This will prevent the menu from scrolling while holding the key, allowing you to scroll tooltips"
        )
        @ConfigEditorKeybind(defaultKey = 0)
        public int scrollableTooltips = 0;
    }

    public static class StorageGUI {
        @Expose
        @ConfigOption(
                name = "Enable Storage GUI",
                desc = "Show a custom storage overlay when accessing /storage. " +
                        "Makes switching between pages much easier and also allows for searching through all storages"
        )
        @ConfigEditorBoolean()
        public boolean storageGuiEnabled = true;

        @Expose
        @ConfigOption(
                name = "Fancy Glass Panes",
                desc = "Replace the glass pane textures in your storage containers with a fancy connected texture"
        )
        @ConfigEditorDropdown(
                values = {"On", "Locked", "Off"}
        )
        public int fancyPanes = 0;

        @Expose
        @ConfigOption(
                name = "Storage Style",
                desc = "Change the visual style of the overlay"
        )
        @ConfigEditorDropdown(
                values = {"Transparent", "Minecraft", "Grey", "Custom"}
        )
        public int displayStyle = 0;

        @Expose
        @ConfigOption(
                name = "Storage Height",
                desc = "Change the height of the storage preview section. Increasing this allows more storages to be seen at once"
        )
        @ConfigEditorSlider(
                minValue = 104,
                maxValue = 312,
                minStep = 26
        )
        public int storageHeight = 208;

        @Expose
        @ConfigOption(
                name = "Search Bar Autofocus",
                desc = "Automatically focus the search bar when pressing keys"
        )
        @ConfigEditorBoolean
        public boolean searchBarAutofocus = true;

        @Expose
        @ConfigOption(
                name = "Compact Vertically",
                desc = "Remove the space between backpacks when there is a size discrepancy"
        )
        @ConfigEditorBoolean
        public boolean compactVertically = false;

        @Expose
        @ConfigOption(
                name = "Selected Storage Colour",
                desc = "Change the colour used to draw the selected storage border"
        )
        @ConfigEditorColour
        public String selectedStorageColour = "0:255:255:223:0";

//        public int cancelScrollKey = 0;
//
//        public boolean showEnchantGlint = true;
    }

    @Override
    public void executeRunnable(String runnableId) {

    }

    @Override
    public String getHeaderText() {
        return "Prisons v1.0.0";
    }

    @Override
    public void save() {

    }

    @Override
    public Badge[] getBadges() {
        return new Badge[] { new Badge(GuiTextures.GITHUB, "https://github.com/u9g") };
    }
}
