package pl.skyrise.skyRiseCore.features.chat;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class GlobalChatCommand implements CommandExecutor {

  private final ChatModule module;

  public GlobalChatCommand(ChatModule module) {
    this.module = module;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(ColorUtil.mini("<red>» Tylko gracze mogą używać czatu globalnego."));
      return true;
    }

    if (args.length == 0) {
      player.sendMessage(
          ColorUtil.mini("<#459df5>»</#459df5> <yellow>Użycie: <white>/g <wiadomość>"));
      return true;
    }

    if (!player.hasPermission("skyrise.globalchat.bypass")) {
      int remaining = module.getRemainingCooldown(player.getUniqueId());
      if (remaining > 0) {

        if (pl.skyrise
            .skyRiseCore
            .SkyRiseCore
            .getInstance()
            .getMessageCache()
            .canSend(player.getUniqueId(), "chat:cooldown_warning")) {
          player.sendMessage(
              ColorUtil.mini(
                  module.getCooldownMessage().replace("{time}", String.valueOf(remaining))));
        }
        return true;
      }
      module.setCooldown(player.getUniqueId());
    }

    String safeMessage = ColorUtil.escape(String.join(" ", args));

    String format =
        module
            .getGlobalFormat()
            .replace("{prefix}", "")
            .replace("{group_color}", "")
            .replace("{player}", ColorUtil.escape(player.getName()))
            .replace("{message}", safeMessage);

    Bukkit.getServer().sendMessage(ColorUtil.mini(format));
    return true;
  }
}
