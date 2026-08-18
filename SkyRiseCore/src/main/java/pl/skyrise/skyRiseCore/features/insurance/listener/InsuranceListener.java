package pl.skyrise.skyRiseCore.features.insurance.listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.skyrise.skyRiseCore.features.insurance.InsuranceModule;
import pl.skyrise.skyRiseCore.features.insurance.gui.InsuranceMenu;

public class InsuranceListener implements Listener {

  private final InsuranceModule module;
  private final Map<UUID, Long> totemMessageCooldown = new ConcurrentHashMap<>();

  public InsuranceListener(InsuranceModule module) {
    this.module = module;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onFatalDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (module.shouldIgnoreCause(event.getCause())) return;
    if (!module.hasInsurance(player.getUniqueId())) return;
    if (player.getHealth() - event.getFinalDamage() > 0.0) return;

    String effectId = module.getSelectedEffectId(player.getUniqueId());
    event.setCancelled(true);
    if (module.consumeInsurance(player.getUniqueId())) {
      module.applyProtection(player, event.getCause(), effectId);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onVanillaTotemUse(EntityResurrectEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    event.setCancelled(true);
    sendTotemBlocked(player);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onTotemPickup(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (event.getItem().getItemStack().getType() != Material.TOTEM_OF_UNDYING) return;

    module.send(player, "totem-blocked");
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    if (!(event.getInventory().getHolder() instanceof InsuranceMenu menu)) return;

    event.setCancelled(true);
    if (event.getClickedInventory() != event.getInventory()) return;

    int slot = event.getRawSlot();

    if (menu.getPage() == InsuranceMenu.Page.EFFECTS) {
      if (slot == menu.getModule().getGuiCloseSlot()) {
        new InsuranceMenu(module, player, InsuranceMenu.Page.MAIN).open();
        return;
      }

      if (!player.hasPermission(module.getPremiumPermission())) {
        module.playPurchaseSound(player, InsuranceModule.PurchaseResult.NO_PERMISSION);
        module.send(player, "premium-required");
        return;
      }

      for (var effect : module.getRescueEffects().values()) {
        if (slot == effect.slot()) {
          if (module.setSelectedEffect(player.getUniqueId(), effect.id())) {
            module.send(player, "premium-effect-selected");
          } else {
            module.send(player, "error");
          }
          new InsuranceMenu(module, player, InsuranceMenu.Page.EFFECTS).open();
          return;
        }
      }
      return;
    }

    if (slot == menu.getModule().getGuiCloseSlot()) {
      player.closeInventory();
      return;
    }

    if (slot == menu.getModule().getGuiRenewSlot() && module.hasInsurance(player.getUniqueId())) {
      InsuranceModule.PurchaseResult result = module.renewPolicy(player);
      module.playPurchaseSound(player, result);
      switch (result) {
        case RENEWED -> module.send(player, "renewed");
        case NO_MONEY -> module.send(player, "no-money");
        case RENEW_NOT_AVAILABLE, ALREADY_ACTIVE -> module.send(player, "renew-not-available");
        case NO_ACTIVE_POLICY -> module.send(player, "no-active-policy");
        case NO_PERMISSION -> module.send(player, "no-permission");
        default -> module.send(player, "error");
      }
      new InsuranceMenu(module, player).open();
      return;
    }

    if (slot == menu.getModule().getGuiEffectButtonSlot()) {
      if (!player.hasPermission(module.getPremiumPermission())) {
        module.playPurchaseSound(player, InsuranceModule.PurchaseResult.NO_PERMISSION);
        module.send(player, "premium-required");
        return;
      }
      new InsuranceMenu(module, player, InsuranceMenu.Page.EFFECTS).open();
      return;
    }

    int insuranceSlot =
        module.hasInsurance(player.getUniqueId())
            ? menu.getModule().getGuiInsuranceActiveSlot()
            : menu.getModule().getGuiInsuranceSlot();
    if (slot != insuranceSlot) return;

    InsuranceModule.PurchaseResult result = module.purchase(player);
    module.playPurchaseSound(player, result);
    switch (result) {
      case SUCCESS -> {
        module.send(player, "purchased");
        player.closeInventory();
      }
      case RENEWED -> {
        module.send(player, "renewed");
        player.closeInventory();
      }
      case ALREADY_ACTIVE -> module.send(player, "already-active");
      case NO_MONEY -> module.send(player, "no-money");
      case NO_PERMISSION -> module.send(player, "no-permission");
      case ERROR -> module.send(player, "error");
      default -> module.send(player, "error");
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInventoryDrag(InventoryDragEvent event) {
    if (event.getInventory().getHolder() instanceof InsuranceMenu) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    module.handleJoin(event.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    totemMessageCooldown.remove(event.getPlayer().getUniqueId());
    module.clearGuiCooldown(event.getPlayer().getUniqueId());
  }

  private void sendTotemBlocked(Player player) {
    long now = System.currentTimeMillis();
    Long last = totemMessageCooldown.get(player.getUniqueId());
    if (last != null && now - last < 2000L) return;
    totemMessageCooldown.put(player.getUniqueId(), now);
    module.send(player, "totem-blocked");
    module.playTotemBlockedSound(player);
  }
}
