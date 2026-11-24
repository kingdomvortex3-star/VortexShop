
package my.kingdomvortex.vortexshop.config;

import my.kingdomvortex.vortexshop.VortexShop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final VortexShop plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(VortexShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String path) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        return color(message);
    }

    public String getPrefix() {
        return color(plugin.getConfig().getString(
                "settings.prefix", "&8[&bVortexShop&8] "
        ));
    }

    public String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    public FileConfiguration getMessages() {
        return messages;
    }
}
