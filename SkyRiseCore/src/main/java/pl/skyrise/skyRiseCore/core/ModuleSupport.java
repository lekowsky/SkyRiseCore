package pl.skyrise.skyRiseCore.core;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ModuleSupport {

  private ModuleSupport() {}

  public static void bindExecutor(
      JavaPlugin plugin, CommandExecutor executor, String... commandNames) {
    if (plugin == null || executor == null || commandNames == null) return;
    for (String commandName : commandNames) {
      PluginCommand command = plugin.getCommand(commandName);
      if (command != null) {
        command.setExecutor(executor);
      }
    }
  }

  public static void bindDisabled(JavaPlugin plugin, String moduleName, String... commandNames) {
    DisabledCommandExecutor.bind(plugin, moduleName, commandNames);
  }

  public static <T extends Listener> T registerListener(JavaPlugin plugin, T listener) {
    if (plugin != null && listener != null) {
      plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
    return listener;
  }

  public static void unregisterListener(Listener listener) {
    if (listener != null) {
      HandlerList.unregisterAll(listener);
    }
  }

  public static void unregisterListeners(Iterable<? extends Listener> listeners) {
    if (listeners == null) return;
    for (Listener listener : listeners) {
      unregisterListener(listener);
    }
  }

  public static void unregisterTabs(TabRegistry tabRegistry, String... commands) {
    if (tabRegistry == null || commands == null) return;
    for (String command : commands) {
      if (command != null && !command.isBlank()) {
        tabRegistry.unregister(command);
      }
    }
  }
}
