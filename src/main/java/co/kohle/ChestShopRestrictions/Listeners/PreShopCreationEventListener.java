package co.kohle.ChestShopRestrictions.Listeners;

import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Events.PreShopCreationEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import co.kohle.ChestShopRestrictions.ChestShopRestrictions;
import co.kohle.ChestShopRestrictions.Permissions.Permission;

import java.math.BigDecimal;
import java.util.OptionalInt;

public final class PreShopCreationEventListener implements Listener {

    private final ChestShopRestrictions plugin;

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

        // Price is on sign line 3 (index 2).
        // By HIGHEST priority, ChestShop's PriceChecker (LOWEST) has already normalized this line.
        String line = event.getSignLine((byte) 2);
        if (line == null) return;

        boolean hasBuy  = PriceUtil.hasBuyPrice(line);
        boolean hasSell = PriceUtil.hasSellPrice(line);

        // If no recognizable price is present, let ChestShop decide.
        if (!hasBuy && !hasSell) return;

        BigDecimal buyPrice  = PriceUtil.getExactBuyPrice(line);
        BigDecimal sellPrice = PriceUtil.getExactSellPrice(line);

        // Whole numbers only: reject if any present price has decimals
        if (plugin.isWholeNumbersOnly() && !Permission.WHOLE_NUMBER_BYPASS.has(event.getPlayer())) {
            if ((hasBuy && hasFraction(buyPrice)) || (hasSell && hasFraction(sellPrice))) {
                event.getPlayer().sendMessage(plugin.formatMessage(plugin.getMsgWholeNumbersOnly()));
                event.setOutcome(PreShopCreationEvent.CreationOutcome.INVALID_PRICE);
                return;
            }
        }

        // Minimum buy price check
        if (hasBuy && buyPrice.compareTo(plugin.getMinBuy()) < 0
                && !Permission.MIN_BUY_BYPASS.has(event.getPlayer())) {
            // Allow free shops (price 0) if configured
            if (!(plugin.isAllowFreeShops() && buyPrice.compareTo(BigDecimal.ZERO) == 0)) {
                event.getPlayer().sendMessage(plugin.formatMessage(plugin.getMsgMinBuy(), plugin.getMinBuy()));
                event.setOutcome(PreShopCreationEvent.CreationOutcome.BUY_PRICE_BELOW_MIN);
                return;
            }
        }

        // Minimum sell price check
        if (hasSell && sellPrice.compareTo(plugin.getMinSell()) < 0
                && !Permission.MIN_SELL_BYPASS.has(event.getPlayer())) {
            // Allow free shops (price 0) if configured
            if (!(plugin.isAllowFreeShops() && sellPrice.compareTo(BigDecimal.ZERO) == 0)) {
                event.getPlayer().sendMessage(plugin.formatMessage(plugin.getMsgMinSell(), plugin.getMinSell()));
                event.setOutcome(PreShopCreationEvent.CreationOutcome.SELL_PRICE_BELOW_MIN);
            }
        }
    }

    private static boolean hasFraction(BigDecimal v) {
        // "1.0" becomes scale 0 after stripping zeros; "1.5" stays scale 1
        return v.stripTrailingZeros().scale() > 0;
    }
}