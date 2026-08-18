package pl.skyrise.skyRiseCore.features.automat.manager;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;

public class NexoManager {

  private final AutomatModule plugin;
  private boolean nexoAvailable;
  private final Map<String, String> furnitureMappings;
  private Method idFromItemMethod;
  private boolean idFromItemLookupAttempted;

  public NexoManager(AutomatModule plugin) {
    this.plugin = plugin;
    this.furnitureMappings = new HashMap<>();
    checkNexo();
    loadMappings();
  }

  private void checkNexo() {
    nexoAvailable = plugin.getServer().getPluginManager().getPlugin("Nexo") != null;
    if (!nexoAvailable) {
      plugin.getLogger().warning("Brak integracji Nexo.");
    }
  }

  public void loadMappings() {
    furnitureMappings.clear();
    if (!plugin.getConfig().getBoolean("nexo.enabled", true)) return;

    var section = plugin.getConfig().getConfigurationSection("nexo.furniture-mappings");
    if (section == null) return;

    for (String nexoId : section.getKeys(false)) {
      String templateName = section.getString(nexoId);
      if (templateName == null || templateName.isBlank()) continue;
      furnitureMappings.put(nexoId.toLowerCase(Locale.ROOT), templateName);
      plugin.getLogger().info("Nexo mapping: " + nexoId + " -> '" + templateName + "'");
    }
  }

  public boolean isNexoAvailable() {
    return nexoAvailable;
  }

  public String getTemplateName(String nexoItemId) {
    if (nexoItemId == null) return null;
    if (!plugin.getConfig().getBoolean("nexo.enabled", true)) return null;

    String mapped = furnitureMappings.get(nexoItemId.toLowerCase(Locale.ROOT));
    if (mapped != null) return mapped;

    for (MachineTemplate template : plugin.getMachineManager().getAllTemplates()) {
      String id = template.getNexoFurnitureId();
      if (id != null && id.equalsIgnoreCase(nexoItemId)) {
        return template.getName();
      }
    }
    return null;
  }

  public void setMapping(String nexoItemId, String templateName) {
    if (nexoItemId == null
        || nexoItemId.isBlank()
        || templateName == null
        || templateName.isBlank()) return;
    furnitureMappings.put(nexoItemId.toLowerCase(Locale.ROOT), templateName);
    plugin.getConfig().set("nexo.furniture-mappings." + nexoItemId, templateName);
    plugin.saveConfig();
  }

  public String idFromItem(ItemStack item) {
    if (item == null || !nexoAvailable) return null;
    try {
      Method method = getIdFromItemMethod();
      if (method == null) return null;
      Object result = method.invoke(null, item);
      return result != null ? result.toString() : null;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private Method getIdFromItemMethod() throws ReflectiveOperationException {
    if (idFromItemLookupAttempted) return idFromItemMethod;
    idFromItemLookupAttempted = true;

    Class<?> nexoItems = Class.forName("com.nexomc.nexo.api.NexoItems");
    idFromItemMethod = nexoItems.getMethod("idFromItem", ItemStack.class);
    idFromItemMethod.setAccessible(true);
    return idFromItemMethod;
  }

  public Map<String, String> getFurnitureMappings() {
    return furnitureMappings;
  }

  public boolean isAutoRegister() {
    return plugin.getConfig().getBoolean("nexo.auto-register-furniture", true);
  }

  public boolean allowVanillaBlocks() {
    return plugin.getConfig().getBoolean("nexo.allow-vanilla-blocks", true);
  }
}
