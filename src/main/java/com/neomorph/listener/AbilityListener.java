package com.neomorph.listener;

import com.neomorph.ability.MobAbility;
import com.neomorph.ability.MobAbility.ActiveAbility;
import com.neomorph.morph.MorphManager;
import com.neomorph.morph.MorphSession;
import com.neomorph.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Random;

public class AbilityListener implements Listener {

    private final JavaPlugin plugin;
    private final MorphManager morphManager;
    private final Random random = new Random();

    public AbilityListener(JavaPlugin plugin, MorphManager morphManager) {
        this.plugin = plugin;
        this.morphManager = morphManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) return;
        if (!morphManager.isMorphed(player)) return;

        MorphSession session = morphManager.getSession(player);
        MobAbility ability = session.getAbility();
        if (!ability.hasActiveAbility()) return;

        long currentTick = player.getWorld().getGameTime();
        long elapsed = currentTick - session.getLastActiveAbilityUse();
        if (elapsed < ability.getAbilityCooldownTicks()) {
            double remaining = (ability.getAbilityCooldownTicks() - elapsed) / 20.0;
            String msg = plugin.getConfig().getString("messages.ability-cooldown",
                    "&c&lNEOMORPH &8» &7Ability on cooldown! &e%time%s &7remaining.");
            MessageUtil.send(player, msg.replace("%time%", String.format("%.1f", remaining)));
            return;
        }

        boolean success = executeActiveAbility(player, session, ability.getActiveAbility());
        if (success) {
            session.setLastActiveAbilityUse(currentTick);
            String msg = plugin.getConfig().getString("messages.ability-used",
                    "&a&lNEOMORPH &8» &7Used &e%ability%&7!");
            MessageUtil.send(player, msg.replace("%ability%", ability.getActiveAbilityName()));
        }
    }

    private boolean executeActiveAbility(Player player, MorphSession session, ActiveAbility ability) {
        return switch (ability) {
            case CREEPER_EXPLODE -> creeperExplode(player);
            case ENDERMAN_TELEPORT -> endermanTeleport(player);
            case GHAST_FIREBALL -> ghastFireball(player);
            case BLAZE_FIREBALL -> blazeFireball(player);
            case WITHER_SKULL -> witherSkull(player);
            case DRAGON_BREATH -> dragonBreath(player);
            case WARDEN_SONIC_BOOM -> wardenSonicBoom(player);
            case SHULKER_BULLET -> shulkerBullet(player);
            case WITCH_POTION -> witchPotion(player);
            case EVOKER_FANGS -> evokerFangs(player);
            case RAVAGER_CHARGE -> ravagerCharge(player);
            case GUARDIAN_LASER -> guardianLaser(player);
            case PHANTOM_DIVE -> phantomDive(player);
            case BREEZE_WIND_CHARGE -> breezeWindCharge(player);
            case SNOWBALL_THROW -> snowballThrow(player);
            case LLAMA_SPIT -> llamaSpit(player);
            case GOAT_RAM -> goatRam(player);
            case CAMEL_DASH -> camelDash(player);
            case IRON_GOLEM_SLAM -> ironGolemSlam(player);
            case SILVERFISH_SUMMON -> silverfishSummon(player);
            case SPIDER_CLIMB -> spiderClimb(player);
            case DROWNED_TRIDENT -> drownedTrident(player);
            case ELDER_GUARDIAN_FATIGUE -> elderGuardianFatigue(player);
            case DOLPHIN_BOOST -> dolphinBoost(player);
            case PANDA_ROLL -> pandaRoll(player);
            case WOLF_HOWL -> wolfHowl(player);
            default -> false;
        };
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!morphManager.isMorphed(player)) return;

        MorphSession session = morphManager.getSession(player);
        MobAbility ability = session.getAbility();

        if (ability.getAttackDamage() > 1.0) {
            event.setDamage(ability.getAttackDamage());
        }

        if (!ability.hasAttackAbility()) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        switch (ability.getAttackAbility()) {
            case BEE_STING -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
            }
            default -> {}
        }

        if (session.getAbility().getEntityType() == EntityType.WITHER_SKELETON) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 0));
        }

        if (session.getAbility().getEntityType() == EntityType.HUSK) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 140, 0));
        }

        if (session.getAbility().getEntityType() == EntityType.STRAY) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0));
        }

        if (session.getAbility().getEntityType() == EntityType.CAVE_SPIDER) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 150, 0));
        }

        if (session.getAbility().getEntityType() == EntityType.BOGGED) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0));
        }
    }


    private boolean creeperExplode(Player player) {
        float power = (float) plugin.getConfig().getDouble("creeper-explosion-power", 3.0);
        boolean breakBlocks = plugin.getConfig().getBoolean("creeper-explosion-breaks-blocks", true);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                Location loc = player.getLocation();
                player.setInvulnerable(true);
                player.getWorld().createExplosion(loc, power, false, breakBlocks, player);
                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.setInvulnerable(false);
                    }
                }.runTaskLater(plugin, 1L);
            }
        }.runTaskLater(plugin, plugin.getConfig().getInt("creeper-charge-time", 30));

        return true;
    }

    private boolean endermanTeleport(Player player) {
        int range = plugin.getConfig().getInt("enderman-teleport-range", 50);

        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), player.getLocation().getDirection(), range);

        if (result == null || result.getHitBlock() == null) {
            MessageUtil.send(player, "&c&lNEOMORPH &8» &7No valid teleport location found.");
            return false;
        }

        Block hitBlock = result.getHitBlock();
        Location teleportLoc = hitBlock.getLocation().add(0.5, 1, 0.5);
        teleportLoc.setYaw(player.getLocation().getYaw());
        teleportLoc.setPitch(player.getLocation().getPitch());

        if (!teleportLoc.getBlock().isPassable() || !teleportLoc.clone().add(0, 1, 0).getBlock().isPassable()) {
            teleportLoc = hitBlock.getLocation().add(0.5, 1, 0.5);
            if (!teleportLoc.getBlock().isPassable()) {
                MessageUtil.send(player, "&c&lNEOMORPH &8» &7Teleport location is blocked.");
                return false;
            }
        }

        Location origin = player.getLocation();
        player.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, origin.add(0, 1, 0), 50, 0.5, 1, 0.5, 0.5);

        player.teleport(teleportLoc);

        player.getWorld().playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, teleportLoc.add(0, 1, 0), 50, 0.5, 1, 0.5, 0.5);

        return true;
    }

    private boolean ghastFireball(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        double speed = plugin.getConfig().getDouble("ghast-fireball-speed", 1.5);

        Fireball fireball = player.getWorld().spawn(
                eyeLoc.add(direction.multiply(2)), Fireball.class);
        fireball.setShooter(player);
        fireball.setDirection(direction);
        fireball.setYield(2.0f);
        fireball.setIsIncendiary(true);

        player.setInvulnerable(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) player.setInvulnerable(false);
            }
        }.runTaskLater(plugin, 5L);

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
        return true;
    }

    private boolean blazeFireball(Player player) {
        int count = plugin.getConfig().getInt("blaze-fireball-count", 3);
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        player.setInvulnerable(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) player.setInvulnerable(false);
            }
        }.runTaskLater(plugin, (long)(count * 4) + 5L);

        for (int i = 0; i < count; i++) {
            final int delay = i * 4;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    Location loc = player.getEyeLocation();
                    Vector dir = loc.getDirection().add(new Vector(
                            (random.nextDouble() - 0.5) * 0.2,
                            (random.nextDouble() - 0.5) * 0.1,
                            (random.nextDouble() - 0.5) * 0.2
                    )).normalize();

                    SmallFireball fb = player.getWorld().spawn(
                            loc.add(dir.clone().multiply(1.5)), SmallFireball.class);
                    fb.setShooter(player);
                    fb.setDirection(dir);
                }
            }.runTaskLater(plugin, delay);
        }
        return true;
    }

    private boolean witherSkull(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        WitherSkull skull = player.getWorld().spawn(
                eyeLoc.add(direction.multiply(2)), WitherSkull.class);
        skull.setShooter(player);
        skull.setDirection(direction);

        player.setInvulnerable(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) player.setInvulnerable(false);
            }
        }.runTaskLater(plugin, 5L);

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
        return true;
    }

    private boolean dragonBreath(Player player) {
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        Location loc = player.getLocation().add(dir.multiply(5));
        int duration = plugin.getConfig().getInt("dragon-breath-duration", 100);

        AreaEffectCloud cloud = player.getWorld().spawn(loc, AreaEffectCloud.class);
        cloud.setSource(player);
        cloud.setRadius(4.0f);
        cloud.setRadiusOnUse(-0.5f);
        cloud.setRadiusPerTick(-0.01f);
        cloud.setDuration(duration);
        cloud.setParticle(Particle.DRAGON_BREATH);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1), true);

        player.setInvulnerable(true);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.add(0, 0.5, 0), 100, 2, 0.5, 2, 0.01);
        new BukkitRunnable() {
            @Override
            public void run() { player.setInvulnerable(false); }
        }.runTaskLater(plugin, 5L);

        return true;
    }

    private boolean wardenSonicBoom(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        double damage = plugin.getConfig().getDouble("warden-sonic-boom-damage", 30.0);

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.0f, 1.0f);

        RayTraceResult result = player.getWorld().rayTraceEntities(
                eyeLoc, direction, 15.0, 1.0,
                entity -> entity instanceof LivingEntity && entity != player);

        for (double d = 0; d < 15; d += 0.5) {
            Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
        }

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.damage(damage, player);
            Vector knockback = direction.clone().normalize().multiply(2.0);
            knockback.setY(0.5);
            target.setVelocity(target.getVelocity().add(knockback));
        }

        return true;
    }

    private boolean shulkerBullet(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        RayTraceResult result = player.getWorld().rayTraceEntities(
                eyeLoc, direction, 20.0, 1.5,
                entity -> entity instanceof LivingEntity && entity != player);

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            ShulkerBullet bullet = player.getWorld().spawn(
                    eyeLoc.add(direction.multiply(1.5)), ShulkerBullet.class);
            bullet.setShooter(player);
            bullet.setTarget(target);

            player.getWorld().playSound(eyeLoc, Sound.ENTITY_SHULKER_SHOOT, 1.0f, 1.0f);
            return true;
        }

        MessageUtil.send(player, "&c&lNEOMORPH &8» &7No target found for shulker bullet.");
        return false;
    }

    private boolean witchPotion(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().multiply(1.2);
        direction.setY(direction.getY() + 0.3);

        PotionEffectType[] effects = {
                PotionEffectType.POISON, PotionEffectType.SLOWNESS,
                PotionEffectType.WEAKNESS, PotionEffectType.INSTANT_DAMAGE
        };
        PotionEffectType chosen = effects[random.nextInt(effects.length)];

        ThrownPotion potion = player.launchProjectile(ThrownPotion.class);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta)
                potion.getItem().getItemMeta();
        if (meta != null) {
            if (chosen == PotionEffectType.INSTANT_DAMAGE) {
                meta.addCustomEffect(new PotionEffect(chosen, 1, 0), true);
            } else {
                meta.addCustomEffect(new PotionEffect(chosen, 100, 0), true);
            }
            org.bukkit.inventory.ItemStack potionItem = potion.getItem();
            potionItem.setItemMeta(meta);
            potion.setItem(potionItem);
        }

        player.setInvulnerable(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) player.setInvulnerable(false);
            }
        }.runTaskLater(plugin, 10L);

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_WITCH_THROW, 1.0f, 1.0f);
        return true;
    }

    private boolean evokerFangs(Player player) {
        Location loc = player.getLocation();
        Vector direction = loc.getDirection().setY(0).normalize();

        player.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f);

        for (int i = 1; i <= 10; i++) {
            final int distance = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    Location fangLoc = player.getLocation().add(direction.clone().multiply(distance));
                    fangLoc.setY(player.getWorld().getHighestBlockYAt(fangLoc));

                    EvokerFangs fangs = player.getWorld().spawn(fangLoc, EvokerFangs.class);
                    fangs.setOwner(player);
                }
            }.runTaskLater(plugin, i * 2L);
        }
        return true;
    }

    private boolean ravagerCharge(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Vector velocity = direction.multiply(2.5);
        velocity.setY(0.2);

        player.setVelocity(velocity);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 10 || !player.isOnline()) {
                    cancel();
                    return;
                }
                for (Entity entity : player.getNearbyEntities(2, 1, 2)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        target.damage(12.0, player);
                        Vector kb = target.getLocation().subtract(player.getLocation())
                                .toVector().normalize().multiply(1.5);
                        kb.setY(0.5);
                        target.setVelocity(kb);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 5L, 1L);

        return true;
    }

    private boolean guardianLaser(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        RayTraceResult result = player.getWorld().rayTraceEntities(
                eyeLoc, direction, 15.0, 1.0,
                entity -> entity instanceof LivingEntity && entity != player);

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            player.getWorld().playSound(eyeLoc, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 1.0f);

            Location start = eyeLoc.clone();
            Location end = target.getEyeLocation();
            Vector path = end.toVector().subtract(start.toVector());
            double distance = path.length();
            Vector step = path.normalize().multiply(0.3);

            for (double d = 0; d < distance; d += 0.3) {
                Location particleLoc = start.clone().add(step.clone().multiply(d / 0.3));
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0);
            }

            target.damage(8.0, player);
            return true;
        }

        MessageUtil.send(player, "&c&lNEOMORPH &8» &7No target in range for laser.");
        return false;
    }

    private boolean phantomDive(Player player) {
        if (!player.isFlying()) {
            MessageUtil.send(player, "&c&lNEOMORPH &8» &7You must be flying to dive bomb!");
            return false;
        }

        Vector direction = player.getLocation().getDirection().normalize().multiply(3.0);
        direction.setY(Math.min(direction.getY(), -0.5));
        player.setVelocity(direction);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1.5f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20 || !player.isOnline() || player.isOnGround()) {
                    if (player.isOnGround()) {
                        for (Entity entity : player.getNearbyEntities(3, 2, 3)) {
                            if (entity instanceof LivingEntity target && entity != player) {
                                target.damage(6.0, player);
                            }
                        }
                        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 3);
                    }
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation(), 5, 0.3, 0.3, 0.3, 0);
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return true;
    }

    private boolean breezeWindCharge(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        WindCharge windCharge = player.getWorld().spawn(
                eyeLoc.add(direction.multiply(1.5)), WindCharge.class);
        windCharge.setShooter(player);

        player.setInvulnerable(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) player.setInvulnerable(false);
            }
        }.runTaskLater(plugin, 5L);

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);
        return true;
    }

    private boolean snowballThrow(Player player) {
        Snowball snowball = player.launchProjectile(Snowball.class);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, 1.0f, 1.0f);
        return true;
    }

    private boolean llamaSpit(Player player) {
        LlamaSpit spit = player.getWorld().spawn(
                player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.5)),
                LlamaSpit.class);
        spit.setShooter(player);
        spit.setVelocity(player.getLocation().getDirection().multiply(1.5));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LLAMA_SPIT, 1.0f, 1.0f);
        return true;
    }

    private boolean goatRam(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Vector velocity = direction.multiply(2.0);
        velocity.setY(0.3);

        player.setVelocity(velocity);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GOAT_RAM_IMPACT, 1.5f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 8 || !player.isOnline()) {
                    cancel();
                    return;
                }
                for (Entity entity : player.getNearbyEntities(1.5, 1, 1.5)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        target.damage(5.0, player);
                        Vector kb = direction.clone().multiply(2.5);
                        kb.setY(0.8);
                        target.setVelocity(kb);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 2L, 1L);

        return true;
    }

    private boolean camelDash(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Vector velocity = direction.multiply(2.5);
        velocity.setY(0.4);

        player.setVelocity(velocity);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CAMEL_DASH, 1.0f, 1.0f);

        return true;
    }

    private boolean ironGolemSlam(Player player) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.8f);
        player.getWorld().spawnParticle(Particle.CRIT, loc, 30, 2, 0.5, 2, 0.1);

        for (Entity entity : player.getNearbyEntities(4, 2, 4)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(10.0, player);
                Vector diff = target.getLocation().subtract(loc).toVector();
                if (diff.lengthSquared() < 0.01) {
                    diff = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5).normalize();
                } else {
                    diff.normalize();
                }
                diff.setY(0.8);
                diff.multiply(1.5);
                target.setVelocity(diff);
            }
        }

        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            for (double r = 0.5; r <= 3; r += 0.5) {
                Location particleLoc = loc.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                player.getWorld().spawnParticle(Particle.BLOCK, particleLoc, 2,
                        loc.getBlock().getRelative(0, -1, 0).getBlockData());
            }
        }

        return true;
    }

    private boolean silverfishSummon(Player player) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_SILVERFISH_AMBIENT, 2.0f, 0.5f);

        for (int i = 0; i < 5; i++) {
            Location spawnLoc = loc.clone().add(
                    random.nextDouble() * 3 - 1.5,
                    0,
                    random.nextDouble() * 3 - 1.5
            );
            Silverfish silverfish = player.getWorld().spawn(spawnLoc, Silverfish.class);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (silverfish.isValid()) silverfish.remove();
                }
            }.runTaskLater(plugin, 200L);
        }

        return true;
    }

    private boolean spiderClimb(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 15, 1, true, false, false));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_STEP, 0.5f, 1.0f);
        return true;
    }

    private boolean drownedTrident(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().multiply(2.5);

        Trident trident = player.getWorld().spawn(
                eyeLoc.add(player.getLocation().getDirection().multiply(1.5)), Trident.class);
        trident.setShooter(player);
        trident.setVelocity(direction);

        player.getWorld().playSound(eyeLoc, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);
        return true;
    }

    private boolean elderGuardianFatigue(Player player) {
        Location loc = player.getLocation();
        int range = plugin.getConfig().getInt("warden-darkness-range", 20);

        player.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 1.0f);

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player target && target != player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 300, 2));
            }
        }

        player.getWorld().spawnParticle(Particle.ELDER_GUARDIAN, loc.add(0, 1, 0), 1);
        return true;
    }

    private boolean dolphinBoost(Player player) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_DOLPHIN_PLAY, 1.0f, 1.0f);

        for (Entity entity : player.getNearbyEntities(10, 5, 10)) {
            if (entity instanceof Player target) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 200, 0));
                MessageUtil.send(target, "&b&lNEOMORPH &8» &7A dolphin granted you &bDolphin's Grace&7!");
            }
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 200, 0));
        return true;
    }

    private boolean pandaRoll(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Vector velocity = direction.multiply(1.8);
        velocity.setY(0.15);

        player.setVelocity(velocity);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PANDA_STEP, 1.0f, 0.5f);

        return true;
    }

    private boolean wolfHowl(Player player) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_WOLF_HOWL, 2.0f, 1.0f);

        for (Entity entity : player.getNearbyEntities(15, 5, 15)) {
            if (entity instanceof Player target) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0));
                MessageUtil.send(target, "&6&lNEOMORPH &8» &7A wolf's howl empowers you!");
            }
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
        return true;
    }
}
