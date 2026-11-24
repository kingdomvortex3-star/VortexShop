
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

    public Inventory create(String type, String category, String slot,
                            String title, int size) {
        MenuHolder holder = new MenuHolder(type, category, slot);
        Inventory inventory = Bukkit.createInventory(
                holder, size, plugin.getConfigManager().color(title)
        );
        holder.setInventory(inventory);

        if (plugin.getConfig().getBoolean("gui.filler.enabled", true)) {
            Material filler = Material.matchMaterial(
                    plugin.getConfig().getString(
                            "gui.filler.material", "BLACK_STAINED_GLASS_PANE"
                    )
            );

            if (filler == null) filler = Material.BLACK_STAINED_GLASS_PANE;

            ItemStack item = new ItemStack(filler);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(plugin.getConfigManager().color(
                        plugin.getConfig().getString(
                                "gui.filler.name", " "
                        )
                ));
                item.setItemMeta(meta);
            }

            for (int i = 0; i < size; i++) {
                inventory.setItem(i, item);
            }
        }

        return inventory;
    }

    public ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().color(name));

            List<String> colored = new ArrayList<>();
            for (String line : lore) {
                colored.add(plugin.getConfigManager().color(line));
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
                plugin.getConfig().getString(
                        "gui.main.title", "&8VortexShop"
                ),
                54
        );

        for (String category : shop.getCategories()) {
            String permission = shop.getCategoryPermission(category);

            if (!permission.isEmpty() &&
                    !player.hasPermission(permission)) continue;

            int slot = shop.getCategorySlot(category);

            if (slot < 0 || slot >= 54) continue;

            ItemStack icon = button(
                    shop.getCategoryIcon(category),
                    shop.getCategoryName(category),
                    List.of("&7Click to open category")
            );

            inventory.setItem(slot, icon);
        }

        player.openInventory(inventory);
    }

    public void openCategory(Player player, String category) {
        ShopManager shop = plugin.getShopManager();

        if (!shop.hasCategory(category)) return;

        String permission = shop.getCategoryPermission(category);

        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            return;
        }

        Inventory inventory = create(
                "CATEGORY", category, null,
                plugin.getConfig().getString(
                        "gui.category.title", "&8{category}"
                ).replace("{category}", shop.getCategoryName(category)),
                54
        );

        for (String slot : shop.getItems(category)) {
            int index;

            try {
                index = Integer.parseInt(slot);
            } catch (NumberFormatException exception) {
                continue;
            }

            if (index < 0 || index >= 54) continue;

            ItemStack item = shop.getItem(category, slot);
            if (item == null) continue;

            String permissionItem = shop.getItemPermission(category, slot);

            if (!permissionItem.isEmpty() &&
                    !player.hasPermission(permissionItem)) continue;

            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                List<String> lore = meta.hasLore()
                        ? new ArrayList<>(meta.getLore())
                        : new ArrayList<>();

                lore.add("");
                lore.add(plugin.getConfigManager().color(
                        "&aBuy: &f" + plugin.getEconomyManager().format(
                                shop.getBuyPrice(category, slot)
                        )
                ));
                lore.add(plugin.getConfigManager().color(
                        "&cSell: &f" + plugin.getEconomyManager().format(
                                shop.getSellPrice(category, slot)
                        )
                ));
                lore.add(plugin.getConfigManager().color(
                        "&7Left Click: Buy 1"
                ));
                lore.add(plugin.getConfigManager().color(
                        "&7Shift Click: Buy 64"
                ));

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(index, item);
        }

        inventory.setItem(49, button(
                Material.ARROW, "&cBack",
                List.of("&7Return to shop")
        ));

        player.openInventory(inventory);
    }

    public void openEditor(Player player) {
        Inventory inventory = create(
                "EDITOR", null, null,
                "&8✦ &cVortexShop Editor", 27
        );

        inventory.setItem(10, button(
                Material.CHEST, "&bManage Categories",
                List.of("&7Create, delete and edit categories")
        ));

        inventory.setItem(12, button(
                Material.ANVIL, "&eReload Shop",
                List.of("&7Reload shop configuration")
        ));

        inventory.setItem(14, button(
                Material.EMERALD, "&aManage Prices",
                List.of("&7Edit item prices")
        ));

        inventory.setItem(16, button(
                Material.BARRIER, "&cClose",
                List.of("&7Close editor")
        ));

        player.openInventory(inventory);
    }

    public void openCategoryEditor(Player player, String category) {
        ShopManager shop = plugin.getShopManager();

        Inventory inventory = create(
                "CATEGORY_EDITOR", category, null,
                "&8Editing: " + shop.getCategoryName(category), 54
        );

        for (String slot : shop.getItems(category)) {
            try {
                int index = Integer.parseInt(slot);
                ItemStack item = shop.getItem(category, slot);

                if (index < 0 || index >= 54 || item == null) continue;

                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    List<String> lore = meta.hasLore()
                            ? new ArrayList<>(meta.getLore())
                            : new ArrayList<>();

                    lore.add("");
                    lore.add("&aBuy: " + shop.getBuyPrice(category, slot));
                    lore.add("&cSell: " + shop.getSellPrice(category, slot));
                    lore.add("&eLeft click: Edit prices");
                    lore.add("&cRight click: Remove item");

                    meta.setLore(lore.stream()
                            .map(plugin.getConfigManager()::color).toList());

                    item.setItemMeta(meta);
                }

                inventory.setItem(index, item);
            } catch (NumberFormatException ignored) {
            }
        }

        inventory.setItem(45, button(
                Material.HOPPER, "&aAdd Item",
                List.of("&7Hold an item and click an empty slot")
        ));

        inventory.setItem(49, button(
                Material.ARROW, "&cBack",
                List.of("&7Back to editor")
        ));

        inventory.setItem(53, button(
                Material.PAPER, "&eCategory Settings",
                List.of("&7Click to edit category settings")
        ));

        player.openInventory(inventory);
    }

    public void openSell(Player player) {
        Inventory inventory = create(
                "SELL", null, null,
                plugin.getConfig().getString(
                        "gui.sell.title", "&8SELL ITEMS"
                ),
                54
        );

        inventory.setItem(49, button(
                Material.EMERALD, "&aSell All",
                List.of("&7Sell all supported items", "&eClick to confirm")
        ));

        inventory.setItem(53, button(
                Material.BARRIER, "&cCancel",
                List.of("&7Return items to inventory")
        ));

        player.openInventory(inventory);
    }

    public VortexShop getPlugin() {
        return plugin;
    }
}
