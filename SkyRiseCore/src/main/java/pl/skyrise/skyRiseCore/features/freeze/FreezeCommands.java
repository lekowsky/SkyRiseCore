package pl.skyrise.skyRiseCore.features.freeze;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class FreezeCommands implements CommandExecutor {

  private final FreezeModule module;

  public FreezeCommands(FreezeModule module) {
    this.module = module;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String cmdName = command.getName().toLowerCase();

    switch (cmdName) {
      case "setfreeze" -> {
        if (!(sender instanceof Player player)) {
          sender.sendMessage(ColorUtil.mini("<red>» Tylko gracze mogą ustawiać punkt freeze!"));
          return true;
        }
        if (!player.isOp()) {
          player.sendMessage(
              ColorUtil.mini(
                  "<red>» Nie masz uprawnień! Tylko operatorzy (OP) mogą ustawiać punkt freeze."));
          return true;
        }
        module.setFreezeLocation(player.getLocation());
        player.sendMessage(ColorUtil.mini("<green>» Punkt freeze ustawiony na twoją pozycję."));
      }
      case "delfreeze" -> {
        if (!sender.isOp()) {
          sender.sendMessage(
              ColorUtil.mini(
                  "<red>» Nie masz uprawnień! Tylko operatorzy (OP) mogą usuwać punkt freeze."));
          return true;
        }
        if (module.getFreezeLocation() == null) {
          sender.sendMessage(ColorUtil.mini("<red>» Nie ustawiono jeszcze punktu freeze."));
          return true;
        }
        module.removeFreezeLocation();
        sender.sendMessage(ColorUtil.mini("<green>» Punkt freeze został usunięty."));
      }
      case "freeze" -> {
        if (!sender.hasPermission(module.getPermission())) {
          sender.sendMessage(ColorUtil.mini("<red>» Nie masz uprawnień!"));
          return true;
        }
        if (args.length == 0) {
          sender.sendMessage(ColorUtil.mini("<#459df5>»</#459df5> <yellow>Użyj: /freeze <gracz>"));
          return true;
        }

        String targetName = args[0];

        if (targetName.equalsIgnoreCase(sender.getName())) {
          sender.sendMessage(ColorUtil.mini("<red>» Nie możesz zamrozić samego siebie!"));
          return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();

        if (target.isOp()
            || (target.isOnline()
                && target.getPlayer() != null
                && target.getPlayer().hasPermission(module.getPermission()))) {
          sender.sendMessage(ColorUtil.mini("<red>» Nie możesz zamrozić członka administracji!"));
          return true;
        }

        if (module.isFrozen(uuid)) {
          sender.sendMessage(
              ColorUtil.mini(
                  "<red>» Gracz <white>"
                      + (target.getName() != null ? target.getName() : targetName)
                      + "</white> jest już zamrożony."));
          return true;
        }

        if (target.isOnline() && target.getPlayer() != null) {
          Player onlinePlayer = target.getPlayer();
          module.freezePlayer(
              uuid,
              onlinePlayer.getLocation(),
              onlinePlayer.getLocation().getYaw(),
              onlinePlayer.getLocation().getPitch());
        } else {
          Location defaultLoc =
              module.getFreezeLocation() != null
                  ? module.getFreezeLocation()
                  : new Location(Bukkit.getWorlds().get(0), 0.5, 64, 0.5);
          module.freezePlayer(uuid, defaultLoc, 0.0f, 0.0f);
        }

        broadcastToStaff(
            "<red>» Gracz <white>"
                + (target.getName() != null ? target.getName() : targetName)
                + "</white><red> został zamrożony przez <white>"
                + sender.getName()
                + "</white>.");
      }
      case "unfreeze" -> {
        if (!sender.hasPermission(module.getPermission())) {
          sender.sendMessage(ColorUtil.mini("<red>» Nie masz uprawnień!"));
          return true;
        }
        if (args.length == 0) {
          sender.sendMessage(
              ColorUtil.mini("<#459df5>»</#459df5> <yellow>Użyj: /unfreeze <gracz>"));
          return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();

        if (!module.isFrozen(uuid)) {
          sender.sendMessage(
              ColorUtil.mini(
                  "<red>» Gracz <white>"
                      + (target.getName() != null ? target.getName() : targetName)
                      + "</white> nie jest zamrożony."));
          return true;
        }

        module.unfreezePlayer(uuid);
        broadcastToStaff(
            "<green>» Gracz <white>"
                + (target.getName() != null ? target.getName() : targetName)
                + "</white> został odmrożony przez <white>"
                + sender.getName()
                + "</white>.");
      }
    }

    return true;
  }

  private void broadcastToStaff(String message) {
    net.kyori.adventure.text.Component parsed = ColorUtil.mini(message);
    for (Player online : Bukkit.getOnlinePlayers()) {
      if (online.hasPermission(module.getPermission())) {
        online.sendMessage(parsed);
      }
    }
    Bukkit.getConsoleSender().sendMessage(parsed);
  }
}
