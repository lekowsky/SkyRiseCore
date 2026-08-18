package pl.skyrise.skyRiseCore.features.knockout;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.utils.ColorUtil;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class KnockoutModule implements Module {

  private org.bukkit.event.Listener listener;

  private final JavaPlugin plugin;
  private final TabRegistry tabRegistry;
  private CustomConfig config;

  private int bleedoutTime;
  private int reviveTime;
  private String msgKnockedSelf;
  private String msgKnockedBroadcast;
  private String msgRevivingActionbar;
  private String msgRevivedSelf;
  private String msgRevivedOther;
  private String msgBleedoutBroadcast;

  private final Map<UUID, Integer> knockedPlayers = new ConcurrentHashMap<>();
  private final Map<UUID, Integer> sneakCounts = new ConcurrentHashMap<>();
  private final Map<UUID, Location> fakeBarrierLocations = new ConcurrentHashMap<>();
  private final Map<UUID, org.bukkit.boss.BossBar> activeBossBars = new ConcurrentHashMap<>();

  private final Map<UUID, Integer> reviveProgress = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> revivingTargets = new ConcurrentHashMap<>();

  private BukkitTask tickTask;

  public KnockoutModule(JavaPlugin plugin, TabRegistry tabRegistry) {
    this.plugin = plugin;
    this.tabRegistry = tabRegistry;
  }

  @Override
  public String getName() {
    return "Knockout";
  }

  @Override
  public void onEnable() {
    config = new CustomConfig(plugin, "knockout.yml");
    config.load();
    cacheConfig();

    this.listener =
        pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(
            plugin, new KnockoutListener(this));

    plugin.getLogger().info("  → Knockout: Załadowano pomyślnie.");
  }

  @Override
  public void onDisable() {
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListener(this.listener);
    this.listener = null;
    if (tickTask != null) {
      tickTask.cancel();
      tickTask = null;
    }

    for (UUID uuid : knockedPlayers.keySet()) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null && player.isOnline()) {
        resetKnockoutState(player);
      }
    }

    knockedPlayers.clear();
    sneakCounts.clear();
    reviveProgress.clear();
    revivingTargets.clear();
    activeBossBars.values().forEach(org.bukkit.boss.BossBar::removeAll);
    activeBossBars.clear();

    config.save();
  }

  @Override
  public void onReload() {
    config.reload();
    cacheConfig();
  }

  private void cacheConfig() {
    this.bleedoutTime = config.getConfig().getInt("bleedout-time", 30);
    this.reviveTime = config.getConfig().getInt("revive-time", 5);

    this.msgKnockedSelf =
        config
            .getConfig()
            .getString(
                "messages.knocked-self",
                "<red>» Zostałeś/aś powalony/a! Masz {time}s na ratunek. Kliknij 3x Shift aby się"
                    + " wykrwawić.");
    this.msgKnockedBroadcast =
        config
            .getConfig()
            .getString(
                "messages.knocked-broadcast",
                "<red>» Gracz <white>{player}</white> został powalony i potrzebuje pomocy!");
    this.msgRevivingActionbar =
        config
            .getConfig()
            .getString("messages.reviving-actionbar", "<gold>Podnoszenie: </gold>{bar}");
    this.msgRevivedSelf =
        config
            .getConfig()
            .getString(
                "messages.revived-self",
                "<green>» Zostałeś/aś podniesiony/a przez <white>{player}</white>!");
    this.msgRevivedOther =
        config
            .getConfig()
            .getString(
                "messages.revived-other", "<green>» Podniosłeś/aś gracza <white>{player}</white>!");
    this.msgBleedoutBroadcast =
        config
            .getConfig()
            .getString(
                "messages.bleedout-broadcast",
                "<red>» Gracz <white>{player}</white> wykrwawił się na śmierć.");
  }

  private void runTick() {
    if (knockedPlayers.isEmpty()) {
      if (!reviveProgress.isEmpty()) {
        reviveProgress.clear();
        revivingTargets.clear();
      }
      stopTickTaskIfIdle();
      return;
    }

    for (Map.Entry<UUID, Integer> entry : knockedPlayers.entrySet()) {
      UUID uuid = entry.getKey();
      Player player = Bukkit.getPlayer(uuid);
      if (player == null || !player.isOnline()) {
        knockedPlayers.remove(uuid);
        fakeBarrierLocations.remove(uuid);
        continue;
      }

      int currentSteps = entry.getValue();

      if (!revivingTargets.containsValue(uuid)) {
        currentSteps++;
        knockedPlayers.put(uuid, currentSteps);
      }

      player.setSwimming(true);
      player.setPose(Pose.SWIMMING);

      org.bukkit.boss.BossBar bar = activeBossBars.get(uuid);
      if (bar != null) {
        int totalSteps = bleedoutTime * 4;
        double progress = (double) (totalSteps - currentSteps) / totalSteps;
        progress = Math.max(0.0, Math.min(1.0, progress));
        bar.setProgress(progress);

        int secondsLeft = (int) Math.ceil((double) (totalSteps - currentSteps) / 4.0);

        if (revivingTargets.containsValue(uuid)) {
          bar.setColor(org.bukkit.boss.BarColor.BLUE);
          bar.setTitle("§bWykrwawianie: §f" + secondsLeft + "s §7[Wstrzymano]");
        } else {
          bar.setColor(org.bukkit.boss.BarColor.RED);
          bar.setTitle("§cWykrwawianie: §f" + secondsLeft + "s");
        }
      }

      if (currentSteps >= bleedoutTime * 4) {
        bleedoutPlayer(player);
      }
    }

    Set<UUID> activeReviversThisTick = new HashSet<>();

    for (UUID knockedUuid : knockedPlayers.keySet()) {
      Player knocked = Bukkit.getPlayer(knockedUuid);
      if (knocked == null || !knocked.isOnline()) continue;

      Location knockedLoc = knocked.getLocation();

      for (Player reviver : knocked.getWorld().getPlayers()) {
        if (reviver == knocked) continue;
        if (!reviver.isSneaking() || isKnocked(reviver.getUniqueId())) continue;

        Location reviverLoc = reviver.getLocation();
        if (reviverLoc.getBlockX() == knockedLoc.getBlockX()
            && reviverLoc.getBlockZ() == knockedLoc.getBlockZ()
            && Math.abs(reviverLoc.getBlockY() - knockedLoc.getBlockY()) <= 1) {

          UUID reviverUuid = reviver.getUniqueId();
          activeReviversThisTick.add(reviverUuid);
          revivingTargets.put(reviverUuid, knockedUuid);

          int progress = reviveProgress.getOrDefault(reviverUuid, 0) + 1;
          reviveProgress.put(reviverUuid, progress);

          int maxSteps = reviveTime * 4;
          String bar = getProgressBar(progress, maxSteps);
          String barMessage = msgRevivingActionbar.replace("{bar}", bar);

          reviver.sendActionBar(ColorUtil.mini(barMessage));
          knocked.sendActionBar(ColorUtil.mini(barMessage));

          float pitch = 0.5f + (((float) progress) / maxSteps) * 1.0f;
          reviver.playSound(
              reviver.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, pitch);
          knocked.playSound(
              knocked.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, pitch);

          if (progress >= maxSteps) {
            revivePlayer(reviver, knocked);
            activeReviversThisTick.remove(reviverUuid);
          }

          break;
        }
      }
    }

    reviveProgress
        .keySet()
        .removeIf(
            uuid -> {
              boolean keep = activeReviversThisTick.contains(uuid);
              if (!keep) {
                revivingTargets.remove(uuid);
              }
              return !keep;
            });

    stopTickTaskIfIdle();
  }

  private void ensureTickTask() {
    if (tickTask != null && !tickTask.isCancelled()) return;
    tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::runTick, 5L, 5L);
  }

  private void stopTickTaskIfIdle() {
    if (!knockedPlayers.isEmpty() || tickTask == null) return;
    tickTask.cancel();
    tickTask = null;
  }

  public boolean isKnocked(UUID uuid) {
    return knockedPlayers.containsKey(uuid);
  }

  public Set<UUID> getKnockedPlayers() {
    return knockedPlayers.keySet();
  }

  public void knockPlayer(Player player) {
    UUID uuid = player.getUniqueId();
    knockedPlayers.put(uuid, 0);
    sneakCounts.put(uuid, 0);
    ensureTickTask();

    player.setHealth(1.0);
    player.setSwimming(true);
    player.setPose(Pose.SWIMMING);

    Location barrierLoc = player.getLocation().add(0, 1.5, 0);
    player.sendBlockChange(barrierLoc, Material.BARRIER.createBlockData());
    fakeBarrierLocations.put(uuid, barrierLoc);

    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              if (isKnocked(uuid)) {
                player.addPotionEffect(
                    new PotionEffect(PotionEffectType.BLINDNESS, 999999, 0, false, false, true));
                player.addPotionEffect(
                    new PotionEffect(PotionEffectType.DARKNESS, 999999, 0, false, false, true));
              }
            });

    org.bukkit.boss.BossBar bar =
        Bukkit.createBossBar(
            "§cWykrwawianie: §f" + bleedoutTime + "s",
            org.bukkit.boss.BarColor.RED,
            org.bukkit.boss.BarStyle.SOLID);
    bar.setProgress(1.0);
    bar.addPlayer(player);
    activeBossBars.put(uuid, bar);

    player.sendMessage(
        ColorUtil.mini(msgKnockedSelf.replace("{time}", String.valueOf(bleedoutTime))));

    player
        .getWorld()
        .playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.5f, 0.8f);

    Location loc = player.getLocation();
    double radiusSq = 40.0 * 40.0;
    for (Player online : player.getWorld().getPlayers()) {
      if (online.getLocation().distanceSquared(loc) <= radiusSq) {
        online.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
      }
    }

    String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
    String posX = String.valueOf(loc.getBlockX());
    String posY = String.valueOf(loc.getBlockY());
    String posZ = String.valueOf(loc.getBlockZ());

    String broadcastMsg =
        msgKnockedBroadcast
            .replace("{player}", player.getName())
            .replace("{world}", worldName)
            .replace("{x}", posX)
            .replace("{y}", posY)
            .replace("{z}", posZ);

    net.kyori.adventure.text.Component parsed = ColorUtil.mini(broadcastMsg);

    for (Player online : Bukkit.getOnlinePlayers()) {
      if (online.hasPermission("skyrise.doctor")) {
        online.sendMessage(parsed);
        online.playSound(online.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
      }
    }
    Bukkit.getConsoleSender().sendMessage(parsed);
  }

  public void revivePlayer(Player reviver, Player target) {
    UUID targetUuid = target.getUniqueId();
    UUID reviverUuid = reviver.getUniqueId();

    resetKnockoutState(target);

    target.setHealth(6.0);

    target.sendMessage(ColorUtil.mini(msgRevivedSelf.replace("{player}", reviver.getName())));
    reviver.sendMessage(ColorUtil.mini(msgRevivedOther.replace("{player}", target.getName())));

    target.sendActionBar(ColorUtil.mini("<green>» Zostałeś/aś podniesiony/a!"));
    reviver.sendActionBar(ColorUtil.mini("<green>» Podniosłeś/aś gracza!"));

    target
        .getWorld()
        .playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.2f);

    reviveProgress.remove(reviverUuid);
    revivingTargets.remove(reviverUuid);
  }

  public void bleedoutPlayer(Player player) {
    UUID uuid = player.getUniqueId();

    resetKnockoutState(player);

    Location loc = player.getLocation().add(0, 0.4, 0);
    org.bukkit.block.data.BlockData blockData =
        org.bukkit.Bukkit.createBlockData(org.bukkit.Material.REDSTONE_BLOCK);
    player
        .getWorld()
        .spawnParticle(org.bukkit.Particle.BLOCK, loc, 120, 0.35, 0.35, 0.35, 0.15, blockData);

    player
        .getWorld()
        .playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_DEATH, 1.5f, 0.8f);
    player
        .getWorld()
        .playSound(
            player.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.5f, 0.7f);

    player.setHealth(0.0);

    String msg = msgBleedoutBroadcast.replace("{player}", player.getName());
    Bukkit.broadcast(ColorUtil.mini(msg));
  }

  public void handleKnockedSneak(Player player) {
    UUID uuid = player.getUniqueId();

    if (revivingTargets.containsValue(uuid)) {
      player.sendMessage(
          ColorUtil.mini("<red>» Nie możesz się wykrwawić, ponieważ ktoś Cię podnosi!"));
      return;
    }

    int count = sneakCounts.getOrDefault(uuid, 0) + 1;
    sneakCounts.put(uuid, count);

    if (count >= 3) {
      bleedoutPlayer(player);
    } else {
      player.sendMessage(
          ColorUtil.mini("<red>» Kliknij Shift jeszcze " + (3 - count) + "x aby się wykrwawić."));
    }
  }

  private void resetKnockoutState(Player player) {
    UUID uuid = player.getUniqueId();
    knockedPlayers.remove(uuid);
    sneakCounts.remove(uuid);

    org.bukkit.boss.BossBar bar = activeBossBars.remove(uuid);
    if (bar != null) {
      bar.removeAll();
    }

    Location barrierLoc = fakeBarrierLocations.remove(uuid);
    if (barrierLoc != null) {
      player.sendBlockChange(barrierLoc, barrierLoc.getBlock().getBlockData());
    }

    player.setSwimming(false);
    player.setPose(Pose.STANDING);
    player.removePotionEffect(PotionEffectType.BLINDNESS);
    player.removePotionEffect(PotionEffectType.DARKNESS);
  }

  private String getProgressBar(int current, int max) {
    int barLength = 10;
    int filledCount = (current * barLength) / max;
    filledCount = Math.min(filledCount, barLength);

    StringBuilder sb = new StringBuilder();
    sb.append("<dark_gray>[");
    sb.append("<green>");
    for (int i = 0; i < filledCount; i++) {
      sb.append("■");
    }
    sb.append("</green>");
    sb.append("<gray>");
    for (int i = filledCount; i < barLength; i++) {
      sb.append("■");
    }
    sb.append("</gray>");
    sb.append("<dark_gray>] </dark_gray>");

    int percent = (current * 100) / max;
    sb.append("<gold>").append(percent).append("%</gold>");
    return sb.toString();
  }
}
