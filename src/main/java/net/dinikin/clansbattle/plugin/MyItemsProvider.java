package net.dinikin.clansbattle.plugin;

import net.brcdev.shopgui.provider.item.ItemProvider;
import net.brcdev.shopgui.util.ItemUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/*
    Actually this class is not for this plugin and should be moved to separate one.
 */
public class MyItemsProvider extends ItemProvider {

    private final Set<ItemStack> itemStackSet = new HashSet<>();


    public MyItemsProvider() {
        // You must pass the name of your item provider in the superclass constructor
        super("MythicMobs");
        this.ready = false;
    }

    public boolean isValidItem(ItemStack itemStack) {
        return itemStackSet.contains(itemStack) || itemStackSet.stream().anyMatch(i -> compare(i, itemStack));
    }


    public ItemStack loadItem(ConfigurationSection section) {
        String config = section.getString("mythicMobItem");
        ItemStack itemStack = null;
        if (config != null) {
            itemStack = ItemUtils.loadItemStackFromConfig(section, "mythicMobItem");
            itemStackSet.add(itemStack);
        }
        return itemStack;
    }

    public boolean compare(ItemStack stack1, ItemStack stack2) {
        ItemMeta itemMeta1 = stack1.getItemMeta();
        ItemMeta itemMeta2 = stack2.getItemMeta();
        if (itemMeta1 != null && itemMeta2 != null) {
            if (itemMeta1.getDisplayName() == null || itemMeta2.getDisplayName() == null) {
                return false;
            }
            if (itemMeta1.getDisplayName().equals(itemMeta2.getDisplayName())) {
                if (itemMeta1.getLore() != null && itemMeta2.getLore() != null) {
                    List<String> list1 = itemMeta1.getLore();
                    List<String> list2 = itemMeta2.getLore();
                    Collections.sort(list1);
                    Collections.sort(list2);
                    return list1.equals(list2);
                }
                return true;
            }
        }
        return false;
    }

}