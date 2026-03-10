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

    // =========================================================================
    //  iDisguise API ACCESS
    // =========================================================================
    private DisguiseAPI getDisguiseAPI() {
        RegisteredServiceProvider<DisguiseAPI> provider =
                Bukkit.getServicesManager().getRegistration(DisguiseAPI.class);
        if (provider != null) {
            return provider.getProvider();
        }
        // Fallback: try to get it from the plugin directly
        plugin.getLogger().warning("[NeoMorph] DisguiseAPI service not found. Is iDisguise installed?");
        return null;
    }

    // =========================================================================
    //  COLLISION MANAGEMENT
    //  Uses a scoreboard team with NEVER collision rule. This is a vanilla
    //  mechanic enforced CLIENT-SIDE — no plugin can override it.
    //
    //  CRITICAL: We use player.getScoreboard() NOT getMainScoreboard().
    //  Many plugins (tab list, sidebar stats, etc.) give players custom
    //  scoreboards. If we only put our team on the main scoreboard, those
    //  players never see it. Using player.getScoreboard() targets whatever
    //  scoreboard the player's client is actually rendering.
    // =========================================================================
    private void ensureNoCollisionTeam() {
        // Set up on main scoreboard as a baseline
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
        }
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    /**
     * Gets or creates the no-collision team on the player's CURRENT scoreboard.
     * This is the key fix — if a plugin gave this player a custom scoreboard,
     * we add our team to THAT scoreboard so the client actually sees it.
     */
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

    /**
     * Adds a disguise entity to the no-collision team on the player's scoreboard.
     * Both the player and the entity must be in the same team for the client to
     * apply the NEVER collision rule between them.
     */
    public void addEntityToNoCollisionTeam(Player player, Entity entity) {
        try {
            Team team = getOrCreateTeamForPlayer(player);
            // Ensure both the player AND the entity are in the team
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
            // Also clean from main scoreboard
            Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
            if (main != scoreboard) {
                Team mainTeam = main.getTeam(TEAM_NAME);
                if (mainTeam != null) {
                    mainTeam.removeEntry(player.getName());
                }
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    //  DISGUISE PERSISTENCE — lightweight backup safety net.
    //  The MAIN persistence is DisguisePersistListener (cancels undisguise events)
    //  + AbilityTickHandler (refreshes invisibility & self-visibility).
    //  This task only catches the rare edge case where iDisguise drops the
    //  disguise without firing an event at all.
    // =========================================================================
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

                    // Only reapply if iDisguise reports fully not disguised
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
        }.runTaskTimer(plugin, 100L, 100L); // Check every 5 seconds
    }

    // =========================================================================
    //  PUBLIC ACCESSORS
    // =========================================================================
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

    // =========================================================================
    //  MORPH INTO MOB
    // =========================================================================
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

        // Unmorph first if already morphed
        if (isMorphed(player)) {
            unmorph(player, false);
        }

        // Save original attribute values
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

        // --- STEP 1: Apply disguise via iDisguise ---
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
                // API failed — try command fallback
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

        // --- STEP 2: Disable collision ---
        addToNoCollisionTeam(player);
        player.setCollidable(false);

        // --- STEP 3: Scale player entity (camera/eye height + hitbox) ---
        double targetScale = ability.getScale();
        setAttr(player, Attribute.GENERIC_SCALE, targetScale);

        // --- STEP 4: Apply mob stats ---
        setAttr(player, Attribute.GENERIC_MAX_HEALTH, ability.getMaxHealth());
        player.setHealth(ability.getMaxHealth());

        setAttr(player, Attribute.GENERIC_MOVEMENT_SPEED, 0.1 * ability.getSpeedMultiplier());
        setAttr(player, Attribute.GENERIC_ATTACK_DAMAGE, ability.getAttackDamage());
        setAttr(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, ability.getKnockbackResistance());
        setAttr(player, Attribute.GENERIC_JUMP_STRENGTH, ability.getJumpStrength());
        setAttr(player, Attribute.GENERIC_GRAVITY, ability.getGravity());
        setAttr(player, Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER, ability.getFallDamageMultiplier());
        setAttr(player, Attribute.GENERIC_SAFE_FALL_DISTANCE, ability.getSafeFallDistance());

        // --- STEP 5: Flight ---
        if (ability.canFly()) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }

        // --- STEP 6: Passive effects ---
        for (PotionEffect effect : ability.getPassiveEffects()) {
            player.addPotionEffect(effect);
        }

        // Store session & cooldown
        activeMorphs.put(player.getUniqueId(), session);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Effects & messaging
        morphEffect.playMorphEffect(player);
        String msg = plugin.getConfig().getString("messages.morph-success",
                "&a&lNEOMORPH &8» &7You morphed into &e%mob%&7!");
        MessageUtil.send(player, msg.replace("%mob%", ability.getDisplayName()));

        if (ability.hasActiveAbility()) {
            MessageUtil.sendActionBar(player, "&eSNEAK &7to use &f" + ability.getActiveAbilityName());
        }

        return true;
    }

    // =========================================================================
    //  MORPH INTO PLAYER
    // =========================================================================
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

        // Apply player disguise via iDisguise
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

    // =========================================================================
    //  UNMORPH
    // =========================================================================
    public void unmorph(Player player, boolean sendMessage) {
        UUID uuid = player.getUniqueId();
        MorphSession session = activeMorphs.remove(uuid);
        if (session == null) return;

        // Mark as unmorphing so the persist listener and persist task don't interfere
        unmorphingPlayers.add(uuid);

        try {
            // Remove disguise via iDisguise
            DisguiseAPI api = getDisguiseAPI();
            if (api != null && api.isDisguised(player)) {
                try {
                    api.undisguise(player, false);
                } catch (Exception ignored) {}
            }

            // Remove from collision team
            removeFromNoCollisionTeam(player);
            player.setCollidable(true);

            // Restore ALL original attributes
            setAttr(player, Attribute.GENERIC_MAX_HEALTH, session.getOriginalMaxHealth());
            setAttr(player, Attribute.GENERIC_SCALE, session.getOriginalScale());
            setAttr(player, Attribute.GENERIC_MOVEMENT_SPEED, session.getOriginalSpeed());
            setAttr(player, Attribute.GENERIC_ATTACK_DAMAGE, session.getOriginalAttackDamage());
            setAttr(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, session.getOriginalKnockbackResistance());
            setAttr(player, Attribute.GENERIC_JUMP_STRENGTH, session.getOriginalJumpStrength());
            setAttr(player, Attribute.GENERIC_GRAVITY, session.getOriginalGravity());
            setAttr(player, Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER, session.getOriginalFallDamageMultiplier());
            setAttr(player, Attribute.GENERIC_SAFE_FALL_DISTANCE, session.getOriginalSafeFallDistance());

            // ONLY set health if the player is alive — setting health on a dead player
            // corrupts state and is the root cause of the "can never morph again" bug
            if (!player.isDead()) {
                double targetHealth = Math.min(session.getOriginalHealth(), session.getOriginalMaxHealth());
                // Clamp to at least 1.0 to avoid killing the player during unmorph
                targetHealth = Math.max(targetHealth, 1.0);
                player.setHealth(targetHealth);
            }

            if (!player.isDead() &&
                    player.getGameMode() != GameMode.CREATIVE &&
                    player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(session.wasFlying());
                player.setAllowFlight(session.wasAllowFlight());
            }

            // Remove all passive potion effects from the morph
            MobAbility ability = session.getAbility();
            for (PotionEffect effect : ability.getPassiveEffects()) {
                player.removePotionEffect(effect.getType());
            }

            // Remove the invisibility we keep applying for self-visibility
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            // Make sure invulnerability is cleaned up (in case an ability left it on)
            player.setInvulnerable(false);

            morphEffect.playUnmorphEffect(player);

            if (sendMessage) {
                MessageUtil.send(player, plugin.getConfig().getString("messages.unmorph-success",
                        "&a&lNEOMORPH &8» &7You returned to your normal form."));
            }
        } finally {
            // Always clear the unmorphing flag
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

    /**
     * Called by the undisguise event listener — prevents iDisguise from
     * removing disguises that NeoMorph is managing.
     * IMPORTANT: Allow undisguise when player is dead or when we're
     * in the middle of an unmorph operation, otherwise the disguise
     * gets permanently stuck and the player can never morph again.
     */
    public boolean shouldPreventUndisguise(Player player) {
        if (player.isDead()) return false;
        if (unmorphingPlayers.contains(player.getUniqueId())) return false;
        return activeMorphs.containsKey(player.getUniqueId());
    }

    // =========================================================================
    //  ATTRIBUTE HELPERS
    // =========================================================================
    private double getAttr(Player player, Attribute attribute) {
        AttributeInstance inst = player.getAttribute(attribute);
        return inst != null ? inst.getBaseValue() : 0.0;
    }

    private void setAttr(Player player, Attribute attribute, double value) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) inst.setBaseValue(value);
    }
}
