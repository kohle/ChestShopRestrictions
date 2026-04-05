package co.kohle.ChestShopRestrictions.Storage;

import co.kohle.ChestShopRestrictions.ChestShopRestrictions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.UUID;

/**
 * Tracks ChestShops by their sign location using SQLite or MySQL.
 * Write operations are performed asynchronously to avoid blocking the main thread.
 */
public final class ShopTracker {

    private final ChestShopRestrictions plugin;
    private Database database;

    public ShopTracker(ChestShopRestrictions plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the database connection based on configuration.
     *
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        String type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

        try {
            if (type.equals("mysql")) {
                database = createMySQLDatabase();
            } else {
                database = createSQLiteDatabase();
            }

            database.initialize();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private SQLiteDatabase createSQLiteDatabase() {
        String fileName = plugin.getConfig().getString("database.sqlite.file", "shops.db");
        String tablePrefix = plugin.getConfig().getString("database.table-prefix", "csr_");
        File dbFile = new File(plugin.getDataFolder(), fileName);

        return new SQLiteDatabase(dbFile, tablePrefix, plugin.getLogger());
    }

    private MySQLDatabase createMySQLDatabase() {
        ConfigurationSection mysql = plugin.getConfig().getConfigurationSection("database.mysql");

        String host = mysql != null ? mysql.getString("host", "localhost") : "localhost";
        int port = mysql != null ? mysql.getInt("port", 3306) : 3306;
        String dbName = mysql != null ? mysql.getString("database", "minecraft") : "minecraft";
        String username = mysql != null ? mysql.getString("username", "root") : "root";
        String password = mysql != null ? mysql.getString("password", "") : "";
        String tablePrefix = plugin.getConfig().getString("database.table-prefix", "csr_");
        boolean useSSL = mysql != null && mysql.getBoolean("use-ssl", false);

        return new MySQLDatabase(host, port, dbName, username, password, tablePrefix, useSSL, plugin.getLogger());
    }

    /**
     * Shuts down the database connection pool.
     */
    public void shutdown() {
        if (database != null) {
            database.shutdown();
        }
    }

    /**
     * Returns the current shop count for the given player.
     * This is a synchronous read operation.
     */
    public int getShopCount(UUID playerUuid) {
        if (database == null) return 0;
        return database.getShopCount(playerUuid);
    }

    /**
     * Checks if a shop is already tracked at the given location.
     */
    public boolean shopExists(Location location) {
        if (database == null || location == null || location.getWorld() == null) return false;
        return database.shopExists(location);
    }

    /**
     * Adds a shop to the tracker asynchronously.
     *
     * @param playerUuid the shop owner's UUID
     * @param location   the sign location
     */
    public void addShop(UUID playerUuid, Location location) {
        if (database == null || location == null || location.getWorld() == null) return;

        // Clone location data for async use
        final String world = location.getWorld().getName();
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location loc = new Location(Bukkit.getWorld(world), x, y, z);
            boolean added = database.addShop(playerUuid, loc);
            if (added) {
                plugin.getLogger().fine("Shop added for " + playerUuid + " at " + world + " " + x + "," + y + "," + z);
            }
        });
    }

    /**
     * Removes a shop from the tracker asynchronously.
     *
     * @param location the sign location
     */
    public void removeShop(Location location) {
        if (database == null || location == null || location.getWorld() == null) return;

        // Clone location data for async use
        final String world = location.getWorld().getName();
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location loc = new Location(Bukkit.getWorld(world), x, y, z);
            boolean removed = database.removeShop(loc);
            if (removed) {
                plugin.getLogger().fine("Shop removed at " + world + " " + x + "," + y + "," + z);
            }
        });
    }

    /**
     * Tracks a shop if it's not already in the database (for discovering existing shops).
     * This is useful when listening to transaction events.
     *
     * @param playerUuid the shop owner's UUID
     * @param location   the sign location
     */
    public void trackShopIfNew(UUID playerUuid, Location location) {
        if (database == null || location == null || location.getWorld() == null) return;

        // Clone location data for async use
        final String world = location.getWorld().getName();
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location loc = new Location(Bukkit.getWorld(world), x, y, z);
            // addShop already handles "insert if not exists"
            boolean added = database.addShop(playerUuid, loc);
            if (added) {
                plugin.getLogger().info("Discovered existing shop for " + playerUuid + " at " + world + " " + x + "," + y + "," + z);
            }
        });
    }
}
