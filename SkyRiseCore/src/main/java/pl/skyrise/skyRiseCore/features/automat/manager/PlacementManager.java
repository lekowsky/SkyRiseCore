package pl.skyrise.skyRiseCore.features.automat.manager;

import java.util.*;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;

public class PlacementManager {

  private final AutomatModule plugin;
  private final Map<String, MachinePlacement> placements;
  private final Map<String, MachinePlacement> byEntityUUID;
  private final Collection<MachinePlacement> placementsView;
  private final NamespacedKey templateKey;

  public PlacementManager(AutomatModule plugin) {
    this.plugin = plugin;
    this.placements = new LinkedHashMap<>();
    this.byEntityUUID = new HashMap<>();
    this.placementsView = Collections.unmodifiableCollection(placements.values());
    this.templateKey = new NamespacedKey(plugin.getPlugin(), "automat_template");
  }

  public MachinePlacement place(
      String templateName, Location location, String nexoItemId, UUID placedBy) {
    if (templateName == null || location == null || location.getWorld() == null) return null;
    String key = MachinePlacement.toLocationKey(location);
    MachineTemplate template = plugin.getMachineManager().getTemplate(templateName);
    if (template == null) return null;
    if (placements.containsKey(key)) return null;

    MachinePlacement placement = new MachinePlacement(templateName, location);
    placement.setNexoItemId(nexoItemId);
    placement.setPlacedBy(placedBy);
    placement.initStockFromTemplate(template);

    placements.put(key, placement);
    plugin.getDataManager().savePlacements();
    return placement;
  }

  public boolean remove(Location location) {
    if (location == null || location.getWorld() == null) return false;
    MachinePlacement removed = placements.remove(MachinePlacement.toLocationKey(location));
    if (removed != null) {
      if (removed.getNexoEntityUUID() != null) {
        byEntityUUID.remove(removed.getNexoEntityUUID());
      }
      plugin.getDataManager().savePlacements();
      return true;
    }
    return false;
  }

  public boolean removeByPlacement(MachinePlacement placement) {
    if (placement == null) return false;
    String key = placement.getLocationKey();
    MachinePlacement removed = placements.remove(key);
    if (removed != null) {
      if (removed.getNexoEntityUUID() != null) {
        byEntityUUID.remove(removed.getNexoEntityUUID());
      }
      plugin.getDataManager().savePlacements();
      return true;
    }
    return false;
  }

  public MachinePlacement getPlacement(Location location) {
    if (location == null || location.getWorld() == null) return null;
    return placements.get(MachinePlacement.toLocationKey(location));
  }

  public MachinePlacement getPlacementByEntityUUID(String entityUUID) {
    if (entityUUID == null) return null;
    return byEntityUUID.get(entityUUID);
  }

  public void registerEntityUUID(MachinePlacement placement, String entityUUID) {
    if (placement == null || entityUUID == null) return;

    if (placement.getNexoEntityUUID() != null) {
      byEntityUUID.remove(placement.getNexoEntityUUID());
    }

    placement.setNexoEntityUUID(entityUUID);
    byEntityUUID.put(entityUUID, placement);
  }

  public Collection<MachinePlacement> getAllPlacements() {
    return placementsView;
  }

  public int getPlacementCount(String templateName) {
    int count = 0;
    for (MachinePlacement p : placements.values()) {
      if (p.getTemplateName().equalsIgnoreCase(templateName)) count++;
    }
    return count;
  }

  public void addPlacement(MachinePlacement p) {
    placements.put(p.getLocationKey(), p);
    if (p.getNexoEntityUUID() != null) {
      byEntityUUID.put(p.getNexoEntityUUID(), p);
    }
  }

  public NamespacedKey getTemplateKey() {
    return templateKey;
  }

  public String getTemplateFromItem(ItemStack item) {
    if (item == null || !item.hasItemMeta()) return null;
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return null;
    return meta.getPersistentDataContainer().get(templateKey, PersistentDataType.STRING);
  }
}
