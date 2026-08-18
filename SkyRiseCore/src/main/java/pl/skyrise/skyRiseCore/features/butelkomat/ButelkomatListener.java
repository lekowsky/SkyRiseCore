package pl.skyrise.skyRiseCore.features.butelkomat;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ButelkomatListener implements Listener {

  private final ButelkomatModule module;
  private final Map<UUID, Long> cooldowns = new HashMap<>();

  public ButelkomatListener(ButelkomatModule module) {
    this.module = module;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInteract(NexoFurnitureInteractEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;

    try {
      if (event.getHand() != EquipmentSlot.HAND) return;
    } catch (Throwable ignored) {
    }

    String furnitureId = event.getMechanic().getItemID();

    if (player.isSneaking() && player.hasPermission("butelkomat.admin")) {
      module.sendMsg(player, "admin-click-info", furnitureId);
    }

    if (!module.isButelkomat(furnitureId)) return;

    event.setCancelled(true);

    long now = System.currentTimeMillis();
    Long last = cooldowns.get(player.getUniqueId());
    if (last != null && (now - last) < module.getCooldownMs()) {
      return;
    }
    cooldowns.put(player.getUniqueId(), now);

    if (!player.hasPermission("butelkomat.use")) {
      module.sendMsg(player, "no-permission");
      return;
    }

    ItemStack hand = player.getInventory().getItemInMainHand();
    if (hand == null || hand.getType() == Material.AIR) {
      return;
    }

    module.processReturn(player, hand);
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlace(NexoFurniturePlaceEvent event) {
    String furnitureId = event.getMechanic().getItemID();
    if (module.isButelkomat(furnitureId)) {
      Player player = event.getPlayer();
      if (player != null && player.hasPermission("butelkomat.admin")) {
        module.sendMsg(player, "machine-placed");
      }
    }
  }
}
