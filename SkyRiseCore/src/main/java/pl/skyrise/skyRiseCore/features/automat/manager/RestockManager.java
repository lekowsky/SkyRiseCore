package pl.skyrise.skyRiseCore.features.automat.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class RestockManager {

  private final AutomatModule plugin;
  private final Map<String, Long> nextRestockAtMillis = new HashMap<>();
  private BukkitTask restockDispatcherTask;
  private BukkitTask lowStockScanTask;

  public RestockManager(AutomatModule plugin) {
    this.plugin = plugin;
  }

  public void startAll() {
    cancelRestockDispatcher();
    nextRestockAtMillis.clear();

    long now = System.currentTimeMillis();
    for (MachineTemplate template : plugin.getMachineManager().getAllTemplates()) {
      if (template.isAutoRestockEnabled()) {
        scheduleTemplate(template, now);
      }
    }
    scheduleNextRestock();
    plugin
        .getLogger()
        .info("[Restock] Zaplanowano " + nextRestockAtMillis.size() + " timerów restocku.");

    startLowStockScan();
  }

  public void stopAll() {
    cancelRestockDispatcher();
    nextRestockAtMillis.clear();

    if (lowStockScanTask != null) {
      lowStockScanTask.cancel();
      lowStockScanTask = null;
    }
  }

  public void startRestock(MachineTemplate template) {
    if (template == null) return;

    String key = key(template.getName());
    nextRestockAtMillis.remove(key);
    if (template.isAutoRestockEnabled()) {
      scheduleTemplate(template, System.currentTimeMillis());
      plugin
          .getLogger()
          .info(
              "[Restock] Timer dla '"
                  + template.getName()
                  + "' — co "
                  + template.getAutoRestockInterval()
                  + " min, +"
                  + template.getAutoRestockAmount()
                  + " szt./placement.");
    }
    scheduleNextRestock();
  }

  public int restockAllPlacementsForTemplate(MachineTemplate template) {
    int count = 0;
    int amount = template.getAutoRestockAmount();

    for (MachinePlacement placement : plugin.getPlacementManager().getAllPlacements()) {
      if (!placement.getTemplateName().equalsIgnoreCase(template.getName())) continue;

      boolean added = false;
      for (VendingItem item : template.getItems().values()) {
        if (item.isUnlimitedStock()) continue;
        if (placement.addStock(item.getId(), amount, item.getMaxStock()) > 0) {
          added = true;
        }
      }
      if (added) count++;
    }
    return count;
  }

  public void fillPlacement(MachinePlacement placement) {
    MachineTemplate template = plugin.getMachineManager().getTemplate(placement.getTemplateName());
    if (template == null) return;

    for (VendingItem item : template.getItems().values()) {
      if (!item.isUnlimitedStock()) {
        placement.setStock(item.getId(), item.getMaxStock());
      }
    }
    plugin.getDataManager().queueSavePlacements();
  }

  public void stopRestock(String templateName) {
    if (templateName == null) return;
    if (nextRestockAtMillis.remove(key(templateName)) != null) {
      scheduleNextRestock();
    }
  }

  public void restartRestock(MachineTemplate template) {
    startRestock(template);
  }

  private void scheduleTemplate(MachineTemplate template, long now) {
    nextRestockAtMillis.put(key(template.getName()), now + restockIntervalMillis(template));
  }

  private void scheduleNextRestock() {
    cancelRestockDispatcher();
    if (nextRestockAtMillis.isEmpty()) return;

    long now = System.currentTimeMillis();
    long nearest = Long.MAX_VALUE;
    for (long scheduledAt : nextRestockAtMillis.values()) {
      if (scheduledAt < nearest) {
        nearest = scheduledAt;
      }
    }

    long delayMillis = Math.max(1L, nearest - now);
    long delayTicks = Math.max(1L, (delayMillis + 49L) / 50L);
    restockDispatcherTask =
        Bukkit.getScheduler().runTaskLater(plugin.getPlugin(), this::runDueRestocks, delayTicks);
  }

  private void runDueRestocks() {
    restockDispatcherTask = null;
    long now = System.currentTimeMillis();

    for (String templateKey : new ArrayList<>(nextRestockAtMillis.keySet())) {
      Long scheduledAt = nextRestockAtMillis.get(templateKey);
      if (scheduledAt == null || scheduledAt > now) continue;

      MachineTemplate template = plugin.getMachineManager().getTemplate(templateKey);
      if (template == null || !template.isAutoRestockEnabled()) {
        nextRestockAtMillis.remove(templateKey);
        continue;
      }

      performAutoRestock(template);
      nextRestockAtMillis.put(templateKey, now + restockIntervalMillis(template));
    }

    scheduleNextRestock();
  }

  private void performAutoRestock(MachineTemplate template) {
    int restocked = restockAllPlacementsForTemplate(template);
    if (restocked <= 0) return;

    plugin.getDataManager().queueSavePlacements();

    if (plugin.getConfig().getBoolean("auto-restock.notify-on-restock", true)) {
      plugin
          .getLogger()
          .info(
              "[Restock] Auto-restocked "
                  + restocked
                  + " automat(y) typu '"
                  + template.getName()
                  + "' (+"
                  + template.getAutoRestockAmount()
                  + " szt./item)");
    }

    if (plugin.getConfig().getBoolean("auto-restock.broadcast-restock", false)) {
      String msg =
          ColorUtil.miniToLegacy(
              plugin
                  .getConfig()
                  .getString(
                      "auto-restock.broadcast-message",
                      "<reset><#38f28f><bold>Automat <reset><gray>» <reset><white>Uzupełniono"
                          + " zapasy automatu <reset><yellow>{template}<reset><white>.")
                  .replace("{template}", template.getName()));
      Bukkit.broadcastMessage(msg);
    }

    playRestockSound(template);
  }

  private long restockIntervalMillis(MachineTemplate template) {
    return Math.max(1L, template.getAutoRestockInterval()) * 60_000L;
  }

  private void cancelRestockDispatcher() {
    if (restockDispatcherTask != null) {
      restockDispatcherTask.cancel();
      restockDispatcherTask = null;
    }
  }

  private void startLowStockScan() {
    if (lowStockScanTask != null) {
      lowStockScanTask.cancel();
      lowStockScanTask = null;
    }
    if (!plugin.getConfig().getBoolean("stock.low-stock-warning.enabled", true)) return;

    int scanIntervalMin =
        Math.max(1, plugin.getConfig().getInt("stock.low-stock-warning.scan-interval", 5));
    long ticks = scanIntervalMin * 60L * 20L;

    lowStockScanTask =
        Bukkit.getScheduler()
            .runTaskTimer(plugin.getPlugin(), this::scanAllForLowStock, ticks, ticks);
    plugin.getLogger().info("[Stock] Low-stock scanner started (co " + scanIntervalMin + " min)");
  }

  public void scanAllForLowStock() {
    int threshold = plugin.getConfig().getInt("stock.low-stock-warning.threshold", 5);
    int emptyCount = 0;
    int lowCount = 0;

    for (MachinePlacement placement : plugin.getPlacementManager().getAllPlacements()) {
      MachineTemplate template =
          plugin.getMachineManager().getTemplate(placement.getTemplateName());
      if (template == null) continue;

      boolean hasEmpty = false;
      boolean hasLow = false;
      for (VendingItem item : template.getItems().values()) {
        if (item.isUnlimitedStock()) continue;
        int stock = placement.getStock(item.getId());
        if (stock == 0) hasEmpty = true;
        else if (stock <= threshold) hasLow = true;
      }

      if (hasEmpty) emptyCount++;
      else if (hasLow) lowCount++;
    }

    if (emptyCount == 0 && lowCount == 0) return;

    String summary =
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><red>⚠ Automaty wymagają uzupełnienia! <reset><red>Wyprzedane:"
                    + " <reset><white>"
                    + emptyCount
                    + " <reset><gray>| <reset><yellow>Niski stock: <reset><white>"
                    + lowCount
                    + " <reset><dark_gray>(/automat restocklist)");

    if (plugin.getConfig().getBoolean("stock.low-stock-warning.log-to-console", true)) {
      plugin.getLogger().info(ColorUtil.miniPlain(summary));
    }
    notifyRestockers(summary);
  }

  public void notifyItemSoldOut(MachinePlacement placement, VendingItem item) {
    MachineTemplate template = plugin.getMachineManager().getTemplate(placement.getTemplateName());
    if (template == null) return;

    Location loc = placement.getLocation();
    String locStr =
        loc.getWorld().getName()
            + " "
            + loc.getBlockX()
            + ","
            + loc.getBlockY()
            + ","
            + loc.getBlockZ();

    String msg =
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                plugin
                    .getConfig()
                    .getString(
                        "messages.stock-empty",
                        "<reset><red>⚠ Wyprzedano <reset><white>{item} <reset><red>w automacie"
                            + " <reset><gold>{template}<reset><red>!")
                    .replace("{item}", ColorUtil.miniToLegacy(item.getDisplayName()))
                    .replace("{template}", template.getName())
                    .replace("{location}", locStr));

    if (plugin.getConfig().getBoolean("stock.low-stock-warning.log-to-console", true)) {
      plugin.getLogger().warning("[Stock] " + ColorUtil.miniPlain(msg) + " @ " + locStr);
    }
    notifyRestockers(msg);
  }

  public void notifyItemLowStock(MachinePlacement placement, VendingItem item, int currentStock) {
    MachineTemplate template = plugin.getMachineManager().getTemplate(placement.getTemplateName());
    if (template == null) return;

    Location loc = placement.getLocation();
    String locStr =
        loc.getWorld().getName()
            + " "
            + loc.getBlockX()
            + ","
            + loc.getBlockY()
            + ","
            + loc.getBlockZ();

    String msg =
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                plugin
                    .getConfig()
                    .getString(
                        "messages.stock-low-warning",
                        "<reset><yellow>⚠ Niski stock w automacie"
                            + " <reset><gold>{template}<reset><yellow>: <reset><white>{item}"
                            + " <reset><gray>(<reset><yellow>{stock}<reset><gray>/<reset><yellow>{max}<reset><gray>)")
                    .replace("{item}", ColorUtil.miniToLegacy(item.getDisplayName()))
                    .replace("{template}", template.getName())
                    .replace("{stock}", String.valueOf(currentStock))
                    .replace("{max}", String.valueOf(item.getMaxStock()))
                    .replace("{location}", locStr));

    notifyRestockers(msg);
  }

  private void notifyRestockers(String message) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.hasPermission("vendingmachine.restock.notify")) {
        player.sendMessage(message);
      }
    }
  }

  private void playRestockSound(MachineTemplate template) {
    try {
      Sound sound =
          Sound.valueOf(plugin.getConfig().getString("sounds.restock", "BLOCK_NOTE_BLOCK_PLING"));
      for (MachinePlacement placement : plugin.getPlacementManager().getAllPlacements()) {
        if (!placement.getTemplateName().equalsIgnoreCase(template.getName())) continue;
        Location loc = placement.getLocation();
        if (loc.getWorld() == null) continue;

        for (Player player : loc.getWorld().getPlayers()) {
          Location playerLocation = player.getLocation();
          double dx = playerLocation.getX() - loc.getX();
          double dy = playerLocation.getY() - loc.getY();
          double dz = playerLocation.getZ() - loc.getZ();
          if (dx * dx + dy * dy + dz * dz <= 100.0) {
            player.playSound(loc, sound, 0.3f, 1.2f);
          }
        }
      }
    } catch (IllegalArgumentException ignored) {

    }
  }

  private static String key(String name) {
    return name.toLowerCase(Locale.ROOT);
  }
}
