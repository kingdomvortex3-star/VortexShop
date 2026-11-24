
package my.kingdomvortex.vortexshop.gui;

import my.kingdomvortex.vortexshop.VortexShop;
import my.kingdomvortex.vortexshop.manager.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    private final Map<UUID, Integer> pendingAmounts = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> pendingSell = new ConcurrentHashMap<>();

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
            case "MAIN" -> handleMain(player, slot);
            case "CATEGORY" -> handleCategory(player, holder, slot, event);
            case "EDITOR" -> handleEditor(player, slot);
            case "CATEGORIES_EDITOR" ->
                    handleCategoriesEditor(player, slot, event);
            case "CATEGORY_EDITOR" ->
                    handleCategoryEditor(player, holder, slot, event);
            case "ITEM_EDITOR" -> handleItemEditor(player, holder, slot);
            case "QUANTITY" -> handleQuantity(player, holder, slot);
            case "BUY_CONFIRM" -> handleBuyConfirm(player, holder, slot);
            case "SELL_CONFIRM" -> handleSellConfirm(player, slot);
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
                send(player, "&cKamu tidak memiliki akses ke kategori ini.");
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

        if (slot < 0 || slot >= 45) return;

        String category = holder.getCategory();
        String itemSlot = String.valueOf(slot);
        ShopManager shop = plugin.getShopManager();

        if (!shop.hasItem(category, itemSlot)) return;

        String permission = shop.getItemPermission(category, itemSlot);

        if (permission != null && !permission.isBlank()
                && !player.hasPermission(permission)
                && !player.hasPermission("vortexshop.bypass")) {
            send(player, "&cKamu tidak memiliki permission untuk item ini.");
            return;
        }

        if (event.isLeftClick()) {
            gui.openQuantity(player, category, itemSlot);
        } else if (event.isRightClick()) {
            if (plugin.getTransactionManager().sellItem(
                    player, category, itemSlot, 1)) {
                gui.openCategory(player, category);
            }
        }
    }

    private void handleEditor(Player player, int slot) {
        switch (slot) {
            case 10 -> gui.openCategoriesEditor(player);
            case 12 -> {
                plugin.reloadPlugin();
                send(player, "&aVortexShop berhasil di-reload.");
                gui.openEditor(player);
            }
            case 14 -> gui.openMain(player);
            case 16 -> player.closeInventory();
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
                send(player, "&cKategori &f" + category + " &cberhasil dihapus.");
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

        if (!shop.hasCategory(category)) {
            gui.openCategoriesEditor(player);
            return;
        }

        if (slot == 49) {
            gui.openCategoriesEditor(player);
            return;
        }

        if (slot == 51) {
            requestChat(player, "CATEGORY_SETTINGS:" + category);
            return;
        }

        if (slot == 53) {
            addHeldItem(player, category, findEmptyItemSlot(category));
            return;
        }

        if (slot < 0 || slot >= 45) return;

        String itemSlot = String.valueOf(slot);

        if (shop.hasItem(category, itemSlot)) {
            if (event.isRightClick()) {
                shop.removeItem(category, itemSlot);
                send(player, "&cItem berhasil dihapus.");
                gui.openCategoryEditor(player, category);
            } else {
                gui.openItemEditor(player, category, itemSlot);
            }
            return;
        }

        addHeldItem(player, category, slot);
    }

    private void addHeldItem(Player player, String category, int slot) {
        if (slot < 0 || slot >= 45) {
            send(player, "&cTidak ada slot item kosong.");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            send(player, "&cPegang item terlebih dahulu.");
            return;
        }

        ItemStack copy = hand.clone();
        copy.setAmount(1);

        if (plugin.getShopManager().addItem(category, slot, copy, 0, 0)) {
            send(player, "&aItem berhasil ditambahkan.");
            gui.openItemEditor(player, category, String.valueOf(slot));
        } else {
            send(player, "&cGagal menambahkan item.");
        }
    }

    private int findEmptyItemSlot(String category) {
        ShopManager shop = plugin.getShopManager();

        for (int i = 0; i < 45; i++) {
            if (!shop.hasItem(category, String.valueOf(i))) return i;
        }

        return -1;
    }

    private int findEmptyCategorySlot() {
        ShopManager shop = plugin.getShopManager();

        for (int slot = 0; slot < 54; slot++) {
            boolean occupied = false;

            for (String category : shop.getCategories()) {
                if (shop.getCategorySlot(category) == slot) {
                    occupied = true;
                    break;
                }
            }

            if (!occupied) return slot;
        }

        return -1;
    }

    private void handleItemEditor(Player player, MenuHolder holder, int slot) {
        String category = holder.getCategory();
        String itemSlot = holder.getSlot();

        if (category == null || itemSlot == null) return;

        ShopManager shop = plugin.getShopManager();

        if (!shop.hasItem(category, itemSlot)) {
            gui.openCategoryEditor(player, category);
            return;
        }

        switch (slot) {
            case 18 -> gui.openCategoryEditor(player, category);
            case 10 -> requestChat(player, "BUY_PRICE:" + category + ":" + itemSlot);
            case 12 -> requestChat(player, "SELL_PRICE:" + category + ":" + itemSlot);
            case 14 -> requestChat(player, "ITEM_NAME:" + category + ":" + itemSlot);
            case 16 -> requestChat(player, "ITEM_LORE:" + category + ":" + itemSlot);
            case 20 -> requestChat(player, "ITEM_PERMISSION:" + category + ":" + itemSlot);
            case 22 -> requestChat(player, "ITEM_SLOT:" + category + ":" + itemSlot);
            case 26 -> {
                shop.removeItem(category, itemSlot);
                send(player, "&cItem berhasil dihapus.");
                gui.openCategoryEditor(player, category);
            }
            default -> {
            }
        }
    }

    private void handleQuantity(Player player, MenuHolder holder, int slot) {
        String category = holder.getCategory();
        String itemSlot = holder.getSlot();

        if (category == null || itemSlot == null) return;

        if (slot == 22) {
            gui.openCategory(player, category);
            return;
        }

        int amount = switch (slot) {
            case 10 -> 1;
            case 12 -> 16;
            case 14 -> 32;
            case 16 -> 64;
            default -> 0;
        };

        if (amount <= 0) return;

        pendingAmounts.put(player.getUniqueId(), amount);
        gui.openConfirm(player, category, itemSlot, amount);
    }

    private void handleBuyConfirm(Player player, MenuHolder holder, int slot) {
        String category = holder.getCategory();
        String itemSlot = holder.getSlot();

        if (category == null || itemSlot == null) return;

        if (slot == 15) {
            pendingAmounts.remove(player.getUniqueId());
            gui.openCategory(player, category);
            return;
        }

        if (slot != 11) return;

        Integer amount = pendingAmounts.remove(player.getUniqueId());

        if (amount == null) {
            gui.openCategory(player, category);
            return;
        }

        if (plugin.getTransactionManager().buy(player, category, itemSlot, amount)) {
            gui.openCategory(player, category);
        } else {
            gui.openConfirm(player, category, itemSlot, amount);
            pendingAmounts.put(player.getUniqueId(), amount);
        }
    }

    // =========================================================
    // SELL GUI - STRICT SHOP VALIDATION
    // =========================================================

    private boolean isSellable(ItemStack item) {
        return item != null
                && !item.getType().isAir()
                && plugin.getTransactionManager().calculateSellPrice(item) > 0;
    }

    private void handleSellClick(
            InventoryClickEvent event,
            Player player,
            int rawSlot
    ) {
        Inventory top = event.getView().getTopInventory();

        if (rawSlot == 49) {
            event.setCancelled(true);
            openSellConfirmation(player, top);
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

        // Click inside Sell GUI.
        if (event.getClickedInventory() == top) {
            ItemStack cursor = event.getCursor();

            // Reject placing an item that is not registered in Shop.
            if (cursor != null && !cursor.getType().isAir()
                    && !isSellable(cursor)) {
                event.setCancelled(true);
                send(player, "&cItem ini tidak terdaftar di Shop dan tidak bisa dijual!");
                return;
            }

            event.setCancelled(false);
            return;
        }

        // Click inside player inventory.
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            ItemStack moving = event.getCurrentItem();

            if (moving == null || moving.getType().isAir()) {
                event.setCancelled(true);
                return;
            }

            // Shift-click: move only registered and sellable items.
            if (event.isShiftClick()) {
                event.setCancelled(true);

                if (!isSellable(moving)) {
                    send(player, "&cItem ini tidak terdaftar di Shop dan tidak bisa dijual!");
                    return;
                }

                Map<Integer, ItemStack> leftovers =
                        top.addItem(moving.clone());

                int leftoverAmount = 0;

                for (ItemStack leftover : leftovers.values()) {
                    leftoverAmount += leftover.getAmount();
                }

                int moved = moving.getAmount() - leftoverAmount;

                if (moved <= 0) return;

                if (moved >= moving.getAmount()) {
                    event.setCurrentItem(null);
                } else {
                    ItemStack updated = moving.clone();
                    updated.setAmount(moving.getAmount() - moved);
                    event.setCurrentItem(updated);
                }

                return;
            }

            // Allow normal pickup. Placement into the Sell GUI
            // is validated by the top-inventory click handler.
            event.setCancelled(false);
            return;
        }

        event.setCancelled(true);
    }

    private void openSellConfirmation(Player player, Inventory sellInventory) {
        double total = 0;

        for (int i = 0; i < 49; i++) {
            ItemStack item = sellInventory.getItem(i);

            if (!isSellable(item)) continue;

            total += plugin.getTransactionManager().calculateSellPrice(item);
        }

        if (total <= 0 || !Double.isFinite(total)) {
            send(player, "&cTidak ada item Shop yang bisa dijual.");
            return;
        }

        Inventory confirm = gui.create(
                "SELL_CONFIRM",
                null,
                null,
                "&8✦ &eConfirm Sell All",
                27
        );

        confirm.setItem(11, gui.button(
                Material.LIME_CONCRETE,
                "&aConfirm Sell",
                List.of(
                        "&7Total: &e" +
                                plugin.getEconomyManager().format(total),
                        "",
                        "&eClick to confirm"
                )
        ));

        confirm.setItem(15, gui.button(
                Material.RED_CONCRETE,
                "&cCancel",
                List.of("&7Return to Sell GUI")
        ));

        pendingSell.put(player.getUniqueId(), sellInventory);
        player.openInventory(confirm);
    }

    private void handleSellConfirm(Player player, int slot) {
        if (slot == 15) {
            Inventory sell = pendingSell.get(player.getUniqueId());

            if (sell != null) {
                player.openInventory(sell);
            } else {
                gui.openSell(player);
            }
            return;
        }

        if (slot != 11) return;

        Inventory sell = pendingSell.remove(player.getUniqueId());

        if (sell == null) {
            player.closeInventory();
            return;
        }

        double total = 0;

        // Recheck all items at confirmation time.
        for (int i = 0; i < 49; i++) {
            ItemStack item = sell.getItem(i);

            if (!isSellable(item)) continue;

            total += plugin.getTransactionManager().calculateSellPrice(item);
        }

        if (total <= 0 || !Double.isFinite(total)) {
            send(player, "&cTidak ada item Shop yang bisa dijual.");
            returnItems(player, sell);
            player.closeInventory();
            return;
        }

        if (!plugin.getEconomyManager().deposit(player, total)) {
            send(player, "&cPembayaran gagal. Item tetap aman.");
            returnItems(player, sell);
            player.closeInventory();
            return;
        }

        // Remove only items that are currently sellable.
        for (int i = 0; i < 49; i++) {
            ItemStack item = sell.getItem(i);

            if (isSellable(item)) {
                sell.setItem(i, null);
            }
        }

        send(player, "&aBerhasil menjual item Shop seharga &e"
                + plugin.getEconomyManager().format(total));

        // Return all non-sellable items to the player.
        returnItems(player, sell);
        player.closeInventory();
    }

    // =========================================================
    // CHAT INPUT
    // =========================================================

    private void requestChat(Player player, String action) {
        chatActions.put(player.getUniqueId(), action);
        player.closeInventory();

        send(player, "&eMasukkan nilai melalui chat.");
        send(player, "&7Ketik &ccancel &7untuk membatalkan.");
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
                send(player, "&cInput dibatalkan.");
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
                send(player, "&cID kategori tidak valid atau sudah digunakan.");
                gui.openCategoriesEditor(player);
                return;
            }

            int categorySlot = findEmptyCategorySlot();

            if (categorySlot == -1) {
                send(player, "&cTidak ada slot kategori kosong (0-53).");
                gui.openCategoriesEditor(player);
                return;
            }

            if (shop.createCategory(
                    id, "&b" + input, Material.CHEST, categorySlot
            )) {
                send(player, "&aKategori berhasil dibuat.");
            } else {
                send(player, "&cGagal membuat kategori.");
            }

            gui.openCategoriesEditor(player);
            return;
        }

        if (action.startsWith("CATEGORY_SETTINGS:")) {
            String category = action.substring("CATEGORY_SETTINGS:".length());

            if (!shop.hasCategory(category)) {
                send(player, "&cKategori tidak ditemukan.");
                gui.openCategoriesEditor(player);
                return;
            }

            String[] settings = input.split("\\|", 4);

            if (settings.length < 3) {
                send(player, "&cFormat: NAMA | MATERIAL | SLOT | PERMISSION");
                gui.openCategoryEditor(player, category);
                return;
            }

            try {
                String name = settings[0].trim();
                Material icon = Material.matchMaterial(settings[1].trim());
                int newSlot = Integer.parseInt(settings[2].trim());
                String permission = settings.length >= 4
                        ? settings[3].trim() : "";

                if (name.isBlank() || icon == null
                        || newSlot < 0 || newSlot >= 54) {
                    throw new IllegalArgumentException();
                }

                for (String other : shop.getCategories()) {
                    if (!other.equals(category)
                            && shop.getCategorySlot(other) == newSlot) {
                        send(player, "&cSlot tersebut sudah digunakan kategori lain.");
                        gui.openCategoryEditor(player, category);
                        return;
                    }
                }

                shop.setCategoryName(category, name);
                shop.setCategoryIcon(category, icon);
                shop.setCategorySlot(category, newSlot);
                shop.setCategoryPermission(
                        category,
                        permission.equalsIgnoreCase("none") ? "" : permission
                );

                send(player, "&aPengaturan kategori berhasil diperbarui.");
                gui.openCategoryEditor(player, category);

            } catch (IllegalArgumentException exception) {
                send(player, "&cFormat salah. Contoh: Blocks | BRICKS | 20 | none");
                gui.openCategoryEditor(player, category);
            }
            return;
        }

        String[] parts = action.split(":", 3);
        if (parts.length != 3) return;

        String type = parts[0];
        String category = parts[1];
        String itemSlot = parts[2];

        if (!shop.hasCategory(category) || !shop.hasItem(category, itemSlot)) {
            send(player, "&cKategori atau item tidak ditemukan.");
            return;
        }

        if (type.equals("BUY_PRICE") || type.equals("SELL_PRICE")) {
            try {
                double price = Double.parseDouble(input);

                if (!Double.isFinite(price) || price < 0) {
                    throw new NumberFormatException();
                }

                String priceType = type.equals("BUY_PRICE") ? "buy" : "sell";

                if (shop.updatePrice(category, itemSlot, priceType, price)) {
                    send(player, "&aHarga berhasil diperbarui.");
                }

                gui.openItemEditor(player, category, itemSlot);

            } catch (NumberFormatException exception) {
                send(player, "&cMasukkan angka yang valid, contoh: 100");
                gui.openItemEditor(player, category, itemSlot);
            }
            return;
        }

        if (type.equals("ITEM_NAME")) {
            shop.setItemName(category, itemSlot, input);
            send(player, "&aNama item berhasil diperbarui.");
            gui.openItemEditor(player, category, itemSlot);
            return;
        }

        if (type.equals("ITEM_LORE")) {
            shop.setItemLore(
                    category,
                    itemSlot,
                    Arrays.asList(input.split("\\|"))
            );
            send(player, "&aLore berhasil diperbarui.");
            gui.openItemEditor(player, category, itemSlot);
            return;
        }

        if (type.equals("ITEM_PERMISSION")) {
            shop.setItemPermission(
                    category,
                    itemSlot,
                    input.equalsIgnoreCase("none") ? "" : input
            );
            send(player, "&aPermission berhasil diperbarui.");
            gui.openItemEditor(player, category, itemSlot);
            return;
        }

        if (type.equals("ITEM_SLOT")) {
            try {
                int newSlot = Integer.parseInt(input);

                if (newSlot < 0 || newSlot >= 45) {
                    send(player, "&cSlot item harus antara 0 sampai 44.");
                    gui.openItemEditor(player, category, itemSlot);
                    return;
                }

                if (shop.hasItem(category, String.valueOf(newSlot))) {
                    send(player, "&cSlot tersebut sudah terisi item lain.");
                    gui.openItemEditor(player, category, itemSlot);
                    return;
                }

                shop.setItemSlot(category, itemSlot, newSlot);

                if (shop.hasItem(category, String.valueOf(newSlot))) {
                    send(player, "&aSlot item berhasil dipindahkan.");
                    gui.openCategoryEditor(player, category);
                } else {
                    send(player, "&cItem gagal dipindahkan.");
                    gui.openItemEditor(player, category, itemSlot);
                }

            } catch (NumberFormatException exception) {
                send(player, "&cMasukkan nomor slot yang valid.");
                gui.openItemEditor(player, category, itemSlot);
            }
        }
    }

    // =========================================================
    // DRAG
    // =========================================================

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false)
                instanceof MenuHolder holder)) {
            return;
        }

        if (!holder.getType().equals("SELL")) {
            event.setCancelled(true);
            return;
        }

        for (Map.Entry<Integer, ItemStack> entry :
                event.getNewItems().entrySet()) {

            int rawSlot = entry.getKey();

            // Only validate items entering Sell GUI slots.
            if (rawSlot >= 0
                    && rawSlot < event.getView().getTopInventory().getSize()
                    && !isSellable(entry.getValue())) {
                event.setCancelled(true);

                if (event.getWhoClicked() instanceof Player player) {
                    send(player,
                            "&cItem ini tidak terdaftar di Shop dan tidak bisa dijual!");
                }
                return;
            }
        }
    }

    // =========================================================
    // CLOSE / RETURN ITEMS
    // =========================================================

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false)
                instanceof MenuHolder holder)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) return;

        if (holder.getType().equals("SELL")) {
            if (pendingSell.containsKey(player.getUniqueId())) return;

            returnItems(player, event.getInventory());
            return;
        }

        if (holder.getType().equals("SELL_CONFIRM")) {
            Inventory sell = pendingSell.remove(player.getUniqueId());

            if (sell != null) {
                returnItems(player, sell);
            }
        }
    }

    private void returnItems(Player player, Inventory inventory) {
        for (int i = 0; i < 49; i++) {
            ItemStack item = inventory.getItem(i);

            if (item == null || item.getType().isAir()) continue;

            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(item.clone());

            for (ItemStack stack : leftovers.values()) {
                player.getWorld().dropItemNaturally(
                        player.getLocation(),
                        stack
                );
            }

            inventory.setItem(i, null);
        }
    }

    // =========================================================
    // MESSAGE
    // =========================================================

    private void send(Player player, String message) {
        player.sendMessage(
                plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color(message)
        );
    }
}
