package com.neomorph.listener;

import com.neomorph.morph.MorphManager;
import com.neomorph.morph.MorphSession;
import com.neomorph.ability.MobAbility;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MorphListener implements Listener {

    private final JavaPlugin plugin;
    private final MorphManager morphManager;

    public MorphListener(JavaPlugin plugin, MorphManager morphManager) {
        this.plugin = plugin;
        this.morphManager = morphManager;
    }

    // =========================================================================
    //  PREVENT SELF-DAMAGE — morphed players hitting their own disguise entity
    //  or any self-inflicted damage from abilities
    // =========================================================================
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSelfDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // Case 1: Player directly hitting themselves (shouldn't happen normally,
        // but iDisguise's fake entity can cause this)
        if (damager instanceof Player player && victim instanceof Player target) {
            if (player.equals(target) && morphManager.isMorphed(player)) {
                event.setCancelled(true);
                return;
            }
        }

        // Case 2: Morphed player hitting a non-player entity that is AT their location
        // (the iDisguise fake entity). If the victim is NOT a player and is within
        // 1.5 blocks of the damager who is morphed, it's likely the disguise entity.
        if (damager instanceof Player player && morphManager.isMorphed(player)) {
            if (!(victim instanceof Player)) {
                double distSq = player.getLocation().distanceSquared(victim.getLocation());
                if (distSq < 2.25) { // within 1.5 blocks
                    // Check if this entity is the disguise by seeing if it's the same type
                    MorphSession session = morphManager.getSession(player);
                    if (session != null && victim.getType() == session.getMorphType()) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // Case 3: A morphed player's projectile/ability damaging themselves
        if (victim instanceof Player player && morphManager.isMorphed(player)) {
            if (damager instanceof org.bukkit.entity.Projectile proj) {
                if (proj.getShooter() instanceof Player shooter && shooter.equals(player)) {
                    event.setCancelled(true);
                    return;
                }
            }
            // Case 4: AreaEffectCloud (dragon breath etc.) damaging the caster
            if (damager instanceof org.bukkit.entity.AreaEffectCloud cloud) {
                if (cloud.getSource() instanceof Player source && source.equals(player)) {
                    event.setCancelled(true);
                    return;
                }
            }
            // Case 5: EvokerFangs owned by the player damaging themselves
            if (damager instanceof org.bukkit.entity.EvokerFangs fangs) {
                if (fangs.getOwner() instanceof Player owner && owner.equals(player)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // =========================================================================
    //  EXPLOSION SELF-DAMAGE IMMUNITY — for creeper explode etc.
    // =========================================================================
    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionSelfDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!morphManager.isMorphed(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        // Block explosion self-damage (creeper ability) and entity explosion
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            // Check if this is from the player's own ability (within short time)
            MorphSession session = morphManager.getSession(player);
            if (session != null) {
                long lastAbility = session.getLastActiveAbilityUse();
                long now = player.getWorld().getGameTime();
                // If ability was used within last 3 seconds (60 ticks), immune to explosions
                if (now - lastAbility < 60) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // =========================================================================
    //  FIRE / FALL DAMAGE IMMUNITY
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!morphManager.isMorphed(player)) return;

        MorphSession session = morphManager.getSession(player);
        if (session == null) return; // Race condition: morph removed between checks
        MobAbility ability = session.getAbility();

        if (ability.isFireImmune()) {
            EntityDamageEvent.DamageCause cause = event.getCause();
            if (cause == EntityDamageEvent.DamageCause.FIRE ||
                    cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    cause == EntityDamageEvent.DamageCause.LAVA ||
                    cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
                event.setCancelled(true);
                player.setFireTicks(0);
                return;
            }
        }

        if (ability.hasNoFallDamage() && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    // =========================================================================
    //  DEATH — clean unmorph, delay respawn restore
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (morphManager.isMorphed(player)) {
            // Unmorph IMMEDIATELY on death to prevent persist task from interfering
            morphManager.unmorph(player, false);
        }
        // Safety: force-undisguise even if unmorph didn't fully clean up
        // This prevents the disguise from getting permanently stuck
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    org.bukkit.plugin.RegisteredServiceProvider<de.luisagrether.idisguise.api.DisguiseAPI> provider =
                            org.bukkit.Bukkit.getServicesManager().getRegistration(de.luisagrether.idisguise.api.DisguiseAPI.class);
                    if (provider != null) {
                        de.luisagrether.idisguise.api.DisguiseAPI api = provider.getProvider();
                        if (api.isDisguised(player)) {
                            api.undisguise(player, false);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Delay cleanup by 1 tick to ensure player has fully respawned
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                // Force-clean any lingering morph state
                if (morphManager.isMorphed(player)) {
                    morphManager.unmorph(player, false);
                }
                // Force-undisguise via iDisguise API in case state got stuck
                try {
                    org.bukkit.plugin.RegisteredServiceProvider<de.luisagrether.idisguise.api.DisguiseAPI> provider =
                            org.bukkit.Bukkit.getServicesManager().getRegistration(de.luisagrether.idisguise.api.DisguiseAPI.class);
                    if (provider != null) {
                        de.luisagrether.idisguise.api.DisguiseAPI api = provider.getProvider();
                        if (api.isDisguised(player)) {
                            api.undisguise(player, false);
                        }
                    }
                } catch (Exception ignored) {}
                // Reset scale to normal in case it got stuck
                var scaleAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                if (scaleAttr != null && scaleAttr.getBaseValue() != 1.0) {
                    scaleAttr.setBaseValue(1.0);
                }
                // Reset max health in case it got stuck
                var healthAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                if (healthAttr != null && healthAttr.getBaseValue() != 20.0) {
                    healthAttr.setBaseValue(20.0);
                    player.setHealth(20.0);
                }
                // Reset speed
                var speedAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED);
                if (speedAttr != null && speedAttr.getBaseValue() != 0.1) {
                    speedAttr.setBaseValue(0.1);
                }
                // Reset attack damage
                var attackAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
                if (attackAttr != null && attackAttr.getBaseValue() != 1.0) {
                    attackAttr.setBaseValue(1.0);
                }
                // Reset knockback resistance
                var kbAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE);
                if (kbAttr != null && kbAttr.getBaseValue() != 0.0) {
                    kbAttr.setBaseValue(0.0);
                }
                // Reset jump strength
                var jumpAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_JUMP_STRENGTH);
                if (jumpAttr != null && jumpAttr.getBaseValue() != 0.42) {
                    jumpAttr.setBaseValue(0.42);
                }
                // Reset flight if not creative/spectator
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE &&
                        player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
                // Clean up invulnerability in case an ability left it on
                player.setInvulnerable(false);
                // Restore collision
                player.setCollidable(true);
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (morphManager.isMorphed(player)) {
            morphManager.unmorph(player, false);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Force clean state on join
        if (morphManager.isMorphed(player)) {
            morphManager.unmorph(player, false);
        }
        // Safety reset
        new BukkitRunnable() {
            @Override
            public void run() {
                var scaleAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                if (scaleAttr != null && scaleAttr.getBaseValue() != 1.0) {
                    scaleAttr.setBaseValue(1.0);
                }
            }
        }.runTaskLater(plugin, 5L);
    }
}
