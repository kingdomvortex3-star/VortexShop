
package my.kingdomvortex.vortexshop.gui;

import my.kingdomvortex.vortexshop.VortexShop;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SellGuardListener implements Listener {

    private final VortexShop plugin;

    public SellGuardListener(VortexShop plugin) {
        this.plugin = plugin;
    }

    private boolean isSellInventory(Inventory inventory) {
        return inventory != null
                && inventory.getHolder(false) instanceof MenuHolder holder
                && holder.getType().equals("SELL");
    }

    private boolean isSellable(Player player, ItemStack item) {
        return item != null
                && !item.getType().isAir()
                && plugin.getTransactionManager()
                .isSellable(player, item);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();

        if (!isSellInventory(top)) return;

        int rawSlot = event.getRawSlot();
        int topSize = top.getSize();

        // Biarkan tombol SELL ALL dan CLOSE diproses GUIListener.
        if (rawSlot == 49 || rawSlot == 53) return;

        // Klik di luar inventory tidak boleh memindahkan item.
        if (rawSlot < 0) {
            event.setCancelled(true);
            return;
        }

        ClickType click = event.getClick();

        // Blokir jalur pemindahan yang bisa melewati pemeriksaan cursor.
        if (click == ClickType.NUMBER_KEY
                || click == ClickType.SWAP_OFFHAND
                || click == ClickType.DOUBLE_CLICK
                || click == ClickType.MIDDLE
                || click == ClickType.DROP
                || click == ClickType.CONTROL_DROP
                || click == ClickType.CREATIVE) {
            event.setCancelled(true);
            return;
        }

        // Top inventory: hanya item terdaftar yang boleh dimasukkan.
        if (rawSlot < topSize) {
            ItemStack cursor = event.getCursor();

            if (cursor != null && !cursor.getType().isAir()
                    && !isSellable(player, cursor)) {
                event.setCancelled(true);
                player.sendMessage(
                        plugin.getConfigManager().getPrefix()
                                + plugin.getConfigManager().color(
                                "&cItem ini tidak terdaftar di Shop!"
                        )
                );
                return;
            }

            return;
        }

        // Player inventory: blokir shift-click item yang tidak valid.
        if (event.isShiftClick()) {
            ItemStack moving = event.getCurrentItem();

            if (!isSellable(player, moving)) {
                event.setCancelled(true);
                player.sendMessage(
                        plugin.getConfigManager().getPrefix()
                                + plugin.getConfigManager().color(
                                "&cItem ini tidak terdaftar di Shop!"
                        )
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();

        if (!isSellInventory(top)) return;

        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        for (var entry : event.getNewItems().entrySet()) {
            int rawSlot = entry.getKey();

            if (rawSlot < top.getSize()
                    && !isSellable(player, entry.getValue())) {
                event.setCancelled(true);

                player.sendMessage(
                        plugin.getConfigManager().getPrefix()
                                + plugin.getConfigManager().color(
                                "&cItem ini tidak terdaftar di Shop!"
                        )
                );
                return;
            }
        }
    }
}
