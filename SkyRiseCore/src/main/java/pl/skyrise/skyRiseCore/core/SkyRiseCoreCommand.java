package pl.skyrise.skyRiseCore.core;

import java.util.List;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.skyrise.skyRiseCore.SkyRiseCore;
import pl.skyrise.skyRiseCore.api.ModuleManager;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class SkyRiseCoreCommand implements CommandExecutor {

  private final SkyRiseCore plugin;
  private final TabRegistry tabRegistry;

  public SkyRiseCoreCommand(SkyRiseCore plugin, TabRegistry tabRegistry) {
    this.plugin = plugin;
    this.tabRegistry = tabRegistry;

    tabRegistry.register(
        "skyrisecore",
        (sender, args) -> {
          if (args.length == 1) {
            return TabRegistry.filter(
                List.of("reload", "enable", "disable", "list", "version"), args[0]);
          }
          if (args.length == 2
              && (args[0].equalsIgnoreCase("reload")
                  || args[0].equalsIgnoreCase("enable")
                  || args[0].equalsIgnoreCase("disable"))) {
            return TabRegistry.filter(plugin.getModuleManager().getModuleNames(), args[1]);
          }
          return List.of();
        });
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      sendInfo(sender);
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "reload" -> handleReload(sender, args);
      case "enable" -> handleEnable(sender, args);
      case "disable" -> handleDisable(sender, args);
      case "list" -> handleList(sender);
      case "version" -> sendInfo(sender);
      default -> sendHelp(sender);
    }
    return true;
  }

  private void handleEnable(CommandSender sender, String[] args) {
    ModuleManager mm = plugin.getModuleManager();

    if (args.length == 1) {
      sender.sendMessage(
          pl.skyrise.skyRiseCore.utils.ColorUtil.mini(
              "<#459df5>»</#459df5> <yellow>Użycie: <white>/src enable <moduł>"));
      return;
    }

    String moduleName = args[1].toLowerCase();
    if (!mm.getModuleNames().contains(moduleName)) {
      sender.sendMessage(
          pl.skyrise.skyRiseCore.utils.ColorUtil.mini(
              "<red>» Moduł <gold>" + moduleName + "</gold> nie istnieje."));
      return;
    }

    if (mm.enable(moduleName)) {
      sender.sendMessage(
          pl.skyrise.skyRiseCore.utils.ColorUtil.mini(
              "<green>» Moduł <gold>" + moduleName + "</gold> włączony. Stan zapisano."));
    } else {
      sender.sendMessage(
          pl.skyrise.skyRiseCore.utils.ColorUtil.mini(
              "<red>» Moduł <gold>" + moduleName + "</gold> jest już włączony lub wystąpił błąd."));
    }
  }

  private void handleDisable(CommandSender sender, String[] args) {
    ModuleManager mm = plugin.getModuleManager();

    if (args.length == 1) {
      sender.sendMessage(
          pl.skyrise.skyRiseCore.utils.ColorUtil.mini(
              "<#459df5>»</#459df5> <yellow>Użycie: <white>/src disable <moduł>"));
      return;
    }

    String moduleName = args[1].toLowerCase();
    if (!mm.getModuleNames().contains(moduleName)) {
      sender.sendMessage(
          pl.skyrise.skyRiseCore.utils.ColorUtil.mini(
              "<red>» Moduł <gold>" + moduleName + "</gold> nie istnieje."));
      return;
    }

    if (mm.disable(moduleName)) {
      sender.sendMessage(
          pl.skyrise.skyRiseCore.utils.ColorUtil.mini(
              "<green>» Moduł <gold>" + moduleName + "</gold> wyłączony. Stan zapisano."));
    } else {
      sender.sendMessage(
          pl.skyrise.skyRiseCore.utils.ColorUtil.mini(
              "<red>» Moduł <gold>"
                  + moduleName
                  + "</gold> jest już wyłączony lub wystąpił błąd."));
    }
  }

  private void handleReload(CommandSender sender, String[] args) {
    ModuleManager mm = plugin.getModuleManager();

    if (args.length == 1) {
      mm.reloadAll();
      sender.sendMessage(
          ColorUtil.mini(
              "<green>» Przeładowano wszystkie moduły <dark_gray>(<white>"
                  + mm.getModuleCount()
                  + "<dark_gray>)</dark_gray>."));
      return;
    }

    String moduleName = args[1].toLowerCase();
    if (mm.reload(moduleName)) {
      sender.sendMessage(
          ColorUtil.mini("<green>» Moduł <gold>" + moduleName + "</gold> przeładowany."));
    } else {
      sender.sendMessage(
          ColorUtil.mini("<red>» Moduł <gold>" + moduleName + "</gold> nie istnieje."));
      sender.sendMessage(
          ColorUtil.mini("<#459df5>» Dostępne: <white>" + String.join(", ", mm.getModuleNames())));
    }
  }

  private void handleList(CommandSender sender) {
    Set<String> names = plugin.getModuleManager().getModuleNames();
    if (names.isEmpty()) {
      sender.sendMessage(ColorUtil.mini("<red>» Brak załadowanych modułów."));
      return;
    }

    sender.sendMessage(
        ColorUtil.mini(
            "<gold>Załadowane moduły <dark_gray>(<white>"
                + names.size()
                + "<dark_gray>)</dark_gray>:"));
    for (String name : names) {
      if (plugin.getModuleManager().isEnabled(name)) {
        sender.sendMessage(ColorUtil.mini("  <green>✔ <white>" + name));
      } else {
        sender.sendMessage(ColorUtil.mini("  <red>✘ <gray>" + name));
      }
    }
  }

  private void sendInfo(CommandSender sender) {
    sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
    sender.sendMessage(
        ColorUtil.mini(
            "<gold><bold>SkyRiseCore <dark_gray>- <#459df5>v"
                + plugin.getDescription().getVersion()));
    sender.sendMessage(
        ColorUtil.mini("  <#459df5>Moduły: <white>" + plugin.getModuleManager().getModuleCount()));
    sender.sendMessage(
        ColorUtil.mini(
            "  <#459df5>Autor: <white>" + String.join(", ", plugin.getDescription().getAuthors())));
    sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
  }

  private void sendHelp(CommandSender sender) {
    sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
    sender.sendMessage(ColorUtil.mini("<gold><bold>SkyRiseCore <dark_gray>- <#459df5>Pomoc"));
    sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
    sender.sendMessage(
        ColorUtil.mini("  <yellow>/src list              <dark_gray>- <#459df5>Lista modułów"));
    sender.sendMessage(
        ColorUtil.mini("  <yellow>/src enable <moduł>    <dark_gray>- <#459df5>Włącz moduł"));
    sender.sendMessage(
        ColorUtil.mini("  <yellow>/src disable <moduł>   <dark_gray>- <#459df5>Wyłącz moduł"));
    sender.sendMessage(
        ColorUtil.mini(
            "  <yellow>/src reload            <dark_gray>- <#459df5>Przeładuj wszystko"));
    sender.sendMessage(
        ColorUtil.mini("  <yellow>/src reload <moduł>    <dark_gray>- <#459df5>Przeładuj moduł"));
    sender.sendMessage(
        ColorUtil.mini("  <yellow>/src version           <dark_gray>- <#459df5>Informacje"));
    sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
  }
}
