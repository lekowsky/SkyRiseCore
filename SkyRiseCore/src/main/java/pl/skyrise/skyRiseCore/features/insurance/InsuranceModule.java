package pl.skyrise.skyRiseCore.features.insurance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pl.skyrise.skyRiseCore.SkyRiseCore;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.CitizensHook;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.core.VaultHook;
import pl.skyrise.skyRiseCore.core.npc.NpcRegistry;
import pl.skyrise.skyRiseCore.features.insurance.command.InsuranceCommand;
import pl.skyrise.skyRiseCore.features.insurance.command.InsuranceNpcCommand;
import pl.skyrise.skyRiseCore.features.insurance.command.InsuranceReloadCommand;
import pl.skyrise.skyRiseCore.features.insurance.gui.InsuranceMenu;
import pl.skyrise.skyRiseCore.features.insurance.listener.InsuranceListener;
import pl.skyrise.skyRiseCore.features.insurance.model.InsurancePolicy;
import pl.skyrise.skyRiseCore.features.insurance.model.NpcPoint;
import pl.skyrise.skyRiseCore.features.insurance.model.RescueEffect;
import pl.skyrise.skyRiseCore.features.insurance.util.InsuranceText;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

public class InsuranceModule implements Module {

  public static final String MODULE_ID = "insurance";

  private final JavaPlugin plugin;
  private final TabRegistry tabRegistry;
  private final NpcRegistry npcRegistry;
  private final NamespacedKey labelModuleKey;
  private final NamespacedKey labelIdKey;

  private CustomConfig config;
  private File dataFile;
  private BukkitTask queuedSaveTask;
  private BukkitTask cleanupTask;
  private BukkitTask warningTask;

  private InsuranceListener listener;
  private InsuranceCommand insuranceCommand;
  private InsuranceNpcCommand npcCommand;
  private InsuranceReloadCommand reloadCommand;

  private final Map<UUID, InsurancePolicy> insuredPlayers = new ConcurrentHashMap<>();
  private final Map<UUID, Long> expiredPolicyNotifications = new ConcurrentHashMap<>();
  private final Map<UUID, Long> guiOpenCooldowns = new ConcurrentHashMap<>();
  private final Map<String, NpcPoint> npcPoints = new LinkedHashMap<>();
  private final Map<String, RescueEffect> rescueEffects = new LinkedHashMap<>();

  private String prefix;
  private double price;
  private double renewalPrice;
  private String currencySymbol;
  private long durationMillis;
  private long warningBeforeMillis;
  private long cleanupIntervalTicks;
  private long onlineWarningIntervalTicks;
  private boolean allowRenewal;
  private String premiumPermission;
  private String defaultEffectId;
  private int maxCharges;

  private long guiOpenCooldownMillis;
  private int guiSize;
  private String guiTitle;
  private boolean guiFillEmpty;
  private Material guiFillerMaterial;
  private String guiFillerName;
  private int guiInsuranceSlot;
  private int guiInsuranceActiveSlot;
  private Material guiInsuranceMaterial;
  private String guiInsuranceName;
  private List<String> guiInsuranceLore;
  private String guiClickToBuyLine;
  private String guiAlreadyOwnedLine;
  private int guiRenewSlot;
  private Material guiRenewMaterial;
  private String guiRenewName;
  private List<String> guiRenewLore;
  private int guiCloseSlot;
  private Material guiCloseMaterial;
  private String guiCloseName;
  private List<String> guiCloseLore;
  private int guiEffectButtonSlot;
  private Material guiEffectButtonMaterial;
  private String guiEffectButtonName;
  private List<String> guiEffectButtonLore;
  private String guiEffectsTitle;
  private Material guiBackMaterial;
  private String guiBackName;
  private List<String> guiBackLore;

  private EntityType npcEntityType;
  private String npcProvider;
  private String npcName;
  private String npcCitizensName;
  private String npcSkin;
  private boolean npcGlowing;

  private double restoreHealth;
  private boolean restoreFood;
  private int restoreFoodLevel;
  private float restoreSaturation;
  private boolean extinguish;
  private boolean clearNegativeEffects;
  private int noDamageTicks;
  private boolean teleportOnVoid;
  private Sound protectionSound;
  private Sound soundOpen;
  private Sound soundPurchase;
  private Sound soundRenewed;
  private Sound soundAlreadyActive;
  private Sound soundNoMoney;
  private Sound soundError;
  private Sound soundTotemBlocked;
  private Particle protectionParticle;
  private int protectionParticleCount;
  private final List<PotionEffect> protectionEffects = new ArrayList<>();
  private final Set<String> ignoredCauses = new HashSet<>();

  private Map<String, String> messages = new HashMap<>();

  public InsuranceModule(JavaPlugin plugin, TabRegistry tabRegistry) {
    this.plugin = plugin;
    this.tabRegistry = tabRegistry;
    this.npcRegistry = plugin instanceof SkyRiseCore core ? core.getNpcRegistry() : null;
    this.labelModuleKey = new NamespacedKey(plugin, "insurance_label_module");
    this.labelIdKey = new NamespacedKey(plugin, "insurance_label_id");
  }

  @Override
  public String getName() {
    return "Ubezpieczenie";
  }

  @Override
  public void onEnable() {
    if (npcRegistry == null) {
      throw new IllegalStateException("NpcRegistry nie jest dostępny.");
    }
    if (VaultHook.getEconomy() == null) {
      throw new IllegalStateException(
          "Vault economy nie znaleziony — moduł Ubezpieczenie wymaga ekonomii.");
    }

    config = new CustomConfig(plugin, "insurance.yml");
    config.load();
    cacheConfig();

    dataFile = new File(plugin.getDataFolder(), "insurance" + File.separator + "data.yml");
    loadData();

    listener =
        pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(
            plugin, new InsuranceListener(this));

    insuranceCommand = new InsuranceCommand(this);
    npcCommand = new InsuranceNpcCommand(this);
    reloadCommand = new InsuranceReloadCommand(this);

    pl.skyrise.skyRiseCore.core.ModuleSupport.bindExecutor(
        plugin, insuranceCommand, "ubezpieczenie");
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindExecutor(plugin, npcCommand, "ubezpieczenienpc");
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindExecutor(
        plugin, reloadCommand, "ubezpieczeniereload");

    tabRegistry.register("ubezpieczenie", (sender, args) -> insuranceCommand.tab(sender, args));
    tabRegistry.register("ubezpieczenienpc", (sender, args) -> npcCommand.tab(sender, args));
    tabRegistry.register("ubezpieczeniereload", (sender, args) -> List.of());

    npcRegistry.registerHandler(MODULE_ID, (player, entity, npcId) -> openGui(player));
    respawnAllNpcs();
    cleanupExpiredPolicies();
    startCleanupTask();
    startWarningTask();

    plugin
        .getLogger()
        .info(
            "  → Ubezpieczenie: "
                + insuredPlayers.size()
                + " aktywnych polis, "
                + npcPoints.size()
                + " NPC.");
  }

  @Override
  public void onDisable() {
    flushDataSave();
    if (cleanupTask != null) {
      cleanupTask.cancel();
      cleanupTask = null;
    }
    if (warningTask != null) {
      warningTask.cancel();
      warningTask = null;
    }
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListener(listener);
    listener = null;
    if (npcRegistry != null) {
      npcRegistry.unregisterHandler(MODULE_ID);
      for (NpcPoint point : npcPoints.values()) {
        destroyCitizensNpc(point.getCitizensId());
      }
      npcRegistry.removeAll(MODULE_ID);
    }
    pl.skyrise.skyRiseCore.core.ModuleSupport.bindDisabled(
        plugin, getName(), "ubezpieczenie", "ubezpieczenienpc", "ubezpieczeniereload");
    pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterTabs(
        tabRegistry, "ubezpieczenie", "ubezpieczenienpc", "ubezpieczeniereload");
  }

  @Override
  public void onReload() {
    flushDataSave();
    config.reload();
    cacheConfig();
    cleanupExpiredPolicies();
    restartCleanupTask();
    restartWarningTask();
    respawnAllNpcs();
  }

  private void cacheConfig() {
    FileConfiguration c = config.getConfig();
    this.prefix =
        c.getString("prefix", "<reset><#f5f242><bold>Ubezpieczenie <reset><gray>» <reset><white>");
    this.price = Math.max(0.0, c.getDouble("economy.price", 500.0));
    this.renewalPrice = Math.max(0.0, c.getDouble("economy.renewal-price", price));
    this.currencySymbol = c.getString("economy.currency-symbol", "$ ").trim();
    if (currencySymbol.isEmpty()) currencySymbol = "$";

    long durationDays = Math.max(1L, c.getLong("insurance.duration-days", 7L));
    long warningHours = Math.max(0L, c.getLong("insurance.expiry-warning-hours", 24L));
    long cleanupMinutes = Math.max(1L, c.getLong("insurance.cleanup-interval-minutes", 30L));
    long onlineWarningMinutes =
        Math.max(1L, c.getLong("insurance.online-warning-interval-minutes", 15L));
    this.durationMillis = durationDays * 24L * 60L * 60L * 1000L;
    this.warningBeforeMillis = warningHours * 60L * 60L * 1000L;
    this.cleanupIntervalTicks = cleanupMinutes * 60L * 20L;
    this.onlineWarningIntervalTicks = onlineWarningMinutes * 60L * 20L;
    this.allowRenewal = c.getBoolean("insurance.allow-renewal", true);
    this.premiumPermission = c.getString("premium.permission", "insurance.premium");
    this.defaultEffectId =
        c.getString("premium.default-effect", "default").toLowerCase(Locale.ROOT);

    this.maxCharges = 1;

    this.guiOpenCooldownMillis = Math.max(0L, c.getLong("gui.open-cooldown-ms", 2000L));
    this.guiSize = normalizeGuiSize(c.getInt("gui.size", 27));
    this.guiTitle = c.getString("gui.title", "<reset><#f5f242><bold>Ubezpieczenie");
    this.guiFillEmpty = c.getBoolean("gui.fill-empty", true);
    this.guiFillerMaterial =
        material(
            c.getString("gui.filler.material", "YELLOW_STAINED_GLASS_PANE"),
            Material.YELLOW_STAINED_GLASS_PANE);
    this.guiFillerName = c.getString("gui.filler.name", " ");
    this.guiInsuranceSlot = clamp(c.getInt("gui.insurance-item.slot", 13), 0, guiSize - 1);
    this.guiInsuranceActiveSlot =
        clamp(c.getInt("gui.insurance-item.active-slot", 12), 0, guiSize - 1);
    this.guiInsuranceMaterial =
        material(
            c.getString("gui.insurance-item.material", "TOTEM_OF_UNDYING"),
            Material.TOTEM_OF_UNDYING);
    this.guiInsuranceName =
        c.getString("gui.insurance-item.name", "<reset><#f5f242><bold>Ubezpieczenie");
    this.guiInsuranceLore =
        c.getStringList("gui.insurance-item.lore").stream().map(this::sanitizeText).toList();
    if (guiInsuranceLore.isEmpty()) {
      guiInsuranceLore =
          List.of(
              "<reset><white>Jednorazowa ochrona przed śmiercią.",
              "<reset><white>Aktywuje się automatycznie przy zagrożeniu życia.",
              "<reset><white>Po użyciu polisa wygasa.",
              "",
              "<reset><white>Koszt: <reset><#f5f242>{price}{currency}",
              "<reset><white>Status: {status}",
              "<reset><white>Ważna jeszcze: <reset><#f5f242>{time_left}");
    }
    this.guiClickToBuyLine =
        sanitizeText(
            c.getString("gui.insurance-item.click-to-buy", "<reset><#f5f242>Kliknij, aby kupić."));
    this.guiAlreadyOwnedLine =
        sanitizeText(
            c.getString(
                "gui.insurance-item.already-owned", "<reset><white>Masz już aktywną polisę."));

    boolean layoutMigrated = false;
    int configuredRenewSlot = c.getInt("gui.renew-button.slot", 14);
    if (configuredRenewSlot == 4) {
      configuredRenewSlot = 14;
      c.set("gui.renew-button.slot", configuredRenewSlot);
      layoutMigrated = true;
    }
    this.guiRenewSlot = clamp(configuredRenewSlot, 0, guiSize - 1);
    this.guiRenewMaterial =
        material(c.getString("gui.renew-button.material", "CLOCK"), Material.CLOCK);
    this.guiRenewName =
        sanitizeText(c.getString("gui.renew-button.name", "<reset><#f5f242><bold>Przedłuż polisę"));
    this.guiRenewLore =
        c.getStringList("gui.renew-button.lore").stream().map(this::sanitizeText).toList();
    this.guiCloseSlot = clamp(c.getInt("gui.close-button.slot", guiSize - 1), 0, guiSize - 1);
    this.guiCloseMaterial =
        material(c.getString("gui.close-button.material", "BARRIER"), Material.BARRIER);
    this.guiCloseName =
        sanitizeText(c.getString("gui.close-button.name", "<reset><red><bold>Wyjście"));
    this.guiCloseLore =
        c.getStringList("gui.close-button.lore").stream().map(this::sanitizeText).toList();

    int configuredEffectSlot = c.getInt("gui.effect-menu-button.slot", 18);
    if (configuredEffectSlot == 22) {
      configuredEffectSlot = 18;
      c.set("gui.effect-menu-button.slot", configuredEffectSlot);
      layoutMigrated = true;
    }
    this.guiEffectButtonSlot = clamp(configuredEffectSlot, 0, guiSize - 1);
    this.guiEffectButtonMaterial =
        material(
            c.getString("gui.effect-menu-button.material", "AMETHYST_CLUSTER"),
            Material.AMETHYST_CLUSTER);
    this.guiEffectButtonName =
        sanitizeText(
            c.getString("gui.effect-menu-button.name", "<reset><#f5f242><bold>Efekt ratunku"));
    this.guiEffectButtonLore =
        c.getStringList("gui.effect-menu-button.lore").stream().map(this::sanitizeText).toList();
    this.guiEffectsTitle =
        sanitizeText(c.getString("gui.effects-menu.title", "<reset><#f5f242><bold>Wybór efektu"));
    this.guiBackMaterial =
        material(c.getString("gui.effects-menu.back.material", "ARROW"), Material.ARROW);
    this.guiBackName =
        sanitizeText(c.getString("gui.effects-menu.back.name", "<reset><red><bold>Powrót"));
    this.guiBackLore =
        c.getStringList("gui.effects-menu.back.lore").stream().map(this::sanitizeText).toList();
    if (layoutMigrated) {
      config.save();
    }

    this.npcProvider = c.getString("npc.provider", "CITIZENS").toUpperCase(Locale.ROOT);
    this.npcEntityType = entityType(c.getString("npc.entity-type", "VILLAGER"));
    this.npcName = c.getString("npc.name", "<reset><#f5f242><bold>Ubezpieczenia");
    String configuredCitizensName = c.getString("npc.citizens-name", "");
    this.npcCitizensName =
        configuredCitizensName == null
                || configuredCitizensName.isBlank()
                || !configuredCitizensName.contains("&")
            ? npcName
            : configuredCitizensName;
    this.npcSkin = c.getString("npc.skin", "Steve");
    this.npcGlowing = c.getBoolean("npc.glowing", false);

    this.restoreHealth = Math.max(1.0, c.getDouble("protection.restore-health", 8.0));
    this.restoreFood = c.getBoolean("protection.restore-food", true);
    this.restoreFoodLevel = clamp(c.getInt("protection.food-level", 20), 0, 20);
    this.restoreSaturation = (float) Math.max(0.0, c.getDouble("protection.saturation", 6.0));
    this.extinguish = c.getBoolean("protection.extinguish", true);
    this.clearNegativeEffects = c.getBoolean("protection.clear-negative-effects", false);
    this.noDamageTicks = Math.max(0, c.getInt("protection.no-damage-ticks", 60));
    this.teleportOnVoid = c.getBoolean("protection.teleport-on-void", true);
    this.protectionSound =
        sound(c.getString("protection.sound", "ITEM_TOTEM_USE"), Sound.ITEM_TOTEM_USE);
    this.soundOpen = sound(c.getString("sounds.open", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK);
    this.soundPurchase =
        sound(c.getString("sounds.purchase", "ENTITY_PLAYER_LEVELUP"), Sound.ENTITY_PLAYER_LEVELUP);
    this.soundRenewed =
        sound(
            c.getString("sounds.renewed", "BLOCK_NOTE_BLOCK_PLING"), Sound.BLOCK_NOTE_BLOCK_PLING);
    this.soundAlreadyActive =
        sound(c.getString("sounds.already-active", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO);
    this.soundNoMoney =
        sound(c.getString("sounds.no-money", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO);
    this.soundError =
        sound(c.getString("sounds.error", "BLOCK_ANVIL_LAND"), Sound.BLOCK_ANVIL_LAND);
    this.soundTotemBlocked =
        sound(c.getString("sounds.totem-blocked", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO);
    this.protectionParticle =
        particle(c.getString("protection.particle", "TOTEM_OF_UNDYING"), Particle.TOTEM_OF_UNDYING);
    this.protectionParticleCount = Math.max(0, c.getInt("protection.particle-count", 80));

    loadPremiumConfig(c);

    ignoredCauses.clear();
    for (String cause : c.getStringList("protection.ignored-damage-causes")) {
      ignoredCauses.add(cause.toUpperCase(Locale.ROOT));
    }

    protectionEffects.clear();
    ConfigurationSection effects = c.getConfigurationSection("protection.effects");
    if (effects != null) {
      for (String key : effects.getKeys(false)) {
        PotionEffectType type = PotionEffectType.getByName(key.toUpperCase(Locale.ROOT));
        if (type == null) continue;
        int duration = Math.max(1, effects.getInt(key + ".duration", 200));
        int amplifier = Math.max(0, effects.getInt(key + ".amplifier", 0));
        boolean ambient = effects.getBoolean(key + ".ambient", false);
        boolean particles = effects.getBoolean(key + ".particles", true);
        boolean icon = effects.getBoolean(key + ".icon", true);
        protectionEffects.add(
            new PotionEffect(type, duration, amplifier, ambient, particles, icon));
      }
    }

    messages = new HashMap<>();
    ConfigurationSection msg = c.getConfigurationSection("messages");
    if (msg != null) {
      for (String key : msg.getKeys(false)) {
        messages.put(key, sanitizeText(msg.getString(key, "")));
      }
    }
  }

  private void loadPremiumConfig(FileConfiguration c) {
    rescueEffects.clear();

    rescueEffects.put(
        "default",
        new RescueEffect(
            "default",
            31,
            Material.TOTEM_OF_UNDYING,
            "<reset><white><bold>Standardowy",
            List.of("<reset><white>Klasyczny efekt polisy."),
            protectionParticle,
            protectionParticleCount,
            protectionSound));

    ConfigurationSection effects = c.getConfigurationSection("premium.effects");
    if (effects != null) {
      for (String id : effects.getKeys(false)) {
        String path = "premium.effects." + id;
        int slot = clamp(c.getInt(path + ".slot", 31), 0, guiSize - 1);
        Material effectMaterial =
            material(c.getString(path + ".material", "NETHER_STAR"), Material.NETHER_STAR);
        String name = sanitizeText(c.getString(path + ".name", "<reset><#f5f242><bold>" + id));
        List<String> lore =
            c.getStringList(path + ".lore").stream().map(this::sanitizeText).toList();
        Particle effectParticle =
            particle(
                c.getString(path + ".particle", protectionParticle.name()), protectionParticle);
        int effectParticleCount =
            Math.max(0, c.getInt(path + ".particle-count", protectionParticleCount));
        Sound effectSound =
            sound(c.getString(path + ".sound", protectionSound.name()), protectionSound);
        rescueEffects.put(
            id.toLowerCase(Locale.ROOT),
            new RescueEffect(
                id.toLowerCase(Locale.ROOT),
                slot,
                effectMaterial,
                name,
                lore,
                effectParticle,
                effectParticleCount,
                effectSound));
      }
    }
    rescueEffects.putIfAbsent(
        "lightning",
        new RescueEffect(
            "lightning",
            clamp(31, 0, guiSize - 1),
            Material.LIGHTNING_ROD,
            "<reset><aqua><bold>Piorun",
            List.of("<reset><white>Efekt ratunku z błyskiem i grzmotem."),
            particle("FLASH", Particle.FLASH),
            1,
            sound("ENTITY_LIGHTNING_BOLT_THUNDER", Sound.ENTITY_LIGHTNING_BOLT_THUNDER)));
    rescueEffects.putIfAbsent(
        "smoke",
        new RescueEffect(
            "smoke",
            clamp(32, 0, guiSize - 1),
            Material.CAMPFIRE,
            "<reset><dark_gray><bold>Dym",
            List.of("<reset><white>Efekt ratunku z dymną teleportacją."),
            particle("CAMPFIRE_COSY_SMOKE", Particle.CAMPFIRE_COSY_SMOKE),
            60,
            sound("ENTITY_ENDERMAN_TELEPORT", Sound.ENTITY_ENDERMAN_TELEPORT)));
    rescueEffects.putIfAbsent(
        "magic",
        new RescueEffect(
            "magic",
            clamp(33, 0, guiSize - 1),
            Material.AMETHYST_SHARD,
            "<reset><light_purple><bold>Magia",
            List.of("<reset><white>Efekt ratunku z magicznymi cząsteczkami."),
            particle("WITCH", Particle.TOTEM_OF_UNDYING),
            80,
            sound("ENTITY_PLAYER_LEVELUP", Sound.ENTITY_PLAYER_LEVELUP)));
    if (!rescueEffects.containsKey(defaultEffectId)) defaultEffectId = "default";
  }

  public void openGui(Player player) {
    if (player == null) return;
    if (!player.hasPermission("skyrise.insurance.use")) {
      send(player, "no-permission");
      return;
    }
    if (!canOpenGui(player.getUniqueId())) return;
    new InsuranceMenu(this, player).open();
    playSound(player, soundOpen);
  }

  private boolean canOpenGui(UUID uuid) {
    if (uuid == null || guiOpenCooldownMillis <= 0L) return true;
    long now = System.currentTimeMillis();
    Long last = guiOpenCooldowns.get(uuid);
    if (last != null && now - last < guiOpenCooldownMillis) return false;
    guiOpenCooldowns.put(uuid, now);
    return true;
  }

  public void clearGuiCooldown(UUID uuid) {
    if (uuid != null) guiOpenCooldowns.remove(uuid);
  }

  public PurchaseResult purchase(Player player) {
    if (player != null && hasInsurance(player.getUniqueId())) {
      return PurchaseResult.ALREADY_ACTIVE;
    }
    return purchaseInternal(player, 1, price);
  }

  public PurchaseResult renewPolicy(Player player) {
    if (player == null) return PurchaseResult.ERROR;
    if (!player.hasPermission("skyrise.insurance.use")) return PurchaseResult.NO_PERMISSION;
    UUID uuid = player.getUniqueId();
    InsurancePolicy policy = getActivePolicy(uuid);
    if (policy == null) return PurchaseResult.NO_ACTIVE_POLICY;
    if (!allowRenewal || !isExpiringSoon(uuid)) return PurchaseResult.RENEW_NOT_AVAILABLE;

    Economy economy = VaultHook.getEconomy();
    if (economy == null) return PurchaseResult.ERROR;
    if (renewalPrice > 0.0 && !economy.has(player, renewalPrice)) return PurchaseResult.NO_MONEY;
    if (renewalPrice > 0.0 && !economy.withdrawPlayer(player, renewalPrice).transactionSuccess())
      return PurchaseResult.NO_MONEY;

    policy.setExpiresAt(System.currentTimeMillis() + durationMillis);
    queueDataSave();
    return PurchaseResult.RENEWED;
  }

  private PurchaseResult purchaseInternal(Player player, int charges, double cost) {
    if (player == null) return PurchaseResult.ERROR;
    if (!player.hasPermission("skyrise.insurance.use")) return PurchaseResult.NO_PERMISSION;

    boolean alreadyActive = hasInsurance(player.getUniqueId());
    if (alreadyActive) {
      return PurchaseResult.ALREADY_ACTIVE;
    }

    Economy economy = VaultHook.getEconomy();
    if (economy == null) return PurchaseResult.ERROR;

    if (cost > 0.0 && !economy.has(player, cost)) return PurchaseResult.NO_MONEY;
    if (cost > 0.0 && !economy.withdrawPlayer(player, cost).transactionSuccess())
      return PurchaseResult.NO_MONEY;

    UUID uuid = player.getUniqueId();
    long newExpiresAt = System.currentTimeMillis() + durationMillis;
    InsurancePolicy policy = insuredPlayers.get(uuid);
    if (policy == null || !policy.isActive(System.currentTimeMillis())) {
      String effectId = policy != null ? policy.getEffectId() : defaultEffectId;
      insuredPlayers.put(
          uuid, new InsurancePolicy(newExpiresAt, clampCharges(Math.max(1, charges)), effectId));
      expiredPolicyNotifications.remove(uuid);
      queueDataSave();
      return PurchaseResult.SUCCESS;
    }

    policy.setCharges(Math.max(1, policy.getCharges()));
    policy.setExpiresAt(newExpiresAt);
    queueDataSave();
    return PurchaseResult.RENEWED;
  }

  public boolean hasInsurance(UUID uuid) {
    InsurancePolicy policy = getActivePolicy(uuid);
    return policy != null;
  }

  public boolean consumeInsurance(UUID uuid) {
    InsurancePolicy policy = getActivePolicy(uuid);
    if (policy == null) return false;
    boolean consumed = policy.consumeCharge();
    if (!consumed) return false;
    if (policy.getCharges() <= 0) {
      insuredPlayers.remove(uuid);
    }
    queueDataSave();
    return true;
  }

  public InsurancePolicy getActivePolicy(UUID uuid) {
    if (uuid == null) return null;
    InsurancePolicy policy = insuredPlayers.get(uuid);
    if (policy == null) return null;
    if (policy.getCharges() > maxCharges) {
      policy.setCharges(maxCharges);
      queueDataSave();
    }
    if (policy.getCharges() <= 0) return null;
    if (policy.getExpiresAt() <= System.currentTimeMillis()) {
      expirePolicy(uuid, policy, true);
      return null;
    }
    return policy;
  }

  public long getExpiresAt(UUID uuid) {
    InsurancePolicy policy = getActivePolicy(uuid);
    return policy != null ? policy.getExpiresAt() : 0L;
  }

  public int getCharges(UUID uuid) {
    InsurancePolicy policy = getActivePolicy(uuid);
    return policy != null ? policy.getCharges() : 0;
  }

  public long getRemainingMillis(UUID uuid) {
    long expiresAt = getExpiresAt(uuid);
    return expiresAt <= 0L ? 0L : Math.max(0L, expiresAt - System.currentTimeMillis());
  }

  public boolean isExpiringSoon(UUID uuid) {
    long remaining = getRemainingMillis(uuid);
    return remaining > 0L && remaining <= warningBeforeMillis;
  }

  public void handleJoin(Player player) {
    if (player == null) return;
    UUID uuid = player.getUniqueId();
    long now = System.currentTimeMillis();

    InsurancePolicy policy = insuredPlayers.get(uuid);
    if (policy != null && policy.getCharges() > 0 && policy.getExpiresAt() <= now) {
      expirePolicy(uuid, policy, false);
    }

    Long expiredAt = expiredPolicyNotifications.remove(uuid);
    if (expiredAt != null) {
      send(player, "policy-expired");
      queueDataSave();
      return;
    }

    if (hasInsurance(uuid) && isExpiringSoon(uuid)) {
      send(player, "expiry-warning");
    }
  }

  private boolean expirePolicy(UUID uuid, InsurancePolicy policy, boolean notifyOnline) {
    if (uuid == null || policy == null || policy.getCharges() <= 0) return false;
    policy.setCharges(0);
    long expiredAt =
        policy.getExpiresAt() > 0L ? policy.getExpiresAt() : System.currentTimeMillis();

    Player online = Bukkit.getPlayer(uuid);
    if (notifyOnline && online != null && online.isOnline()) {
      send(online, "policy-expired");
    } else {
      expiredPolicyNotifications.put(uuid, expiredAt);
    }
    queueDataSave();
    return true;
  }

  public void grantInsurance(UUID uuid, long days) {
    grantInsurance(uuid, days, 1);
  }

  public void grantInsurance(UUID uuid, long days, int charges) {
    if (uuid == null) return;
    long millis = Math.max(1L, days) * 24L * 60L * 60L * 1000L;
    InsurancePolicy existing = insuredPlayers.get(uuid);
    String effectId = existing != null ? existing.getEffectId() : defaultEffectId;
    insuredPlayers.put(
        uuid,
        new InsurancePolicy(
            System.currentTimeMillis() + millis, clampCharges(Math.max(1, charges)), effectId));
    expiredPolicyNotifications.remove(uuid);
    queueDataSave();
  }

  public void setInsuranceSeconds(UUID uuid, long seconds) {
    if (uuid == null) return;
    InsurancePolicy existing = insuredPlayers.get(uuid);
    int charges = existing != null ? Math.max(1, existing.getCharges()) : 1;
    String effectId = existing != null ? existing.getEffectId() : defaultEffectId;
    insuredPlayers.put(
        uuid,
        new InsurancePolicy(
            System.currentTimeMillis() + Math.max(1L, seconds) * 1000L,
            clampCharges(charges),
            effectId));
    expiredPolicyNotifications.remove(uuid);
    queueDataSave();
  }

  public void setCharges(UUID uuid, int charges) {
    if (uuid == null) return;
    InsurancePolicy existing = insuredPlayers.get(uuid);
    long expiresAt =
        existing != null && existing.getExpiresAt() > System.currentTimeMillis()
            ? existing.getExpiresAt()
            : System.currentTimeMillis() + durationMillis;
    String effectId = existing != null ? existing.getEffectId() : defaultEffectId;
    int clampedCharges = clampCharges(Math.max(0, charges));
    insuredPlayers.put(uuid, new InsurancePolicy(expiresAt, clampedCharges, effectId));
    if (clampedCharges <= 0) {
      insuredPlayers.remove(uuid);
    } else {
      expiredPolicyNotifications.remove(uuid);
    }
    queueDataSave();
  }

  public boolean setSelectedEffect(UUID uuid, String effectId) {
    if (uuid == null || effectId == null) return false;
    String id = effectId.toLowerCase(Locale.ROOT);
    if (!rescueEffects.containsKey(id)) return false;
    InsurancePolicy existing = insuredPlayers.get(uuid);
    if (existing == null) {
      insuredPlayers.put(uuid, new InsurancePolicy(0L, 0, id));
    } else {
      existing.setEffectId(id);
    }
    queueDataSave();
    return true;
  }

  public String getSelectedEffectId(UUID uuid) {
    InsurancePolicy policy = insuredPlayers.get(uuid);
    return policy != null ? policy.getEffectId() : defaultEffectId;
  }

  public boolean removeInsurance(UUID uuid) {
    if (uuid == null) return false;
    boolean removed = insuredPlayers.remove(uuid) != null;
    boolean notificationRemoved = expiredPolicyNotifications.remove(uuid) != null;
    if (removed || notificationRemoved) queueDataSave();
    return removed || notificationRemoved;
  }

  private int clampCharges(int charges) {
    return Math.max(0, Math.min(maxCharges, charges));
  }

  public void applyProtection(
      Player player, org.bukkit.event.entity.EntityDamageEvent.DamageCause cause) {
    applyProtection(player, cause, getSelectedEffectId(player.getUniqueId()));
  }

  public void applyProtection(
      Player player, org.bukkit.event.entity.EntityDamageEvent.DamageCause cause, String effectId) {
    RescueEffect rescueEffect =
        rescueEffects.getOrDefault(
            effectId == null ? defaultEffectId : effectId.toLowerCase(Locale.ROOT),
            rescueEffects.get("default"));
    if (cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID && teleportOnVoid) {
      Location spawn = player.getWorld().getSpawnLocation().clone().add(0.5, 0.0, 0.5);
      player.teleport(spawn);
    }

    player.setFallDistance(0.0f);
    player.setFireTicks(extinguish ? 0 : player.getFireTicks());
    player.setRemainingAir(player.getMaximumAir());
    player.setNoDamageTicks(noDamageTicks);

    double maxHealth = Math.max(1.0, player.getMaxHealth());
    player.setHealth(Math.min(maxHealth, restoreHealth));

    if (restoreFood) {
      player.setFoodLevel(restoreFoodLevel);
      player.setSaturation(restoreSaturation);
    }

    if (clearNegativeEffects) {
      for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
        if (isNegativeEffect(effect.getType())) {
          player.removePotionEffect(effect.getType());
        }
      }
    }

    for (PotionEffect effect : protectionEffects) {
      player.addPotionEffect(effect);
    }

    if (rescueEffect != null && rescueEffect.id().equalsIgnoreCase("lightning")) {
      player.getWorld().strikeLightningEffect(player.getLocation());
    }
    if (rescueEffect != null && rescueEffect.sound() != null) {
      player.getWorld().playSound(player.getLocation(), rescueEffect.sound(), 1.0f, 1.0f);
    }
    if (rescueEffect != null
        && rescueEffect.particle() != null
        && rescueEffect.particleCount() > 0) {
      player
          .getWorld()
          .spawnParticle(
              rescueEffect.particle(),
              player.getLocation().add(0, 1, 0),
              rescueEffect.particleCount(),
              0.6,
              0.8,
              0.6,
              0.08);
    }

    send(player, "used");
  }

  public boolean shouldIgnoreCause(org.bukkit.event.entity.EntityDamageEvent.DamageCause cause) {
    return cause != null && ignoredCauses.contains(cause.name().toUpperCase(Locale.ROOT));
  }

  public void setNpc(String id, Location location) {
    id = normalizeId(id);
    NpcPoint point = new NpcPoint(id, location, null);
    Entity entity = spawnNpc(point);
    if (entity != null) point.setEntityUuid(entity.getUniqueId());
    npcPoints.put(id, point);
    saveDataNow();
  }

  public boolean removeNpc(String id) {
    id = normalizeId(id);
    NpcPoint removed = npcPoints.remove(id);
    if (removed != null) destroyCitizensNpc(removed.getCitizensId());
    int removedEntities = npcRegistry.removeNpc(MODULE_ID, id);
    if (removed != null || removedEntities > 0) {
      saveDataNow();
      return true;
    }
    return false;
  }

  public void respawnAllNpcs() {
    for (NpcPoint point : npcPoints.values()) {
      destroyCitizensNpc(point.getCitizensId());
      point.setCitizensId(null);
    }
    npcRegistry.removeAll(MODULE_ID);
    for (NpcPoint point : npcPoints.values()) {
      Entity entity = spawnNpc(point);
      if (entity != null) point.setEntityUuid(entity.getUniqueId());
    }
    queueDataSave();
  }

  public void setNpcSkin(String skin) {
    if (skin == null || skin.isBlank()) return;
    this.npcSkin = skin;
    config.getConfig().set("npc.skin", skin);
    config.save();
    respawnAllNpcs();
  }

  public boolean respawnNpc(String id) {
    id = normalizeId(id);
    NpcPoint point = npcPoints.get(id);
    if (point == null) return false;
    destroyCitizensNpc(point.getCitizensId());
    point.setCitizensId(null);
    Entity entity = spawnNpc(point);
    if (entity != null) {
      point.setEntityUuid(entity.getUniqueId());
      saveDataNow();
      return true;
    }
    return false;
  }

  private Entity spawnNpc(NpcPoint point) {
    if (point == null || !point.isWorldLoaded()) return null;
    if (shouldUseCitizens()) {
      Entity citizens = spawnCitizensNpc(point);
      if (citizens != null) return citizens;
      plugin
          .getLogger()
          .warning("Ubezpieczenie: Citizens NPC nie został utworzony, używam natywnego NPC.");
    }
    return npcRegistry.spawnNpc(
        MODULE_ID,
        point.getId(),
        point.getLocation(),
        npcEntityType,
        InsuranceText.component(npcName),
        e -> e.setGlowing(npcGlowing));
  }

  private boolean shouldUseCitizens() {
    if (npcProvider.equals("NATIVE")) return false;
    return CitizensHook.isEnabled()
        && (npcProvider.equals("CITIZENS") || npcProvider.equals("AUTO"));
  }

  private Entity spawnCitizensNpc(NpcPoint point) {
    CitizensHook.CreatedNpc created =
        CitizensHook.createPlayerNpc(
            MODULE_ID, point.getId(), point.getLocation(), npcCitizensName, npcSkin, npcGlowing);
    if (created == null) return null;
    point.setCitizensId(created.citizensId());
    point.setEntityUuid(created.entityUuid());
    return created.entity();
  }

  private void destroyCitizensNpc(Integer citizensId) {
    if (citizensId == null) return;
    CitizensHook.destroyNpc(citizensId);
  }

  private void startCleanupTask() {
    if (cleanupTask != null) cleanupTask.cancel();
    cleanupTask =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin, this::cleanupExpiredPolicies, cleanupIntervalTicks, cleanupIntervalTicks);
  }

  private void restartCleanupTask() {
    if (cleanupTask != null) {
      cleanupTask.cancel();
      cleanupTask = null;
    }
    startCleanupTask();
  }

  private void startWarningTask() {
    if (warningTask != null) warningTask.cancel();
    warningTask =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                this::sendOnlineExpiryWarnings,
                onlineWarningIntervalTicks,
                onlineWarningIntervalTicks);
  }

  private void restartWarningTask() {
    if (warningTask != null) {
      warningTask.cancel();
      warningTask = null;
    }
    startWarningTask();
  }

  public void sendOnlineExpiryWarnings() {
    long now = System.currentTimeMillis();
    for (Player player : Bukkit.getOnlinePlayers()) {
      InsurancePolicy policy = insuredPlayers.get(player.getUniqueId());
      if (policy != null && policy.getCharges() > 0 && policy.getExpiresAt() <= now) {
        expirePolicy(player.getUniqueId(), policy, true);
        continue;
      }
      if (isExpiringSoon(player.getUniqueId())) {
        send(player, "expiry-warning");
      }
    }
  }

  public void cleanupExpiredPolicies() {
    long now = System.currentTimeMillis();
    for (Map.Entry<UUID, InsurancePolicy> entry : insuredPlayers.entrySet()) {
      InsurancePolicy policy = entry.getValue();
      if (policy.getCharges() > 0 && policy.getExpiresAt() <= now) {
        expirePolicy(entry.getKey(), policy, true);
      }
    }
  }

  private void loadData() {
    insuredPlayers.clear();
    expiredPolicyNotifications.clear();
    npcPoints.clear();
    ensureDataFile();

    FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
    long now = System.currentTimeMillis();

    for (String raw : data.getStringList("insured-players")) {
      try {
        insuredPlayers.put(
            UUID.fromString(raw),
            new InsurancePolicy(now + durationMillis, clampCharges(1), defaultEffectId));
      } catch (IllegalArgumentException ignored) {
      }
    }

    ConfigurationSection policies = data.getConfigurationSection("policies");
    if (policies != null) {
      for (String rawUuid : policies.getKeys(false)) {
        try {
          UUID uuid = UUID.fromString(rawUuid);
          long expiresAt = policies.getLong(rawUuid + ".expires-at", 0L);
          int charges = clampCharges(Math.max(0, policies.getInt(rawUuid + ".charges", 1)));
          String effectId = policies.getString(rawUuid + ".effect", defaultEffectId);
          InsurancePolicy policy = new InsurancePolicy(expiresAt, charges, effectId);
          if (policy.isActive(now)) {
            insuredPlayers.put(uuid, policy);
          } else {
            if (charges > 0 && expiresAt <= now) {
              expiredPolicyNotifications.put(uuid, expiresAt > 0L ? expiresAt : now);
              policy.setCharges(0);
            }
            if (!policy.getEffectId().equalsIgnoreCase(defaultEffectId)) {
              insuredPlayers.put(uuid, policy);
            }
          }
        } catch (IllegalArgumentException ignored) {
        }
      }
    }

    ConfigurationSection expiredNotifications =
        data.getConfigurationSection("expired-notifications");
    if (expiredNotifications != null) {
      for (String rawUuid : expiredNotifications.getKeys(false)) {
        try {
          UUID uuid = UUID.fromString(rawUuid);
          expiredPolicyNotifications.put(uuid, expiredNotifications.getLong(rawUuid, now));
        } catch (IllegalArgumentException ignored) {
        }
      }
    }

    ConfigurationSection npcs = data.getConfigurationSection("npcs");
    if (npcs != null) {
      for (String id : npcs.getKeys(false)) {
        ConfigurationSection sec = npcs.getConfigurationSection(id);
        if (sec == null) continue;
        Location loc =
            NpcPoint.location(
                sec.getString("world"),
                sec.getDouble("x"),
                sec.getDouble("y"),
                sec.getDouble("z"),
                (float) sec.getDouble("yaw"),
                (float) sec.getDouble("pitch"));
        if (loc == null) {
          plugin
              .getLogger()
              .warning("Ubezpieczenie: pominięto NPC '" + id + "' — świat nie jest załadowany.");
          continue;
        }
        UUID entityUuid = null;
        String entityRaw = sec.getString("entity-uuid");
        if (entityRaw != null) {
          try {
            entityUuid = UUID.fromString(entityRaw);
          } catch (IllegalArgumentException ignored) {
          }
        }
        Integer citizensId = sec.contains("citizens-id") ? sec.getInt("citizens-id") : null;
        npcPoints.put(normalizeId(id), new NpcPoint(normalizeId(id), loc, entityUuid, citizensId));
      }
    }
  }

  public void saveDataNow() {
    if (queuedSaveTask != null) {
      queuedSaveTask.cancel();
      queuedSaveTask = null;
    }
    ensureDataFile();
    FileConfiguration data = new YamlConfiguration();
    long now = System.currentTimeMillis();
    for (Map.Entry<UUID, InsurancePolicy> entry : insuredPlayers.entrySet()) {
      InsurancePolicy policy = entry.getValue();
      if (policy.getCharges() > 0 && policy.getExpiresAt() <= now) {
        policy.setCharges(0);
        expiredPolicyNotifications.putIfAbsent(
            entry.getKey(), policy.getExpiresAt() > 0L ? policy.getExpiresAt() : now);
      }
    }
    for (Map.Entry<UUID, InsurancePolicy> entry : insuredPlayers.entrySet()) {
      InsurancePolicy policy = entry.getValue();
      if (policy.getCharges() <= 0 && policy.getEffectId().equalsIgnoreCase(defaultEffectId))
        continue;
      String path = "policies." + entry.getKey();
      data.set(path + ".expires-at", policy.getExpiresAt());
      data.set(path + ".charges", policy.getCharges());
      data.set(path + ".effect", policy.getEffectId());
    }
    for (Map.Entry<UUID, Long> entry : expiredPolicyNotifications.entrySet()) {
      data.set("expired-notifications." + entry.getKey(), entry.getValue());
    }

    for (NpcPoint point : npcPoints.values()) {
      Location loc = point.getLocation();
      if (loc == null || loc.getWorld() == null) continue;
      String path = "npcs." + point.getId();
      data.set(path + ".world", loc.getWorld().getName());
      data.set(path + ".x", loc.getX());
      data.set(path + ".y", loc.getY());
      data.set(path + ".z", loc.getZ());
      data.set(path + ".yaw", loc.getYaw());
      data.set(path + ".pitch", loc.getPitch());
      data.set(
          path + ".entity-uuid",
          point.getEntityUuid() != null ? point.getEntityUuid().toString() : null);
      data.set(path + ".citizens-id", point.getCitizensId());
    }

    try {
      data.save(dataFile);
    } catch (IOException e) {
      plugin.getLogger().severe("Nie można zapisać danych modułu Ubezpieczenie: " + e.getMessage());
    }
  }

  public void queueDataSave() {
    if (queuedSaveTask != null) return;
    queuedSaveTask = Bukkit.getScheduler().runTaskLater(plugin, this::saveDataNow, 20L * 10L);
  }

  public void flushDataSave() {
    if (queuedSaveTask != null) {
      queuedSaveTask.cancel();
      queuedSaveTask = null;
    }
    saveDataNow();
  }

  private void ensureDataFile() {
    if (dataFile == null) {
      dataFile = new File(plugin.getDataFolder(), "insurance" + File.separator + "data.yml");
    }
    migrateLegacyDataFile();
    if (!dataFile.exists()) {
      try {
        File parent = dataFile.getParentFile();
        if (parent != null) parent.mkdirs();
        dataFile.createNewFile();
      } catch (IOException e) {
        plugin
            .getLogger()
            .severe("Nie można utworzyć danych modułu Ubezpieczenie: " + e.getMessage());
      }
    }
  }

  private void migrateLegacyDataFile() {
    File legacyFile =
        new File(plugin.getDataFolder(), "config" + File.separator + "insurance-data.yml");
    if (dataFile.exists() || !legacyFile.exists()) return;
    try {
      File parent = dataFile.getParentFile();
      if (parent != null) parent.mkdirs();
      Files.move(legacyFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      File legacyDir = legacyFile.getParentFile();
      if (legacyDir != null && legacyDir.isDirectory()) {
        String[] children = legacyDir.list();
        if (children == null || children.length == 0) legacyDir.delete();
      }
      plugin.getLogger().info("Ubezpieczenie: przeniesiono dane do insurance/data.yml.");
    } catch (IOException e) {
      plugin
          .getLogger()
          .warning(
              "Ubezpieczenie: nie udało się przenieść starego pliku danych: " + e.getMessage());
    }
  }

  private void playSound(Player player, Sound sound) {
    if (player == null || sound == null) return;
    player.playSound(player.getLocation(), sound, 0.7f, 1.0f);
  }

  public void playPurchaseSound(Player player, PurchaseResult result) {
    switch (result) {
      case SUCCESS -> playSound(player, soundPurchase);
      case RENEWED -> playSound(player, soundRenewed);
      case ALREADY_ACTIVE, NO_ACTIVE_POLICY, RENEW_NOT_AVAILABLE ->
          playSound(player, soundAlreadyActive);
      case NO_MONEY -> playSound(player, soundNoMoney);
      case NO_PERMISSION, ERROR -> playSound(player, soundError);
    }
  }

  public void playTotemBlockedSound(Player player) {
    playSound(player, soundTotemBlocked);
  }

  public void send(Player player, String key) {
    if (player == null) return;
    String raw = replacePlaceholders(message(key), player);
    String formatted = raw.contains("Ubezpieczenie <reset><gray>»") ? raw : prefix + raw;
    player.sendMessage(InsuranceText.component(formatted));
  }

  public String replacePlaceholders(String text, Player player) {
    if (text == null) return "";
    String status =
        player != null && hasInsurance(player.getUniqueId())
            ? message("status-active-raw")
            : message("status-inactive-raw");
    long remaining = player != null ? getRemainingMillis(player.getUniqueId()) : 0L;
    int charges = player != null ? getCharges(player.getUniqueId()) : 0;
    String effectId = player != null ? getSelectedEffectId(player.getUniqueId()) : defaultEffectId;
    RescueEffect effect = rescueEffects.getOrDefault(effectId, rescueEffects.get("default"));
    String effectName = effect != null ? effect.name() : effectId;
    return text.replace("{price}", String.format(Locale.US, "%.2f", price))
        .replace("{renewal_price}", String.format(Locale.US, "%.2f", renewalPrice))
        .replace("{currency}", currencySymbol)
        .replace("{player}", player != null ? player.getName() : "")
        .replace("{status}", status)
        .replace("{charges}", String.valueOf(charges))
        .replace("{max_charges}", String.valueOf(maxCharges))
        .replace("{effect}", effectName)
        .replace("{time_left}", formatDuration(remaining));
  }

  public String formatDuration(long millis) {
    if (millis <= 0L) return "0m";
    long totalMinutes = (long) Math.ceil(millis / 60000.0);
    long days = totalMinutes / 1440L;
    long hours = (totalMinutes % 1440L) / 60L;
    long minutes = totalMinutes % 60L;
    if (days > 0) return days + "d " + hours + "h";
    if (hours > 0) return hours + "h " + minutes + "m";
    return minutes + "m";
  }

  public String message(String key) {
    return messages.getOrDefault(key, "<reset><red>" + key);
  }

  private String getMessageRaw(String key, String fallback) {
    return messages.getOrDefault(key, fallback);
  }

  public String normalizeId(String id) {
    if (id == null || id.isBlank()) return "default";
    return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
  }

  private String sanitizeText(String text) {
    if (text == null) return "";
    return text.replace("Odnów je u NPC.", "Przedłuż ważność swojej polisy u ubezpieczyciela.")
        .replace("Odnów je u NPC", "Przedłuż ważność swojej polisy u ubezpieczyciela")
        .replace("odnów je u NPC.", "Przedłuż ważność swojej polisy u ubezpieczyciela.")
        .replace("odnów je u NPC", "Przedłuż ważność swojej polisy u ubezpieczyciela")
        .replace("Kup je u NPC.", "Aktywuj polisę u ubezpieczyciela.")
        .replace("Kup je u NPC", "Aktywuj polisę u ubezpieczyciela");
  }

  private int normalizeGuiSize(int size) {
    int normalized = Math.max(9, Math.min(54, size));
    return ((normalized + 8) / 9) * 9;
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private Material material(String raw, Material fallback) {
    if (raw == null) return fallback;
    try {
      return Material.valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return fallback;
    }
  }

  private EntityType entityType(String raw) {
    if (raw == null) return EntityType.VILLAGER;
    try {
      return EntityType.valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return EntityType.VILLAGER;
    }
  }

  private Sound sound(String raw, Sound fallback) {
    if (raw == null) return fallback;
    try {
      return Sound.valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return fallback;
    }
  }

  private Particle particle(String raw, Particle fallback) {
    if (raw == null) return fallback;
    try {
      return Particle.valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return fallback;
    }
  }

  private boolean isNegativeEffect(PotionEffectType type) {
    if (type == null || type.getKey() == null) return false;
    String key = type.getKey().getKey().toLowerCase(Locale.ROOT);
    return Set.of(
            "bad_omen",
            "blindness",
            "darkness",
            "hunger",
            "infested",
            "instant_damage",
            "mining_fatigue",
            "nausea",
            "oozing",
            "poison",
            "raid_omen",
            "slowness",
            "trial_omen",
            "unluck",
            "weakness",
            "weaving",
            "wither",
            "wind_charged")
        .contains(key);
  }

  public JavaPlugin getPlugin() {
    return plugin;
  }

  public double getPrice() {
    return price;
  }

  public double getRenewalPrice() {
    return renewalPrice;
  }

  public String getCurrencySymbol() {
    return currencySymbol;
  }

  public Set<UUID> getInsuredPlayers() {
    return Collections.unmodifiableSet(new HashSet<>(insuredPlayers.keySet()));
  }

  public Map<UUID, Long> getPolicyExpirations() {
    Map<UUID, Long> expirations = new HashMap<>();
    long now = System.currentTimeMillis();
    for (Map.Entry<UUID, InsurancePolicy> entry : insuredPlayers.entrySet()) {
      if (entry.getValue().isActive(now)) {
        expirations.put(entry.getKey(), entry.getValue().getExpiresAt());
      }
    }
    return Collections.unmodifiableMap(expirations);
  }

  public Map<String, NpcPoint> getNpcPoints() {
    return Collections.unmodifiableMap(npcPoints);
  }

  public Map<String, RescueEffect> getRescueEffects() {
    return Collections.unmodifiableMap(rescueEffects);
  }

  public String getPremiumPermission() {
    return premiumPermission;
  }

  public int getMaxCharges() {
    return maxCharges;
  }

  public int getGuiSize() {
    return guiSize;
  }

  public String getGuiTitle() {
    return guiTitle;
  }

  public boolean isGuiFillEmpty() {
    return guiFillEmpty;
  }

  public Material getGuiFillerMaterial() {
    return guiFillerMaterial;
  }

  public String getGuiFillerName() {
    return guiFillerName;
  }

  public int getGuiInsuranceSlot() {
    return guiInsuranceSlot;
  }

  public int getGuiInsuranceActiveSlot() {
    return guiInsuranceActiveSlot;
  }

  public Material getGuiInsuranceMaterial() {
    return guiInsuranceMaterial;
  }

  public String getGuiInsuranceName() {
    return guiInsuranceName;
  }

  public List<String> getGuiInsuranceLore() {
    return guiInsuranceLore;
  }

  public String getGuiClickToBuyLine() {
    return guiClickToBuyLine;
  }

  public String getGuiAlreadyOwnedLine() {
    return guiAlreadyOwnedLine;
  }

  public int getGuiRenewSlot() {
    return guiRenewSlot;
  }

  public Material getGuiRenewMaterial() {
    return guiRenewMaterial;
  }

  public String getGuiRenewName() {
    return guiRenewName;
  }

  public List<String> getGuiRenewLore() {
    return guiRenewLore;
  }

  public int getGuiCloseSlot() {
    return guiCloseSlot;
  }

  public Material getGuiCloseMaterial() {
    return guiCloseMaterial;
  }

  public String getGuiCloseName() {
    return guiCloseName;
  }

  public List<String> getGuiCloseLore() {
    return guiCloseLore;
  }

  public int getGuiEffectButtonSlot() {
    return guiEffectButtonSlot;
  }

  public Material getGuiEffectButtonMaterial() {
    return guiEffectButtonMaterial;
  }

  public String getGuiEffectButtonName() {
    return guiEffectButtonName;
  }

  public List<String> getGuiEffectButtonLore() {
    return guiEffectButtonLore;
  }

  public String getGuiEffectsTitle() {
    return guiEffectsTitle;
  }

  public Material getGuiBackMaterial() {
    return guiBackMaterial;
  }

  public String getGuiBackName() {
    return guiBackName;
  }

  public List<String> getGuiBackLore() {
    return guiBackLore;
  }

  public enum PurchaseResult {
    SUCCESS,
    RENEWED,
    NO_ACTIVE_POLICY,
    RENEW_NOT_AVAILABLE,
    ALREADY_ACTIVE,
    NO_MONEY,
    NO_PERMISSION,
    ERROR
  }
}
