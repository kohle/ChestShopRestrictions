package co.kohle.ChestShopRestrictions.Storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Location;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * SQLite implementation of the Database interface using HikariCP.
 * Tracks individual shops by their sign location.
 */
public final class SQLiteDatabase implements Database {

    private final File databaseFile;
    private final String tablePrefix;
    private final Logger logger;
    private HikariDataSource dataSource;

    public SQLiteDatabase(File databaseFile, String tablePrefix, Logger logger) {
        this.databaseFile = databaseFile;
        this.tablePrefix = tablePrefix;
        this.logger = logger;
    }

    @Override
    public void initialize() throws Exception {
        // Ensure parent directory exists
        if (!databaseFile.getParentFile().exists()) {
            databaseFile.getParentFile().mkdirs();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite only supports one connection
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("ChestShopRestrictions-SQLite");

        dataSource = new HikariDataSource(config);

        // Create table if not exists
        createTable();

        logger.info("SQLite database initialized: " + databaseFile.getName());
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "shops (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "world VARCHAR(64) NOT NULL, " +
                "x INT NOT NULL, " +
                "y INT NOT NULL, " +
                "z INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE(world, x, y, z)" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }

        // Create index on owner_uuid for faster count queries
        String indexSql = "CREATE INDEX IF NOT EXISTS idx_" + tablePrefix + "shops_owner " +
                "ON " + tablePrefix + "shops(owner_uuid)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(indexSql)) {
            stmt.execute();
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("SQLite database connection closed.");
        }
    }

    @Override
    public int getShopCount(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM " + tablePrefix + "shops WHERE owner_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get shop count for " + playerUuid + ": " + e.getMessage());
        }

        return 0;
    }

    @Override
    public boolean shopExists(Location location) {
        String sql = "SELECT 1 FROM " + tablePrefix + "shops WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("Failed to check shop existence: " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean addShop(UUID playerUuid, Location location) {
        String sql = "INSERT OR IGNORE INTO " + tablePrefix + "shops (owner_uuid, world, x, y, z) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, location.getWorld().getName());
            stmt.setInt(3, location.getBlockX());
            stmt.setInt(4, location.getBlockY());
            stmt.setInt(5, location.getBlockZ());

            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            logger.severe("Failed to add shop for " + playerUuid + ": " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean removeShop(Location location) {
        String sql = "DELETE FROM " + tablePrefix + "shops WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());

            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            logger.severe("Failed to remove shop: " + e.getMessage());
        }

        return false;
    }

    @Override
    public UUID getShopOwner(Location location) {
        String sql = "SELECT owner_uuid FROM " + tablePrefix + "shops WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("owner_uuid"));
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get shop owner: " + e.getMessage());
        }

        return null;
    }
}
