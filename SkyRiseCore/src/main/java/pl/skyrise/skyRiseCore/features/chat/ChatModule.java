package pl.skyrise.skyRiseCore.features.chat;

import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class ChatModule implements Module {

  private org.bukkit.event.Listener listener;

  private final JavaPlugin plugin;
  private final TabRegistry tabRegistry;
  private final MessageCache messageCache;
  private CustomConfig config;

  private int radiusSquared;
  private int globalCooldownMs;
  private String globalFormat;
  private String cooldownMessage;

  public ChatModule(JavaPlugin plugin, TabRegistry tabRegistry, MessageCache messageCache) {
    this.plugin = plugin;
    this.tabRegistry = tabRegistry;
    this.messageCache = messageCache;
  }

  @Override
  public String getName() {
    return "Chat";
  }

  @Override
  public void onEnable() {
    config = new CustomConfig(plugin, "chat.yml");
    config.load();
    cacheConfig();

    this.listener =
        pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(plugin, new ChatListener(this));

    GlobalChatCommand command = new GlobalChatCommand(this);
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindExecutor(plugin, command, "globalchat");

    tabRegistry.register("globalchat", (sender, args) -> java.util.List.of());

    plugin
        .getLogger()
        .info(
            "  → Chat: lokalny "
                + (int) Math.sqrt(radiusSquared)
                + " bloków, globalny /g ("
                + (globalCooldownMs / 1000)
                + "s cooldown)");
  }

  @Override
  public void onDisable() {
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindDisabled(plugin, getName(), "globalchat");
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListener(this.listener);
    this.listener = null;
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterTabs(tabRegistry, "globalchat");
    config.save();
  }

  @Override
  public void onReload() {
    config.reload();
    cacheConfig();
  }

  private void cacheConfig() {
    int radius = config.getConfig().getInt("local-radius", 40);
    this.radiusSquared = radius * radius;
    this.globalCooldownMs = config.getConfig().getInt("global-cooldown", 30) * 1000;

    this.globalFormat =
        config
            .getConfig()
            .getString(
                "global-format",
                "<white>[<aqua>G</aqua>]</white> <gray>{player}</gray> <dark_gray>»</dark_gray>"
                    + " <white>{message}</white>");

    this.cooldownMessage =
        config
            .getConfig()
            .getString(
                "cooldown-message",
                "<red>» Musisz odczekać jeszcze {time} sekund przed ponownym użyciem czatu"
                    + " globalnego.");
  }

  public int getRemainingCooldown(UUID uuid) {
    long last = messageCache.getLastSend(uuid, "globalchat");
    if (last == 0) return 0;
    long remaining = globalCooldownMs - (System.currentTimeMillis() - last);
    return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
  }

  public void setCooldown(UUID uuid) {
    messageCache.setSent(uuid, "globalchat");
  }

  public int getRadiusSquared() {
    return radiusSquared;
  }

  public String getGlobalFormat() {
    return globalFormat;
  }

  public String getCooldownMessage() {
    return cooldownMessage;
  }

  public JavaPlugin getPlugin() {
    return plugin;
  }
}
