
package my.kingdomvortex.vortexshop.manager;

import my.kingdomvortex.vortexshop.VortexShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ShopManager {

    private final VortexShop plugin;
    private File file;
    private YamlConfiguration data;

    public ShopManager(VortexShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File folder = new File(plugin.getDataFolder(), "shops");

        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("Cannot create shops folder!");
            return;
        }

        file = new File(folder, "default.yml");

        if (!file.exists()) {
            plugin.saveResource("shops/default.yml", false);
        }

        data = YamlConfiguration.loadConfiguration(file);

        if (!data.contains("categories")) {
            data.createSection("categories");
        }

        mergeDefaultCatalog();
        logShopInformation();
    }

    private void mergeDefaultCatalog() {
        int addedCategories = 0;
        int addedItems = 0;

        try (InputStream stream =
                     plugin.getResource("shops/default.yml")) {

            if (stream == null) {
                plugin.getLogger().severe(
                        "shops/default.yml resource is missing!"
                );
                return;
            }

            YamlConfiguration defaults =
                    YamlConfiguration.loadConfiguration(
                            new InputStreamReader(
                                    stream,
                                    StandardCharsets.UTF_8
                            )
                    );

            ConfigurationSection defaultCategories =
                    defaults.getConfigurationSection("categories");

            if (defaultCategories == null) return;

            for (String category :
                    defaultCategories.getKeys(false)) {

                String categoryPath = "categories." + category;

                if (!data.contains(categoryPath)) {
                    data.createSection(categoryPath);
                    addedCategories++;
                }

                copyIfMissing(defaults, categoryPath + ".name");
                copyIfMissing(defaults, categoryPath + ".icon");
                copyIfMissing(defaults, categoryPath + ".slot");
                copyIfMissing(defaults, categoryPath + ".permission");

                String itemsPath = categoryPath + ".items";

                ConfigurationSection defaultItems =
                        defaults.getConfigurationSection(itemsPath);

                if (defaultItems == null) continue;

                if (!data.contains(itemsPath)) {
                    data.createSection(itemsPath);
                }

                for (String itemSlot :
                        defaultItems.getKeys(false)) {

                    String target =
                            itemsPath + "." + itemSlot;

                    if (data.contains(target)) continue;

                    ConfigurationSection item =
                            defaults.getConfigurationSection(target);

                    if (item == null) continue;

                    data.set(target, item.getValues(true));
                    addedItems++;
                }
            }

            if (addedCategories > 0 || addedItems > 0) {
                save();
            }

            plugin.getLogger().info(
                    "Default catalog migration completed: "
                            + addedCategories + " categories, "
                            + addedItems + " items added."
            );

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Default catalog migration failed: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }

    private void copyIfMissing(
            YamlConfiguration defaults,
            String path
    ) {
        if (!data.contains(path) && defaults.contains(path)) {
            data.set(path, defaults.get(path));
        }
    }

    private void logShopInformation() {
        plugin.getLogger().info("----------------------------------");
        plugin.getLogger().info("VortexShop Shop Data");
        plugin.getLogger().info("File: " + file.getAbsolutePath());
        plugin.getLogger().info(
                "Loaded categories: " + getCategories().size()
        );

        for (String category : getCategories()) {
            plugin.getLogger().info(
                    " - " + category
                            + " (" + getItems(category).size() + " items)"
            );
        }

        plugin.getLogger().info("----------------------------------");
    }

    public void save() {
        if (data == null || file == null) return;

        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Failed to save shop: " + exception.getMessage()
            );
        }
    }

    public Set<String> getCategories() {
        if (data == null) return new LinkedHashSet<>();

        ConfigurationSection section =
                data.getConfigurationSection("categories");

        if (section == null) return new LinkedHashSet<>();

        return new LinkedHashSet<>(section.getKeys(false));
    }

    public String getCategoryName(String id) {
        return data.getString("categories." + id + ".name", id);
    }

    public Material getCategoryIcon(String id) {
        Material material = Material.matchMaterial(
                data.getString("categories." + id + ".icon", "CHEST")
        );

        return material == null ? Material.CHEST : material;
    }

    public int getCategorySlot(String id) {
        return data.getInt("categories." + id + ".slot", 0);
    }

    public String getCategoryPermission(String id) {
        return data.getString(
                "categories." + id + ".permission", ""
        );
    }

    public boolean createCategory(
            String id,
            String name,
            Material icon,
            int slot
    ) {
        if (data == null
                || id == null
                || id.isBlank()
                || name == null
                || icon == null
                || slot < 0
                || slot >= 54
                || hasCategory(id)) {
            return false;
        }

        String path = "categories." + id;

        data.set(path + ".name", name);
        data.set(path + ".icon", icon.name());
        data.set(path + ".slot", slot);
        data.set(path + ".permission", "");
        data.createSection(path + ".items");

        save();
        return true;
    }

    public boolean deleteCategory(String id) {
        if (!hasCategory(id)) return false;

        data.set("categories." + id, null);
        save();
        return true;
    }

    public boolean hasCategory(String id) {
        return id != null && getCategories().contains(id);
    }

    public void setCategoryName(String category, String name) {
        if (!hasCategory(category)
                || name == null
                || name.isBlank()) return;

        data.set("categories." + category + ".name", name);
        save();
    }

    public void setCategoryIcon(String category, Material material) {
        if (!hasCategory(category) || material == null) return;

        data.set(
                "categories." + category + ".icon",
                material.name()
        );
        save();
    }

    public void setCategorySlot(String category, int slot) {
        if (!hasCategory(category) || slot < 0 || slot >= 54) return;

        for (String other : getCategories()) {
            if (!other.equals(category)
                    && getCategorySlot(other) == slot) {
                return;
            }
        }

        data.set("categories." + category + ".slot", slot);
        save();
    }

    public void setCategoryPermission(
            String category,
            String permission
    ) {
        if (!hasCategory(category)) return;

        data.set(
                "categories." + category + ".permission",
                permission == null ? "" : permission
        );
        save();
    }

    public Set<String> getItems(String category) {
        if (!hasCategory(category)) return new LinkedHashSet<>();

        ConfigurationSection section =
                data.getConfigurationSection(
                        "categories." + category + ".items"
                );

        if (section == null) return new LinkedHashSet<>();

        return new LinkedHashSet<>(section.getKeys(false));
    }

    public ItemStack getItem(String category, String slot) {
        if (!hasItem(category, slot)) return null;

        String path =
                "categories." + category + ".items." + slot;

        Material material = Material.matchMaterial(
                data.getString(path + ".material", "STONE")
        );

        if (material == null || material.isAir()) return null;

        int amount = Math.max(1, data.getInt(path + ".amount", 1));

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = data.getString(path + ".name", "");

            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(
                        plugin.getConfigManager().color(name)
                );
            }

            List<String> lore = data.getStringList(path + ".lore");

            if (!lore.isEmpty()) {
                meta.setLore(
                        lore.stream()
                                .map(plugin.getConfigManager()::color)
                                .toList()
                );
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public double getBuyPrice(String category, String slot) {
        return data.getDouble(
                "categories." + category + ".items." + slot + ".buy",
                -1
        );
    }

    public double getSellPrice(String category, String slot) {
        return data.getDouble(
                "categories." + category + ".items." + slot + ".sell",
                -1
        );
    }

    public String getItemPermission(String category, String slot) {
        return data.getString(
                "categories." + category + ".items." + slot + ".permission",
                ""
        );
    }

    public boolean hasItem(String category, String slot) {
        return hasCategory(category)
                && slot != null
                && data.contains(
                        "categories." + category + ".items." + slot
                );
    }

    public boolean addItem(
            String category,
            int slot,
            ItemStack item,
            double buy,
            double sell
    ) {
        if (!hasCategory(category)
                || item == null
                || item.getType().isAir()
                || slot < 0
                || slot >= 45
                || hasItem(category, String.valueOf(slot))
                || !Double.isFinite(buy)
                || !Double.isFinite(sell)
                || buy < 0
                || sell < 0) {
            return false;
        }

        String path =
                "categories." + category + ".items." + slot;

        data.set(path + ".material", item.getType().name());
        data.set(path + ".amount", item.getAmount());

        ItemMeta meta = item.getItemMeta();

        data.set(
                path + ".name",
                meta != null && meta.hasDisplayName()
                        ? meta.getDisplayName() : ""
        );

        data.set(
                path + ".lore",
                meta != null && meta.hasLore()
                        ? meta.getLore() : new ArrayList<>()
        );

        data.set(path + ".buy", buy);
        data.set(path + ".sell", sell);
        data.set(path + ".permission", "");

        save();
        return true;
    }

    public boolean updatePrice(
            String category,
            String slot,
            String type,
            double price
    ) {
        if (!hasItem(category, slot)
                || !Double.isFinite(price)
                || price < 0
                || type == null
                || (!type.equalsIgnoreCase("buy")
                && !type.equalsIgnoreCase("sell"))) {
            return false;
        }

        data.set(
                "categories." + category + ".items." + slot + "."
                        + type.toLowerCase(Locale.ROOT),
                price
        );

        save();
        return true;
    }

    public boolean removeItem(String category, String slot) {
        if (!hasItem(category, slot)) return false;

        data.set(
                "categories." + category + ".items." + slot,
                null
        );

        save();
        return true;
    }

    public void setItemPermission(
            String category,
            String slot,
            String permission
    ) {
        if (!hasItem(category, slot)) return;

        data.set(
                "categories." + category + ".items." + slot + ".permission",
                permission == null ? "" : permission
        );

        save();
    }

    public void setItemName(
            String category,
            String slot,
            String name
    ) {
        if (!hasItem(category, slot)) return;

        data.set(
                "categories." + category + ".items." + slot + ".name",
                name == null ? "" : name
        );

        save();
    }

    public void setItemLore(
            String category,
            String slot,
            List<String> lore
    ) {
        if (!hasItem(category, slot)) return;

        data.set(
                "categories." + category + ".items." + slot + ".lore",
                lore == null ? new ArrayList<>() : lore
        );

        save();
    }

    public void setItemSlot(
            String category,
            String oldSlot,
            int newSlot
    ) {
        if (!hasItem(category, oldSlot)
                || newSlot < 0
                || newSlot >= 45) {
            return;
        }

        String newSlotString = String.valueOf(newSlot);

        if (hasItem(category, newSlotString)) return;

        String oldPath =
                "categories." + category + ".items." + oldSlot;

        String newPath =
                "categories." + category + ".items." + newSlotString;

        ConfigurationSection section =
                data.getConfigurationSection(oldPath);

        if (section == null) return;

        data.set(newPath, section.getValues(true));
        data.set(oldPath, null);

        save();
    }

    public File getShopFile() {
        return file;
    }

    public int getCategoryCount() {
        return getCategories().size();
    }
}
