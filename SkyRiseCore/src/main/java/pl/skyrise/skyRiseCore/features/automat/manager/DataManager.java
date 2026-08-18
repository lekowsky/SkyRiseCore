package pl.skyrise.skyRiseCore.features.automat.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.scheduler.BukkitTask;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;

public class DataManager {

  private static final long SAVE_DELAY_TICKS = 20L * 10L;

  private final AutomatModule plugin;
  private final File templatesFile;
  private final File placementsFile;

  private BukkitTask queuedSaveTask;
  private boolean templatesDirty;
  private boolean placementsDirty;

  public DataManager(AutomatModule plugin) {
    this.plugin = plugin;
    this.templatesFile = new File(plugin.getDataFolder(), "templates.yml");
    this.placementsFile = new File(plugin.getDataFolder(), "placements.yml");
    migrateLegacyDataFiles();
  }

  public void loadTemplates() {
    ensureFile(templatesFile);
    FileConfiguration config = YamlConfiguration.loadConfiguration(templatesFile);
    ConfigurationSection section = config.getConfigurationSection("templates");
    if (section == null) return;

    for (String name : section.getKeys(false)) {
      ConfigurationSection ts = section.getConfigurationSection(name);
      if (ts == null) continue;

      MachineTemplate t = new MachineTemplate(name);
      t.setTitle(ts.getString("title", "<reset><dark_gray>Automat"));
      t.setRows(ts.getInt("rows", 5));
      t.setEnabled(ts.getBoolean("enabled", true));
      t.setPermission(ts.getString("permission", ""));
      t.setFillEmpty(ts.getBoolean("fill-empty", false));
      t.setBorder(ts.getBoolean("border", false));
      t.setNexoFurnitureId(ts.getString("nexo-furniture-id", null));

      t.setFillerMaterial(material(ts, "filler-material", Material.BLACK_STAINED_GLASS_PANE));
      t.setFillerName(ts.getString("filler-name", " "));
      t.setBorderMaterial(material(ts, "border-material", Material.BLACK_STAINED_GLASS_PANE));
      t.setBorderName(ts.getString("border-name", " "));

      t.setCloseButtonEnabled(ts.getBoolean("close-button.enabled", true));
      t.setCloseButtonSlot(ts.getInt("close-button.slot", -1));
      t.setCloseButtonMaterial(material(ts, "close-button.material", Material.BARRIER));
      t.setCloseButtonName(ts.getString("close-button.name", "<reset><red>Zamknij"));
      t.setCloseButtonLore(ts.getStringList("close-button.lore"));

      t.setAutoRestockEnabled(ts.getBoolean("auto-restock.enabled", false));
      t.setAutoRestockInterval(ts.getInt("auto-restock.interval", 30));
      t.setAutoRestockAmount(ts.getInt("auto-restock.amount", 10));

      ConfigurationSection itemsSection = ts.getConfigurationSection("items");
      if (itemsSection != null) {
        for (String itemId : itemsSection.getKeys(false)) {
          ConfigurationSection is = itemsSection.getConfigurationSection(itemId);
          if (is == null) continue;

          VendingItem item = new VendingItem(itemId);
          item.setMaterial(material(is, "material", Material.STONE));
          item.setDisplayName(is.getString("display-name", "<reset><white>Item"));
          item.setLore(is.getStringList("lore"));
          item.setAmount(is.getInt("amount", 1));
          item.setPrice(is.getDouble("price", 0));
          item.setSlot(is.getInt("slot", 0));
          item.setGlowing(is.getBoolean("glowing", false));
          item.setCustomModelData(is.getInt("custom-model-data", -1));
          item.setPermission(is.getString("permission", ""));
          item.setPurchaseLimit(is.getInt("purchase-limit", 0));
          item.setCommandsOnPurchase(is.getStringList("commands-on-purchase"));

          item.setStock(is.getInt("stock", 0));
          item.setMaxStock(is.getInt("max-stock", 64));
          item.setUnlimitedStock(is.getBoolean("unlimited-stock", true));

          String serialized = is.getString("serialized-item", null);
          if (serialized != null && !serialized.isEmpty()) {
            item.setSerializedItem(serialized);
            item.setUseSerializedItem(is.getBoolean("use-serialized", true));
          }

          ConfigurationSection enchSec = is.getConfigurationSection("enchantments");
          if (enchSec != null) {
            Map<Enchantment, Integer> enchants = new HashMap<>();
            for (String key : enchSec.getKeys(false)) {
              Enchantment ench =
                  Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
              if (ench != null) enchants.put(ench, enchSec.getInt(key));
            }
            item.setEnchantments(enchants);
          }

          t.addItem(item);
        }
      }

      plugin.getMachineManager().addTemplate(t);
      plugin.getLogger().info("Loaded template: " + name + " (" + t.getItems().size() + " items)");
    }
  }

  public void saveTemplates() {
    templatesDirty = false;
    saveTemplatesNow();
  }

  private void saveTemplatesNow() {
    FileConfiguration config = new YamlConfiguration();

    for (MachineTemplate t : plugin.getMachineManager().getAllTemplates()) {
      String path = "templates." + t.getName().toLowerCase(Locale.ROOT);

      config.set(path + ".title", t.getTitle());
      config.set(path + ".rows", t.getRows());
      config.set(path + ".enabled", t.isEnabled());
      config.set(path + ".permission", t.getPermission());
      config.set(path + ".fill-empty", t.isFillEmpty());
      config.set(path + ".filler-material", t.getFillerMaterial().name());
      config.set(path + ".filler-name", t.getFillerName());
      config.set(path + ".border", t.isBorder());
      config.set(path + ".border-material", t.getBorderMaterial().name());
      config.set(path + ".border-name", t.getBorderName());
      config.set(path + ".nexo-furniture-id", t.getNexoFurnitureId());

      config.set(path + ".close-button.enabled", t.isCloseButtonEnabled());
      config.set(path + ".close-button.slot", t.getCloseButtonSlot());
      config.set(path + ".close-button.material", t.getCloseButtonMaterial().name());
      config.set(path + ".close-button.name", t.getCloseButtonName());
      config.set(path + ".close-button.lore", t.getCloseButtonLore());

      config.set(path + ".auto-restock.enabled", t.isAutoRestockEnabled());
      config.set(path + ".auto-restock.interval", t.getAutoRestockInterval());
      config.set(path + ".auto-restock.amount", t.getAutoRestockAmount());

      for (VendingItem item : t.getItems().values()) {
        String ip = path + ".items." + item.getId();
        config.set(ip + ".material", item.getMaterial().name());
        config.set(ip + ".display-name", item.getDisplayName());
        config.set(ip + ".lore", item.getLore());
        config.set(ip + ".amount", item.getAmount());
        config.set(ip + ".price", item.getPrice());
        config.set(ip + ".slot", item.getSlot());
        config.set(ip + ".glowing", item.isGlowing());
        config.set(ip + ".custom-model-data", item.getCustomModelData());
        config.set(ip + ".permission", item.getPermission());
        config.set(ip + ".purchase-limit", item.getPurchaseLimit());
        config.set(ip + ".commands-on-purchase", item.getCommandsOnPurchase());

        config.set(ip + ".stock", item.getStock());
        config.set(ip + ".max-stock", item.getMaxStock());
        config.set(ip + ".unlimited-stock", item.isUnlimitedStock());

        if (item.isUseSerializedItem() && item.getSerializedItem() != null) {
          config.set(ip + ".serialized-item", item.getSerializedItem());
          config.set(ip + ".use-serialized", true);
        }

        if (!item.getEnchantments().isEmpty()) {
          for (Map.Entry<Enchantment, Integer> e : item.getEnchantments().entrySet()) {
            config.set(ip + ".enchantments." + e.getKey().getKey().getKey(), e.getValue());
          }
        }
      }
    }
    saveConfig(config, templatesFile);
  }

  public void loadPlacements() {
    ensureFile(placementsFile);
    FileConfiguration config = YamlConfiguration.loadConfiguration(placementsFile);
    ConfigurationSection section = config.getConfigurationSection("placements");
    if (section == null) return;

    for (String key : section.getKeys(false)) {
      ConfigurationSection ps = section.getConfigurationSection(key);
      if (ps == null) continue;

      try {
        UUID id = UUID.fromString(ps.getString("id", UUID.randomUUID().toString()));
        String templateName = ps.getString("template");
        String world = ps.getString("world");

        if (templateName == null || templateName.isBlank()) {
          plugin.getLogger().warning("Skipped placement " + key + " - missing template name.");
          continue;
        }

        MachineTemplate template = plugin.getMachineManager().getTemplate(templateName);
        if (template == null) {
          plugin
              .getLogger()
              .warning(
                  "Skipped placement "
                      + key
                      + " - template '"
                      + templateName
                      + "' does not exist.");
          continue;
        }

        if (world == null || Bukkit.getWorld(world) == null) {
          plugin.getLogger().warning("World '" + world + "' not found for placement " + key);
          continue;
        }

        Location loc =
            new Location(
                Bukkit.getWorld(world), ps.getDouble("x"), ps.getDouble("y"), ps.getDouble("z"));
        MachinePlacement placement = new MachinePlacement(id, templateName, loc);
        placement.setNexoItemId(ps.getString("nexo-item-id", null));
        placement.setNexoEntityUUID(ps.getString("nexo-entity-uuid", null));

        String placedByStr = ps.getString("placed-by", null);
        if (placedByStr != null) {
          try {
            placement.setPlacedBy(UUID.fromString(placedByStr));
          } catch (Exception ignored) {
          }
        }

        ConfigurationSection stockSec = ps.getConfigurationSection("stock");
        if (stockSec != null) {
          Map<String, Integer> stockMap = new HashMap<>();
          for (String itemId : stockSec.getKeys(false)) {
            stockMap.put(itemId, stockSec.getInt(itemId, 0));
          }
          placement.setStockMap(stockMap);
        }

        placement.syncWithTemplate(template);
        plugin.getPlacementManager().addPlacement(placement);
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to load placement: " + key + " - " + e.getMessage());
      }
    }

    plugin
        .getLogger()
        .info("Loaded " + plugin.getPlacementManager().getAllPlacements().size() + " placements.");
  }

  public void savePlacements() {
    placementsDirty = false;
    savePlacementsNow();
  }

  private void savePlacementsNow() {
    FileConfiguration config = new YamlConfiguration();
    int index = 0;
    for (MachinePlacement p : plugin.getPlacementManager().getAllPlacements()) {
      Location loc = p.getLocation();
      if (loc == null || loc.getWorld() == null) continue;

      String path = "placements.p" + index;
      config.set(path + ".id", p.getId().toString());
      config.set(path + ".template", p.getTemplateName());
      config.set(path + ".world", loc.getWorld().getName());
      config.set(path + ".x", loc.getX());
      config.set(path + ".y", loc.getY());
      config.set(path + ".z", loc.getZ());
      config.set(path + ".nexo-item-id", p.getNexoItemId());
      config.set(path + ".nexo-entity-uuid", p.getNexoEntityUUID());
      config.set(path + ".placed-by", p.getPlacedBy() != null ? p.getPlacedBy().toString() : null);

      for (Map.Entry<String, Integer> e : p.getStockMap().entrySet()) {
        config.set(path + ".stock." + e.getKey(), e.getValue());
      }

      index++;
    }
    saveConfig(config, placementsFile);
  }

  public void queueSaveTemplates() {
    templatesDirty = true;
    scheduleQueuedSave();
  }

  public void queueSavePlacements() {
    placementsDirty = true;
    scheduleQueuedSave();
  }

  private void scheduleQueuedSave() {
    if (queuedSaveTask != null) return;
    queuedSaveTask =
        Bukkit.getScheduler()
            .runTaskLater(plugin.getPlugin(), this::flushQueuedSaves, SAVE_DELAY_TICKS);
  }

  public void flushQueuedSaves() {
    BukkitTask task = queuedSaveTask;
    queuedSaveTask = null;
    if (task != null && !task.isCancelled()) {
      task.cancel();
    }

    boolean saveTemplates = templatesDirty;
    boolean savePlacements = placementsDirty;
    templatesDirty = false;
    placementsDirty = false;

    if (saveTemplates) saveTemplatesNow();
    if (savePlacements) savePlacementsNow();
  }

  public void saveAll() {
    BukkitTask task = queuedSaveTask;
    queuedSaveTask = null;
    if (task != null && !task.isCancelled()) {
      task.cancel();
    }
    templatesDirty = false;
    placementsDirty = false;
    saveTemplatesNow();
    savePlacementsNow();
  }

  public void loadAll() {
    loadTemplates();
    loadPlacements();
  }

  private Material material(ConfigurationSection section, String path, Material fallback) {
    String raw = section.getString(path, fallback.name());
    if (raw == null) return fallback;
    try {
      return Material.valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      plugin
          .getLogger()
          .warning(
              "Invalid material '"
                  + raw
                  + "' at "
                  + section.getCurrentPath()
                  + "."
                  + path
                  + ", using "
                  + fallback.name());
      return fallback;
    }
  }

  private void migrateLegacyDataFiles() {
    File oldConfigDir = new File(plugin.getPlugin().getDataFolder(), "config");
    File oldTemplates = new File(oldConfigDir, "automat-templates.yml");
    File oldPlacements = new File(oldConfigDir, "automat-placements.yml");

    migrateLegacyFile(oldTemplates, templatesFile);
    migrateLegacyFile(oldPlacements, placementsFile);

    if (oldConfigDir.isDirectory()) {
      String[] children = oldConfigDir.list();
      if (children == null || children.length == 0) oldConfigDir.delete();
    }
  }

  private void migrateLegacyFile(File legacy, File target) {
    if (target.exists() || !legacy.exists()) return;
    try {
      File parent = target.getParentFile();
      if (parent != null) parent.mkdirs();
      Files.move(legacy.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
      plugin
          .getLogger()
          .info(
              "Automat: przeniesiono " + legacy.getName() + " do config/" + target.getName() + ".");
    } catch (IOException e) {
      plugin
          .getLogger()
          .warning(
              "Automat: nie udało się przenieść starego pliku "
                  + legacy.getName()
                  + ": "
                  + e.getMessage());
    }
  }

  private void ensureFile(File file) {
    if (!file.exists()) {
      try {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        file.createNewFile();
      } catch (IOException e) {
        plugin.getLogger().severe("Could not create " + file.getName());
      }
    }
  }

  private void saveConfig(FileConfiguration config, File file) {
    try {
      File parent = file.getParentFile();
      if (parent != null) parent.mkdirs();
      config.save(file);
    } catch (IOException e) {
      plugin.getLogger().severe("Could not save " + file.getName() + ": " + e.getMessage());
    }
  }
}
