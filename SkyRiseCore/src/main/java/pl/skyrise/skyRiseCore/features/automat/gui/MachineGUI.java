package pl.skyrise.skyRiseCore.features.automat.gui;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;
import pl.skyrise.skyRiseCore.utils.ColorUtil;
import pl.skyrise.skyRiseCore.utils.ItemBuilder;

public class MachineGUI implements InventoryHolder {

  private final AutomatModule plugin;
  private final MachineTemplate template;
  private final MachinePlacement placement;
  private final Player player;
  private Inventory inventory;

  public MachineGUI(
      AutomatModule plugin, MachineTemplate template, Player player, MachinePlacement placement) {
    this.plugin = plugin;
    this.template = template;
    this.player = player;
    this.placement = placement;
  }

  public MachineGUI(AutomatModule plugin, MachineTemplate template, Player player) {
    this(plugin, template, player, null);
  }

  public void open() {
    inventory =
        Bukkit.createInventory(
            this, template.getSize(), ColorUtil.miniToLegacy(template.getTitle()));
    populate();
    player.openInventory(inventory);
  }

  private void populate() {
    String currency = plugin.getEconomyManager().getCurrencySymbol();

    Set<Integer> borderSlots = new HashSet<>();
    if (template.isBorder()) {
      int size = template.getSize();
      int rows = template.getRows();
      for (int i = 0; i < size; i++) {
        int row = i / 9;
        int col = i % 9;
        if (row == 0 || row == rows - 1 || col == 0 || col == 8) borderSlots.add(i);
      }
      ItemStack borderItem =
          new ItemBuilder(template.getBorderMaterial()).name(template.getBorderName()).build();
      for (int slot : borderSlots) inventory.setItem(slot, borderItem);
    }

    if (template.isFillEmpty()) {
      ItemStack filler =
          new ItemBuilder(template.getFillerMaterial()).name(template.getFillerName()).build();
      for (int i = 0; i < template.getSize(); i++) {
        if (inventory.getItem(i) == null) inventory.setItem(i, filler);
      }
    }

    for (VendingItem item : template.getItems().values()) {
      if (item.getSlot() >= 0 && item.getSlot() < template.getSize()) {
        ItemStack displayItem;
        if (placement != null && !item.isUnlimitedStock()) {

          int origStock = item.getStock();
          item.setStock(placement.getStock(item.getId()));
          displayItem = item.buildDisplayItem(currency);
          item.setStock(origStock);
        } else {
          displayItem = item.buildDisplayItem(currency);
        }
        inventory.setItem(item.getSlot(), displayItem);
      }
    }

    if (template.isCloseButtonEnabled()) {
      int closeSlot = template.getResolvedCloseSlot();
      if (closeSlot >= 0 && closeSlot < template.getSize()) {
        inventory.setItem(
            closeSlot,
            new ItemBuilder(template.getCloseButtonMaterial())
                .name(template.getCloseButtonName())
                .loreMini(template.getCloseButtonLore())
                .build());
      }
    }
  }

  public MachineTemplate getTemplate() {
    return template;
  }

  public MachinePlacement getPlacement() {
    return placement;
  }

  @Override
  public Inventory getInventory() {
    return inventory;
  }
}
