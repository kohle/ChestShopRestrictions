package co.kohle.ChestShopRestrictions.Listeners;

import com.Acrobot.ChestShop.Database.Account;
import com.Acrobot.ChestShop.Events.AccountQueryEvent;
import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;

import java.util.UUID;

/**
 * Listens for shop destruction events and decrements the owner's shop count.
 */
public final class ShopDestroyedEventListener implements Listener {

    private final ChestShopRestrictions plugin;

    public ShopDestroyedEventListener(ChestShopRestrictions plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopDestroyed(ShopDestroyedEvent event) {

        // The first line of a ChestShop sign is the owner's name
        String ownerName = PlainTextComponentSerializer.plainText()
                .serialize(event.getSign().line(0));
        if (ownerName.isBlank()) return;

        // Use ChestShop's AccountQueryEvent to resolve the name to an Account (with UUID)
        AccountQueryEvent query = new AccountQueryEvent(ownerName);
        plugin.getServer().getPluginManager().callEvent(query);

        Account account = query.getAccount();
        if (account == null) {
            plugin.getLogger().warning("Could not resolve account for shop owner: " + ownerName);
            return;
        }

        UUID ownerUuid = account.getUuid();
        if (ownerUuid == null) return;

        plugin.getShopTracker().decrementShopCount(ownerUuid);
        plugin.getLogger().fine("Shop destroyed for " + ownerUuid + ". New count: "
                + plugin.getShopTracker().getShopCount(ownerUuid));
    }
}




