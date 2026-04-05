package co.kohle.ChestShopRestrictions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import co.kohle.ChestShopRestrictions.Commands.ChestShopRestrictionsCommand;
import co.kohle.ChestShopRestrictions.Listeners.PlayerJoinListener;
import co.kohle.ChestShopRestrictions.Listeners.PreShopCreationEventListener;
import co.kohle.ChestShopRestrictions.Listeners.ShopCreatedEventListener;
import co.kohle.ChestShopRestrictions.Listeners.ShopDestroyedEventListener;
import co.kohle.ChestShopRestrictions.Listeners.TransactionEventListener;
import co.kohle.ChestShopRestrictions.Storage.ShopTracker;

import java.math.BigDecimal;

public final class ChestShopRestrictions extends JavaPlugin {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // Vault economy hook (nullable)
    private Economy economy;

    // Shop count tracker
    private ShopTracker shopTracker;

    // Configuration values
    private BigDecimal minBuy;
    private BigDecimal minSell;
    private boolean wholeNumbersOnly;
    private boolean maxShopsEnabled;
    private boolean blockTransactionsOverLimit;

    private String msgMinBuy;
    private String msgMinSell;
    private String msgWholeNumbersOnly;
    private String msgMaxShops;
    private String msgCount;
    private String msgTransactionBlocked;
    private String msgOverLimitLogin;
    private String msgLimits;

    @Override
    public void onEnable() {
        // Save default config if not present, then load settings
        getLogger().info("Loading configuration...");

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        setupEconomy();
        loadSettings();

        // Initialize database
        getLogger().info("Initializing database...");
        shopTracker = new ShopTracker(this);
        if (!shopTracker.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register events and listeners
        getLogger().info("Registering events...");

        getServer().getPluginManager().registerEvents(new PreShopCreationEventListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopCreatedEventListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopDestroyedEventListener(this), this);
        getServer().getPluginManager().registerEvents(new TransactionEventListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Register commands
        getLogger().info("Registering commands...");

        ChestShopRestrictionsCommand csrCommand = new ChestShopRestrictionsCommand(this);
        PluginCommand command = getCommand("chestshoprestrictions");
        if (command != null) {
            command.setExecutor(csrCommand);
            command.setTabCompleter(csrCommand);
        }

        // Initiate bStats and register custom metrics
        getLogger().info("Initializing bStats metrics...");

        int pluginId = 29990;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new SimplePie("wholeNumbersOnly", () -> {
            return getConfig().getBoolean("whole-numbers-only", true) ? "Yes" : "No";
        }));

        metrics.addCustomChart(new SimplePie("maxShopsEnabled", () -> {
            return getConfig().getBoolean("max-shops-enabled", false) ? "Yes" : "No";
        }));

        metrics.addCustomChart(new SimplePie("databaseType", () -> {
            return getConfig().getString("database.type", "sqlite");
        }));

        getLogger().info("ChestShopRestrictions enabled!");
    }

    @Override
    public void onDisable() {
        // Shutdown database connection pool
        if (shopTracker != null) {
            getLogger().info("Closing database connection...");
            shopTracker.shutdown();
        }
        getLogger().info("ChestShopRestrictions disabled.");
    }

    /**
     * Attempts to hook into the Vault economy provider.
     */
    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found — <currency> placeholder will use fallback values.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            getLogger().info("Hooked into Vault economy: " + economy.getName());
        } else {
            getLogger().warning("Vault found but no economy provider registered — <currency> placeholder will use fallback values.");
        }
    }

    public void loadSettings() {
        minBuy = readDecimal("minimum-prices.buy", "1");
        minSell = readDecimal("minimum-prices.sell", "1");
        wholeNumbersOnly = getConfig().getBoolean("whole-numbers-only", true);
        maxShopsEnabled = getConfig().getBoolean("max-shops-enabled", false);
        blockTransactionsOverLimit = getConfig().getBoolean("block-transactions-over-limit", false);

        msgMinBuy = getConfig().getString(
                "messages.minimum-prices.buy",
                "<red>The minimum price for selling items is <price> <currency>."
        );

        msgMinSell = getConfig().getString(
                "messages.minimum-prices.sell",
                "<red>The minimum price for buying items is <price> <currency>."
        );

        msgWholeNumbersOnly = getConfig().getString(
                "messages.whole-numbers-only",
                "<red>The price must be a whole number."
        );

        msgMaxShops = getConfig().getString(
                "messages.max-shops",
                "<red>You can only create up to <max> shops."
        );

        msgCount = getConfig().getString(
                "messages.count",
                "<gold><player>'s shop count: <yellow><count>/<max>"
        );

        msgTransactionBlocked = getConfig().getString(
                "messages.transaction-blocked",
                "<red>This shop is currently disabled because the owner has too many shops."
        );

        msgOverLimitLogin = getConfig().getString(
                "messages.over-limit-login",
                "<red>You have <count> shops but are only allowed <max>. Your shops are disabled until you remove some."
        );

        msgLimits = getConfig().getString(
                "messages.limits",
                "<gold>ChestShopRestrictions Limits:\n<yellow>Min Buy Price: <min_buy> <currency_buy>\n<yellow>Min Sell Price: <min_sell> <currency_sell>\n<yellow>Whole Numbers Only: <whole_numbers>"
        );

        getLogger().info("Min buy: " + minBuy + ", min sell: " + minSell +
                ", whole numbers only: " + wholeNumbersOnly);
    }

    private BigDecimal readDecimal(String path, String fallback) {
        String s = getConfig().getString(path, fallback);
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            getLogger().warning("Invalid number at " + path + " (" + s + "), using " + fallback);
            return new BigDecimal(fallback);
        }
    }

    /**
     * Returns the appropriate currency name (singular or plural) for the given amount.
     */
    public String getCurrencyName(BigDecimal amount) {
        if (economy == null) {
            return amount.compareTo(BigDecimal.ONE) == 0 ? "dollar" : "dollars";
        }
        return amount.compareTo(BigDecimal.ONE) == 0
                ? economy.currencyNameSingular()
                : economy.currencyNamePlural();
    }

    /**
     * Deserializes a MiniMessage string into a {@link Component},
     * replacing {@code <price>} with the given price value and
     * {@code <currency>} with the appropriate currency name.
     */
    public Component formatMessage(String message, BigDecimal price) {
        if (message == null || message.isEmpty()) return Component.empty();
        String replaced = message
                .replace("<price>", price.toPlainString())
                .replace("<currency>", getCurrencyName(price));
        return MINI_MESSAGE.deserialize(replaced);
    }

    /**
     * Deserializes a MiniMessage string into a {@link Component}.
     */
    public Component formatMessage(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(message);
    }

    public BigDecimal getMinBuy() { return minBuy; }
    public BigDecimal getMinSell() { return minSell; }
    public boolean isWholeNumbersOnly() { return wholeNumbersOnly; }
    public boolean isMaxShopsEnabled() { return maxShopsEnabled; }
    public boolean isBlockTransactionsOverLimit() { return blockTransactionsOverLimit; }

    public String getMsgMinBuy() { return msgMinBuy; }
    public String getMsgMinSell() { return msgMinSell; }
    public String getMsgWholeNumbersOnly() { return msgWholeNumbersOnly; }
    public String getMsgMaxShops() { return msgMaxShops; }
    public String getMsgLimits() { return msgLimits; }
    public String getMsgTransactionBlocked() { return msgTransactionBlocked; }

    public ShopTracker getShopTracker() { return shopTracker; }

    /**
     * Formats the over-limit login message, replacing placeholders.
     *
     * @param count current shop count
     * @param max   max allowed shops
     */
    public Component formatOverLimitLoginMessage(int count, int max) {
        if (msgOverLimitLogin == null || msgOverLimitLogin.isEmpty()) return Component.empty();
        String replaced = msgOverLimitLogin
                .replace("<count>", String.valueOf(count))
                .replace("<max>", String.valueOf(max));
        return MINI_MESSAGE.deserialize(replaced);
    }

    /**
     * Formats the max-shops message, replacing {@code <max>} with the limit.
     */
    public Component formatMaxShopsMessage(int max) {
        if (msgMaxShops == null || msgMaxShops.isEmpty()) return Component.empty();
        String replaced = msgMaxShops.replace("<max>", String.valueOf(max));
        return MINI_MESSAGE.deserialize(replaced);
    }

    /**
     * Formats the count message, replacing placeholders with player info.
     *
     * @param playerName the target player's name
     * @param count      the current shop count
     * @param max        the max shop limit (or "∞" if unlimited)
     */
    public Component formatCountMessage(String playerName, int count, String max) {
        if (msgCount == null || msgCount.isEmpty()) return Component.empty();
        String replaced = msgCount
                .replace("<player>", playerName)
                .replace("<count>", String.valueOf(count))
                .replace("<max>", max);
        return MINI_MESSAGE.deserialize(replaced);
    }

    /**
     * Formats the limits message, replacing placeholders with current config values.
     * Handles multiline strings (from YAML block scalars) by deserializing each line
     * and joining them with newline components.
     */
    public Component formatLimitsMessage() {
        if (msgLimits == null || msgLimits.isEmpty()) return Component.empty();
        String replaced = msgLimits.trim()
                .replace("<min_buy>", minBuy.toPlainString())
                .replace("<min_sell>", minSell.toPlainString())
                .replace("<currency_buy>", getCurrencyName(minBuy))
                .replace("<currency_sell>", getCurrencyName(minSell))
                .replace("<whole_numbers>", wholeNumbersOnly ? "Yes" : "No");
        String[] lines = replaced.split("\n");
        Component result = MINI_MESSAGE.deserialize(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            result = result.append(Component.newline()).append(MINI_MESSAGE.deserialize(lines[i]));
        }
        return result;
    }
}