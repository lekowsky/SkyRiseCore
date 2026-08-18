package pl.skyrise.skyRiseCore.core;

import java.util.*;
import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;

public class TabRegistry {

  private final Map<String, BiFunction<CommandSender, String[], List<String>>> providers =
      new HashMap<>();

  private static final List<String> EMPTY = List.of();

  public void register(String command, BiFunction<CommandSender, String[], List<String>> provider) {
    if (command == null || provider == null) return;
    providers.put(command.toLowerCase(Locale.ROOT), provider);
  }

  public void unregister(String command) {
    if (command == null) return;
    providers.remove(command.toLowerCase(Locale.ROOT));
  }

  public List<String> complete(String command, CommandSender sender, String[] args) {
    if (command == null) return EMPTY;
    BiFunction<CommandSender, String[], List<String>> provider =
        providers.get(command.toLowerCase(Locale.ROOT));
    return provider != null ? provider.apply(sender, args) : EMPTY;
  }

  public Set<String> getRegisteredCommands() {
    return Collections.unmodifiableSet(providers.keySet());
  }

  public static List<String> filter(Collection<String> options, String input) {
    if (input.isEmpty()) return List.copyOf(options);
    String lower = input.toLowerCase();
    return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
  }
}
