package pl.skyrise.skyRiseCore.features.freeze;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class FreezeListener implements Listener {

  private final FreezeModule module;

  public FreezeListener(FreezeModule module) {
    this.module = module;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    if (module.isFrozen(uuid)) {
      if (player.hasPermission(module.getPermission()) || player.isOp()) {
        module.unfreezePlayer(uuid);
        player.sendMessage(
            ColorUtil.mini(
                "<green>» Wykryto uprawnienia administracyjne — zostałeś automatycznie"
                    + " odmrożony."));
      } else {
        module.applyFreezeEffects(player);
      }
    }

    for (UUID frozenUuid : module.getFrozenPlayers()) {
      Player frozen = Bukkit.getPlayer(frozenUuid);
      if (frozen != null && frozen.isOnline() && frozen != player) {
        player.hidePlayer(module.getPlugin(), frozen);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    if (module.isFrozen(player.getUniqueId())) {
      module.revealPlayer(player);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (module.isFrozen(player.getUniqueId())) {
      Location freezeLoc = module.getFreezeLocation();
      if (freezeLoc == null) {
        freezeLoc = new Location(player.getWorld(), 0.5, 64, 0.5);
      }

      Location dest = freezeLoc.clone();
      dest.setYaw(module.getLockedYaw(player.getUniqueId()));
      dest.setPitch(module.getLockedPitch(player.getUniqueId()));

      event.setTo(dest);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    if (module.isFrozen(player.getUniqueId())) {
      event.setCancelled(true);
      player.sendMessage(module.getFreezeChatComponent());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    Player player = event.getPlayer();
    if (module.isFrozen(player.getUniqueId())) {
      event.setCancelled(true);
      player.sendMessage(module.getFreezeChatComponent());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (module.isFrozen(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    if (module.isFrozen(event.getWhoClicked().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    if (module.isFrozen(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player) {
      if (module.isFrozen(player.getUniqueId())) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSwapItems(PlayerSwapHandItemsEvent event) {
    if (module.isFrozen(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInteract(PlayerInteractEvent event) {
    if (module.isFrozen(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInteractEntity(PlayerInteractEntityEvent event) {
    if (module.isFrozen(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }
}
