package pl.skyrise.skyRiseCore.features.armorworld;

import java.util.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class ArmorWorldModule implements Module {

  private org.bukkit.event.Listener listener;

  private final JavaPlugin plugin;
  private final TabRegistry tabRegistry;
  private final MessageCache messageCache;
  private CustomConfig config;

  private final Set<String> blockedWorlds = new HashSet<>();
  private Set<Material> blockedMaterials;
  private String denyMessage;
  private String bypassPermission;

  private Map<Material, String> materialNames;
  private org.bukkit.scheduler.BukkitTask checkTask;

  public ArmorWorldModule(JavaPlugin plugin, TabRegistry tabRegistry, MessageCache messageCache) {
    this.plugin = plugin;
    this.tabRegistry = tabRegistry;
    this.messageCache = messageCache;
  }

  @Override
  public String getName() {
    return "ArmorWorld";
  }

  @Override
  public void onEnable() {
    config = new CustomConfig(plugin, "armorworld.yml");
    config.load();
    cacheConfig();

    this.listener =
        pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(
            plugin, new ArmorWorldListener(this, messageCache));

    ArmorWorldCommand command = new ArmorWorldCommand(this);
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindExecutor(plugin, command, "armorworld");

    tabRegistry.register(
        "armorworld",
        (sender, args) -> {
          if (args.length == 1) {
            return TabRegistry.filter(List.of("add", "remove", "list", "off", "items"), args[0]);
          }
          if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
              List<String> worlds = new ArrayList<>();
              plugin.getServer().getWorlds().forEach(w -> worlds.add(w.getName()));
              return TabRegistry.filter(worlds, args[1]);
            }
          }
          return List.of();
        });

    plugin
        .getLogger()
        .info(
            "  → ArmorWorld: "
                + blockedWorlds.size()
                + " światów, "
                + blockedMaterials.size()
                + " przedmiotów");

    startSafetyTask();
  }

  @Override
  public void onDisable() {
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindDisabled(plugin, getName(), "armorworld");
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListener(this.listener);
    this.listener = null;
    if (checkTask != null) {
      checkTask.cancel();
      checkTask = null;
    }
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterTabs(tabRegistry, "armorworld");
    config.save();
  }

  @Override
  public void onReload() {
    config.reload();
    cacheConfig();
    startSafetyTask();
  }

  private void cacheConfig() {
    blockedWorlds.clear();
    for (String world : config.getConfig().getStringList("blocked-worlds")) {
      if (world != null && !world.isBlank()) {
        blockedWorlds.add(world.toLowerCase(Locale.ROOT));
      }
    }

    Set<Material> materials = EnumSet.noneOf(Material.class);
    Map<Material, String> names = new EnumMap<>(Material.class);

    for (String item : config.getConfig().getStringList("blocked-items")) {
      try {
        Material mat = Material.valueOf(item.toUpperCase(Locale.ROOT));
        materials.add(mat);
        names.put(mat, prettify(item.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException ignored) {
      }
    }

    if (materials.isEmpty()) {
      for (Material mat :
          new Material[] {
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
          }) {
        materials.add(mat);
        names.put(mat, prettify(mat.name()));
      }
    }

    this.blockedMaterials = Collections.unmodifiableSet(materials);
    this.materialNames = Collections.unmodifiableMap(names);

    this.denyMessage =
        config.getConfig().getString("deny-message", "<red>» Zbroja jest zakazana w mieście.");
    this.bypassPermission =
        config.getConfig().getString("bypass-permission", "skyrise.armorworld.bypass");
  }

  private void startSafetyTask() {
    if (checkTask != null) {
      checkTask.cancel();
      checkTask = null;
    }
    if (blockedWorlds.isEmpty() || blockedMaterials.isEmpty()) return;

    long intervalTicks =
        Math.max(1L, config.getConfig().getLong("safety-check-interval-ticks", 10L));
    checkTask =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  for (Player player : plugin.getServer().getOnlinePlayers()) {
                    enforceArmorRestriction(player);
                  }
                },
                intervalTicks,
                intervalTicks);
  }

  public boolean enforceArmorRestriction(Player player) {
    if (player == null || !isWorldBlocked(player.getWorld().getName())) return false;
    if (!hasBlockedArmor(player) || player.hasPermission(bypassPermission)) return false;

    Material firstBlocked = null;
    org.bukkit.inventory.PlayerInventory inventory = player.getInventory();

    ItemStack helmet = inventory.getHelmet();
    if (slotMaterial(helmet) != null) {
      firstBlocked = helmet.getType();
      returnArmorToPlayer(player, helmet);
      inventory.setHelmet(null);
    }

    ItemStack chestplate = inventory.getChestplate();
    if (slotMaterial(chestplate) != null) {
      if (firstBlocked == null) firstBlocked = chestplate.getType();
      returnArmorToPlayer(player, chestplate);
      inventory.setChestplate(null);
    }

    ItemStack leggings = inventory.getLeggings();
    if (slotMaterial(leggings) != null) {
      if (firstBlocked == null) firstBlocked = leggings.getType();
      returnArmorToPlayer(player, leggings);
      inventory.setLeggings(null);
    }

    ItemStack boots = inventory.getBoots();
    if (slotMaterial(boots) != null) {
      if (firstBlocked == null) firstBlocked = boots.getType();
      returnArmorToPlayer(player, boots);
      inventory.setBoots(null);
    }

    if (firstBlocked != null) {
      sendDenyMessage(player, firstBlocked);
      return true;
    }
    return false;
  }

  private void returnArmorToPlayer(Player player, ItemStack item) {
    Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
    for (ItemStack leftover : leftovers.values()) {
      player.getWorld().dropItemNaturally(player.getLocation(), leftover);
    }
  }

  private static String prettify(String raw) {
    StringBuilder sb = new StringBuilder();
    boolean upper = true;
    for (char c : raw.toCharArray()) {
      if (c == '_') {
        sb.append(' ');
        upper = true;
      } else {
        sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
        upper = false;
      }
    }
    return sb.toString();
  }

  public boolean isBlocked(Material mat) {
    return blockedMaterials.contains(mat);
  }

  public boolean hasBlockedArmor(Player player) {
    return slotMaterial(player.getInventory().getHelmet()) != null
        || slotMaterial(player.getInventory().getChestplate()) != null
        || slotMaterial(player.getInventory().getLeggings()) != null
        || slotMaterial(player.getInventory().getBoots()) != null;
  }

  public List<Material> getBlockedArmor(Player player) {
    List<Material> found = null;

    Material m = slotMaterial(player.getInventory().getHelmet());
    if (m != null) {
      found = new ArrayList<>();
      found.add(m);
    }

    m = slotMaterial(player.getInventory().getChestplate());
    if (m != null) {
      if (found == null) found = new ArrayList<>();
      found.add(m);
    }

    m = slotMaterial(player.getInventory().getLeggings());
    if (m != null) {
      if (found == null) found = new ArrayList<>();
      found.add(m);
    }

    m = slotMaterial(player.getInventory().getBoots());
    if (m != null) {
      if (found == null) found = new ArrayList<>();
      found.add(m);
    }

    return found != null ? found : List.of();
  }

  private Material slotMaterial(ItemStack item) {
    if (item == null) return null;
    Material mat = item.getType();
    return blockedMaterials.contains(mat) ? mat : null;
  }

  public String formatBlocked(List<Material> blocked) {
    if (blocked.isEmpty()) return "";
    if (blocked.size() == 1) return "<gold>" + materialNames.get(blocked.get(0));

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < blocked.size(); i++) {
      if (i > 0) sb.append("<dark_gray>, </dark_gray>");
      sb.append("<gold>").append(materialNames.get(blocked.get(i)));
    }
    return sb.toString();
  }

  public String formatMaterial(Material mat) {
    return materialNames.getOrDefault(mat, prettify(mat.name()));
  }

  public boolean addWorld(String world) {
    if (blockedWorlds.contains(world.toLowerCase(Locale.ROOT))) return false;
    blockedWorlds.add(world.toLowerCase(Locale.ROOT));
    saveWorlds();
    return true;
  }

  public boolean removeWorld(String world) {
    if (!blockedWorlds.contains(world.toLowerCase(Locale.ROOT))) return false;
    blockedWorlds.remove(world.toLowerCase(Locale.ROOT));
    saveWorlds();
    return true;
  }

  public void clearWorlds() {
    blockedWorlds.clear();
    saveWorlds();
  }

  private void saveWorlds() {
    config.getConfig().set("blocked-worlds", new ArrayList<>(blockedWorlds));
    config.save();
  }

  public boolean isWorldBlocked(String world) {
    return blockedWorlds.contains(world.toLowerCase(Locale.ROOT));
  }

  public String getDenyMessage() {
    return denyMessage;
  }

  public String getBypassPermission() {
    return bypassPermission;
  }

  public Set<String> getBlockedWorlds() {
    return Collections.unmodifiableSet(blockedWorlds);
  }

  public Set<Material> getBlockedMaterials() {
    return blockedMaterials;
  }

  public void sendDenyMessage(Player player, Material mat) {
    if (!messageCache.canSend(player.getUniqueId(), "armorworld:equip")) return;
    player.sendMessage(pl.skyrise.skyRiseCore.utils.ColorUtil.mini(getDenyMessage()));
    player.sendMessage(
        pl.skyrise.skyRiseCore.utils.ColorUtil.mini(
            "<red>» Zablokowane: <gold>" + formatMaterial(mat)));
  }

  public JavaPlugin getPlugin() {
    return plugin;
  }
}
