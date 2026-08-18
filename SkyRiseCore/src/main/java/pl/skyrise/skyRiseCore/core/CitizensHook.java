package pl.skyrise.skyRiseCore.core;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.core.npc.NpcRegistry;

public final class CitizensHook {

  private static JavaPlugin plugin;
  private static NpcRegistry npcRegistry;
  private static boolean enabled;

  private CitizensHook() {}

  public static void setup(JavaPlugin javaPlugin, NpcRegistry registry) {
    plugin = javaPlugin;
    npcRegistry = registry;
    enabled =
        javaPlugin != null
            && javaPlugin.getServer().getPluginManager().getPlugin("Citizens") != null;
  }

  public static boolean isEnabled() {
    return enabled;
  }

  public static CreatedNpc createPlayerNpc(
      String moduleId,
      String npcId,
      Location location,
      String citizensRawName,
      String skinName,
      boolean glowing) {
    if (!enabled || location == null || location.getWorld() == null) return null;

    try {
      Object registry = citizensRegistry();
      String rawName =
          citizensRawName == null || citizensRawName.isBlank() ? npcId : citizensRawName;

      Object npc =
          registry
              .getClass()
              .getMethod("createNPC", EntityType.class, String.class)
              .invoke(registry, EntityType.PLAYER, rawName);

      setNpcName(npc, rawName);
      setProtected(npc, true);
      setNameplateVisible(npc, true);
      applySkin(npc, skinName);

      boolean spawned =
          Boolean.TRUE.equals(
              npc.getClass().getMethod("spawn", Location.class).invoke(npc, location));
      if (!spawned) {
        destroyNpcObject(npc);
        return null;
      }

      int citizensId = ((Number) npc.getClass().getMethod("getId").invoke(npc)).intValue();
      Entity entity = null;
      Object entityObject = npc.getClass().getMethod("getEntity").invoke(npc);
      if (entityObject instanceof Entity bukkitEntity) {
        entity = bukkitEntity;
        entity.setInvulnerable(true);
        entity.setGlowing(glowing);
        if (npcRegistry != null) {
          npcRegistry.markNpc(entity, moduleId, npcId);
        }
      }

      if (npcRegistry != null) {
        npcRegistry.markCitizensNpc(citizensId, moduleId, npcId);
      }

      UUID entityUuid = entity != null ? entity.getUniqueId() : null;
      return new CreatedNpc(citizensId, entityUuid, entity);
    } catch (Throwable throwable) {
      if (plugin != null) {
        plugin
            .getLogger()
            .log(
                Level.WARNING,
                "Nie udało się utworzyć NPC Citizens: " + throwable.getMessage(),
                throwable);
      }
      return null;
    }
  }

  public static void destroyNpc(int citizensId) {
    if (!enabled) return;
    if (npcRegistry != null) npcRegistry.unmarkCitizensNpc(citizensId);

    try {
      Object registry = citizensRegistry();
      Object npc = registry.getClass().getMethod("getById", int.class).invoke(registry, citizensId);
      if (npc != null) destroyNpcObject(npc);
    } catch (Throwable ignored) {
    }
  }

  private static Object citizensRegistry() throws ReflectiveOperationException {
    Class<?> citizensApi = Class.forName("net.citizensnpcs.api.CitizensAPI");
    return citizensApi.getMethod("getNPCRegistry").invoke(null);
  }

  private static void setNpcName(Object npc, String rawName) {
    try {
      npc.getClass().getMethod("setName", String.class).invoke(npc, rawName);
    } catch (Throwable ignored) {
    }
  }

  private static void setProtected(Object npc, boolean value) {
    try {
      npc.getClass().getMethod("setProtected", boolean.class).invoke(npc, value);
    } catch (Throwable ignored) {
    }
  }

  private static void setNameplateVisible(Object npc, boolean visible) {
    try {
      Class<?> metadataClass = Class.forName("net.citizensnpcs.api.npc.NPC$Metadata");
      Object key = metadataClass.getField("NAMEPLATE_VISIBLE").get(null);
      Object data = npc.getClass().getMethod("data").invoke(npc);
      for (Method method : data.getClass().getMethods()) {
        if (!method.getName().equals("setPersistent") && !method.getName().equals("set")) continue;
        if (method.getParameterCount() != 2) continue;
        method.invoke(data, key, visible);
        return;
      }
    } catch (Throwable ignored) {
    }
  }

  private static void applySkin(Object npc, String skinName) {
    if (skinName == null || skinName.isBlank()) return;
    try {
      Class<?> skinTrait = Class.forName("net.citizensnpcs.trait.SkinTrait");
      Object trait = npc.getClass().getMethod("getOrAddTrait", Class.class).invoke(npc, skinTrait);
      try {
        trait
            .getClass()
            .getMethod("setSkinName", String.class, boolean.class)
            .invoke(trait, skinName, true);
      } catch (NoSuchMethodException ignored) {
        trait.getClass().getMethod("setSkinName", String.class).invoke(trait, skinName);
      }
    } catch (Throwable throwable) {
      if (plugin != null) {
        plugin
            .getLogger()
            .warning(
                "Nie udało się ustawić skina Citizens '"
                    + skinName
                    + "': "
                    + throwable.getMessage());
      }
    }
  }

  private static void destroyNpcObject(Object npc) {
    try {
      npc.getClass().getMethod("destroy").invoke(npc);
    } catch (Throwable ignored) {
    }
  }

  public record CreatedNpc(int citizensId, UUID entityUuid, Entity entity) {}
}
