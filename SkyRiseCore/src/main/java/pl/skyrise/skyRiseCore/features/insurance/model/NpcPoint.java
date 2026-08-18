package pl.skyrise.skyRiseCore.features.insurance.model;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class NpcPoint {

  private final String id;
  private Location location;
  private UUID entityUuid;
  private Integer citizensId;

  public NpcPoint(String id, Location location, UUID entityUuid) {
    this(id, location, entityUuid, null);
  }

  public NpcPoint(String id, Location location, UUID entityUuid, Integer citizensId) {
    this.id = id;
    this.location = location;
    this.entityUuid = entityUuid;
    this.citizensId = citizensId;
  }

  public String getId() {
    return id;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public UUID getEntityUuid() {
    return entityUuid;
  }

  public void setEntityUuid(UUID entityUuid) {
    this.entityUuid = entityUuid;
  }

  public Integer getCitizensId() {
    return citizensId;
  }

  public void setCitizensId(Integer citizensId) {
    this.citizensId = citizensId;
  }

  public boolean isWorldLoaded() {
    return location != null && location.getWorld() != null;
  }

  public String formatLocation() {
    if (location == null) return "brak";
    World world = location.getWorld();
    String worldName = world != null ? world.getName() : "?";
    return worldName
        + " "
        + location.getBlockX()
        + ", "
        + location.getBlockY()
        + ", "
        + location.getBlockZ();
  }

  public static Location location(
      String worldName, double x, double y, double z, float yaw, float pitch) {
    World world = Bukkit.getWorld(worldName);
    if (world == null) return null;
    return new Location(world, x, y, z, yaw, pitch);
  }
}
