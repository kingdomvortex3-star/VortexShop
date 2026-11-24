
package my.kingdomvortex.vortexshop.manager;

import my.kingdomvortex.vortexshop.VortexShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TransactionManager {

    private final VortexShop plugin;

    public TransactionManager(VortexShop plugin) {
        this.plugin = plugin;
    }

    public boolean buy(Player player, String category, String slot, int amount) {
        ShopManager shop = plugin.getShopManager();

        if (!shop.hasItem(category, slot)) return false;
        if (amount <= 0 || amount > plugin.getConfig().getInt(
                "settings.max-buy-amount", 2304)) return false;

        String permission = shop.getItemPermission(category, slot);

        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            return false;
        }

        ItemStack template = shop.getItem(category, slot);
        double price = shop.getBuyPrice(category, slot) * amount;

        if (template == null || price < 0 || !Double.isFinite(price)) {
            return false;
        }

        if (!plugin.getEconomyManager().has(player, price)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("not-enough-money"));
            return false;
        }

        int maxStack = template.getMaxStackSize();
        int remaining = amount;

        if (remaining > 2304) return false;

        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= maxStack;
            } else if (content.isSimilar(template)) {
                remaining -= Math.max(0, maxStack - content.getAmount());
            }

            if (remaining <= 0) break;
        }

        if (remaining > 0) {
            return false;
        }

        if (!plugin.getEconomyManager().withdraw(player, price)) {
            return false;
        }

        int left = amount;

        while (left > 0) {
            int stackAmount = Math.min(left, maxStack);
            ItemStack stack = template.clone();
            stack.setAmount(stackAmount);

            player.getInventory().addItem(stack).values().forEach(
                    player.getWorld()::dropItemNaturally
            );

            left -= stackAmount;
        }

        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("purchase-success")
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{item}", template.getType().name())
                        .replace("{price}",
                                plugin.getEconomyManager().format(price)));

        return true;
    }

    public double calculateSellPrice(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;

        String blacklist = item.getType().name();

        if (plugin.getConfig().getStringList(
                "settings.sell-blacklist").contains(blacklist)) {
            return 0;
        }

        for (String category : plugin.getShopManager().getCategories()) {
            for (String slot : plugin.getShopManager().getItems(category)) {
                ItemStack shopItem = plugin.getShopManager().getItem(category, slot);

                if (shopItem == null || !shopItem.isSimilar(item)) continue;

                double price = plugin.getShopManager().getSellPrice(category, slot);

                if (price < 0 || !Double.isFinite(price)) return 0;

                return price * item.getAmount();
            }
        }

        return 0;
    }

    public double sellAll(Player player) {
        double total = 0;

        ItemStack[] contents = player.getInventory().getStorageContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            if (item == null || item.getType().isAir()) continue;

            double price = calculateSellPrice(item);

            if (price <= 0) continue;

            total += price;
            contents[i] = null;
        }

        if (total > 0 && Double.isFinite(total)) {
            if (plugin.getEconomyManager().deposit(player, total)) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("sell-success")
                                .replace("{price}",
                                        plugin.getEconomyManager().format(total)));
            }
        }

        player.getInventory().setStorageContents(contents);
        return total;
    }

    public VortexShop getPlugin() {
        return plugin;
    }
}
