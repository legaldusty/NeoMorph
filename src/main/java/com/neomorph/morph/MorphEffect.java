package com.neomorph.morph;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MorphEffect {

    private final JavaPlugin plugin;
    private final boolean showParticles;
    private final boolean playSound;

    public MorphEffect(JavaPlugin plugin, boolean showParticles, boolean playSound) {
        this.plugin = plugin;
        this.showParticles = showParticles;
        this.playSound = playSound;
    }

    public void playMorphEffect(Player player) {
        if (playSound) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        }

        if (showParticles) {
            new BukkitRunnable() {
                int tick = 0;
                @Override
                public void run() {
                    if (tick >= 20 || !player.isOnline()) {
                        cancel();
                        return;
                    }
                    Location loc = player.getLocation().add(0, 1, 0);
                    double radius = 1.5 - (tick * 0.05);
                    double angle = tick * 0.5;

                    for (int i = 0; i < 8; i++) {
                        double a = angle + (i * Math.PI / 4);
                        double x = Math.cos(a) * radius;
                        double z = Math.sin(a) * radius;
                        double y = (tick * 0.1) - 0.5;

                        player.getWorld().spawnParticle(
                                Particle.PORTAL,
                                loc.clone().add(x, y, z),
                                1, 0, 0, 0, 0
                        );
                        player.getWorld().spawnParticle(
                                Particle.WITCH,
                                loc.clone().add(x, y, z),
                                1, 0, 0, 0, 0
                        );
                    }
                    tick++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    public void playUnmorphEffect(Player player) {
        if (playSound) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
        }

        if (showParticles) {
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.CLOUD, loc, 30, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.POOF, loc, 20, 0.5, 0.5, 0.5, 0.05);
        }
    }

    public void playAbilityEffect(Player player, Particle particle, int count) {
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(particle, loc, count, 0.3, 0.3, 0.3, 0.05);
    }
}
