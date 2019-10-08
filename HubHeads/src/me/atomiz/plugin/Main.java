package me.atomiz.plugin;

import java.io.File;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

	ConsoleCommandSender console = getServer().getConsoleSender();
	static FileConfiguration config;
	private File customConfigFile;
	private FileConfiguration playerData;

	static long time = 0;
	static int maxHeads = 0;
	static int existingHeads = 0;
	static int found = 0; // the amount of heads the player with most heads has
	static int goal = 10; // the goal amount of heads to be found by one player
	static boolean ended = true;

	@Override
	public void onEnable() {

		// fix the config
		saveDefaultConfig();
		config = getConfig();
		config.options().copyDefaults();
		saveConfig();

		// initialize vars
		time = Bukkit.getWorld(config.getString("hub")).getTime();
		maxHeads = config.getInt("max_heads");
		goal = config.getInt("goal");

		// wait until specified time to spawn heads
		waitUntil(Remaining(time, config.getLong("time")));

		// register the events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);

		console.sendMessage(ChatColor.GREEN + "Enabled " + getDescription().getName());
		saveConfig();
	}

	@Override
	public void onDisable() {

		console.sendMessage(ChatColor.RED + "Disabled " + getDescription().getName());

	}

	/*
	 * private void registerCommands() {
	 * 
	 * getCommand("hubheads").setExecutor(new HubHeads());
	 * 
	 * }
	 * 
	 * private void registerEvents() { PluginManager pm = getServer().getPluginManager();
	 * 
	 * pm.registerEvents(new TimeDetect(), this); pm.registerEvents(new HeadPickup(), this); pm.registerEvents(new OnComplete(), this);
	 * 
	 * }
	 */

	// should wait until the server ticks are equal to the 'time' variable in the config

	private void waitUntil(long delay) {
		Bukkit.getScheduler().runTaskLater(this, new Runnable() {

			@Override
			public void run() {

				ended = false;
				// createCustomConfig();
				GenerateHeads();
			}

		}, delay + 2L);
	}

	// should return the ticks until the server time becomes equal to the 'time' variable in the config

	// calculates the remaining time until it should spawn the heads
	private long Remaining(long server, long config) {
		long remaining;

		if (server > config)
			remaining = 24000 - server + config;
		else
			remaining = config - server;

		return remaining;
	}

//
	public void GenerateHeads() {
		console.sendMessage("Generating heads");
		if (ended == false)
			if (existingHeads < maxHeads)
				SpawnHead();

//		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
//
//			@Override
//			public void run() {
//				System.out.println(ChatColor.BLUE + "Spawning head");
//				time = Bukkit.getWorld(config.getString("hub")).getTime();
//				while (ended == false) {
//					if (found < goal) {
//						
//					}
//				}
//			}
//		}, 0L, 0L);
	}

	public void SpawnHead() {
		// TODO continue from here
		console.sendMessage("Spawning head");
		Location spawn = getServer().getWorld(config.getString("hub")).getSpawnLocation();
		int configX = config.getInt("radius_x");
		int configY = config.getInt("radius_y");
		int configZ = config.getInt("radius_z");
		int randomX = randomInteger((int) spawn.getX() - configX / 2, (int) spawn.getX() + configX / 2);
		int randomY = randomInteger((int) spawn.getY() - configY / 2, (int) spawn.getY() + configY / 2);
		int randomZ = randomInteger((int) spawn.getZ() - configZ / 2, (int) spawn.getZ() + configZ / 2);
		Block block = getServer().getWorld(config.getString("hub")).getBlockAt(randomX, randomY, randomZ);
		if (block.getType() == Material.AIR)
			block.setType(Material.PLAYER_WALL_HEAD);
		else
			SpawnHead();
	}

	public int randomInteger(int min, int max) {
		if (min >= max)
			throw new IllegalArgumentException("max must be greater than min");
		return new Random().nextInt(max - min + 1) + min;
	}

	@EventHandler
	private void HeadBreak(PlayerInteractEvent e) {
		e.getPlayer().sendMessage("" + ended);
		if (ended == false) {
			e.getPlayer().sendMessage("hi");
			if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				e.getPlayer().sendMessage("hi2");
				// TODO this doesnt detect on head click help it
				if (e.getClickedBlock().getType() == Material.PLAYER_HEAD) {
					e.getPlayer().sendMessage("hi3");

					e.getClickedBlock().breakNaturally();

					String uuid = e.getPlayer().getUniqueId().toString();

					if (!getCustomConfig().contains(uuid))
						getCustomConfig().set(uuid, 1);
					else
						getCustomConfig().set(uuid, getCustomConfig().getInt(uuid) + 1);

					if (getCustomConfig().getInt(uuid) == goal) {
						ended = true;
						// TODO something to announce the winner
						// TODO? v remove the .temp file
						customConfigFile.delete();
					} else
						GenerateHeads();
				}
			}
		}
	}

	public FileConfiguration getCustomConfig() {
		return playerData;
	}
}
