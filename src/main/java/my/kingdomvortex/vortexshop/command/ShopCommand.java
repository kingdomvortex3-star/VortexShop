
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
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        String name = command.getName().toLowerCase();

        if (name.equals("sr")) {
            if (!sender.hasPermission("vortexshop.admin")) {
                sender.sendMessage("&cNo permission!");
                return true;
            }

            plugin.reloadPlugin();
            sender.sendMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMessage("reloaded"));
            return true;
        }

        if (name.equals("se")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only!");
                return true;
            }

            if (!player.hasPermission("vortexshop.admin")) {
                player.sendMessage("&cNo permission!");
                return true;
            }

            gui.openEditor(player);
            return true;
        }

        if (name.equals("shop")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only!");
                return true;
            }

            if (args.length > 0) {
                String sub = args[0].toLowerCase();

                if (sub.equals("edit")) {
                    if (!player.hasPermission("vortexshop.admin")) {
                        player.sendMessage("&cNo permission!");
                        return true;
                    }

                    gui.openEditor(player);
                    return true;
                }

                if (sub.equals("reload")) {
                    if (!player.hasPermission("vortexshop.admin")) {
                        player.sendMessage("&cNo permission!");
                        return true;
                    }

                    plugin.reloadPlugin();
                    player.sendMessage(plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().getMessage("reloaded"));
                    return true;
                }

                player.sendMessage("&cUsage: /shop [edit|reload]");
                return true;
            }

            if (!player.hasPermission("vortexshop.use")) {
                player.sendMessage("&cNo permission!");
                return true;
            }

            gui.openMain(player);
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        List<String> result = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("shop")
                && sender.hasPermission("vortexshop.admin")
                && args.length == 1) {

            for (String value : List.of("edit", "reload")) {
                if (value.startsWith(args[0].toLowerCase())) {
                    result.add(value);
                }
            }
        }

        return result;
    }
}
