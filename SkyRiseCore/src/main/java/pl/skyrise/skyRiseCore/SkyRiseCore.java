package pl.skyrise.skyRiseCore;

import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.ModuleManager;
import pl.skyrise.skyRiseCore.core.CitizensHook;
import pl.skyrise.skyRiseCore.core.CoreTabCompleter;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.core.SkyRiseCoreCommand;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.core.VaultHook;
import pl.skyrise.skyRiseCore.core.npc.NpcRegistry;
import pl.skyrise.skyRiseCore.features.adminchat.AdminChatModule;
import pl.skyrise.skyRiseCore.features.armorworld.ArmorWorldModule;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.batspawn.BatSpawnModule;
import pl.skyrise.skyRiseCore.features.butelkomat.ButelkomatModule;
import pl.skyrise.skyRiseCore.features.chat.ChatModule;
import pl.skyrise.skyRiseCore.features.freeze.FreezeModule;
import pl.skyrise.skyRiseCore.features.insurance.InsuranceModule;
import pl.skyrise.skyRiseCore.features.knockout.KnockoutModule;
import pl.skyrise.skyRiseCore.features.roleplay.RolePlayModule;

public class SkyRiseCore extends JavaPlugin {

  private static final String LOADED_MODULES_PREFIX = "Załadowane moduły: ";

  private static SkyRiseCore instance;
  private ModuleManager moduleManager;
  private TabRegistry tabRegistry;
  private MessageCache messageCache;
  private NpcRegistry npcRegistry;

  @Override
  public void onEnable() {
    instance = this;
    getLogger()
        .setFilter(
            record -> {
              String message = record.getMessage();
              return record.getLevel().intValue() >= Level.SEVERE.intValue()
                  || (message != null
                      && (message.startsWith(LOADED_MODULES_PREFIX)
                          || message.startsWith("Brak integracji")));
            });
    moduleManager = new ModuleManager(this);
    tabRegistry = new TabRegistry();
    messageCache = new MessageCache(2000);
    npcRegistry = new NpcRegistry(this);

    CitizensHook.setup(this, npcRegistry);
    VaultHook.setup();

    if (!VaultHook.isEconomyAvailable()) {
      getLogger().warning("Brak integracji Vault Economy.");
    }
    if (!CitizensHook.isEnabled()) {
      getLogger().warning("Brak integracji Citizens.");
    }

    getServer().getPluginManager().registerEvents(messageCache, this);
    getServer().getPluginManager().registerEvents(npcRegistry, this);
    getServer()
        .getPluginManager()
        .registerEvents(new pl.skyrise.skyRiseCore.gui.GuiListener(), this);
    getCommand("skyrisecore").setExecutor(new SkyRiseCoreCommand(this, tabRegistry));

    moduleManager.register(new AdminChatModule(this, tabRegistry, messageCache));
    moduleManager.register(new BatSpawnModule(this, messageCache));
    moduleManager.register(new ArmorWorldModule(this, tabRegistry, messageCache));
    moduleManager.register(new ChatModule(this, tabRegistry, messageCache));
    moduleManager.register(new RolePlayModule(this, tabRegistry));
    moduleManager.register(new FreezeModule(this, tabRegistry));
    moduleManager.register(new KnockoutModule(this, tabRegistry));
    moduleManager.register(new AutomatModule(this, tabRegistry));
    moduleManager.register(new InsuranceModule(this, tabRegistry));

    moduleManager.register(new ButelkomatModule(this, tabRegistry));
    CoreTabCompleter completer = new CoreTabCompleter(tabRegistry);
    for (String cmd : tabRegistry.getRegisteredCommands()) {
      getCommand(cmd).setTabCompleter(completer);
    }

    getLogger()
        .info(
            LOADED_MODULES_PREFIX
                + moduleManager.getEnabledModuleCount()
                + "/"
                + moduleManager.getModuleCount()
                + ".");
  }

  @Override
  public void onDisable() {
    if (moduleManager != null) {
      moduleManager.disableAll();
    }
  }

  public static SkyRiseCore getInstance() {
    return instance;
  }

  public ModuleManager getModuleManager() {
    return moduleManager;
  }

  public TabRegistry getTabRegistry() {
    return tabRegistry;
  }

  public MessageCache getMessageCache() {
    return messageCache;
  }

  public NpcRegistry getNpcRegistry() {
    return npcRegistry;
  }
}
