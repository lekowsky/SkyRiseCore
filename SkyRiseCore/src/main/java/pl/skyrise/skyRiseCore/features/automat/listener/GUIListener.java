package pl.skyrise.skyRiseCore.features.automat.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.gui.EditorGUI;
import pl.skyrise.skyRiseCore.features.automat.gui.ItemEditorGUI;
import pl.skyrise.skyRiseCore.features.automat.gui.MachineGUI;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public class GUIListener implements Listener {

  private final AutomatModule plugin;

  public GUIListener(AutomatModule plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (event.getInventory().getHolder() instanceof MachineGUI
        || event.getInventory().getHolder() instanceof EditorGUI
        || event.getInventory().getHolder() instanceof ItemEditorGUI) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    if (event.getClickedInventory() == null) return;

    if (event.getInventory().getHolder() instanceof MachineGUI gui) {
      event.setCancelled(true);
      if (event.getClickedInventory() != event.getInventory()) return;
      handlePurchase(player, gui, event.getSlot());
    } else if (event.getInventory().getHolder() instanceof EditorGUI gui) {
      event.setCancelled(true);
      if (event.getClickedInventory() != event.getInventory()) return;
      handleEditor(player, gui, event.getSlot(), event.getClick());
    } else if (event.getInventory().getHolder() instanceof ItemEditorGUI gui) {
      event.setCancelled(true);
      if (event.getClickedInventory() != event.getInventory()) return;
      handleItemEditor(player, gui, event.getSlot(), event.getClick());
    }
  }

  private void handlePurchase(Player player, MachineGUI gui, int slot) {
    MachineTemplate t = gui.getTemplate();
    MachinePlacement placement = gui.getPlacement();
    String curr = plugin.getEconomyManager().getCurrencySymbol();

    if (t.isCloseButtonEnabled() && slot == t.getResolvedCloseSlot()) {
      player.closeInventory();
      playSound(player, "open");
      return;
    }

    VendingItem vendingItem = t.getItemBySlot(slot);
    if (vendingItem == null) return;

    if (placement == null) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  "<reset><white>Tryb podglądu — zakup jest możliwy tylko z postawionego"
                      + " automatu."));
      playSound(player, "fail");
      return;
    }

    if (!vendingItem.getPermission().isEmpty()
        && !player.hasPermission(vendingItem.getPermission())) {
      player.sendMessage(plugin.getPrefix() + msg("no-permission"));
      playSound(player, "fail");
      return;
    }
    if (!vendingItem.canPurchase(player.getUniqueId())) {
      player.sendMessage(plugin.getPrefix() + msg("limit-reached"));
      playSound(player, "fail");
      return;
    }

    boolean hasStock =
        vendingItem.isUnlimitedStock()
            || placement.getStock(vendingItem.getId()) >= vendingItem.getAmount();
    if (!hasStock) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  plugin
                      .getConfig()
                      .getString("messages.sold-out", "<reset><red>Ten produkt jest wyprzedany!")));
      playSound(player, "fail");
      plugin.getRestockManager().notifyItemSoldOut(placement, vendingItem);
      return;
    }

    ItemStack purchaseItem = vendingItem.buildPurchaseItem();
    if (purchaseItem == null
        || purchaseItem.getType() == Material.AIR
        || purchaseItem.getAmount() <= 0) {
      player.sendMessage(
          plugin.getPrefix()
              + ColorUtil.miniToLegacy(
                  "<reset><red>Ten produkt jest nieprawidłowo skonfigurowany."));
      playSound(player, "fail");
      return;
    }

    double price = vendingItem.getPrice();
    if (!plugin.getEconomyManager().hasEnough(player, price)) {
      player.sendMessage(
          plugin.getPrefix()
              + msg("no-money")
                  .replace("{price}", String.format("%.2f", price))
                  .replace("{currency}", curr));
      playSound(player, "fail");
      return;
    }

    if (!canFit(player, purchaseItem)) {
      player.sendMessage(plugin.getPrefix() + msg("no-space"));
      playSound(player, "fail");
      return;
    }

    boolean stockWithdrawn = false;
    if (!vendingItem.isUnlimitedStock()) {
      if (!placement.withdrawStock(vendingItem.getId(), vendingItem.getAmount())) {
        player.sendMessage(
            plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>Brak produktu w magazynie."));
        playSound(player, "fail");
        return;
      }
      stockWithdrawn = true;
    }

    if (!plugin.getEconomyManager().withdraw(player, price)) {
      if (stockWithdrawn) {
        placement.addStock(vendingItem.getId(), vendingItem.getAmount(), vendingItem.getMaxStock());
      }
      player.sendMessage(
          plugin.getPrefix()
              + msg("no-money")
                  .replace("{price}", String.format("%.2f", price))
                  .replace("{currency}", curr));
      playSound(player, "fail");
      return;
    }

    var leftovers = player.getInventory().addItem(purchaseItem.clone());
    if (!leftovers.isEmpty()) {

      leftovers
          .values()
          .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
      plugin
          .getLogger()
          .warning(
              "Automat: inventory overflow after canFit() for player "
                  + player.getName()
                  + ", leftovers dropped naturally.");
    }
    player.updateInventory();
    vendingItem.recordPurchase(player.getUniqueId());

    if (stockWithdrawn) {
      plugin.getDataManager().queueSavePlacements();
    }

    for (String cmd : vendingItem.getCommandsOnPurchase()) {
      try {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            cmd.replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(vendingItem.getAmount())));
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning("Automat: command-on-purchase failed: " + cmd + " (" + e.getMessage() + ")");
      }
    }

    player.sendMessage(
        plugin.getPrefix()
            + msg("purchase-success")
                .replace("{item}", ColorUtil.miniPlain(vendingItem.getDisplayName()))
                .replace("{price}", String.format("%.2f", price))
                .replace("{currency}", curr));
    playSound(player, "success");

    if (!vendingItem.isUnlimitedStock()) {
      int newStock = placement.getStock(vendingItem.getId());
      int threshold = plugin.getConfig().getInt("stock.low-stock-warning.threshold", 5);

      if (newStock == 0) {
        plugin.getRestockManager().notifyItemSoldOut(placement, vendingItem);
      } else if (newStock <= threshold) {
        plugin.getRestockManager().notifyItemLowStock(placement, vendingItem, newStock);
      }
    }

    if (plugin.getConfig().getBoolean("particles.enabled", true)) {
      try {
        player
            .getWorld()
            .spawnParticle(
                Particle.valueOf(plugin.getConfig().getString("particles.type", "HAPPY_VILLAGER")),
                player.getLocation().add(0, 1, 0),
                plugin.getConfig().getInt("particles.count", 10),
                0.5,
                0.5,
                0.5,
                0);
      } catch (Exception ignored) {
      }
    }

    new MachineGUI(plugin, t, player, placement).open();
  }

  private void handleEditor(Player player, EditorGUI gui, int slot, ClickType click) {
    MachineTemplate t = gui.getTemplate();
    playSound(player, "open");

    switch (gui.getMode()) {
      case MAIN_MENU -> {
        switch (slot) {
          case 19 -> {
            player.closeInventory();
            ChatInputListener.requestInput(
                player,
                "<reset><yellow>Wpisz nowy tytuł:",
                input -> {
                  t.setTitle(input);
                  plugin.getMachineManager().save();
                  new EditorGUI(plugin, t, player).open();
                });
          }
          case 20 -> {
            t.setRows(t.getRows() + (click == ClickType.LEFT ? 1 : -1));
            plugin.getMachineManager().save();
            gui.openMainMenu();
          }
          case 21 -> gui.openItemsList();
          case 22 -> gui.openGUISettings();
          case 23 -> {
            t.setEnabled(!t.isEnabled());
            plugin.getMachineManager().save();
            gui.openMainMenu();
          }
          case 24 -> {
            if (click == ClickType.RIGHT) {
              t.setPermission("");
              plugin.getMachineManager().save();
              gui.openMainMenu();
            } else {
              player.closeInventory();
              ChatInputListener.requestInput(
                  player,
                  "<reset><yellow>Wpisz uprawnienie:",
                  input -> {
                    t.setPermission(input);
                    plugin.getMachineManager().save();
                    new EditorGUI(plugin, t, player).open();
                  });
            }
          }
          case 25 -> {
            if (click == ClickType.RIGHT) {
              t.setNexoFurnitureId(null);
              plugin.getMachineManager().save();
              gui.openMainMenu();
            } else {
              player.closeInventory();
              ChatInputListener.requestInput(
                  player,
                  "<reset><yellow>Wpisz Nexo furniture ID:",
                  input -> {
                    t.setNexoFurnitureId(input);
                    plugin.getMachineManager().save();
                    new EditorGUI(plugin, t, player).open();
                  });
            }
          }

          case 28 -> {
            player.closeInventory();
            player.sendMessage(
                plugin.getPrefix()
                    + ColorUtil.miniToLegacy(
                        "<reset><white>Aby postawić automat, użyj powiązanego mebla Nexo."));
            player.sendMessage(
                plugin.getPrefix()
                    + ColorUtil.miniToLegacy(
                        "<reset><white>Powiąż model przez <reset><yellow>/automat setmodel "
                            + t.getName()
                            + " <reset><white>lub sekcję"
                            + " <reset><yellow>nexo.furniture-mappings<reset><white>."));
          }
          case 29 -> {
            t.setCloseButtonEnabled(!t.isCloseButtonEnabled());
            plugin.getMachineManager().save();
            gui.openMainMenu();
          }
          case 30 -> new MachineGUI(plugin, t, player, null).open();
          case 32 -> {
            t.setAutoRestockEnabled(!t.isAutoRestockEnabled());
            plugin.getMachineManager().save();
            plugin.getRestockManager().restartRestock(t);
            gui.openMainMenu();
          }
          case 33 -> {
            if (click == ClickType.SHIFT_LEFT) {
              player.closeInventory();
              ChatInputListener.requestInput(
                  player,
                  "<reset><yellow>Wpisz interwał w minutach:",
                  input -> {
                    try {
                      t.setAutoRestockInterval(Integer.parseInt(input));
                      plugin.getMachineManager().save();
                      plugin.getRestockManager().restartRestock(t);
                    } catch (Exception e) {
                      player.sendMessage(plugin.getPrefix() + msg("invalid"));
                    }
                    new EditorGUI(plugin, t, player).open();
                  });
            } else {
              t.setAutoRestockInterval(
                  t.getAutoRestockInterval() + (click == ClickType.LEFT ? 5 : -5));
              plugin.getMachineManager().save();
              plugin.getRestockManager().restartRestock(t);
              gui.openMainMenu();
            }
          }
          case 34 -> {
            if (click == ClickType.SHIFT_LEFT) {
              player.closeInventory();
              ChatInputListener.requestInput(
                  player,
                  "<reset><yellow>Wpisz ilość restocku:",
                  input -> {
                    try {
                      t.setAutoRestockAmount(Integer.parseInt(input));
                      plugin.getMachineManager().save();
                    } catch (Exception e) {
                      player.sendMessage(plugin.getPrefix() + msg("invalid"));
                    }
                    new EditorGUI(plugin, t, player).open();
                  });
            } else {
              t.setAutoRestockAmount(t.getAutoRestockAmount() + (click == ClickType.LEFT ? 5 : -5));
              plugin.getMachineManager().save();
              gui.openMainMenu();
            }
          }
          case 49 -> player.closeInventory();
        }
      }
      case ITEMS_LIST -> {
        if (slot == 45) {
          gui.openMainMenu();
          return;
        }
        if (slot == 48) {
          if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            player.sendMessage(
                plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>Trzymaj przedmiot!"));
            return;
          }
          player.closeInventory();
          ChatInputListener.requestInput(
              player,
              "<reset><yellow>Wpisz cenę:",
              input -> {
                try {
                  double price = Double.parseDouble(input);
                  VendingItem newItem =
                      plugin.getMachineManager().createItemFromHand(player, t, price);
                  if (newItem != null) {
                    t.addItem(newItem);
                    plugin.getMachineManager().save();
                    player.sendMessage(plugin.getPrefix() + msg("item-added"));
                  } else {
                    player.sendMessage(
                        plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>Brak slotów!"));
                  }
                } catch (NumberFormatException e) {
                  player.sendMessage(plugin.getPrefix() + msg("invalid"));
                }
                new EditorGUI(plugin, t, player).openItemsList();
              });
          return;
        }
        if (slot == 50) {
          VendingItem ni = new VendingItem(plugin.getMachineManager().generateItemId(t));
          int free = t.getNextFreeSlot();
          if (free == -1) {
            player.sendMessage(
                plugin.getPrefix() + ColorUtil.miniToLegacy("<reset><red>Brak slotów!"));
            return;
          }
          ni.setSlot(free);
          ni.setPrice(10.0);
          ni.setDisplayName("<reset><white>Nowy przedmiot");
          t.addItem(ni);
          plugin.getMachineManager().save();
          gui.openItemsList();
          return;
        }
        if (slot < 45) {
          List<VendingItem> list = new ArrayList<>(t.getItems().values());
          if (slot < list.size()) {
            VendingItem ci = list.get(slot);
            if (click == ClickType.RIGHT) {
              t.removeItem(ci.getId());
              plugin.getMachineManager().save();
              gui.openItemsList();
            } else {
              new ItemEditorGUI(plugin, t, ci, player).open();
            }
          }
        }
      }
      case GUI_SETTINGS -> {
        switch (slot) {
          case 20 -> {
            t.setFillEmpty(!t.isFillEmpty());
            plugin.getMachineManager().save();
            gui.openGUISettings();
          }
          case 21 -> {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() != Material.AIR) {
              t.setFillerMaterial(hand.getType());
              plugin.getMachineManager().save();
            }
            gui.openGUISettings();
          }
          case 23 -> {
            t.setBorder(!t.isBorder());
            plugin.getMachineManager().save();
            gui.openGUISettings();
          }
          case 24 -> {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() != Material.AIR) {
              t.setBorderMaterial(hand.getType());
              plugin.getMachineManager().save();
            }
            gui.openGUISettings();
          }
          case 45 -> gui.openMainMenu();
        }
      }
    }
  }

  private void handleItemEditor(Player player, ItemEditorGUI gui, int slot, ClickType click) {
    MachineTemplate t = gui.getTemplate();
    VendingItem item = gui.getVendingItem();
    playSound(player, "open");

    switch (slot) {
      case 19 -> {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.AIR) {
          item.setMaterial(hand.getType());
          plugin.getMachineManager().save();
        }
        gui.open();
      }
      case 20 -> {
        player.closeInventory();
        ChatInputListener.requestInput(
            player,
            "<reset><yellow>Wpisz nową nazwę:",
            input -> {
              item.setDisplayName(input);
              plugin.getMachineManager().save();
              new ItemEditorGUI(plugin, t, item, player).open();
            });
      }
      case 21 -> {
        if (click == ClickType.RIGHT) {
          item.setLore(new ArrayList<>());
          plugin.getMachineManager().save();
          gui.open();
        } else {
          player.closeInventory();
          List<String> lines = new ArrayList<>();
          @SuppressWarnings("unchecked")
          Consumer<String>[] handler = new Consumer[1];
          handler[0] =
              input -> {
                if (input.equalsIgnoreCase("done")) {
                  item.setLore(lines);
                  plugin.getMachineManager().save();
                  new ItemEditorGUI(plugin, t, item, player).open();
                } else {
                  lines.add(input);
                  player.sendMessage(
                      ColorUtil.miniToLegacy(
                          "<reset><gray>Dodano: " + ColorUtil.miniToLegacy(input)));
                  ChatInputListener.requestInput(
                      player, "<reset><yellow>Następna linia ('done' = koniec):", handler[0]);
                }
              };
          ChatInputListener.requestInput(
              player, "<reset><yellow>Wpisz linię opisu ('done' = koniec):", handler[0]);
        }
      }
      case 22 -> {
        player.closeInventory();
        ChatInputListener.requestInput(
            player,
            "<reset><yellow>Wpisz cenę:",
            input -> {
              try {
                item.setPrice(Double.parseDouble(input));
                plugin.getMachineManager().save();
              } catch (Exception e) {
                player.sendMessage(plugin.getPrefix() + msg("invalid"));
              }
              new ItemEditorGUI(plugin, t, item, player).open();
            });
      }
      case 23 -> {
        int ch =
            switch (click) {
              case SHIFT_LEFT -> 10;
              case SHIFT_RIGHT -> -10;
              case RIGHT -> -1;
              default -> 1;
            };
        item.setAmount(item.getAmount() + ch);
        plugin.getMachineManager().save();
        gui.open();
      }
      case 24 -> {
        player.closeInventory();
        ChatInputListener.requestInput(
            player,
            "<reset><yellow>Wpisz numer slotu:",
            input -> {
              try {
                int s = Integer.parseInt(input);
                if (s >= 0 && s < t.getSize()) {
                  item.setSlot(s);
                  plugin.getMachineManager().save();
                }
              } catch (Exception e) {
                player.sendMessage(plugin.getPrefix() + msg("invalid"));
              }
              new ItemEditorGUI(plugin, t, item, player).open();
            });
      }
      case 25 -> {
        item.setGlowing(!item.isGlowing());
        plugin.getMachineManager().save();
        gui.open();
      }
      case 28 -> {
        player.closeInventory();
        ChatInputListener.requestInput(
            player,
            "<reset><yellow>Wpisz CMD (0 = brak):",
            input -> {
              try {
                item.setCustomModelData(Integer.parseInt(input));
                plugin.getMachineManager().save();
              } catch (Exception e) {
                player.sendMessage(plugin.getPrefix() + msg("invalid"));
              }
              new ItemEditorGUI(plugin, t, item, player).open();
            });
      }
      case 29 -> {
        if (click == ClickType.RIGHT) {
          item.setPermission("");
          plugin.getMachineManager().save();
          gui.open();
        } else {
          player.closeInventory();
          ChatInputListener.requestInput(
              player,
              "<reset><yellow>Wpisz uprawnienie:",
              input -> {
                item.setPermission(input);
                plugin.getMachineManager().save();
                new ItemEditorGUI(plugin, t, item, player).open();
              });
        }
      }
      case 30 -> {
        if (click == ClickType.SHIFT_LEFT) {
          player.closeInventory();
          ChatInputListener.requestInput(
              player,
              "<reset><yellow>Wpisz limit (0 = brak):",
              input -> {
                try {
                  item.setPurchaseLimit(Math.max(0, Integer.parseInt(input)));
                  plugin.getMachineManager().save();
                } catch (Exception e) {
                  player.sendMessage(plugin.getPrefix() + msg("invalid"));
                }
                new ItemEditorGUI(plugin, t, item, player).open();
              });
        } else {
          item.setPurchaseLimit(
              Math.max(0, item.getPurchaseLimit() + (click == ClickType.LEFT ? 1 : -1)));
          plugin.getMachineManager().save();
          gui.open();
        }
      }
      case 31 -> {
        if (click == ClickType.RIGHT) {
          item.setCommandsOnPurchase(new ArrayList<>());
          plugin.getMachineManager().save();
          gui.open();
        } else {
          player.closeInventory();
          ChatInputListener.requestInput(
              player,
              "<reset><yellow>Wpisz komendę (bez /):",
              input -> {
                item.getCommandsOnPurchase().add(input);
                plugin.getMachineManager().save();
                new ItemEditorGUI(plugin, t, item, player).open();
              });
        }
      }
      case 37 -> {
        item.setUnlimitedStock(!item.isUnlimitedStock());
        plugin.getMachineManager().save();
        gui.open();
      }
      case 38 -> {
        if (click == ClickType.SHIFT_LEFT) {
          player.closeInventory();
          ChatInputListener.requestInput(
              player,
              "<reset><yellow>Wpisz domyślną ilość stocku:",
              input -> {
                try {
                  item.setStock(Integer.parseInt(input));
                  plugin.getMachineManager().save();
                } catch (Exception e) {
                  player.sendMessage(plugin.getPrefix() + msg("invalid"));
                }
                new ItemEditorGUI(plugin, t, item, player).open();
              });
        } else {
          int change = click == ClickType.LEFT ? 10 : -10;
          item.setStock(Math.max(0, item.getStock() + change));
          plugin.getMachineManager().save();
          gui.open();
        }
      }
      case 39 -> {
        if (click == ClickType.SHIFT_LEFT) {
          player.closeInventory();
          ChatInputListener.requestInput(
              player,
              "<reset><yellow>Wpisz max stock:",
              input -> {
                try {
                  item.setMaxStock(Integer.parseInt(input));
                  plugin.getMachineManager().save();
                } catch (Exception e) {
                  player.sendMessage(plugin.getPrefix() + msg("invalid"));
                }
                new ItemEditorGUI(plugin, t, item, player).open();
              });
        } else {
          int change = click == ClickType.LEFT ? 10 : -10;
          item.setMaxStock(Math.max(1, item.getMaxStock() + change));
          plugin.getMachineManager().save();
          gui.open();
        }
      }
      case 45 -> new EditorGUI(plugin, t, player).openItemsList();
      case 49 -> {
        t.removeItem(item.getId());
        plugin.getMachineManager().save();
        new EditorGUI(plugin, t, player).openItemsList();
      }
    }
  }

  private boolean canFit(Player player, ItemStack item) {
    int remaining = item.getAmount();
    int maxStack = Math.max(1, item.getMaxStackSize());

    for (ItemStack content : player.getInventory().getStorageContents()) {
      if (content == null || content.getType() == Material.AIR) {
        remaining -= maxStack;
      } else if (content.isSimilar(item) && content.getAmount() < content.getMaxStackSize()) {
        remaining -= content.getMaxStackSize() - content.getAmount();
      }

      if (remaining <= 0) return true;
    }
    return false;
  }

  private void playSound(Player p, String key) {
    try {
      p.playSound(
          p.getLocation(),
          Sound.valueOf(plugin.getConfig().getString("sounds." + key, "UI_BUTTON_CLICK")),
          0.5f,
          1f);
    } catch (Exception ignored) {
    }
  }

  private String msg(String key) {
    return ColorUtil.miniToLegacy(
        plugin.getConfig().getString("messages." + key, "<reset><red>" + key));
  }
}
