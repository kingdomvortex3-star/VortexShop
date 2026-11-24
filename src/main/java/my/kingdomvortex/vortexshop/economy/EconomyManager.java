
package my.kingdomvortex.vortexshop.economy;

import my.kingdomvortex.vortexshop.VortexShop;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final VortexShop plugin;
    private Economy economy;

    public EconomyManager(VortexShop plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            return false;
        }

        economy = provider.getProvider();
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null || amount < 0 || !has(player, amount)) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null || amount < 0) {
            return false;
        }

        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public double getBalance(Player player) {
        return economy == null ? 0 : economy.getBalance(player);
    }

    public String format(double amount) {
        String symbol = plugin.getConfig().getString(
                "settings.currency-symbol", "$"
        );

        String pattern = plugin.getConfig().getString(
                "settings.decimal-format", "#,##0.00"
        );

        return symbol + new java.text.DecimalFormat(pattern).format(amount);
    }

    public Economy getEconomy() {
        return economy;
    }
}
