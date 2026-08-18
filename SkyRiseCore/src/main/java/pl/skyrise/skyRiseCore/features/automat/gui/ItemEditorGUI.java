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

public class ItemEditorGUI implements InventoryHolder {

  private final AutomatModule plugin;
  private final MachineTemplate template;
  private final VendingItem item;
  private final Player player;
  private Inventory inventory;

  public ItemEditorGUI(
      AutomatModule plugin, MachineTemplate template, VendingItem item, Player player) {
    this.plugin = plugin;
    this.template = template;
    this.item = item;
    this.player = player;
  }

  public void open() {
    inventory =
        Bukkit.createInventory(
            this,
            54,
            ColorUtil.miniToLegacy("<reset><dark_gray>Item: <reset><white>" + item.getId()));
    populate();
    player.openInventory(inventory);
  }

  private void populate() {
    ItemStack bg = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();
    for (int i = 0; i < 54; i++) inventory.setItem(i, bg);

    String curr = plugin.getEconomyManager().getCurrencySymbol();

    inventory.setItem(4, item.buildDisplayItem(curr));

    inventory.setItem(
        19,
        new ItemBuilder(Material.CRAFTING_TABLE)
            .name("<reset><yellow>Materiał")
            .loreMini(
                "<reset><gray>Obecny: <reset><white>" + item.getMaterial().name(),
                "",
                "<reset><gray>Trzymaj item i kliknij")
            .build());

    inventory.setItem(
        20,
        new ItemBuilder(Material.NAME_TAG)
            .name("<reset><yellow>Nazwa")
            .loreMini(
                "<reset><gray>Obecna: <reset><white>" + item.getDisplayName(),
                "",
                "<reset><yellow>Kliknij aby zmienić")
            .build());

    inventory.setItem(
        21,
        new ItemBuilder(Material.WRITABLE_BOOK)
            .name("<reset><yellow>Opis")
            .loreMini(
                "<reset><gray>Linie: <reset><white>" + item.getLore().size(),
                "",
                "<reset><yellow>LPM edytuj",
                "<reset><red>PPM wyczyść")
            .build());

    inventory.setItem(
        22,
        new ItemBuilder(Material.GOLD_INGOT)
            .name("<reset><yellow>Cena")
            .loreMini(
                "<reset><gray>Obecna: <reset><green>"
                    + String.format("%.2f", item.getPrice())
                    + curr,
                "",
                "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        23,
        new ItemBuilder(Material.CHEST)
            .name("<reset><yellow>Ilość")
            .loreMini(
                "<reset><gray>Obecna: <reset><white>" + item.getAmount(),
                "",
                "<reset><green>LPM +1 / Shift+LPM +10",
                "<reset><red>PPM -1 / Shift+PPM -10")
            .build());

    inventory.setItem(
        24,
        new ItemBuilder(Material.ITEM_FRAME)
            .name("<reset><yellow>Slot w GUI")
            .loreMini(
                "<reset><gray>Obecny: <reset><white>" + item.getSlot(),
                "",
                "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        25,
        new ItemBuilder(item.isGlowing() ? Material.GLOWSTONE : Material.COAL)
            .name("<reset><yellow>Świecenie")
            .loreMini(
                "<reset><gray>Status: "
                    + (item.isGlowing() ? "<reset><green>Wł" : "<reset><red>Wył"),
                "",
                "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        28,
        new ItemBuilder(Material.ARMOR_STAND)
            .name("<reset><yellow>Custom Model Data")
            .loreMini(
                "<reset><gray>Obecne: <reset><white>"
                    + (item.getCustomModelData() > 0 ? item.getCustomModelData() : "Brak"),
                "",
                "<reset><yellow>Kliknij")
            .build());

    inventory.setItem(
        29,
        new ItemBuilder(Material.IRON_BARS)
            .name("<reset><yellow>Uprawnienie")
            .loreMini(
                "<reset><gray>Obecne: <reset><white>"
                    + (item.getPermission().isEmpty() ? "Brak" : item.getPermission()),
                "",
                "<reset><yellow>LPM zmień",
                "<reset><red>PPM usuń")
            .build());

    inventory.setItem(
        30,
        new ItemBuilder(Material.HOPPER)
            .name("<reset><yellow>Limit zakupów")
            .loreMini(
                "<reset><gray>Obecny: <reset><white>"
                    + (item.getPurchaseLimit() > 0 ? item.getPurchaseLimit() : "Brak"),
                "",
                "<reset><green>LPM +1",
                "<reset><red>PPM -1",
                "<reset><gold>Shift+LPM ręcznie")
            .build());

    inventory.setItem(
        31,
        new ItemBuilder(Material.COMMAND_BLOCK)
            .name("<reset><yellow>Komendy po zakupie")
            .loreMini(
                "<reset><gray>Ilość: <reset><white>" + item.getCommandsOnPurchase().size(),
                "<reset><gray>{player}, {amount}",
                "",
                "<reset><yellow>LPM dodaj",
                "<reset><red>PPM wyczyść")
            .build());

    inventory.setItem(
        37,
        new ItemBuilder(item.isUnlimitedStock() ? Material.LIME_DYE : Material.GRAY_DYE)
            .name("<reset><yellow>Nieograniczony stock")
            .loreMini(
                "<reset><gray>Status: "
                    + (item.isUnlimitedStock()
                        ? "<reset><green>∞ Nieograniczony"
                        : "<reset><red>Ograniczony"),
                "",
                "<reset><yellow>Kliknij aby przełączyć")
            .build());

    inventory.setItem(
        38,
        new ItemBuilder(Material.BARREL)
            .name("<reset><yellow>Obecny stock")
            .loreMini(
                "<reset><gray>Stan: <reset><yellow>"
                    + item.getStock()
                    + "<reset><gray>/<reset><yellow>"
                    + item.getMaxStock(),
                "",
                "<reset><green>LPM +10",
                "<reset><red>PPM -10",
                "<reset><gold>Shift+LPM wpisz ręcznie")
            .build());

    inventory.setItem(
        39,
        new ItemBuilder(Material.ENDER_CHEST)
            .name("<reset><yellow>Maksymalny stock")
            .loreMini(
                "<reset><gray>Obecny max: <reset><yellow>" + item.getMaxStock(),
                "",
                "<reset><green>LPM +10",
                "<reset><red>PPM -10",
                "<reset><gold>Shift+LPM wpisz ręcznie")
            .build());

    inventory.setItem(45, new ItemBuilder(Material.ARROW).name("<reset><red>Powrót").build());
    inventory.setItem(
        49, new ItemBuilder(Material.BARRIER).name("<reset><red>Usuń przedmiot").build());
  }

  public MachineTemplate getTemplate() {
    return template;
  }

  public VendingItem getVendingItem() {
    return item;
  }

  public Player getPlayer() {
    return player;
  }

  @Override
  public Inventory getInventory() {
    return inventory;
  }
}
