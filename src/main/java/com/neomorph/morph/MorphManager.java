package com.neomorph.morph;

import com.neomorph.NeoMorph;
import com.neomorph.ability.AbilityRegistry;
import com.neomorph.ability.MobAbility;
import com.neomorph.util.MessageUtil;
import de.luisagrether.idisguise.api.DisguiseAPI;
import de.luisagrether.idisguise.api.EventCancelledException;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MorphManager {

    private static final String TEAM_NAME = "neomorph_nocol";

    private final NeoMorph plugin;
    private final AbilityRegistry registry;
    private final MorphEffect morphEffect;
    private final Map<UUID, MorphSession> activeMorphs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> unmorphingPlayers = ConcurrentHashMap.newKeySet();

    public MorphManager(NeoMorph plugin, AbilityRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.morphEffect = new MorphEffect(
                plugin,
                plugin.getConfig().getBoolean("show-morph-particles", true),
                plugin.getConfig().getBoolean("morph-sound", true)
        );
        ensureNoCollisionTeam();
        startDisguisePersistTask();
    }

    private DisguiseAPI getDisguiseAPI() {
        RegisteredServiceProvider<DisguiseAPI> provider =
                Bukkit.getServicesManager().getRegistration(DisguiseAPI.class);
        if (provider != null) {
            return provider.getProvider();
        }
        plugin.getLogger().warning("[NeoMorph] DisguiseAPI service not found. Is iDisguise installed?");
        return null;
    }

    private void ensureNoCollisionTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
        }
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    private Team getOrCreateTeamForPlayer(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
        }
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        return team;
    }

    private void addToNoCollisionTeam(Player player) {
        try {
            Team team = getOrCreateTeamForPlayer(player);
            team.addEntry(player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("[NeoMorph] Collision team error: " + e.getMessage());
        }
    }

    public void addEntityToNoCollisionTeam(Player player, Entity entity) {
        try {
            Team team = getOrCreateTeamForPlayer(player);
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
            team.addEntry(entity.getUniqueId().toString());
        } catch (Exception e) {
            plugin.getLogger().warning("[NeoMorph] Entity collision team error: " + e.getMessage());
        }
    }

    private void removeFromNoCollisionTeam(Player player) {
        try {
            Scoreboard scoreboard = player.getScoreboard();
            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team != null) {
                team.removeEntry(player.getName());
            }
            Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
            if (main != scoreboard) {
                Team mainTeam = main.getTeam(TEAM_NAME);
                if (mainTeam != null) {
                    mainTeam.removeEntry(player.getName());
                }
            }
        } catch (Exception ignored) {}
    }

    private void startDisguisePersistTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                DisguiseAPI api = getDisguiseAPI();
                if (api == null) return;

                for (MorphSession session : activeMorphs.values()) {
                    Player player = Bukkit.getPlayer(session.getPlayerUUID());
                    if (player == null || !player.isOnline()) continue;
                    if (player.isDead()) continue;
                    if (unmorphingPlayers.contains(session.getPlayerUUID())) continue;

                    try {
                        if (!api.isDisguised(player)) {
                            EntityType type = session.getMorphType();
                            if (type == EntityType.PLAYER) {
                                String targetName = session.getAbility().getDisplayName();
                                api.disguiseAsPlayer(player, targetName, false, success -> {});
                            } else {
                                api.disguise(player, type, false);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    public AbilityRegistry getRegistry() { return registry; }

    public boolean isMorphed(Player player) {
        return activeMorphs.containsKey(player.getUniqueId());
    }

    public MorphSession getSession(Player player) {
        return activeMorphs.get(player.getUniqueId());
    }

    public Collection<MorphSession> getAllSessions() {
        return activeMorphs.values();
    }

    public boolean isOnCooldown(Player player) {
        if (player.hasPermission("neomorph.bypass.cooldown")) return false;
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) return false;
        int cooldownSeconds = plugin.getConfig().getInt("morph-cooldown", 5);
        return (System.currentTimeMillis() - lastUse) < (cooldownSeconds * 1000L);
    }

    public int getCooldownRemaining(Player player) {
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) return 0;
        int cooldownSeconds = plugin.getConfig().getInt("morph-cooldown", 5);
        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = (cooldownSeconds * 1000L) - elapsed;
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    public boolean morph(Player player, EntityType entityType) {
        MobAbility ability = registry.getAbility(entityType);
        if (ability == null) {
            MessageUtil.send(player, "&c&lNEOMORPH &8» &7That mob is not available for morphing.");
            return false;
        }

        if (ability.getCategory() == MobAbility.Category.BOSS && !player.hasPermission("neomorph.boss")) {
            MessageUtil.send(player, plugin.getConfig().getString("messages.no-permission",
                    "&c&lNEOMORPH &8» &7You don't have permission to do that."));
            return false;
        }

        if (isOnCooldown(player)) {
            String msg = plugin.getConfig().getString("messages.morph-cooldown",
                    "&c&lNEOMORPH &8» &7Please wait &e%time%s &7before morphing again.");
            MessageUtil.send(player, msg.replace("%time%", String.valueOf(getCooldownRemaining(player))));
            return false;
        }

        if (isMorphed(player)) {
            unmorph(player, false);
        }

        double origMaxHealth = getAttr(player, Attribute.GENERIC_MAX_HEALTH);
        double origHealth = player.getHealth();
        double origScale = getAttr(player, Attribute.GENERIC_SCALE);
        double origSpeed = getAttr(player, Attribute.GENERIC_MOVEMENT_SPEED);
        double origAttack = getAttr(player, Attribute.GENERIC_ATTACK_DAMAGE);
        double origKB = getAttr(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        double origJump = getAttr(player, Attribute.GENERIC_JUMP_STRENGTH);
        double origGravity = getAttr(player, Attribute.GENERIC_GRAVITY);
        double origFallDmg = getAttr(player, Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER);
        double origSafeFall = getAttr(player, Attribute.GENERIC_SAFE_FALL_DISTANCE);
        boolean origAllowFlight = player.getAllowFlight();
        boolean origFlying = player.isFlying();

        MorphSession session = new MorphSession(
                player.getUniqueId(), entityType, ability,
                origMaxHealth, origHealth, origScale,
                origSpeed, origAttack, origKB, origJump,
                origGravity, origFallDmg, origSafeFall,
                origAllowFlight, origFlying
        );

        DisguiseAPI api = getDisguiseAPI();
        if (api != null) {
            try {
                api.disguise(player, entityType, false);
            } catch (EventCancelledException e) {
                MessageUtil.send(player, "&c&lNEOMORPH &8» &7Disguise was blocked by another plugin.");
                return false;
            } catch (UnsupportedOperationException e) {
                MessageUtil.send(player, "&c&lNEOMORPH &8» &7That mob type is not supported for disguising.");
                return false;
            } catch (Exception e) {
                plugin.getLogger().warning("[NeoMorph] Disguise API error for " + entityType.name() + ": " + e.getMessage());
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "disguise " + player.getName() + " " + entityType.name().toLowerCase());
                } catch (Exception ex) {
                    plugin.getLogger().warning("[NeoMorph] Command fallback also failed: " + ex.getMessage());
                }
            }
        } else {
            MessageUtil.send(player, "&c&lNEOMORPH &8» &7iDisguise not found! Install iDisguise for visual morphing.");
        }

        addToNoCollisionTeam(player);
        player.setCollidable(false);

        double targetScale = ability.getScale();
        setAttr(player, Attribute.GENERIC_SCALE, targetScale);

        setAttr(player, Attribute.GENERIC_MAX_HEALTH, ability.getMaxHealth());
        player.setHealth(ability.getMaxHealth());

        setAttr(player, Attribute.GENERIC_MOVEMENT_SPEED, 0.1 * ability.getSpeedMultiplier());
        setAttr(player, Attribute.GENERIC_ATTACK_DAMAGE, ability.getAttackDamage());
        setAttr(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, ability.getKnockbackResistance());
        setAttr(player, Attribute.GENERIC_JUMP_STRENGTH, ability.getJumpStrength());
        setAttr(player, Attribute.GENERIC_GRAVITY, ability.getGravity());
        setAttr(player, Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER, ability.getFallDamageMultiplier());
        setAttr(player, Attribute.GENERIC_SAFE_FALL_DISTANCE, ability.getSafeFallDistance());

        if (ability.canFly()) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }

        for (PotionEffect effect : ability.getPassiveEffects()) {
            player.addPotionEffect(effect);
        }

        activeMorphs.put(player.getUniqueId(), session);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        morphEffect.playMorphEffect(player);
        String msg = plugin.getConfig().getString("messages.morph-success",
                "&a&lNEOMORPH &8» &7You morphed into &e%mob%&7!");
        MessageUtil.send(player, msg.replace("%mob%", ability.getDisplayName()));

        if (ability.hasActiveAbility()) {
            MessageUtil.sendActionBar(player, "&eSNEAK &7to use &f" + ability.getActiveAbilityName());
        }

        return true;
    }

    public boolean morphIntoPlayer(Player player, String targetName) {
        if (!player.hasPermission("neomorph.player")) {
            MessageUtil.send(player, plugin.getConfig().getString("messages.no-permission",
                    "&c&lNEOMORPH &8» &7You don't have permission to do that."));
            return false;
        }

        if (isOnCooldown(player)) {
            String msg = plugin.getConfig().getString("messages.morph-cooldown",
                    "&c&lNEOMORPH &8» &7Please wait &e%time%s &7before morphing again.");
            MessageUtil.send(player, msg.replace("%time%", String.valueOf(getCooldownRemaining(player))));
            return false;
        }

        if (isMorphed(player)) {
            unmorph(player, false);
        }

        double origMaxHealth = getAttr(player, Attribute.GENERIC_MAX_HEALTH);
        double origHealth = player.getHealth();
        double origScale = getAttr(player, Attribute.GENERIC_SCALE);
        double origSpeed = getAttr(player, Attribute.GENERIC_MOVEMENT_SPEED);
        double origAttack = getAttr(player, Attribute.GENERIC_ATTACK_DAMAGE);
        double origKB = getAttr(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        double origJump = getAttr(player, Attribute.GENERIC_JUMP_STRENGTH);
        double origGravity = getAttr(player, Attribute.GENERIC_GRAVITY);
        double origFallDmg = getAttr(player, Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER);
        double origSafeFall = getAttr(player, Attribute.GENERIC_SAFE_FALL_DISTANCE);
        boolean origAllowFlight = player.getAllowFlight();
        boolean origFlying = player.isFlying();

        MobAbility dummyAbility = MobAbility.builder(EntityType.PLAYER)
                .displayName(targetName).maxHealth(20).speed(1.0).build();

        MorphSession session = new MorphSession(
                player.getUniqueId(), EntityType.PLAYER, dummyAbility,
                origMaxHealth, origHealth, origScale,
                origSpeed, origAttack, origKB, origJump,
                origGravity, origFallDmg, origSafeFall,
                origAllowFlight, origFlying
        );

        DisguiseAPI api = getDisguiseAPI();
        if (api != null) {
            try {
                api.disguiseAsPlayer(player, targetName, false, success -> {
                    if (!success) {
                        MessageUtil.send(player, "&c&lNEOMORPH &8» &7Failed to load skin for " + targetName + ".");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("[NeoMorph] Player disguise error: " + e.getMessage());
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "disguise " + player.getName() + " player " + targetName);
                } catch (Exception ex) {
                    plugin.getLogger().warning("[NeoMorph] Command fallback also failed: " + ex.getMessage());
                }
            }
        }

        addToNoCollisionTeam(player);
        player.setCollidable(false);
        activeMorphs.put(player.getUniqueId(), session);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        morphEffect.playMorphEffect(player);
        MessageUtil.send(player, "&a&lNEOMORPH &8» &7You morphed into player &e" + targetName + "&7!");
        return true;
    }

    public void unmorph(Player player, boolean sendMessage) {
        UUID uuid = player.getUniqueId();
        MorphSession session = activeMorphs.remove(uuid);
        if (session == null) return;

        unmorphingPlayers.add(uuid);

        try {
            DisguiseAPI api = getDisguiseAPI();
            if (api != null && api.isDisguised(player)) {
                try {
                    api.undisguise(player, false);
                } catch (Exception ignored) {}
            }

            removeFromNoCollisionTeam(player);
            player.setCollidable(true);

            setAttr(player, Attribute.GENERIC_MAX_HEALTH, session.getOriginalMaxHealth());
            setAttr(player, Attribute.GENERIC_SCALE, session.getOriginalScale());
            setAttr(player, Attribute.GENERIC_MOVEMENT_SPEED, session.getOriginalSpeed());
            setAttr(player, Attribute.GENERIC_ATTACK_DAMAGE, session.getOriginalAttackDamage());
            setAttr(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, session.getOriginalKnockbackResistance());
            setAttr(player, Attribute.GENERIC_JUMP_STRENGTH, session.getOriginalJumpStrength());
            setAttr(player, Attribute.GENERIC_GRAVITY, session.getOriginalGravity());
            setAttr(player, Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER, session.getOriginalFallDamageMultiplier());
            setAttr(player, Attribute.GENERIC_SAFE_FALL_DISTANCE, session.getOriginalSafeFallDistance());

            if (!player.isDead()) {
                double targetHealth = Math.min(session.getOriginalHealth(), session.getOriginalMaxHealth());
                targetHealth = Math.max(targetHealth, 1.0);
                player.setHealth(targetHealth);
            }

            if (!player.isDead() &&
                    player.getGameMode() != GameMode.CREATIVE &&
                    player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(session.wasFlying());
                player.setAllowFlight(session.wasAllowFlight());
            }

            MobAbility ability = session.getAbility();
            for (PotionEffect effect : ability.getPassiveEffects()) {
                player.removePotionEffect(effect.getType());
            }

            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            player.setInvulnerable(false);

            morphEffect.playUnmorphEffect(player);

            if (sendMessage) {
                MessageUtil.send(player, plugin.getConfig().getString("messages.unmorph-success",
                        "&a&lNEOMORPH &8» &7You returned to your normal form."));
            }
        } finally {
            unmorphingPlayers.remove(uuid);
        }
    }

    public void unmorphAll() {
        for (UUID uuid : new HashSet<>(activeMorphs.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                unmorph(player, false);
            }
        }
        activeMorphs.clear();
    }

    public boolean shouldPreventUndisguise(Player player) {
        if (player.isDead()) return false;
        if (unmorphingPlayers.contains(player.getUniqueId())) return false;
        return activeMorphs.containsKey(player.getUniqueId());
    }

    private double getAttr(Player player, Attribute attribute) {
        AttributeInstance inst = player.getAttribute(attribute);
        return inst != null ? inst.getBaseValue() : 0.0;
    }

    private void setAttr(Player player, Attribute attribute, double value) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) inst.setBaseValue(value);
    }
}
