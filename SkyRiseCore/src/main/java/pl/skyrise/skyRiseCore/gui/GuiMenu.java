package pl.skyrise.skyRiseCore.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class GuiMenu implements InventoryHolder {
  private final Inventory inventory;
  private final Map<Integer, Consumer<InventoryClickEvent>> actions;

  public GuiMenu(String title, int size) {
    this.inventory = Bukkit.createInventory(this, size, ColorUtil.mini(title));
    this.actions = new HashMap<>();
  }

  public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
    inventory.setItem(slot, item);
    if (action != null) {
      actions.put(slot, action);
    } else {
      actions.remove(slot);
    }
  }

  public void handleAction(InventoryClickEvent event) {
    Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
    if (action != null) {
      action.accept(event);
    }
  }

  @Override
  public @NotNull Inventory getInventory() {
    return inventory;
  }
}
