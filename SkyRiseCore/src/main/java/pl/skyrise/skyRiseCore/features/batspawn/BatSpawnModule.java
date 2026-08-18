package pl.skyrise.skyRiseCore.features.batspawn;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class BatSpawnModule implements Module {

  private org.bukkit.event.Listener listener;

  private final JavaPlugin plugin;
  private final MessageCache messageCache;
  private CustomConfig config;

  private int maxY;
  private Set<String> allowedBlocks;
  private String denyMessage;

  public BatSpawnModule(JavaPlugin plugin, MessageCache messageCache) {
    this.plugin = plugin;
    this.messageCache = messageCache;
  }

  @Override
  public String getName() {
    return "BatSpawn";
  }

  @Override
  public void onEnable() {
    config = new CustomConfig(plugin, "batspawn.yml");
    config.load();
    cacheConfig();

    this.listener =
        pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(
            plugin, new BatSpawnListener(this, messageCache));

    plugin.getLogger().info("  → BatSpawn: nietoperze zablokowane powyżej Y " + maxY);
  }

  @Override
  public void onDisable() {
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListener(this.listener);
    this.listener = null;
    config.save();
  }

  @Override
  public void onReload() {
    config.reload();
    cacheConfig();
  }

  private void cacheConfig() {
    this.maxY = config.getConfig().getInt("max-y", 40);

    Set<String> blocks = new HashSet<>();
    for (String block : config.getConfig().getStringList("allowed-blocks")) {
      blocks.add(block.toUpperCase());
    }
    if (blocks.isEmpty()) {
      blocks = Set.of("STONE", "DEEPSLATE");
    }
    this.allowedBlocks = Collections.unmodifiableSet(blocks);

    this.denyMessage =
        config
            .getConfig()
            .getString(
                "deny-message",
                "<red>» Nietoperze mogą spawnować się tylko poniżej Y {max_y} na blokach"
                    + " stone/deepslate.")
            .replace("{max_y}", String.valueOf(maxY));
  }

  public boolean isAllowed(Material material) {
    return allowedBlocks.contains(material.name());
  }

  public int getMaxY() {
    return maxY;
  }

  public String getDenyMessage() {
    return denyMessage;
  }

  public JavaPlugin getPlugin() {
    return plugin;
  }
}
