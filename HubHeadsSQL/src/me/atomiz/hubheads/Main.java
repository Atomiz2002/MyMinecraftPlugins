package me.atomiz.hubheads;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.gmail.filoghost.holographicdisplays.api.Hologram;

import net.citizensnpcs.api.npc.NPC;

public class Main extends JavaPlugin { // NO_UCD (use default)

	private static Main main;
	static boolean enabled;
	static boolean debug;
	static boolean citizens;
	static boolean holograms;
	static boolean protocollib;
	static ProtocolManager manager;

	static String host, port, database, username, password;
	static Connection SQLconnection;
	static Statement statement;
	static int connectTimeoutSeconds;

	static FileConfiguration config;

	static List<Location> busyLocations = new ArrayList<>();
	static List<Location> availableLocations = new ArrayList<>();

	static String hub;
	static int eventDelay = 1440;
	static int eventTimeout = 300;
	static int maxHeadsAtOnce = 0;
	static int maxHeadsPerPlayer = 0;
	static int headsGoal = 10;
	static int headsUnableToSpawn = 0;
	static int radiusX = 0;
	static int radiusY = 0;
	static int radiusZ = 0;

	static int[] triggersDelay;
	static String eventStartMessage;
	static String eventFindMessage;
	static String eventEndMessage;
	static String eventTimeoutMessage;
	static String eventWaitMessage;
	static String errorMessage;
	static String wrongMessage;

	static List<String> validHeadsBlocks = new ArrayList<>();
	static List<String> presentsSkinsValues = new ArrayList<>();
	static List<Entity> presents = new ArrayList<>();

	static boolean executeTriggersAsCommands = true;
	static List<String[]> startCommands = new ArrayList<>();
	static List<String[]> existCommands = new ArrayList<>();
	static List<String[]> findCommands = new ArrayList<>();
	static List<String[]> endCommands = new ArrayList<>();
	static List<String[]> timeoutCommands = new ArrayList<>();

	static List<Player> holosReady = new ArrayList<>(); // the holos of the players who can run the event (so it doesnt update unnecessarily)
	static List<String> holoReady = new ArrayList<>();
	static long holoRunningUpdate = 20L;
	static List<String> holoRunning = new ArrayList<>();
	static long holoWaitingUpdate = 1200L;
	static List<String> holoWaiting = new ArrayList<>();

	static Map<UUID, Integer> headsSpawned = new HashMap<>();
	static ConcurrentHashMap<UUID, Integer> onlinePlayerHeads = new ConcurrentHashMap<>();
	static ConcurrentHashMap<UUID, Long> onlinePlayerDelay = new ConcurrentHashMap<>();
	static ConcurrentHashMap<UUID, Long> onlinePlayerTimeout = new ConcurrentHashMap<>();

	static Map<Player, List<Hologram>> holos = new HashMap<>();
	static Map<NPC, List<Hologram>> npcs = new HashMap<>();

	static Main main() {
		return main;
	}

	@Override
	public void onEnable() {
		Helpers.p("onEnable");
		main = this;
		enabled = true;

		main.saveDefaultConfig();
		config = main.getConfig();
		config.options().copyDefaults();
		main.saveConfig();

		citizens = Helpers.isPluginEnabled("Citizens");
		holograms = Helpers.isPluginEnabled("HolographicDisplays");
		protocollib = Helpers.isPluginEnabled("ProtocolLib");

		if (protocollib)
			manager = ProtocolLibrary.getProtocolManager();

		loadConfig();

		Helpers.scanWorldBlocks(getServer().getConsoleSender());

		// register the events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new Events(), this);
		// register the commands
		getCommand("hubheads").setExecutor(new Commands());

		saveConfig();

		Helpers.pr(ChatColor.GREEN + "Enabled", Logger.INFO);
		if (!enabled)
			Helpers.pr(
					"The plugin will not function until the errors above are resolved. You can use /hh reload to reload the config",
					Logger.WARN);

		if (enabled)
			new BukkitRunnable() {

				@Override
				public void run() {
					for (Player p : getServer().getOnlinePlayers())
						if (p.getWorld().getName().equals(hub) && !holosReady.contains(p)) {
							if (Helpers.getEventRemainingDelay(p).equals("0:00")
									&& onlinePlayerHeads.getOrDefault(p.getUniqueId(), -1) < 0) // if the event can start and isnt running
								holosReady.add(p);
							Helpers.updateHolograms(p);
						}
				}
			}.runTaskTimer(Main.main(), 0L, holoWaitingUpdate);
	}

	static void loadConfig() {
		Helpers.p("ReloadConfig");

		host = config.getString("SQL.host");
		port = config.getString("SQL.port");
		database = config.getString("SQL.database");
		username = config.getString("SQL.username");
		password = config.getString("SQL.password");
		connectTimeoutSeconds = config.getInt("SQL.connectTimeoutSeconds");

		SQL.testConnection();

		// check if the world is valid otherwise disable the plugin
		hub = config.getString("hub", "world");
		if (Bukkit.getWorld(hub) == null) {
			Helpers.pr(ChatColor.RED + "Invalid world specified", Logger.ERROR);
			enabled = false || enabled;
			return;
		} else
			enabled = true && enabled;

		// TODO placeholder for found heads
		// TODO timer 10 - 1

		eventDelay = config.getInt("delay");
		eventTimeout = config.getInt("timeout");
		maxHeadsAtOnce = config.getInt("maxHeadsAtOnce") == 0 ? Integer.MAX_VALUE : config.getInt("maxHeadsAtOnce");
		maxHeadsPerPlayer = config.getInt("maxHeadsPerPlayer");
		headsGoal = config.getInt("headsGoal");
		presentsSkinsValues = config.getStringList("presentsSkinsValues");

		validHeadsBlocks = config.getStringList("validBlocks");
		List<String> toRemove = new ArrayList<>();
		validHeadsBlocks.replaceAll(block -> block.toUpperCase());

		validHeadsBlocks.forEach(block -> {
			if (Material.getMaterial(block) == null) {
				Helpers.pr(ChatColor.YELLOW + block + " is not a valid block type", Logger.WARN);
				toRemove.add(block);
			}
		});
		validHeadsBlocks.removeAll(toRemove);
		toRemove.clear();
		if (validHeadsBlocks.isEmpty()) {
			Helpers.pr(ChatColor.RED + "There are no valid blocks specified", Logger.ERROR);
			enabled = false;
			return;
		}

		radiusX = config.getInt("radius.x");
		radiusY = config.getInt("radius.y");
		radiusZ = config.getInt("radius.z");

		executeTriggersAsCommands = config.getBoolean("triggers.executeCommands");
		String[] delay = config.getString("triggers.delay").split(";");
		triggersDelay = new int[] { Integer.parseInt(delay[0]), Integer.parseInt(delay[1]) };
		startCommands.clear();
		config.getStringList("triggers.start").forEach(line -> {
			startCommands.add(Helpers.formatCommandsLines(line));
		});
		existCommands.clear();
		config.getStringList("triggers.exist").forEach(line -> {
			existCommands.add(Helpers.formatCommandsLines(line));
		});
		findCommands.clear();
		config.getStringList("triggers.find").forEach(line -> {
			findCommands.add(Helpers.formatCommandsLines(line));
		});
		endCommands.clear();
		config.getStringList("triggers.end").forEach(line -> {
			endCommands.add(Helpers.formatCommandsLines(line));
		});
		timeoutCommands.clear();
		config.getStringList("triggers.timeout").forEach(line -> {
			timeoutCommands.add(Helpers.formatCommandsLines(line));
		});

		eventStartMessage = config.getString("message.start");
		eventWaitMessage = config.getString("message.wait");
		eventEndMessage = config.getString("message.end");
		eventTimeoutMessage = config.getString("message.timeout");
		eventFindMessage = config.getString("message.find");
		errorMessage = config.getString("message.error");
		wrongMessage = config.getString("message.wrong");

		holoReady = config.getStringList("hologram.ready.lines");
		holoRunningUpdate = config.getInt("hologram.running.update");
		holoRunning = config.getStringList("hologram.running.lines");
		holoWaitingUpdate = config.getInt("hologram.waiting.update");
		holoWaiting = config.getStringList("hologram.waiting.lines");
	}

	/*
	 * Presents spawning logic
	 * task: all remaining heads for the players within the maxheadsperplayer bounds (min)
	 * goal = 10
	 * maxheadsperplayer = 3
	 * ph(playerheads) = 6
	 * min(maxheadsperplayer, goal - ph = 4) = 3
	 * ph = 9
	 * min(maxheadsperplayer, goal - ph = 1) = 1
	 * ph = -1
	 * min(maxheadsperplayer, goal(10) - ph(-1) = 11) = 11
	 * ph = 6
	 * min(maxheadsperplayer, (ph < 0 ? 0 : goal - ph = 4)) = 3
	 * ph = 9
	 * min(maxheadsperplayer, (ph < 0 ? 0 : goal - ph = 1)) = 1
	 * ph = -1
	 * min(maxheadsperplayer, (ph < 0 ? 0 : goal - ph = 11)) = 0
	 * 
	 * This forumula will get the remaining player heads within the max per player bounds
	 * min(maxheadsperplayer, (ph < 0 ? 0 : goal - ph))
	 * 
	 */
	static void StartEvent(Player p, boolean force) {
		Helpers.p("StartEvent");
		synchronized (Main.main()) {
			if (Main.main().isEnabled()) {
				UUID uuid = p.getUniqueId();
				Helpers.removeHeadsFromInventory(p);
				if (onlinePlayerHeads.getOrDefault(uuid, -1) >= 0) { // event has started
					if (onlinePlayerHeads.get(uuid) >= headsGoal) { // if the goal is cleared
						Helpers.NotifyPlayer(p, eventEndMessage);
						Helpers.onceScheduler(p, endCommands, null);
						onlinePlayerHeads.put(uuid, -1);
						onlinePlayerDelay.put(uuid, Helpers.getRealTimeInMillis());
						onlinePlayerTimeout.remove(uuid);
						holosReady.remove(p);
						Helpers.updateHolograms(p);
					} else if (Helpers.getEventRemainingTimeout(p) != 0) { // if not timed out
						if (!availableLocations.isEmpty()) {
							headsUnableToSpawn = 0;
							while (presents.size() < maxHeadsAtOnce && headsSpawned.get(uuid) + headsUnableToSpawn < Integer
									.min(maxHeadsPerPlayer, headsGoal - onlinePlayerHeads.get(uuid)))
								SpawnPresent(p);
						} else
							// will try to find available location and say there is none
							SpawnPresent(p);
					} else { // when timed out
						Helpers.NotifyPlayer(p, eventTimeoutMessage);
						Helpers.onceScheduler(p, timeoutCommands, null);
						onlinePlayerHeads.put(uuid, -1);
						onlinePlayerDelay.put(uuid, Helpers.getRealTimeInMillis());
						onlinePlayerTimeout.remove(uuid);
						int remove = Integer.min(Integer.min(Main.maxHeadsPerPlayer, Main.busyLocations.size()),
								Main.headsGoal - Main.onlinePlayerHeads.get(uuid));
						for (int i = remove; i > 0; --i)
							Helpers.removePresent(p, Helpers.getRandomElement(Main.presents), false);
					}
				} else if (Helpers.getEventRemainingDelay(p).equals("0:00")) { // event can start so it will
					if (holosReady.contains(p))
						holosReady.remove(p);
					onlinePlayerHeads.put(uuid, 0);
					Helpers.onceScheduler(p, startCommands, null);
					Helpers.startEventTimeout(p);
					StartEvent(p, false);
				} else if (force) { // the event should start so reset the player first
					onlinePlayerHeads.put(uuid, -1);
					onlinePlayerDelay.put(uuid, -1L);
					onlinePlayerTimeout.put(uuid, -1L);
					StartEvent(p, false);
				} else
					Helpers.NotifyPlayer(p, Main.eventWaitMessage);
				Helpers.updateHolograms(p);
			}
		}
	}

	private static void SpawnPresent(Player p) {
		Helpers.p("SpawnPresent");
		if (!Helpers.schedulerRepeats)
			Helpers.repeatScheduler(p, existCommands);
		Helpers.summonHead(Helpers.findAvailableLocation(p), Helpers.getRandomElement(presentsSkinsValues));
	}

	@Override
	public void onDisable() {
		Helpers.p("onDisable");
		presents.forEach(Entity::remove);
		synchronized (main) {
			if (SQL.openConnection()) {
				onlinePlayerHeads.keySet().forEach(uuid -> {
					try {
						statement.execute("REPLACE INTO HubHeads VALUES('" + uuid + "'," + Main.onlinePlayerHeads.get(uuid) + ","
								+ Main.onlinePlayerDelay.get(uuid) + "," + Main.onlinePlayerTimeout.getOrDefault(uuid, -1L)
								+ ")");
						statement = SQLconnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
					} catch (SQLException e) {}
				});
				SQL.closeConnection();
			} else
				Helpers.pr("Couldn't save players' data to the database", Logger.ERROR);
		}
		Helpers.pr(ChatColor.RED + "Disabled", Logger.INFO);
	}

	enum Logger {
		INFO,
		WARN,
		ERROR
	}
}
