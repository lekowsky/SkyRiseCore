package pl.skyrise.skyRiseCore.features.roleplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class RolePlayCommands implements CommandExecutor {

  private final RolePlayModule module;
  private final Map<UUID, String> pendingOpis2Text = new ConcurrentHashMap<>();
  private final Random random = new Random();

  public RolePlayCommands(RolePlayModule module) {
    this.module = module;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(ColorUtil.mini("<red>» Tylko gracze mogą używać komend RolePlay!"));
      return true;
    }

    String cmdName = command.getName().toLowerCase();

    switch (cmdName) {
      case "me" -> handleMe(player, args);
      case "do" -> handleDo(player, args);
      case "try" -> handleTry(player, args);
      case "opis" -> handleOpis(player, args);
      case "opis2" -> handleOpis2(player, args);
    }

    return true;
  }

  private void handleMe(Player player, String[] args) {
    if (args.length == 0) {
      player.sendMessage(ColorUtil.mini("<#459df5>»</#459df5> <yellow>Użycie: /me <akcja>"));
      return;
    }

    String message = String.join(" ", args);
    String formatted =
        module.getFormatMe().replace("{player}", player.getName()).replace("{message}", message);

    sendLocalMessage(player, formatted);
    module.showHeadText(player, formatted);
  }

  private void handleDo(Player player, String[] args) {
    if (args.length == 0) {
      player.sendMessage(
          ColorUtil.mini("<#459df5>»</#459df5> <yellow>Użycie: /do <opis otoczenia>"));
      return;
    }

    String message = String.join(" ", args);
    String formatted =
        module.getFormatDo().replace("{player}", player.getName()).replace("{message}", message);

    sendLocalMessage(player, formatted);
    module.showHeadText(player, formatted);
  }

  private void handleTry(Player player, String[] args) {
    if (args.length == 0) {
      player.sendMessage(ColorUtil.mini("<#459df5>»</#459df5> <yellow>Użycie: /try <próba akcji>"));
      return;
    }

    boolean success = random.nextBoolean();
    String resultText = success ? module.getFormatTrySuccess() : module.getFormatTryFailure();

    String message = String.join(" ", args);
    String formatted =
        module
            .getFormatTry()
            .replace("{player}", player.getName())
            .replace("{message}", message)
            .replace("{result}", resultText);

    sendLocalMessage(player, formatted);
    module.showHeadText(player, formatted);
  }

  private void handleOpis(Player player, String[] args) {
    UUID uuid = player.getUniqueId();

    if (module.hasOpis(uuid)) {
      module.removePlayerOpis(uuid);
      player.sendMessage(ColorUtil.mini("<green>» Twój opis postaci został usunięty."));
      return;
    }

    if (args.length == 0) {
      player.sendMessage(
          ColorUtil.mini("<#459df5>»</#459df5> <yellow>Użycie: /opis <treść opisu postaci>"));
      return;
    }

    String text = String.join(" ", args);
    String formatted = module.getFormatOpis().replace("{message}", text);

    module.setPlayerOpis(player, formatted);
    player.sendMessage(ColorUtil.mini("<green>» Twój opis postaci został pomyślnie ustawiony."));
  }

  private void handleOpis2(Player player, String[] args) {
    UUID uuid = player.getUniqueId();

    if (module.hasOpis2(uuid)) {
      module.removeLocationOpis(uuid);
      player.sendMessage(ColorUtil.mini("<green>» Opis miejsca został usunięty."));
      pendingOpis2Text.remove(uuid);
      return;
    }

    if (args.length >= 2 && args[0].equalsIgnoreCase("time")) {
      String text = pendingOpis2Text.remove(uuid);
      if (text == null) {
        player.sendMessage(ColorUtil.mini("<red>» Błąd: Sesja wygasła. Wpisz komendę ponownie."));
        return;
      }

      try {
        int minutes = Integer.parseInt(args[1]);
        String formatted = module.getFormatOpis2().replace("{message}", text);
        module.setLocationOpis(player, formatted, minutes);

        String durationText = minutes >= 60 ? (minutes / 60) + " godz." : minutes + " min.";
        player.sendMessage(
            ColorUtil.mini(
                "<green>» Opis miejsca został ustawiony na " + durationText + ".</green>"));
      } catch (NumberFormatException e) {
        player.sendMessage(ColorUtil.mini("<red>» Wystąpił błąd podczas ustawiania czasu."));
      }
      return;
    }

    if (args.length == 0) {
      player.sendMessage(
          ColorUtil.mini("<#459df5>»</#459df5> <yellow>Użycie: /opis2 <opis miejsca stojącego>"));
      return;
    }

    String text = String.join(" ", args);
    pendingOpis2Text.put(uuid, text);

    player.sendMessage(
        ColorUtil.mini("<#459df5>»</#459df5> <#459df5>Wybierz czas trwania dla opisu miejsca:"));

    Component button5 =
        ColorUtil.mini("<green><bold>[ 5 minut ]</bold></green> ")
            .clickEvent(ClickEvent.runCommand("/opis2 time 5"))
            .hoverEvent(HoverEvent.showText(ColorUtil.mini("<gray>Ustaw czas trwania na 5 minut")));

    Component button15 =
        ColorUtil.mini("<yellow><bold>[ 15 minut ]</bold></yellow> ")
            .clickEvent(ClickEvent.runCommand("/opis2 time 15"))
            .hoverEvent(
                HoverEvent.showText(ColorUtil.mini("<gray>Ustaw czas trwania na 15 minut")));

    Component button60 =
        ColorUtil.mini("<gold><bold>[ 1 godzina ]</bold></gold> ")
            .clickEvent(ClickEvent.runCommand("/opis2 time 60"))
            .hoverEvent(
                HoverEvent.showText(ColorUtil.mini("<gray>Ustaw czas trwania na 1 godzinę")));

    Component button120 =
        ColorUtil.mini("<red><bold>[ 2 godziny ]</bold></red>")
            .clickEvent(ClickEvent.runCommand("/opis2 time 120"))
            .hoverEvent(
                HoverEvent.showText(ColorUtil.mini("<gray>Ustaw czas trwania na 2 godziny")));

    Component menu =
        Component.text("   ")
            .append(button5)
            .append(Component.text("   "))
            .append(button15)
            .append(Component.text("   "))
            .append(button60)
            .append(Component.text("   "))
            .append(button120);

    player.sendMessage(menu);
  }

  private void sendLocalMessage(Player sender, String messageText) {
    Component parsed = ColorUtil.mini(messageText);
    int rangeSq = module.getRpRange() * module.getRpRange();

    for (Player online : sender.getWorld().getPlayers()) {
      if (online.getLocation().distanceSquared(sender.getLocation()) <= rangeSq) {
        online.sendMessage(parsed);
      }
    }
    sender.getServer().getConsoleSender().sendMessage(parsed);
  }
}
