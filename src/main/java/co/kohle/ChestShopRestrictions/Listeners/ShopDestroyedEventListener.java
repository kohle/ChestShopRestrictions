package co.kohle.ChestShopRestrictions.Listeners;

import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;

/**
 * Listens for shop destruction events and removes them from the tracker.
 */
public final class ShopDestroyedEventListener implements Listener {

    private final ChestShopRestrictions plugin;

    public ShopDestroyedEventListener(ChestShopRestrictions plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopDestroyed(ShopDestroyedEvent event) {
        Location signLocation = event.getSign().getLocation();
        plugin.getShopTracker().removeShop(signLocation);
        plugin.getLogger().fine("Shop destroyed at " + signLocation);
    }
}




