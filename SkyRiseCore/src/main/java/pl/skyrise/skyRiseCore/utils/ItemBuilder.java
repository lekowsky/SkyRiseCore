package pl.skyrise.skyRiseCore.utils;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.skyrise.skyRiseCore.SkyRiseCore;

public final class ItemBuilder {

  private final ItemStack item;
  private final ItemMeta meta;

  public ItemBuilder(Material material) {
    this(material, 1);
  }

  public ItemBuilder(Material material, int amount) {
    this.item = new ItemStack(material, amount);
    this.meta = item.getItemMeta();
  }

  public ItemBuilder name(Component name) {
    if (meta != null) {
      meta.displayName(name);
    }
    return this;
  }

  public ItemBuilder name(String miniMessage) {
    return name(ColorUtil.mini(miniMessage));
  }

  public ItemBuilder lore(Component line) {
    if (meta != null) {
      List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
      lore.add(line);
      meta.lore(lore);
    }
    return this;
  }

  public ItemBuilder lore(List<Component> lines) {
    if (meta != null) {
      meta.lore(lines == null ? List.of() : new ArrayList<>(lines));
    }
    return this;
  }

  public ItemBuilder loreMini(String... lines) {
    List<Component> components = new ArrayList<>();
    if (lines != null) {
      for (String line : lines) {
        components.add(ColorUtil.mini(line));
      }
    }
    return lore(components);
  }

  public ItemBuilder loreMini(List<String> lines) {
    List<Component> components = new ArrayList<>();
    if (lines != null) {
      for (String line : lines) {
        components.add(ColorUtil.mini(line));
      }
    }
    return lore(components);
  }

  public ItemBuilder glow() {
    if (meta != null) {
      meta.addEnchant(Enchantment.UNBREAKING, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
    return this;
  }

  public ItemBuilder amount(int amount) {
    item.setAmount(amount);
    return this;
  }

  public ItemBuilder setStringData(String keyString, String value) {
    if (meta != null) {
      NamespacedKey key = new NamespacedKey(SkyRiseCore.getInstance(), keyString);
      meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
    }
    return this;
  }

  public static String getStringData(ItemStack item, String keyString) {
    if (item == null || !item.hasItemMeta()) return null;
    NamespacedKey key = new NamespacedKey(SkyRiseCore.getInstance(), keyString);
    return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
  }

  public ItemStack build() {
    if (meta != null) {
      item.setItemMeta(meta);
    }
    return item;
  }
}
