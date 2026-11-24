
package my.kingdomvortex.vortexshop;

import my.kingdomvortex.vortexshop.command.ShopCommand;
import my.kingdomvortex.vortexshop.command.SellCommand;
import my.kingdomvortex.vortexshop.config.ConfigManager;
import my.kingdomvortex.vortexshop.economy.EconomyManager;
import my.kingdomvortex.vortexshop.gui.GUIListener;
import my.kingdomvortex.vortexshop.gui.SellGuardListener;
import my.kingdomvortex.vortexshop.manager.ShopManager;
import my.kingdomvortex.vortexshop.manager.TransactionManager;
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
        getCommand("se").setExecutor(shopCommand);
        getCommand("sr").setExecutor(shopCommand);
        getCommand("sell").setExecutor(sellCommand);

        getServer().getPluginManager().registerEvents(
                new GUIListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new SellGuardListener(this),
                this
        );

        getLogger().info("==================================");
        getLogger().info(" VortexShop v1.0.0 ENABLED");
        getLogger().info(" Vault Economy Connected");
        getLogger().info(" GUI Shop System Ready");
        getLogger().info(" /shop");
        getLogger().info(" /shop edit");
        getLogger().info(" /shop reload");
        getLogger().info(" /se");
        getLogger().info(" /sr");
        getLogger().info(" /sell");
        getLogger().info("==================================");
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.save();
        }

        getLogger().info("VortexShop disabled.");
        instance = null;
    }

    private void saveResourceIfMissing(String resource) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        java.io.File file = new java.io.File(
                getDataFolder(),
                resource
        );

        if (!file.exists()) {
            saveResource(resource, false);
        }
    }

    public void reloadPlugin() {
        reloadConfig();

        if (configManager != null) {
            configManager.reload();
        }

        if (shopManager != null) {
            shopManager.load();
        }

        getLogger().info("VortexShop configuration reloaded.");
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
