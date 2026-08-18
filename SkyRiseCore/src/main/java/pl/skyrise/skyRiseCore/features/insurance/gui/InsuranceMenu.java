package pl.skyrise.skyRiseCore.features.insurance.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import pl.skyrise.skyRiseCore.features.insurance.InsuranceModule;
import pl.skyrise.skyRiseCore.features.insurance.model.RescueEffect;
import pl.skyrise.skyRiseCore.features.insurance.util.InsuranceText;

public class InsuranceMenu implements InventoryHolder {

  public enum Page {
    MAIN,
    EFFECTS
  }

  private final InsuranceModule module;
  private final Player viewer;
  private final Page page;
  private final Inventory inventory;

  public InsuranceMenu(InsuranceModule module, Player viewer) {
    this(module, viewer, Page.MAIN);
  }

  public InsuranceMenu(InsuranceModule module, Player viewer, Page page) {
    this.module = module;
    this.viewer = viewer;
    this.page = page != null ? page : Page.MAIN;
    String title = this.page == Page.EFFECTS ? module.getGuiEffectsTitle() : module.getGuiTitle();
    this.inventory =
        Bukkit.createInventory(this, module.getGuiSize(), InsuranceText.component(title));
    populate();
  }

  private void populate() {
    fillBackground();
    if (page == Page.EFFECTS) {
      populateEffects();
    } else {
      populateMain();
    }
  }

  private void fillBackground() {
    if (!module.isGuiFillEmpty()) return;
    ItemStack filler = item(module.getGuiFillerMaterial(), module.getGuiFillerName(), List.of(), 1);
    for (int i = 0; i < inventory.getSize(); i++) {
      inventory.setItem(i, filler);
    }
  }

  private void populateMain() {
    boolean active = module.hasInsurance(viewer.getUniqueId());
    List<String> lore = new ArrayList<>();
    for (String line : module.getGuiInsuranceLore()) {
      if (!active && line.contains("{time_left}")) continue;
      if (line.contains("{charges}")) continue;

      if (line.contains("{effect}") && !viewer.hasPermission(module.getPremiumPermission()))
        continue;
      lore.add(module.replacePlaceholders(line, viewer));
    }
    lore.add("");
    lore.add(active ? module.getGuiAlreadyOwnedLine() : module.getGuiClickToBuyLine());

    inventory.setItem(
        active ? module.getGuiInsuranceActiveSlot() : module.getGuiInsuranceSlot(),
        item(
            module.getGuiInsuranceMaterial(),
            module.replacePlaceholders(module.getGuiInsuranceName(), viewer),
            lore,
            1));

    if (active) {
      List<String> renewLore =
          module.getGuiRenewLore().isEmpty()
              ? List.of(
                  "<reset><white>Przedłuż ważność aktywnej polisy.",
                  "<reset><white>Koszt: <reset><#f5f242>{renewal_price}{currency}",
                  "<reset><gray>Dostępne w ostatnich 24h ważności.")
              : module.getGuiRenewLore();
      inventory.setItem(
          module.getGuiRenewSlot(),
          item(
              module.getGuiRenewMaterial(),
              module.replacePlaceholders(module.getGuiRenewName(), viewer),
              renewLore.stream().map(line -> module.replacePlaceholders(line, viewer)).toList(),
              1));
    }

    List<String> effectLore = new ArrayList<>(module.getGuiEffectButtonLore());
    if (!viewer.hasPermission(module.getPremiumPermission())) {
      effectLore.add("");
      effectLore.add("<reset><red>Wymagana ranga premium.");
    }
    inventory.setItem(
        module.getGuiEffectButtonSlot(),
        item(module.getGuiEffectButtonMaterial(), module.getGuiEffectButtonName(), effectLore, 1));

    inventory.setItem(
        module.getGuiCloseSlot(),
        item(module.getGuiCloseMaterial(), module.getGuiCloseName(), module.getGuiCloseLore(), 1));
  }

  private void populateEffects() {
    String selected = module.getSelectedEffectId(viewer.getUniqueId());
    for (RescueEffect effect : module.getRescueEffects().values()) {
      List<String> lore =
          new ArrayList<>(
              effect.lore().isEmpty()
                  ? List.of("<reset><white>Wybierz efekt ratunku.")
                  : effect.lore());
      lore.add("");
      lore.add(
          effect.id().equalsIgnoreCase(selected)
              ? "<reset><#f5f242>Aktualnie wybrany efekt."
              : "<reset><#f5f242>Kliknij, aby wybrać efekt.");
      inventory.setItem(effect.slot(), item(effect.material(), effect.name(), lore, 1));
    }

    inventory.setItem(
        module.getGuiCloseSlot(),
        item(module.getGuiBackMaterial(), module.getGuiBackName(), module.getGuiBackLore(), 1));
  }

  private ItemStack item(Material material, String name, List<String> lore, int amount) {
    int displayAmount = Math.max(1, Math.min(64, amount));
    ItemStack item = new ItemStack(material != null ? material : Material.STONE, displayAmount);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.displayName(InsuranceText.component(name));
      if (lore != null && !lore.isEmpty()) {
        meta.lore(lore.stream().map(InsuranceText::component).toList());
      }
      if (displayAmount > 1) {
        meta.setMaxStackSize(64);
      }
      meta.addItemFlags(
          org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
          org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
      item.setItemMeta(meta);
      item.setAmount(displayAmount);
    }
    return item;
  }

  public void open() {
    viewer.openInventory(inventory);
  }

  public InsuranceModule getModule() {
    return module;
  }

  public Player getViewer() {
    return viewer;
  }

  public Page getPage() {
    return page;
  }

  @Override
  public @NotNull Inventory getInventory() {
    return inventory;
  }
}
