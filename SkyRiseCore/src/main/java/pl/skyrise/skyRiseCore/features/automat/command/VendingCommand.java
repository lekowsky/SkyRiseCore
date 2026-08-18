package pl.skyrise.skyRiseCore.features.automat.command;

import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.gui.EditorGUI;
import pl.skyrise.skyRiseCore.features.automat.gui.MachineGUI;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class VendingCommand implements CommandExecutor, TabCompleter {

  private final AutomatModule plugin;

  private static final List<String> SUBCOMMANDS =
      Arrays.asList(
          "create",
          "delete",
          "edit",
          "open",
          "remove",
          "stock",
          "restock",
          "restocklist",
          "setmodel",
          "list",
          "reload",
          "info",
          "tp",
          "help");

  public VendingCommand(AutomatModule plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      sendHelp(sender, label);
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "create" -> handleCreate(sender, args);
      case "delete" -> handleDelete(sender, args);
      case "edit" -> handleEdit(sender, args);
      case "open" -> handleOpen(sender, args);
      case "remove" -> handleRemove(sender);
      case "stock" -> handleStock(sender, args);
      case "restock" -> handleRestock(sender, args);
      case "restocklist" -> handleRestockList(sender);
      case "setmodel" -> handleSetModel(sender, args);
      case "list" -> handleList(sender);
      case "reload" -> handleReload(sender);
      case "info" -> handleInfo(sender, args);
      case "tp" -> handleTeleport(sender, args);
      case "help" -> sendHelp(sender, label);
      default ->
          sender.sendMessage(
              plugin.getPrefix()
                  + ColorUtil.miniToLegacy("<reset><red>Nieznana komenda. /" + label + " help"));
    }
    return true;
  }

  private void handleCreate(CommandSender sender, String[] args) {
    if (!sender.hasPermission("vendingmachine.create")) {
      noPermission(sender);
      return;
    }
    if (args.length < 2) {
      sender.sendMessage(
          plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>/automat create <nazwa>"));
      return;
    }
    String name = args[1];
    MachineTemplate t = plugin.getMachineManager().createTemplate(name);
    if (t == null) {
      sender.sendMessage(plugin.getPrefix() + msg("exists").replace("{name}", name));
      return;
    }
    sender.sendMessage(plugin.getPrefix() + msg("created").replace("{name}", name));
    if (sender instanceof Player p) new EditorGUI(plugin, t, p).open();
  }

  private void handleDelete(CommandSender sender, String[] args) {
    if (!sender.hasPermission("vendingmachine.delete")) {
      noPermission(sender);
      return;
    }
    if (args.length < 2) {
      sender.sendMessage(
          plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>/automat delete <nazwa>"));
      return;
    }
    if (plugin.getMachineManager().deleteTemplate(args[1]))
      sender.sendMessage(plugin.getPrefix() + msg("deleted").replace("{name}", args[1]));
    else sender.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1]));
  }

  private void handleEdit(CommandSender sender, String[] args) {
    if (!(sender instanceof Player p)) {
      sender.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>Tylko gracze mogą użyć tej komendy."));
      return;
    }
    if (!p.hasPermission("vendingmachine.edit")) {
      noPermission(p);
      return;
    }
    if (args.length < 2) {
      p.sendMessage(
          plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>/automat edit <nazwa>"));
      return;
    }
    MachineTemplate t = plugin.getMachineManager().getTemplate(args[1]);
    if (t == null) {
      p.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1]));
      return;
    }
    new EditorGUI(plugin, t, p).open();
  }

  private void handleOpen(CommandSender sender, String[] args) {
    if (!(sender instanceof Player p)) {
      sender.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>Tylko gracze mogą użyć tej komendy."));
      return;
    }
    if (args.length < 2) {
      p.sendMessage(
          plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>/automat open <nazwa>"));
      return;
    }
    MachineTemplate t = plugin.getMachineManager().getTemplate(args[1]);
    if (t == null) {
      p.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1]));
      return;
    }
    if (!t.isEnabled() && !p.hasPermission("vendingmachine.admin")) {
      p.sendMessage(plugin.getPrefix() + msg("disabled"));
      return;
    }
    if (!t.getPermission().isEmpty() && !p.hasPermission(t.getPermission())) {
      noPermission(p);
      return;
    }

    new MachineGUI(plugin, t, p, null).open();
  }

  private void handleRemove(CommandSender sender) {
    if (!(sender instanceof Player p)) {
      sender.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>Tylko gracze mogą użyć tej komendy."));
      return;
    }
    if (!p.hasPermission("vendingmachine.delete")) {
      noPermission(p);
      return;
    }
    org.bukkit.block.Block target = p.getTargetBlockExact(5);
    if (target == null) {
      p.sendMessage(
          plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>Musisz patrzeć na blok!"));
      return;
    }
    if (plugin.getPlacementManager().remove(target.getLocation()))
      p.sendMessage(plugin.getPrefix() + msg("removed"));
    else
      p.sendMessage(plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>Tu nie ma automatu!"));
  }

  private void handleStock(CommandSender sender, String[] args) {
    if (!sender.hasPermission("vendingmachine.admin")) {
      noPermission(sender);
      return;
    }
    if (args.length < 2) {
      sender.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>/automat stock <info|fill> [szablon]"));
      return;
    }

    String action = args[1].toLowerCase();

    switch (action) {
      case "fill" -> {
        if (!(sender instanceof Player p)) {
          sender.sendMessage(
              plugin.getPrefix()
                  + ColorUtil.miniToLegacy(
                      "<reset><red>Tylko gracze (musisz patrzeć na automat)."));
          return;
        }
        org.bukkit.block.Block target = p.getTargetBlockExact(5);
        if (target == null) {
          p.sendMessage(
              plugin.getPrefix()
                  + ColorUtil.miniToLegacy("<reset><red>Musisz patrzeć na automat!"));
          return;
        }
        MachinePlacement placement =
            plugin.getPlacementManager().getPlacement(target.getLocation());
        if (placement == null) {
          p.sendMessage(
              plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>Tu nie ma automatu!"));
          return;
        }
        plugin.getRestockManager().fillPlacement(placement);
        p.sendMessage(
            plugin.getPrefix()
                + ColorUtil.miniToLegacy(
                    "<reset><white>Wypełniono zapasy automatu <reset><yellow>"
                        + placement.getTemplateName()
                        + "<reset><white> do maksimum!"));
      }
      case "info" -> {
        if (args.length < 3) {
          sender.sendMessage(
              plugin.getPrefix()
                  + ColorUtil.miniToLegacy("<reset><red>/automat stock info <szablon>"));
          return;
        }
        MachineTemplate t = plugin.getMachineManager().getTemplate(args[2]);
        if (t == null) {
          sender.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[2]));
          return;
        }

        int placements = plugin.getPlacementManager().getPlacementCount(t.getName());
        sender.sendMessage(
            plugin.getPrefix()
                + ColorUtil.miniToLegacy(
                    "<reset><gold><bold>--- Stock szablonu: " + t.getName() + " ---"));
        sender.sendMessage(
            plugin.getPrefix()
                + ColorUtil.miniToLegacy(
                    "<reset><gray>Aktywnych automatów: <reset><yellow>" + placements));
        sender.sendMessage(
            plugin.getPrefix()
                + ColorUtil.miniToLegacy(
                    "<reset><gray>Auto-restock: "
                        + (t.isAutoRestockEnabled() ? "<reset><green>Wł" : "<reset><red>Wył")
                        + " <reset><gray>(co "
                        + t.getAutoRestockInterval()
                        + " min, +"
                        + t.getAutoRestockAmount()
                        + " szt.)"));
        sender.sendMessage(
            plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><gray>Itemy (defaulty):"));
        for (VendingItem item : t.getItems().values()) {
          String stockInfo =
              item.isUnlimitedStock()
                  ? "<reset><green>∞"
                  : "<reset><gray>max: <reset><yellow>" + item.getMaxStock();
          sender.sendMessage(
              plugin.getPrefix()
                  + ColorUtil.miniToLegacy(
                      "  <reset><gray>"
                          + item.getId()
                          + ": "
                          + stockInfo
                          + " <reset><gray>- <reset><white>"
                          + item.getDisplayName()));
        }
      }
    }
  }

  private void handleRestock(CommandSender sender, String[] args) {
    if (!sender.hasPermission("vendingmachine.admin")) {
      noPermission(sender);
      return;
    }
    if (args.length < 2) {
      sender.sendMessage(
          plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>/automat restock <szablon>"));
      return;
    }
    MachineTemplate t = plugin.getMachineManager().getTemplate(args[1]);
    if (t == null) {
      sender.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1]));
      return;
    }

    int count = plugin.getRestockManager().restockAllPlacementsForTemplate(t);
    plugin.getDataManager().savePlacements();
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><white>Wykonano restock <reset><yellow>"
                    + count
                    + " <reset><white>automatu/ów typu <reset><yellow>"
                    + t.getName()
                    + " <reset><white>(+"
                    + t.getAutoRestockAmount()
                    + " szt./item)"));
  }

  private void handleRestockList(CommandSender sender) {
    if (!sender.hasPermission("vendingmachine.restock.notify")
        && !sender.hasPermission("vendingmachine.admin")) {
      noPermission(sender);
      return;
    }

    int threshold = plugin.getConfig().getInt("stock.low-stock-warning.threshold", 5);

    List<String[]> emptyList = new ArrayList<>();
    List<String[]> lowList = new ArrayList<>();

    int nr = 1;
    for (MachinePlacement placement : plugin.getPlacementManager().getAllPlacements()) {
      MachineTemplate t = plugin.getMachineManager().getTemplate(placement.getTemplateName());
      if (t == null) continue;

      Location loc = placement.getLocation();
      String locStr =
          loc.getWorld().getName()
              + " <reset><gray>(<reset><white>"
              + loc.getBlockX()
              + "<reset><gray>, <reset><white>"
              + loc.getBlockY()
              + "<reset><gray>, <reset><white>"
              + loc.getBlockZ()
              + "<reset><gray>)";

      for (VendingItem item : t.getItems().values()) {
        if (item.isUnlimitedStock()) continue;
        int stock = placement.getStock(item.getId());

        if (stock == 0) {
          emptyList.add(
              new String[] {
                String.valueOf(nr++),
                t.getName(),
                locStr,
                ColorUtil.miniToLegacy(item.getDisplayName()),
                "<reset><red>0<reset><gray>/<reset><yellow>" + item.getMaxStock()
              });
        } else if (stock <= threshold) {
          lowList.add(
              new String[] {
                String.valueOf(nr++),
                t.getName(),
                locStr,
                ColorUtil.miniToLegacy(item.getDisplayName()),
                "<reset><yellow>" + stock + "<reset><gray>/<reset><yellow>" + item.getMaxStock()
              });
        }
      }
    }

    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><gold><bold>========= AUTOMATY DO UZUPELNIENIA ========="));

    if (emptyList.isEmpty() && lowList.isEmpty()) {
      sender.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  "<reset><white>Wszystkie automaty mają wystarczająco zapasów!"));
      return;
    }

    if (!emptyList.isEmpty()) {
      sender.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  "<reset><red><bold>--- WYPRZEDANE (" + emptyList.size() + ") ---"));
      for (String[] row : emptyList) {
        sender.sendMessage(
            plugin.getPrefix()
                + ColorUtil.miniToLegacy(
                    "<reset><gray>#"
                        + row[0]
                        + " <reset><dark_gray>[<reset><gold>"
                        + row[1]
                        + "<reset><dark_gray>] <reset><white>"
                        + row[3]
                        + " <reset><dark_gray>» "
                        + row[4]
                        + " <reset><dark_gray>- <reset><gray>"
                        + row[2]));
      }
    }

    if (!lowList.isEmpty()) {
      sender.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  "<reset><yellow><bold>--- NISKI STOCK (" + lowList.size() + ") ---"));
      for (String[] row : lowList) {
        sender.sendMessage(
            plugin.getPrefix()
                + ColorUtil.miniToLegacy(
                    "<reset><gray>#"
                        + row[0]
                        + " <reset><dark_gray>[<reset><gold>"
                        + row[1]
                        + "<reset><dark_gray>] <reset><white>"
                        + row[3]
                        + " <reset><dark_gray>» "
                        + row[4]
                        + " <reset><dark_gray>- <reset><gray>"
                        + row[2]));
      }
    }

    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><gray>Łącznie: <reset><red>"
                    + emptyList.size()
                    + " wyprzedanych <reset><gray>| <reset><yellow>"
                    + lowList.size()
                    + " z niskim stockiem"));
  }

  private void handleSetModel(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>Tylko gracze mogą użyć tej komendy."));
      return;
    }
    if (!player.hasPermission("vendingmachine.edit")
        && !player.hasPermission("vendingmachine.admin")) {
      noPermission(player);
      return;
    }
    if (args.length < 2) {
      player.sendMessage(
          plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>/automat setmodel <szablon>"));
      return;
    }
    MachineTemplate template = plugin.getMachineManager().getTemplate(args[1]);
    if (template == null) {
      player.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1]));
      return;
    }
    if (plugin.getNexoManager() == null || !plugin.getNexoManager().isNexoAvailable()) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>Nexo nie jest dostępne na serwerze."));
      return;
    }

    ItemStack hand = player.getInventory().getItemInMainHand();
    if (hand == null || hand.getType() == Material.AIR) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>Musisz trzymać item/mebel Nexo w ręce."));
      return;
    }

    String nexoId = plugin.getNexoManager().idFromItem(hand);
    if (nexoId == null || nexoId.isBlank()) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>Przedmiot w ręce nie wygląda na item Nexo."));
      return;
    }

    template.setNexoFurnitureId(nexoId);
    plugin.getNexoManager().setMapping(nexoId, template.getName());
    plugin.getMachineManager().save();
    player.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><white>Powiązano model Nexo <reset><yellow>"
                    + nexoId
                    + " <reset><white>z szablonem <reset><yellow>"
                    + template.getName()
                    + "<reset><white>."));
  }

  private void handleList(CommandSender sender) {
    if (!sender.hasPermission("vendingmachine.admin")) {
      noPermission(sender);
      return;
    }
    sender.sendMessage(plugin.getPrefix() + msg("list-header"));
    var templates = plugin.getMachineManager().getAllTemplates();
    if (templates.isEmpty()) {
      sender.sendMessage(
          plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><gray>Brak szablonów."));
      return;
    }
    int id = 1;
    for (MachineTemplate t : templates) {
      int placements = plugin.getPlacementManager().getPlacementCount(t.getName());
      sender.sendMessage(
          plugin.getPrefix()
              + msg("list-entry")
                  .replace("{id}", String.valueOf(id))
                  .replace("{name}", t.getName())
                  .replace("{rows}", String.valueOf(t.getRows()))
                  .replace("{items}", String.valueOf(t.getItems().size()))
                  .replace("{placements}", String.valueOf(placements)));
      id++;
    }
  }

  private void handleReload(CommandSender sender) {
    if (!sender.hasPermission("vendingmachine.admin")) {
      noPermission(sender);
      return;
    }
    plugin.getDataManager().flushQueuedSaves();
    plugin.reloadConfig();
    if (plugin.getNexoManager() != null) plugin.getNexoManager().loadMappings();
    plugin.getRestockManager().stopAll();
    plugin.getRestockManager().startAll();
    sender.sendMessage(plugin.getPrefix() + msg("reload"));
  }

  private void handleInfo(CommandSender sender, String[] args) {
    if (!sender.hasPermission("vendingmachine.admin")) {
      noPermission(sender);
      return;
    }
    if (args.length < 2) {
      sender.sendMessage(
          plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>/automat info <nazwa>"));
      return;
    }
    MachineTemplate t = plugin.getMachineManager().getTemplate(args[1]);
    if (t == null) {
      sender.sendMessage(plugin.getPrefix() + msg("not-found").replace("{name}", args[1]));
      return;
    }
    int placements = plugin.getPlacementManager().getPlacementCount(t.getName());
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy("<reset><gold><bold>--- Info: " + t.getName() + " ---"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy("<reset><gray>Tytuł: <reset><white>" + t.getTitle()));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy("<reset><gray>Rzędy: <reset><white>" + t.getRows()));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><gray>Włączony: "
                    + (t.isEnabled() ? "<reset><green>Tak" : "<reset><red>Nie")));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><gray>Nexo ID: <reset><white>"
                    + (t.getNexoFurnitureId() != null ? t.getNexoFurnitureId() : "Brak")));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><gray>Przedmioty: <reset><white>" + t.getItems().size()));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy("<reset><gray>Aktywne automaty: <reset><white>" + placements));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><gray>Auto-restock: "
                    + (t.isAutoRestockEnabled()
                        ? "<reset><green>Co "
                            + t.getAutoRestockInterval()
                            + " min (+"
                            + t.getAutoRestockAmount()
                            + ")"
                        : "<reset><red>Wył")));
  }

  private void handleTeleport(CommandSender sender, String[] args) {
    if (!(sender instanceof Player p)) {
      sender.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy("<reset><red>Tylko gracze mogą użyć tej komendy."));
      return;
    }
    if (!p.hasPermission("vendingmachine.admin")) {
      noPermission(p);
      return;
    }
    if (args.length < 2) {
      p.sendMessage(plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>/automat tp <nazwa>"));
      return;
    }
    var placements =
        plugin.getPlacementManager().getAllPlacements().stream()
            .filter(pl -> pl.getTemplateName().equalsIgnoreCase(args[1]))
            .collect(Collectors.toList());
    if (placements.isEmpty()) {
      p.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  "<reset><red>Brak instancji szablonu <reset><yellow>" + args[1]));
      return;
    }
    p.teleport(placements.get(0).getLocation().clone().add(0.5, 1, 0.5));
    p.sendMessage(
        plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><white>Teleportowano do automatu."));
  }

  private void sendHelp(CommandSender sender, String label) {
    sender.sendMessage(plugin.getPrefix() + ColorUtil.miniToLegacy(msg("help-header")));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/" + label + " create <nazwa> <reset><gray>- Utwórz szablon"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/" + label + " delete <nazwa> <reset><gray>- Usuń szablon"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/" + label + " edit <nazwa> <reset><gray>- Edytuj szablon"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/" + label + " open <nazwa> <reset><gray>- Otwórz podgląd"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/"
                    + label
                    + " remove <reset><gray>- Usuń automat (patrz na blok)"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/"
                    + label
                    + " stock info <szablon> <reset><gray>- Info o defaultach"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/"
                    + label
                    + " stock fill <reset><gray>- Wypełnij automat (patrz na blok)"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/"
                    + label
                    + " restock <szablon> <reset><gray>- Restock wszystkich automatów"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/" + label + " restocklist <reset><gray>- Lista do uzupełnienia"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/"
                    + label
                    + " setmodel <szablon> <reset><gray>- Powiąż item Nexo z szablonem"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/" + label + " info <nazwa> <reset><gray>- Informacje"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/" + label + " list <reset><gray>- Lista szablonów"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/" + label + " tp <nazwa> <reset><gray>- Teleport do automatu"));
    sender.sendMessage(
        plugin.getPrefix()
            + ColorUtil.miniToLegacy(
                "<reset><yellow>/" + label + " reload <reset><gray>- Przeładuj"));
  }

  private void noPermission(CommandSender s) {
    s.sendMessage(plugin.getPrefix() + msg("no-permission"));
  }

  private String msg(String key) {
    return ColorUtil.miniToLegacy(
        plugin.getConfig().getString("messages." + key, "<reset><red>" + key));
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return filterByPermission(sender, SUBCOMMANDS).stream()
          .filter(s -> s.startsWith(args[0].toLowerCase()))
          .sorted()
          .collect(Collectors.toList());
    }
    if (args.length == 2) {
      String sub = args[0].toLowerCase();
      switch (sub) {
        case "delete", "edit", "open", "info", "tp", "restock", "setmodel" -> {
          return plugin.getMachineManager().getTemplateNames().stream()
              .filter(n -> n.startsWith(args[1].toLowerCase()))
              .sorted()
              .collect(Collectors.toList());
        }
        case "stock" -> {
          return Stream2.of("info", "fill")
              .filter(s -> s.startsWith(args[1].toLowerCase()))
              .collect(Collectors.toList());
        }
      }
    }
    if (args.length == 3 && args[0].equalsIgnoreCase("stock") && args[1].equalsIgnoreCase("info")) {
      return plugin.getMachineManager().getTemplateNames().stream()
          .filter(n -> n.startsWith(args[2].toLowerCase()))
          .sorted()
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private List<String> filterByPermission(CommandSender sender, List<String> subs) {
    List<String> result = new ArrayList<>();
    for (String sub : subs) {
      if (hasPermissionForSub(sender, sub)) result.add(sub);
    }
    return result;
  }

  private boolean hasPermissionForSub(CommandSender sender, String sub) {
    return switch (sub) {
      case "create" -> sender.hasPermission("vendingmachine.create");
      case "delete", "remove" -> sender.hasPermission("vendingmachine.delete");
      case "edit", "setmodel" ->
          sender.hasPermission("vendingmachine.edit")
              || sender.hasPermission("vendingmachine.admin");
      case "open" -> sender.hasPermission("vendingmachine.use");
      case "restocklist" ->
          sender.hasPermission("vendingmachine.restock.notify")
              || sender.hasPermission("vendingmachine.admin");
      case "list", "reload", "info", "tp", "stock", "restock" ->
          sender.hasPermission("vendingmachine.admin");
      case "help" -> true;
      default -> sender.hasPermission("vendingmachine.use");
    };
  }

  private static class Stream2 {
    static java.util.stream.Stream<String> of(String... values) {
      return Arrays.stream(values);
    }
  }
}
