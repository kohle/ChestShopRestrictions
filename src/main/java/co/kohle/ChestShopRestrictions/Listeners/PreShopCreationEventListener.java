package co.kohle.ChestShopRestrictions.Listeners;

import com.Acrobot.ChestShop.Events.PreShopCreationEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;
import co.kohle.ChestShopRestrictions.Permissions.Permission;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PreShopCreationEventListener implements Listener {

    private final ChestShopRestrictions plugin;

    // Matches: "B 10", "B10", "b 10.5", "10 B", "10B", "10.5 b"
    private static final Pattern BUY_PATTERN  =
            Pattern.compile("(?i)(?:\\bB\\s*([0-9]+(?:\\.[0-9]+)?)\\b|\\b([0-9]+(?:\\.[0-9]+)?)\\s*B\\b)");

    // Matches: "S 5", "S5", "s 0.99", "5 S", "5S", "0.99 s"
    private static final Pattern SELL_PATTERN =
            Pattern.compile("(?i)(?:\\bS\\s*([0-9]+(?:\\.[0-9]+)?)\\b|\\b([0-9]+(?:\\.[0-9]+)?)\\s*S\\b)");

    public PreShopCreationEventListener(ChestShopRestrictions plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreShopCreation(PreShopCreationEvent event) {
        // --- Max shops limit check ---
        if (plugin.isMaxShopsEnabled() && !Permission.MAX_SHOPS_BYPASS.has(event.getPlayer())) {
            OptionalInt maxShops = Permission.getMaxShops(event.getPlayer());
            if (maxShops.isPresent()) {
                int current = plugin.getShopTracker().getShopCount(event.getPlayer().getUniqueId());
                if (current >= maxShops.getAsInt()) {
                    event.getPlayer().sendMessage(plugin.formatMaxShopsMessage(maxShops.getAsInt()));
                    event.setOutcome(PreShopCreationEvent.CreationOutcome.OTHER_BREAK);
                    return;
                }
            }
        }

        // Price is on sign line 3 (index 2)
        String raw = event.getSignLine((byte) 2);
        String line = (raw == null ? "" : PlainTextComponentSerializer.plainText().serialize(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .deserialize(raw)
        )).trim();

        Optional<BigDecimal> buy  = extract(line, BUY_PATTERN);
        Optional<BigDecimal> sell = extract(line, SELL_PATTERN);

        // If ChestShop parsing fails (we don't recognize the format), let ChestShop decide.
        if (buy.isEmpty() && sell.isEmpty()) return;

        // Whole numbers only: reject if any present price has decimals
        if (plugin.isWholeNumbersOnly() && !Permission.WHOLE_NUMBER_BYPASS.has(event.getPlayer())) {
            if ((buy.isPresent() && hasFraction(buy.get())) || (sell.isPresent() && hasFraction(sell.get()))) {
                event.getPlayer().sendMessage(plugin.formatMessage(plugin.getMsgWholeNumbersOnly()));
                event.setOutcome(PreShopCreationEvent.CreationOutcome.INVALID_PRICE);
                return;
            }
        }

        // Minimum buy
        if (buy.isPresent() && buy.get().compareTo(plugin.getMinBuy()) < 0
                && !Permission.MIN_SELL_BYPASS.has(event.getPlayer())) {
            event.getPlayer().sendMessage(plugin.formatMessage(plugin.getMsgMinBuy(), plugin.getMinBuy()));
            event.setOutcome(PreShopCreationEvent.CreationOutcome.BUY_PRICE_BELOW_MIN);
            return;
        }

        // Minimum sell
        if (sell.isPresent() && sell.get().compareTo(plugin.getMinSell()) < 0
                && !Permission.MIN_BUY_BYPASS.has(event.getPlayer())) {
            event.getPlayer().sendMessage(plugin.formatMessage(plugin.getMsgMinSell(), plugin.getMinSell()));
            event.setOutcome(PreShopCreationEvent.CreationOutcome.SELL_PRICE_BELOW_MIN);
        }
    }

    private static Optional<BigDecimal> extract(String line, Pattern p) {
        Matcher m = p.matcher(line);
        if (!m.find()) return Optional.empty();
        try {
            String value = m.group(1) != null ? m.group(1) : m.group(2);
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static boolean hasFraction(BigDecimal v) {
        // "1.0" becomes scale 0 after stripping zeros; "1.5" stays scale 1
        return v.stripTrailingZeros().scale() > 0;
    }
}