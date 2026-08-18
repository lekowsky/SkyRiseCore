package pl.skyrise.skyRiseCore.features.automat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.features.automat.command.VendingCommand;
import pl.skyrise.skyRiseCore.features.automat.listener.ChatInputListener;
import pl.skyrise.skyRiseCore.features.automat.listener.GUIListener;
import pl.skyrise.skyRiseCore.features.automat.listener.InteractListener;
import pl.skyrise.skyRiseCore.features.automat.listener.NexoListener;
import pl.skyrise.skyRiseCore.features.automat.manager.DataManager;
import pl.skyrise.skyRiseCore.features.automat.manager.EconomyManager;
import pl.skyrise.skyRiseCore.features.automat.manager.MachineManager;
import pl.skyrise.skyRiseCore.features.automat.manager.NexoManager;
import pl.skyrise.skyRiseCore.features.automat.manager.PlacementManager;
import pl.skyrise.skyRiseCore.features.automat.manager.RestockManager;
import pl.skyrise.skyRiseCore.utils.ColorUtil;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class AutomatModule implements Module {

  private static AutomatModule instance;

  private final JavaPlugin plugin;
  private final TabRegistry tabRegistry;
  private final List<Listener> listeners = new ArrayList<>();

  private CustomConfig config;
  private File moduleDataFolder;

  private MachineManager machineManager;
  private PlacementManager placementManager;
  private DataManager dataManager;
  private EconomyManager economyManager;
  private NexoManager nexoManager;
  private RestockManager restockManager;
  private VendingCommand commandExecutor;

  public AutomatModule(JavaPlugin plugin, TabRegistry tabRegistry) {
    this.plugin = plugin;
    this.tabRegistry = tabRegistry;
  }

  @Override
  public String getName() {
    return "Automat";
  }

  @Override
  public void onEnable() {
    instance = this;

    config = new CustomConfig(plugin, "automat.yml");
    config.load();
    getDataFolder().mkdirs();

    economyManager = new EconomyManager(this);
    if (!economyManager.setupEconomy()) {
      throw new IllegalStateException(
          "Vault economy nie znaleziony — moduł Automat wymaga ekonomii.");
    }

    dataManager = new DataManager(this);
    machineManager = new MachineManager(this);
    placementManager = new PlacementManager(this);
    nexoManager = new NexoManager(this);
    restockManager = new RestockManager(this);

    dataManager.loadTemplates();
    dataManager.loadPlacements();
    restockManager.startAll();

    commandExecutor = new VendingCommand(this);
    PluginCommand command = plugin.getCommand("automat");
    if (command == null) {
      throw new IllegalStateException("Brak komendy 'automat' w plugin.yml.");
    }
    command.setExecutor(commandExecutor);
    tabRegistry.register(
        "automat", (sender, args) -> commandExecutor.onTabComplete(sender, null, "automat", args));

    registerListener(new GUIListener(this));
    registerListener(new InteractListener(this));
    registerListener(new ChatInputListener(this));

    if (nexoManager.isNexoAvailable()) {
      registerListener(new NexoListener(this));
      plugin.getLogger().info("  → Automat: integracja Nexo włączona.");
    }

    plugin
        .getLogger()
        .info(
            "  → Automat: "
                + machineManager.getAllTemplates().size()
                + " szablonów, "
                + placementManager.getAllPlacements().size()
                + " instancji.");
  }

  @Override
  public void onDisable() {
    if (restockManager != null) {
      restockManager.stopAll();
    }
    if (dataManager != null) {
      dataManager.saveAll();
    }

    ChatInputListener.clearPendingInputs();

    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListeners(listeners);
    listeners.clear();

    pl.skyrise.skyRiseCore.core.ModuleSupport.bindDisabled(plugin, getName(), "automat");
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterTabs(tabRegistry, "automat");

    if (config != null) {
      config.save();
    }
  }

  @Override
  public void onReload() {
    if (dataManager != null) {
      dataManager.flushQueuedSaves();
    }
    reloadConfig();
    if (nexoManager != null) {
      nexoManager.loadMappings();
    }
    if (restockManager != null) {
      restockManager.stopAll();
      restockManager.startAll();
    }
  }

  private void registerListener(Listener listener) {
    listeners.add(listener);

    pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(plugin, listener);
  }

  public static AutomatModule getInstance() {
    return instance;
  }

  public JavaPlugin getPlugin() {
    return plugin;
  }

  public Server getServer() {
    return plugin.getServer();
  }

  public Logger getLogger() {
    return plugin.getLogger();
  }

  public File getDataFolder() {
    if (moduleDataFolder == null) {
      moduleDataFolder = new File(plugin.getDataFolder(), "automat");
    }
    return moduleDataFolder;
  }

  public FileConfiguration getConfig() {
    return config.getConfig();
  }

  public void reloadConfig() {
    config.reload();
  }

  public void saveConfig() {
    config.save();
  }

  public MachineManager getMachineManager() {
    return machineManager;
  }

  public PlacementManager getPlacementManager() {
    return placementManager;
  }

  public DataManager getDataManager() {
    return dataManager;
  }

  public EconomyManager getEconomyManager() {
    return economyManager;
  }

  public NexoManager getNexoManager() {
    return nexoManager;
  }

  public RestockManager getRestockManager() {
    return restockManager;
  }

  public String getPrefix() {
    return ColorUtil.miniToLegacy(
        getConfig()
            .getString("prefix", "<reset><#38f28f><bold>Automat <reset><gray>» <reset><white>"));
  }
}
