
package my.kingdomvortex.vortexshop.command;

import my.kingdomvortex.vortexshop.VortexShop;
import my.kingdomvortex.vortexshop.gui.GUIManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final VortexShop plugin;
    private final GUIManager gui;

    public ShopCommand(VortexShop plugin) {
        this.plugin = plugin;
        this.gui = new GUIManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        boolean adminShortcut = label.equalsIgnoreCase("se");
        boolean reloadShortcut = label.equalsIgnoreCase("sr");

        if (reloadShortcut) {
            if (!sender.hasPermission("vortexshop.admin")) {
                sender.sendMessage("&cNo permission!");
                return true;
            }

            plugin.reloadPlugin();
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("reloaded"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command is for players only.");
                return true;
            }

            if (adminShortcut) {
                if (!player.hasPermission("vortexshop.admin")) {
                    player.sendMessage("&cNo permission!");
                    return true;
                }

                gui.openEditor(player);
                return true;
            }

            if (!player.hasPermission("vortexshop.use")) {
                player.sendMessage("&cNo permission!");
                return true;
            }

            gui.openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!sender.hasPermission("vortexshop.admin")) {
                sender.sendMessage("&cNo permission!");
                return true;
            }

            plugin.reloadPlugin();

            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("reloaded"));
            return true;
        }

        if (sub.equals("edit")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command is for players only.");
                return true;
            }

            if (!player.hasPermission("vortexshop.admin")) {
                player.sendMessage("&cNo permission!");
                return true;
            }

            gui.openEditor(player);
            return true;
        }

        sender.sendMessage("&bVortexShop Commands");
        sender.sendMessage("&7/shop &f- Open shop");
        sender.sendMessage("&7/shop edit &f- Open editor");
        sender.sendMessage("&7/shop reload &f- Reload configuration");
        sender.sendMessage("&7/se &f- Shop editor");
        sender.sendMessage("&7/sr &f- Reload shop");
        sender.sendMessage("&7/sell &f- Open sell GUI");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {

        List<String> result = new ArrayList<>();

        if (!sender.hasPermission("vortexshop.admin")) {
            return result;
        }

        if (args.length == 1) {
            for (String value : List.of("edit", "reload")) {
                if (value.startsWith(args[0].toLowerCase())) {
                    result.add(value);
                }
            }
        }

        return result;
    }
}
