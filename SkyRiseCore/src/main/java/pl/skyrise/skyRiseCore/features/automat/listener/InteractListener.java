package pl.skyrise.skyRiseCore.features.automat.listener;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.gui.MachineGUI;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class InteractListener implements Listener {

  private final AutomatModule plugin;
  private final Map<UUID, Long> interactCooldowns = new ConcurrentHashMap<>();

  public InteractListener(AutomatModule plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (event.getHand() != EquipmentSlot.HAND) return;

    Block block = event.getClickedBlock();
    if (block == null) return;

    Player player = event.getPlayer();
    MachinePlacement placement = plugin.getPlacementManager().getPlacement(block.getLocation());
    if (placement == null) return;

    event.setCancelled(true);

    long cooldownMs = plugin.getConfig().getLong("interact-cooldown-ms", 500);
    long now = System.currentTimeMillis();
    Long last = interactCooldowns.get(player.getUniqueId());
    if (last != null && (now - last) < cooldownMs) return;
    interactCooldowns.put(player.getUniqueId(), now);

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
                      .getString("messages.disabled", "<reset><red>Automat wyłączony!")));
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

    if (!player.hasPermission("vendingmachine.use")) {
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

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    MachinePlacement placement =
        plugin.getPlacementManager().getPlacement(event.getBlock().getLocation());
    if (placement == null) return;

    if (!plugin.getConfig().getBoolean("protection.allow-player-break", false)
        && !event.getPlayer().hasPermission("vendingmachine.delete")) {
      event.setCancelled(true);
      event
          .getPlayer()
          .sendMessage(
              plugin.getPrefix()
                  + ColorUtil.miniToLegacy(
                      plugin
                          .getConfig()
                          .getString("messages.no-permission", "<reset><red>Brak uprawnień!")));
      return;
    }

    plugin.getPlacementManager().remove(event.getBlock().getLocation());
    event
        .getPlayer()
        .sendMessage(
            plugin.getPrefix()
                + ColorUtil.miniToLegacy(
                    plugin
                        .getConfig()
                        .getString("messages.removed", "<reset><red>Usunięto automat.")));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    handleExplosionBlocks(event.blockList());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockExplode(BlockExplodeEvent event) {
    handleExplosionBlocks(event.blockList());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    interactCooldowns.remove(event.getPlayer().getUniqueId());
  }

  private void handleExplosionBlocks(List<Block> blocks) {
    if (blocks.isEmpty()) return;

    boolean protect = plugin.getConfig().getBoolean("protection.explosion-protection", true);
    boolean cleanup = plugin.getConfig().getBoolean("protection.auto-cleanup", true);

    if (protect) {
      blocks.removeIf(
          block -> plugin.getPlacementManager().getPlacement(block.getLocation()) != null);
      return;
    }

    if (cleanup) {
      for (Block block : blocks) {
        if (plugin.getPlacementManager().getPlacement(block.getLocation()) != null) {
          plugin.getPlacementManager().remove(block.getLocation());
        }
      }
    }
  }
}
