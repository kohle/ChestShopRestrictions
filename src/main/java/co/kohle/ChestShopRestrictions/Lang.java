package co.kohle.ChestShopRestrictions;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Manages the language system for ChestShopRestrictions.
 * Language files are stored in the {@code lang/} folder inside the plugin's data folder.
 * The active language is determined by the {@code language} setting in {@code config.yml}.
 */
public final class Lang {

    private final ChestShopRestrictions plugin;

    // Cached message strings
    private String msgMinBuy;
    private String msgMinSell;
    private String msgWholeNumbersOnly;
    private String msgMaxShops;
    private String msgCount;
    private String msgTransactionBlocked;
    private String msgOverLimitLogin;
    private String msgLimits;

    public Lang(ChestShopRestrictions plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or reloads) the language file specified in config.yml.
     * If the file does not exist on disk, the bundled default will be saved.
     */
    public void load() {
        String language = plugin.getConfig().getString("language", "en_us");
        File langFolder = new File(plugin.getDataFolder(), "lang");

        if (!langFolder.exists() && !langFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create lang/ folder.");
        }

        // Always save the bundled default language files if they don't exist on disk yet
        saveDefaultLangFile("en_us");
        saveDefaultLangFile("es_es");

        File langFile = new File(langFolder, language + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file '" + language + ".yml' not found in lang/ folder. Falling back to en_us.");
            language = "en_us";
            langFile = new File(langFolder, language + ".yml");
        }

        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Merge any missing keys from the bundled default so new messages are always present
        InputStream defaultStream = plugin.getResource("lang/en_us.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaults);
        }

        // Cache messages
        msgMinBuy = langConfig.getString("minimum-prices.buy",
                "<red>The minimum buy (B) price is <price> <currency>.");
        msgMinSell = langConfig.getString("minimum-prices.sell",
                "<red>The minimum sell (S) price is <price> <currency>.");
        msgWholeNumbersOnly = langConfig.getString("whole-numbers-only",
                "<red>The price must be a whole number.");
        msgMaxShops = langConfig.getString("max-shops",
                "<red>You can only create up to <max> shops.");
        msgCount = langConfig.getString("count",
                "<gold><player>'s shop count: <yellow><count>/<max>");
        msgTransactionBlocked = langConfig.getString("transaction-blocked",
                "<red>This shop is currently disabled because the owner has too many shops.");
        msgOverLimitLogin = langConfig.getString("over-limit-login",
                "<red>You have <count> shops but are only allowed <max>. Your shops are disabled until you remove some.");
        msgLimits = langConfig.getString("limits",
                "<gold>ChestShopRestrictions Limits:\n<yellow>Min Buy (B) Price: <min_buy> <currency_buy>\n<yellow>Min Sell (S) Price: <min_sell> <currency_sell>\n<yellow>Whole Numbers Only: <whole_numbers>");

        plugin.getLogger().info("Loaded language: " + language);
    }

    /**
     * Saves the bundled default language file to the lang/ folder if it doesn't already exist.
     */
    private void saveDefaultLangFile(String name) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        File outFile = new File(langFolder, name + ".yml");

        if (!outFile.exists()) {
            InputStream in = plugin.getResource("lang/" + name + ".yml");
            if (in != null) {
                try {
                    java.nio.file.Files.copy(in, outFile.toPath());
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to save default language file: " + name + ".yml", e);
                }
            }
        }
    }

    // --- Getters ---

    public String getMsgMinBuy() { return msgMinBuy; }
    public String getMsgMinSell() { return msgMinSell; }
    public String getMsgWholeNumbersOnly() { return msgWholeNumbersOnly; }
    public String getMsgMaxShops() { return msgMaxShops; }
    public String getMsgCount() { return msgCount; }
    public String getMsgTransactionBlocked() { return msgTransactionBlocked; }
    public String getMsgOverLimitLogin() { return msgOverLimitLogin; }
    public String getMsgLimits() { return msgLimits; }
}



