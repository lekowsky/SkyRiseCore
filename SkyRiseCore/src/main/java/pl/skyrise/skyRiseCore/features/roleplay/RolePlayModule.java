package pl.skyrise.skyRiseCore.features.roleplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.utils.ColorUtil;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class RolePlayModule implements Module {

  private org.bukkit.event.Listener listener;

  private final JavaPlugin plugin;
  private final TabRegistry tabRegistry;
  private CustomConfig config;

  private int rpRange;
  private int headTextDuration;
  private String formatMe;
  private String formatDo;
  private String formatTry;
  private String formatTrySuccess;
  private String formatTryFailure;
  private String formatOpis;
  private String formatOpis2;

  private final Map<UUID, TextDisplay> activeHeadTexts = new ConcurrentHashMap<>();
  private final Map<UUID, BukkitTask> activeHeadTasks = new ConcurrentHashMap<>();
  private final Map<UUID, TextDisplay> activePlayerOpis = new ConcurrentHashMap<>();
  private final Map<UUID, TextDisplay> activeLocationOpis = new ConcurrentHashMap<>();
  private final Map<UUID, BukkitTask> activeLocationTasks = new ConcurrentHashMap<>();

  public RolePlayModule(JavaPlugin plugin, TabRegistry tabRegistry) {
    this.plugin = plugin;
    this.tabRegistry = tabRegistry;
  }

  @Override
  public String getName() {
    return "RolePlay";
  }

  @Override
  public void onEnable() {
    config = new CustomConfig(plugin, "roleplay.yml");
    config.load();
    cacheConfig();

    RolePlayCommands commands = new RolePlayCommands(this);
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindExecutor(
        plugin, commands, "me", "do", "try", "opis", "opis2");

    tabRegistry.register("me", (sender, args) -> List.of());
    tabRegistry.register("do", (sender, args) -> List.of());
    tabRegistry.register("try", (sender, args) -> List.of());
    tabRegistry.register("opis", (sender, args) -> List.of());
    tabRegistry.register("opis2", (sender, args) -> List.of());

    this.listener =
        pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(
            plugin, new RolePlayListener(this));

    plugin.getLogger().info("  → RolePlay (Butter-Smooth Passenger Engine): Załadowano pomyślnie.");
  }

  @Override
  public void onDisable() {
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindDisabled(
        plugin, getName(), "me", "do", "try", "opis", "opis2");
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListener(this.listener);
    this.listener = null;

    activeHeadTexts.values().forEach(TextDisplay::remove);
    activeHeadTasks.values().forEach(BukkitTask::cancel);
    activePlayerOpis.values().forEach(TextDisplay::remove);
    activeLocationOpis.values().forEach(TextDisplay::remove);
    activeLocationTasks.values().forEach(BukkitTask::cancel);

    activeHeadTexts.clear();
    activeHeadTasks.clear();
    activePlayerOpis.clear();
    activeLocationOpis.clear();
    activeLocationTasks.clear();

    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterTabs(
        tabRegistry, "me", "do", "try", "opis", "opis2");

    config.save();
  }

  @Override
  public void onReload() {
    config.reload();
    cacheConfig();
  }

  private void cacheConfig() {
    this.rpRange = config.getConfig().getInt("rp-range", 20);
    this.headTextDuration = config.getConfig().getInt("head-text-duration", 6);

    this.formatMe =
        config.getConfig().getString("formats.me", "<color:#D15DFF>* {player} {message} *");
    this.formatDo =
        config.getConfig().getString("formats.do", "<color:#5D9DFF>* {message} * ({player})");
    this.formatTry =
        config
            .getConfig()
            .getString("formats.try", "<color:#FFAC5D>* {player} próbuje {message}... {result} *");
    this.formatTrySuccess =
        config.getConfig().getString("formats.try-success", "<green>[UDANE]</green>");
    this.formatTryFailure =
        config.getConfig().getString("formats.try-failure", "<red>[NIEUDANE]</red>");
    this.formatOpis =
        config.getConfig().getString("formats.opis", "<color:#FFEB5D>[OPIS] {message}");
    this.formatOpis2 =
        config.getConfig().getString("formats.opis2", "<color:#5DFFEB>[MIEJSCE] {message}");
  }

  public void showHeadText(Player player, String text) {
    UUID uuid = player.getUniqueId();

    removeHeadText(uuid);

    TextDisplay display =
        player
            .getWorld()
            .spawn(
                player.getLocation(),
                TextDisplay.class,
                ent -> {
                  ent.text(ColorUtil.mini(text));
                  ent.setBillboard(Billboard.CENTER);
                  ent.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
                  ent.setSeeThrough(false);

                  Transformation trans = ent.getTransformation();
                  trans.getTranslation().set(0.0f, 0.55f, 0.0f);
                  ent.setTransformation(trans);
                });

    player.addPassenger(display);
    activeHeadTexts.put(uuid, display);

    BukkitTask task =
        plugin
            .getServer()
            .getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  removeHeadText(uuid);
                },
                headTextDuration * 20L);

    activeHeadTasks.put(uuid, task);
  }

  public void removeHeadText(UUID uuid) {
    TextDisplay display = activeHeadTexts.remove(uuid);
    if (display != null) {
      display.remove();
    }
    BukkitTask task = activeHeadTasks.remove(uuid);
    if (task != null) {
      task.cancel();
    }
  }

  public boolean hasOpis(UUID uuid) {
    return activePlayerOpis.containsKey(uuid);
  }

  public void setPlayerOpis(Player player, String text) {
    UUID uuid = player.getUniqueId();
    removePlayerOpis(uuid);

    TextDisplay display =
        player
            .getWorld()
            .spawn(
                player.getLocation(),
                TextDisplay.class,
                ent -> {
                  ent.text(ColorUtil.mini(text));
                  ent.setBillboard(Billboard.CENTER);
                  ent.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
                  ent.setSeeThrough(false);

                  Transformation trans = ent.getTransformation();
                  trans.getTranslation().set(0.0f, -0.8f, 0.0f);
                  ent.setTransformation(trans);
                });

    player.addPassenger(display);
    activePlayerOpis.put(uuid, display);
  }

  public void removePlayerOpis(UUID uuid) {
    TextDisplay display = activePlayerOpis.remove(uuid);
    if (display != null) {
      display.remove();
    }
  }

  public boolean hasOpis2(UUID uuid) {
    return activeLocationOpis.containsKey(uuid);
  }

  public void setLocationOpis(Player player, String text, int minutes) {
    UUID uuid = player.getUniqueId();
    removeLocationOpis(uuid);

    Location loc = player.getLocation().add(0, 1.1, 0);
    TextDisplay display =
        player
            .getWorld()
            .spawn(
                loc,
                TextDisplay.class,
                ent -> {
                  ent.text(ColorUtil.mini(text));
                  ent.setBillboard(Billboard.CENTER);
                  ent.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
                  ent.setSeeThrough(false);
                });

    activeLocationOpis.put(uuid, display);

    int durationMinutes = Math.min(minutes, 120);
    BukkitTask task =
        plugin
            .getServer()
            .getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  removeLocationOpis(uuid);
                },
                durationMinutes * 1200L);

    activeLocationTasks.put(uuid, task);
  }

  public void removeLocationOpis(UUID uuid) {
    TextDisplay display = activeLocationOpis.remove(uuid);
    if (display != null) {
      display.remove();
    }
    BukkitTask task = activeLocationTasks.remove(uuid);
    if (task != null) {
      task.cancel();
    }
  }

  public void cleanPlayer(UUID uuid) {
    removeHeadText(uuid);
    removePlayerOpis(uuid);
    removeLocationOpis(uuid);
  }

  public int getRpRange() {
    return rpRange;
  }

  public String getFormatMe() {
    return formatMe;
  }

  public String getFormatDo() {
    return formatDo;
  }

  public String getFormatTry() {
    return formatTry;
  }

  public String getFormatTrySuccess() {
    return formatTrySuccess;
  }

  public String getFormatTryFailure() {
    return formatTryFailure;
  }

  public String getFormatOpis() {
    return formatOpis;
  }

  public String getFormatOpis2() {
    return formatOpis2;
  }

  public JavaPlugin getPlugin() {
    return plugin;
  }
}
