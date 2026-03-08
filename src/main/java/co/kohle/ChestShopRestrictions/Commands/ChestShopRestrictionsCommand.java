package co.kohle.ChestShopRestrictions.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;
import co.kohle.ChestShopRestrictions.Permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

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

        sender.sendMessage(Component.text("Unknown subcommand. Usage: /csr [reload|limits]", NamedTextColor.RED));
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
            return completions;
        }
        return Collections.emptyList();
    }
}

