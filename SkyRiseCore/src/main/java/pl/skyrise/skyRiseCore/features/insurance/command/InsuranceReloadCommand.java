package pl.skyrise.skyRiseCore.features.insurance.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.skyrise.skyRiseCore.features.insurance.InsuranceModule;
import pl.skyrise.skyRiseCore.features.insurance.util.InsuranceText;

public class InsuranceReloadCommand implements CommandExecutor {

  private final InsuranceModule module;

  public InsuranceReloadCommand(InsuranceModule module) {
    this.module = module;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("skyrise.insurance.admin")) {
      sender.sendMessage(InsuranceText.component(module.message("no-permission")));
      return true;
    }

    module.onReload();
    sender.sendMessage(InsuranceText.component(module.message("reloaded")));
    return true;
  }
}
