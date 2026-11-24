
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
            case "ITEM_EDITOR" ->
                    handleItemEditor(player, holder, slot);
            case "QUANTITY" ->
                    handleQuantity(player, holder, slot);
            case "BUY_CONFIRM" ->
                    handleBuyConfirm(player, holder, slot);
            case "SELL_CONFIRM" ->
                    handleSellConfirm(player, slot);
            default -> {
            }
        }
    }

    // =========================================================
    // MAIN SHOP
    // =========================================================

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

    // =========================================================
    // CATEGORY SHOP
    // =========================================================

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
            return;
        }

        if (event.isRightClick()) {
            if (plugin.getTransactionManager().sellItem(
                    player, category, itemSlot, 1
            )) {
                gui.openCategory(player, category);
            }
        }
    }

    // =========================================================
    // EDITOR MAIN
    // =========================================================

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

    // =========================================================
    // CATEGORY LIST EDITOR
    // =========================================================

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

    // =========================================================
    // CATEGORY EDITOR
    // =========================================================

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
            ItemStack hand = player.getInventory().getItemInMainHand();

            if (hand.getType().isAir()) {
                send(player, "&cPegang item terlebih dahulu.");
                return;
            }

            int emptySlot = findEmptyItemSlot(category);

            if (emptySlot == -1) {
                send(player, "&cTidak ada slot item kosong.");
                return;
            }

            ItemStack copy = hand.clone();
            copy.setAmount(1);

            if (shop.addItem(category, emptySlot, copy, 0, 0)) {
                send(player, "&aItem berhasil ditambahkan.");
                gui.openItemEditor(player, category, String.valueOf(emptySlot));
            }

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

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            send(player, "&cPegang item untuk menambahkannya ke slot ini.");
            return;
        }

        ItemStack copy = hand.clone();
        copy.setAmount(1);

        if (shop.addItem(category, slot, copy, 0, 0)) {
            send(player, "&aItem berhasil ditambahkan.");
            gui.openItemEditor(player, category, itemSlot);
        }
    }

    private int findEmptyItemSlot(String category) {
        ShopManager shop = plugin.getShopManager();

        for (int i = 0; i < 45; i++) {
            if (!shop.hasItem(category, String.valueOf(i))) {
                return i;
            }
        }

        return -1;
    }

    // =========================================================
    // ITEM EDITOR
    // =========================================================

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
            case 18 -> gui.openCategoryEditor(player, category);

            case 10 ->
                    requestChat(player, "BUY_PRICE:" + category + ":" + itemSlot);

            case 12 ->
                    requestChat(player, "SELL_PRICE:" + category + ":" + itemSlot);

            case 14 ->
                    requestChat(player, "ITEM_NAME:" + category + ":" + itemSlot);

            case 16 ->
                    requestChat(player, "ITEM_LORE:" + category + ":" + itemSlot);

            case 20 ->
                    requestChat(player, "ITEM_PERMISSION:" + category + ":" + itemSlot);

            case 22 ->
                    requestChat(player, "ITEM_SLOT:" + category + ":" + itemSlot);

            case 26 -> {
                shop.removeItem(category, itemSlot);
                send(player, "&cItem berhasil dihapus.");
                gui.openCategoryEditor(player, category);
            }

            default -> {
            }
        }
    }

    // =========================================================
    // BUY QUANTITY
    // =========================================================

    private void handleQuantity(
            Player player,
            MenuHolder holder,
            int slot
    ) {
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

    // =========================================================
    // BUY CONFIRM
    // =========================================================

    private void handleBuyConfirm(
            Player player,
            MenuHolder holder,
            int slot
    ) {
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

        if (plugin.getTransactionManager().buy(
                player, category, itemSlot, amount
        )) {
            gui.openCategory(player, category);
        } else {
            gui.openConfirm(player, category, itemSlot, amount);
        }
    }

    // =========================================================
    // SELL GUI
    // =========================================================

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

        if (rawSlot >= 49) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == top) {
            event.setCancelled(false);
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);

            ItemStack moving = event.getCurrentItem();

            if (moving == null || moving.getType().isAir()) return;

            Map<Integer, ItemStack> leftovers =
                    top.addItem(moving.clone());

            if (leftovers.isEmpty()) {
                event.setCurrentItem(null);
            } else {
                ItemStack remaining = leftovers.values().iterator().next();

                int moved = moving.getAmount() - remaining.getAmount();

                if (moved > 0) {
                    ItemStack updated = moving.clone();
                    updated.setAmount(remaining.getAmount());
                    event.setCurrentItem(updated);
                }
            }
            return;
        }

        event.setCancelled(true);
    }

    private void openSellConfirmation(Player player, Inventory sellInventory) {
        double total = 0;

        for (int i = 0; i < 49; i++) {
            ItemStack item = sellInventory.getItem(i);

            if (item == null || item.getType().isAir()) continue;

            total += plugin.getTransactionManager().calculateSellPrice(item);
        }

        if (total <= 0 || !Double.isFinite(total)) {
            send(player, "&cTidak ada item yang bisa dijual.");
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
                        "&7Total: &e" + plugin.getEconomyManager().format(total),
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
            Inventory sell = pendingSell.remove(player.getUniqueId());

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

        for (int i = 0; i < 49; i++) {
            ItemStack item = sell.getItem(i);

            if (item == null || item.getType().isAir()) continue;

            total += plugin.getTransactionManager().calculateSellPrice(item);
        }

        if (total <= 0 || !Double.isFinite(total)) {
            send(player, "&cTidak ada item yang bisa dijual.");
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

        for (int i = 0; i < 49; i++) {
            ItemStack item = sell.getItem(i);

            if (item == null || item.getType().isAir()) continue;

            if (plugin.getTransactionManager().calculateSellPrice(item) > 0) {
                sell.setItem(i, null);
            }
        }

        send(player, "&aBerhasil menjual item seharga &e"
                + plugin.getEconomyManager().format(total));

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

    private void handleChatInput(
            Player player,
            String action,
            String input
    ) {
        ShopManager shop = plugin.getShopManager();

        if (action.equals("CREATE_CATEGORY")) {
            String id = input.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9_-]", "");

            if (id.isBlank() || shop.hasCategory(id)) {
                send(player, "&cID kategori tidak valid atau sudah digunakan.");
                gui.openCategoriesEditor(player);
                return;
            }

            if (shop.createCategory(id, "&b" + input, Material.CHEST, 0)) {
                send(player, "&aKategori berhasil dibuat.");
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
            List<String> lore = Arrays.asList(input.split("\\|"));
            shop.setItemLore(category, itemSlot, lore);
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

                shop.setItemSlot(category, itemSlot, newSlot);
                send(player, "&aSlot item berhasil dipindahkan.");
                gui.openCategoryEditor(player, category);

            } catch (NumberFormatException exception) {
                send(player, "&cMasukkan nomor slot yang valid.");
                gui.openItemEditor(player, category, itemSlot);
            }

            return;
        }

        if (type.equals("CATEGORY_SETTINGS")) {
            String[] settings = input.split("\\|", 4);

            if (settings.length < 3) {
                send(player, "&cFormat: NAMA | MATERIAL | SLOT | PERMISSION");
                gui.openCategoryEditor(player, category);
                return;
            }

            try {
                String name = settings[0].trim();
                Material icon = Material.matchMaterial(settings[1].trim());
                int slot = Integer.parseInt(settings[2].trim());
                String permission = settings.length >= 4
                        ? settings[3].trim() : "";

                if (icon == null || slot < 0 || slot >= 54) {
                    throw new IllegalArgumentException();
                }

                shop.setCategoryName(category, name);
                shop.setCategoryIcon(category, icon);
                shop.setCategorySlot(category, slot);
                shop.setCategoryPermission(category, permission);

                send(player, "&aPengaturan kategori berhasil diperbarui.");
                gui.openCategoryEditor(player, category);

            } catch (IllegalArgumentException exception) {
                send(player, "&cFormat salah. Contoh: Blocks | BRICKS | 20 | none");
                gui.openCategoryEditor(player, category);
            }
        }
    }

    // =========================================================
    // INVENTORY DRAG
    // =========================================================

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false)
                instanceof MenuHolder holder)) return;

        if (!holder.getType().equals("SELL")) {
            event.setCancelled(true);
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 0 || rawSlot >= 49) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // =========================================================
    // INVENTORY CLOSE
    // =========================================================

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false)
                instanceof MenuHolder holder)) return;

        Player player = (Player) event.getPlayer();

        if (holder.getType().equals("SELL")) {
            // Saat konfirmasi dibuka, item tetap disimpan di inventory sementara.
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

    private void send(Player player, String message) {
        player.sendMessage(
                plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().color(message)
        );
    }
}
