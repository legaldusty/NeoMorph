package com.neomorph.ability;

import com.neomorph.ability.MobAbility.ActiveAbility;
import com.neomorph.ability.MobAbility.Category;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry of all mob morphs with correct scale, speed, and abilities.
 *
 * Scale = mob_height / player_height(1.8).
 * This is REQUIRED because LibsDisguises renders the disguise model
 * at the player entity's bounding box size. Without scaling, an Iron Golem
 * gets squished into a player-sized box and looks tiny.
 */
public class AbilityRegistry {

    private final Map<EntityType, MobAbility> abilities = new LinkedHashMap<>();

    public AbilityRegistry() {
        registerAll();
    }

    public MobAbility getAbility(EntityType type) {
        return abilities.get(type);
    }

    public boolean hasAbility(EntityType type) {
        return abilities.containsKey(type);
    }

    public Collection<MobAbility> getAllAbilities() {
        return Collections.unmodifiableCollection(abilities.values());
    }

    public List<MobAbility> getByCategory(Category category) {
        return abilities.values().stream()
                .filter(a -> a.getCategory() == category)
                .sorted(Comparator.comparing(MobAbility::getDisplayName))
                .collect(Collectors.toList());
    }

    private void register(MobAbility ability) {
        abilities.put(ability.getEntityType(), ability);
    }

    private void registerAll() {
        registerPassiveMobs();
        registerNeutralMobs();
        registerHostileMobs();
        registerBossMobs();
    }

    // =========================================================================
    //  PASSIVE MOBS
    // =========================================================================
    private void registerPassiveMobs() {

        // Allay — height 0.6
        register(MobAbility.builder(EntityType.ALLAY)
                .category(Category.PASSIVE)
                .maxHealth(20).scale(0.35).speed(0.8)
                .canFly()
                .passiveEffect(PotionEffectType.GLOWING, 0)
                .build());

        // Armadillo — height 0.65
        register(MobAbility.builder(EntityType.ARMADILLO)
                .category(Category.PASSIVE)
                .maxHealth(12).scale(0.36).speed(0.8)
                .build());

        // Axolotl — height 0.42
        register(MobAbility.builder(EntityType.AXOLOTL)
                .category(Category.PASSIVE)
                .maxHealth(14).scale(0.23).speed(0.7)
                .waterBreathing()
                .passiveEffect(PotionEffectType.REGENERATION, 0)
                .build());

        // Bat — height 0.9
        register(MobAbility.builder(EntityType.BAT)
                .category(Category.PASSIVE)
                .maxHealth(6).scale(0.5).speed(0.7)
                .canFly()
                .passiveEffect(PotionEffectType.NIGHT_VISION, 0)
                .build());

        // Bee — height 0.6
        register(MobAbility.builder(EntityType.BEE)
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.35).speed(0.9)
                .canFly()
                .attackAbility(ActiveAbility.BEE_STING, "Poison Sting")
                .build());

        // Camel — height 2.375
        register(MobAbility.builder(EntityType.CAMEL)
                .category(Category.PASSIVE)
                .maxHealth(32).scale(1.32).speed(1.05)
                .activeAbility(ActiveAbility.CAMEL_DASH, "Dash")
                .abilityCooldown(60)
                .build());

        // Cat — height 0.7
        register(MobAbility.builder(EntityType.CAT)
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.39).speed(1.15)
                .noFallDamage().safeFallDistance(10.0).fallDamageMultiplier(0.0)
                .build());

        // Chicken — height 0.7
        register(MobAbility.builder(EntityType.CHICKEN)
                .category(Category.PASSIVE)
                .maxHealth(4).scale(0.39).speed(0.75)
                .noFallDamage().fallDamageMultiplier(0.0)
                .passiveEffect(PotionEffectType.SLOW_FALLING, 0)
                .build());

        // Cod — height 0.4
        register(MobAbility.builder(EntityType.COD)
                .category(Category.PASSIVE)
                .maxHealth(6).scale(0.22).speed(0.6)
                .waterBreathing()
                .build());

        // Cow — height 1.4
        register(MobAbility.builder(EntityType.COW)
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.78).speed(0.75)
                .build());

        // Donkey — height 1.5
        register(MobAbility.builder(EntityType.DONKEY)
                .category(Category.PASSIVE)
                .maxHealth(22).scale(0.83).speed(0.9)
                .jumpStrength(0.5)
                .build());

        // Fox — height 0.7
        register(MobAbility.builder(EntityType.FOX)
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.39).speed(1.2)
                .passiveEffect(PotionEffectType.NIGHT_VISION, 0)
                .build());

        // Frog — height 0.55
        register(MobAbility.builder(EntityType.FROG)
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.31).speed(0.85)
                .jumpStrength(0.65).noFallDamage().safeFallDistance(10.0)
                .build());

        // Glow Squid — height 0.8
        register(MobAbility.builder(EntityType.GLOW_SQUID)
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.44).speed(0.6)
                .waterBreathing()
                .passiveEffect(PotionEffectType.GLOWING, 0)
                .build());

        // Horse — height 1.6
        register(MobAbility.builder(EntityType.HORSE)
                .category(Category.PASSIVE)
                .maxHealth(26).scale(0.89).speed(1.45)
                .jumpStrength(0.7)
                .build());

        // Llama — height 1.875
        register(MobAbility.builder(EntityType.LLAMA)
                .category(Category.PASSIVE)
                .maxHealth(22).scale(1.04).speed(0.9)
                .activeAbility(ActiveAbility.LLAMA_SPIT, "Spit")
                .abilityCooldown(40)
                .build());

        // Mooshroom — height 1.4
        register(MobAbility.builder(EntityType.MOOSHROOM)
                .displayName("Mooshroom")
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.78).speed(0.75)
                .passiveEffect(PotionEffectType.REGENERATION, 0)
                .build());

        // Mule — height 1.6
        register(MobAbility.builder(EntityType.MULE)
                .category(Category.PASSIVE)
                .maxHealth(22).scale(0.89).speed(0.9)
                .jumpStrength(0.5)
                .build());

        // Ocelot — height 0.7
        register(MobAbility.builder(EntityType.OCELOT)
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.39).speed(1.2)
                .noFallDamage().fallDamageMultiplier(0.0)
                .build());

        // Panda — height 1.25
        register(MobAbility.builder(EntityType.PANDA)
                .category(Category.PASSIVE)
                .maxHealth(20).scale(0.69).speed(0.65)
                .activeAbility(ActiveAbility.PANDA_ROLL, "Roll")
                .abilityCooldown(40)
                .build());

        // Parrot — height 0.9
        register(MobAbility.builder(EntityType.PARROT)
                .category(Category.PASSIVE)
                .maxHealth(6).scale(0.5).speed(0.75)
                .canFly()
                .passiveEffect(PotionEffectType.SLOW_FALLING, 0)
                .build());

        // Pig — height 0.9
        register(MobAbility.builder(EntityType.PIG)
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.5).speed(0.75)
                .build());

        // Pufferfish — height 0.7
        register(MobAbility.builder(EntityType.PUFFERFISH)
                .category(Category.PASSIVE)
                .maxHealth(6).scale(0.39).speed(0.55)
                .waterBreathing()
                .build());

        // Rabbit — height 0.5
        register(MobAbility.builder(EntityType.RABBIT)
                .category(Category.PASSIVE)
                .maxHealth(3).scale(0.28).speed(1.1)
                .jumpStrength(0.7).noFallDamage().safeFallDistance(8.0)
                .build());

        // Salmon — height 0.4
        register(MobAbility.builder(EntityType.SALMON)
                .category(Category.PASSIVE)
                .maxHealth(6).scale(0.22).speed(0.6)
                .waterBreathing()
                .build());

        // Sheep — height 1.3
        register(MobAbility.builder(EntityType.SHEEP)
                .category(Category.PASSIVE)
                .maxHealth(8).scale(0.72).speed(0.75)
                .build());

        // Skeleton Horse — height 1.6
        register(MobAbility.builder(EntityType.SKELETON_HORSE)
                .category(Category.PASSIVE)
                .maxHealth(15).scale(0.89).speed(1.3)
                .jumpStrength(0.7).waterBreathing()
                .build());

        // Sniffer — height 1.75
        register(MobAbility.builder(EntityType.SNIFFER)
                .category(Category.PASSIVE)
                .maxHealth(14).scale(0.97).speed(0.65)
                .build());

        // Snow Golem — height 1.9
        register(MobAbility.builder(EntityType.SNOW_GOLEM)
                .displayName("Snow Golem")
                .category(Category.PASSIVE)
                .maxHealth(4).scale(1.06).speed(0.85)
                .icon(Material.CARVED_PUMPKIN)
                .activeAbility(ActiveAbility.SNOWBALL_THROW, "Snowball")
                .abilityCooldown(10)
                .build());

        // Squid — height 0.8
        register(MobAbility.builder(EntityType.SQUID)
                .category(Category.PASSIVE)
                .maxHealth(10).scale(0.44).speed(0.55)
                .waterBreathing()
                .build());

        // Strider — height 1.7
        register(MobAbility.builder(EntityType.STRIDER)
                .category(Category.PASSIVE)
                .maxHealth(20).scale(0.94).speed(0.85)
                .fireImmune().takesWaterDamage()
                .build());

        // Tadpole — height 0.3
        register(MobAbility.builder(EntityType.TADPOLE)
                .category(Category.PASSIVE)
                .maxHealth(6).scale(0.17).speed(0.5)
                .waterBreathing()
                .build());

        // Tropical Fish — height 0.4
        register(MobAbility.builder(EntityType.TROPICAL_FISH)
                .displayName("Tropical Fish")
                .category(Category.PASSIVE)
                .maxHealth(6).scale(0.22).speed(0.55)
                .waterBreathing()
                .build());

        // Turtle — height 0.4
        register(MobAbility.builder(EntityType.TURTLE)
                .category(Category.PASSIVE)
                .maxHealth(30).scale(0.22).speed(0.45)
                .waterBreathing()
                .passiveEffect(PotionEffectType.RESISTANCE, 0)
                .build());

        // Villager — height 1.95
        register(MobAbility.builder(EntityType.VILLAGER)
                .category(Category.PASSIVE)
                .maxHealth(20).scale(1.08).speed(0.7)
                .build());

        // Wandering Trader — height 1.95
        register(MobAbility.builder(EntityType.WANDERING_TRADER)
                .displayName("Wandering Trader")
                .category(Category.PASSIVE)
                .maxHealth(20).scale(1.08).speed(0.8)
                .icon(Material.WANDERING_TRADER_SPAWN_EGG)
                .build());
    }

    // =========================================================================
    //  NEUTRAL MOBS
    // =========================================================================
    private void registerNeutralMobs() {

        // Cave Spider — height 0.5
        register(MobAbility.builder(EntityType.CAVE_SPIDER)
                .displayName("Cave Spider")
                .category(Category.NEUTRAL)
                .maxHealth(12).scale(0.28).speed(1.0)
                .passiveEffect(PotionEffectType.NIGHT_VISION, 0)
                .activeAbility(ActiveAbility.SPIDER_CLIMB, "Wall Climb")
                .attackAbility(ActiveAbility.BEE_STING, "Poison Bite")
                .abilityCooldown(5)
                .build());

        // Dolphin — height 0.6
        register(MobAbility.builder(EntityType.DOLPHIN)
                .category(Category.NEUTRAL)
                .maxHealth(10).scale(0.33).speed(1.3)
                .waterBreathing()
                .activeAbility(ActiveAbility.DOLPHIN_BOOST, "Dolphin's Grace")
                .abilityCooldown(100)
                .build());

        // Enderman — height 2.9
        register(MobAbility.builder(EntityType.ENDERMAN)
                .category(Category.NEUTRAL)
                .maxHealth(40).scale(1.61).speed(0.95)
                .takesWaterDamage()
                .activeAbility(ActiveAbility.ENDERMAN_TELEPORT, "Teleport")
                .abilityCooldown(20)
                .build());

        // Goat — height 1.3
        register(MobAbility.builder(EntityType.GOAT)
                .category(Category.NEUTRAL)
                .maxHealth(10).scale(0.72).speed(0.85)
                .jumpStrength(0.8).noFallDamage().safeFallDistance(10.0)
                .activeAbility(ActiveAbility.GOAT_RAM, "Ram")
                .abilityCooldown(60)
                .build());

        // Iron Golem — height 2.7
        register(MobAbility.builder(EntityType.IRON_GOLEM)
                .displayName("Iron Golem")
                .category(Category.NEUTRAL)
                .maxHealth(100).scale(1.5).speed(0.65)
                .attackDamage(15.0).knockbackResistance(1.0)
                .noFallDamage()
                .icon(Material.IRON_BLOCK)
                .activeAbility(ActiveAbility.IRON_GOLEM_SLAM, "Ground Slam")
                .abilityCooldown(80)
                .build());

        // Piglin — height 1.95
        register(MobAbility.builder(EntityType.PIGLIN)
                .category(Category.NEUTRAL)
                .maxHealth(16).scale(1.08).speed(0.85)
                .attackDamage(5.0)
                .build());

        // Polar Bear — height 1.4
        register(MobAbility.builder(EntityType.POLAR_BEAR)
                .displayName("Polar Bear")
                .category(Category.NEUTRAL)
                .maxHealth(30).scale(0.78).speed(0.8)
                .attackDamage(6.0)
                .passiveEffect(PotionEffectType.STRENGTH, 0)
                .build());

        // Spider — height 0.9
        register(MobAbility.builder(EntityType.SPIDER)
                .category(Category.NEUTRAL)
                .maxHealth(16).scale(0.5).speed(1.0)
                .passiveEffect(PotionEffectType.NIGHT_VISION, 0)
                .activeAbility(ActiveAbility.SPIDER_CLIMB, "Wall Climb")
                .abilityCooldown(5)
                .build());

        // Trader Llama — height 1.875
        register(MobAbility.builder(EntityType.TRADER_LLAMA)
                .displayName("Trader Llama")
                .category(Category.NEUTRAL)
                .maxHealth(22).scale(1.04).speed(0.9)
                .activeAbility(ActiveAbility.LLAMA_SPIT, "Spit")
                .abilityCooldown(40)
                .build());

        // Wolf — height 0.85
        register(MobAbility.builder(EntityType.WOLF)
                .category(Category.NEUTRAL)
                .maxHealth(8).scale(0.47).speed(1.15)
                .attackDamage(4.0)
                .activeAbility(ActiveAbility.WOLF_HOWL, "Howl")
                .abilityCooldown(200)
                .build());

        // Zombified Piglin — height 1.95
        register(MobAbility.builder(EntityType.ZOMBIFIED_PIGLIN)
                .displayName("Zombified Piglin")
                .category(Category.NEUTRAL)
                .maxHealth(20).scale(1.08).speed(0.75)
                .fireImmune().attackDamage(5.0)
                .build());
    }

    // =========================================================================
    //  HOSTILE MOBS
    // =========================================================================
    private void registerHostileMobs() {

        // Blaze — height 1.8 (same as player)
        register(MobAbility.builder(EntityType.BLAZE)
                .category(Category.HOSTILE)
                .maxHealth(20).speed(0.8)
                .canFly().fireImmune()
                .activeAbility(ActiveAbility.BLAZE_FIREBALL, "Fire Charge")
                .abilityCooldown(30)
                .build());

        // Bogged — height 1.99
        register(MobAbility.builder(EntityType.BOGGED)
                .category(Category.HOSTILE)
                .maxHealth(16).scale(1.11).speed(0.8)
                .burnsInSunlight()
                .build());

        // Breeze — height 1.77
        register(MobAbility.builder(EntityType.BREEZE)
                .category(Category.HOSTILE)
                .maxHealth(20).scale(0.98).speed(0.95)
                .canFly().noFallDamage()
                .activeAbility(ActiveAbility.BREEZE_WIND_CHARGE, "Wind Charge")
                .abilityCooldown(40)
                .build());

        // Creeper — height 1.7
        register(MobAbility.builder(EntityType.CREEPER)
                .category(Category.HOSTILE)
                .maxHealth(20).scale(0.94).speed(0.75)
                .activeAbility(ActiveAbility.CREEPER_EXPLODE, "Explode")
                .abilityCooldown(100)
                .build());

        // Drowned — height 1.95
        register(MobAbility.builder(EntityType.DROWNED)
                .category(Category.HOSTILE)
                .maxHealth(20).scale(1.08).speed(0.75)
                .waterBreathing()
                .activeAbility(ActiveAbility.DROWNED_TRIDENT, "Throw Trident")
                .abilityCooldown(40)
                .build());

        // Elder Guardian — height 1.9975
        register(MobAbility.builder(EntityType.ELDER_GUARDIAN)
                .displayName("Elder Guardian")
                .category(Category.HOSTILE)
                .maxHealth(80).scale(1.11).speed(0.55)
                .waterBreathing()
                .activeAbility(ActiveAbility.ELDER_GUARDIAN_FATIGUE, "Mining Fatigue Aura")
                .abilityCooldown(200)
                .build());

        // Endermite — height 0.3
        register(MobAbility.builder(EntityType.ENDERMITE)
                .category(Category.HOSTILE)
                .maxHealth(8).scale(0.17).speed(0.85)
                .build());

        // Evoker — height 1.95
        register(MobAbility.builder(EntityType.EVOKER)
                .category(Category.HOSTILE)
                .maxHealth(24).scale(1.08).speed(0.7)
                .activeAbility(ActiveAbility.EVOKER_FANGS, "Evoker Fangs")
                .abilityCooldown(60)
                .build());

        // Ghast — height 4.0
        register(MobAbility.builder(EntityType.GHAST)
                .category(Category.HOSTILE)
                .maxHealth(10).scale(2.22).speed(0.5)
                .canFly().fireImmune()
                .activeAbility(ActiveAbility.GHAST_FIREBALL, "Fireball")
                .abilityCooldown(60)
                .build());

        // Guardian — height 0.85
        register(MobAbility.builder(EntityType.GUARDIAN)
                .category(Category.HOSTILE)
                .maxHealth(30).scale(0.47).speed(0.6)
                .waterBreathing()
                .activeAbility(ActiveAbility.GUARDIAN_LASER, "Laser")
                .abilityCooldown(60)
                .build());

        // Hoglin — height 1.4
        register(MobAbility.builder(EntityType.HOGLIN)
                .category(Category.HOSTILE)
                .maxHealth(40).scale(0.78).speed(0.8)
                .attackDamage(8.0)
                .passiveEffect(PotionEffectType.STRENGTH, 0)
                .build());

        // Husk — height 1.95
        register(MobAbility.builder(EntityType.HUSK)
                .category(Category.HOSTILE)
                .maxHealth(20).scale(1.08).speed(0.75)
                .build());

        // Magma Cube — height 2.04 (large)
        register(MobAbility.builder(EntityType.MAGMA_CUBE)
                .displayName("Magma Cube")
                .category(Category.HOSTILE)
                .maxHealth(16).scale(1.13).speed(0.8)
                .fireImmune().noFallDamage()
                .jumpStrength(0.65)
                .build());

        // Phantom — height 0.5
        register(MobAbility.builder(EntityType.PHANTOM)
                .category(Category.HOSTILE)
                .maxHealth(20).scale(0.28).speed(1.1)
                .canFly().burnsInSunlight()
                .activeAbility(ActiveAbility.PHANTOM_DIVE, "Dive Bomb")
                .abilityCooldown(60)
                .build());

        // Piglin Brute — height 1.95
        register(MobAbility.builder(EntityType.PIGLIN_BRUTE)
                .displayName("Piglin Brute")
                .category(Category.HOSTILE)
                .maxHealth(50).scale(1.08).speed(0.85)
                .attackDamage(13.0)
                .passiveEffect(PotionEffectType.STRENGTH, 1)
                .build());

        // Pillager — height 1.95
        register(MobAbility.builder(EntityType.PILLAGER)
                .category(Category.HOSTILE)
                .maxHealth(24).scale(1.08).speed(0.85)
                .attackDamage(4.0)
                .build());

        // Ravager — height 2.2
        register(MobAbility.builder(EntityType.RAVAGER)
                .category(Category.HOSTILE)
                .maxHealth(100).scale(1.22).speed(0.8)
                .attackDamage(12.0).knockbackResistance(0.5)
                .activeAbility(ActiveAbility.RAVAGER_CHARGE, "Charge")
                .abilityCooldown(80)
                .build());

        // Shulker — height 1.0
        register(MobAbility.builder(EntityType.SHULKER)
                .category(Category.HOSTILE)
                .maxHealth(30).scale(0.56).speed(0.3)
                .knockbackResistance(1.0)
                .passiveEffect(PotionEffectType.RESISTANCE, 1)
                .activeAbility(ActiveAbility.SHULKER_BULLET, "Shulker Bullet")
                .abilityCooldown(40)
                .build());

        // Silverfish — height 0.3
        register(MobAbility.builder(EntityType.SILVERFISH)
                .category(Category.HOSTILE)
                .maxHealth(8).scale(0.17).speed(1.0)
                .activeAbility(ActiveAbility.SILVERFISH_SUMMON, "Call Swarm")
                .abilityCooldown(200)
                .build());

        // Skeleton — height 1.99
        register(MobAbility.builder(EntityType.SKELETON)
                .category(Category.HOSTILE)
                .maxHealth(20).scale(1.11).speed(0.8)
                .burnsInSunlight()
                .build());

        // Slime — height 2.04 (large)
        register(MobAbility.builder(EntityType.SLIME)
                .category(Category.HOSTILE)
                .maxHealth(16).scale(1.13).speed(0.8)
                .noFallDamage()
                .jumpStrength(0.65)
                .build());

        // Stray — height 1.99
        register(MobAbility.builder(EntityType.STRAY)
                .category(Category.HOSTILE)
                .maxHealth(20).scale(1.11).speed(0.8)
                .build());

        // Vex — height 0.8
        register(MobAbility.builder(EntityType.VEX)
                .category(Category.HOSTILE)
                .maxHealth(14).scale(0.44).speed(1.1)
                .canFly().noFallDamage()
                .attackDamage(9.0)
                .build());

        // Vindicator — height 1.95
        register(MobAbility.builder(EntityType.VINDICATOR)
                .category(Category.HOSTILE)
                .maxHealth(24).scale(1.08).speed(0.85)
                .attackDamage(8.0)
                .passiveEffect(PotionEffectType.STRENGTH, 0)
                .build());

        // Warden — height 2.9
        register(MobAbility.builder(EntityType.WARDEN)
                .category(Category.HOSTILE)
                .maxHealth(500).scale(1.61).speed(0.7)
                .knockbackResistance(1.0).attackDamage(30.0)
                .noFallDamage()
                .passiveEffect(PotionEffectType.DARKNESS, 0)
                .activeAbility(ActiveAbility.WARDEN_SONIC_BOOM, "Sonic Boom")
                .abilityCooldown(60)
                .build());

        // Witch — height 1.95
        register(MobAbility.builder(EntityType.WITCH)
                .category(Category.HOSTILE)
                .maxHealth(26).scale(1.08).speed(0.85)
                .activeAbility(ActiveAbility.WITCH_POTION, "Splash Potion")
                .abilityCooldown(40)
                .build());

        // Wither Skeleton — height 2.4
        register(MobAbility.builder(EntityType.WITHER_SKELETON)
                .displayName("Wither Skeleton")
                .category(Category.HOSTILE)
                .maxHealth(20).scale(1.33).speed(0.85)
                .fireImmune().attackDamage(8.0)
                .build());

        // Zoglin — height 1.4
        register(MobAbility.builder(EntityType.ZOGLIN)
                .category(Category.HOSTILE)
                .maxHealth(40).scale(0.78).speed(0.8)
                .attackDamage(8.0).fireImmune()
                .build());

        // Zombie — height 1.95
        register(MobAbility.builder(EntityType.ZOMBIE)
                .category(Category.HOSTILE)
                .maxHealth(20).scale(1.08).speed(0.75)
                .burnsInSunlight().attackDamage(3.0)
                .build());

        // Zombie Horse — height 1.6
        register(MobAbility.builder(EntityType.ZOMBIE_HORSE)
                .displayName("Zombie Horse")
                .category(Category.HOSTILE)
                .maxHealth(15).scale(0.89).speed(1.05)
                .jumpStrength(0.6)
                .build());

        // Zombie Villager — height 1.95
        register(MobAbility.builder(EntityType.ZOMBIE_VILLAGER)
                .displayName("Zombie Villager")
                .category(Category.HOSTILE)
                .maxHealth(20).scale(1.08).speed(0.75)
                .burnsInSunlight()
                .build());
    }

    // =========================================================================
    //  BOSS MOBS
    // =========================================================================
    private void registerBossMobs() {

        // Ender Dragon — height 8.0 → scale 4.44, capped at 4.0
        register(MobAbility.builder(EntityType.ENDER_DRAGON)
                .displayName("Ender Dragon")
                .category(Category.BOSS)
                .maxHealth(200).scale(4.0).speed(1.4)
                .canFly().fireImmune().noFallDamage()
                .attackDamage(15.0).knockbackResistance(1.0)
                .icon(Material.DRAGON_HEAD)
                .activeAbility(ActiveAbility.DRAGON_BREATH, "Dragon's Breath")
                .abilityCooldown(100)
                .build());

        // Wither — height 3.5
        register(MobAbility.builder(EntityType.WITHER)
                .category(Category.BOSS)
                .maxHealth(300).scale(1.94).speed(0.9)
                .canFly().fireImmune().noFallDamage()
                .attackDamage(12.0).knockbackResistance(1.0)
                .icon(Material.NETHER_STAR)
                .passiveEffect(PotionEffectType.REGENERATION, 0)
                .activeAbility(ActiveAbility.WITHER_SKULL, "Wither Skull")
                .abilityCooldown(40)
                .build());
    }
}
