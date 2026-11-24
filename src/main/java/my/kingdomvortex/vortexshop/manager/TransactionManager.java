
package my.kingdomvortex.vortexshop.manager;

import my.kingdomvortex.vortexshop.VortexShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TransactionManager {

    private final VortexShop plugin;

    public TransactionManager(VortexShop plugin) {
        this.plugin = plugin;
    }

    public boolean buy(Player player, String category, String slot, int amount) {
        ShopManager shop = plugin.getShopManager();

        if (!shop.hasItem(category, slot)) return false;

        int maximum = plugin.getConfig().getInt(
                "settings.max-buy-amount", 2304
        );

        if (amount <= 0 || amount > maximum) {
            send(player, "&cJumlah pembelian tidak valid!");
            return false;
        }

        String permission = shop.getItemPermission(category, slot);

        if (!permission.isEmpty()
                && !player.hasPermission(permission)) {
            send(player, "&cKamu tidak memiliki permission untuk item ini!");
            return false;
        }

        ItemStack template = shop.getItem(category, slot);

        if (template == null || template.getType().isAir()) {
            return false;
        }

        double unitPrice = shop.getBuyPrice(category, slot);
        double total = unitPrice * amount;

        if (!validPrice(unitPrice) || !validPrice(total)) {
            send(player, "&cHarga item tidak valid!");
            return false;
        }

        if (!hasInventorySpace(player, template, amount)) {
            send(player, "&cInventory kamu tidak cukup!");
            return false;
        }

        if (!plugin.getEconomyManager().has(player, total)) {
            send(player, plugin.getConfigManager()
                    .getMessage("not-enough-money"));
            return false;
        }

        if (!plugin.getEconomyManager().withdraw(player, total)) {
            send(player, "&cTransaksi gagal. Uang tidak berhasil dipotong!");
            return false;
        }

        int remaining = amount;
        int maxStack = template.getMaxStackSize();

        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);

            ItemStack stack = template.clone();
            stack.setAmount(stackAmount);

            player.getInventory().addItem(stack);

            remaining -= stackAmount;
        }

        send(player,
                "&aBerhasil membeli &f" + amount + "x "
                        + template.getType().name()
                        + " &adengan harga &e"
                        + plugin.getEconomyManager().format(total)
        );

        playSound(player, "sounds.purchase");

        return true;
    }

    private boolean hasInventorySpace(
            Player player,
            ItemStack template,
            int amount
    ) {
        int available = 0;
        int maxStack = template.getMaxStackSize();

        for (ItemStack content :
                player.getInventory().getStorageContents()) {

            if (content == null || content.getType().isAir()) {
                available += maxStack;
                continue;
            }

            if (content.isSimilar(template)) {
                available += Math.max(
                        0,
                        maxStack - content.getAmount()
                );
            }

            if (available >= amount) return true;
        }

        return available >= amount;
    }

    public double calculateSellPrice(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }

        List<String> blacklist = plugin.getConfig()
                .getStringList("settings.sell-blacklist");

        if (blacklist.contains(item.getType().name())) {
            return 0;
        }

        ShopManager shop = plugin.getShopManager();

        for (String category : shop.getCategories()) {
            for (String slot : shop.getItems(category)) {

                ItemStack template = shop.getItem(category, slot);

                if (template == null || !template.isSimilar(item)) {
                    continue;
                }

                double unitPrice = shop.getSellPrice(category, slot);

                if (!validPrice(unitPrice)) return 0;

                double total = unitPrice * item.getAmount();

                return validPrice(total) ? total : 0;
            }
        }

        return 0;
    }

    public boolean sellItem(
            Player player,
            String category,
            String slot,
            int amount
    ) {
        ShopManager shop = plugin.getShopManager();

        if (!shop.hasItem(category, slot)) return false;

        if (amount <= 0) return false;

        ItemStack template = shop.getItem(category, slot);

        if (template == null) return false;

        double unitPrice = shop.getSellPrice(category, slot);
        double total = unitPrice * amount;

        if (!validPrice(unitPrice) || !validPrice(total)) {
            return false;
        }

        ItemStack found = null;

        for (ItemStack content :
                player.getInventory().getStorageContents()) {

            if (content == null || !content.isSimilar(template)) {
                continue;
            }

            if (content.getAmount() >= amount) {
                found = content;
                break;
            }
        }

        if (found == null) {
            send(player, "&cItem tidak cukup!");
            return false;
        }

        if (!plugin.getEconomyManager().deposit(player, total)) {
            send(player, "&cPembayaran gagal!");
            return false;
        }

        if (found.getAmount() == amount) {
            player.getInventory().removeItem(found);
        } else {
            found.setAmount(found.getAmount() - amount);
        }

        send(player, "&aBerhasil menjual " + amount
                + "x " + template.getType().name()
                + " seharga "
                + plugin.getEconomyManager().format(total));

        playSound(player, "sounds.sell");

        return true;
    }

    public boolean sellAll(Player player) {
        ItemStack[] contents =
                player.getInventory().getStorageContents();

        double total = 0;

        List<Integer> sellSlots = new ArrayList<>();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            if (item == null || item.getType().isAir()) {
                continue;
            }

            double price = calculateSellPrice(item);

            if (price <= 0) continue;

            total += price;
            sellSlots.add(i);
        }

        if (sellSlots.isEmpty() || !validPrice(total)) {
            send(player, "&cTidak ada item yang bisa dijual!");
            return false;
        }

        // Bayar dahulu. Jangan hapus item jika pembayaran gagal.
        if (!plugin.getEconomyManager().deposit(player, total)) {
            send(player, "&cPembayaran gagal. Item tetap aman!");
            return false;
        }

        // Hapus hanya slot yang sudah dihitung.
        for (int slot : sellSlots) {
            contents[slot] = null;
        }

        player.getInventory().setStorageContents(contents);

        send(player, "&aBerhasil menjual semua item seharga &e"
                + plugin.getEconomyManager().format(total));

        playSound(player, "sounds.sell");

        return true;
    }

    private boolean validPrice(double price) {
        return Double.isFinite(price) && price >= 0;
    }

    private void send(Player player, String message) {
        player.sendMessage(
                plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color(message)
        );
    }

    private void playSound(Player player, String path) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) {
            return;
        }

        String sound = plugin.getConfig().getString(path);

        if (sound == null) return;

        try {
            player.playSound(
                    player.getLocation(),
                    org.bukkit.Sound.valueOf(sound),
                    1f,
                    1f
            );
        } catch (IllegalArgumentException ignored) {
        }
    }
}
