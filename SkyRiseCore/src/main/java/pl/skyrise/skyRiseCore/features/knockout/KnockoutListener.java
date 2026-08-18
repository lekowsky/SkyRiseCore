package pl.skyrise.skyRiseCore.features.knockout;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import pl.skyrise.skyRiseCore.SkyRiseCore;
import pl.skyrise.skyRiseCore.features.insurance.InsuranceModule;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class KnockoutListener implements Listener {

  private final KnockoutModule module;

  public KnockoutListener(KnockoutModule module) {
    this.module = module;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    UUID uuid = player.getUniqueId();

    if (module.isKnocked(uuid)) {
      event.setCancelled(true);
      return;
    }

    if (player.getHealth() - event.getFinalDamage() <= 0) {
      InsuranceModule insurance =
          SkyRiseCore.getInstance().getModuleManager().getModule("Ubezpieczenie");
      if (insurance != null && !insurance.shouldIgnoreCause(event.getCause())) {
        String effectId = insurance.getSelectedEffectId(uuid);
        if (insurance.consumeInsurance(uuid)) {
          event.setCancelled(true);
          insurance.applyProtection(player, event.getCause(), effectId);
          return;
        }
      }

      if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;

      event.setCancelled(true);
      module.knockPlayer(player);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onDamageByKnocked(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player damager) {
      if (module.isKnocked(damager.getUniqueId())) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (module.isKnocked(player.getUniqueId())) {
      Location from = event.getFrom();
      Location to = event.getTo();

      player.setSwimming(true);
      player.setPose(org.bukkit.entity.Pose.SWIMMING);

      if (to != null
          && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {

        Location dest = from.clone();
        dest.setYaw(to.getYaw());
        dest.setPitch(to.getPitch());
        event.setTo(dest);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onToggleSwim(org.bukkit.event.entity.EntityToggleSwimEvent event) {
    if (event.getEntity() instanceof Player player) {
      if (module.isKnocked(player.getUniqueId())) {

        if (!event.isSwimming()) {
          event.setCancelled(true);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    if (module.isKnocked(player.getUniqueId())) {

      if (event.isSneaking()) {
        module.handleKnockedSneak(player);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    if (module.isKnocked(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
      event
          .getPlayer()
          .sendMessage(ColorUtil.mini("<red>» Nie możesz używać komend podczas powalenia!"));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (module.isKnocked(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    if (module.isKnocked(event.getWhoClicked().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    if (module.isKnocked(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player) {
      if (module.isKnocked(player.getUniqueId())) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSwapItems(PlayerSwapHandItemsEvent event) {
    if (module.isKnocked(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInteract(PlayerInteractEvent event) {
    if (module.isKnocked(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInteractEntity(PlayerInteractEntityEvent event) {
    if (module.isKnocked(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }
}
