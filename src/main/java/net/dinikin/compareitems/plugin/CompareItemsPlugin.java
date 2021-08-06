package net.dinikin.compareitems.plugin;

import net.brcdev.shopgui.ShopGuiPlusApi;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;


public class CompareItemsPlugin extends JavaPlugin implements Listener {

    private MyItemsProvider myItemsProvider;

    private void hookIntoShopGui() {
        this.myItemsProvider = new MyItemsProvider();
        ShopGuiPlusApi.registerItemProvider(myItemsProvider);
    }

    public void onEnable() {
        hookIntoShopGui();
    }


}
