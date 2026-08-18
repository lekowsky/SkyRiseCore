package pl.skyrise.skyRiseCore.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomConfig {

  private final JavaPlugin plugin;
  private final String fileName;
  private final String moduleFolderName;
  private final File file;
  private YamlConfiguration config;

  public CustomConfig(JavaPlugin plugin, String fileName) {
    this.plugin = plugin;
    this.fileName = fileName;
    this.moduleFolderName = moduleFolderName(fileName);
    this.file = new File(plugin.getDataFolder(), moduleFolderName + File.separator + "config.yml");
  }

  public void load() {
    migrateLegacyConfigIfNeeded();
    ensureFileExists();
    loadYamlWithBundledDefaults();
  }

  public void save() {
    if (config == null) return;
    try {
      File parent = file.getParentFile();
      if (parent != null) parent.mkdirs();
      config.save(file);
    } catch (IOException e) {
      plugin
          .getLogger()
          .severe("Nie można zapisać configu modułu " + moduleFolderName + ": " + fileName);
      e.printStackTrace();
    }
  }

  public void reload() {
    if (!file.exists()) {
      load();
      return;
    }
    loadYamlWithBundledDefaults();
  }

  public YamlConfiguration getConfig() {
    return config;
  }

  public String getFileName() {
    return fileName;
  }

  public String getModuleFolderName() {
    return moduleFolderName;
  }

  public File getFile() {
    return file;
  }

  private void ensureFileExists() {
    if (file.exists()) return;

    try {
      File parent = file.getParentFile();
      if (parent != null) parent.mkdirs();

      try (InputStream resource = findResource()) {
        if (resource != null) {
          Files.copy(resource, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
          file.createNewFile();
        }
      }
    } catch (IOException e) {
      plugin
          .getLogger()
          .severe("Nie można utworzyć configu modułu " + moduleFolderName + ": " + fileName);
      e.printStackTrace();
    }
  }

  private void loadYamlWithBundledDefaults() {
    config = YamlConfiguration.loadConfiguration(file);
    config.options().copyDefaults(false);

    try (InputStream resource = findResource()) {
      if (resource == null) return;

      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(
              new InputStreamReader(resource, StandardCharsets.UTF_8));
      config.setDefaults(defaults);
    } catch (IOException e) {
      plugin
          .getLogger()
          .warning(
              "Nie można odczytać domyślnego configu z JAR-a " + fileName + ": " + e.getMessage());
    }
  }

  private InputStream findResource() {
    InputStream resource = plugin.getResource("config/" + fileName);
    if (resource != null) return resource;
    return plugin.getResource(fileName);
  }

  private void migrateLegacyConfigIfNeeded() {
    if (file.exists()) return;

    File oldConfigFile = new File(plugin.getDataFolder(), "config" + File.separator + fileName);
    if (oldConfigFile.exists()) {
      moveLegacy(oldConfigFile);
      cleanupEmptyDirectory(oldConfigFile.getParentFile());
      return;
    }

    File oldRootFile = new File(plugin.getDataFolder(), fileName);
    if (oldRootFile.exists()) {
      moveLegacy(oldRootFile);
    }
  }

  private void moveLegacy(File legacyFile) {
    try {
      File parent = file.getParentFile();
      if (parent != null) parent.mkdirs();
      Files.move(legacyFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      plugin
          .getLogger()
          .info(
              "Przeniesiono config modułu "
                  + moduleFolderName
                  + " do "
                  + moduleFolderName
                  + File.separator
                  + "config.yml");
    } catch (IOException e) {
      plugin
          .getLogger()
          .warning(
              "Nie udało się przenieść starego configu "
                  + legacyFile.getPath()
                  + ": "
                  + e.getMessage());
    }
  }

  private void cleanupEmptyDirectory(File dir) {
    if (dir == null || !dir.isDirectory()) return;
    String[] children = dir.list();
    if (children == null || children.length == 0) {
      dir.delete();
    }
  }

  private static String moduleFolderName(String fileName) {
    String name = fileName == null ? "module" : fileName.toLowerCase(Locale.ROOT).trim();
    if (name.endsWith(".yml")) name = name.substring(0, name.length() - 4);
    if (name.endsWith(".yaml")) name = name.substring(0, name.length() - 5);
    name = name.replaceAll("[^a-z0-9_-]", "");
    return name.isBlank() ? "module" : name;
  }
}
