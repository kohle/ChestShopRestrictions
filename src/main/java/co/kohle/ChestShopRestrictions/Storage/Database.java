package co.kohle.ChestShopRestrictions.Storage;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Interface defining database operations for shop tracking.
 * Tracks individual shops by their sign location.
 */
public interface Database {

    /**
     * Initializes the database connection and creates tables if needed.
     *
     * @throws Exception if initialization fails
     */
    void initialize() throws Exception;

    /**
     * Shuts down the database connection pool gracefully.
     */
    void shutdown();

    /**
     * Returns the current shop count for the given player.
     *
     * @param playerUuid the player's UUID
     * @return the shop count
     */
    int getShopCount(UUID playerUuid);

    /**
     * Checks if a shop exists at the given location.
     *
     * @param location the sign location
     * @return true if a shop is tracked at this location
     */
    boolean shopExists(Location location);

    /**
     * Adds a shop to the database if it doesn't already exist.
     *
     * @param playerUuid the shop owner's UUID
     * @param location   the sign location
     * @return true if the shop was added, false if it already existed
     */
    boolean addShop(UUID playerUuid, Location location);

    /**
     * Removes a shop from the database.
     *
     * @param location the sign location
     * @return true if a shop was removed, false if none existed
     */
    boolean removeShop(Location location);

    /**
     * Gets the owner UUID of a shop at the given location.
     *
     * @param location the sign location
     * @return the owner's UUID, or null if no shop exists
     */
    UUID getShopOwner(Location location);
}

