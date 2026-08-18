package pl.skyrise.skyRiseCore.features.freeze;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.utils.ColorUtil;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class FreezeModule implements Module {

  private org.bukkit.event.Listener listener;

  private final JavaPlugin plugin;
  private final TabRegistry tabRegistry;
  private CustomConfig config;

  private String freezeChat;
  private String freezeActionbar;
  private Component freezeChatComponent;
  private Component freezeActionbarComponent;
  private String permission;
  private Location freezeLocation;

  private final Map<UUID, Location> frozenPlayers = new ConcurrentHashMap<>();
  private final Map<UUID, Float> lockedYaws = new ConcurrentHashMap<>();
  private final Map<UUID, Float> lockedPitches = new ConcurrentHashMap<>();

  private BukkitTask actionbarTask;

  public FreezeModule(JavaPlugin plugin, TabRegistry tabRegistry) {
    this.plugin = plugin;
    this.tabRegistry = tabRegistry;
  }

  @Override
  public String getName() {
    return "Freeze";
  }

  @Override
  public void onEnable() {
    config = new CustomConfig(plugin, "freeze.yml");
    config.load();
    cacheConfig();

    FreezeCommands commands = new FreezeCommands(this);
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindExecutor(
        plugin, commands, "freeze", "unfreeze", "setfreeze", "delfreeze");

    tabRegistry.register(
        "freeze",
        (sender, args) -> {
          if (args.length == 1) {
            List<String> players = new ArrayList<>();
            plugin.getServer().getOnlinePlayers().forEach(p -> players.add(p.getName()));
            return TabRegistry.filter(players, args[0]);
          }
          return List.of();
        });
    tabRegistry.register(
        "unfreeze",
        (sender, args) -> {
          if (args.length == 1) {
            List<String> frozen = new ArrayList<>();
            for (UUID uuid : frozenPlayers.keySet()) {
              Player p = plugin.getServer().getPlayer(uuid);
              if (p != null) frozen.add(p.getName());
              else {
                org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(uuid);
                if (op.getName() != null) frozen.add(op.getName());
              }
            }
            return TabRegistry.filter(frozen, args[0]);
          }
          return List.of();
        });
    tabRegistry.register("setfreeze", (sender, args) -> List.of());
    tabRegistry.register("delfreeze", (sender, args) -> List.of());

    this.listener =
        pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(
            plugin, new FreezeListener(this));

    actionbarTask =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  for (UUID uuid : frozenPlayers.keySet()) {
                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                      player.sendActionBar(freezeActionbarComponent);
                    }
                  }
                },
                40L,
                40L);

    plugin
        .getLogger()
        .info("  → Freeze: Załadowano pomyślnie. Zamrożonych graczy: " + frozenPlayers.size());
  }

  @Override
  public void onDisable() {
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindDisabled(
        plugin, getName(), "freeze", "unfreeze", "setfreeze", "delfreeze");
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListener(this.listener);
    this.listener = null;
    if (actionbarTask != null) {
      actionbarTask.cancel();
    }

    for (UUID uuid : frozenPlayers.keySet()) {
      Player player = plugin.getServer().getPlayer(uuid);
      if (player != null && player.isOnline()) {
        revealPlayer(player);
      }
    }

    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterTabs(
        tabRegistry, "freeze", "unfreeze", "setfreeze", "delfreeze");

    saveData();
  }

  @Override
  public void onReload() {
    config.reload();
    cacheConfig();
  }

  private void cacheConfig() {
    this.freezeChat =
        config
            .getConfig()
            .getString(
                "freeze-chat",
                "<red>» Zostałeś/aś zamrożony/a! Wejdź pilnie na nasz Discord:"
                    + " <click:open_url:'https://discord.gg/lekoland'><yellow><underlined>discord.gg/lekoland</underlined></yellow></click>");
    this.freezeActionbar =
        config
            .getConfig()
            .getString("freeze-actionbar", "<red>» Zostałeś/aś zamrożony/a – wejdź na Discorda!");

    this.freezeChatComponent = ColorUtil.mini(freezeChat);
    this.freezeActionbarComponent = ColorUtil.mini(freezeActionbar);
    this.permission = config.getConfig().getString("permission", "skyrise.freeze");

    if (config.getConfig().contains("freeze-location")) {
      this.freezeLocation = config.getConfig().getLocation("freeze-location");
    } else {
      this.freezeLocation = null;
    }

    frozenPlayers.clear();
    lockedYaws.clear();
    lockedPitches.clear();

    org.bukkit.configuration.ConfigurationSection section =
        config.getConfig().getConfigurationSection("frozen-players");
    if (section != null) {
      for (String key : section.getKeys(false)) {
        try {
          UUID uuid = UUID.fromString(key);
          Location loc = section.getLocation(key + ".return-loc");
          frozenPlayers.put(uuid, loc != null ? loc : playerDefaultLocation());

          float yaw = (float) section.getDouble(key + ".yaw", 0.0);
          float pitch = (float) section.getDouble(key + ".pitch", 0.0);
          lockedYaws.put(uuid, yaw);
          lockedPitches.put(uuid, pitch);
        } catch (IllegalArgumentException ignored) {
        }
      }
    }
  }

  public void saveData() {
    if (config == null) return;

    config.getConfig().set("freeze-location", freezeLocation);

    config.getConfig().set("frozen-players", null);
    for (Map.Entry<UUID, Location> entry : frozenPlayers.entrySet()) {
      UUID uuid = entry.getKey();
      String path = "frozen-players." + uuid.toString();
      config.getConfig().set(path + ".return-loc", entry.getValue());
      config.getConfig().set(path + ".yaw", lockedYaws.getOrDefault(uuid, 0.0f));
      config.getConfig().set(path + ".pitch", lockedPitches.getOrDefault(uuid, 0.0f));
    }

    config.save();
  }

  public boolean isFrozen(UUID uuid) {
    return frozenPlayers.containsKey(uuid);
  }

  public Set<UUID> getFrozenPlayers() {
    return frozenPlayers.keySet();
  }

  public float getLockedYaw(UUID uuid) {
    return lockedYaws.getOrDefault(uuid, 0.0f);
  }

  public float getLockedPitch(UUID uuid) {
    return lockedPitches.getOrDefault(uuid, 0.0f);
  }

  public Location getFreezeLocation() {
    return freezeLocation;
  }

  public void setFreezeLocation(Location loc) {
    this.freezeLocation = loc;
    saveData();
  }

  public void removeFreezeLocation() {
    this.freezeLocation = null;
    saveData();
  }

  public String getFreezeChat() {
    return freezeChat;
  }

  public String getFreezeActionbar() {
    return freezeActionbar;
  }

  public Component getFreezeChatComponent() {
    return freezeChatComponent;
  }

  public Component getFreezeActionbarComponent() {
    return freezeActionbarComponent;
  }

  public String getPermission() {
    return permission;
  }

  public JavaPlugin getPlugin() {
    return plugin;
  }

  private Location playerDefaultLocation() {
    World w = Bukkit.getWorlds().get(0);
    return new Location(w, 0.5, 64, 0.5);
  }

  public boolean freezePlayer(UUID uuid, Location returnLoc, float yaw, float pitch) {
    if (isFrozen(uuid)) return false;

    frozenPlayers.put(uuid, returnLoc);
    lockedYaws.put(uuid, yaw);
    lockedPitches.put(uuid, pitch);
    saveData();

    Player player = Bukkit.getPlayer(uuid);
    if (player != null && player.isOnline()) {
      applyFreezeEffects(player);
    }
    return true;
  }

  public boolean unfreezePlayer(UUID uuid) {
    if (!isFrozen(uuid)) return false;

    Location returnLoc = frozenPlayers.remove(uuid);
    lockedYaws.remove(uuid);
    lockedPitches.remove(uuid);
    saveData();

    Player player = Bukkit.getPlayer(uuid);
    if (player != null && player.isOnline()) {
      revealPlayer(player);
      if (returnLoc != null) {
        player.teleport(returnLoc);
      }
      player.sendMessage(ColorUtil.mini("<green>» Zostałeś/aś odmrożony/a!"));
      player.sendActionBar(ColorUtil.mini("<green>» Odmrożono!"));
    }
    return true;
  }

  public void applyFreezeEffects(Player player) {
    Location dest = freezeLocation != null ? freezeLocation.clone() : playerDefaultLocation();
    dest.setYaw(lockedYaws.getOrDefault(player.getUniqueId(), 0.0f));
    dest.setPitch(lockedPitches.getOrDefault(player.getUniqueId(), 0.0f));

    player.teleport(dest);
    hidePlayer(player);

    player.sendMessage(freezeChatComponent);

    net.kyori.adventure.title.Title title =
        net.kyori.adventure.title.Title.title(
            ColorUtil.mini("<red>Zamrożenie!</red>"),
            freezeActionbarComponent,
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(500),
                java.time.Duration.ofMillis(2000),
                java.time.Duration.ofMillis(500)));
    player.showTitle(title);
  }

  public void hidePlayer(Player target) {
    for (Player online : Bukkit.getOnlinePlayers()) {
      if (online != target) {
        online.hidePlayer(plugin, target);
      }
    }
  }

  public void revealPlayer(Player target) {
    for (Player online : Bukkit.getOnlinePlayers()) {
      if (online != target) {
        online.showPlayer(plugin, target);
      }
    }
  }
}
