package pl.skyrise.skyRiseCore.features.butelkomat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.core.VaultHook;
import pl.skyrise.skyRiseCore.utils.ColorUtil;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class ButelkomatModule implements Module {

  private final JavaPlugin plugin;
  private final TabRegistry tabRegistry;
  private CustomConfig config;
  private org.bukkit.event.Listener listener;

  private final List<AcceptedItem> acceptedItems = new ArrayList<>();
  private List<String> furnitureIds = new ArrayList<>();
  private boolean returnAll;
  private long cooldownMs;

  public ButelkomatModule(JavaPlugin plugin, TabRegistry tabRegistry) {
    this.plugin = plugin;
    this.tabRegistry = tabRegistry;
  }

  @Override
  public String getName() {
    return "Butelkomat";
  }

  @Override
  public void onEnable() {
    if (VaultHook.getEconomy() == null) {
      plugin
          .getLogger()
          .warning(
              "Vault/Economy nie znaleziony. Komendy Butelkomatu są dostępne, ale zwrot butelek"
                  + " będzie wyłączony.");
    }

    config = new CustomConfig(plugin, "butelkomat.yml");
    config.load();

    if (!config.getConfig().contains("messages")) {
      config.getConfig().set("butelkomat-furniture-ids", List.of("butelkomat"));
      config.getConfig().set("settings.return-all-in-hand", true);
      config.getConfig().set("settings.interact-cooldown-ms", 500);
      config.getConfig().set("settings.sound-volume", 0.7);
      config.getConfig().set("settings.sound-pitch", 1.0);
      config.getConfig().set("sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP");
      config.getConfig().set("sounds.error", "ENTITY_VILLAGER_NO");
      config
          .getConfig()
          .set("messages.prefix", "<bold><#459df5>Butelkomat</#459df5></bold> <gray>»</gray> ");
      config
          .getConfig()
          .set(
              "messages.no-permission",
              "{prefix}<red>Brak uprawnień do korzystania z butelkomatu.</red>");
      config
          .getConfig()
          .set(
              "messages.not-accepted",
              "{prefix}<red>Ten przedmiot nie jest przyjmowany przez butelkomat!</red>");
      config
          .getConfig()
          .set("messages.only-player", "{prefix}<red>Tylko gracz może tego użyć.</red>");
      config
          .getConfig()
          .set("messages.invalid-price", "{prefix}<red>Podana cena jest nieprawidłowa!</red>");
      config
          .getConfig()
          .set("messages.empty-hand", "{prefix}<red>Musisz trzymać przedmiot w ręce!</red>");
      config
          .getConfig()
          .set("messages.invalid-index", "{prefix}<red>Podany index jest nieprawidłowy!</red>");
      config
          .getConfig()
          .set(
              "messages.not-found",
              "{prefix}<red>Nie znaleziono przedmiotu o takim indeksie.</red>");
      config
          .getConfig()
          .set(
              "messages.not-nexo",
              "{prefix}<red>Przedmiot w Twojej ręce nie pochodzi z pluginu Nexo!</red>");
      config
          .getConfig()
          .set(
              "messages.nexo-error",
              "{prefix}<red>Błąd API Nexo. Upewnij się, że plugin jest aktualny.</red>");
      config
          .getConfig()
          .set("messages.cmd-disabled", "{prefix}<red>Ta komenda jest obecnie wyłączona.</red>");
      config.getConfig().set("messages.cmd-usage", "{prefix}<#459df5>Użycie: {0}</#459df5>");
      config
          .getConfig()
          .set(
              "messages.item-added",
              "{prefix}<white>Dodano przedmiot do butelkomatu! Cena:"
                  + " <yellow>{0}$</yellow></white>");
      config
          .getConfig()
          .set(
              "messages.item-removed",
              "{prefix}<white>Pomyślnie usunięto przedmiot <yellow>#{0}</yellow> z bazy.</white>");
      config
          .getConfig()
          .set("messages.list-header", "{prefix}<white>Lista akceptowanych przedmiotów:</white>");
      config
          .getConfig()
          .set("messages.list-empty", "  <gray>Brak akceptowanych przedmiotów.</gray>");
      config
          .getConfig()
          .set(
              "messages.list-format",
              "  <yellow>#{0}</yellow> <gray>-</gray> <white>{1}</white> <gray>-</gray> Cena:"
                  + " <yellow>{2}$</yellow>");
      config
          .getConfig()
          .set(
              "messages.model-set",
              "{prefix}<white>Pomyślnie powiązano! Każdy postawiony model <yellow>{0}</yellow>"
                  + " będzie działał jako maszyna.</white>");
      config
          .getConfig()
          .set(
              "messages.reloaded",
              "{prefix}<white>Pomyślnie przeładowano pliki konfiguracyjne!</white>");
      config
          .getConfig()
          .set(
              "messages.return-success",
              "{prefix}<white>Oddano <yellow>{0}</yellow> szt. Otrzymano:"
                  + " <yellow>{1}$</yellow></white>");
      config
          .getConfig()
          .set(
              "messages.machine-placed",
              "{prefix}<white>Pomyślnie postawiono nową maszynę Butelkomatu!</white>");
      config
          .getConfig()
          .set(
              "messages.admin-click-info",
              "{prefix}<white>Kliknięto mebel Nexo o ID: <yellow>{0}</yellow></white>");
      config.save();
    }

    loadConfig();

    var cmd = new ButelkomatCommand(this);
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindExecutor(plugin, cmd, "butelkomat");

    tabRegistry.register(
        "butelkomat",
        (sender, args) -> {
          if (args.length == 1) {
            return pl.skyrise.skyRiseCore.core.TabRegistry.filter(
                List.of("additem", "removeitem", "list", "setmodel", "reload"), args[0]);
          }
          return List.of();
        });

    this.listener =
        pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(
            plugin, new ButelkomatListener(this));
  }

  @Override
  public void onDisable() {
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindDisabled(plugin, getName(), "butelkomat");
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterTabs(tabRegistry, "butelkomat");
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListener(this.listener);
    this.listener = null;
    if (config != null) saveItems();
  }

  @Override
  public void onReload() {
    config.reload();
    loadConfig();
  }

  private void loadConfig() {
    acceptedItems.clear();
    furnitureIds = config.getConfig().getStringList("butelkomat-furniture-ids");
    returnAll = config.getConfig().getBoolean("settings.return-all-in-hand", true);
    cooldownMs = config.getConfig().getLong("settings.interact-cooldown-ms", 500);

    List<Map<?, ?>> itemsList = config.getConfig().getMapList("items");
    for (Map<?, ?> map : itemsList) {
      try {
        String data = (String) map.get("item-data");
        if (data == null) continue;

        ItemStack stack = deserializeItem(data);
        if (stack == null) continue;

        double price = 0;
        Object priceObj = map.get("price");
        if (priceObj instanceof Number num) price = num.doubleValue();

        boolean checkName = getBool(map, "check-name", true);
        boolean checkLore = getBool(map, "check-lore", true);
        boolean checkNbt = getBool(map, "check-nbt", true);
        boolean strict = getBool(map, "strict", false);

        acceptedItems.add(new AcceptedItem(stack, price, checkName, checkLore, checkNbt, strict));
      } catch (Exception e) {
        plugin.getLogger().warning("Błąd ładowania przedmiotu butelkomatu: " + e.getMessage());
      }
    }
  }

  public void saveItems() {
    List<Map<String, Object>> itemsList = new ArrayList<>();
    for (AcceptedItem item : acceptedItems) {
      Map<String, Object> map = new java.util.LinkedHashMap<>();
      map.put("item-data", serializeItem(item.itemStack));
      map.put("price", item.price);
      map.put("check-name", item.checkName);
      map.put("check-lore", item.checkLore);
      map.put("check-nbt", item.checkNbt);
      map.put("strict", item.strict);
      itemsList.add(map);
    }
    config.getConfig().set("items", itemsList);
    config.save();
  }

  public void sendMsg(CommandSender sender, String key, Object... args) {
    String msg = config.getConfig().getString("messages." + key);
    if (msg == null || msg.isEmpty()) return;

    String prefix =
        config
            .getConfig()
            .getString(
                "messages.prefix", "<bold><#459df5>Butelkomat</#459df5></bold> <gray>»</gray> ");
    msg = msg.replace("{prefix}", prefix);

    for (int i = 0; i < args.length; i++) {
      msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
    }

    sender.sendMessage(ColorUtil.mini(msg));
  }

  public boolean isButelkomat(String furnitureId) {
    if (furnitureId == null) return false;
    for (String id : furnitureIds) {
      if (id.equalsIgnoreCase(furnitureId)) return true;
    }
    return false;
  }

  public void addFurnitureId(String id) {
    if (!furnitureIds.contains(id)) {
      furnitureIds.add(id);
      config.getConfig().set("butelkomat-furniture-ids", furnitureIds);
      config.save();
    }
  }

  public AcceptedItem findMatch(ItemStack playerItem) {
    if (playerItem == null) return null;
    for (AcceptedItem accepted : acceptedItems) {
      if (matches(accepted, playerItem)) return accepted;
    }
    return null;
  }

  public void processReturn(Player player, ItemStack handItem) {
    if (VaultHook.getEconomy() == null) {
      player.sendMessage(ColorUtil.mini("<red>Ekonomia serwera jest obecnie niedostępna.</red>"));
      return;
    }

    AcceptedItem match = findMatch(handItem);
    if (match == null) {
      sendMsg(player, "not-accepted");
      playSound(player, "error");
      return;
    }

    int amount = returnAll ? handItem.getAmount() : 1;
    double total = match.price * amount;

    if (returnAll) {
      player.getInventory().setItemInMainHand(null);
    } else {
      if (handItem.getAmount() > 1) {
        handItem.setAmount(handItem.getAmount() - 1);
      } else {
        player.getInventory().setItemInMainHand(null);
      }
    }

    VaultHook.getEconomy().depositPlayer(player, total);

    sendMsg(player, "return-success", amount, String.format("%.2f", total));
    playSound(player, "success");
  }

  public void addItem(ItemStack itemStack, double price) {
    acceptedItems.add(new AcceptedItem(itemStack.clone(), price, true, true, true, false));
    saveItems();
  }

  public boolean removeItem(int index) {
    if (index < 0 || index >= acceptedItems.size()) return false;
    acceptedItems.remove(index);
    saveItems();
    return true;
  }

  public List<AcceptedItem> getAcceptedItems() {
    return acceptedItems;
  }

  public long getCooldownMs() {
    return cooldownMs;
  }

  private boolean matches(AcceptedItem accepted, ItemStack other) {
    ItemStack template = accepted.itemStack;
    if (template.getType() != other.getType()) return false;
    if (accepted.strict) return template.isSimilar(other);

    var tMeta = template.getItemMeta();
    var oMeta = other.getItemMeta();

    if (tMeta == null && oMeta == null) return true;
    if (tMeta == null || oMeta == null) return false;

    if (accepted.checkName) {
      if (tMeta.hasDisplayName() != oMeta.hasDisplayName()) return false;
      if (tMeta.hasDisplayName() && !tMeta.displayName().equals(oMeta.displayName())) return false;
    }

    if (accepted.checkLore) {
      if (tMeta.hasLore() != oMeta.hasLore()) return false;
      if (tMeta.hasLore() && !tMeta.lore().equals(oMeta.lore())) return false;
    }

    if (accepted.checkNbt) {
      if (!tMeta.getPersistentDataContainer().equals(oMeta.getPersistentDataContainer()))
        return false;
    }

    return true;
  }

  public void playSound(Player player, String soundType) {
    try {
      String soundName =
          config.getConfig().getString("sounds." + soundType, "ENTITY_EXPERIENCE_ORB_PICKUP");
      Sound sound = Sound.valueOf(soundName);
      float volume = (float) config.getConfig().getDouble("settings.sound-volume", 0.7);
      float pitch = (float) config.getConfig().getDouble("settings.sound-pitch", 1.0);
      player.playSound(player.getLocation(), sound, volume, pitch);
    } catch (Exception ignored) {
    }
  }

  private boolean getBool(Map<?, ?> map, String key, boolean def) {
    Object val = map.get(key);
    if (val instanceof Boolean b) return b;
    return def;
  }

  public static String serializeItem(ItemStack item) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(out);
      dataOut.writeObject(item);
      dataOut.close();
      return Base64.getEncoder().encodeToString(out.toByteArray());
    } catch (Exception e) {
      return null;
    }
  }

  public static ItemStack deserializeItem(String data) {
    try {
      ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(data));
      BukkitObjectInputStream dataIn = new BukkitObjectInputStream(in);
      ItemStack item = (ItemStack) dataIn.readObject();
      dataIn.close();
      return item;
    } catch (Exception e) {
      return null;
    }
  }

  public static class AcceptedItem {
    public final ItemStack itemStack;
    public final double price;
    public final boolean checkName;
    public final boolean checkLore;
    public final boolean checkNbt;
    public final boolean strict;

    public AcceptedItem(
        ItemStack itemStack,
        double price,
        boolean checkName,
        boolean checkLore,
        boolean checkNbt,
        boolean strict) {
      this.itemStack = itemStack.clone();
      this.price = price;
      this.checkName = checkName;
      this.checkLore = checkLore;
      this.checkNbt = checkNbt;
      this.strict = strict;
    }
  }
}
