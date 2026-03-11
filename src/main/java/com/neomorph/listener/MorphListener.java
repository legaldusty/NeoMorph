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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSelfDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (damager instanceof Player player && victim instanceof Player target) {
            if (player.equals(target) && morphManager.isMorphed(player)) {
                event.setCancelled(true);
                return;
            }
        }

        if (damager instanceof Player player && morphManager.isMorphed(player)) {
            if (!(victim instanceof Player)) {
                double distSq = player.getLocation().distanceSquared(victim.getLocation());
                if (distSq < 2.25) {
                    MorphSession session = morphManager.getSession(player);
                    if (session != null && victim.getType() == session.getMorphType()) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        if (victim instanceof Player player && morphManager.isMorphed(player)) {
            if (damager instanceof org.bukkit.entity.Projectile proj) {
                if (proj.getShooter() instanceof Player shooter && shooter.equals(player)) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (damager instanceof org.bukkit.entity.AreaEffectCloud cloud) {
                if (cloud.getSource() instanceof Player source && source.equals(player)) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (damager instanceof org.bukkit.entity.EvokerFangs fangs) {
                if (fangs.getOwner() instanceof Player owner && owner.equals(player)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionSelfDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!morphManager.isMorphed(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            MorphSession session = morphManager.getSession(player);
            if (session != null) {
                long lastAbility = session.getLastActiveAbilityUse();
                long now = player.getWorld().getGameTime();
                if (now - lastAbility < 60) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!morphManager.isMorphed(player)) return;

        MorphSession session = morphManager.getSession(player);
        if (session == null) return;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (morphManager.isMorphed(player)) {
            morphManager.unmorph(player, false);
        }
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
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                if (morphManager.isMorphed(player)) {
                    morphManager.unmorph(player, false);
                }
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
                var scaleAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                if (scaleAttr != null && scaleAttr.getBaseValue() != 1.0) {
                    scaleAttr.setBaseValue(1.0);
                }
                var healthAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                if (healthAttr != null && healthAttr.getBaseValue() != 20.0) {
                    healthAttr.setBaseValue(20.0);
                    player.setHealth(20.0);
                }
                var speedAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED);
                if (speedAttr != null && speedAttr.getBaseValue() != 0.1) {
                    speedAttr.setBaseValue(0.1);
                }
                var attackAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
                if (attackAttr != null && attackAttr.getBaseValue() != 1.0) {
                    attackAttr.setBaseValue(1.0);
                }
                var kbAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE);
                if (kbAttr != null && kbAttr.getBaseValue() != 0.0) {
                    kbAttr.setBaseValue(0.0);
                }
                var jumpAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_JUMP_STRENGTH);
                if (jumpAttr != null && jumpAttr.getBaseValue() != 0.42) {
                    jumpAttr.setBaseValue(0.42);
                }
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE &&
                        player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
                player.setInvulnerable(false);
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
        if (morphManager.isMorphed(player)) {
            morphManager.unmorph(player, false);
        }
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
