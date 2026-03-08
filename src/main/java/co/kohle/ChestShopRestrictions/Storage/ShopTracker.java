package co.kohle.ChestShopRestrictions.Storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Tracks the number of ChestShops each player owns using a data.yml file.
 * Format: <player-uuid>: <shop-count>
 */
public final class ShopTracker {

    private final ChestShopRestrictions plugin;
    private final File dataFile;
    private FileConfiguration data;

    public ShopTracker(ChestShopRestrictions plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    /**
     * Loads (or creates) the data.yml file.
     */
    public void load() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Saves the data.yml file to disk.
     */
    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    /**
     * Returns the current shop count for the given player.
     */
    public int getShopCount(UUID playerUuid) {
        return data.getInt(playerUuid.toString(), 0);
    }

    /**
     * Increments the shop count for the given player by 1 and saves.
     */
    public void incrementShopCount(UUID playerUuid) {
        int current = getShopCount(playerUuid);
        data.set(playerUuid.toString(), current + 1);
        save();
    }

    /**
     * Decrements the shop count for the given player by 1 (minimum 0) and saves.
     */
    public void decrementShopCount(UUID playerUuid) {
        int current = getShopCount(playerUuid);
        int updated = Math.max(0, current - 1);
        if (updated == 0) {
            data.set(playerUuid.toString(), null); // Remove the key entirely
        } else {
            data.set(playerUuid.toString(), updated);
        }
        save();
    }
}

