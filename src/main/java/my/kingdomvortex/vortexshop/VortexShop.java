
package my.kingdomvortex.vortexshop;

import my.kingdomvortex.vortexshop.command.ShopCommand;
import my.kingdomvortex.vortexshop.command.SellCommand;
import my.kingdomvortex.vortexshop.config.ConfigManager;
import my.kingdomvortex.vortexshop.economy.EconomyManager;
import my.kingdomvortex.vortexshop.gui.GUIListener;
import my.kingdomvortex.vortexshop.manager.ShopManager;
import my.kingdomvortex.vortexshop.manager.TransactionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class VortexShop extends JavaPlugin {

    private static VortexShop instance;

    private ConfigManager configManager;
    private EconomyManager economyManager;
    private ShopManager shopManager;
    private TransactionManager transactionManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("shops/default.yml");

        configManager = new ConfigManager(this);
        economyManager = new EconomyManager(this);

        if (!economyManager.setup()) {
            getLogger().severe("Vault Economy tidak ditemukan!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shopManager = new ShopManager(this);
        transactionManager = new TransactionManager(this);

        shopManager.load();

        ShopCommand shopCommand = new ShopCommand(this);
        SellCommand sellCommand = new SellCommand(this);

        getCommand("shop").setExecutor(shopCommand);
        getCommand("shop").setTabCompleter(shopCommand);

        getCommand("sell").setExecutor(sellCommand);

        getCommand("vortexshop").setExecutor(shopCommand);
        getCommand("vortexshop").setTabCompleter(shopCommand);

        getServer().getPluginManager().registerEvents(
                new GUIListener(this), this
        );

        getLogger().info("==================================");
        getLogger().info(" VortexShop v1.0.0 ENABLED");
        getLogger().info(" Vault Economy Connected");
        getLogger().info(" GUI Shop System Ready");
        getLogger().info("==================================");
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.save();
        }

        getLogger().info("VortexShop disabled.");
    }

    private void saveResourceIfMissing(String resource) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        if (!new java.io.File(getDataFolder(), resource).exists()) {
            saveResource(resource, false);
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        shopManager.load();
    }

    public static VortexShop getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
}
