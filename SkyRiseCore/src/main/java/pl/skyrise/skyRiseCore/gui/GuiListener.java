package pl.skyrise.skyRiseCore.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {
  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (event.getClickedInventory() == null) return;
    if (event.getInventory().getHolder() instanceof GuiMenu) {
      GuiMenu menu = (GuiMenu) event.getInventory().getHolder();
      event.setCancelled(true);
      if (event.getClickedInventory().equals(event.getInventory())) {
        menu.handleAction(event);
      }
    }
  }
}
