package pl.skyrise.skyRiseCore.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public final class DisabledCommandExecutor implements CommandExecutor {

  private final String moduleName;

  public DisabledCommandExecutor(String moduleName) {
    this.moduleName = moduleName == null || moduleName.isBlank() ? "ten moduł" : moduleName;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    sender.sendMessage(
        ColorUtil.mini(
            "<#459df5><bold>SkyRiseCore</bold> <gray>» <red>Ta komenda jest obecnie wyłączona "
                + "<gray>(moduł: <yellow>"
                + ColorUtil.escape(moduleName)
                + "</yellow>)<red>."));
    return true;
  }

  public static void bind(JavaPlugin plugin, String moduleName, String... commandNames) {
    if (plugin == null || commandNames == null) return;
    DisabledCommandExecutor executor = new DisabledCommandExecutor(moduleName);
    for (String commandName : commandNames) {
      if (commandName == null || commandName.isBlank()) continue;
      PluginCommand command = plugin.getCommand(commandName);
      if (command != null) {
        command.setExecutor(executor);
      }
    }
  }
}
