package co.kohle.ChestShopRestrictions.Storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * MySQL implementation of the Database interface using HikariCP.
 * Tracks individual shops by their sign location.
 */
public final class MySQLDatabase implements Database {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String tablePrefix;
    private final boolean useSSL;
    private final Logger logger;
    private HikariDataSource dataSource;

    public MySQLDatabase(String host, int port, String database, String username,
                         String password, String tablePrefix, boolean useSSL, Logger logger) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.tablePrefix = tablePrefix;
        this.useSSL = useSSL;
        this.logger = logger;
    }

    @Override
    public void initialize() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Sensible defaults for connection pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000); // 5 minutes
        config.setConnectionTimeout(30000); // 30 seconds
        config.setMaxLifetime(1800000); // 30 minutes
        config.setPoolName("ChestShopRestrictions-MySQL");

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);

        // Create table if not exists
        createTable();

        logger.info("MySQL database initialized: " + host + ":" + port + "/" + database);
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "shops (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "world VARCHAR(64) NOT NULL, " +
                "x INT NOT NULL, " +
                "y INT NOT NULL, " +
                "z INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_location (world, x, y, z), " +
                "INDEX idx_owner (owner_uuid)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("MySQL database connection closed.");
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
        String sql = "INSERT IGNORE INTO " + tablePrefix + "shops (owner_uuid, world, x, y, z) VALUES (?, ?, ?, ?, ?)";

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
