package pl.skyrise.skyRiseCore.features.roleplay;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class RolePlayListener implements Listener {

  private final RolePlayModule module;

  public RolePlayListener(RolePlayModule module) {
    this.module = module;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {

    module.cleanPlayer(event.getPlayer().getUniqueId());
  }
}
