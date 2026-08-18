package pl.skyrise.skyRiseCore.core.npc;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class NpcRegistry implements Listener {

  private final JavaPlugin plugin;
  private final NamespacedKey moduleKey;
  private final NamespacedKey idKey;
  private final Map<String, NpcClickHandler> handlers = new ConcurrentHashMap<>();
  private final Map<Integer, CitizensNpcLink> citizensLinks = new ConcurrentHashMap<>();

  public NpcRegistry(JavaPlugin plugin) {
    this.plugin = plugin;
    this.moduleKey = new NamespacedKey(plugin, "npc_module");
    this.idKey = new NamespacedKey(plugin, "npc_id");
    registerCitizensRightClickEvent();
  }

  public void registerHandler(String moduleId, NpcClickHandler handler) {
    if (moduleId == null || handler == null) return;
    handlers.put(normalize(moduleId), handler);
  }

  public void unregisterHandler(String moduleId) {
    if (moduleId == null) return;
    handlers.remove(normalize(moduleId));
  }

  public Entity spawnNpc(
      String moduleId,
      String npcId,
      Location location,
      EntityType entityType,
      Component displayName,
      Consumer<Entity> customizer) {
    Objects.requireNonNull(moduleId, "moduleId");
    Objects.requireNonNull(npcId, "npcId");
    Objects.requireNonNull(location, "location");
    Objects.requireNonNull(entityType, "entityType");

    if (location.getWorld() == null) {
      throw new IllegalArgumentException("NPC location has no world");
    }

    removeNpc(moduleId, npcId);

    location.getChunk().load();
    Entity entity = location.getWorld().spawnEntity(location, entityType);
    mark(entity, moduleId, npcId);

    entity.setPersistent(true);
    entity.setInvulnerable(true);
    entity.setSilent(true);
    entity.setCustomNameVisible(true);
    if (displayName != null) {
      entity.customName(displayName);
    }

    if (entity instanceof LivingEntity living) {
      living.setRemoveWhenFarAway(false);
      living.setCanPickupItems(false);
      living.setCollidable(false);
    }
    if (entity instanceof Mob mob) {
      mob.setAI(false);
      mob.setAware(false);
    }
    if (entity instanceof Villager villager) {
      villager.setProfession(Villager.Profession.CLERIC);
      villager.setVillagerLevel(5);
      villager.setVillagerExperience(0);
    }

    if (customizer != null) {
      customizer.accept(entity);
    }

    return entity;
  }

  public int removeNpc(String moduleId, String npcId) {
    int removed = 0;
    String module = normalize(moduleId);
    String id = normalize(npcId);
    citizensLinks
        .entrySet()
        .removeIf(
            entry ->
                entry.getValue().moduleId().equals(module) && entry.getValue().npcId().equals(id));
    for (World world : Bukkit.getWorlds()) {
      for (Entity entity : world.getEntities()) {
        if (isNpc(entity, module, id)) {
          entity.remove();
          removed++;
        }
      }
    }
    return removed;
  }

  public int removeAll(String moduleId) {
    int removed = 0;
    String module = normalize(moduleId);
    citizensLinks.entrySet().removeIf(entry -> entry.getValue().moduleId().equals(module));
    for (World world : Bukkit.getWorlds()) {
      for (Entity entity : world.getEntities()) {
        if (isNpc(entity, module, null)) {
          entity.remove();
          removed++;
        }
      }
    }
    return removed;
  }

  public boolean isManagedNpc(Entity entity) {
    if (entity == null) return false;
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    return pdc.has(moduleKey, PersistentDataType.STRING)
        && pdc.has(idKey, PersistentDataType.STRING);
  }

  public boolean isNpc(Entity entity, String moduleId, String npcId) {
    if (entity == null) return false;
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    String module = pdc.get(moduleKey, PersistentDataType.STRING);
    String id = pdc.get(idKey, PersistentDataType.STRING);
    if (module == null || id == null) return false;
    if (!module.equals(normalize(moduleId))) return false;
    return npcId == null || id.equals(normalize(npcId));
  }

  public String getNpcModule(Entity entity) {
    if (entity == null) return null;
    return entity.getPersistentDataContainer().get(moduleKey, PersistentDataType.STRING);
  }

  public String getNpcId(Entity entity) {
    if (entity == null) return null;
    return entity.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
  }

  public void markNpc(Entity entity, String moduleId, String npcId) {
    mark(entity, moduleId, npcId);
  }

  public void markCitizensNpc(int citizensId, String moduleId, String npcId) {
    citizensLinks.put(citizensId, new CitizensNpcLink(normalize(moduleId), normalize(npcId)));
  }

  public void unmarkCitizensNpc(int citizensId) {
    citizensLinks.remove(citizensId);
  }

  private void mark(Entity entity, String moduleId, String npcId) {
    if (entity == null) return;
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    pdc.set(moduleKey, PersistentDataType.STRING, normalize(moduleId));
    pdc.set(idKey, PersistentDataType.STRING, normalize(npcId));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (isManagedNpc(event.getEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDamageByEntity(EntityDamageByEntityEvent event) {
    if (isManagedNpc(event.getEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInteract(PlayerInteractEntityEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) return;

    Entity entity = event.getRightClicked();
    if (!isManagedNpc(entity)) return;

    String module = getNpcModule(entity);
    String id = getNpcId(entity);
    NpcClickHandler handler = handlers.get(module);
    if (handler == null) return;

    event.setCancelled(true);
    handler.onClick(event.getPlayer(), entity, id);
  }

  @SuppressWarnings("unchecked")
  private void registerCitizensRightClickEvent() {
    try {
      Class<?> eventClass = Class.forName("net.citizensnpcs.api.event.NPCRightClickEvent");
      Bukkit.getPluginManager()
          .registerEvent(
              (Class<? extends Event>) eventClass,
              this,
              EventPriority.HIGHEST,
              (EventExecutor) (listener, event) -> handleCitizensRightClick(event),
              plugin,
              false);
    } catch (ClassNotFoundException ignored) {

    } catch (Throwable throwable) {
      plugin
          .getLogger()
          .warning(
              "Nie udało się zarejestrować Citizens NPC click hook: " + throwable.getMessage());
    }
  }

  private void handleCitizensRightClick(Event event) {
    try {
      Object npc = invoke(event, "getNPC");
      Object clicker = invokeAny(event, "getClicker", "getPlayer");
      if (!(clicker instanceof org.bukkit.entity.Player player)) return;
      if (npc == null) return;

      Object npcIdObject = invoke(npc, "getId");
      CitizensNpcLink link = null;
      if (npcIdObject instanceof Number number) {
        link = citizensLinks.get(number.intValue());
      }

      Entity entity = null;
      try {
        Object entityObject = invoke(npc, "getEntity");
        if (entityObject instanceof Entity bukkitEntity) entity = bukkitEntity;
      } catch (ReflectiveOperationException ignored) {
      }

      String module =
          link != null ? link.moduleId() : (entity != null ? getNpcModule(entity) : null);
      String id = link != null ? link.npcId() : (entity != null ? getNpcId(entity) : null);
      if (module == null || id == null) return;

      NpcClickHandler handler = handlers.get(module);
      if (handler == null) return;

      if (event instanceof Cancellable cancellable) {
        cancellable.setCancelled(true);
      }
      handler.onClick(player, entity, id);
    } catch (Throwable ignored) {
    }
  }

  private Object invokeAny(Object target, String... methods) throws ReflectiveOperationException {
    ReflectiveOperationException last = null;
    for (String method : methods) {
      try {
        return invoke(target, method);
      } catch (ReflectiveOperationException ex) {
        last = ex;
      }
    }
    throw last != null ? last : new NoSuchMethodException();
  }

  private Object invoke(Object target, String method) throws ReflectiveOperationException {
    Method m = target.getClass().getMethod(method);
    m.setAccessible(true);
    return m.invoke(target);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private record CitizensNpcLink(String moduleId, String npcId) {}

  @FunctionalInterface
  public interface NpcClickHandler {
    void onClick(org.bukkit.entity.Player player, Entity entity, String npcId);
  }
}
