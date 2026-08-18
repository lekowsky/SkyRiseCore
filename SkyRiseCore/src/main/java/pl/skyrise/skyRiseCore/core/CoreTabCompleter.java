package pl.skyrise.skyRiseCore.core;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class CoreTabCompleter implements TabCompleter {

  private final TabRegistry registry;

  public CoreTabCompleter(TabRegistry registry) {
    this.registry = registry;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    return registry.complete(command.getName(), sender, args);
  }
}
