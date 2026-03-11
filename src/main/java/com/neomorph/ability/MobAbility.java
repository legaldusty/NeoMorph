package com.neomorph.ability;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MobAbility {

    public enum Category {
        PASSIVE, NEUTRAL, HOSTILE, BOSS, PLAYER, MISC
    }

    public enum ActiveAbility {
        NONE,
        CREEPER_EXPLODE,
        ENDERMAN_TELEPORT,
        GHAST_FIREBALL,
        BLAZE_FIREBALL,
        WITHER_SKULL,
        DRAGON_BREATH,
        WARDEN_SONIC_BOOM,
        SHULKER_BULLET,
        WITCH_POTION,
        EVOKER_FANGS,
        RAVAGER_CHARGE,
        GUARDIAN_LASER,
        PHANTOM_DIVE,
        BREEZE_WIND_CHARGE,
        SNOWBALL_THROW,
        LLAMA_SPIT,
        GOAT_RAM,
        CAMEL_DASH,
        IRON_GOLEM_SLAM,
        SILVERFISH_SUMMON,
        SPIDER_CLIMB,
        DROWNED_TRIDENT,
        ELDER_GUARDIAN_FATIGUE,
        BEE_STING,
        DOLPHIN_BOOST,
        PANDA_ROLL,
        WOLF_HOWL
    }

    private final EntityType entityType;
    private final String displayName;
    private final Material icon;
    private final Category category;
    private final double maxHealth;
    private final double scale;
    private final double speedMultiplier;
    private final double attackDamage;
    private final double knockbackResistance;
    private final double jumpStrength;
    private final double gravity;
    private final double fallDamageMultiplier;
    private final double safeFallDistance;
    private final boolean canFly;
    private final boolean fireImmune;
    private final boolean burnsInSunlight;
    private final boolean waterBreathing;
    private final boolean takesWaterDamage;
    private final boolean noFallDamage;
    private final List<PotionEffect> passiveEffects;
    private final ActiveAbility activeAbility;
    private final String activeAbilityName;
    private final ActiveAbility attackAbility;
    private final String attackAbilityName;
    private final int abilityCooldownTicks;

    private MobAbility(Builder builder) {
        this.entityType = builder.entityType;
        this.displayName = builder.displayName;
        this.icon = builder.icon;
        this.category = builder.category;
        this.maxHealth = builder.maxHealth;
        this.scale = builder.scale;
        this.speedMultiplier = builder.speedMultiplier;
        this.attackDamage = builder.attackDamage;
        this.knockbackResistance = builder.knockbackResistance;
        this.jumpStrength = builder.jumpStrength;
        this.gravity = builder.gravity;
        this.fallDamageMultiplier = builder.fallDamageMultiplier;
        this.safeFallDistance = builder.safeFallDistance;
        this.canFly = builder.canFly;
        this.fireImmune = builder.fireImmune;
        this.burnsInSunlight = builder.burnsInSunlight;
        this.waterBreathing = builder.waterBreathing;
        this.takesWaterDamage = builder.takesWaterDamage;
        this.noFallDamage = builder.noFallDamage;
        this.passiveEffects = Collections.unmodifiableList(builder.passiveEffects);
        this.activeAbility = builder.activeAbility;
        this.activeAbilityName = builder.activeAbilityName;
        this.attackAbility = builder.attackAbility;
        this.attackAbilityName = builder.attackAbilityName;
        this.abilityCooldownTicks = builder.abilityCooldownTicks;
    }

    public EntityType getEntityType() { return entityType; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public Category getCategory() { return category; }
    public double getMaxHealth() { return maxHealth; }
    public double getScale() { return scale; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public double getAttackDamage() { return attackDamage; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public double getJumpStrength() { return jumpStrength; }
    public double getGravity() { return gravity; }
    public double getFallDamageMultiplier() { return fallDamageMultiplier; }
    public double getSafeFallDistance() { return safeFallDistance; }
    public boolean canFly() { return canFly; }
    public boolean isFireImmune() { return fireImmune; }
    public boolean burnsInSunlight() { return burnsInSunlight; }
    public boolean hasWaterBreathing() { return waterBreathing; }
    public boolean takesWaterDamage() { return takesWaterDamage; }
    public boolean hasNoFallDamage() { return noFallDamage; }
    public List<PotionEffect> getPassiveEffects() { return passiveEffects; }
    public ActiveAbility getActiveAbility() { return activeAbility; }
    public String getActiveAbilityName() { return activeAbilityName; }
    public ActiveAbility getAttackAbility() { return attackAbility; }
    public String getAttackAbilityName() { return attackAbilityName; }
    public int getAbilityCooldownTicks() { return abilityCooldownTicks; }

    public boolean hasActiveAbility() { return activeAbility != ActiveAbility.NONE; }
    public boolean hasAttackAbility() { return attackAbility != ActiveAbility.NONE; }

    public static Builder builder(EntityType type) {
        return new Builder(type);
    }

    public static class Builder {
        private final EntityType entityType;
        private String displayName;
        private Material icon;
        private Category category = Category.PASSIVE;
        private double maxHealth = 20.0;
        private double scale = 1.0;
        private double speedMultiplier = 1.0;
        private double attackDamage = 1.0;
        private double knockbackResistance = 0.0;
        private double jumpStrength = 0.42;
        private double gravity = 0.08;
        private double fallDamageMultiplier = 1.0;
        private double safeFallDistance = 3.0;
        private boolean canFly = false;
        private boolean fireImmune = false;
        private boolean burnsInSunlight = false;
        private boolean waterBreathing = false;
        private boolean takesWaterDamage = false;
        private boolean noFallDamage = false;
        private final List<PotionEffect> passiveEffects = new ArrayList<>();
        private ActiveAbility activeAbility = ActiveAbility.NONE;
        private String activeAbilityName = "";
        private ActiveAbility attackAbility = ActiveAbility.NONE;
        private String attackAbilityName = "";
        private int abilityCooldownTicks = 60;

        private Builder(EntityType type) {
            this.entityType = type;
            this.displayName = com.neomorph.util.MessageUtil.formatEntityName(type.name());
            String eggName = type.name() + "_SPAWN_EGG";
            try {
                this.icon = Material.valueOf(eggName);
            } catch (IllegalArgumentException e) {
                this.icon = Material.BARRIER;
            }
        }

        public Builder displayName(String name) { this.displayName = name; return this; }
        public Builder icon(Material icon) { this.icon = icon; return this; }
        public Builder category(Category cat) { this.category = cat; return this; }
        public Builder maxHealth(double hp) { this.maxHealth = hp; return this; }
        public Builder scale(double s) { this.scale = s; return this; }
        public Builder speed(double mult) { this.speedMultiplier = mult; return this; }
        public Builder attackDamage(double dmg) { this.attackDamage = dmg; return this; }
        public Builder knockbackResistance(double kr) { this.knockbackResistance = kr; return this; }
        public Builder jumpStrength(double js) { this.jumpStrength = js; return this; }
        public Builder gravity(double g) { this.gravity = g; return this; }
        public Builder fallDamageMultiplier(double fdm) { this.fallDamageMultiplier = fdm; return this; }
        public Builder safeFallDistance(double sfd) { this.safeFallDistance = sfd; return this; }
        public Builder canFly() { this.canFly = true; return this; }
        public Builder fireImmune() { this.fireImmune = true; return this; }
        public Builder burnsInSunlight() { this.burnsInSunlight = true; return this; }
        public Builder waterBreathing() { this.waterBreathing = true; return this; }
        public Builder takesWaterDamage() { this.takesWaterDamage = true; return this; }
        public Builder noFallDamage() { this.noFallDamage = true; return this; }

        public Builder passiveEffect(PotionEffectType type, int amplifier) {
            this.passiveEffects.add(new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, true, false, false));
            return this;
        }

        public Builder activeAbility(ActiveAbility ability, String name) {
            this.activeAbility = ability;
            this.activeAbilityName = name;
            return this;
        }

        public Builder attackAbility(ActiveAbility ability, String name) {
            this.attackAbility = ability;
            this.attackAbilityName = name;
            return this;
        }

        public Builder abilityCooldown(int ticks) {
            this.abilityCooldownTicks = ticks;
            return this;
        }

        public MobAbility build() {
            return new MobAbility(this);
        }
    }
}
