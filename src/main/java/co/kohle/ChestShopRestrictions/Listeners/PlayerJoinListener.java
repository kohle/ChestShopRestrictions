package co.kohle.ChestShopRestrictions.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;
import co.kohle.ChestShopRestrictions.Permissions.Permission;

import java.util.OptionalInt;

/**
 * Listens for player join events to notify players who are over their shop limit.
 */
public final class PlayerJoinListener implements Listener {

    private final ChestShopRestrictions plugin;

    public PlayerJoinListener(ChestShopRestrictions plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Only check if blocking is enabled and max shops feature is enabled
        if (!plugin.isBlockTransactionsOverLimit() || !plugin.isMaxShopsEnabled()) return;

        Player player = event.getPlayer();

        // Check if player has bypass permission
        if (Permission.MAX_SHOPS_BYPASS.has(player)) return;

        // Get their max shop limit
        OptionalInt maxShops = Permission.getMaxShops(player);
        if (maxShops.isEmpty()) return; // No limit set

        // Check their current shop count
        int currentCount = plugin.getShopTracker().getShopCount(player.getUniqueId());

        if (currentCount > maxShops.getAsInt()) {
            // Player is over their limit - notify them (delayed slightly so they see it)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(plugin.formatOverLimitLoginMessage(currentCount, maxShops.getAsInt()));
                }
            }, 40L); // 2 second delay
        }
    }
}

