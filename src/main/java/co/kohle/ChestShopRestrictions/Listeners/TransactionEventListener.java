package co.kohle.ChestShopRestrictions.Listeners;

import com.Acrobot.ChestShop.Events.TransactionEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;
import co.kohle.ChestShopRestrictions.Permissions.Permission;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * Listens for shop transactions (buys/sells) to:
 * 1. Discover existing shops that were created before the plugin was installed
 * 2. Block transactions if the shop owner is over their shop limit (when configured)
 */
public final class TransactionEventListener implements Listener {

    private final ChestShopRestrictions plugin;

    public TransactionEventListener(ChestShopRestrictions plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTransactionCheck(TransactionEvent event) {
        // Only check if blocking is enabled and max shops feature is enabled
        if (!plugin.isBlockTransactionsOverLimit() || !plugin.isMaxShopsEnabled()) return;

        UUID ownerUuid = event.getOwnerAccount().getUuid();
        if (ownerUuid == null) return;

        // Check if owner has bypass permission (need to get owner as player if online)
        Player ownerPlayer = Bukkit.getPlayer(ownerUuid);
        if (ownerPlayer != null && Permission.MAX_SHOPS_BYPASS.has(ownerPlayer)) return;

        // Get the owner's max shop limit
        // If owner is offline, we need to check their permissions differently
        // For now, we'll use the online player check or skip if offline
        OptionalInt maxShops;
        if (ownerPlayer != null) {
            maxShops = Permission.getMaxShops(ownerPlayer);
        } else {
            // Owner is offline - we can't easily check permissions
            // Skip the check for offline players (they'll be notified on login)
            return;
        }

        if (maxShops.isEmpty()) return; // No limit set

        int currentCount = plugin.getShopTracker().getShopCount(ownerUuid);
        if (currentCount > maxShops.getAsInt()) {
            // Owner is over their limit - block the transaction
            event.setCancelled(true);
            event.getClient().sendMessage(plugin.formatMessage(plugin.getMsgTransactionBlocked()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransactionTrack(TransactionEvent event) {
        // Get the shop owner's UUID
        UUID ownerUuid = event.getOwnerAccount().getUuid();
        if (ownerUuid == null) return;

        // Get the sign location
        Location signLocation = event.getSign().getLocation();

        // Track this shop if it's not already in the database
        plugin.getShopTracker().trackShopIfNew(ownerUuid, signLocation);
    }
}
