package pl.skyrise.skyRiseCore.features.insurance.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.features.insurance.InsuranceModule;
import pl.skyrise.skyRiseCore.features.insurance.util.InsuranceText;

public class InsuranceCommand implements CommandExecutor {

  private final InsuranceModule module;

  public InsuranceCommand(InsuranceModule module) {
    this.module = module;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
      handleStatus(sender, args);
      return true;
    }

    String sub = args[0].toLowerCase();
    switch (sub) {
      case "open" -> handleOpen(sender);
      case "give" -> handleGive(sender, args);
      case "remove" -> handleRemove(sender, args);
      case "expire" -> handleExpire(sender, args);
      case "setseconds" -> handleSetSeconds(sender, args);
      case "charges" -> handleCharges(sender, args);
      case "effect" -> handleEffect(sender, args);
      case "list" -> handleList(sender);
      case "help" -> sendHelp(sender, label);
      default -> sendHelp(sender, label);
    }
    return true;
  }

  private void handleStatus(CommandSender sender, String[] args) {
    if (args.length >= 2) {
      if (!sender.hasPermission("skyrise.insurance.admin")) {
        send(sender, module.message("no-permission"));
        return;
      }
      OfflinePlayer target = offline(args[1]);
      boolean active = module.hasInsurance(target.getUniqueId());
      String msg =
          module
              .message(active ? "admin-status-active" : "admin-status-inactive")
              .replace("{player}", target.getName() != null ? target.getName() : args[1])
              .replace(
                  "{time_left}",
                  module.formatDuration(module.getRemainingMillis(target.getUniqueId())))
              .replace("{charges}", String.valueOf(module.getCharges(target.getUniqueId())))
              .replace("{effect}", module.getSelectedEffectId(target.getUniqueId()));
      send(sender, msg);
      return;
    }

    if (!(sender instanceof Player player)) {
      send(sender, module.message("only-player"));
      return;
    }
    module.send(
        player, module.hasInsurance(player.getUniqueId()) ? "status-active" : "status-inactive");
  }

  private void handleOpen(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      send(sender, module.message("only-player"));
      return;
    }
    if (!player.hasPermission("skyrise.insurance.admin")) {
      module.send(player, "no-permission");
      return;
    }
    module.openGui(player);
  }

  private void handleGive(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 2) {
      send(
          sender,
          "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><red>Użycie: /ubezpieczenie"
              + " give <gracz> [dni]");
      return;
    }
    OfflinePlayer target = offline(args[1]);
    long days = args.length >= 3 ? parseLong(args[2], 7L) : 7L;
    module.grantInsurance(target.getUniqueId(), days);
    send(
        sender,
        module
            .message("admin-given")
            .replace("{player}", target.getName() != null ? target.getName() : args[1])
            .replace("{days}", String.valueOf(Math.max(1L, days)))
            .replace(
                "{time_left}",
                module.formatDuration(module.getRemainingMillis(target.getUniqueId()))));
    if (target.isOnline() && target.getPlayer() != null) {
      module.send(target.getPlayer(), "admin-given-target");
    }
  }

  private void handleRemove(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 2) {
      send(
          sender,
          "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><red>Użycie: /ubezpieczenie"
              + " remove <gracz>");
      return;
    }
    OfflinePlayer target = offline(args[1]);
    boolean removed = module.removeInsurance(target.getUniqueId());
    send(
        sender,
        module
            .message(removed ? "admin-removed" : "admin-no-policy")
            .replace("{player}", target.getName() != null ? target.getName() : args[1]));
  }

  private void handleExpire(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 2) {
      send(
          sender,
          "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><red>Użycie: /ubezpieczenie"
              + " expire <gracz>");
      return;
    }
    OfflinePlayer target = offline(args[1]);
    boolean removed = module.removeInsurance(target.getUniqueId());
    send(
        sender,
        module
            .message(removed ? "admin-expired" : "admin-no-policy")
            .replace("{player}", target.getName() != null ? target.getName() : args[1]));
  }

  private void handleSetSeconds(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 3) {
      send(
          sender,
          "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><red>Użycie: /ubezpieczenie"
              + " setseconds <gracz> <sekundy>");
      return;
    }
    OfflinePlayer target = offline(args[1]);
    long seconds = parseLong(args[2], 60L);
    module.setInsuranceSeconds(target.getUniqueId(), seconds);
    send(
        sender,
        module
            .message("admin-setseconds")
            .replace("{player}", target.getName() != null ? target.getName() : args[1])
            .replace("{seconds}", String.valueOf(Math.max(1L, seconds)))
            .replace(
                "{time_left}",
                module.formatDuration(module.getRemainingMillis(target.getUniqueId()))));
  }

  private void handleCharges(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 3) {
      send(
          sender,
          "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><red>Użycie: /ubezpieczenie"
              + " charges <gracz> <ilość>");
      return;
    }
    OfflinePlayer target = offline(args[1]);
    int charges = (int) Math.max(0L, parseLong(args[2], 1L));
    module.setCharges(target.getUniqueId(), charges);
    send(
        sender,
        "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><white>Ustawiono ładunki gracza"
            + " <reset><#f5f242>"
            + (target.getName() != null ? target.getName() : args[1])
            + " <reset><white>na <reset><#f5f242>"
            + module.getCharges(target.getUniqueId())
            + "<reset><gray>/<reset><yellow>"
            + module.getMaxCharges()
            + "<reset><white>.");
  }

  private void handleEffect(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 3) {
      send(
          sender,
          "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><red>Użycie: /ubezpieczenie"
              + " effect <gracz> <efekt>");
      return;
    }
    OfflinePlayer target = offline(args[1]);
    if (module.setSelectedEffect(target.getUniqueId(), args[2])) {
      send(
          sender,
          "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><white>Ustawiono efekt gracza"
              + " <reset><#f5f242>"
              + (target.getName() != null ? target.getName() : args[1])
              + " <reset><white>na <reset><#f5f242>"
              + args[2]
              + "<reset><white>.");
    } else {
      send(
          sender,
          "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><red>Nie znaleziono efektu"
              + " <reset><#f5f242>"
              + args[2]
              + "<reset><red>.");
    }
  }

  private void handleList(CommandSender sender) {
    if (!requireAdmin(sender)) return;
    Map<UUID, Long> policies = module.getPolicyExpirations();
    if (policies.isEmpty()) {
      send(sender, module.message("admin-list-empty"));
      return;
    }
    send(sender, module.message("admin-list-header"));
    for (Map.Entry<UUID, Long> entry : policies.entrySet()) {
      OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
      String name = player.getName() != null ? player.getName() : entry.getKey().toString();
      send(
          sender,
          module
              .message("admin-list-entry")
              .replace("{player}", name)
              .replace(
                  "{time_left}",
                  module.formatDuration(
                      Math.max(0L, entry.getValue() - System.currentTimeMillis())))
              .replace("{charges}", String.valueOf(module.getCharges(entry.getKey())))
              .replace("{effect}", module.getSelectedEffectId(entry.getKey())));
    }
  }

  private void sendHelp(CommandSender sender, String label) {
    send(sender, module.message("help-header"));
    send(
        sender,
        "<reset><#f5f242>/"
            + label
            + " status <reset><gray>- <reset><white>Sprawdź aktywne ubezpieczenie");
    if (sender.hasPermission("skyrise.insurance.admin")) {
      send(
          sender,
          "<reset><#f5f242>/"
              + label
              + " status <gracz> <reset><gray>- <reset><white>Status gracza");
      send(
          sender,
          "<reset><#f5f242>/" + label + " open <reset><gray>- <reset><white>Otwórz GUI testowo");
      send(
          sender,
          "<reset><#f5f242>/"
              + label
              + " give <gracz> [dni] <reset><gray>- <reset><white>Nadaj/przedłuż polisę");
      send(
          sender,
          "<reset><#f5f242>/"
              + label
              + " setseconds <gracz> <sekundy> <reset><gray>- <reset><white>Ustaw czas testowy");
      send(
          sender,
          "<reset><#f5f242>/"
              + label
              + " charges <gracz> <ilość> <reset><gray>- <reset><white>Ustaw ładunki");
      send(
          sender,
          "<reset><#f5f242>/"
              + label
              + " effect <gracz> <efekt> <reset><gray>- <reset><white>Ustaw efekt ratunku");
      send(
          sender,
          "<reset><#f5f242>/" + label + " remove <gracz> <reset><gray>- <reset><white>Usuń polisę");
      send(
          sender,
          "<reset><#f5f242>/"
              + label
              + " expire <gracz> <reset><gray>- <reset><white>Wymuś wygaśnięcie");
      send(sender, "<reset><#f5f242>/" + label + " list <reset><gray>- <reset><white>Lista polis");
    }
  }

  public List<String> tab(CommandSender sender, String[] args) {
    if (args.length == 1) {
      if (sender.hasPermission("skyrise.insurance.admin")) {
        return TabRegistry.filter(
            List.of(
                "status",
                "open",
                "give",
                "remove",
                "expire",
                "setseconds",
                "charges",
                "effect",
                "list",
                "help"),
            args[0]);
      }
      return TabRegistry.filter(List.of("status", "help"), args[0]);
    }
    if (args.length == 2 && sender.hasPermission("skyrise.insurance.admin")) {
      String sub = args[0].toLowerCase();
      if (List.of("status", "give", "remove", "expire", "setseconds", "charges", "effect")
          .contains(sub)) {
        List<String> names = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
        return TabRegistry.filter(names, args[1]);
      }
    }
    if (args.length == 3 && sender.hasPermission("skyrise.insurance.admin")) {
      if (args[0].equalsIgnoreCase("give"))
        return TabRegistry.filter(List.of("1", "7", "14", "30"), args[2]);
      if (args[0].equalsIgnoreCase("setseconds"))
        return TabRegistry.filter(List.of("30", "60", "3600", "86400"), args[2]);
      if (args[0].equalsIgnoreCase("charges"))
        return TabRegistry.filter(List.of("1", "5", "10", "25"), args[2]);
      if (args[0].equalsIgnoreCase("effect"))
        return TabRegistry.filter(module.getRescueEffects().keySet(), args[2]);
    }
    return List.of();
  }

  private boolean requireAdmin(CommandSender sender) {
    if (!sender.hasPermission("skyrise.insurance.admin")) {
      send(sender, module.message("no-permission"));
      return false;
    }
    return true;
  }

  private void send(CommandSender sender, String raw) {
    sender.sendMessage(InsuranceText.component(raw));
  }

  @SuppressWarnings("deprecation")
  private OfflinePlayer offline(String name) {
    Player online = Bukkit.getPlayerExact(name);
    return online != null ? online : Bukkit.getOfflinePlayer(name);
  }

  private long parseLong(String raw, long fallback) {
    try {
      return Long.parseLong(raw);
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }
}
