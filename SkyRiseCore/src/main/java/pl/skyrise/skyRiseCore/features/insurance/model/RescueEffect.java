package pl.skyrise.skyRiseCore.features.insurance.model;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

public record RescueEffect(
    String id,
    int slot,
    Material material,
    String name,
    List<String> lore,
    Particle particle,
    int particleCount,
    Sound sound) {}
