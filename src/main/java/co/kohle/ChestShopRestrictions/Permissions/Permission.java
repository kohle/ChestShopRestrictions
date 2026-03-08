package co.kohle.ChestShopRestrictions.Permissions;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.OptionalInt;

public enum Permission {

    INFO("chestshoprestrictions.info"),
    RELOAD("chestshoprestrictions.reload"),
    SHOW_LIMITS("chestshoprestrictions.showlimits"),
    MIN_SELL_BYPASS("chestshoprestrictions.min.sell.bypass"),
    MIN_BUY_BYPASS("chestshoprestrictions.min.buy.bypass"),
    WHOLE_NUMBER_BYPASS("chestshoprestrictions.wholenumber.bypass"),
    MAX_SHOPS_BYPASS("chestshoprestrictions.maxshops.bypass");

    private final String node;

    Permission(String node) {
        this.node = node;
    }

    public String getNode() {
        return node;
    }

    public boolean has(CommandSender sender) {
        return sender.hasPermission(node);
    }

    private static final String MAX_SHOPS_PREFIX = "chestshoprestrictions.maxshops.";

    /**
     * Scans the player's effective permissions for nodes matching
     * {@code chestshoprestrictions.maxshops.<number>} and returns the highest value found.
     *
     * @return the highest max-shops limit, or empty if the player has no such permission
     */
    public static OptionalInt getMaxShops(Player player) {
        int max = -1;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue; // only consider granted (true) permissions
            String perm = info.getPermission();
            if (perm.startsWith(MAX_SHOPS_PREFIX)) {
                String suffix = perm.substring(MAX_SHOPS_PREFIX.length());
                // Skip the "bypass" node
                if (suffix.equalsIgnoreCase("bypass")) continue;
                try {
                    int value = Integer.parseInt(suffix);
                    if (value > max) {
                        max = value;
                    }
                } catch (NumberFormatException ignored) {
                    // Not a numeric suffix — skip
                }
            }
        }
        return max >= 0 ? OptionalInt.of(max) : OptionalInt.empty();
    }
}
