package pl.skyrise.skyRiseCore.features.batspawn;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class BatSpawnListener implements Listener {

  private final BatSpawnModule module;
  private final MessageCache messageCache;

  public BatSpawnListener(BatSpawnModule module, MessageCache messageCache) {
    this.module = module;
    this.messageCache = messageCache;
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (event.getEntityType() != org.bukkit.entity.EntityType.BAT) return;

    if (event.getLocation().getY() >= module.getMaxY()) {
      event.setCancelled(true);
      return;
    }

    Block below = event.getLocation().getBlock().getRelative(0, -1, 0);
    if (!module.isAllowed(below.getType())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    String message = event.getMessage().toLowerCase();
    if (!message.startsWith("/summon") && !message.startsWith("/minecraft:summon")) return;
    if (!message.contains("bat")) return;

    event.setCancelled(true);

    if (!messageCache.canSend(event.getPlayer().getUniqueId(), "batspawn:deny")) return;
    event.getPlayer().sendMessage(ColorUtil.mini(module.getDenyMessage()));
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Player player = event.getPlayer();
    Material mainHand = player.getInventory().getItemInMainHand().getType();
    Material offHand = player.getInventory().getItemInOffHand().getType();
    if (mainHand != Material.BAT_SPAWN_EGG && offHand != Material.BAT_SPAWN_EGG) return;

    Block target = event.getClickedBlock();
    if (target == null) return;

    if (target.getY() >= module.getMaxY()
        || !module.isAllowed(target.getRelative(0, -1, 0).getType())) {
      event.setCancelled(true);
      if (messageCache.canSend(player.getUniqueId(), "batspawn:deny")) {
        player.sendMessage(ColorUtil.mini(module.getDenyMessage()));
      }
    }
  }
}
