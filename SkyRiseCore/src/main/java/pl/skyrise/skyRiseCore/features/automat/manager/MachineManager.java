package pl.skyrise.skyRiseCore.features.automat.manager;

import java.util.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;

public class MachineManager {

  private final AutomatModule plugin;
  private final Map<String, MachineTemplate> templates;
  private final Collection<MachineTemplate> templatesView;

  public MachineManager(AutomatModule plugin) {
    this.plugin = plugin;
    this.templates = new LinkedHashMap<>();
    this.templatesView = Collections.unmodifiableCollection(templates.values());
  }

  public MachineTemplate createTemplate(String name) {
    if (name == null || name.isBlank()) return null;
    if (templates.containsKey(name.toLowerCase(Locale.ROOT))) return null;

    MachineTemplate template = new MachineTemplate(name);
    template.setRows(plugin.getConfig().getInt("default-template.rows", 5));
    template.setTitle(
        plugin.getConfig().getString("default-template.title", "<reset><dark_gray>Automat"));
    template.setFillEmpty(plugin.getConfig().getBoolean("default-template.fill-empty", false));

    try {
      template.setFillerMaterial(
          Material.valueOf(
              plugin
                  .getConfig()
                  .getString("default-template.filler-material", "BLACK_STAINED_GLASS_PANE")));
    } catch (Exception e) {
      template.setFillerMaterial(Material.BLACK_STAINED_GLASS_PANE);
    }
    template.setFillerName(plugin.getConfig().getString("default-template.filler-name", " "));
    template.setBorder(plugin.getConfig().getBoolean("default-template.border", false));
    try {
      template.setBorderMaterial(
          Material.valueOf(
              plugin
                  .getConfig()
                  .getString("default-template.border-material", "BLACK_STAINED_GLASS_PANE")));
    } catch (Exception e) {
      template.setBorderMaterial(Material.BLACK_STAINED_GLASS_PANE);
    }
    template.setBorderName(plugin.getConfig().getString("default-template.border-name", " "));

    template.setCloseButtonEnabled(
        plugin.getConfig().getBoolean("default-template.close-button.enabled", true));
    template.setCloseButtonSlot(
        plugin.getConfig().getInt("default-template.close-button.slot", -1));
    try {
      template.setCloseButtonMaterial(
          Material.valueOf(
              plugin.getConfig().getString("default-template.close-button.material", "BARRIER")));
    } catch (Exception e) {
      template.setCloseButtonMaterial(Material.BARRIER);
    }
    template.setCloseButtonName(
        plugin.getConfig().getString("default-template.close-button.name", "<reset><red>Zamknij"));
    template.setCloseButtonLore(
        plugin.getConfig().getStringList("default-template.close-button.lore"));

    template.setAutoRestockEnabled(
        plugin.getConfig().getBoolean("auto-restock.defaults.enabled", false));
    template.setAutoRestockInterval(
        plugin.getConfig().getInt("auto-restock.defaults.interval", 30));
    template.setAutoRestockAmount(plugin.getConfig().getInt("auto-restock.defaults.amount", 10));

    templates.put(name.toLowerCase(Locale.ROOT), template);
    plugin.getDataManager().saveAll();
    return template;
  }

  public boolean deleteTemplate(String name) {
    if (name == null) return false;
    MachineTemplate removed = templates.remove(name.toLowerCase(Locale.ROOT));
    if (removed == null) return false;

    if (plugin.getRestockManager() != null) {
      plugin.getRestockManager().stopRestock(removed.getName());
    }
    plugin.getDataManager().saveAll();
    return true;
  }

  public MachineTemplate getTemplate(String name) {
    return name == null ? null : templates.get(name.toLowerCase(Locale.ROOT));
  }

  public Collection<MachineTemplate> getAllTemplates() {
    return templatesView;
  }

  public Set<String> getTemplateNames() {
    return Collections.unmodifiableSet(templates.keySet());
  }

  public void addTemplate(MachineTemplate t) {
    if (t == null || t.getName() == null) return;
    templates.put(t.getName().toLowerCase(Locale.ROOT), t);
  }

  public String generateItemId(MachineTemplate template) {
    int counter = 1;
    while (template.getItem("item_" + counter) != null) counter++;
    return "item_" + counter;
  }

  public VendingItem createItemFromHand(Player player, MachineTemplate template, double price) {
    ItemStack handItem = player.getInventory().getItemInMainHand();
    if (handItem.getType() == Material.AIR) return null;

    String id = generateItemId(template);
    VendingItem vendingItem = new VendingItem(id);

    vendingItem.setFromItemStack(handItem.clone());
    vendingItem.setPrice(price);

    int freeSlot = template.getNextFreeSlot();
    if (freeSlot == -1) return null;
    vendingItem.setSlot(freeSlot);
    return vendingItem;
  }

  private String formatMaterialName(Material material) {
    String name = material.name().replace("_", " ").toLowerCase();
    StringBuilder formatted = new StringBuilder();
    for (String word : name.split(" ")) {
      if (!word.isEmpty())
        formatted
            .append(Character.toUpperCase(word.charAt(0)))
            .append(word.substring(1))
            .append(" ");
    }
    return formatted.toString().trim();
  }

  public void save() {
    plugin.getDataManager().saveAll();
  }
}
