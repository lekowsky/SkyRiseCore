package pl.skyrise.skyRiseCore.features.automat.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseCore.core.VaultHook;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;

public class EconomyManager {

  private final AutomatModule plugin;
  private Economy economy;

  public EconomyManager(AutomatModule plugin) {
    this.plugin = plugin;
  }

  public boolean setupEconomy() {
    economy = VaultHook.getEconomy();
    return economy != null;
  }

  public boolean isReady() {
    return provider() != null;
  }

  public double getBalance(Player player) {
    Economy provider = provider();
    if (provider == null || player == null) return 0.0;
    return provider.getBalance(player);
  }

  public boolean hasEnough(Player player, double amount) {
    if (amount <= 0.0) return true;
    Economy provider = provider();
    return provider != null && player != null && provider.has(player, amount);
  }

  public boolean withdraw(Player player, double amount) {
    if (amount <= 0.0) return true;
    Economy provider = provider();
    return provider != null
        && player != null
        && provider.withdrawPlayer(player, amount).transactionSuccess();
  }

  public boolean deposit(Player player, double amount) {
    if (amount <= 0.0) return true;
    Economy provider = provider();
    return provider != null
        && player != null
        && provider.depositPlayer(player, amount).transactionSuccess();
  }

  public String getCurrencySymbol() {
    return plugin.getConfig().getString("currency-symbol", "$");
  }

  private Economy provider() {
    if (economy == null) {
      economy = VaultHook.getEconomy();
    }
    return economy;
  }
}
