
package my.kingdomvortex.vortexshop.gui;

import my.kingdomvortex.vortexshop.VortexShop;
import my.kingdomvortex.vortexshop.manager.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIManager {

    private final VortexShop plugin;

    public GUIManager(VortexShop plugin) {
        this.plugin = plugin;
    }

    public Inventory create(
            String type,
            String category,
            String slot,
            String title,
            int size
    ) {
        MenuHolder holder = new MenuHolder(type, category, slot);

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                plugin.getConfigManager().color(title)
        );

        holder.setInventory(inventory);

        if (plugin.getConfig().getBoolean("gui.filler.enabled", true)) {
            Material material = Material.matchMaterial(
                    plugin.getConfig().getString(
                            "gui.filler.material",
                            "BLACK_STAINED_GLASS_PANE"
                    )
            );

            if (material == null) {
                material = Material.BLACK_STAINED_GLASS_PANE;
            }

            ItemStack filler = button(
                    material,
                    plugin.getConfig().getString(
                            "gui.filler.name", " "
                    ),
                    List.of()
            );

            for (int i = 0; i < size; i++) {
                inventory.setItem(i, filler.clone());
            }
        }

        return inventory;
    }

    public ItemStack button(
            Material material,
            String name,
            List<String> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(
                    plugin.getConfigManager().color(name)
            );

            List<String> colored = new ArrayList<>();

            for (String line : lore) {
                colored.add(
                        plugin.getConfigManager().color(line)
                );
            }

            meta.setLore(colored);
            item.setItemMeta(meta);
        }

        return item;
    }

    public void openMain(Player player) {
        ShopManager shop = plugin.getShopManager();

        Inventory inventory = create(
                "MAIN", null, null,
                "&8✦ &bNOVATERRA SHOP", 54
        );

        for (String category : shop.getCategories()) {
            String permission =
                    shop.getCategoryPermission(category);

            if (!permission.isEmpty()
                    && !player.hasPermission(permission)) {
                continue;
            }

            int slot = shop.getCategorySlot(category);

            if (slot < 0 || slot >= 54) continue;

            inventory.setItem(
                    slot,
                    button(
                            shop.getCategoryIcon(category),
                            shop.getCategoryName(category),
                            List.of(
                                    "&7Browse category",
                                    "",
                                    "&eClick to open"
                            )
                    )
            );
        }

        player.openInventory(inventory);
    }

    public void openCategory(Player player, String category) {
        ShopManager shop = plugin.getShopManager();

        if (!shop.hasCategory(category)) return;

        String permission =
                shop.getCategoryPermission(category);

        if (!permission.isEmpty()
                && !player.hasPermission(permission)) {
            return;
        }

        Inventory inventory = create(
                "CATEGORY",
                category,
                null,
                "&8✦ " + shop.getCategoryName(category),
                54
        );

        for (String itemSlot : shop.getItems(category)) {
            int index;

            try {
                index = Integer.parseInt(itemSlot);
            } catch (NumberFormatException exception) {
                continue;
            }

            if (index < 0 || index >= 45) continue;

            ItemStack item = shop.getItem(category, itemSlot);

            if (item == null) continue;

            String permissionItem =
                    shop.getItemPermission(category, itemSlot);

            if (!permissionItem.isEmpty()
                    && !player.hasPermission(permissionItem)) {
                continue;
            }

            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                List<String> lore = meta.hasLore()
                        ? new ArrayList<>(meta.getLore())
                        : new ArrayList<>();

                lore.add("");
                lore.add("&aBuy: &f" +
                        plugin.getEconomyManager().format(
                                shop.getBuyPrice(category, itemSlot)
                        ));
                lore.add("&cSell: &f" +
                        plugin.getEconomyManager().format(
                                shop.getSellPrice(category, itemSlot)
                        ));
                lore.add("");
                lore.add("&eClick to purchase");

                meta.setLore(
                        lore.stream()
                                .map(plugin.getConfigManager()::color)
                                .toList()
                );

                item.setItemMeta(meta);
            }

            inventory.setItem(index, item);
        }

        inventory.setItem(
                49,
                button(
                        Material.ARROW,
                        "&cBack",
                        List.of("&7Return to shop")
                )
        );

        player.openInventory(inventory);
    }

    public void openEditor(Player player) {
        Inventory inventory = create(
                "EDITOR", null, null,
                "&8✦ &cVortexShop Editor", 27
        );

        inventory.setItem(10, button(
                Material.CHEST,
                "&bManage Categories",
                List.of("&7Create, edit and delete categories")
        ));

        inventory.setItem(12, button(
                Material.ANVIL,
                "&eReload Shop",
                List.of("&7Reload shop configuration")
        ));

        inventory.setItem(14, button(
                Material.BOOK,
                "&aShop Preview",
                List.of("&7Preview player shop")
        ));

        inventory.setItem(16, button(
                Material.BARRIER,
                "&cClose",
                List.of("&7Close editor")
        ));

        player.openInventory(inventory);
    }

    public void openCategoriesEditor(Player player) {
        ShopManager shop = plugin.getShopManager();

        Inventory inventory = create(
                "CATEGORIES_EDITOR",
                null,
                null,
                "&8✦ &bCategory Management",
                54
        );

        int slot = 0;

        for (String category : shop.getCategories()) {
            if (slot >= 45) break;

            inventory.setItem(
                    slot,
                    button(
                            shop.getCategoryIcon(category),
                            shop.getCategoryName(category),
                            List.of(
                                    "&7ID: &f" + category,
                                    "",
                                    "&eLeft click: Edit",
                                    "&cRight click: Delete"
                            )
                    )
            );

            slot++;
        }

        inventory.setItem(49, button(
                Material.EMERALD,
                "&aCreate Category",
                List.of("&7Click to create category")
        ));

        inventory.setItem(53, button(
                Material.ARROW,
                "&cBack",
                List.of("&7Return to editor")
        ));

        player.openInventory(inventory);
    }

    public void openCategoryEditor(
            Player player,
            String category
    ) {
        ShopManager shop = plugin.getShopManager();

        if (!shop.hasCategory(category)) return;

        Inventory inventory = create(
                "CATEGORY_EDITOR",
                category,
                null,
                "&8✦ &b" + shop.getCategoryName(category),
                54
        );

        for (String itemSlot : shop.getItems(category)) {
            int index;

            try {
                index = Integer.parseInt(itemSlot);
            } catch (NumberFormatException exception) {
                continue;
            }

            if (index < 0 || index >= 45) continue;

            ItemStack item = shop.getItem(category, itemSlot);

            if (item == null) continue;

            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                List<String> lore = meta.hasLore()
                        ? new ArrayList<>(meta.getLore())
                        : new ArrayList<>();

                lore.add("");
                lore.add("&aBuy: &f" +
                        shop.getBuyPrice(category, itemSlot));
                lore.add("&cSell: &f" +
                        shop.getSellPrice(category, itemSlot));
                lore.add("");
                lore.add("&eLeft click: Edit item");
                lore.add("&cRight click: Remove item");

                meta.setLore(
                        lore.stream()
                                .map(plugin.getConfigManager()::color)
                                .toList()
                );

                item.setItemMeta(meta);
            }

            inventory.setItem(index, item);
        }

        inventory.setItem(49, button(
                Material.ARROW,
                "&cBack",
                List.of("&7Return to categories")
        ));

        inventory.setItem(51, button(
                Material.NAME_TAG,
                "&eCategory Settings",
                List.of(
                        "&7Rename category",
                        "&7Change icon",
                        "&7Change slot",
                        "&7Set permission"
                )
        ));

        inventory.setItem(53, button(
                Material.EMERALD,
                "&aAdd Item",
                List.of(
                        "&7Hold an item in your hand",
                        "&7Click an empty category slot"
                )
        ));

        player.openInventory(inventory);
    }

    public void openItemEditor(
            Player player,
            String category,
            String slot
    ) {
        ShopManager shop = plugin.getShopManager();

        ItemStack item = shop.getItem(category, slot);

        if (item == null) return;

        Inventory inventory = create(
                "ITEM_EDITOR",
                category,
                slot,
                "&8✦ &eItem Editor",
                27
        );

        inventory.setItem(4, item.clone());

        inventory.setItem(10, button(
                Material.EMERALD,
                "&aBuy Price",
                List.of(
                        "&7Current: " +
                                shop.getBuyPrice(category, slot)
                )
        ));

        inventory.setItem(12, button(
                Material.GOLD_INGOT,
                "&6Sell Price",
                List.of(
                        "&7Current: " +
                                shop.getSellPrice(category, slot)
                )
        ));

        inventory.setItem(14, button(
                Material.NAME_TAG,
                "&bDisplay Name",
                List.of("&7Change item name")
        ));

        inventory.setItem(16, button(
                Material.BOOK,
                "&dLore",
                List.of("&7Edit item lore")
        ));

        inventory.setItem(20, button(
                Material.TRIPWIRE_HOOK,
                "&6Permission",
                List.of(
                        "&7Current: " +
                                shop.getItemPermission(category, slot),
                        "&eClick to edit"
                )
        ));

        inventory.setItem(22, button(
                Material.ENDER_PEARL,
                "&eChange Slot",
                List.of("&7Move item to another slot")
        ));

        inventory.setItem(26, button(
                Material.BARRIER,
                "&cDelete Item",
                List.of("&7Permanently remove item")
        ));

        inventory.setItem(18, button(
                Material.ARROW,
                "&cBack",
                List.of("&7Return to category editor")
        ));

        player.openInventory(inventory);
    }

    public void openQuantity(
            Player player,
            String category,
            String slot
    ) {
        ShopManager shop = plugin.getShopManager();

        ItemStack item = shop.getItem(category, slot);

        if (item == null) return;

        Inventory inventory = create(
                "QUANTITY",
                category,
                slot,
                "&8✦ &bSelect Quantity",
                27
        );

        inventory.setItem(4, item.clone());

        int[] amounts = {1, 16, 32, 64};
        int[] positions = {10, 12, 14, 16};

        for (int i = 0; i < amounts.length; i++) {
            int amount = amounts[i];

            inventory.setItem(
                    positions[i],
                    button(
                            Material.PAPER,
                            "&aBuy " + amount,
                            List.of(
                                    "&7Total: &f" +
                                            plugin.getEconomyManager().format(
                                                    shop.getBuyPrice(category, slot)
                                                            * amount
                                            ),
                                    "",
                                    "&eClick to continue"
                            )
                    )
            );
        }

        inventory.setItem(22, button(
                Material.ARROW,
                "&cBack",
                List.of("&7Return to shop")
        ));

        player.openInventory(inventory);
    }

    public void openConfirm(
            Player player,
            String category,
            String slot,
            int amount
    ) {
        ShopManager shop = plugin.getShopManager();

        ItemStack item = shop.getItem(category, slot);

        if (item == null) return;

        Inventory inventory = create(
                "BUY_CONFIRM",
                category,
                slot,
                "&8✦ &eConfirm Purchase",
                27
        );

        inventory.setItem(13, item.clone());

        inventory.setItem(11, button(
                Material.LIME_CONCRETE,
                "&aConfirm",
                List.of(
                        "&7Amount: &f" + amount,
                        "&7Total: &f" +
                                plugin.getEconomyManager().format(
                                        shop.getBuyPrice(category, slot)
                                                * amount
                                )
                )
        ));

        inventory.setItem(15, button(
                Material.RED_CONCRETE,
                "&cCancel",
                List.of("&7Cancel purchase")
        ));

        player.openInventory(inventory);
    }

    public void openSell(Player player) {
        Inventory inventory = create(
                "SELL",
                null,
                null,
                "&8✦ &cSELL ITEMS",
                54
        );

        for (int i = 0; i < 49; i++) {
            inventory.setItem(i, null);
        }

        inventory.setItem(49, button(
                Material.EMERALD,
                "&aSELL ALL",
                List.of(
                        "&7Sell all supported items",
                        "",
                        "&eClick to sell"
                )
        ));

        inventory.setItem(53, button(
                Material.BARRIER,
                "&cClose",
                List.of("&7Return items and close")
        ));

        player.openInventory(inventory);
    }
}
