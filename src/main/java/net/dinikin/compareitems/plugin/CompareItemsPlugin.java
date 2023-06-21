package net.dinikin.compareitems.plugin;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;


public class CompareItemsPlugin extends JavaPlugin implements Listener {

    private ShopGUIPlusHook shopGUIPlusHook;

    private void hookIntoShopGui() {
        if (Bukkit.getPluginManager().getPlugin("ShopGUIPlus") != null) {
            this.shopGUIPlusHook = new ShopGUIPlusHook(this);
            Bukkit.getPluginManager().registerEvents(shopGUIPlusHook, this);

            this.getLogger().info("ShopGUI+ detected.");
        } else {
            this.getLogger().warning("ShopGUI+ not found.");
        }
    }

    @Override
    public void onEnable() {
        hookIntoShopGui();
    }


}
