
package my.kingdomvortex.vortexshop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MenuHolder implements InventoryHolder {

    private final String type;
    private final String category;
    private final String slot;
    private Inventory inventory;

    public MenuHolder(String type, String category, String slot) {
        this.type = type;
        this.category = category;
        this.slot = slot;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getSlot() {
        return slot;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
