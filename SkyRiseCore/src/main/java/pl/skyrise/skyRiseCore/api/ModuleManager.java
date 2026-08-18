package pl.skyrise.skyRiseCore.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class ModuleManager {

  private final JavaPlugin plugin;
  private final Map<String, Module> allModules;
  private final Set<String> enabledModules;
  private final CustomConfig moduleStateConfig;

  public ModuleManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.allModules = new LinkedHashMap<>();
    this.enabledModules = new HashSet<>();
    this.moduleStateConfig = new CustomConfig(plugin, "modules.yml");
    this.moduleStateConfig.load();
  }

  public void register(Module module) {
    if (module == null) return;

    String key = key(module.getName());
    allModules.put(key, module);

    if (isConfiguredEnabled(key)) {
      enableInternal(key, false);
    }
  }

  public boolean enable(String name) {
    return enableInternal(key(name), true);
  }

  private boolean enableInternal(String key, boolean persistState) {
    Module module = allModules.get(key);
    if (module == null || enabledModules.contains(key)) return false;

    try {
      module.onEnable();
      enabledModules.add(key);
      if (persistState) {
        saveConfiguredState(key, true);
      }
      return true;
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "✖ Nie udało się włączyć modułu " + module.getName() + ".", e);
      return false;
    }
  }

  public boolean disable(String name) {
    return disableInternal(key(name), true);
  }

  private boolean disableInternal(String key, boolean persistState) {
    Module module = allModules.get(key);
    if (module == null || !enabledModules.contains(key)) return false;

    try {
      module.onDisable();
      enabledModules.remove(key);
      if (persistState) {
        saveConfiguredState(key, false);
      }
      return true;
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "✖ Nie udało się wyłączyć modułu " + module.getName() + ".", e);
      return false;
    }
  }

  public void unregister(String name) {
    String key = key(name);
    disableInternal(key, false);
    allModules.remove(key);
  }

  public void disableAll() {
    for (String key : new HashSet<>(enabledModules)) {
      disableInternal(key, false);
    }
  }

  public boolean reload(String name) {
    String key = key(name);
    if (!enabledModules.contains(key)) return false;
    Module module = allModules.get(key);
    if (module == null) return false;

    try {
      module.onReload();
      return true;
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "✖ Nie udało się przeładować modułu " + module.getName() + ".", e);
      return false;
    }
  }

  public void reloadAll() {
    for (String key : enabledModules) {
      reload(key);
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends Module> T getModule(String name) {
    return (T) allModules.get(key(name));
  }

  public boolean isEnabled(String name) {
    return enabledModules.contains(key(name));
  }

  public int getModuleCount() {
    return allModules.size();
  }

  public int getEnabledModuleCount() {
    return enabledModules.size();
  }

  public Set<String> getModuleNames() {
    return Collections.unmodifiableSet(allModules.keySet());
  }

  public Map<String, Module> getModules() {
    return Collections.unmodifiableMap(allModules);
  }

  private boolean isConfiguredEnabled(String key) {
    return moduleStateConfig.getConfig().getBoolean(configPath(key), true);
  }

  private void saveConfiguredState(String key, boolean enabled) {
    moduleStateConfig.getConfig().set(configPath(key), enabled);
    moduleStateConfig.save();
  }

  private static String configPath(String key) {
    return "modules." + key + ".enabled";
  }

  private static String key(String name) {
    return name == null ? "" : name.toLowerCase(Locale.ROOT);
  }
}
