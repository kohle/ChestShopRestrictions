package co.kohle.ChestShopRestrictions.Listeners;

import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;

import java.util.UUID;

/**
 * Listens for successfully created shops and adds them to the tracker.
 */
public final class ShopCreatedEventListener implements Listener {

    private final ChestShopRestrictions plugin;

    public ShopCreatedEventListener(ChestShopRestrictions plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopCreated(ShopCreatedEvent event) {

        // Only count shops created by their owner (not admin shops created for others)
        if (!event.createdByOwner()) return;

        UUID ownerUuid = event.getOwnerAccount().getUuid();
        if (ownerUuid == null) return;

        Location signLocation = event.getSign().getLocation();
        plugin.getShopTracker().addShop(ownerUuid, signLocation);
        plugin.getLogger().fine("Shop created by " + ownerUuid + " at " + signLocation);
    }
}


