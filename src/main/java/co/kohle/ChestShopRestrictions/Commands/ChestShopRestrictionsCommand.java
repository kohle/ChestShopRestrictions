package co.kohle.ChestShopRestrictions.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;
import co.kohle.ChestShopRestrictions.Permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public final class ChestShopRestrictionsCommand implements CommandExecutor, TabCompleter {

    private final ChestShopRestrictions plugin;

    public ChestShopRestrictionsCommand(ChestShopRestrictions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            // Info subcommand
            if (!Permission.INFO.has(sender)) {
                sender.sendMessage(Component.text("You do not have permission to do that.", NamedTextColor.RED));
                return true;
            }

            String name = plugin.getPluginMeta().getName();
            String version = plugin.getPluginMeta().getVersion();
            sender.sendMessage(Component.text(name + " v" + version, NamedTextColor.GOLD));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!Permission.RELOAD.has(sender)) {
                sender.sendMessage(Component.text("You do not have permission to do that.", NamedTextColor.RED));
                return true;
            }

            plugin.reloadConfig();
            plugin.loadSettings();
            sender.sendMessage(Component.text("ChestShopRestrictions configuration reloaded.", NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("limits")) {
            if (!Permission.SHOW_LIMITS.has(sender)) {
                sender.sendMessage(Component.text("You do not have permission to do that.", NamedTextColor.RED));
                return true;
            }

            sender.sendMessage(plugin.formatLimitsMessage());
            return true;
        }

        if (args[0].equalsIgnoreCase("count")) {
            if (!Permission.COUNT.has(sender)) {
                sender.sendMessage(Component.text("You do not have permission to do that.", NamedTextColor.RED));
                return true;
            }

            OfflinePlayer targetPlayer;
            Player onlineTarget = null;

            if (args.length >= 2) {
                // First try to find online player
                onlineTarget = Bukkit.getPlayer(args[1]);
                if (onlineTarget != null) {
                    targetPlayer = onlineTarget;
                } else {
                    // Try to find offline player by name
                    @SuppressWarnings("deprecation")
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                    if (!offline.hasPlayedBefore() && !offline.isOnline()) {
                        sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                        return true;
                    }
                    targetPlayer = offline;
                }
            } else {
                // Default to sender if they are a player
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("Usage: /csr count <player>", NamedTextColor.RED));
                    return true;
                }
                onlineTarget = (Player) sender;
                targetPlayer = onlineTarget;
            }

            int currentCount = plugin.getShopTracker().getShopCount(targetPlayer.getUniqueId());

            // Can only get max shops permission if player is online
            String maxDisplay;
            if (onlineTarget != null) {
                OptionalInt maxShops = Permission.getMaxShops(onlineTarget);
                maxDisplay = maxShops.isPresent() ? String.valueOf(maxShops.getAsInt()) : "∞";
            } else {
                maxDisplay = "?"; // Can't check permissions for offline players
            }

            String playerName = targetPlayer.getName() != null ? targetPlayer.getName() : args[1];
            sender.sendMessage(plugin.formatCountMessage(playerName, currentCount, maxDisplay));
            return true;
        }

        sender.sendMessage(Component.text("Unknown subcommand. Usage: /csr [reload|limits|count]", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new java.util.ArrayList<>();
            if (Permission.RELOAD.has(sender) && "reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if (Permission.SHOW_LIMITS.has(sender) && "limits".startsWith(args[0].toLowerCase())) {
                completions.add("limits");
            }
            if (Permission.COUNT.has(sender) && "count".startsWith(args[0].toLowerCase())) {
                completions.add("count");
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("count") && Permission.COUNT.has(sender)) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}

