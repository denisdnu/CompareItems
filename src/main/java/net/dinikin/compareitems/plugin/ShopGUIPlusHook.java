package net.dinikin.compareitems.plugin;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.event.ShopGUIPlusPostEnableEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ShopGUIPlusHook implements Listener {

    private CompareItemsPlugin myItemsPlugin;
    private MyItemsProvider myItemsProvider;

    public ShopGUIPlusHook(CompareItemsPlugin myItemsPlugin) {
        this.myItemsPlugin = myItemsPlugin;
    }

    @EventHandler
    public void onShopGUIPlusPostEnable(ShopGUIPlusPostEnableEvent event) {
        this.myItemsProvider = new MyItemsProvider();
        ShopGuiPlusApi.registerItemProvider(myItemsProvider);
        myItemsPlugin.getLogger().info("Registered MyItemsProvider in ShopGUI+!");
    }
}