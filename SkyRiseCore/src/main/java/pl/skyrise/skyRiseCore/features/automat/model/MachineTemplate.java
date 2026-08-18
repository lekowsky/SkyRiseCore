package pl.skyrise.skyRiseCore.features.automat.model;

import java.util.*;
import org.bukkit.Material;

public class MachineTemplate {

  private String name;
  private String title;
  private int rows;
  private Map<String, VendingItem> items;
  private boolean enabled;
  private String permission;

  private boolean fillEmpty;
  private Material fillerMaterial;
  private String fillerName;
  private boolean border;
  private Material borderMaterial;
  private String borderName;

  private boolean closeButtonEnabled;
  private int closeButtonSlot;
  private Material closeButtonMaterial;
  private String closeButtonName;
  private List<String> closeButtonLore;

  private String nexoFurnitureId;

  private boolean autoRestockEnabled;
  private int autoRestockInterval;
  private int autoRestockAmount;

  public MachineTemplate(String name) {
    this.name = name;
    this.title = "<reset><dark_gray>Automat: " + name;
    this.rows = 5;
    this.items = new LinkedHashMap<>();
    this.enabled = true;
    this.permission = "";

    this.fillEmpty = false;
    this.fillerMaterial = Material.BLACK_STAINED_GLASS_PANE;
    this.fillerName = " ";
    this.border = false;
    this.borderMaterial = Material.BLACK_STAINED_GLASS_PANE;
    this.borderName = " ";

    this.closeButtonEnabled = true;
    this.closeButtonSlot = -1;
    this.closeButtonMaterial = Material.BARRIER;
    this.closeButtonName = "<reset><red>Zamknij";
    this.closeButtonLore = new ArrayList<>(Collections.singletonList("<reset><gray>Wróć do gry"));

    this.nexoFurnitureId = null;

    this.autoRestockEnabled = false;
    this.autoRestockInterval = 30;
    this.autoRestockAmount = 10;
  }

  public int getSize() {
    return rows * 9;
  }

  public void addItem(VendingItem item) {
    items.put(item.getId(), item);
  }

  public void removeItem(String itemId) {
    items.remove(itemId);
  }

  public VendingItem getItem(String itemId) {
    return items.get(itemId);
  }

  public VendingItem getItemBySlot(int slot) {
    for (VendingItem item : items.values()) {
      if (item.getSlot() == slot) return item;
    }
    return null;
  }

  public int getResolvedCloseSlot() {
    return closeButtonSlot >= 0 ? closeButtonSlot : getSize() - 1;
  }

  public Set<Integer> getOccupiedSlots() {
    Set<Integer> slots = new HashSet<>();
    for (VendingItem item : items.values()) slots.add(item.getSlot());
    if (closeButtonEnabled) slots.add(getResolvedCloseSlot());
    return slots;
  }

  public int getNextFreeSlot() {
    Set<Integer> occupied = getOccupiedSlots();
    for (int i = 0; i < getSize(); i++) {
      if (!occupied.contains(i)) return i;
    }
    return -1;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getRows() {
    return rows;
  }

  public void setRows(int rows) {
    this.rows = Math.max(1, Math.min(6, rows));
  }

  public Map<String, VendingItem> getItems() {
    return items;
  }

  public void setItems(Map<String, VendingItem> items) {
    this.items = items;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public boolean isFillEmpty() {
    return fillEmpty;
  }

  public void setFillEmpty(boolean fillEmpty) {
    this.fillEmpty = fillEmpty;
  }

  public Material getFillerMaterial() {
    return fillerMaterial;
  }

  public void setFillerMaterial(Material m) {
    this.fillerMaterial = m;
  }

  public String getFillerName() {
    return fillerName;
  }

  public void setFillerName(String fillerName) {
    this.fillerName = fillerName;
  }

  public boolean isBorder() {
    return border;
  }

  public void setBorder(boolean border) {
    this.border = border;
  }

  public Material getBorderMaterial() {
    return borderMaterial;
  }

  public void setBorderMaterial(Material m) {
    this.borderMaterial = m;
  }

  public String getBorderName() {
    return borderName;
  }

  public void setBorderName(String borderName) {
    this.borderName = borderName;
  }

  public boolean isCloseButtonEnabled() {
    return closeButtonEnabled;
  }

  public void setCloseButtonEnabled(boolean e) {
    this.closeButtonEnabled = e;
  }

  public int getCloseButtonSlot() {
    return closeButtonSlot;
  }

  public void setCloseButtonSlot(int s) {
    this.closeButtonSlot = s;
  }

  public Material getCloseButtonMaterial() {
    return closeButtonMaterial;
  }

  public void setCloseButtonMaterial(Material m) {
    this.closeButtonMaterial = m;
  }

  public String getCloseButtonName() {
    return closeButtonName;
  }

  public void setCloseButtonName(String n) {
    this.closeButtonName = n;
  }

  public List<String> getCloseButtonLore() {
    return closeButtonLore;
  }

  public void setCloseButtonLore(List<String> l) {
    this.closeButtonLore = l;
  }

  public String getNexoFurnitureId() {
    return nexoFurnitureId;
  }

  public void setNexoFurnitureId(String id) {
    this.nexoFurnitureId = id;
  }

  public boolean isAutoRestockEnabled() {
    return autoRestockEnabled;
  }

  public void setAutoRestockEnabled(boolean e) {
    this.autoRestockEnabled = e;
  }

  public int getAutoRestockInterval() {
    return autoRestockInterval;
  }

  public void setAutoRestockInterval(int i) {
    this.autoRestockInterval = Math.max(1, i);
  }

  public int getAutoRestockAmount() {
    return autoRestockAmount;
  }

  public void setAutoRestockAmount(int a) {
    this.autoRestockAmount = Math.max(1, a);
  }
}
