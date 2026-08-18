package pl.skyrise.skyRiseCore.features.adminchat;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class AdminChatListener implements Listener {

  private final AdminChatModule module;

  public AdminChatListener(AdminChatModule module) {
    this.module = module;
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onChat(AsyncChatEvent event) {
    Player player = event.getPlayer();

    if (!module.isToggled(player.getUniqueId())) return;

    if (!player.hasPermission(module.getPermission())) {
      module.toggleOff(player.getUniqueId());
      return;
    }

    event.setCancelled(true);

    String message = ColorUtil.plain(event.message());
    AdminChatCommand.broadcast(module, player.getName(), message);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    module.toggleOff(event.getPlayer().getUniqueId());
  }
}
