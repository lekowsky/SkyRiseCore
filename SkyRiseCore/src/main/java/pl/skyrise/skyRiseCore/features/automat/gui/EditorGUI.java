package pl.skyrise.skyRiseCore.features.automat.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;
import pl.skyrise.skyRiseCore.utils.ColorUtil;
import pl.skyrise.skyRiseCore.utils.ItemBuilder;

public class EditorGUI implements InventoryHolder {

  private final AutomatModule plugin;
  private final MachineTemplate template;
  private final Player player;
  private Inventory inventory;
  private EditorMode mode;

  public enum EditorMode {
    MAIN_MENU,
    ITEMS_LIST,
    GUI_SETTINGS
  }

  public EditorGUI(AutomatModule plugin, MachineTemplate template, Player player) {
    this.plugin = plugin;
    this.template = template;
    this.player = player;
    this.mode = EditorMode.MAIN_MENU;
  }

  public void open() {
    openMainMenu();
  }

  public void openMainMenu() {
    this.mode = EditorMode.MAIN_MENU;
    inventory =
        Bukkit.createInventory(
            this,
            54,
            ColorUtil.miniToLegacy(
                "<reset><dark_gray>Edytor: <reset><white>" + template.getName()));

    ItemStack bg = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();
    for (int i = 0; i < 54; i++) inventory.setItem(i, bg);

    int placements = plugin.getPlacementManager().getPlacementCount(template.getName());

    inventory.setItem(
        19,
        new ItemBuilder(Material.NAME_TAG)
            .name("<reset><yellow>Tytuł GUI")
            .loreMini(
                "<reset><gray>Obecny: <reset><white>" + template.getTitle(),
                "",
                "<reset><yellow>Kliknij aby zmienić")
            .build());

    inventory.setItem(
        20,
        new ItemBuilder(Material.CHEST)
            .name("<reset><yellow>Rozmiar")
            .loreMini(
                "<reset><gray>Rzędy: <reset><white>" + template.getRows(),
                "",
                "<reset><green>LPM +1",
                "<reset><red>PPM -1")
            .build());

    inventory.setItem(
        21,
        new ItemBuilder(Material.DIAMOND)
            .name("<reset><aqua>Przedmioty")
            .loreMini(
                "<reset><gray>Ilość: <reset><white>" + template.getItems().size(),
                "",
                "<reset><yellow>Kliknij aby edytować")
            .build());

    inventory.setItem(
        22,
        new ItemBuilder(Material.PAINTING)
            .name("<reset><light_purple>Wygląd GUI")
            .loreMini("<reset><gray>Tło, ramki, kolory", "", "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        23,
        new ItemBuilder(template.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
            .name(template.isEnabled() ? "<reset><green>Włączony" : "<reset><red>Wyłączony")
            .loreMini("<reset><gray>Status automatu", "", "<reset><yellow>Kliknij aby przełączyć")
            .build());

    inventory.setItem(
        24,
        new ItemBuilder(Material.IRON_BARS)
            .name("<reset><gold>Uprawnienie")
            .loreMini(
                "<reset><gray>Obecne: <reset><white>"
                    + (template.getPermission().isEmpty() ? "Brak" : template.getPermission()),
                "",
                "<reset><yellow>LPM zmień",
                "<reset><red>PPM usuń")
            .build());

    inventory.setItem(
        25,
        new ItemBuilder(Material.ARMOR_STAND)
            .name("<reset><gold>Nexo ID")
            .loreMini(
                "<reset><gray>Obecne: <reset><white>"
                    + (template.getNexoFurnitureId() != null
                        ? template.getNexoFurnitureId()
                        : "Brak"),
                "",
                "<reset><yellow>LPM zmień",
                "<reset><red>PPM usuń")
            .build());

    inventory.setItem(
        28,
        new ItemBuilder(Material.COMPASS)
            .name("<reset><green>Postaw automat")
            .loreMini(
                "<reset><gray>Instancje: <reset><white>" + placements,
                "",
                "<reset><gray>Włącza tryb stawiania",
                "",
                "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        29,
        new ItemBuilder(template.isCloseButtonEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
            .name("<reset><red>Przycisk Zamknij")
            .loreMini(
                "<reset><gray>Status: "
                    + (template.isCloseButtonEnabled() ? "<reset><green>Wł" : "<reset><red>Wył"),
                "",
                "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        30,
        new ItemBuilder(Material.ENDER_EYE)
            .name("<reset><green>Podgląd")
            .loreMini("<reset><gray>Otwórz automat jako gracz", "", "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        32,
        new ItemBuilder(template.isAutoRestockEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
            .name("<reset><gold>Auto-restock")
            .loreMini(
                "<reset><gray>Status: "
                    + (template.isAutoRestockEnabled()
                        ? "<reset><green>Włączony"
                        : "<reset><red>Wyłączony"),
                "",
                "<reset><yellow>Kliknij aby przełączyć")
            .build());

    inventory.setItem(
        33,
        new ItemBuilder(Material.CLOCK)
            .name("<reset><gold>Interwał restocku")
            .loreMini(
                "<reset><gray>Co: <reset><yellow>"
                    + template.getAutoRestockInterval()
                    + " <reset><gray>minut",
                "",
                "<reset><green>LPM +5 min",
                "<reset><red>PPM -5 min",
                "<reset><gold>Shift+LPM wpisz ręcznie")
            .build());

    inventory.setItem(
        34,
        new ItemBuilder(Material.HOPPER)
            .name("<reset><gold>Ilość restocku")
            .loreMini(
                "<reset><gray>Dodaje: <reset><yellow>+"
                    + template.getAutoRestockAmount()
                    + " <reset><gray>szt.",
                "",
                "<reset><green>LPM +5",
                "<reset><red>PPM -5",
                "<reset><gold>Shift+LPM wpisz ręcznie")
            .build());

    inventory.setItem(
        49, new ItemBuilder(Material.BARRIER).name("<reset><red>Zamknij edytor").build());

    player.openInventory(inventory);
  }

  public void openItemsList() {
    this.mode = EditorMode.ITEMS_LIST;
    inventory =
        Bukkit.createInventory(
            this,
            54,
            ColorUtil.miniToLegacy(
                "<reset><dark_gray>Przedmioty: <reset><white>" + template.getName()));

    ItemStack bg = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();
    for (int i = 0; i < 54; i++) inventory.setItem(i, bg);

    int slot = 0;
    for (VendingItem item : template.getItems().values()) {
      if (slot >= 45) break;
      inventory.setItem(slot, item.buildEditorItem());
      slot++;
    }

    inventory.setItem(
        48,
        new ItemBuilder(Material.EMERALD)
            .name("<reset><green>Dodaj z ręki")
            .loreMini("<reset><gray>Trzymaj item w ręce", "", "<reset><yellow>Kliknij aby dodać")
            .build());

    inventory.setItem(
        50,
        new ItemBuilder(Material.CRAFTING_TABLE)
            .name("<reset><yellow>Dodaj pusty")
            .loreMini("<reset><gray>Dodaje przedmiot do edycji", "", "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(45, new ItemBuilder(Material.ARROW).name("<reset><red>Powrót").build());

    player.openInventory(inventory);
  }

  public void openGUISettings() {
    this.mode = EditorMode.GUI_SETTINGS;
    inventory =
        Bukkit.createInventory(
            this,
            54,
            ColorUtil.miniToLegacy(
                "<reset><dark_gray>Wygląd: <reset><white>" + template.getName()));

    ItemStack bg = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();
    for (int i = 0; i < 54; i++) inventory.setItem(i, bg);

    inventory.setItem(
        20,
        new ItemBuilder(template.isFillEmpty() ? Material.LIME_DYE : Material.GRAY_DYE)
            .name("<reset><yellow>Wypełnianie tła")
            .loreMini(
                "<reset><gray>Status: "
                    + (template.isFillEmpty() ? "<reset><green>Wł" : "<reset><red>Wył"),
                "",
                "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        21,
        new ItemBuilder(template.getFillerMaterial())
            .name("<reset><yellow>Materiał tła")
            .loreMini(
                "<reset><gray>Obecny: <reset><white>" + template.getFillerMaterial().name(),
                "",
                "<reset><gray>Trzymaj blok i kliknij")
            .build());

    inventory.setItem(
        23,
        new ItemBuilder(template.isBorder() ? Material.LIME_DYE : Material.GRAY_DYE)
            .name("<reset><yellow>Ramka")
            .loreMini(
                "<reset><gray>Status: "
                    + (template.isBorder() ? "<reset><green>Wł" : "<reset><red>Wył"),
                "",
                "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        24,
        new ItemBuilder(template.getBorderMaterial())
            .name("<reset><yellow>Materiał ramki")
            .loreMini(
                "<reset><gray>Obecny: <reset><white>" + template.getBorderMaterial().name(),
                "",
                "<reset><gray>Trzymaj blok i kliknij")
            .build());

    inventory.setItem(45, new ItemBuilder(Material.ARROW).name("<reset><red>Powrót").build());

    player.openInventory(inventory);
  }

  public MachineTemplate getTemplate() {
    return template;
  }

  public EditorMode getMode() {
    return mode;
  }

  public Player getPlayer() {
    return player;
  }

  @Override
  public Inventory getInventory() {
    return inventory;
  }
}
