package de.luisagrether.idisguise;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import de.luisagrether.idisguise.api.DisguiseAPI;
import de.luisagrether.idisguise.api.EventCancelledException;
import de.luisagrether.idisguise.api.PlayerDisguiseAsPlayerEvent;
import de.luisagrether.idisguise.api.PlayerDisguiseEvent;
import de.luisagrether.idisguise.api.PlayerInteractDisguisedPlayerEvent;
import de.luisagrether.idisguise.api.PlayerUndisguiseEvent;
import de.luisagrether.util.ObjectUtil;
import de.luisagrether.util.StringUtil;

/**
 * Embedded disguise engine based on iDisguise by Luisa Grether.
 * Original source: https://github.com/LuisaGretworthy/iDisguise
 * Licensed under the original iDisguise license.
 * Refactored from standalone plugin into embedded engine for NeoMorph.
 *
 * @author LuisaGrether (original iDisguise)
 * @author NeoMorph Team (embedding/refactoring)
 */
public class iDisguise implements Listener, DisguiseAPI {
	
	public static final Pattern INT_VAL = Pattern.compile("[+-]?[0-9]+");
	public static final Pattern DOUBLE_VAL = Pattern.compile("[+-]?[0-9]*\\.[0-9]+");
	public static final Pattern ENUM_VAL = Pattern.compile("(?:([A-Za-z0-9.]+)\\.){0,1}([A-Za-z0-9_]+)");
	public static final Pattern ITEMSTACK_VAL = Pattern.compile("([A-Za-z0-9_]+),([0-9]+)");
	public static final Pattern STRING_VAL = Pattern.compile("\".*\"");
	public static final Pattern ACCOUNTNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

	private static final Pattern MC_VERSION = Pattern.compile("([0-9]+)\\.([0-9]+)(?:\\.([0-9]+))?");
	private static int[] MINECRAFT_VERSION;
	private static String PACKAGE_VERSION = null;
	private static boolean LEGACY_INJECTION = false;
	private static Method LegacyInjector_inject = null;
	private static Method LegacyInjector_toggleIntercept = null;
	private static Method CraftEntity_getHandle = null;
	private static Method Entity_copyMetadataFrom = null;
	private static boolean LEGACY_DISABLE_AI = false;
	private static Class<?> EntityInsentient = null;
	private static Method EntityInsentient_setNoAI = null;
	private static boolean PLAYER_DISGUISE_AVAILABLE = false;
	private static boolean LEGACY_PROFILES = false;
	private static boolean PLAYER_DISGUISE_VIEWSELF = false;
	private static boolean LEGACY_PLAYER_DISGUISE_VIEWSELF = false;
	private static Method OfflinePlayer_getPlayerProfile = null;
	private static Method PlayerProfile_update = null;
	private static Class<?> CraftPlayerProfile = null;
	private static Constructor<?> CraftPlayerProfile_new = null;
	private static Method CraftPlayerProfile_buildGameProfile = null;
	private static Field CraftOfflinePlayer_profile = null;
	private static Method CraftPlayer_getHandle = null;
	private static Method CraftPlayer_getProfile = null;
	private static Method CraftServer_getServer = null;
	private static Method MinecraftServer_getMinecraftSessionService = null;
	private static Method MinecraftSessionService_fillProfileProperties = null;
	private static Constructor<?> GameProfile_new = null;
	private static Method GameProfile_getProperties = null;
	private static Field PropertyMap_properties = null;
	private static sun.misc.Unsafe UNSAFE = null;
	private static Class<?> EntityPlayer = null;
	private static Field EntityPlayer_playerConnection = null;
	private static Method PlayerConnection_sendPacket = null;
	private static Constructor<?> PacketUpdatePlayerInfo_new = null;
	private static Constructor<?> PacketRemovePlayerInfo_new = null;
	private static Object UpdatePlayerInfo_ADD_PLAYER = null;
	private static Object UpdatePlayerInfo_REMOVE_PLAYER = null;
	private static Method World_setGameRule = null;
	private static Object GameRule_doMobSpawning = null;
	private static Object GameRule_doMobSpawning_value = null;
	private static boolean LEGACY_MATERIALS = false;
	private static Method Material_createBlockData = null;
	private static Class<?> BlockData = null;
	private static Class<?> MaterialData = null;
	private static Constructor<?> MaterialData_new = null;

	private static String formatOBCClass(String path) {
		return PACKAGE_VERSION == null ? "org.bukkit.craftbukkit." + path : "org.bukkit.craftbukkit." + PACKAGE_VERSION + "." + path;
	}

	private static String formatNMSClass(String path) {
		return PACKAGE_VERSION == null ? "net.minecraft.server." + path : "net.minecraft.server." + PACKAGE_VERSION + "." + path;
	}

	static {
		try {
			Matcher m = MC_VERSION.matcher(Bukkit.getBukkitVersion());
			if(m.find()) {
				if(m.group(3) != null) {
					MINECRAFT_VERSION = new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
				} else {
					MINECRAFT_VERSION = new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
				}
			}
			
			PACKAGE_VERSION = Bukkit.getServer().getClass().getName();
			PACKAGE_VERSION = PACKAGE_VERSION.substring(0, PACKAGE_VERSION.lastIndexOf("."));
			PACKAGE_VERSION = PACKAGE_VERSION.substring(PACKAGE_VERSION.lastIndexOf(".") + 1);
			if(PACKAGE_VERSION.equals("craftbukkit")) PACKAGE_VERSION = null;
		} catch(NumberFormatException|IndexOutOfBoundsException e) {
			MINECRAFT_VERSION = null;
		}
		if(MINECRAFT_VERSION != null) {
			if(MINECRAFT_VERSION[1] < 18) {
				LEGACY_INJECTION = true;
				if(MINECRAFT_VERSION[1] >= 14) {
					try {
						Class<?> LegacyInjector = Class.forName("de.luisagrether.idisguise.impl.EntityTracker_" + PACKAGE_VERSION);
						LegacyInjector_inject = LegacyInjector.getMethod("inject", Entity.class, Player.class);
						LegacyInjector_toggleIntercept = LegacyInjector.getMethod("toggleIntercept", Entity.class, Player.class, boolean.class);
					} catch(ClassNotFoundException|NoSuchMethodException e) {
					}
				} else if(MINECRAFT_VERSION[1] >= 8) {
					try {
						Class<?> LegacyInjector = Class.forName("de.luisagrether.idisguise.impl.EntityTrackerEntry_" + PACKAGE_VERSION);
						LegacyInjector_inject = LegacyInjector.getMethod("inject", Entity.class, Player.class);
						LegacyInjector_toggleIntercept = LegacyInjector.getMethod("toggleIntercept", Entity.class, Player.class, boolean.class);
					} catch(ClassNotFoundException|NoSuchMethodException e) {
					}
				}
				try {
					Class<?> CraftEntity = Class.forName(formatOBCClass("entity.CraftEntity"));
					CraftEntity_getHandle = CraftEntity.getMethod("getHandle");
					Class<?> Entity = CraftEntity_getHandle.getReturnType();
					if(MINECRAFT_VERSION[1] == 17) {
						Entity_copyMetadataFrom = Entity.getDeclaredMethod("t", Entity);
					} else if(MINECRAFT_VERSION[1] >= 13) {
						Entity_copyMetadataFrom = Entity.getDeclaredMethod("v", Entity);
					} else if(MINECRAFT_VERSION[1] >= 9) {
						Entity_copyMetadataFrom = Entity.getDeclaredMethod("a", Entity);
					} else {
						Entity_copyMetadataFrom = Entity.getDeclaredMethod("n", Entity);
					}
					Entity_copyMetadataFrom.setAccessible(true);
				} catch(ClassNotFoundException|NoSuchMethodException e) {
					LegacyInjector_inject = null;
				}
				try {
					LivingEntity.class.getDeclaredMethod("setAI", boolean.class);
					// everything fine
				} catch(NoSuchMethodException e) {
					try {
						EntityInsentient = Class.forName(formatNMSClass("EntityInsentient"));
						EntityInsentient_setNoAI = EntityInsentient.getDeclaredMethod("k", boolean.class);
						EntityInsentient_setNoAI.setAccessible(true);
						LEGACY_DISABLE_AI = true;
					} catch(ClassNotFoundException|NoSuchMethodException e2) {
						LegacyInjector_inject = null;
					}
				}
			}
			try {
				Class<?> CraftPlayer = Class.forName(formatOBCClass("entity.CraftPlayer"));
				CraftPlayer_getHandle = CraftPlayer.getMethod("getHandle");
				CraftPlayer_getProfile = CraftPlayer.getMethod("getProfile");

				Class<?> GameProfile = null;
				try {
					OfflinePlayer_getPlayerProfile = OfflinePlayer.class.getDeclaredMethod("getPlayerProfile");
					Class<?> PlayerProfile = OfflinePlayer_getPlayerProfile.getReturnType();
					PlayerProfile_update = PlayerProfile.getDeclaredMethod("update");
					CraftPlayerProfile = Class.forName(formatOBCClass("profile.CraftPlayerProfile"));
					CraftPlayerProfile_new = CraftPlayerProfile.getConstructor(UUID.class, String.class);
					CraftPlayerProfile_buildGameProfile = CraftPlayerProfile.getDeclaredMethod("buildGameProfile");
					GameProfile = CraftPlayerProfile_buildGameProfile.getReturnType();

					LEGACY_PROFILES = false;
				} catch(NoSuchMethodException|ClassNotFoundException e) {
					Class<?> CraftOfflinePlayer = Class.forName(formatOBCClass("CraftOfflinePlayer"));
					CraftOfflinePlayer_profile = CraftOfflinePlayer.getDeclaredField("profile");
					CraftOfflinePlayer_profile.setAccessible(true);
					Class<?> CraftServer = Bukkit.getServer().getClass();
					CraftServer_getServer = CraftServer.getMethod("getServer");
					Class<?> MinecraftServer = CraftServer_getServer.getReturnType();
					for(Method method : MinecraftServer.getMethods()) {
						if(method.getReturnType().getSimpleName().equals("MinecraftSessionService")) {
							MinecraftServer_getMinecraftSessionService = method;
							break;
						}
					}
					if(MinecraftServer_getMinecraftSessionService == null) throw new NoSuchMethodException("Method MinecraftServer.getMinecraftSessionService() not found.");
					
					Class<?> MinecraftSessionService = MinecraftServer_getMinecraftSessionService.getReturnType();
					for(Method method : MinecraftSessionService.getMethods()) {
						if(method.getName().equals("fillProfileProperties")) {
							MinecraftSessionService_fillProfileProperties = method;
							LEGACY_PROFILES = true;
							break;
						}
					}
					if(MinecraftSessionService_fillProfileProperties == null) throw new NoSuchMethodException("Method MinecraftSessionService.fillProfileProperties() not found.");
					
					GameProfile = MinecraftSessionService_fillProfileProperties.getReturnType();
					GameProfile_new = GameProfile.getConstructor(UUID.class, String.class);
				}
				
				for(Method method : GameProfile.getMethods()) {
					if(StringUtil.equals(method.getName(), "getProperties", "properties")) {
						GameProfile_getProperties = method;
						break;
					}
				}
				if(GameProfile_getProperties == null) throw new NoSuchMethodException("Method GameProfile.properties() not found.");

				Class<?> PropertyMap = GameProfile_getProperties.getReturnType();
				PropertyMap_properties = PropertyMap.getDeclaredField("properties");
				PropertyMap_properties.setAccessible(true);
				if(MINECRAFT_VERSION[0] >= 1 && (MINECRAFT_VERSION[1] >= 22 || (MINECRAFT_VERSION[1] == 21 && MINECRAFT_VERSION[2] >= 9))) {
					Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
					theUnsafe.setAccessible(true);
					UNSAFE = (sun.misc.Unsafe)theUnsafe.get(null);
				}

				PLAYER_DISGUISE_AVAILABLE = true;
			} catch(ClassNotFoundException|NoSuchFieldException|NoSuchMethodException|IllegalAccessException e) {
				e.printStackTrace();
			}
			try {
				EntityPlayer = CraftPlayer_getHandle.getReturnType();
				for(Field field : EntityPlayer.getFields()) {
					if(StringUtil.equals(field.getName(), "playerConnection", "connection") || field.getType().getSimpleName().equals("PlayerConnection")) {
						EntityPlayer_playerConnection = field;
						break;
					}
				}
				if(EntityPlayer_playerConnection == null) throw new NoSuchFieldException("Field EntityPlayer.playerConnection not found.");

				Class<?> PlayerConnection = EntityPlayer_playerConnection.getType();
				for(Method method : PlayerConnection.getMethods()) {
					if(StringUtil.equals(method.getName(), "sendPacket", "send") && method.getParameterTypes().length == 1 /*&& method.getParameterTypes()[0].getSimpleName().equals("Packet")*/) {
						PlayerConnection_sendPacket = method;
						break;
					}
				}
				if(PlayerConnection_sendPacket == null) throw new NoSuchMethodException("Method PlayerConnection.sendPacket() not found.");

				try {
					Class<?> PacketRemovePlayerInfo = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
					PacketRemovePlayerInfo_new = PacketRemovePlayerInfo.getConstructor(List.class);
					Class<?> PacketUpdatePlayerInfo = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
					Class<?> UpdatePlayerInfo = null;
					for(Class<?> clazz : PacketUpdatePlayerInfo.getDeclaredClasses()) {
						if(clazz.isEnum()) {
							UpdatePlayerInfo = clazz;
							break;
						}
					}
					PacketUpdatePlayerInfo_new = PacketUpdatePlayerInfo.getConstructor(UpdatePlayerInfo, EntityPlayer);
					UpdatePlayerInfo_ADD_PLAYER = UpdatePlayerInfo.getEnumConstants()[0];
					PLAYER_DISGUISE_VIEWSELF = true;
				} catch(ClassNotFoundException e) {
					try {
						Class<?> PacketUpdatePlayerInfo = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo");
						Class<?> UpdatePlayerInfo = null;
						for(Class<?> clazz : PacketUpdatePlayerInfo.getDeclaredClasses()) {
							if(clazz.isEnum()) {
								UpdatePlayerInfo = clazz;
								break;
							}
						}
						PacketUpdatePlayerInfo_new = PacketUpdatePlayerInfo.getConstructor(UpdatePlayerInfo, Array.newInstance(EntityPlayer, 0).getClass());
						UpdatePlayerInfo_ADD_PLAYER = UpdatePlayerInfo.getEnumConstants()[0];
						UpdatePlayerInfo_REMOVE_PLAYER = UpdatePlayerInfo.getEnumConstants()[UpdatePlayerInfo.getEnumConstants().length - 1];
						PLAYER_DISGUISE_VIEWSELF = true;
						LEGACY_PLAYER_DISGUISE_VIEWSELF = true;
					} catch(ClassNotFoundException e2) {
						try {
							Class<?> PacketUpdatePlayerInfo = Class.forName(formatNMSClass("PacketPlayOutPlayerInfo"));
							Class<?> UpdatePlayerInfo = null;
							for(Class<?> clazz : PacketUpdatePlayerInfo.getDeclaredClasses()) {
								if(clazz.isEnum()) {
									UpdatePlayerInfo = clazz;
									break;
								}
							}
							if(UpdatePlayerInfo == null) {
								UpdatePlayerInfo = Class.forName(formatNMSClass("EnumPlayerInfoAction"));
							}
							PacketUpdatePlayerInfo_new = PacketUpdatePlayerInfo.getConstructor(UpdatePlayerInfo, Array.newInstance(EntityPlayer, 0).getClass());
							UpdatePlayerInfo_ADD_PLAYER = UpdatePlayerInfo.getEnumConstants()[0];
							UpdatePlayerInfo_REMOVE_PLAYER = UpdatePlayerInfo.getEnumConstants()[UpdatePlayerInfo.getEnumConstants().length - 1];
							PLAYER_DISGUISE_VIEWSELF = true;
							LEGACY_PLAYER_DISGUISE_VIEWSELF = true;
						} catch(ClassNotFoundException e3) {
							e.printStackTrace();
						}
					}
				}
				if(PLAYER_DISGUISE_VIEWSELF) {
					try {
						World_setGameRule = World.class.getDeclaredMethod("setGameRuleValue", String.class, String.class);
						GameRule_doMobSpawning = "doMobSpawning";
						GameRule_doMobSpawning_value = "false";
					} catch(NoSuchMethodException e) {
						try {
							Class<?> GameRule = Class.forName("org.bukkit.GameRule");
							World_setGameRule = World.class.getDeclaredMethod("setGameRule", GameRule, Object.class);
							GameRule_doMobSpawning_value = false;
							try {
								GameRule_doMobSpawning = GameRule.getDeclaredField("DO_MOB_SPAWNING").get(null);
							} catch(NoSuchFieldException|IllegalAccessException e2) {
								try {
									GameRule_doMobSpawning = GameRule.getDeclaredField("SPAWN_MOBS").get(null);
								} catch(NoSuchFieldException|IllegalAccessException e3) {
									PLAYER_DISGUISE_VIEWSELF = false;
								}
							}
						} catch(ClassNotFoundException|NoSuchMethodException e2) {
							PLAYER_DISGUISE_VIEWSELF = false;
						}
					}
				}
			} catch(NoSuchFieldException|NoSuchMethodException e) {
				e.printStackTrace();
			}
			try {
				Material_createBlockData = Material.class.getMethod("createBlockData");
				BlockData = Material_createBlockData.getReturnType();
			} catch(NoSuchMethodException e) {
				LEGACY_MATERIALS = true;
				try {
					MaterialData = Class.forName("org.bukkit.material.MaterialData");
					MaterialData_new = MaterialData.getConstructor(Material.class);
				} catch(ClassNotFoundException|NoSuchMethodException e2) {
				}
			}
		}
	}
	
	private static iDisguise INSTANCE;
	private JavaPlugin plugin;
	
	private boolean debugMode = false;
	private Map<UUID, Entity> disguiseMap = new HashMap<>();
	private Map<UUID, String> playerDisguiseMap = new HashMap<>();
	private Map<String, Object> profileDatabase = new HashMap<>();
	private World dummyWorld;
	
	public iDisguise(JavaPlugin plugin) { INSTANCE = this; this.plugin = plugin; }
	
	public void init() {
		if(MINECRAFT_VERSION == null) {
			plugin.getLogger().severe("This Minecraft server version is not supported!");
			return;
		}

		if(LEGACY_INJECTION && LegacyInjector_inject == null) {
			plugin.getLogger().severe("This Minecraft server version is not supported!");
			return;
		}


		// Config handled by NeoMorph
		Bukkit.getPluginManager().registerEvents(this, plugin);

		if(PLAYER_DISGUISE_AVAILABLE && PLAYER_DISGUISE_VIEWSELF && true) {
			dummyWorld = Bukkit.getWorld("iDisguiseDummyWorld");
			if(dummyWorld == null) {
				dummyWorld = Bukkit.createWorld(
					WorldCreator.name("iDisguiseDummyWorld")
					.type(WorldType.FLAT)
					.generateStructures(false)
				);
				try {
					World_setGameRule.invoke(dummyWorld, GameRule_doMobSpawning, GameRule_doMobSpawning_value);
				} catch(InvocationTargetException|IllegalAccessException e) {
				}
				dummyWorld.getWorldBorder().setCenter(dummyWorld.getSpawnLocation());
				dummyWorld.getWorldBorder().setSize(16.0);
			}
		} else {
			try {
				/* Remove dummy world if existent */
				File worldDir = new File(Bukkit.getWorldContainer(), "iDisguiseDummyWorld");
				if(worldDir.isDirectory()) {
					ObjectUtil.deleteDirectory(worldDir);
				}
			} catch(SecurityException e) {
			}
		}
		try {
			/* Remove old dummy world if existent */
			File worldDir = new File(Bukkit.getWorldContainer(), "iDisguise-Dummy");
			if(worldDir.isDirectory()) {
				ObjectUtil.deleteDirectory(worldDir);
			}
		} catch(SecurityException e) {
		}


		Bukkit.getServicesManager().register(DisguiseAPI.class, this, plugin, ServicePriority.Normal);


		plugin.getLogger().info("Enabled!");
	}
	
	public void shutdown() {
		for(Player player : Bukkit.getOnlinePlayers()) {
			if(isDisguised(player)) {
				undisguise0(player);
			}
		}

		for(Entity entity : disguiseMap.values()) {
			entity.remove();
		}
		disguiseMap.clear();
		playerDisguiseMap.clear();

		plugin.getLogger().info("Disabled!");
	}
	public synchronized EntityType getDisguise(Player player) {
		if(disguiseMap.containsKey(player.getUniqueId())) {
			return disguiseMap.get(player.getUniqueId()).getType();
		}
		if(playerDisguiseMap.containsKey(player.getUniqueId())) {
			return EntityType.PLAYER;
		}
		return null;
	}
	
	public synchronized boolean isDisguised(Player player) {
		return disguiseMap.containsKey(player.getUniqueId()) || playerDisguiseMap.containsKey(player.getUniqueId());
	}

	public Entity disguise(Player player, EntityType type, boolean fireEvent) throws EventCancelledException {
		if(false) throw new UnsupportedOperationException("Currently not supported!");
		if(type == EntityType.PLAYER) throw new UnsupportedOperationException();
		
		if(fireEvent) {
			PlayerDisguiseEvent event = new PlayerDisguiseEvent(player, type);
			Bukkit.getPluginManager().callEvent(event);
			if(event.isCancelled()) {
				throw new EventCancelledException();
			}
		}

		return disguise0(player, type);
	}

	private synchronized Entity disguise0(Player player, EntityType type) {
		if(false) throw new UnsupportedOperationException("Currently not supported!");
		if(type == EntityType.PLAYER) throw new UnsupportedOperationException();
		
		if(isDisguised(player)) undisguise0(player);

		Entity entity;
		if(type == EntityType.ITEM) {
			entity = player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.STONE));
		} else {
			entity = player.getWorld().spawnEntity(player.getLocation(), type);
		}
		if(entity instanceof LivingEntity) {
			if(!LEGACY_DISABLE_AI) {
				((LivingEntity)entity).setAI(false);
			} else {
				try {
					Object nmsEntity = CraftEntity_getHandle.invoke(entity);
					if(EntityInsentient.isInstance(nmsEntity)) {
						EntityInsentient_setNoAI.invoke(nmsEntity, true);
					}
				} catch(Exception e) {
					if(debugMode) plugin.getLogger().log(Level.SEVERE, "Unexpected failure!", e);
				}
			}
			// Prevent the disguise entity from pushing the player
			((LivingEntity)entity).setCollidable(false);
		} else if(entity instanceof Item) {
			((Item)entity).setPickupDelay(Integer.MAX_VALUE);
		} else if(entity instanceof TNTPrimed) {
			((TNTPrimed)entity).setFuseTicks(Integer.MAX_VALUE);
		}
		entity.setMetadata("iDisguise", new FixedMetadataValue(plugin, player.getUniqueId()));
		// Add entity to no-collision team on the PLAYER'S scoreboard (not main!)
		try {
			org.bukkit.scoreboard.Scoreboard sb = player.getScoreboard();
			org.bukkit.scoreboard.Team team = sb.getTeam("neomorph_nocol");
			if(team == null) {
				team = sb.registerNewTeam("neomorph_nocol");
				team.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
			}
			team.addEntry(player.getName());
			team.addEntry(entity.getUniqueId().toString());
		} catch(Exception ignored) {}
		if(LEGACY_INJECTION) {
			try {
				LegacyInjector_inject.invoke(null, entity, player);
			} catch(Exception e) {
				if(debugMode) plugin.getLogger().log(Level.SEVERE, "Unexpected failure!", e);
			}
		}
		player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 1, true, false));
		if(type.name().equals("SHULKER")) {
			Location to = player.getLocation();
			entity.teleport(new Location(to.getWorld(), to.getBlockX()+0.5, to.getBlockY()+0.0, to.getBlockZ()+0.5));
		} else {
			entity.teleport(player.getLocation());
		}
		disguiseMap.put(player.getUniqueId(), entity);
		for(Player observer : Bukkit.getOnlinePlayers()) {
			if(observer != player) {
				observer.hidePlayer(plugin, player);
			}
		}

		Entity finalEntity = entity;
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if(!LEGACY_INJECTION) {
				player.hideEntity(plugin, finalEntity);
			} else if(player.getWorld().equals(finalEntity.getWorld())) {
				try {
					LegacyInjector_toggleIntercept.invoke(null, finalEntity, player, true);
				} catch(Exception e) {
					if(debugMode) plugin.getLogger().log(Level.SEVERE, "Unexpected failure!", e);
				}
			}
		}, 100L);

		return entity;
	}

	public void disguiseAsPlayer(Player player, String targetSkin, boolean fireEvent, @Nullable Consumer<Boolean> callback) throws EventCancelledException {
		if(!PLAYER_DISGUISE_AVAILABLE || false) throw new UnsupportedOperationException("Currently not supported!");
		if(!ACCOUNTNAME.matcher(targetSkin).matches()) throw new IllegalArgumentException("This account name is invalid.");
		if(false) throw new IllegalArgumentException("This account name is blacklisted.");
		
		if(fireEvent) {
			PlayerDisguiseAsPlayerEvent event = new PlayerDisguiseAsPlayerEvent(player, targetSkin);
			Bukkit.getPluginManager().callEvent(event);
			if(event.isCancelled()) {
				throw new EventCancelledException();
			}
		}

		disguiseAsPlayer0(player, targetSkin, callback);
	}

	private void disguiseAsPlayer0(Player player, String targetSkin, @Nullable Consumer<Boolean> callback) {
		if(!PLAYER_DISGUISE_AVAILABLE || false) throw new UnsupportedOperationException("Currently not supported!");
		if(!ACCOUNTNAME.matcher(targetSkin).matches()) throw new IllegalArgumentException("This account name is invalid.");
		if(false) throw new IllegalArgumentException("This account name is blacklisted.");
		
		if(isDisguised(player)) undisguise0(player);

		if(!profileDatabase.containsKey(targetSkin.toLowerCase(Locale.ENGLISH))) {
			retrieveProfile(targetSkin, (success) -> {
				if(success) {
					disguiseAsPlayer0(player, targetSkin, callback);
				} else {
					callback.accept(false);
				}
			});
			return;
		}

		synchronized(this) {
			try {
				Object targetProfile = CraftPlayer_getProfile.invoke(player);
				Multimap sourceMap = (Multimap)GameProfile_getProperties.invoke(profileDatabase.get(targetSkin.toLowerCase(Locale.ENGLISH)));
				Multimap targetMap = (Multimap)GameProfile_getProperties.invoke(targetProfile);
				if(sourceMap.containsKey("textures")) {
					if(PropertyMap_properties.get(targetMap).getClass().getSimpleName().contains("Immutable")) {
						UNSAFE.putObject(targetMap, UNSAFE.objectFieldOffset(PropertyMap_properties), LinkedHashMultimap.create((Multimap)PropertyMap_properties.get(targetMap)));
					}
					if(targetMap.containsKey("textures")) {
						targetMap.removeAll("textures");
					}
					targetMap.putAll("textures", sourceMap.get("textures"));
				}
				
				playerDisguiseMap.put(player.getUniqueId(), targetSkin);
				for(Player observer : Bukkit.getOnlinePlayers()) {
					if(observer != player) {
						observer.hidePlayer(plugin, player);
					}
				}
				for(Player observer : Bukkit.getOnlinePlayers()) {
					if(observer != player) {
						observer.showPlayer(plugin, player);
					}
				}

				if(PLAYER_DISGUISE_VIEWSELF && true) {
					Object entityPlayer = CraftPlayer_getHandle.invoke(player);
					if(!LEGACY_PLAYER_DISGUISE_VIEWSELF) {
						Object PacketRemovePlayerInfo = PacketRemovePlayerInfo_new.newInstance(Arrays.asList(player.getUniqueId()));
						Object PacketUpdatePlayerInfo = PacketUpdatePlayerInfo_new.newInstance(UpdatePlayerInfo_ADD_PLAYER, entityPlayer);
						Object playerConnection = EntityPlayer_playerConnection.get(entityPlayer);
						PlayerConnection_sendPacket.invoke(playerConnection, PacketRemovePlayerInfo);
						PlayerConnection_sendPacket.invoke(playerConnection, PacketUpdatePlayerInfo);
					} else {
						Object entityPlayerArray = Array.newInstance(EntityPlayer, 1);
						Array.set(entityPlayerArray, 0, entityPlayer);
						Object PacketRemovePlayerInfo = PacketUpdatePlayerInfo_new.newInstance(UpdatePlayerInfo_REMOVE_PLAYER, entityPlayerArray);
						Object PacketUpdatePlayerInfo = PacketUpdatePlayerInfo_new.newInstance(UpdatePlayerInfo_ADD_PLAYER, entityPlayerArray);
						Object playerConnection = EntityPlayer_playerConnection.get(entityPlayer);
						PlayerConnection_sendPacket.invoke(playerConnection, PacketRemovePlayerInfo);
						PlayerConnection_sendPacket.invoke(playerConnection, PacketUpdatePlayerInfo);
					}

					Location originalLocation = player.getLocation();
					player.teleport(dummyWorld.getSpawnLocation());
					player.teleport(originalLocation);
				}

				if(callback != null) Bukkit.getScheduler().runTask(iDisguise.this.plugin, () -> callback.accept(true));
			} catch(IllegalAccessException|InvocationTargetException|IllegalStateException|InstantiationException e) {
				if(debugMode) plugin.getLogger().log(Level.SEVERE, "Unexpected failure!", e);
				if(callback != null) Bukkit.getScheduler().runTask(iDisguise.this.plugin, () -> callback.accept(false));
			}
		}
	}

	private UUID retrieveProfileUID(String targetSkin) {
		BufferedReader reader = null;
		UUID uid = null;
		try {
			URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + targetSkin.toLowerCase(Locale.ENGLISH));
			URLConnection connection = url.openConnection();
			connection.addRequestProperty("User-Agent", getNameAndVersion().replace(' ', '/') + " (by LuisaGrether)");
			connection.setDoOutput(true);
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String response = "";
			String line;
			while((line = reader.readLine()) != null) {
				response += line;
			}
			JSONObject jsonObject = (JSONObject)JSONValue.parse(response);
			String id = (String)jsonObject.get("id");
			if(!id.contains("-")) {
				id = id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20);
			}
			uid = UUID.fromString(id);
		} catch(IOException e) {
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch(IOException e) {
				}
			}
		}
		return uid;
	}

	private void retrieveProfile(String targetSkin, @Nullable Consumer<Boolean> callback) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			UUID uid = retrieveProfileUID(targetSkin);
			if(uid != null) {
				if(!LEGACY_PROFILES) {
					try {
						Object sourceProfile = CraftPlayerProfile_new.newInstance(uid, targetSkin);
						sourceProfile = ((CompletableFuture)PlayerProfile_update.invoke(sourceProfile)).get();
						
						profileDatabase.put(targetSkin.toLowerCase(Locale.ENGLISH), CraftPlayerProfile_buildGameProfile.invoke(sourceProfile));

						if(callback != null) Bukkit.getScheduler().runTask(iDisguise.this.plugin, () -> callback.accept(true));
					} catch(IllegalAccessException|InvocationTargetException|InstantiationException|ExecutionException|InterruptedException e) {
						if(callback != null) Bukkit.getScheduler().runTask(iDisguise.this.plugin, () -> callback.accept(false));
					}
				} else {
					try {
						Object sourceProfile = GameProfile_new.newInstance(uid, targetSkin);
						MinecraftSessionService_fillProfileProperties.invoke(MinecraftServer_getMinecraftSessionService.invoke(CraftServer_getServer.invoke(Bukkit.getServer())), sourceProfile, true);
						
						profileDatabase.put(targetSkin.toLowerCase(Locale.ENGLISH), sourceProfile);

						if(callback != null) Bukkit.getScheduler().runTask(iDisguise.this.plugin, () -> callback.accept(true));
					} catch(IllegalAccessException|InvocationTargetException|InstantiationException e) {
						if(callback != null) Bukkit.getScheduler().runTask(iDisguise.this.plugin, () -> callback.accept(false));
					}
				}
			} else {
				if(callback != null) Bukkit.getScheduler().runTask(iDisguise.this.plugin, () -> callback.accept(false));
			}
		});
	}
	
	public EntityType undisguise(Player player, boolean fireEvent) throws EventCancelledException {
		if(!isDisguised(player)) return null;

		if(fireEvent) {
			PlayerUndisguiseEvent event = new PlayerUndisguiseEvent(player);
			Bukkit.getPluginManager().callEvent(event);
			if(event.isCancelled()) {
				throw new EventCancelledException();
			}
		}

		return undisguise0(player);
	}

	private synchronized EntityType undisguise0(Player player) {
		if(!isDisguised(player)) return null;
		
		if(getDisguise(player) == EntityType.PLAYER) {
			try {
				Object targetProfile = CraftPlayer_getProfile.invoke(player);
				Multimap sourceMap = (Multimap)GameProfile_getProperties.invoke(profileDatabase.get(player.getName().toLowerCase(Locale.ENGLISH)));
				Multimap targetMap = (Multimap)GameProfile_getProperties.invoke(targetProfile);
				if(sourceMap.containsKey("textures")) {
					if(PropertyMap_properties.get(targetMap).getClass().getSimpleName().contains("Immutable")) {
						UNSAFE.putObject(targetMap, UNSAFE.objectFieldOffset(PropertyMap_properties), LinkedHashMultimap.create((Multimap)PropertyMap_properties.get(targetMap)));
					}
					if(targetMap.containsKey("textures")) {
						targetMap.removeAll("textures");
					}
					targetMap.putAll("textures", sourceMap.get("textures"));
				}
				
				playerDisguiseMap.remove(player.getUniqueId());
				for(Player observer : Bukkit.getOnlinePlayers()) {
					if(observer != player) {
						observer.hidePlayer(plugin, player);
					}
				}
				for(Player observer : Bukkit.getOnlinePlayers()) {
					if(observer != player) {
						observer.showPlayer(plugin, player);
					}
				}

				if(PLAYER_DISGUISE_VIEWSELF && true) {
					Object entityPlayer = CraftPlayer_getHandle.invoke(player);
					if(!LEGACY_PLAYER_DISGUISE_VIEWSELF) {
						Object PacketRemovePlayerInfo = PacketRemovePlayerInfo_new.newInstance(Arrays.asList(player.getUniqueId()));
						Object PacketUpdatePlayerInfo = PacketUpdatePlayerInfo_new.newInstance(UpdatePlayerInfo_ADD_PLAYER, entityPlayer);
						Object playerConnection = EntityPlayer_playerConnection.get(entityPlayer);
						PlayerConnection_sendPacket.invoke(playerConnection, PacketRemovePlayerInfo);
						PlayerConnection_sendPacket.invoke(playerConnection, PacketUpdatePlayerInfo);
					} else {
						Object entityPlayerArray = Array.newInstance(EntityPlayer, 1);
						Array.set(entityPlayerArray, 0, entityPlayer);
						Object PacketRemovePlayerInfo = PacketUpdatePlayerInfo_new.newInstance(UpdatePlayerInfo_REMOVE_PLAYER, entityPlayerArray);
						Object PacketUpdatePlayerInfo = PacketUpdatePlayerInfo_new.newInstance(UpdatePlayerInfo_ADD_PLAYER, entityPlayerArray);
						Object playerConnection = EntityPlayer_playerConnection.get(entityPlayer);
						PlayerConnection_sendPacket.invoke(playerConnection, PacketRemovePlayerInfo);
						PlayerConnection_sendPacket.invoke(playerConnection, PacketUpdatePlayerInfo);
					}

					Location originalLocation = player.getLocation();
					player.teleport(dummyWorld.getSpawnLocation());
					player.teleport(originalLocation);
				}
				
				return EntityType.PLAYER;
			} catch(IllegalAccessException|InvocationTargetException|IllegalStateException|InstantiationException e) {
				if(debugMode) plugin.getLogger().log(Level.SEVERE, "Unexpected failure!", e);
				return null;
			}
		} else {
			Entity entity = disguiseMap.remove(player.getUniqueId());
			// Remove entity from no-collision team on player's scoreboard
			try {
				org.bukkit.scoreboard.Scoreboard sb = player.getScoreboard();
				org.bukkit.scoreboard.Team team = sb.getTeam("neomorph_nocol");
				if(team != null) {
					team.removeEntry(entity.getUniqueId().toString());
				}
			} catch(Exception ignored) {}
			entity.remove();
			for(Player observer : Bukkit.getOnlinePlayers()) {
				if(observer != player) {
					observer.showPlayer(plugin, player);
				}
			}
			return entity.getType();
		}
	}
	
	@EventHandler
	public void handlePlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		for(Entry<UUID, Entity> entry : disguiseMap.entrySet()) {
			player.hidePlayer(plugin, Bukkit.getPlayer(entry.getKey()));
		}

		String targetSkin = player.getName().toLowerCase(Locale.ENGLISH);
		if(!profileDatabase.containsKey(targetSkin)) retrieveProfile(targetSkin, null);

	}
	
	@EventHandler
	public void handlePlayerQuit(PlayerQuitEvent event) {
		if(isDisguised(event.getPlayer())) {
			undisguise0(event.getPlayer());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void handlePlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if(!event.isCancelled() && disguiseMap.containsKey(player.getUniqueId())) {
			Entity entity = disguiseMap.get(player.getUniqueId());
			World worldFrom = entity.getWorld();
			if(LEGACY_INJECTION && !event.getTo().getWorld().equals(worldFrom)) {
				undisguise0(player);
				Entity newEntity = disguise0(player, entity.getType());
				try {
					Entity_copyMetadataFrom.invoke(CraftEntity_getHandle.invoke(newEntity), CraftEntity_getHandle.invoke(entity));
				} catch(IllegalAccessException|InvocationTargetException e) {
					if(debugMode) plugin.getLogger().log(Level.SEVERE, "Unexpected failure!", e);
				}
			} else {
				if(entity.getType().name().equals("SHULKER")) {
					Location to = event.getTo();
					entity.teleport(new Location(to.getWorld(), to.getBlockX()+0.5, to.getBlockY()+0.0, to.getBlockZ()+0.5));
				} else {
					entity.teleport(event.getTo());
				}
				entity.setVelocity(player.getVelocity());
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void handleEntityDamageLowest(EntityDamageEvent event) {
		if(event.getEntity().hasMetadata("iDisguise")) {
			if(StringUtil.equals(event.getCause().name(), "DROWNING", "DRYOUT", "FLY_INTO_WALL", "SUFFOCATION")) {
				event.setCancelled(true);
			}
		}
		if(event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent event2 = (EntityDamageByEntityEvent)event;
			if(event2.getDamager().hasMetadata("iDisguise")) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void handleEntityDamageMonitor(EntityDamageEvent event) {
		if(event.getEntity().hasMetadata("iDisguise")) {
			if(!event.isCancelled()) {
				Player player = Bukkit.getPlayer((UUID)event.getEntity().getMetadata("iDisguise").get(0).value());
				player.damage(event.getDamage());
				if(debugMode) plugin.getLogger().info("Dealt damage (" + event.getCause().name() + "," + event.getDamage() + ") to " + player.getName());
				event.setDamage(Double.MIN_VALUE);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void handlePlayerInteractEntity(PlayerInteractEntityEvent event) {
		if(event.getRightClicked().hasMetadata("iDisguise")) {
			event.setCancelled(true);
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				Bukkit.getPluginManager().callEvent(new PlayerInteractDisguisedPlayerEvent(
					event.getPlayer(),
					Bukkit.getPlayer((UUID)event.getRightClicked().getMetadata("iDisguise").get(0).value())
				));
			}, 1L);
		} else if(event.getRightClicked() instanceof Player && playerDisguiseMap.containsKey(((Player)event.getRightClicked()).getUniqueId())) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				Bukkit.getPluginManager().callEvent(new PlayerInteractDisguisedPlayerEvent(
					event.getPlayer(),
					(Player)event.getRightClicked()
				));
			}, 1L);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void handleEntityCombustLowest(EntityCombustEvent event) {
		if(event.getEntity().hasMetadata("iDisguise")) {
			if(!(event instanceof EntityCombustByBlockEvent || event instanceof EntityCombustByEntityEvent)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void handleEntityCombustMonitor(EntityCombustEvent event) {
		if(event.getEntity().hasMetadata("iDisguise")) {
			if(!event.isCancelled()) {
				Bukkit.getPlayer((UUID)event.getEntity().getMetadata("iDisguise").get(0).value()).setFireTicks((int)event.getDuration());
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void handleEntityTarget(EntityTargetEvent event) {
		if(event.getTarget() != null && event.getTarget().hasMetadata("iDisguise")) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void handleEntityBlockForm(EntityBlockFormEvent event) {
		if(event.getEntity().hasMetadata("iDisguise")) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void handleEntityPortalEvent(EntityPortalEvent event) {
		if(event.getEntity().hasMetadata("iDisguise")) {
			event.setCancelled(true);
		}
	}

	
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}
	
	public String getNameAndVersion() {
		return "NeoMorph " + getVersion();
	}

	
	public boolean debugMode() {
		return debugMode;
	}
	
	public JavaPlugin getPlugin() {
		return plugin;
	}

	public static iDisguise getInstance() {
		return INSTANCE;
	}
	
}
