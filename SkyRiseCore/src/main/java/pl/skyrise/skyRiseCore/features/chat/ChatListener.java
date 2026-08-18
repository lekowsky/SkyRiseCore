package pl.skyrise.skyRiseCore.features.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

  private final ChatModule module;

  public ChatListener(ChatModule module) {
    this.module = module;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onChat(AsyncChatEvent event) {
    Player sender = event.getPlayer();
    Location senderLocation = sender.getLocation();
    World senderWorld = senderLocation.getWorld();
    if (senderWorld == null) return;

    double senderX = senderLocation.getX();
    double senderY = senderLocation.getY();
    double senderZ = senderLocation.getZ();
    int maxDistSq = module.getRadiusSquared();
    Location targetLocation = new Location(senderWorld, 0.0, 0.0, 0.0);

    event
        .viewers()
        .removeIf(
            audience -> {
              if (!(audience instanceof Player target)) return false;
              if (target == sender) return false;
              if (target.getWorld() != senderWorld) return true;

              target.getLocation(targetLocation);
              double dx = targetLocation.getX() - senderX;
              double dy = targetLocation.getY() - senderY;
              double dz = targetLocation.getZ() - senderZ;
              return (dx * dx + dy * dy + dz * dz) > maxDistSq;
            });
  }
}
