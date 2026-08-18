package pl.skyrise.skyRiseCore.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class MessageCache implements Listener {

  private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> cache =
      new ConcurrentHashMap<>();
  private volatile long cooldownMs;

  public MessageCache(long cooldownMs) {
    this.cooldownMs = cooldownMs;
  }

  public boolean canSend(UUID uuid, String messageKey) {
    if (uuid == null || messageKey == null) return false;

    long now = System.currentTimeMillis();
    long cooldown = cooldownMs;
    ConcurrentHashMap<String, Long> playerCache = playerCache(uuid);

    while (true) {
      Long last = playerCache.get(messageKey);
      if (last != null && now - last < cooldown) return false;

      if (last == null) {
        if (playerCache.putIfAbsent(messageKey, now) == null) return true;
      } else if (playerCache.replace(messageKey, last, now)) {
        return true;
      }
    }
  }

  public long getLastSend(UUID uuid, String messageKey) {
    if (uuid == null || messageKey == null) return 0L;
    ConcurrentHashMap<String, Long> playerCache = cache.get(uuid);
    if (playerCache == null) return 0L;
    Long last = playerCache.get(messageKey);
    return last != null ? last : 0L;
  }

  public void setSent(UUID uuid, String messageKey) {
    if (uuid == null || messageKey == null) return;
    playerCache(uuid).put(messageKey, System.currentTimeMillis());
  }

  public void setCooldownMs(long cooldownMs) {
    this.cooldownMs = cooldownMs;
  }

  public long getCooldownMs() {
    return cooldownMs;
  }

  public void clean(UUID uuid) {
    if (uuid != null) {
      cache.remove(uuid);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    clean(event.getPlayer().getUniqueId());
  }

  private ConcurrentHashMap<String, Long> playerCache(UUID uuid) {
    ConcurrentHashMap<String, Long> playerCache = cache.get(uuid);
    if (playerCache != null) return playerCache;

    ConcurrentHashMap<String, Long> created = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Long> existing = cache.putIfAbsent(uuid, created);
    return existing != null ? existing : created;
  }
}
