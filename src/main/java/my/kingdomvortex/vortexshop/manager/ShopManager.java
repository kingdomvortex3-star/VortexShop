
package my.kingdomvortex.vortexshop.manager;

import my.kingdomvortex.vortexshop.VortexShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
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
        if (!folder.exists()) folder.mkdirs();

        file = new File(folder, "default.yml");

        if (!file.exists()) {
            plugin.saveResource("shops/default.yml", false);
        }

        data = YamlConfiguration.loadConfiguration(file);
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
        ConfigurationSection section = data.getConfigurationSection("categories");
        return section == null ? new LinkedHashSet<>() : section.getKeys(false);
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
        return data.getString("categories." + id + ".permission", "");
    }

    public boolean createCategory(String id, String name, Material icon, int slot) {
        if (id == null || id.isBlank() || getCategories().contains(id)) {
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
        if (!getCategories().contains(id)) return false;

        data.set("categories." + id, null);
        save();
        return true;
    }

    public boolean hasCategory(String id) {
        return getCategories().contains(id);
    }

    public Set<String> getItems(String category) {
        ConfigurationSection section =
                data.getConfigurationSection("categories." + category + ".items");

        return section == null ? new LinkedHashSet<>() : section.getKeys(false);
    }

    public ItemStack getItem(String category, String slot) {
        String path = "categories." + category + ".items." + slot;

        Material material = Material.matchMaterial(
                data.getString(path + ".material", "STONE")
        );

        if (material == null) return null;

        ItemStack item = new ItemStack(
                material,
                Math.max(1, data.getInt(path + ".amount", 1))
        );

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = data.getString(path + ".name", "");

            if (!name.isEmpty()) {
                meta.setDisplayName(plugin.getConfigManager().color(name));
            }

            List<String> lore = data.getStringList(path + ".lore");

            if (!lore.isEmpty()) {
                meta.setLore(lore.stream()
                        .map(plugin.getConfigManager()::color)
                        .toList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public double getBuyPrice(String category, String slot) {
        return data.getDouble(
                "categories." + category + ".items." + slot + ".buy", -1
        );
    }

    public double getSellPrice(String category, String slot) {
        return data.getDouble(
                "categories." + category + ".items." + slot + ".sell", -1
        );
    }

    public String getItemPermission(String category, String slot) {
        return data.getString(
                "categories." + category + ".items." + slot + ".permission", ""
        );
    }

    public boolean addItem(String category, int slot, ItemStack item,
                            double buy, double sell) {

        if (!hasCategory(category) || item == null ||
                item.getType().isAir() || slot < 0 || slot > 53 ||
                !Double.isFinite(buy) || !Double.isFinite(sell) ||
                buy < 0 || sell < 0) {
            return false;
        }

        String path = "categories." + category + ".items." + slot;

        data.set(path + ".material", item.getType().name());
        data.set(path + ".amount", item.getAmount());
        data.set(path + ".name",
                item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : "");
        data.set(path + ".lore",
                item.hasItemMeta() && item.getItemMeta().hasLore()
                        ? item.getItemMeta().getLore()
                        : new ArrayList<>());
        data.set(path + ".buy", buy);
        data.set(path + ".sell", sell);
        data.set(path + ".permission", "");

        save();
        return true;
    }

    public boolean updatePrice(String category, String slot,
                                String type, double price) {
        if (!hasItem(category, slot) ||
                !Double.isFinite(price) || price < 0 ||
                (!type.equalsIgnoreCase("buy") &&
                 !type.equalsIgnoreCase("sell"))) {
            return false;
        }

        data.set("categories." + category + ".items." + slot + "." +
                type.toLowerCase(Locale.ROOT), price);

        save();
        return true;
    }

    public boolean hasItem(String category, String slot) {
        return data.contains("categories." + category + ".items." + slot);
    }

    public boolean removeItem(String category, String slot) {
        if (!hasItem(category, slot)) return false;

        data.set("categories." + category + ".items." + slot, null);
        save();
        return true;
    }

    public void setCategoryPermission(String category, String permission) {
        data.set("categories." + category + ".permission", permission);
        save();
    }

    public void setItemPermission(String category, String slot, String permission) {
        if (!hasItem(category, slot)) return;

        data.set("categories." + category + ".items." + slot + ".permission",
                permission);
        save();
    }

    public void setItemName(String category, String slot, String name) {
        if (!hasItem(category, slot)) return;

        data.set("categories." + category + ".items." + slot + ".name", name);
        save();
    }

    public void setItemLore(String category, String slot, List<String> lore) {
        if (!hasItem(category, slot)) return;

        data.set("categories." + category + ".items." + slot + ".lore", lore);
        save();
    }

    public void setCategoryName(String category, String name) {
        if (!hasCategory(category)) return;

        data.set("categories." + category + ".name", name);
        save();
    }

    public void setCategoryIcon(String category, Material material) {
        if (!hasCategory(category)) return;

        data.set("categories." + category + ".icon", material.name());
        save();
    }

    public void setCategorySlot(String category, int slot) {
        if (!hasCategory(category) || slot < 0 || slot > 53) return;

        data.set("categories." + category + ".slot", slot);
        save();
    }

    public void setItemSlot(String category, String oldSlot, int newSlot) {
        if (!hasItem(category, oldSlot) || newSlot < 0 || newSlot > 53 ||
                hasItem(category, String.valueOf(newSlot))) return;

        String oldPath = "categories." + category + ".items." + oldSlot;
        String newPath = "categories." + category + ".items." + newSlot;

        data.set(newPath, data.getConfigurationSection(oldPath).getValues(true));
        data.set(oldPath, null);
        save();
    }
}
