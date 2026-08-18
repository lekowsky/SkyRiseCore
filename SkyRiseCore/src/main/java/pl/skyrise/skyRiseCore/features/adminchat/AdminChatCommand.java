package pl.skyrise.skyRiseCore.features.adminchat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class AdminChatCommand implements CommandExecutor {

  private final AdminChatModule module;

  public AdminChatCommand(AdminChatModule module) {
    this.module = module;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    if (!(sender instanceof Player player)) {
      if (args.length == 0) {
        sender.sendMessage(
            ColorUtil.mini("<#459df5>»</#459df5> <yellow>Użycie: <white>/ac <wiadomość>"));
        return true;
      }
      broadcast(sender.getName(), String.join(" ", args));
      return true;
    }

    if (!player.hasPermission(module.getPermission())) {
      player.sendMessage(ColorUtil.mini("<red>» Nie masz uprawnień do AdminChat!"));
      return true;
    }

    if (args.length > 0) {
      broadcast(player.getName(), String.join(" ", args));
      return true;
    }

    if (module.isToggled(player.getUniqueId())) {
      module.toggleOff(player.getUniqueId());
      player.sendMessage(ColorUtil.mini(module.getToggleOffMsg()));
    } else {
      module.toggleOn(player.getUniqueId());
      player.sendMessage(ColorUtil.mini(module.getToggleOnMsg()));
    }
    return true;
  }

  static void broadcast(AdminChatModule module, String senderName, String message) {
    String safePlayer = ColorUtil.escape(senderName);
    String safeMessage = ColorUtil.escape(message);

    String formatted =
        module.getFormat().replace("{player}", safePlayer).replace("{message}", safeMessage);

    Component component = ColorUtil.mini(formatted);

    for (Player online : Bukkit.getOnlinePlayers()) {
      if (online.hasPermission(module.getPermission())) {
        online.sendMessage(component);
      }
    }
    Bukkit.getConsoleSender().sendMessage(component);
  }

  private void broadcast(String senderName, String message) {
    broadcast(module, senderName, message);
  }
}
