
package my.kingdomvortex.vortexshop.gui;

import my.kingdomvortex.vortexshop.VortexShop;
import my.kingdomvortex.vortexshop.manager.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GUIListener implements Listener {

    private final VortexShop plugin;
    private final GUIManager gui;

    private final Map<UUID, String> chatActions = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> sellInventories = new ConcurrentHashMap<>();

    public GUIListener(VortexShop plugin) {
        this.plugin = plugin;
        this.gui = new GUIManager(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder(false) instanceof MenuHolder holder)) return;

        String type = holder.getType();
        int slot = event.getRawSlot();

        if (type.equals("SELL")) {
            handleSellClick(event, player, slot);
            return;
        }

        event.setCancelled(true);

        if (slot < 0 || slot >= top.getSize()) return;

        switch (type) {
            case "MAIN" ->
                    handleMain(player, slot);

            case "CATEGORY" ->
                    handleCategory(player, holder, slot, event);

            case "EDITOR" ->
                    handleEditor(player, slot);

            case "CATEGORIES_EDITOR" ->
                    handleCategoriesEditor(player, slot, event);

            case "CATEGORY_EDITOR" ->
                    handleCategoryEditor(player, holder, slot, event);

            case "ITEM_EDITOR" ->
                    handleItemEditor(player, holder, slot);

            default -> {
            }
        }
    }

    private void handleMain(Player player, int slot) {
        ShopManager shop = plugin.getShopManager();

        for (String category : shop.getCategories()) {
            if (shop.getCategorySlot(category) != slot) continue;

            String permission = shop.getCategoryPermission(category);

            if (permission != null && !permission.isBlank()
                    && !player.hasPermission(permission)
                    && !player.hasPermission("vortexshop.bypass")) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&cKamu tidak memiliki akses."));
                return;
            }

            gui.openCategory(player, category);
            return;
        }
    }

    private void handleCategory(
            Player player,
            MenuHolder holder,
            int slot,
            InventoryClickEvent event
    ) {
        if (slot == 49) {
            gui.openMain(player);
            return;
        }

        String category = holder.getCategory();

        if (category == null) return;

        ShopManager shop = plugin.getShopManager();

        String itemSlot = String.valueOf(slot);

        if (!shop.hasItem(category, itemSlot)) return;

        if (event.isRightClick()) {
            int amount = event.isShiftClick() ? 64 : 1;

            plugin.getTransactionManager().sellItem(
                    player,
                    category,
                    itemSlot,
                    amount
            );

            gui.openCategory(player, category);
            return;
        }

        if (event.isLeftClick()) {
            gui.openQuantity(player, category, itemSlot);
        }
    }

    private void handleEditor(Player player, int slot) {
        switch (slot) {
            case 10, 14 ->
                    gui.openCategoriesEditor(player);

            case 12 -> {
                plugin.reloadPlugin();

                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMessage("reloaded"));

                gui.openEditor(player);
            }

            case 16 ->
                    player.closeInventory();

            default -> {
            }
        }
    }

    private void handleCategoriesEditor(
            Player player,
            int slot,
            InventoryClickEvent event
    ) {
        if (slot == 49) {
            requestChat(player, "CREATE_CATEGORY");
            return;
        }

        if (slot == 53) {
            gui.openEditor(player);
            return;
        }

        if (slot < 0 || slot >= 45) return;

        List<String> categories =
                new ArrayList<>(plugin.getShopManager().getCategories());

        if (slot >= categories.size()) return;

        String category = categories.get(slot);

        if (event.isRightClick()) {
            if (plugin.getShopManager().deleteCategory(category)) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&cKategori berhasil dihapus."));
            }

            gui.openCategoriesEditor(player);
            return;
        }

        gui.openCategoryEditor(player, category);
    }

    private void handleCategoryEditor(
            Player player,
            MenuHolder holder,
            int slot,
            InventoryClickEvent event
    ) {
        String category = holder.getCategory();

        if (category == null) return;

        ShopManager shop = plugin.getShopManager();

        if (slot == 49) {
            gui.openCategoriesEditor(player);
            return;
        }

        if (slot == 51) {
            ItemStack hand = player.getInventory().getItemInMainHand();

            if (hand.getType().isAir()) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&cPegang item terlebih dahulu."));
                return;
            }

            int emptySlot = -1;

            for (int i = 0; i < 45; i++) {
                if (!shop.hasItem(category, String.valueOf(i))) {
                    emptySlot = i;
                    break;
                }
            }

            if (emptySlot == -1) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&cTidak ada slot item kosong."));
                return;
            }

            ItemStack copy = hand.clone();
            copy.setAmount(1);

            if (shop.addItem(category, emptySlot, copy, 0, 0)) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&aItem berhasil ditambahkan."));

                gui.openItemEditor(player, category, String.valueOf(emptySlot));
            }

            return;
        }

        if (slot == 53) {
            if (shop.deleteCategory(category)) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&cKategori berhasil dihapus."));
            }

            gui.openCategoriesEditor(player);
            return;
        }

        if (slot < 0 || slot >= 45) return;

        String itemSlot = String.valueOf(slot);

        if (!shop.hasItem(category, itemSlot)) {
            ItemStack hand = player.getInventory().getItemInMainHand();

            if (hand.getType().isAir()) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&cPegang item untuk menambahkannya."));
                return;
            }

            ItemStack copy = hand.clone();
            copy.setAmount(1);

            if (shop.addItem(category, slot, copy, 0, 0)) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&aItem berhasil ditambahkan."));

                gui.openItemEditor(player, category, itemSlot);
            }

            return;
        }

        if (event.isRightClick()) {
            shop.removeItem(category, itemSlot);

            player.sendMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().color("&cItem berhasil dihapus."));

            gui.openCategoryEditor(player, category);
            return;
        }

        gui.openItemEditor(player, category, itemSlot);
    }

    private void handleItemEditor(
            Player player,
            MenuHolder holder,
            int slot
    ) {
        String category = holder.getCategory();
        String itemSlot = holder.getSlot();

        if (category == null || itemSlot == null) return;

        ShopManager shop = plugin.getShopManager();

        if (!shop.hasItem(category, itemSlot)) {
            gui.openCategoryEditor(player, category);
            return;
        }

        switch (slot) {
            case 18, 22 ->
                    gui.openCategoryEditor(player, category);

            case 10 ->
                    requestChat(player, "BUY_PRICE:" + category + ":" + itemSlot);

            case 12 ->
                    requestChat(player, "SELL_PRICE:" + category + ":" + itemSlot);

            case 14 ->
                    requestChat(player, "ITEM_NAME:" + category + ":" + itemSlot);

            case 16 -> {
                shop.removeItem(category, itemSlot);

                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&cItem berhasil dihapus."));

                gui.openCategoryEditor(player, category);
            }

            case 20 ->
                    requestChat(player, "ITEM_PERMISSION:" + category + ":" + itemSlot);

            case 26 ->
                    requestChat(player, "ITEM_LORE:" + category + ":" + itemSlot);

            default -> {
            }
        }
    }

    private void handleSellClick(
            InventoryClickEvent event,
            Player player,
            int rawSlot
    ) {
        Inventory top = event.getView().getTopInventory();

        if (rawSlot == 49) {
            event.setCancelled(true);

            double total = 0;

            for (int i = 0; i < 45; i++) {
                ItemStack item = top.getItem(i);

                if (item != null && !item.getType().isAir()) {
                    total += plugin.getTransactionManager()
                            .calculateSellPrice(item);
                }
            }

            if (total <= 0 || !Double.isFinite(total)) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMessage("no-items"));
                return;
            }

            // Sell All diproses hanya setelah pemain mengonfirmasi.
            Inventory confirm = gui.create(
                    "SELL_CONFIRM",
                    null,
                    null,
                    "&8✦ &bConfirm Sell All",
                    27
            );

            confirm.setItem(11, gui.button(
                    Material.LIME_WOOL,
                    "&aConfirm Sell",
                    List.of(
                            "&7Total: &e" + plugin.getEconomyManager().format(total),
                            "&7Klik untuk melanjutkan."
                    )
            ));

            confirm.setItem(15, gui.button(
                    Material.RED_WOOL,
                    "&cCancel",
                    List.of("&7Kembali ke Sell GUI.")
            ));

            sellInventories.put(player.getUniqueId(), top);
            player.openInventory(confirm);
            return;
        }

        if (rawSlot == 53) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        if (rawSlot < 0) {
            event.setCancelled(true);
            return;
        }

        if (rawSlot < 45) {
            if (event.getClickedInventory() == top) {
                event.setCancelled(false);
                return;
            }

            // Shift-click dari inventory player ke sell GUI.
            if (event.isShiftClick()) {
                event.setCancelled(true);

                ItemStack moving = event.getCurrentItem();
                if (moving == null || moving.getType().isAir()) return;

                Map<Integer, ItemStack> leftover = top.addItem(moving.clone());

                if (leftover.isEmpty()) {
                    event.setCurrentItem(null);
                } else {
                    int remaining = leftover.values().iterator().next().getAmount();
                    int moved = moving.getAmount() - remaining;

                    if (moved > 0) {
                        ItemStack updated = moving.clone();
                        updated.setAmount(remaining);
                        event.setCurrentItem(updated);
                    }
                }
                return;
            }

            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onConfirmClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!(event.getView().getTopInventory().getHolder(false)
                instanceof MenuHolder holder)) return;

        if (!holder.getType().equals("SELL_CONFIRM")) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot == 15) {
            Inventory sell = sellInventories.remove(player.getUniqueId());

            if (sell != null) {
                player.openInventory(sell);
            } else {
                gui.openSell(player);
            }
            return;
        }

        if (slot != 11) return;

        Inventory sell = sellInventories.remove(player.getUniqueId());

        if (sell == null) {
            player.closeInventory();
            return;
        }

        ItemStack[] contents = new ItemStack[45];

        for (int i = 0; i < 45; i++) {
            ItemStack item = sell.getItem(i);
            contents[i] = item == null ? null : item.clone();
        }

        double total = 0;

        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) continue;
            total += plugin.getTransactionManager().calculateSellPrice(item);
        }

        if (total <= 0 || !Double.isFinite(total)) {
            player.sendMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMessage("no-items"));
            player.closeInventory();
            return;
        }

        if (!plugin.getEconomyManager().deposit(player, total)) {
            player.sendMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().color("&cPembayaran gagal. Item tetap aman."));
            player.closeInventory();
            return;
        }

        for (int i = 0; i < 45; i++) {
            ItemStack item = contents[i];

            if (item == null || item.getType().isAir()) continue;

            if (plugin.getTransactionManager().calculateSellPrice(item) > 0) {
                sell.setItem(i, null);
            }
        }

        player.sendMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMessage("sell-success")
                .replace("{price}", plugin.getEconomyManager().format(total)));

        player.closeInventory();
    }

    private void requestChat(Player player, String action) {
        chatActions.put(player.getUniqueId(), action);
        player.closeInventory();

        player.sendMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().color("&eMasukkan nilai melalui chat."));
        player.sendMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().color("&7Ketik &ccancel &7untuk membatalkan."));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String action = chatActions.remove(player.getUniqueId());

        if (action == null) return;

        event.setCancelled(true);

        String input = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMessage("cancelled"));
                return;
            }

            handleChatInput(player, action, input);
        });
    }

    private void handleChatInput(Player player, String action, String input) {
        ShopManager shop = plugin.getShopManager();

        if (action.equals("CREATE_CATEGORY")) {
            String id = input.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9_-]", "");

            if (id.isBlank() || shop.hasCategory(id)) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&cID kategori tidak valid atau sudah digunakan."));
                gui.openCategoriesEditor(player);
                return;
            }

            if (shop.createCategory(id, "&b" + input, Material.CHEST, 0)) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMessage("category-created"));
            }

            gui.openCategoriesEditor(player);
            return;
        }

        String[] parts = action.split(":", 3);

        if (parts.length != 3) return;

        String type = parts[0];
        String category = parts[1];
        String itemSlot = parts[2];

        if (!shop.hasCategory(category)) return;

        if (type.equals("BUY_PRICE") || type.equals("SELL_PRICE")) {
            try {
                double price = Double.parseDouble(input);

                if (!Double.isFinite(price) || price < 0) {
                    throw new NumberFormatException();
                }

                String priceType = type.equals("BUY_PRICE") ? "buy" : "sell";

                if (shop.updatePrice(category, itemSlot, priceType, price)) {
                    player.sendMessage(plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().color("&aHarga berhasil diperbarui."));
                }

                gui.openItemEditor(player, category, itemSlot);

            } catch (NumberFormatException exception) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color("&cMasukkan angka yang valid."));
                gui.openItemEditor(player, category, itemSlot);
            }

            return;
        }

        if (type.equals("ITEM_NAME")) {
            shop.setItemName(category, itemSlot, input);
            player.sendMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().color("&aNama item diperbarui."));
            gui.openItemEditor(player, category, itemSlot);
            return;
        }

        if (type.equals("ITEM_PERMISSION")) {
            shop.setItemPermission(category, itemSlot, input.equalsIgnoreCase("none") ? "" : input);
            player.sendMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().color("&aPermission item diperbarui."));
            gui.openItemEditor(player, category, itemSlot);
            return;
        }

        if (type.equals("ITEM_LORE")) {
            shop.setItemLore(category, itemSlot, Arrays.asList(input.split("\\|")));
            player.sendMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().color("&aLore item diperbarui."));
            gui.openItemEditor(player, category, itemSlot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false)
                instanceof MenuHolder holder)) return;

        if (!holder.getType().equals("SELL")) {
            event.setCancelled(true);
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 45 || rawSlot < 0) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) return;
        if (!holder.getType().equals("SELL")) return;

        Player player = (Player) event.getPlayer();

        for (int i = 0; i < 45; i++) {
            ItemStack item = event.getInventory().getItem(i);

            if (item == null || item.getType().isAir()) continue;

            Map<Integer, ItemStack> leftover =
                    player.getInventory().addItem(item.clone());

            for (ItemStack stack : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }

            event.getInventory().setItem(i, null);
        }
    }
}
