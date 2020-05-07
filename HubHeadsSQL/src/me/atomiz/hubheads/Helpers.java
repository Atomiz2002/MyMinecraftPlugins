package me.atomiz.hubheads;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import me.atomiz.hubheads.Main.Logger;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import net.citizensnpcs.api.npc.NPC;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

class Helpers {

	private static List<Integer> selfDelays = new ArrayList<>();
	static boolean schedulerRepeats = false;

	static boolean isPluginEnabled(String name) {
		if (Main.main().getServer().getPluginManager().getPlugin(name) == null
				|| Main.main().getServer().getPluginManager().getPlugin(name).isEnabled() == false) {
			Helpers.pr(name + " is not enabled!", Logger.WARN);
			return false;
		}
		return true;
	}

	static void Editor(Player p, String message, HoverEvent.Action hoverAction, String hoverText, Action clickAction,
			String clickText) {

		TextComponent text = new TextComponent();
		text.setText(message);
		if (clickText != null)
			text.setClickEvent(new ClickEvent(clickAction, clickText));
		if (hoverText != null)
			text.setHoverEvent(new HoverEvent(hoverAction, new ComponentBuilder(hoverText).create()));

		p.spigot().sendMessage(text);
	}

	/**
	 * Sends a message from the config messages to the player
	 *
	 * @param p       the player
	 * @param message the message
	 */
	static void NotifyPlayer(Player p, String message) {
		Helpers.p("NotifyPlayer");
		if (p != null)
			switch (message.charAt(0)) {
			case 'c':
				p.sendMessage(formatPlaceholders(p, message).substring(2));
				break;
			case 't':
				try {
					String[] messages = formatPlaceholders(p, message).substring(2).split(";");
					if (messages.length > 2)
						p.sendTitle(messages[0], messages[1], Integer.parseInt(messages[2]) * 20,
								Integer.parseInt(messages[3]) * 20, Integer.parseInt(messages[4]) * 20);
					else
						p.sendTitle(messages[0], messages[1], 20, 60, 20);
				} catch (ArrayIndexOutOfBoundsException e) {
					Bukkit.getLogger().warning("Something went wrong with the titles. Please check your configuration");
					NotifyPlayer(p, formatPlaceholders(p, Main.errorMessage));
				}
				break;
			case 'h':
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
						new TextComponent(formatPlaceholders(p, message.substring(2))));
				break;
			default:
				p.sendMessage(formatPlaceholders(p, message));
				break;
			}
	}

	static String[] formatCommandsLines(String line) {
		Helpers.p("formatCommandsLines");
		switch (line.split(";").length) {
		case 2:
			if (Character.isDigit(line.charAt(0)))
				return new String[] { line.split(";")[0], line.split(";")[1], "" + Main.triggersDelay[0] };
			else
				return new String[] { 0 + "", line.split(";")[0], line.split(";")[1] };
		case 3:
			return line.split(";");
		default:
			return new String[] { "0", line, "" + Main.triggersDelay[1] };
		}
	}

	private static String formatPlaceholders(Player p, String string) {
		String seconds = "";
		if (string.contains("%tm%")) {
			string = string.replace("%tm%", TimeUnit.SECONDS.toMinutes(getEventRemainingTimeout(p)) + "");

			if (string.contains("%ts%")) {
				if (getEventRemainingTimeout(p) % 60 < 10)
					seconds = getEventRemainingTimeout(p) % 60 + "0";
				else
					seconds = getEventRemainingTimeout(p) % 60 + "";
				string = string.replace("%ts%", seconds);
			}
		} else if (string.contains("%ts%")) {
			if (getEventRemainingTimeout(p) < 10)
				seconds = getEventRemainingTimeout(p) + "0";
			else
				seconds = getEventRemainingTimeout(p) + "";
			string = string.replace("%ts%", seconds);
		}

		// skip %hl%, %hf% and %pl% to be able to check for them later and then replace properly
		if (string.contains("%p%"))
			string = string.replace("%p%", p.getName());
		if (string.contains("%hg%"))
			string = string.replace("%hg%", "" + Main.headsGoal);
		if (string.contains("%hr%"))
			string = string.replace("%hr%", "" + (Main.headsGoal - Main.onlinePlayerHeads.get(p.getUniqueId())));
		if (string.contains("%dr%"))
			string = string.replace("%dr%", "" + getEventRemainingDelay(p));
		if (string.contains("%sl%"))
			string = string.replace("%sl%", normalizeLocation(p.getWorld().getSpawnLocation()));
		string = ChatColor.translateAlternateColorCodes('&', string);
		return string;
	}

	private static String normalizeLocation(Location loc) {
		Helpers.p("normalizeLocation");
		return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
	}

	public static void createHolograms(Player p) {
		Helpers.p("createHolograms");
		Main.holos.remove(p);
		if (Main.holograms && Main.npcs != null && !Main.holos.containsKey(p))
			for (NPC npc : Main.npcs.keySet()) {
				Hologram holo = HologramsAPI.createHologram(Main.main(), npc.getEntity().getLocation().add(0.0, 2.6, 0.0));
				holo.getVisibilityManager().setVisibleByDefault(false);
				holo.getVisibilityManager().showTo(p);

				List<Hologram> holos = new ArrayList<>();
				if (Main.holos.containsKey(p))
					holos.addAll(Main.holos.get(p));
				holos.add(holo);

				Main.holos.put(p, holos);
				Main.npcs.put(npc, holos);

				updateHolograms(p);
			}
	}

	public static boolean updateHolograms(Player p) {
		Helpers.p("updateHolograms");
		if (Main.holograms)
			if (p.isOnline()) {
				if (!Main.holos.containsKey(p))
					createHolograms(p);

				List<Hologram> holos = new ArrayList<>();
				for (Hologram holo : Main.holos.get(p))
					if (holo.isDeleted())
						holos.add(holo);

					else {
						holo.clearLines();
						if (Main.onlinePlayerHeads.get(p.getUniqueId()) >= 0)
							for (String line : Main.holoRunning)
								holo.appendTextLine(formatPlaceholders(p, line));
						else if (!Main.holosReady.contains(p))
							for (String line : Main.holoWaiting)
								holo.appendTextLine(formatPlaceholders(p, line));
						else
							for (String line : Main.holoReady)
								holo.appendTextLine(formatPlaceholders(p, line));
					}

				Main.holos.get(p).removeAll(holos);
				return true;
			} else
				Helpers.removeHolograms(p);
		return false;
	}

	public static void removeHolograms(Player p) {
		Helpers.p("removeHolograms");
		if (Main.holograms && Main.holos.containsKey(p)) {

			Main.holos.get(p).forEach(holo -> {
				holo.delete();
			});

			Main.holos.remove(p);
		}
	}

	private static void setEntityMetaData(Entity e, String key, Object value) {
		Helpers.p("setEntityMetaData");
		e.setMetadata(key, new FixedMetadataValue(Main.main(), value));
	}

	static <T> T getRandomElement(List<T> list) {
		Helpers.p("getRandomElement");
		return list.get(new Random().nextInt(list.size()));
	}

	static void summonHead(Location location, String value) {
		Helpers.p("summonHead");
		if (location == null)
			return;
		Entity entity = location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
		entity.setCustomName("present");
		ArmorStand armorstand = (ArmorStand) entity;
		armorstand.setInvulnerable(true);
		armorstand.setVisible(false);
		armorstand.setGravity(false);
		armorstand.setHelmet(getCustomHead(value));
		setEntityMetaData(armorstand, "present", value);
		Main.presents.add(armorstand);
	}

	static ItemStack getCustomHead(String value) {
		Helpers.p("getCustomHead");
		final ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) skull.getItemMeta();
		assert meta != null;
		GameProfile profile = new GameProfile(UUID.randomUUID(), null);
		profile.getProperties().put("textures", new Property("textures", value));
		try {
			Field profileField = meta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(meta, profile);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
		skull.setItemMeta(meta);
		return skull;
	}

	static boolean isPresent(Entity e) {
		Helpers.p("isPresent");
		return e.getType().equals(EntityType.ARMOR_STAND) && e.hasMetadata("present") && Main.presents.contains(e)
				&& e.getCustomName().equals("present");
	}

	static boolean removePresent(Player p, Entity e, boolean find) {
		Helpers.p("removePresent");
		Main.presents.remove(e);
		Main.headsSpawned.put(p.getUniqueId(), Main.headsSpawned.get(p.getUniqueId()) - 1);
		Main.busyLocations.remove(e.getLocation());
		Main.availableLocations.add(e.getLocation());
		Helpers.removeHeadsFromInventory(p);
		e.remove();
		if (find && e.isDead()) {
			Main.onlinePlayerHeads.computeIfPresent(p.getUniqueId(), (u, i) -> i + 1);
			if (Main.headsGoal - Main.onlinePlayerHeads.get(p.getUniqueId()) > 0)
				Helpers.NotifyPlayer(p, Main.eventFindMessage);
			return true;
		}
		return false;
	}

	static void removeHeadsFromInventory(Player p) {
		Helpers.p("removeHeadsFromInventory");
		for (String present : Main.presentsSkinsValues)
			if (p.getInventory().contains(Helpers.getCustomHead(present)))
				p.getInventory().remove(Helpers.getCustomHead(present));
	}

	static int onceScheduler(Player p, List<String[]> list, @Nullable Location loc) {
		int[] delay = { 0 };

		for (String[] line : list) {
			final int line_selfdelay = Integer.parseInt(line[0]);
			final String line_command = line[1];
			final int line_delay = Integer.parseInt(line[2]);
			delay[0] += line_delay;

			final int line_index = list.indexOf(line);
			final int Delay = delay[0];

			new BukkitRunnable() {

				@Override
				public void run() {

					boolean exists;
					try {
						selfDelays.get(line_index);
						exists = true;
					} catch (IndexOutOfBoundsException ex) {
						exists = false;
					}

					if (!exists) {
						if (line_selfdelay - line_delay > 0)
							selfDelays.add(line_index, line_selfdelay);

						switch (line_command.split(" ")[0]) {
						case "particle":
							String[] particle = formatPlaceholders(p, line_command).split(" ");
							if (line_command.contains("%hl%")) {
								for (Location loc : Main.busyLocations) {
									// Bukkit.dispatchCommand(silentConsole(), replaceVars(p, line_command.replace("%hl%", normalizeLocs(loc))));
									// particle type %where% count offsetX offsetY offsetZ extra force
									particle[2] = loc.getX() + " " + (loc.getY() + 2) + " " + loc.getZ();
									spawnParticles(particle);
								}
								break;
							} else if (line_command.contains("%hf%")) {
								if (loc != null)
									particle[2] = loc.getX() + " " + (loc.getY() + 1.7) + " " + loc.getZ();
								else
									pr("%hf% can only be used in the find trigger", Logger.ERROR);
							} else if (line_command.contains("%pl%"))
								// Bukkit.dispatchCommand(silentConsole(), replaceVars(p, line_command));
								particle[2] = p.getLocation().getX() + " " + p.getLocation().getY() + " "
										+ p.getLocation().getZ();
							spawnParticles(particle); // the var replace handles the loc
							break;

						case "playsound":
							String[] playsound = formatPlaceholders(p, line_command).split(" ");
							if (line_command.contains("%hl%"))
								for (Location loc : Main.busyLocations) {
									// Bukkit.dispatchCommand(silentConsole(), replaceVars(p, line_command.replace("%hl%", normalizeLocs(loc))));
									// particle type %where% count offsetX offsetY offsetZ extra force
									playsound[2] = loc.getX() + " " + (loc.getY() + 2) + " " + loc.getZ();
									playSound(playsound);
								}
							else if (line_command.contains("%hf%")) {
								if (loc != null)
									playsound[2] = loc.getX() + " " + (loc.getY() + 2) + " " + loc.getZ();
								else
									pr("%hf% can only be used in the find trigger", Logger.ERROR);
							} else if (line_command.contains("%pl%"))
								// Bukkit.dispatchCommand(silentConsole(), replaceVars(p, line_command));
								playsound[2] = p.getLocation().getX() + " " + p.getLocation().getY() + " "
										+ p.getLocation().getZ();
							playSound(playsound); // the var replace handles the loc
							break;

						case "command":
							String command = formatPlaceholders(p, line_command);
							if (Main.executeTriggersAsCommands)
								command = command.replaceFirst("command ", "");
							if (line_command.contains("%hl%"))
								for (Location loc : Main.busyLocations)
									// Bukkit.dispatchCommand(silentConsole(), replaceVars(p, line_command.replace("%hl%", normalizeLocs(loc))));
									// particle type %where% count offsetX offsetY offsetZ extra force
									Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatPlaceholders(p, line_command
											.replace("%hl%", loc.getX() + " " + (loc.getY() + 2) + " " + loc.getZ())));
							else if (line_command.contains("%hf%")) {
								if (loc != null)
									command = line_command.replace("%hf%",
											loc.getX() + " " + (loc.getY() + 2) + " " + loc.getZ());
								else
									pr("%hf% can only be used in the find trigger", Logger.ERROR);
							} else if (line_command.contains("%pl%"))
								// Bukkit.dispatchCommand(silentConsole(), replaceVars(p, line_command));
								command = line_command.replace("%pl%",
										p.getLocation().getX() + " " + p.getLocation().getY() + " " + p.getLocation().getZ());
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command); // the var replace handles the loc
							break;

						default:
							if (Main.executeTriggersAsCommands)
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatPlaceholders(p, line_command));
							else {
								pr("There is an invalid executor in the config", Main.Logger.ERROR);
								break;
							}
						}

					} else {
						selfDelays.set(line_index, selfDelays.get(line_index) - delay[0]);
						if (selfDelays.get(line_index) <= line_delay)
							selfDelays.remove(line_index);
					}
				}
			}.runTaskLater(Main.main(), Delay);
		}
		return delay[0];
	}

	static void repeatScheduler(Player p, List<String[]> list) {
		int[] delay = { onceScheduler(p, list, null) };
		schedulerRepeats = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				if (!Main.busyLocations.isEmpty()) {
					onceScheduler(p, list, null);
					schedulerRepeats = true;
				} else {
					selfDelays.clear();
					cancel();
					schedulerRepeats = false;
				}
			}
		}.runTaskTimer(Main.main(), 0, delay[0]);
	}

	private static void spawnParticles(String[] line) {
		// 0particle 1type 2%where% 3count 4offsetX 5offsetY 6offsetZ 7extra 8force
		try {
			boolean force = false;
			double extra = 0;
			double offsetZ = 1;
			double offsetY = 1;
			double offsetX = 1;
			int count = Integer.parseInt(line[3]);

			World world = Main.main().getServer().getWorld(Main.hub);
			double x = Double.parseDouble(line[2].split(" ")[0]);
			double y = Double.parseDouble(line[2].split(" ")[1]);
			double z = Double.parseDouble(line[2].split(" ")[2]);
			Location loc = new Location(world, x, y, z);

			Particle type = Particle.valueOf(line[1].toUpperCase());

			switch (line.length) {
			case 9:
				force = Boolean.parseBoolean(line[8]);
			case 8:
				extra = Double.parseDouble(line[7]);
			case 7:
				offsetZ = Double.parseDouble(line[6]);
				offsetY = Double.parseDouble(line[5]);
				offsetX = Double.parseDouble(line[4]);
			}
			switch (line.length) {
			case 9:
				loc.getWorld().spawnParticle(type, loc, count, offsetX, offsetY, offsetZ, extra, null, force);
				break;
			case 8:
				loc.getWorld().spawnParticle(type, loc, count, offsetX, offsetY, offsetZ, extra);
				break;
			case 7:
				loc.getWorld().spawnParticle(type, loc, count, offsetX, offsetY, offsetZ);
				break;
			default:
				loc.getWorld().spawnParticle(type, loc, count);
				break;
			}
		} catch (IllegalArgumentException ex) {
			pr("Something is wrong with the particle arguments", Logger.ERROR);
			ex.printStackTrace();
		}
	}

	private static void playSound(String[] line) {
		// 0playsound 1sound 2%loc% 3volume 4pitch 5category
		try {
			String sound = line[1];
			World world = Main.main().getServer().getWorld(Main.hub);
			double x = Double.parseDouble(line[2].split(" ")[0]);
			double y = Double.parseDouble(line[2].split(" ")[1]);
			double z = Double.parseDouble(line[2].split(" ")[2]);
			Location loc = new Location(world, x, y, z);
			float volume = 1;
			float pitch = 0;
			try {
				volume = Float.parseFloat(line[3]);
				pitch = Float.parseFloat(line[4]);
			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Invalid number specified");
			} catch (NullPointerException ex) {} catch (IndexOutOfBoundsException ex) {}

			if (line.length == 6)
				loc.getWorld().playSound(loc, sound, SoundCategory.valueOf(line[5].toUpperCase()), volume, pitch);
			else
				loc.getWorld().playSound(loc, sound, volume, pitch);
		} catch (IllegalArgumentException ex) {
			pr("Something is wrong with the playsound arguments", Logger.ERROR);
			ex.printStackTrace();
		}
	}

	static Location findAvailableLocation(@Nullable Player p) {
		Helpers.p("findAvailableLocation");
		if (!Main.availableLocations.isEmpty()) {
			Location loc = getRandomElement(Main.availableLocations);
			Main.busyLocations.add(loc);
			Main.headsSpawned.put(p.getUniqueId(), Main.headsSpawned.getOrDefault(p.getUniqueId(), 0) + 1);
			Main.availableLocations.remove(loc);
			return loc;
		} else {
			// to limit the head spawn tries
			Main.headsUnableToSpawn++;
			if (p != null && Main.presents.size() <= 0)
				Helpers.NotifyPlayer(p, Main.errorMessage);
			return null;
		}
	}

	static String getEventRemainingDelay(Player p) {
		Helpers.p("getEventRemainingDelay");
		UUID uuid = p.getUniqueId();

		if (getRealTimeInMillis() - Main.onlinePlayerDelay.get(uuid) > TimeUnit.MINUTES.toMillis(Main.eventDelay))
			return "0:00";
		long pTime = Main.onlinePlayerDelay.get(uuid);
		long rTime = getRealTimeInMillis();

		long time = TimeUnit.MINUTES.toMillis(Main.eventDelay) - rTime + pTime;

		long hour = TimeUnit.MILLISECONDS.toHours(time);
		long mins = TimeUnit.MILLISECONDS.toMinutes(time - TimeUnit.HOURS.toMillis(hour));

		String h = "" + hour;
		String m = "" + mins;

		if (mins < 10)
			m = "0" + m;

		return h + ":" + m.toString();
	}

	static long getEventRemainingTimeout(Player p) {
		Helpers.p("getEventTimeout");
		UUID uuid = p.getUniqueId();

		long pTime = Main.onlinePlayerTimeout.get(uuid);
		long rTime = getRealTimeInMillis();
		long time = TimeUnit.SECONDS.toMillis(Main.eventTimeout) - rTime + pTime;

		if (getRealTimeInMillis() - Main.onlinePlayerTimeout.get(uuid) > TimeUnit.SECONDS.toMillis(Main.eventTimeout))
			return 0;
		return TimeUnit.MILLISECONDS.toSeconds(time);
	}

	static void startEventTimeout(Player p) {
		Main.onlinePlayerTimeout.put(p.getUniqueId(), getRealTimeInMillis());
		Helpers.NotifyPlayer(p, Main.eventStartMessage);

		int[] delay = { 0 };
		if (Main.eventTimeout > 60)
			delay[0] = 60;
		else if (Main.eventTimeout > 30)
			delay[0] = 30;
		else
			delay[0] = 10;

		new BukkitRunnable() {

			@Override
			public void run() { // p != null maybe the player is null when they leave? idk
				if (p == null || !updateHolograms(p)) {
					removeHolograms(p);
					cancel();
				}
			}
		}.runTaskTimer(Main.main(), 0L, Main.holoRunningUpdate);

		new BukkitRunnable() {

			@Override
			public void run() {
				if (Main.onlinePlayerTimeout.getOrDefault(p.getUniqueId(), -1L) > -1L) {
					if (p.isOnline())
						p.sendMessage(ChatColor.GOLD + "HubHeads" + ChatColor.AQUA + "> " + ChatColor.RED + "You have " + delay[0]
								+ " seconds left!");
					new BukkitRunnable() {
						@Override
						public void run() {
							if (p.isOnline() && Main.onlinePlayerTimeout.getOrDefault(p.getUniqueId(), -1L) > -1L)
								Main.StartEvent(p, false);
						}
					}.runTaskLater(Main.main(), delay[0] * 20);
				}
			}
		}.runTaskLater(Main.main(), (Main.eventTimeout - delay[0]) * 20);
	}

	static boolean checkPlayerData(Player p, boolean heads, boolean delay) {
		Helpers.p("checkPlayerData");
		try {
			boolean checkHeads = heads == Main.onlinePlayerHeads.get(p.getUniqueId()) >= 0;
			boolean checkDelay = delay != getEventRemainingDelay(p).equals("0:00");
			return checkHeads && checkDelay;
		} catch (NullPointerException ex) {
			// when the player joins and presses cancel he leaves before the data is fetched from the table
			// the online tables do not have the player values
			return false;
		}
	}

	static long getRealTimeInMillis() {
		Helpers.p("getRealTimeInMillis");
		return Calendar.getInstance().getTimeInMillis();
	}

	static void scanWorldBlocks(CommandSender s) {
		Helpers.p("scanWorldBlocks");
		if (s instanceof Player)
			s.sendMessage(ChatColor.GREEN + "Scanning the world...");
		else
			pr(ChatColor.GREEN + "Scanning the world...", Logger.INFO);
		int x0 = Bukkit.getWorld(Main.hub).getSpawnLocation().getBlockX();
		int y0 = Bukkit.getWorld(Main.hub).getSpawnLocation().getBlockY();
		int z0 = Bukkit.getWorld(Main.hub).getSpawnLocation().getBlockZ();

		int x1 = x0 - Main.radiusX / 2;
		int y1 = y0 - Main.radiusY / 2;
		int z1 = z0 - Main.radiusZ / 2;

		int x2 = x0 + Main.radiusX / 2;
		int y2 = y0 + Main.radiusY / 2;
		int z2 = z0 + Main.radiusZ / 2;

		for (int x = x1; x <= x2; ++x)
			for (int y = y1; y <= y2; ++y)
				for (int z = z1; z <= z2; ++z)
					if (Main.validHeadsBlocks.contains(Bukkit.getWorld(Main.hub).getBlockAt(x, y, z).getType().toString()))
						if (Bukkit.getWorld(Main.hub).getBlockAt(x, y + 1, z).getType().equals(Material.AIR))
							if (!Main.busyLocations.contains(new Location(Bukkit.getWorld(Main.hub), x + 0.5, y - 0.5, z + 0.5)))
								Main.availableLocations.add(new Location(Bukkit.getWorld(Main.hub), x + 0.5, y - 0.5, z + 0.5));
		if (s instanceof Player) {
			s.sendMessage(ChatColor.GREEN + "World scan complete!");
			if (Main.availableLocations.size() < Main.maxHeadsAtOnce)
				if (Main.availableLocations.isEmpty())
					s.sendMessage(ChatColor.RED + "There is no space to spawn a head! Please check your configuration, then run "
							+ ChatColor.DARK_RED + "/hh reload" + ChatColor.RED + " and " + ChatColor.DARK_RED + "/hh scan");
				else
					s.sendMessage(ChatColor.YELLOW + "There is not enough space to spawn the max specified amount of heads");
		} else {
			pr(ChatColor.GREEN + "World scan complete!", Logger.INFO);
			if (Main.availableLocations.size() < Main.maxHeadsAtOnce)
				if (Main.availableLocations.isEmpty())
					pr(ChatColor.RED
							+ "There is no space to spawn a head! Please check your configuration or world and run /hh scan",
							Logger.ERROR);
				else
					pr(ChatColor.RED + "There is not enough space to spawn the max specified amount of heads", Logger.ERROR);
		}
	}

	static void p(Object o) {
		if (Main.debug)
			System.out.println("[HH Debug] " + o);
	}

	static void pr(Object o, Logger log) {
		switch (log) {
		case INFO:
			Bukkit.getLogger().info(ChatColor.getLastColors(o.toString()) + "[HubHeads] " + ChatColor.RESET + o);
			break;
		case WARN:
			Bukkit.getLogger().warning("[HubHeads] " + o);
			break;
		case ERROR:
			Bukkit.getLogger().severe("[HubHeads] " + o);
			break;
		}
	}

}
