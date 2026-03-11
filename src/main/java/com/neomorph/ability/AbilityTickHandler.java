package com.neomorph.ability;

import com.neomorph.morph.MorphManager;
import com.neomorph.morph.MorphSession;
import com.neomorph.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class AbilityTickHandler extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final MorphManager morphManager;
    private int tickCounter = 0;

    public AbilityTickHandler(JavaPlugin plugin, MorphManager morphManager) {
        this.plugin = plugin;
        this.morphManager = morphManager;
    }

    @Override
    public void run() {
        tickCounter++;

        for (MorphSession session : morphManager.getAllSessions()) {
            Player player = plugin.getServer().getPlayer(session.getPlayerUUID());
            if (player == null || !player.isOnline() || player.isDead()) continue;

            MobAbility ability = session.getAbility();

            if (tickCounter % 80 == 0 && session.getMorphType() != EntityType.PLAYER) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.INVISIBILITY, 200, 1, true, false, false));

                if (plugin.getConfig().getBoolean("morph-self-visible", true)) {
                    showDisguiseEntityToSelf(player);
                }
            }

            if (ability.burnsInSunlight() && tickCounter % 20 == 0) {
                handleSunlightBurning(player);
            }

            if (ability.takesWaterDamage() && tickCounter % 10 == 0) {
                handleWaterDamage(player);
            }

            if (tickCounter % 5 == 0) {
                spawnAmbientParticles(player, session);
            }

            if (ability.getEntityType() == EntityType.PUFFERFISH && tickCounter % 20 == 0) {
                handlePufferfishPoison(player);
            }

            if (ability.getEntityType() == EntityType.WARDEN && tickCounter % 40 == 0) {
                handleWardenDarkness(player);
            }

            if (ability.canFly() && !player.getAllowFlight()) {
                player.setAllowFlight(true);
            }

            if (tickCounter % 60 == 0 && ability.hasActiveAbility()) {
                MessageUtil.sendActionBar(player,
                        "&7Morphed as &e" + ability.getDisplayName() +
                                " &8| &eSNEAK &7= &f" + ability.getActiveAbilityName());
            }

            if (tickCounter % 200 == 0) {
                for (PotionEffect effect : ability.getPassiveEffects()) {
                    if (!player.hasPotionEffect(effect.getType())) {
                        player.addPotionEffect(effect);
                    }
                }
            }
        }
    }

    private void showDisguiseEntityToSelf(Player player) {
        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (entity.hasMetadata("iDisguise")) {
                for (MetadataValue meta : entity.getMetadata("iDisguise")) {
                    if (meta.value() instanceof java.util.UUID uuid &&
                            uuid.equals(player.getUniqueId())) {
                        try {
                            player.showEntity(plugin, entity);
                        } catch (Exception ignored) {}
                        if (entity instanceof org.bukkit.entity.LivingEntity le) {
                            le.setCollidable(false);
                        }
                        morphManager.addEntityToNoCollisionTeam(player, entity);
                        return;
                    }
                }
            }
        }
    }

    private void handleSunlightBurning(Player player) {
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        Location loc = player.getLocation();
        long time = world.getTime();

        if (time > 13000 || time < 0) return;

        int highestY = world.getHighestBlockYAt(loc);
        if (loc.getBlockY() < highestY) return;

        if (world.hasStorm()) return;

        if (player.getEquipment() != null && player.getEquipment().getHelmet() != null) return;

        player.setFireTicks(40);
    }

    private void handleWaterDamage(Player player) {
        if (player.isInWater() || player.isInRain()) {
            player.damage(1.0);
            player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0),
                    10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private void spawnAmbientParticles(Player player, MorphSession session) {
        EntityType type = session.getMorphType();
        Location loc = player.getLocation().add(0, 0.5, 0);

        switch (type) {
            case BLAZE -> player.getWorld().spawnParticle(Particle.FLAME, loc, 2, 0.3, 0.5, 0.3, 0.01);
            case ENDERMAN -> player.getWorld().spawnParticle(Particle.PORTAL, loc, 3, 0.3, 0.5, 0.3, 0.5);
            case WITHER -> player.getWorld().spawnParticle(Particle.SMOKE, loc, 2, 0.3, 0.5, 0.3, 0.01);
            case MAGMA_CUBE -> player.getWorld().spawnParticle(Particle.LAVA, loc, 1, 0.3, 0.3, 0.3, 0);
            case SNOW_GOLEM -> player.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 2, 0.3, 0.5, 0.3, 0.01);
            case ALLAY -> player.getWorld().spawnParticle(Particle.ENCHANT, loc, 3, 0.3, 0.5, 0.3, 0.5);
            case GLOW_SQUID -> player.getWorld().spawnParticle(Particle.GLOW, loc, 1, 0.3, 0.3, 0.3, 0.01);
            case WARDEN -> player.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, loc, 1, 0.3, 0.3, 0.3, 0);
            case ENDER_DRAGON -> {
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 2, 0.5, 0.3, 0.5, 0.01);
                player.getWorld().spawnParticle(Particle.PORTAL, loc, 3, 1, 1, 1, 0.5);
            }
            default -> {}
        }
    }

    private void handlePufferfishPoison(Player player) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(2, 2, 2)) {
            if (entity instanceof Player target && target != player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
            }
        }
    }

    private void handleWardenDarkness(Player player) {
        int range = plugin.getConfig().getInt("warden-darkness-range", 20);
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player target && target != player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
            }
        }
    }
}
