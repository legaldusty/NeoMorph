package com.neomorph.morph;

import com.neomorph.ability.MobAbility;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public class MorphSession {

    private final UUID playerUUID;
    private final EntityType morphType;
    private final MobAbility ability;
    private final long morphTime;

    private final double originalMaxHealth;
    private final double originalHealth;
    private final double originalScale;
    private final double originalSpeed;
    private final double originalAttackDamage;
    private final double originalKnockbackResistance;
    private final double originalJumpStrength;
    private final double originalGravity;
    private final double originalFallDamageMultiplier;
    private final double originalSafeFallDistance;
    private final boolean wasAllowFlight;
    private final boolean wasFlying;

    private long lastActiveAbilityUse;
    private long lastAttackAbilityUse;
    private boolean abilityCharging;
    private long chargeStartTick;

    public MorphSession(UUID playerUUID, EntityType morphType, MobAbility ability,
                        double originalMaxHealth, double originalHealth, double originalScale,
                        double originalSpeed, double originalAttackDamage,
                        double originalKnockbackResistance, double originalJumpStrength,
                        double originalGravity, double originalFallDamageMultiplier,
                        double originalSafeFallDistance,
                        boolean wasAllowFlight, boolean wasFlying) {
        this.playerUUID = playerUUID;
        this.morphType = morphType;
        this.ability = ability;
        this.morphTime = System.currentTimeMillis();
        this.originalMaxHealth = originalMaxHealth;
        this.originalHealth = originalHealth;
        this.originalScale = originalScale;
        this.originalSpeed = originalSpeed;
        this.originalAttackDamage = originalAttackDamage;
        this.originalKnockbackResistance = originalKnockbackResistance;
        this.originalJumpStrength = originalJumpStrength;
        this.originalGravity = originalGravity;
        this.originalFallDamageMultiplier = originalFallDamageMultiplier;
        this.originalSafeFallDistance = originalSafeFallDistance;
        this.wasAllowFlight = wasAllowFlight;
        this.wasFlying = wasFlying;
        this.lastActiveAbilityUse = 0;
        this.lastAttackAbilityUse = 0;
        this.abilityCharging = false;
        this.chargeStartTick = 0;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public EntityType getMorphType() { return morphType; }
    public MobAbility getAbility() { return ability; }
    public long getMorphTime() { return morphTime; }

    public double getOriginalMaxHealth() { return originalMaxHealth; }
    public double getOriginalHealth() { return originalHealth; }
    public double getOriginalScale() { return originalScale; }
    public double getOriginalSpeed() { return originalSpeed; }
    public double getOriginalAttackDamage() { return originalAttackDamage; }
    public double getOriginalKnockbackResistance() { return originalKnockbackResistance; }
    public double getOriginalJumpStrength() { return originalJumpStrength; }
    public double getOriginalGravity() { return originalGravity; }
    public double getOriginalFallDamageMultiplier() { return originalFallDamageMultiplier; }
    public double getOriginalSafeFallDistance() { return originalSafeFallDistance; }
    public boolean wasAllowFlight() { return wasAllowFlight; }
    public boolean wasFlying() { return wasFlying; }

    public long getLastActiveAbilityUse() { return lastActiveAbilityUse; }
    public void setLastActiveAbilityUse(long tick) { this.lastActiveAbilityUse = tick; }
    public long getLastAttackAbilityUse() { return lastAttackAbilityUse; }
    public void setLastAttackAbilityUse(long tick) { this.lastAttackAbilityUse = tick; }

    public boolean isAbilityCharging() { return abilityCharging; }
    public void setAbilityCharging(boolean charging) { this.abilityCharging = charging; }
    public long getChargeStartTick() { return chargeStartTick; }
    public void setChargeStartTick(long tick) { this.chargeStartTick = tick; }
}
