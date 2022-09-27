package net.fabricmc.example;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class ExtraUtils {
    /**
     * Checks whether an itemstack matches a certain query, following the same rules implemented by the more complex
     * map-based search function.
     */
    public static boolean doesStackMatchSearch(ItemStack stack, String query) {
        if (query.startsWith("title:")) {
            query = query.substring(6);
            return searchString(stack.getDisplayName(), query);
        } else if (query.startsWith("desc:")) {
            query = query.substring(5);
            String lore = "";
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null) {
                NBTTagCompound display = tag.getCompoundTag("display");
                if (display.hasKey("Lore", 9)) {
                    NBTTagList list = display.getTagList("Lore", 8);
                    for (int i = 0; i < list.tagCount(); i++) {
                        lore += list.getStringTagAt(i) + " ";
                    }
                }
            }
            return searchString(lore, query);
        } /*else if (query.startsWith("id:")) {
            query = query.substring(3);
            String internalName = getInternalNameForItem(stack);
            return query.equalsIgnoreCase(internalName);
        }*/ else {
            boolean result = false;
            if (!query.trim().contains(" ")) {
                StringBuilder sb = new StringBuilder();
                for (char c : query.toCharArray()) {
                    sb.append(c).append(" ");
                }
                result = result || searchString(stack.getDisplayName(), sb.toString());
            }
            result = result || searchString(stack.getDisplayName(), query);

            String lore = "";
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null) {
                NBTTagCompound display = tag.getCompoundTag("display");
                if (display.hasKey("Lore", 9)) {
                    NBTTagList list = display.getTagList("Lore", 8);
                    for (int i = 0; i < list.tagCount(); i++) {
                        lore += list.getStringTagAt(i) + " ";
                    }
                }
            }

            result = result || searchString(lore, query);

            return result;
        }
    }

    /**
     * Searches a string for a query. This method is used to mimic the behaviour of the more complex map-based search
     * function. This method is used for the chest-item-search feature.
     */
    public static boolean searchString(String toSearch, String query) {
        int lastMatch = -1;

        toSearch = clean(toSearch).toLowerCase();
        query = clean(query).toLowerCase();
        String[] splitToSeach = toSearch.split(" ");
        out:
        for (String s : query.split(" ")) {
            for (int i = 0; i < splitToSeach.length; i++) {
                if (!(lastMatch == -1 || lastMatch == i - 1)) continue;
                if (splitToSeach[i].startsWith(s)) {
                    lastMatch = i;
                    continue out;
                }
            }
            return false;
        }

        return true;
    }

    private static String clean(String str) {
        return str.replaceAll("(\u00a7.)|[^0-9a-zA-Z ]", "").toLowerCase().trim();
    }
}
