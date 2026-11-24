
package my.kingdomvortex.vortexshop.command;

import my.kingdomvortex.vortexshop.VortexShop;
import my.kingdomvortex.vortexshop.gui.GUIManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellCommand implements CommandExecutor {

    private final VortexShop plugin;
    private final GUIManager gui;

    public SellCommand(VortexShop plugin) {
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("vortexshop.sell")) {
            player.sendMessage(
                    plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager()
                            .getMessage("no-permission")
            );
            return true;
        }

        gui.openSell(player);
        return true;
    }
}
