package pl.skyrise.skyRiseCore.core;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHook {

  private static Economy economy;

  private VaultHook() {}

  public static synchronized void setup() {
    refresh();
  }

  public static synchronized void refresh() {
    economy = findProvider();
  }

  public static Economy getEconomy() {
    Economy provider = economy;
    if (provider == null) {
      refresh();
      provider = economy;
    }
    return provider;
  }

  public static boolean isEconomyAvailable() {
    return getEconomy() != null;
  }

  private static Economy findProvider() {
    try {
      RegisteredServiceProvider<Economy> registration =
          Bukkit.getServicesManager().getRegistration(Economy.class);
      return registration != null ? registration.getProvider() : null;
    } catch (Throwable ignored) {
      return null;
    }
  }
}
