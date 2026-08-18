package pl.skyrise.skyRiseCore.features.insurance.util;

import net.kyori.adventure.text.Component;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

public final class InsuranceText {

  private InsuranceText() {}

  public static Component component(String text) {
    return ColorUtil.mini(text);
  }

  public static String color(String text) {
    return ColorUtil.miniToLegacy(text);
  }
}
