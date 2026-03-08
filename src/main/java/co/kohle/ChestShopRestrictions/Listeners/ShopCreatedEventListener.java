package co.kohle.ChestShopRestrictions.Listeners;

import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;

import java.util.UUID;

/**
 * Listens for successfully created shops and increments the owner's shop count.
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

        plugin.getShopTracker().incrementShopCount(ownerUuid);
        plugin.getLogger().fine("Shop created by " + ownerUuid + ". New count: "
                + plugin.getShopTracker().getShopCount(ownerUuid));
    }
}


