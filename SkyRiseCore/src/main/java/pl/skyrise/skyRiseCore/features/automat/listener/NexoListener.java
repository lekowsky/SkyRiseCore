package pl.skyrise.skyRiseCore.features.automat.listener;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.EventExecutor;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.gui.MachineGUI;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class NexoListener implements Listener {

  private final AutomatModule plugin;
  private final Map<UUID, Long> interactCooldowns = new ConcurrentHashMap<>();
  private final Map<String, Method> methodCache = new ConcurrentHashMap<>();
  private boolean nexoAvailable;

  public NexoListener(AutomatModule plugin) {
    this.plugin = plugin;
    registerNexoEvents();
  }

  private void registerNexoEvents() {
    try {
      Class<?> placeEventClass =
          tryLoadClass("com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent");
      Class<?> interactEventClass =
          tryLoadClass("com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent");
      Class<?> breakEventClass =
          tryLoadClass("com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent");

      if (interactEventClass == null) {
        plugin.getLogger().warning("Nexo classes not found - integration disabled.");
        nexoAvailable = false;
        return;
      }

      nexoAvailable = true;

      if (placeEventClass != null) {
        registerEvent(placeEventClass, EventPriority.MONITOR, true, this::handlePlaceReflection);
        plugin.getLogger().info("Nexo PLACE event registered - auto-registration enabled.");
      } else {
        plugin
            .getLogger()
            .warning("NexoFurniturePlaceEvent not found - auto-registration disabled.");
      }

      registerEvent(interactEventClass, EventPriority.HIGH, true, this::handleInteractReflection);

      if (breakEventClass != null) {
        registerEvent(breakEventClass, EventPriority.HIGHEST, true, this::handleBreakReflection);
      }

      plugin.getLogger().info("Nexo events registered successfully.");
    } catch (Throwable t) {
      plugin.getLogger().warning("Failed to register Nexo events: " + t.getMessage());
      nexoAvailable = false;
    }
  }

  @SuppressWarnings("unchecked")
  private void registerEvent(
      Class<?> eventClass,
      EventPriority priority,
      boolean ignoreCancelled,
      EventHandlerCallback callback) {
    Bukkit.getPluginManager()
        .registerEvent(
            (Class<? extends Event>) eventClass,
            this,
            priority,
            (EventExecutor)
                (listener, event) -> {
                  try {
                    callback.handle(event);
                  } catch (Throwable t) {
                    plugin.getLogger().warning("Error in Nexo event handler: " + t.getMessage());
                  }
                },
            plugin.getPlugin(),
            ignoreCancelled);
  }

  private void handlePlaceReflection(Event event) throws Exception {
    if (!plugin.getNexoManager().isAutoRegister()) return;

    String nexoItemId = getMechanicItemID(event);
    if (nexoItemId == null) return;

    String templateName = plugin.getNexoManager().getTemplateName(nexoItemId);
    if (templateName == null) return;

    MachineTemplate template = plugin.getMachineManager().getTemplate(templateName);
    if (template == null) {
      plugin
          .getLogger()
          .warning(
              "Furniture '" + nexoItemId + "' maps to missing template '" + templateName + "'.");
      return;
    }

    Entity baseEntity = (Entity) invokeNoArg(event, "getBaseEntity");
    if (baseEntity == null) return;

    Location blockLoc = baseEntity.getLocation().getBlock().getLocation();
    if (plugin.getPlacementManager().getPlacement(blockLoc) != null) return;

    Player player = null;
    try {
      player = (Player) invokeNoArg(event, "getPlayer");
    } catch (NoSuchMethodException ignored) {
    }

    MachinePlacement newPlacement =
        plugin
            .getPlacementManager()
            .place(
                templateName, blockLoc, nexoItemId, player != null ? player.getUniqueId() : null);

    if (newPlacement != null) {
      plugin
          .getPlacementManager()
          .registerEntityUUID(newPlacement, baseEntity.getUniqueId().toString());
      plugin.getDataManager().savePlacements();

      plugin.getLogger().info("Auto-registered automat '" + templateName + "' at " + blockLoc);
      if (player != null) {
        player.sendMessage(
            plugin.getPrefix()
                + ColorUtil.miniToLegacy(
                    plugin
                        .getConfig()
                        .getString(
                            "messages.placed",
                            "<reset><white>Postawiono automat"
                                + " (<reset><yellow>{template}<reset><white>).")
                        .replace("{template}", templateName)));
      }
    }
  }

  private void handleInteractReflection(Event event) throws Exception {
    Player player = (Player) invokeNoArg(event, "getPlayer");
    if (player == null) return;

    try {
      Object hand = invokeNoArg(event, "getHand");
      if (hand != null && !hand.toString().equals("HAND")) return;
    } catch (NoSuchMethodException ignored) {
    }

    Entity baseEntity = (Entity) invokeNoArg(event, "getBaseEntity");
    if (baseEntity == null) return;

    String entityUUID = baseEntity.getUniqueId().toString();
    Location blockLoc = baseEntity.getLocation().getBlock().getLocation();

    MachinePlacement placement = plugin.getPlacementManager().getPlacementByEntityUUID(entityUUID);

    if (placement == null) {
      placement = plugin.getPlacementManager().getPlacement(blockLoc);
      if (placement != null && placement.getNexoEntityUUID() == null) {
        plugin.getPlacementManager().registerEntityUUID(placement, entityUUID);
        plugin.getDataManager().savePlacements();
      }
    }

    if (placement == null) {
      String nexoItemId = getMechanicItemID(event);
      if (nexoItemId != null && plugin.getNexoManager().isAutoRegister()) {
        String templateName = plugin.getNexoManager().getTemplateName(nexoItemId);
        if (templateName != null && plugin.getMachineManager().getTemplate(templateName) != null) {
          placement =
              plugin
                  .getPlacementManager()
                  .place(templateName, blockLoc, nexoItemId, player.getUniqueId());
          if (placement != null) {
            plugin.getPlacementManager().registerEntityUUID(placement, entityUUID);
            plugin.getDataManager().savePlacements();
            plugin
                .getLogger()
                .info(
                    "Late auto-registered Nexo '"
                        + nexoItemId
                        + "' as template '"
                        + templateName
                        + "'.");
          }
        }
      }
    }

    if (placement == null) return;

    long cooldownMs = plugin.getConfig().getLong("interact-cooldown-ms", 500);
    long now = System.currentTimeMillis();
    Long last = interactCooldowns.get(player.getUniqueId());
    if (last != null && (now - last) < cooldownMs) {
      cancelEvent(event);
      return;
    }
    interactCooldowns.put(player.getUniqueId(), now);

    openMachine(player, placement);
    cancelEvent(event);
  }

  private void handleBreakReflection(Event event) throws Exception {
    Entity baseEntity = (Entity) invokeNoArg(event, "getBaseEntity");
    if (baseEntity == null) return;

    String entityUUID = baseEntity.getUniqueId().toString();
    MachinePlacement placement = plugin.getPlacementManager().getPlacementByEntityUUID(entityUUID);

    if (placement == null) {
      Location blockLoc = baseEntity.getLocation().getBlock().getLocation();
      placement = plugin.getPlacementManager().getPlacement(blockLoc);
    }
    if (placement == null) return;

    Player player = null;
    try {
      player = (Player) invokeNoArg(event, "getPlayer");
    } catch (NoSuchMethodException ignored) {
    }

    if (!plugin.getConfig().getBoolean("protection.allow-player-break", false)
        && (player == null || !player.hasPermission("vendingmachine.delete"))) {
      cancelEvent(event);
      if (player != null) {
        player.sendMessage(
            plugin.getPrefix()
                + ColorUtil.miniToLegacy(
                    plugin
                        .getConfig()
                        .getString("messages.no-permission", "<reset><red>Brak uprawnień!")));
      }
      return;
    }

    plugin.getPlacementManager().removeByPlacement(placement);
    plugin.getLogger().info("Removed placement at " + placement.getLocation());

    if (player != null) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  plugin
                      .getConfig()
                      .getString("messages.removed", "<reset><red>Usunięto automat.")));
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    interactCooldowns.remove(event.getPlayer().getUniqueId());
  }

  private String getMechanicItemID(Event event) {
    try {
      Object mechanic = invokeNoArg(event, "getMechanic");
      if (mechanic == null) return null;

      String[] possibleMethods = {"getItemID", "itemID", "getItemId", "itemId"};
      for (String methodName : possibleMethods) {
        try {
          Object result = invokeNoArg(mechanic, methodName);
          if (result != null) return result.toString();
        } catch (NoSuchMethodException ignored) {
        }
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  private Object invokeNoArg(Object target, String methodName) throws Exception {
    return method(target.getClass(), methodName).invoke(target);
  }

  private Method method(Class<?> clazz, String name) throws NoSuchMethodException {
    String key = clazz.getName() + '#' + name;
    Method cached = methodCache.get(key);
    if (cached != null) return cached;

    Method method = clazz.getMethod(name);
    method.setAccessible(true);
    methodCache.put(key, method);
    return method;
  }

  private void cancelEvent(Event event) {
    if (event instanceof Cancellable cancellable) {
      cancellable.setCancelled(true);
    }
  }

  private Class<?> tryLoadClass(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private void openMachine(Player player, MachinePlacement placement) {
    MachineTemplate template = plugin.getMachineManager().getTemplate(placement.getTemplateName());
    if (template == null) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>Szablon automatu nie istnieje."));
      return;
    }

    if (!template.isEnabled()) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  plugin
                      .getConfig()
                      .getString("messages.disabled", "<reset><red>Ten automat jest wyłączony.")));
      return;
    }

    if (!player.hasPermission("vendingmachine.use")) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  plugin
                      .getConfig()
                      .getString("messages.no-permission", "<reset><red>Brak uprawnień!")));
      return;
    }

    if (!template.getPermission().isEmpty() && !player.hasPermission(template.getPermission())) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  plugin
                      .getConfig()
                      .getString("messages.no-permission", "<reset><red>Brak uprawnień!")));
      return;
    }

    try {
      String soundName = plugin.getConfig().getString("sounds.open", "UI_BUTTON_CLICK");
      player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.5f, 1.0f);
    } catch (Exception ignored) {
    }

    new MachineGUI(plugin, template, player, placement).open();
  }

  public boolean isNexoAvailable() {
    return nexoAvailable;
  }

  @FunctionalInterface
  private interface EventHandlerCallback {
    void handle(Event event) throws Exception;
  }
}
