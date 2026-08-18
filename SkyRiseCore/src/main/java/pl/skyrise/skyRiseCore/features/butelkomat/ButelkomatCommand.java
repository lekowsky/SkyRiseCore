package pl.skyrise.skyRiseCore.features.butelkomat;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class ButelkomatCommand implements CommandExecutor {

  private final ButelkomatModule module;

  public ButelkomatCommand(ButelkomatModule module) {
    this.module = module;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("butelkomat.admin")) {
      module.sendMsg(sender, "no-permission");
      return true;
    }

    if (args.length == 0) {
      sendHelp(sender);
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "additem" -> handleAddItem(sender, args);
      case "removeitem" -> handleRemoveItem(sender, args);
      case "list" -> handleList(sender);
      case "setmodel" -> handleSetModel(sender);
      case "reload" -> handleReload(sender);
      default -> sendHelp(sender);
    }

    return true;
  }

  private void handleAddItem(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      module.sendMsg(sender, "only-player");
      return;
    }

    if (args.length < 2) {
      module.sendMsg(sender, "cmd-usage", "/bm additem <cena>");
      return;
    }

    double price;
    try {
      price = Double.parseDouble(args[1]);
    } catch (NumberFormatException e) {
      module.sendMsg(sender, "invalid-price");
      return;
    }

    ItemStack hand = player.getInventory().getItemInMainHand();
    if (hand.getType() == Material.AIR) {
      module.sendMsg(sender, "empty-hand");
      return;
    }

    module.addItem(hand, price);
    module.sendMsg(sender, "item-added", price);
  }

  private void handleRemoveItem(CommandSender sender, String[] args) {
    if (args.length < 2) {
      module.sendMsg(sender, "cmd-usage", "/bm removeitem <index>");
      return;
    }

    int index;
    try {
      index = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      module.sendMsg(sender, "invalid-index");
      return;
    }

    if (module.removeItem(index)) {
      module.sendMsg(sender, "item-removed", index);
    } else {
      module.sendMsg(sender, "not-found");
    }
  }

  private void handleList(CommandSender sender) {
    module.sendMsg(sender, "list-header");
    var items = module.getAcceptedItems();
    if (items.isEmpty()) {
      module.sendMsg(sender, "list-empty");
      return;
    }

    for (int i = 0; i < items.size(); i++) {
      var item = items.get(i);
      module.sendMsg(sender, "list-format", i, item.itemStack.getType().name(), item.price);
    }
  }

  private void handleSetModel(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      module.sendMsg(sender, "only-player");
      return;
    }

    ItemStack hand = player.getInventory().getItemInMainHand();
    if (hand.getType() == Material.AIR) {
      module.sendMsg(sender, "empty-hand");
      return;
    }

    try {
      String nexoId = com.nexomc.nexo.api.NexoItems.idFromItem(hand);
      if (nexoId == null || nexoId.isEmpty()) {
        module.sendMsg(sender, "not-nexo");
        return;
      }

      module.addFurnitureId(nexoId);
      module.sendMsg(sender, "model-set", nexoId);

    } catch (Throwable t) {
      module.sendMsg(sender, "nexo-error");
    }
  }

  private void handleReload(CommandSender sender) {
    module.onReload();
    module.sendMsg(sender, "reloaded");
  }

  private void sendHelp(CommandSender sender) {
    sender.sendMessage(
        ColorUtil.mini(
            "<gray><strikethrough>                              </strikethrough></gray>"));
    sender.sendMessage(
        ColorUtil.mini(
            "<bold><#459df5>Butelkomat</#459df5></bold> <gray>-</gray> <yellow>Pomoc</yellow>"));
    sender.sendMessage(
        ColorUtil.mini(
            "<gray><strikethrough>                              </strikethrough></gray>"));
    sender.sendMessage(
        ColorUtil.mini(
            "  <yellow>/bm list              <gray>-</gray> <#459df5>Lista przedmiotów</#459df5>"));
    sender.sendMessage(
        ColorUtil.mini(
            "  <yellow>/bm additem <cena>    <gray>-</gray> <#459df5>Dodaj przedmiot z"
                + " ręki</#459df5>"));
    sender.sendMessage(
        ColorUtil.mini(
            "  <yellow>/bm removeitem <id>   <gray>-</gray> <#459df5>Usuń przedmiot z"
                + " bazy</#459df5>"));
    sender.sendMessage(
        ColorUtil.mini(
            "  <yellow>/bm setmodel          <gray>-</gray> <#459df5>Ustaw trzymany mebel Nexo jako"
                + " maszynę</#459df5>"));
    sender.sendMessage(
        ColorUtil.mini(
            "  <yellow>/bm reload            <gray>-</gray> <#459df5>Przeładuj moduł</#459df5>"));
    sender.sendMessage(
        ColorUtil.mini(
            "<gray><strikethrough>                              </strikethrough></gray>"));
  }
}
