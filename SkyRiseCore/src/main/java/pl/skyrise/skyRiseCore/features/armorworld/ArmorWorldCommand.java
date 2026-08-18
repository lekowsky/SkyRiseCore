package pl.skyrise.skyRiseCore.features.armorworld;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class ArmorWorldCommand implements CommandExecutor {

  private final ArmorWorldModule module;

  public ArmorWorldCommand(ArmorWorldModule module) {
    this.module = module;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("skyrise.armorworld.admin")) {
      sender.sendMessage(ColorUtil.mini("<red>» Nie masz uprawnień!"));
      return true;
    }

    if (args.length == 0) {
      sendHelp(sender);
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "add" -> handleAdd(sender, args);
      case "remove" -> handleRemove(sender, args);
      case "list" -> handleList(sender);
      case "off" -> handleOff(sender);
      case "items" -> handleItems(sender);
      default -> sendHelp(sender);
    }
    return true;
  }

  private void handleAdd(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(ColorUtil.mini("<red>» Podaj nazwę świata! <white>/aw add <świat>"));
      return;
    }

    String world = args[1];
    if (module.addWorld(world)) {
      sender.sendMessage(
          ColorUtil.mini(
              "<green>» Świat <gold>" + world + "</gold><green> dodany do blokady zbroi."));
    } else {
      sender.sendMessage(
          ColorUtil.mini("<red>» Świat <gold>" + world + "</gold> jest już zablokowany."));
    }
  }

  private void handleRemove(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(ColorUtil.mini("<red>» Podaj nazwę świata! <white>/aw remove <świat>"));
      return;
    }

    String world = args[1];
    if (module.removeWorld(world)) {
      sender.sendMessage(ColorUtil.mini("<green>» Świat <gold>" + world + "</gold> odblokowany."));
    } else {
      sender.sendMessage(
          ColorUtil.mini("<red>» Świat <gold>" + world + "</gold> nie jest zablokowany."));
    }
  }

  private void handleList(CommandSender sender) {
    Set<String> worlds = module.getBlockedWorlds();
    if (worlds.isEmpty()) {
      sender.sendMessage(ColorUtil.mini("<red>» Brak zablokowanych światów."));
      return;
    }

    sender.sendMessage(
        ColorUtil.mini(
            "<gold>Zablokowane światy <dark_gray>(<white>"
                + worlds.size()
                + "<dark_gray>)</dark_gray>:"));
    for (String name : worlds) {
      sender.sendMessage(ColorUtil.mini("  <red>✘ <white>" + name));
    }
  }

  private void handleItems(CommandSender sender) {
    Set<Material> items = module.getBlockedMaterials();
    if (items.isEmpty()) {
      sender.sendMessage(ColorUtil.mini("<red>» Brak zablokowanych przedmiotów."));
      return;
    }

    sender.sendMessage(
        ColorUtil.mini(
            "<gold>Zablokowane przedmioty <dark_gray>(<white>"
                + items.size()
                + "<dark_gray>)</dark_gray>:"));
    StringBuilder line = new StringBuilder();
    for (Material mat : items) {
      if (line.length() > 0) line.append("<dark_gray>, </dark_gray>");
      line.append("<white>").append(module.formatMaterial(mat));
    }
    sender.sendMessage(ColorUtil.mini("  " + line));
    sender.sendMessage(ColorUtil.mini("<red>» Zmień listę w config/armorworld.yml"));
  }

  private void handleOff(CommandSender sender) {
    module.clearWorlds();
    sender.sendMessage(ColorUtil.mini("<green>» Wszystkie blokady światów usunięte."));
  }

  private void sendHelp(CommandSender sender) {
    sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
    sender.sendMessage(ColorUtil.mini("<gold><bold>ArmorWorld <dark_gray>- <#459df5>Pomoc"));
    sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
    sender.sendMessage(
        ColorUtil.mini("  <yellow>/aw add <świat>     <dark_gray>- <#459df5>Zablokuj świat"));
    sender.sendMessage(
        ColorUtil.mini("  <yellow>/aw remove <świat>  <dark_gray>- <#459df5>Odblokuj świat"));
    sender.sendMessage(
        ColorUtil.mini(
            "  <yellow>/aw list            <dark_gray>- <#459df5>Lista zablokowanych światów"));
    sender.sendMessage(
        ColorUtil.mini(
            "  <yellow>/aw items           <dark_gray>- <#459df5>Lista zablokowanych przedmiotów"));
    sender.sendMessage(
        ColorUtil.mini(
            "  <yellow>/aw off             <dark_gray>- <#459df5>Usuń wszystkie blokady"));
    sender.sendMessage(ColorUtil.mini("<dark_gray><strikethrough>                              "));
  }
}
