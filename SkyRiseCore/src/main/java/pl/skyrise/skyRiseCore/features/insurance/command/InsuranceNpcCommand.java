package pl.skyrise.skyRiseCore.features.insurance.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.features.insurance.InsuranceModule;
import pl.skyrise.skyRiseCore.features.insurance.model.NpcPoint;
import pl.skyrise.skyRiseCore.features.insurance.util.InsuranceText;

public class InsuranceNpcCommand implements CommandExecutor {

  private final InsuranceModule module;

  public InsuranceNpcCommand(InsuranceModule module) {
    this.module = module;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("skyrise.insurance.admin")) {
      sender.sendMessage(InsuranceText.component(module.message("no-permission")));
      return true;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      sendHelp(sender, label);
      return true;
    }

    String sub = args[0].toLowerCase();
    String id = args.length >= 2 ? module.normalizeId(args[1]) : "default";

    switch (sub) {
      case "set" -> {
        if (!(sender instanceof Player player)) {
          sender.sendMessage(InsuranceText.component(module.message("only-player")));
          return true;
        }
        module.setNpc(id, player.getLocation());
        sender.sendMessage(InsuranceText.component(module.message("npc-set").replace("{id}", id)));
      }
      case "remove" -> {
        if (module.removeNpc(id)) {
          sender.sendMessage(
              InsuranceText.component(module.message("npc-removed").replace("{id}", id)));
        } else {
          sender.sendMessage(
              InsuranceText.component(module.message("npc-not-found").replace("{id}", id)));
        }
      }
      case "respawn" -> {
        if (module.respawnNpc(id)) {
          sender.sendMessage(
              InsuranceText.component(module.message("npc-respawned").replace("{id}", id)));
        } else {
          sender.sendMessage(
              InsuranceText.component(module.message("npc-not-found").replace("{id}", id)));
        }
      }
      case "list" -> {
        if (module.getNpcPoints().isEmpty()) {
          sender.sendMessage(InsuranceText.component(module.message("npc-list-empty")));
          return true;
        }
        sender.sendMessage(InsuranceText.component(module.message("npc-list-header")));
        for (NpcPoint point : module.getNpcPoints().values()) {
          sender.sendMessage(
              InsuranceText.component(
                  module
                      .message("npc-list-entry")
                      .replace("{id}", point.getId())
                      .replace("{location}", point.formatLocation())));
        }
      }
      case "skin" -> {
        if (args.length < 2) {
          sender.sendMessage(
              InsuranceText.component(
                  "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><red>Użycie: /"
                      + label
                      + " skin <nick/skóra>"));
          return true;
        }
        module.setNpcSkin(args[1]);
        sender.sendMessage(
            InsuranceText.component(module.message("npc-skin-set").replace("{skin}", args[1])));
      }
      case "tp" -> {
        if (!(sender instanceof Player player)) {
          sender.sendMessage(InsuranceText.component(module.message("only-player")));
          return true;
        }
        NpcPoint point = module.getNpcPoints().get(id);
        if (point == null || point.getLocation() == null) {
          sender.sendMessage(
              InsuranceText.component(module.message("npc-not-found").replace("{id}", id)));
          return true;
        }
        player.teleport(point.getLocation());
        sender.sendMessage(
            InsuranceText.component(module.message("npc-teleported").replace("{id}", id)));
      }
      default -> sendHelp(sender, label);
    }

    return true;
  }

  private void sendHelp(CommandSender sender, String label) {
    sender.sendMessage(
        InsuranceText.component(
            "<reset><#f5f242><bold>Ubezpieczyciel <reset><gray>» <reset><white>Pomoc:"));
    sender.sendMessage(
        InsuranceText.component(
            "<reset><#f5f242>/"
                + label
                + " set [id] <reset><gray>- <reset><white>Ustaw ubezpieczyciela w swojej pozycji"));
    sender.sendMessage(
        InsuranceText.component(
            "<reset><#f5f242>/"
                + label
                + " remove [id] <reset><gray>- <reset><white>Usuń ubezpieczyciela"));
    sender.sendMessage(
        InsuranceText.component(
            "<reset><#f5f242>/"
                + label
                + " list <reset><gray>- <reset><white>Lista ubezpieczycieli"));
    sender.sendMessage(
        InsuranceText.component(
            "<reset><#f5f242>/"
                + label
                + " tp [id] <reset><gray>- <reset><white>Teleport do ubezpieczyciela"));
    sender.sendMessage(
        InsuranceText.component(
            "<reset><#f5f242>/"
                + label
                + " respawn [id] <reset><gray>- <reset><white>Odśwież ubezpieczyciela"));
    sender.sendMessage(
        InsuranceText.component(
            "<reset><#f5f242>/"
                + label
                + " skin <nick/skóra> <reset><gray>- <reset><white>Zmień skin Citizens NPC"));
  }

  public List<String> tab(CommandSender sender, String[] args) {
    if (!sender.hasPermission("skyrise.insurance.admin")) return List.of();
    if (args.length == 1) {
      return TabRegistry.filter(
          List.of("set", "remove", "list", "tp", "respawn", "skin", "help"), args[0]);
    }
    if (args.length == 2 && List.of("remove", "tp", "respawn").contains(args[0].toLowerCase())) {
      return TabRegistry.filter(new ArrayList<>(module.getNpcPoints().keySet()), args[1]);
    }
    return List.of();
  }
}
