
package my.kingdomvortex.vortexshop.gui;

import my.kingdomvortex.vortexshop.VortexShop;
import my.kingdomvortex.vortexshop.manager.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GUIListener implements Listener {

    private final VortexShop plugin;
    private final GUIManager gui;

    private final Map<UUID, String> chatActions =
            new ConcurrentHashMap<>();

    private final Map<UUID, Integer> pendingPurchases =
            new ConcurrentHashMap<>();

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
        int rawSlot = event.getRawSlot();

        if (type.equals("SELL")) {
            handleSellClick(event, player, rawSlot);
            return;
        }

        event.setCancelled(true);

        if (rawSlot < 0 || rawSlot >= top.getSize()) return;

        switch (type) {
            case "MAIN" -> handleMain(player, rawSlot);
            case "CATEGORY" -> handleCategory(player, holder, rawSlot);
            case "EDITOR" -> handleEditor(player, rawSlot);
            case "CATEGORIES_EDITOR" ->
                    handleCategories(player, rawSlot, event);
            case "CATEGORY_EDITOR" ->
                    handleCategoryEditor(player, holder, rawSlot, event);
            case "ITEM_EDITOR" ->
                    handleItemEditor(player, holder, rawSlot);
            case "QUANTITY" ->
                    handleQuantity(player, holder, rawSlot);
            case "BUY_CONFIRM" ->
                    handleBuyConfirm(player, holder, rawSlot);
        }
    }

    private void handleMain(Player player, int slot) {
        ShopManager shop = plugin.getShopManager();

        for (String category : shop.getCategories()) {
            if (shop.getCategorySlot(category) == slot) {
                gui.openCategory(player, category);
                return;
            }
        }
    }

    private void handleCategory(Player player, MenuHolder holder, int slot) {
        if (slot == 49) {
            gui.openMain(player);
            return;
        }

        String category = holder.getCategory();
        String itemSlot = String.valueOf(slot);

        if (!plugin.getShopManager().hasItem(category, itemSlot)) return;

        gui.openQuantity(player, category, itemSlot);
    }

    private void handleEditor(Player player, int slot) {
        switch (slot) {
            case 10 -> gui.openCategoriesEditor(player);

            case 12 -> {
                plugin.reloadPlugin();
                player.sendMessage("&aVortexShop reloaded!");
                gui.openEditor(player);
            }

            case 14 -> gui.openMain(player);
            case 16 -> player.closeInventory();
        }
    }

    private void handleCategories(Player player, int slot,
                                  InventoryClickEvent event) {
        ShopManager shop = plugin.getShopManager();

        if (slot == 49) {
            requestChat(player, "CREATE_CATEGORY");
            return;
        }

        if (slot == 53) {
            gui.openEditor(player);
            return;
        }

        if (slot < 0 || slot >= 45) return;

        List<String> categories = new ArrayList<>(shop.getCategories());

        if (slot >= categories.size()) return;

        String category = categories.get(slot);

        if (event.isRightClick()) {
            shop.deleteCategory(category);
            player.sendMessage(color("&cCategory deleted."));
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
        ShopManager shop = plugin.getShopManager();
        String category = holder.getCategory();

        if (!shop.hasCategory(category)) {
            player.closeInventory();
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
            addItemFromHand(player, category, slot);
            return;
        }

        if (slot < 0 || slot >= 45) return;

        String itemSlot = String.valueOf(slot);

        if (shop.hasItem(category, itemSlot)) {
            if (event.isRightClick()) {
                shop.removeItem(category, itemSlot);
                player.sendMessage(color("&cItem removed."));
                gui.openCategoryEditor(player, category);
            } else {
                gui.openItemEditor(player, category, itemSlot);
            }
            return;
        }

        addItemFromHand(player, category, slot);
    }

    private void addItemFromHand(Player player, String category, int slot) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            player.sendMessage(color("&cHold an item first!"));
            return;
        }

        if (plugin.getShopManager().addItem(
                category, slot, hand.clone(), 0, 0)) {

            player.sendMessage(color("&aItem added!"));
            gui.openItemEditor(
                    player, category, String.valueOf(slot)
            );
        }
    }

    private void handleItemEditor(
            Player player,
            MenuHolder holder,
            int slot
    ) {
        String category = holder.getCategory();
        String itemSlot = holder.getSlot();

        ShopManager shop = plugin.getShopManager();

        if (!shop.hasItem(category, itemSlot)) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 10 -> requestChat(player,
                    "BUY:" + category + ":" + itemSlot);

            case 12 -> requestChat(player,
                    "SELL:" + category + ":" + itemSlot);

            case 14 -> requestChat(player,
                    "NAME:" + category + ":" + itemSlot);

            case 16 -> requestChat(player,
                    "LORE:" + category + ":" + itemSlot);

            case 20 -> requestChat(player,
                    "PERMISSION:" + category + ":" + itemSlot);

            case 22 -> requestChat(player,
                    "MOVE:" + category + ":" + itemSlot);

            case 26 -> {
                shop.removeItem(category, itemSlot);
                player.sendMessage(color("&cItem deleted."));
                gui.openCategoryEditor(player, category);
            }

            case 18 -> gui.openCategoryEditor(player, category);
        }
    }

    private void handleQuantity(
            Player player,
            MenuHolder holder,
            int slot
    ) {
        int amount = switch (slot) {
            case 10 -> 1;
            case 12 -> 16;
            case 14 -> 32;
            case 16 -> 64;
            default -> 0;
        };

        if (slot == 22) {
            gui.openCategory(player, holder.getCategory());
            return;
        }

        if (amount <= 0) return;

        pendingPurchases.put(player.getUniqueId(), amount);

        gui.openConfirm(
                player,
                holder.getCategory(),
                holder.getSlot(),
                amount
        );
    }

    private void handleBuyConfirm(
            Player player,
            MenuHolder holder,
            int slot
    ) {
        if (slot == 15) {
            pendingPurchases.remove(player.getUniqueId());
            gui.openCategory(player, holder.getCategory());
            return;
        }

        if (slot != 11) return;

        Integer amount = pendingPurchases.remove(player.getUniqueId());

        if (amount == null) {
            player.closeInventory();
            return;
        }

        plugin.getTransactionManager().buy(
                player,
                holder.getCategory(),
                holder.getSlot(),
                amount
        );

        player.closeInventory();
    }

    // SELL GUI

    private void handleSellClick(
            InventoryClickEvent event,
            Player player,
            int slot
    ) {
        Inventory top = event.getView().getTopInventory();

        if (slot == 49) {
            event.setCancelled(true);
            sellAllFromGUI(player, top);
            return;
        }

        if (slot == 53) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        if (slot < 0) return;

        if (slot < top.getSize()) {
            event.setCancelled(slot >= 49);
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);

            ItemStack source = event.getCurrentItem();

            if (source == null || source.getType().isAir()) return;

            ItemStack moving = source.clone();

            int moved = insertItem(top, moving);

            if (moved > 0) {
                int remaining = source.getAmount() - moved;

                if (remaining <= 0) {
                    event.setCurrentItem(null);
                } else {
                    source.setAmount(remaining);
                    event.setCurrentItem(source);
                }
            }
        }
    }

    private int insertItem(Inventory inventory, ItemStack item) {
        int remaining = item.getAmount();
        int maxStack = item.getMaxStackSize();

        for (int i = 0; i < 49 && remaining > 0; i++) {
            ItemStack current = inventory.getItem(i);

            if (current == null || !current.isSimilar(item)) continue;

            int space = maxStack - current.getAmount();
            int add = Math.min(space, remaining);

            if (add > 0) {
                current.setAmount(current.getAmount() + add);
                remaining -= add;
            }
        }

        for (int i = 0; i < 49 && remaining > 0; i++) {
            ItemStack current = inventory.getItem(i);

            if (current != null && !current.getType().isAir()) continue;

            int add = Math.min(maxStack, remaining);

            ItemStack stack = item.clone();
            stack.setAmount(add);

            inventory.setItem(i, stack);
            remaining -= add;
        }

        return item.getAmount() - remaining;
    }

    private void sellAllFromGUI(Player player, Inventory inventory) {
        double total = 0;
        boolean[] sellable = new boolean[49];

        for (int i = 0; i < 49; i++) {
            ItemStack item = inventory.getItem(i);

            if (item == null || item.getType().isAir()) continue;

            double price = plugin.getTransactionManager()
                    .calculateSellPrice(item);

            if (price <= 0) continue;

            if (!Double.isFinite(price)) {
                player.sendMessage(color("&cInvalid transaction!"));
                return;
            }

            total += price;
            sellable[i] = true;
        }

        if (total <= 0 || !Double.isFinite(total)) {
            player.sendMessage(color("&cNo sellable items found!"));
            return;
        }

        // Deposit before removing any items.
        if (!plugin.getEconomyManager().deposit(player, total)) {
            player.sendMessage(color("&cPayment failed! Items are safe."));
            return;
        }

        for (int i = 0; i < 49; i++) {
            if (sellable[i]) {
                inventory.setItem(i, null);
            }
        }

        player.sendMessage(color(
                "&aSuccessfully sold items for &e"
                        + plugin.getEconomyManager().format(total)
        ));

        playSound(player, "sounds.sell");
    }

    // INVENTORY DRAG

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory()
                .getHolder(false) instanceof MenuHolder holder)) return;

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

    // RETURN UNSOLD ITEMS

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false)
                instanceof MenuHolder holder)) return;

        Player player = (Player) event.getPlayer();

        if (!holder.getType().equals("SELL")) return;

        Inventory inventory = event.getInventory();

        for (int i = 0; i < 49; i++) {
            ItemStack item = inventory.getItem(i);

            if (item == null || item.getType().isAir()) continue;

            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(item.clone());

            leftovers.values().forEach(stack ->
                    player.getWorld().dropItemNaturally(
                            player.getLocation(), stack
                    )
            );

            inventory.setItem(i, null);
        }
    }

    // CHAT EDITOR

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        String action = chatActions.remove(player.getUniqueId());

        if (action == null) return;

        event.setCancelled(true);

        String input = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(color("&cCancelled."));
                return;
            }

            handleChatInput(player, action, input);
        });
    }

    private void requestChat(Player player, String action) {
        chatActions.put(player.getUniqueId(), action);
        player.closeInventory();

        player.sendMessage(color("&8&m------------------------"));
        player.sendMessage(color("&bVortexShop Editor"));
        player.sendMessage(color("&7Enter the new value in chat."));
        player.sendMessage(color("&7Type &ccancel &7to cancel."));
        player.sendMessage(color("&8&m------------------------"));
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
                player.sendMessage(color("&cInvalid category ID."));
                gui.openCategoriesEditor(player);
                return;
            }

            shop.createCategory(
                    id, "&b" + input, Material.CHEST, 0
            );

            player.sendMessage(color("&aCategory created!"));
            gui.openCategoriesEditor(player);
            return;
        }

        if (action.startsWith("CATEGORY_SETTINGS:")) {
            updateCategory(
                    player,
                    action.substring("CATEGORY_SETTINGS:".length()),
                    input
            );
            return;
        }

        String[] parts = action.split(":", 3);

        if (parts.length != 3) return;

        String type = parts[0];
        String category = parts[1];
        String slot = parts[2];

        if (!shop.hasItem(category, slot)) return;

        switch (type) {
            case "BUY", "SELL" -> {
                try {
                    double price = Double.parseDouble(input);

                    if (!Double.isFinite(price) || price < 0) {
                        throw new NumberFormatException();
                    }

                    shop.updatePrice(
                            category,
                            slot,
                            type.toLowerCase(Locale.ROOT),
                            price
                    );

                    player.sendMessage(color("&aPrice updated!"));

                } catch (NumberFormatException exception) {
                    player.sendMessage(color("&cInvalid price!"));
                }

                gui.openItemEditor(player, category, slot);
            }

            case "NAME" -> {
                shop.setItemName(category, slot, input);
                player.sendMessage(color("&aName updated!"));
                gui.openItemEditor(player, category, slot);
            }

            case "LORE" -> {
                List<String> lore = Arrays.stream(input.split("\\|"))
                        .map(String::trim)
                        .toList();

                shop.setItemLore(category, slot, lore);
                player.sendMessage(color("&aLore updated!"));
                gui.openItemEditor(player, category, slot);
            }

            case "PERMISSION" -> {
                shop.setItemPermission(
                        category,
                        slot,
                        input.equalsIgnoreCase("none") ? "" : input
                );

                player.sendMessage(color("&aPermission updated!"));
                gui.openItemEditor(player, category, slot);
            }

            case "MOVE" -> {
                try {
                    int newSlot = Integer.parseInt(input);

                    if (newSlot < 0 || newSlot >= 45) {
                        throw new NumberFormatException();
                    }

                    shop.setItemSlot(category, slot, newSlot);

                    player.sendMessage(color("&aItem moved!"));

                } catch (NumberFormatException exception) {
                    player.sendMessage(color("&cInvalid slot!"));
                }

                gui.openCategoryEditor(player, category);
            }
        }
    }

    private void updateCategory(
            Player player,
            String category,
            String input
    ) {
        String[] parts = input.split("\\|", 4);

        if (parts.length < 3) {
            player.sendMessage(color(
                    "&cFormat: NAME | MATERIAL | SLOT | PERMISSION"
            ));
            gui.openCategoryEditor(player, category);
            return;
        }

        try {
            Material icon = Material.matchMaterial(parts[1].trim());
            int slot = Integer.parseInt(parts[2].trim());

            if (icon == null || slot < 0 || slot >= 54) {
                throw new IllegalArgumentException();
            }

            shopUpdate(
                    category,
                    parts[0].trim(),
                    icon,
                    slot,
                    parts.length >= 4 ? parts[3].trim() : ""
            );

            player.sendMessage(color("&aCategory updated!"));

        } catch (IllegalArgumentException exception) {
            player.sendMessage(color("&cInvalid settings!"));
        }

        gui.openCategoryEditor(player, category);
    }

    private void shopUpdate(
            String category,
            String name,
            Material icon,
            int slot,
            String permission
    ) {
        ShopManager shop = plugin.getShopManager();

        shop.setCategoryName(category, name);
        shop.setCategoryIcon(category, icon);
        shop.setCategorySlot(category, slot);
        shop.setCategoryPermission(category, permission);
    }

    private String color(String message) {
        return plugin.getConfigManager().color(
                plugin.getConfigManager().getPrefix() + message
        );
    }

    private void playSound(Player player, String path) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) return;

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
