package pl.skyrise.skyRiseCore.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ColorUtil {

  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private static final LegacyComponentSerializer LEGACY_AMPERSAND =
      LegacyComponentSerializer.builder().hexColors().character('&').build();
  private static final LegacyComponentSerializer LEGACY_SECTION =
      LegacyComponentSerializer.builder().hexColors().character('§').build();
  private static final Pattern LEGACY_FORMAT =
      Pattern.compile("(?i)(?:&#[0-9a-f]{6}|&[0-9a-fk-or])");

  private ColorUtil() {}

  public static Component mini(String text) {
    if (text == null || text.isEmpty()) return Component.empty();
    if (LEGACY_FORMAT.matcher(text).find()) return fromLegacy(text);
    if (text.indexOf('§') >= 0) return fromSectionLegacy(text);
    return MINI.deserialize(text);
  }

  public static String miniToLegacy(String text) {
    return LEGACY_SECTION.serialize(mini(text));
  }

  public static List<String> miniToLegacy(List<String> lines) {
    List<String> formatted = new ArrayList<>();
    if (lines == null) return formatted;
    for (String line : lines) {
      formatted.add(miniToLegacy(line));
    }
    return formatted;
  }

  public static String miniPlain(String text) {
    return plain(mini(text));
  }

  public static String escape(String text) {
    if (text == null || text.isEmpty()) return "";
    return MINI.escapeTags(text);
  }

  public static String plain(Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  public static String stripColor(String text) {
    return miniPlain(text);
  }

  private static Component fromLegacy(String text) {
    return LEGACY_AMPERSAND.deserialize(text).decoration(TextDecoration.ITALIC, false);
  }

  private static Component fromSectionLegacy(String text) {
    return LEGACY_SECTION.deserialize(text).decoration(TextDecoration.ITALIC, false);
  }
}
