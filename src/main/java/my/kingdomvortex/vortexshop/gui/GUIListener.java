
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

    private final Map<UUID, String> chatActions = new ConcurrentHashMap<>();

    public GUIListener(VortexShop plugin) {
        this.plugin = plugin;
        this.gui = new GUIManager(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) {
            return;
        }

        String type = holder.getType();
        int slot = event.getRawSlot();

        if (type.equals("SELL")) {
            handleSellClick(event, player, holder, slot);
            return;
        }

        event.setCancelled(true);

        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (type) {
            case "MAIN" -> handleMain(player, slot);
            case "CATEGORY" -> handleCategory(player, holder, slot, event);
            case "EDITOR" -> handleEditor(player, slot);
            case "CATEGORIES_EDITOR" -> handleCategoriesEditor(player, slot);
            case "CATEGORY_EDITOR" ->
                    handleCategoryEditor(player, holder, slot, event);
            case "ITEM_EDITOR" ->
                    handleItemEditor(player, holder, slot);
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

    private void handleCategory(Player player, MenuHolder holder,
                                int slot, InventoryClickEvent event) {

        if (slot == 49) {
            gui.openMain(player);
            return;
        }

        String category = holder.getCategory();

        if (event.isLeftClick()) {
            int amount = event.isShiftClick() ? 64 : 1;

            if (plugin.getTransactionManager().buy(
                    player, category, String.valueOf(slot), amount)) {

                playSound(player, "sounds.purchase");
                gui.openCategory(player, category);
            }
        }

        if (event.isRightClick()) {
            ShopManager shop = plugin.getShopManager();

            if (!shop.hasItem(category, String.valueOf(slot))) return;

            ItemStack item = shop.getItem(category, String.valueOf(slot));

            if (item == null) return;

            double price = shop.getSellPrice(category, String.valueOf(slot));

            if (price <= 0) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "&cItem ini tidak dapat dijual.");
                return;
            }

            ItemStack found = null;

            for (ItemStack content : player.getInventory().getStorageContents()) {
                if (content != null && content.isSimilar(item)) {
                    found = content;
                    break;
                }
            }

            if (found == null) {
                player.sendMessage("&cKamu tidak memiliki item tersebut.");
                return;
            }

            ItemStack selling = found.clone();
            selling.setAmount(1);

            if (found.getAmount() == 1) {
                player.getInventory().removeItem(found);
            } else {
                found.setAmount(found.getAmount() - 1);
            }

            if (plugin.getEconomyManager().deposit(player, price)) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "&aBerhasil menjual 1x " + item.getType().name() +
                        " seharga " + plugin.getEconomyManager().format(price));
                playSound(player, "sounds.sell");
            } else {
                player.getInventory().addItem(selling);
            }
        }
    }

    private void handleEditor(Player player, int slot) {
        switch (slot) {
            case 10 -> openCategoriesEditor(player);
            case 12 -> {
                plugin.reloadPlugin();
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("reloaded"));
                gui.openEditor(player);
            }
            case 14 -> openCategoriesEditor(player);
            case 16 -> player.closeInventory();
        }
    }

    private void openCategoriesEditor(Player player) {
        Inventory inventory = gui.create(
                "CATEGORIES_EDITOR", null, null,
                "&8✦ &bManage Categories", 54
        );

        int slot = 0;

        for (String category : plugin.getShopManager().getCategories()) {
            if (slot >= 45) break;

            inventory.setItem(slot, gui.button(
                    plugin.getShopManager().getCategoryIcon(category),
                    plugin.getShopManager().getCategoryName(category),
                    List.of("&7ID: " + category,
                            "&eLeft click: Edit",
                            "&cRight click: Delete")
            ));

            slot++;
        }

        inventory.setItem(49, gui.button(
                Material.EMERALD, "&aCreate Category",
                List.of("&7Click to create a new category")
        ));

        inventory.setItem(53, gui.button(
                Material.ARROW, "&cBack",
                List.of("&7Back to editor")
        ));

        player.openInventory(inventory);
    }

    private void handleCategoriesEditor(Player player, int slot) {
        if (slot == 49) {
            requestChat(player, "CREATE_CATEGORY");
            return;
        }

        if (slot == 53) {
            gui.openEditor(player);
            return;
        }

        List<String> categories =
                new ArrayList<>(plugin.getShopManager().getCategories());

        if (slot < 0 || slot >= categories.size() || slot >= 45) return;

        String category = categories.get(slot);

        // Find the action through the actual clicked category icon.
        if (player.getOpenInventory().getTopInventory().getItem(slot) == null) {
            return;
        }

        // Right click deletion is handled using the click type.
        // The category editor is opened by default.
        gui.openCategoryEditor(player, category);
    }

    private void handleCategoryEditor(Player player, MenuHolder holder,
                                      int slot, InventoryClickEvent event) {

        String category = holder.getCategory();
        ShopManager shop = plugin.getShopManager();

        if (slot == 49) {
            gui.openEditor(player);
            return;
        }

        if (slot == 53) {
            requestChat(player, "CATEGORY_SETTINGS:" + category);
            return;
        }

        if (slot == 45) {
            player.closeInventory();
            player.sendMessage("&ePegang item yang ingin ditambahkan.");
            player.sendMessage("&eKlik slot kosong di editor kategori.");
            return;
        }

        if (slot < 0 || slot >= 45) return;

        String itemSlot = String.valueOf(slot);

        if (shop.hasItem(category, itemSlot)) {
            if (event.isRightClick()) {
                shop.removeItem(category, itemSlot);
                player.sendMessage("&cItem berhasil dihapus.");
                gui.openCategoryEditor(player, category);
                return;
            }

            requestChat(player, "EDIT_PRICE:" + category + ":" + itemSlot);
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            player.sendMessage("&cPegang item terlebih dahulu!");
            return;
        }

        if (shop.addItem(category, slot, hand.clone(), 0, 0)) {
            player.sendMessage("&aItem ditambahkan. Harga awal Buy dan Sell = 0.");
            requestChat(player, "EDIT_PRICE:" + category + ":" + itemSlot);
        }

        gui.openCategoryEditor(player, category);
    }

    private void handleItemEditor(Player player, MenuHolder holder, int slot) {
        if (slot == 22) {
            gui.openCategoryEditor(player, holder.getCategory());
        }
    }

    private void handleSellClick(InventoryClickEvent event, Player player,
                                 MenuHolder holder, int slot) {

        if (slot == 49) {
            event.setCancelled(true);

            double total = 0;

            for (ItemStack item : event.getInventory().getContents()) {
                if (item == null || item.getType().isAir()) continue;
                if (slot == event.getInventory().first(item)) continue;

                total += plugin.getTransactionManager().calculateSellPrice(item);
            }

            if (total <= 0) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("no-items"));
                return;
            }

            double deposited = 0;

            for (int i = 0; i < 49; i++) {
                ItemStack item = event.getInventory().getItem(i);

                if (item == null || item.getType().isAir()) continue;

                double price = plugin.getTransactionManager()
                        .calculateSellPrice(item);

                if (price > 0) {
                    deposited += price;
                    event.getInventory().setItem(i, null);
                }
            }

            if (plugin.getEconomyManager().deposit(player, deposited)) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("sell-success")
                                .replace("{price}",
                                        plugin.getEconomyManager().format(deposited)));
                playSound(player, "sounds.sell");
                player.closeInventory();
            }

            return;
        }

        if (slot == 53) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        if (slot >= 49 || slot < 0) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(false);
            return;
        }

        event.setCancelled(false);

        if (event.isShiftClick()) {
            event.setCancelled(true);

            ItemStack moving = event.getCurrentItem();

            if (moving == null || moving.getType().isAir()) return;

            Map<Integer, ItemStack> leftover =
                    event.getView().getTopInventory().addItem(moving.clone());

            if (leftover.isEmpty()) {
                event.setCurrentItem(null);
            } else {
                ItemStack remainder = leftover.values().iterator().next();
                int moved = moving.getAmount() - remainder.getAmount();

                if (moved > 0) {
                    moving.setAmount(moving.getAmount() - moved);
                    event.setCurrentItem(moving);
                }
            }
        }
    }

    private void requestChat(Player player, String action) {
        chatActions.put(player.getUniqueId(), action);
        player.closeInventory();

        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "&eMasukkan nilai melalui chat.");
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "&7Ketik &ccancel &7untuk membatalkan.");
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
                player.sendMessage("&cDibatalkan.");
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
                player.sendMessage("&cID kategori tidak valid atau sudah digunakan.");
                return;
            }

            if (shop.createCategory(id, "&b" + input,
                    Material.CHEST, 0)) {
                player.sendMessage("&aKategori berhasil dibuat: " + id);
            }

            openCategoriesEditor(player);
            return;
        }

        if (action.startsWith("EDIT_PRICE:")) {
            String[] parts = action.split(":");

            if (parts.length != 3) return;

            String category = parts[1];
            String slot = parts[2];

            String[] prices = input.split("\\s+");

            if (prices.length != 2) {
                player.sendMessage("&cFormat: BUY SELL");
                return;
            }

            try {
                double buy = Double.parseDouble(prices[0]);
                double sell = Double.parseDouble(prices[1]);

                if (!Double.isFinite(buy) || !Double.isFinite(sell) ||
                        buy < 0 || sell < 0) {
                    throw new NumberFormatException();
                }

                shop.updatePrice(category, slot, "buy", buy);
                shop.updatePrice(category, slot, "sell", sell);

                player.sendMessage("&aHarga berhasil disimpan!");
                gui.openCategoryEditor(player, category);

            } catch (NumberFormatException exception) {
                player.sendMessage("&cFormat harga salah. Contoh: 100 50");
            }

            return;
        }

        if (action.startsWith("CATEGORY_SETTINGS:")) {
            String category = action.substring(
                    "CATEGORY_SETTINGS:".length()
            );

            String[] parts = input.split("\\|", 3);

            if (parts.length < 2) {
                player.sendMessage("&cFormat: NAMA | MATERIAL | SLOT");
                return;
            }

            try {
                String name = parts[0].trim();
                Material icon = Material.matchMaterial(parts[1].trim());
                int slot = parts.length >= 3
                        ? Integer.parseInt(parts[2].trim()) : 0;

                if (icon == null || slot < 0 || slot > 53) {
                    throw new IllegalArgumentException();
                }

                shop.setCategoryName(category, name);
                shop.setCategoryIcon(category, icon);
                shop.setCategorySlot(category, slot);

                player.sendMessage("&aKategori berhasil diperbarui!");
                gui.openCategoryEditor(player, category);

            } catch (IllegalArgumentException exception) {
                player.sendMessage("&cFormat salah. Contoh: &bBlocks | BRICKS | 20");
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof MenuHolder holder) {
            if (holder.getType().equals("SELL")) {
                Inventory inventory = event.getInventory();
                Player player = (Player) event.getPlayer();

                for (int i = 0; i < 49; i++) {
                    ItemStack item = inventory.getItem(i);

                    if (item == null || item.getType().isAir()) continue;

                    Map<Integer, ItemStack> leftover =
                            player.getInventory().addItem(item.clone());

                    leftover.values().forEach(
                            stack -> player.getWorld().dropItemNaturally(
                                    player.getLocation(), stack
                            )
                    );

                    inventory.setItem(i, null);
                }
            }
        }
    }

    private void playSound(Player player, String path) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) return;

        String sound = plugin.getConfig().getString(path);

        if (sound == null) return;

        try {
            player.playSound(player.getLocation(),
                    org.bukkit.Sound.valueOf(sound), 1f, 1f);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
