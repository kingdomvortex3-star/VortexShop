
package my.kingdomvortex.vortexshop.manager;

import my.kingdomvortex.vortexshop.VortexShop;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TransactionManager {

    private final VortexShop plugin;

    public TransactionManager(VortexShop plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // BUY
    // =========================================================

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

        if (permission != null && !permission.isBlank()
                && !player.hasPermission(permission)
                && !player.hasPermission("vortexshop.bypass")) {
            send(player, "&cKamu tidak memiliki permission untuk item ini!");
            return false;
        }

        ItemStack template = shop.getItem(category, slot);

        if (template == null || template.getType().isAir()) {
            send(player, "&cItem toko tidak valid!");
            return false;
        }

        double unitPrice = shop.getBuyPrice(category, slot);
        double total = unitPrice * amount;

        if (!validPrice(unitPrice) || !validPrice(total)) {
            send(player, "&cHarga beli item belum diatur atau tidak valid!");
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

        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                available += maxStack;
            } else if (content.isSimilar(template)) {
                available += Math.max(0, maxStack - content.getAmount());
            }

            if (available >= amount) return true;
        }

        return available >= amount;
    }

    // =========================================================
    // SELL PRICE
    // =========================================================

    /**
     * Menghitung harga jual berdasarkan item yang terdaftar di Shop.
     *
     * Item yang tidak terdaftar di Shop tidak bisa dijual.
     * Harga jual dihitung per item x jumlah stack.
     */
    public double calculateSellPrice(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;

        if (isBlacklisted(item)) return 0;

        ShopManager shop = plugin.getShopManager();

        for (String category : shop.getCategories()) {
            for (String slot : shop.getItems(category)) {

                ItemStack template = shop.getItem(category, slot);

                if (template == null || !matchesShopItem(template, item)) {
                    continue;
                }

                double unitPrice = shop.getSellPrice(category, slot);

                // Harga jual 0 berarti item tidak dapat dijual.
                if (!validSellPrice(unitPrice)) return 0;

                double total = unitPrice * item.getAmount();

                return validSellPrice(total) ? total : 0;
            }
        }

        // Tidak ditemukan di Shop = tidak bisa dijual.
        return 0;
    }

    /**
     * Pencocokan item:
     * - Material harus sama.
     * - Jika item Shop punya nama/lore custom, nama/lore harus cocok.
     * - Jika tidak punya nama/lore custom, material saja cukup.
     */
    private boolean matchesShopItem(ItemStack template, ItemStack item) {
        if (template == null || item == null) return false;

        if (template.getType() != item.getType()) {
            return false;
        }

        ItemMeta templateMeta = template.getItemMeta();

        if (templateMeta == null) return true;

        boolean hasCustomName = templateMeta.hasDisplayName();
        boolean hasCustomLore = templateMeta.hasLore();

        if (!hasCustomName && !hasCustomLore) {
            return true;
        }

        ItemMeta itemMeta = item.getItemMeta();

        if (itemMeta == null) return false;

        if (hasCustomName) {
            if (!itemMeta.hasDisplayName()) return false;

            if (!templateMeta.getDisplayName()
                    .equals(itemMeta.getDisplayName())) {
                return false;
            }
        }

        if (hasCustomLore) {
            if (!itemMeta.hasLore()) return false;

            if (!templateMeta.getLore().equals(itemMeta.getLore())) {
                return false;
            }
        }

        return true;
    }

    private boolean isBlacklisted(ItemStack item) {
        List<String> blacklist = plugin.getConfig()
                .getStringList("settings.sell-blacklist");

        return blacklist.contains(item.getType().name());
    }

    // =========================================================
    // SELL ONE ITEM TYPE
    // =========================================================

    public boolean sellItem(
            Player player,
            String category,
            String slot,
            int amount
    ) {
        ShopManager shop = plugin.getShopManager();

        if (!shop.hasItem(category, slot)) return false;

        if (amount <= 0) return false;

        String permission = shop.getItemPermission(category, slot);

        if (permission != null && !permission.isBlank()
                && !player.hasPermission(permission)
                && !player.hasPermission("vortexshop.bypass")) {
            send(player, "&cKamu tidak memiliki permission untuk item ini!");
            return false;
        }

        ItemStack template = shop.getItem(category, slot);

        if (template == null || template.getType().isAir()) {
            return false;
        }

        double unitPrice = shop.getSellPrice(category, slot);
        double total = unitPrice * amount;

        if (!validSellPrice(unitPrice) || !validSellPrice(total)) {
            send(player, "&cItem ini tidak memiliki harga jual!");
            return false;
        }

        if (isBlacklisted(template)) {
            send(player, "&cItem ini tidak dapat dijual!");
            return false;
        }

        int remaining = amount;
        List<Integer> selectedSlots = new ArrayList<>();

        ItemStack[] contents = player.getInventory().getStorageContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack content = contents[i];

            if (content == null || !matchesShopItem(template, content)) {
                continue;
            }

            selectedSlots.add(i);
            remaining -= content.getAmount();

            if (remaining <= 0) break;
        }

        if (remaining > 0) {
            send(player, "&cItem tidak cukup!");
            return false;
        }

        // Bayar dahulu agar item tidak hilang jika pembayaran gagal.
        if (!plugin.getEconomyManager().deposit(player, total)) {
            send(player, "&cPembayaran gagal. Item tetap aman!");
            return false;
        }

        remaining = amount;

        for (int selectedSlot : selectedSlots) {
            ItemStack content = contents[selectedSlot];

            if (content == null) continue;

            int take = Math.min(remaining, content.getAmount());

            if (take >= content.getAmount()) {
                contents[selectedSlot] = null;
            } else {
                content.setAmount(content.getAmount() - take);
            }

            remaining -= take;

            if (remaining <= 0) break;
        }

        player.getInventory().setStorageContents(contents);

        send(player,
                "&aBerhasil menjual &f" + amount + "x "
                        + template.getType().name()
                        + " &aseharga &e"
                        + plugin.getEconomyManager().format(total)
        );

        playSound(player, "sounds.sell");
        return true;
    }

    // =========================================================
    // SELL ALL
    // =========================================================

    public boolean sellAll(Player player) {
        ItemStack[] contents =
                player.getInventory().getStorageContents();

        double total = 0;
        List<Integer> sellSlots = new ArrayList<>();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            if (item == null || item.getType().isAir()) continue;

            double price = calculateSellPrice(item);

            if (price <= 0) continue;

            total += price;

            if (!Double.isFinite(total)) {
                send(player, "&cTotal harga jual tidak valid!");
                return false;
            }

            sellSlots.add(i);
        }

        if (sellSlots.isEmpty() || !validSellPrice(total)) {
            send(player, "&cTidak ada item Shop yang bisa dijual!");
            return false;
        }

        // Deposit dahulu. Item tidak dihapus jika pembayaran gagal.
        if (!plugin.getEconomyManager().deposit(player, total)) {
            send(player, "&cPembayaran gagal. Item tetap aman!");
            return false;
        }

        for (int slot : sellSlots) {
            contents[slot] = null;
        }

        player.getInventory().setStorageContents(contents);

        send(player,
                "&aBerhasil menjual semua item Shop seharga &e"
                        + plugin.getEconomyManager().format(total)
        );

        playSound(player, "sounds.sell");
        return true;
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private boolean validPrice(double price) {
        return Double.isFinite(price) && price >= 0;
    }

    private boolean validSellPrice(double price) {
        return Double.isFinite(price) && price > 0;
    }

    // =========================================================
    // MESSAGE & SOUND
    // =========================================================

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
