package me.atomiz.gtab;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import me.atomiz.gtab.NMS.PoliceType;
import me.atomiz.gtab.Settings.Settings;
import me.atomiz.gtab.Utils.CrimeListener;
import me.atomiz.gtab.Utils.CrimeManager;
import me.atomiz.gtab.Utils.MetaDataManager;
import me.atomiz.gtab.Utils.StaticMetaDataValue;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;

public class Main extends JavaPlugin implements Listener {

	private ConsoleCommandSender console = getServer().getConsoleSender();
	private FileConfiguration config;
	public static Main main;
	public static List<UUID> policeUUIDs = new ArrayList<UUID>(); // will store the policemen UUIDs
	public static Scoreboard board;
	public static ScoreboardManager manager;
	public static boolean debug = false;

	private HoverEvent.Action show = HoverEvent.Action.SHOW_TEXT;
	private ClickEvent.Action suggest = ClickEvent.Action.SUGGEST_COMMAND;
	private ClickEvent.Action run = ClickEvent.Action.RUN_COMMAND;

	@Override
	public void onEnable() {

		main = this;

		Settings.loadConfig();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new Events(), this);
		pm.registerEvents(new CrimeListener(), this);
		getCommand("gtab").setExecutor(this);

		saveDefaultConfig();
		config = getConfig();
		config.options().copyDefaults();
		saveConfig();

		manager = Bukkit.getScoreboardManager();
		board = manager.getNewScoreboard();
		Helpers.updateScoreboard();

		PoliceType.registerEntities();

		console.sendMessage(ChatColor.GREEN + "Enabled " + getDescription().getName());
	}

	@Override
	public void onDisable() {
		PoliceType.unregisterEntities();

		console.sendMessage(ChatColor.RED + "Disabled " + getDescription().getName());
	}

	// handles the commands
	@Override
	public boolean onCommand(CommandSender s, Command command, String label, String[] args) {

		if (args.length == 0) {
			help(s, 3);
			return false;
		}

		switch (args[0].toLowerCase()) {

		case "d":
		case "debug":
			if (s.hasPermission("gtab.use"))
				debug ^= true;
			if (debug)
				s.sendMessage(ChatColor.GREEN + "Enabled debugging mode");
			else
				s.sendMessage(ChatColor.RED + "Disabled debugging mode");
			break;

		case "h":
		case "help":
			help(s, 3);
			break;

		case "k":
		case "ka":
		case "killall":
			killall(s, args);
			break;

		case "s":
		case "l":
		case "sl":
		case "lvl":
		case "swl":
		case "level":
		case "setlevel":
		case "setwantedlevel":
			setlevel(s, args);
			break;

		case "us":
			if (debug)
				if (s.hasPermission("gtab.use"))
					Helpers.updateScoreboard();
			break;

		case "rg":
			if (debug)
				if (s.hasPermission("gtab.use"))
					for (Entity e : ((Player) s).getWorld().getEntities()) {
						e.setCustomNameVisible(true);
						e.setCustomName("" + policeUUIDs.contains(e.getUniqueId()));
					}
			break;

		case "r":
		case "unp":
		case "unpg":
		case "unt":
		case "untg":
		case "urg":
		case "unrg":
		case "reset":
			if (debug)
				if (s.hasPermission("gtab.use"))
					for (Entity e : ((Player) s).getWorld().getEntities())
						if (policeUUIDs.contains(e.getUniqueId())) {
							e.setCustomNameVisible(Settings.isMobNameVisible());
							e.setCustomName(Settings.setNamesToTargets()
									? getServer().getPlayer(UUID.fromString(MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target))).getName()
									: PoliceType.getPoliceName(e));
						}
			break;

		case "tg":
			if (debug)
				if (s.hasPermission("gtab.use"))
					for (Entity e : ((Player) s).getWorld().getEntities())
						if (policeUUIDs.contains(e.getUniqueId())) {
							e.setCustomNameVisible(true);
							Helpers.pr(MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target));
							try {
								e.setCustomName(getServer().getPlayer(MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target)).getName());
							} catch (NullPointerException ex) {
								switch (e.getType()) {
								case ZOMBIE:
									e.setCustomName(Settings.getOfficerName());
									break;
								case SKELETON:
									e.setCustomName(Settings.getSniperName());
									break;
								case PIG_ZOMBIE:
									e.setCustomName(Settings.getSwatName());
									break;
								default:
									e.setCustomName("Report me to Atom.");
									break;
								}
							}
						}
			break;

		case "pt":
			if (debug)
				if (s.hasPermission("gtab.use")) {
					s.sendMessage(ChatColor.GOLD + "Targeters:");
					for (Entity e : ((Player) s).getWorld().getEntities())
						if (policeUUIDs.contains(e.getUniqueId())) {
							e.setCustomNameVisible(true);
							e.setCustomName(ChatColor.BOLD + e.getUniqueId().toString());
							if (MetaDataManager.getPlayersMetaList((Player) s, StaticMetaDataValue.Target).contains(e.getUniqueId().toString()))
								s.sendMessage(ChatColor.YELLOW + e.getUniqueId().toString()); // contains
							else
								s.sendMessage(ChatColor.GOLD + e.getUniqueId().toString()); // doesnt
						}
				}
			break;

		case "pm":
			if (debug)
				pm(s, args);
			break;

		default:
			if (s instanceof Player)
				Helpers.Editor((Player) s, ChatColor.RED + "Invalid command.", show, ChatColor.GREEN + "/gtab help", run, "/gtab help");
			else
				s.sendMessage(ChatColor.RED + "Invalid command.");
		}
		return true;
	}

	// handles the tab completetion while typing a command
	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {

		// /cmd args[0] args[1] args[2]
		ArrayList<String> list = new ArrayList<String>();
		ArrayList<String> commands = new ArrayList<String>();

		commands.add("killall");
		commands.add("setlevel");

		if (debug) {
			commands.add("us");
			commands.add("rg");
			commands.add("reset");
			commands.add("tg");
			commands.add("pt");
			commands.add("pm");
		}

		if (args[0].equals(""))
			return commands;

		if (args.length == 1) {
			commands.forEach(string -> {
				if (string.toLowerCase().startsWith(args[0].toLowerCase()))
					list.add(string);
			});
			return list;
		}

		switch (args[0]) {

		case "l":
		case "level":
		case "s":
		case "sl":
		case "setlevel": // /gtab _0_killall _1_level _2_player
			if (args.length == 2)
				for (int i = 0; i < 5 + 1; i++)
					list.add(i, "" + i);
			if (args.length == 3)
				return null;
			return list;

		case "k":
		case "ka":
		case "killall":
			if (args.length == 2)
				return null;
			else
				list.clear();
			return list;

		case "us":
			if (debug) {
				list.clear();
				return list;
			} else
				return null;

		case "rg":
			if (debug) {
				list.clear();
				return list;
			} else
				return null;

		case "reset":
			if (debug) {
				list.clear();
				return list;
			} else
				return null;

		case "tg":
			if (debug) {
				list.clear();
				return list;
			} else
				return null;

		case "pt":
			if (debug) {
				list.clear();
				return list;
			} else
				return null;

		case "pm": // /gtab _0_pm _1_key _2_(player)
			if (debug) {
				commands.clear();
				commands.add(StaticMetaDataValue.Target);
				commands.add(StaticMetaDataValue.Police);
				commands.add(StaticMetaDataValue.Level);
				commands.add(StaticMetaDataValue.Crimes);
				if (args[1].equals(""))
					return commands;

				if (args.length == 2) {
					commands.forEach(string -> {
						if (string.toLowerCase().startsWith(args[1].toLowerCase()))
							list.add(string);
					});
					return list;
				}
				if (args.length == 3)
					return null;
			} else
				return null;
		}
		return null;
	}

	// /gtab _0_help
	// shows the commands help menu
	private void help(CommandSender s, int cmd) {
		s.sendMessage(ChatColor.GOLD + "--- GTAbounty Help ---");
		if (s instanceof Player) {
			if (cmd == 1 || cmd == 3)
				Helpers.Editor((Player) s, ChatColor.AQUA + "- " + ChatColor.DARK_AQUA + "killall" + ChatColor.YELLOW + " - Kill the whole Police", show, "/gtab ka",
						suggest, "/gtab ka");
			if (cmd == 2 || cmd == 3)
				Helpers.Editor((Player) s, ChatColor.AQUA + "- " + ChatColor.DARK_AQUA + "setlevel" + ChatColor.YELLOW + " - Sets the player wanted level", show,
						"/gtab sl (0-5) [player]", suggest, "/gtab sl ");
		} else {
			if (cmd == 1 || cmd == 3)
				s.sendMessage(ChatColor.AQUA + "- " + ChatColor.DARK_AQUA + "killall");
			if (cmd == 2 || cmd == 3)
				s.sendMessage(ChatColor.AQUA + "- " + ChatColor.DARK_AQUA + "setlevel");
		}
	}

	// /gtab _0_killall (_1_player)
	// kills the all policemen
	private void killall(CommandSender s, String[] args) {

		switch (args.length) {
		case 1:
			s.getServer().getWorlds().forEach(world -> {
				for (Player p : world.getPlayers())
					CrimeManager.getInstance().resetPlayer(p);
			});

			for (UUID uuid : policeUUIDs) {
				getServer().getEntity(uuid).remove();
				policeUUIDs.remove(uuid);
			}

			s.sendMessage(ChatColor.GREEN + "Killed all police members");
			break;
		case 2:
			try {
				Player p = getServer().getPlayer(args[1]);
				MetaDataManager.getPlayersMetaList(p, StaticMetaDataValue.Target).forEach(meta -> {
					getServer().getEntity(UUID.fromString(meta)).remove();
					MetaDataManager.removePlayerPolice(p, PoliceType.getTypeFromEntity(getServer().getEntity(UUID.fromString(meta))));
					MetaDataManager.removePlayerTarget(p, meta);
					policeUUIDs.remove(UUID.fromString(meta));
				});
				s.sendMessage(ChatColor.GREEN + "Killed all police members");
			} catch (NullPointerException ex) {
				s.sendMessage(ChatColor.RED + "Invalid player");
			}
			break;
		default:
			if (s instanceof Player)
				Helpers.Editor((Player) s, ChatColor.RED + "Invalid arguments", show, ChatColor.GREEN + "/gtab killall (player)", suggest, "/gtab killall ");
			else
				s.sendMessage(ChatColor.RED + "Invalid arguments");
			break;
		}
	}

	// /gtab _0_setlevel _1_level _2_player
	// the highter wanted level you have the more policemen will follow you
	private void setlevel(CommandSender s, String[] args) {

		if (args.length == 1) {
			help(s, 2);
			return;
		}

		if (!(args.length == 2 || args.length == 3)) {
			if (s instanceof Player)
				Helpers.Editor((Player) s, ChatColor.RED + "Invalid arguments.", show, ChatColor.GREEN + "/gtab setlevel (level) [player]", suggest, "/gtab sl ");
			else
				s.sendMessage(ChatColor.RED + "Invalid arguments. " + ChatColor.GREEN + "/gtab setlevel (level) (player)");
			return;
		}

		// if _1_level is a number
		if (!Helpers.isStringInt(args[1])) {
			s.sendMessage(ChatColor.DARK_RED + args[1] + ChatColor.RED + " is not a number.");
			return;
		}

		// args[1] is a number. now set int level equal to it
		int level = Integer.parseInt(args[1]);

		// check is level is between 0 and 5
		if (level < 0 || level > 5) {
			s.sendMessage(ChatColor.RED + "Value out of range.");
			return;
		}

		// if s is a player
		if (s instanceof Player) {
			// if the command has no player
			if (args.length == 2) {
				CrimeManager.getInstance().setWantedLevel((Player) s, level);
				s.sendMessage(ChatColor.GREEN + "Your level was set to " + ChatColor.DARK_GREEN + args[1]);
				// if the command has a valid player
			} else if (getServer().getPlayer(args[2]) != null) {
				CrimeManager.getInstance().setWantedLevel(getServer().getPlayer(args[2]), level);
				s.sendMessage(ChatColor.DARK_GREEN + Helpers.S(args[2]) + ChatColor.GREEN + " level was set to " + ChatColor.DARK_GREEN + args[1]);
				// if the command doesnt have a valid player
			} else {
				s.sendMessage(ChatColor.DARK_RED + args[2] + ChatColor.RED + " is not online.");
				return;
			}
		} else if (args.length == 2) { // if the sender is not a player and has no player specified
			s.sendMessage(ChatColor.RED + "Invalid arguments. " + ChatColor.GREEN + "/gtab setlevel (level) (player)");
			return;
		} else if (getServer().getPlayer(args[2]) != null) { // if the sender is not a player and has a valid player specified
			CrimeManager.getInstance().setWantedLevel(getServer().getPlayer(args[2]), level);
			s.sendMessage(ChatColor.DARK_GREEN + Helpers.S(args[2]) + ChatColor.GREEN + " level was set to " + ChatColor.DARK_GREEN + args[1]);
		} else { // if the sender is not a player and doesnt have a valid player specified
			s.sendMessage(ChatColor.DARK_RED + args[2] + ChatColor.RED + " is not online.");
			return;
		}

		Helpers.updateScoreboard();
	}

	// /gtab _0_pm _1_key _2_player
	// shows the player's meta
	public void pm(CommandSender s, String[] args) {

		if (args.length > 1) {
			Player p;
			String check = args[1].contains(StaticMetaDataValue.Police) || args[1].contains(StaticMetaDataValue.Target) ? "0" : "";
			if (s.hasPermission("gtab.use"))
				switch (args.length) {
				case 2:
					if (s instanceof Player) {
						p = (Player) s;
						if (p.hasMetadata(args[1].toLowerCase() + check)) {
							if (check == "0") {
								s.sendMessage(ChatColor.GOLD + "Your " + args[1] + " metadata:");
								for (String element : MetaDataManager.getPlayersMetaList(p, args[1]))
									s.sendMessage(ChatColor.YELLOW + element);
							} else
								s.sendMessage(ChatColor.GOLD + "Your " + args[1] + " metadata: " + MetaDataManager.getPlayerMetaData(p, args[1]));
						} else
							s.sendMessage(ChatColor.RED + "You have no " + ChatColor.DARK_RED + args[1] + ChatColor.RED + " metadata");
					} else
						s.sendMessage(ChatColor.RED + "Invalid arguments");
					break;
				case 3:
					if (getServer().getPlayer(args[2]) != null) {
						p = getServer().getPlayer(args[2]);
						if (p.hasMetadata(args[1].toLowerCase() + check)) {
							if (check == "0") {
								s.sendMessage(ChatColor.GOLD + Helpers.S(args[2]) + " metadata:");
								for (String element : MetaDataManager.getPlayersMetaList(p, args[1]))
									s.sendMessage(ChatColor.YELLOW + element);
							} else
								s.sendMessage(ChatColor.GOLD + Helpers.S(args[2]) + " metadata: " + MetaDataManager.getPlayerMetaData(p, args[1]));
						} else
							s.sendMessage(ChatColor.RED + Helpers.S(args[2]) + " has no " + ChatColor.DARK_RED + args[1] + ChatColor.RED + " metadata");
					} else
						s.sendMessage(ChatColor.DARK_RED + args[2] + ChatColor.RED + " is not online");
					break;
				default:
					if (s instanceof Player)
						Helpers.Editor((Player) s, ChatColor.RED + "Invalid arguments", show, ChatColor.GREEN + "/gtab pm <key> (player)", suggest, "/gtab pm ");
					else
						s.sendMessage(ChatColor.RED + "Invalid arguments");
					break;
				}
		} else if (s instanceof Player)
			Helpers.Editor((Player) s, ChatColor.RED + "Invalid arguments", show, ChatColor.GREEN + "/gtab pm <key> (player)", suggest, "/gtab pm ");
		else
			s.sendMessage(ChatColor.RED + "Invalid arguments");

		/*		
		  		if (s.hasPermission("gtab.use"))
					if (args.length > 1)
						try {
							if (getServer().getPlayer(args[1]).isOnline()) {
								s.sendMessage(ChatColor.GOLD + args[1] + Helpers.S(args[1]) + " meta:");
								int i = 0;
								while (getServer().getPlayer(args[1]).hasMetadata(StaticMetaDataValue.Target + i)) {
									s.sendMessage(
											ChatColor.YELLOW + MetaDataManager.getPlayerMetaData(getServer().getPlayer(args[1]), StaticMetaDataValue.Target + i).toString());
									getServer().getEntity(UUID.fromString(MetaDataManager.getPlayerMetaData((Player) s, StaticMetaDataValue.Target + i)))
											.setCustomName(MetaDataManager.getPlayerMetaData((Player) s, StaticMetaDataValue.Target + i));
									i++;
								}
							} else
								s.sendMessage(ChatColor.DARK_RED + args[1] + ChatColor.RED + " is'nt online.");
						} catch (NullPointerException ex) {
							s.sendMessage(ChatColor.RED + "Please specify a valid player name.");
						}
					else if (s instanceof Player) {
						String name = s.getName();
						s.sendMessage(ChatColor.GOLD + name + Helpers.S(name) + " meta:");
						Helpers.pr(ChatColor.GOLD + name + Helpers.S(name) + " meta:");
						int i = 0;
						while (((Player) s).hasMetadata(StaticMetaDataValue.Target + i)) {
							Helpers.pr(ChatColor.YELLOW + MetaDataManager.getPlayerMetaData((Player) s, StaticMetaDataValue.Target + i).toString());
							s.sendMessage(ChatColor.YELLOW + MetaDataManager.getPlayerMetaData((Player) s, StaticMetaDataValue.Target + i).toString());
							getServer().getEntity(UUID.fromString(MetaDataManager.getPlayerMetaData((Player) s, StaticMetaDataValue.Target + i)))
									.setCustomName(MetaDataManager.getPlayerMetaData((Player) s, StaticMetaDataValue.Target + i));
							i++;
						}
					} else
						s.sendMessage(ChatColor.RED + "Please specify a valid player name.");
						*/
	}
}
