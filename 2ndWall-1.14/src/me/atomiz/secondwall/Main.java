package me.atomiz.secondwall;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

public class Main extends JavaPlugin implements Listener {

	ConsoleCommandSender console = getServer().getConsoleSender();
	FileConfiguration config;

	static net.md_5.bungee.api.chat.ClickEvent.Action suggest = net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND;
	static net.md_5.bungee.api.chat.HoverEvent.Action show = net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;
	static ChatColor red = ChatColor.RED;
	static ChatColor darkR = ChatColor.DARK_RED;
	static ChatColor green = ChatColor.GREEN;
	static ChatColor darkG = ChatColor.DARK_GREEN;
	String noPerm = red + "You have no permission to execute this command.";
	String invalidArgs = red + "Invalid arguments.";

	List<String> ranksAll = new ArrayList<String>();

	@Override
	public void onEnable() {

//		Sets the config to what it should be
		saveDefaultConfig();
		config = getConfig();
		config.options().copyDefaults();
		saveConfig();

//		Assign all ranks. values to ranksAll

		for (String section : config.getConfigurationSection("ranks").getKeys(false)) {
			for (String rank : config.getStringList("ranks." + section)) {
				ranksAll.add(rank);
			}
		}

//		check in config whether to create the ranks from the config or not
		if (config.getBoolean("CreateMissingRanks")) {
			String command = config.getString("CreateMissingRanksCommand");
			for (String section : config.getConfigurationSection("ranks").getKeys(false)) {
				for (String rank : config.getStringList("ranks." + section)) {
					cmd(command.replace("%group%", rank));
				}
			}
		}

//		- assign online and in per player variables
		for (Player p : getServer().getOnlinePlayers()) {
			if (isRegistered(p.getName())) {
				config.set("staff." + p.getName() + ".online", true);
			}
		}

//		register events
		Bukkit.getPluginManager().registerEvents(this, this);
//		register commands
		getCommand("staffmode").setExecutor(this);

//		send Enabled in console
		console.sendMessage(ChatColor.GREEN + "Enabled " + getDescription().getName());
		saveConfig();
	}

	@Override
//	onDisable { send Disabled in console }
	public void onDisable() {

		for (String p : config.getConfigurationSection("staff").getKeys(false)) {
			config.set("staff." + p + ".online", false);
		}

		console.sendMessage(ChatColor.RED + "Disabled " + getDescription().getName());

	}

//	playerHasGroups { checks if the player has any of the groups }
	private static boolean playerHasGroups(Player p, List<String> groups) {
		for (String string : groups) {
			if (p.hasPermission("group." + string)) {
				return true;
			}
		}
		return false;
	}

//	getPlayerRanks { returns all the ranks the player has from the config }	
	private static List<String> getPlayerRanks(Player p, List<String> groups) {
		List<String> ranks = new ArrayList<String>();
		for (String string : groups) {
			if (p.hasPermission("group." + string)) {
				ranks.add(string);
			}
		}
		return ranks;
	}

//	addPlayerRanks { give the player the ranks from their list in the config }
	private void addPlayerConfigRanks(Player p) {
		String command = config.getString("AddRanks");
		for (String rank : config.getStringList("staff." + p.getName() + ".ranks")) {
			cmd(command.replace("%player%", p.getName()).replace("%group%", rank));
		}
	}

	private boolean isRegistered(String p) {
		if (config.getConfigurationSection("staff").getKeys(false).contains(p)) {
			return true;
		}
		return false;
	}

	private void cmd(String command) {
		Bukkit.dispatchCommand(console, command);
	}

	@EventHandler
	private void onJoin(PlayerJoinEvent event) {

		Player p = event.getPlayer();
		String command = config.getString("OnJoinCommand").replace("%player%", p.getName());

//		Deop player
		p.setOp(false);

//		if (player has staff ranks) { set player parent to default } # should i remove them if they arent in the staff. config
		if (playerHasGroups(p, config.getStringList("ranks.staff"))) {
			if (!isRegistered(p.getName())) {
				config.set("unregistered.users." + p.getName() + ".UUID", p.getUniqueId().toString());
				config.set("unregistered.users." + p.getName() + ".ranks", getPlayerRanks(p, ranksAll));

				if (!command.contains("%group%")) {
					cmd(command);
				} else {
					for (String rank : config.getStringList("ranks.staff")) {
						if (p.hasPermission("group." + rank)) {
							cmd(command.replace("%group%", rank));
						}
					}
				}
			}
		}

//		if (player is in the staff list in config) { set player parent to default }
		if (isRegistered(p.getName())) {
			config.set("staff." + p.getName() + ".online", true);
			config.set("staff." + p.getName() + ".logged", false);
			cmd(command);
		}
		saveConfig();
	}

	@EventHandler
	private void onQuit(PlayerQuitEvent event) {

		Player p = event.getPlayer();
		String command = config.getString("OnLeaveCommand").replace("%player%", p.getName());

//		Deop player		
		p.setOp(false);

//		if (staff. contains player) { set player to default; set .on = false; set .in = false }
		if (isRegistered(p.getName())) {
			config.set("staff." + p.getName() + ".online", false);
			config.set("staff." + p.getName() + ".logged", false);
			cmd(command);
		}
		if (isRegistered(p.getName())) {
			cmd(command);
		}
		saveConfig();
	}

//	+---------------------------------------------------+----+---------+-------+
//	| Commands                                          | OP | Console | Staff |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_get <_1_name>                              |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_unregister _1_name                         |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_reload (config)                            |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_add_ _1_name_ _2_ranks_                    |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_create _1_section _2_ranks..               |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_remove _1_name _2_ranks...                 |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_delete _1_ranks...                         |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_register _1_name _2_password <_3_ranks...> |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_password _1_player _2_new                  |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_set _1_player _2_password                  |  1 |       1 |     0 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_help                                       |  1 |       1 |     1 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_login _1_password                          |  1 |       0 |     1 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_get                                        |  1 |       0 |     1 |
//	+---------------------------------------------------+----+---------+-------+
//	| /sm _0_password _1_old _2_new                     |  0 |       0 |     1 |
//	+---------------------------------------------------+----+---------+-------+

	@Override
	public boolean onCommand(CommandSender s, Command command, String label, String[] args) {

		String ar = "";

		for (String arg : args) {
			ar = ar + " " + arg;
		}

		if (!(s instanceof Player)) {
			Log("(C) CONSOLE: /sm" + ar);
		} else {
			Player p = (Player) s;
			if (p.isOp()) {
				Log("(O) " + p.getName() + ": /sm" + ar);
			} else {
				if (isRegistered(p.getName())) {
					Log("(R) " + p.getName() + ": /sm" + ar);
				} else {
					Log("(U) " + p.getName() + ": /sm" + ar);
				}
			}
		}

		if (s instanceof Player && !isRegistered(s.getName())) {
			s.sendMessage(red + "You have no access to this plugin.");
			return false;
		}

		switch (args[0]) {

		case "get":

			get(s, command, label, args);
			break;

		case "help":

			help(s, command, label, args);
			break;

		case "reload":

			reload(s, command, label, args);
			break;

		case "add":

			add(s, command, label, args);
			break;

		case "create":

			create(s, command, label, args);
			break;

		case "delete":

			delete(s, command, label, args);
			break;

		case "remove":

			remove(s, command, label, args);
			break;

		case "register":

			register(s, command, label, args);
			break;

		case "unregister":

			unregister(s, command, label, args);
			break;

		case "login":

			login(s, command, label, args);
			break;

		case "password":

			password(s, command, label, args);
			break;

		case "set":

			set(s, command, label, args);
			break;

		default:
			if (s instanceof Player) {
				Editor((Player) s, red + "Invalid command.", show, "Click for help.", net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/sm help");
			} else {
				s.sendMessage(red + "Invalid command.");
			}
			return false;
		}
		return false;
	}

	// /sm _0_get <_1_name>
	private void get(CommandSender s, Command command, String label, String[] args) {

		if (!(s instanceof Player)) {

			if (args.length == 2) {
				if (isRegistered(args[1])) {
					if (config.getStringList("staff." + args[1] + ".ranks").size() > 0) {
						if (args[1].endsWith("s")) {
							s.sendMessage(ChatColor.AQUA + args[1] + "' ranks: "
									+ color(ChatColor.GOLD, config.getStringList("staff." + args[1] + ".ranks").toString().replace("[", "").replace("]", "")));
						} else {
							s.sendMessage(ChatColor.AQUA + args[1] + "'s ranks: "
									+ color(ChatColor.GOLD, config.getStringList("staff." + args[1] + ".ranks").toString().replace("[", "").replace("]", "")));
						}
					} else {
						s.sendMessage(red + "This player has no ranks.");
					}
				} else {
					s.sendMessage(red + "This player is not registered.");
				}
			} else if (args.length == 1) {
				s.sendMessage(red + "You have to specify a player.");
			} else {
				s.sendMessage(invalidArgs);
			}

		} else {
			Player p = (Player) s;
			if (p.isOp()) {

				if (args.length == 2) {
					if (isRegistered(args[1])) {
						if (config.getStringList("staff." + args[1] + ".ranks").size() > 0) {
							if (args[1].endsWith("s")) {
								s.sendMessage(ChatColor.AQUA + args[1] + "' ranks: "
										+ color(ChatColor.GOLD, config.getStringList("staff." + args[1] + ".ranks").toString().replace("[", "").replace("]", "")));
							} else {
								s.sendMessage(ChatColor.AQUA + args[1] + "'s ranks: "
										+ color(ChatColor.GOLD, config.getStringList("staff." + args[1] + ".ranks").toString().replace("[", "").replace("]", "")));
							}
						} else {
							s.sendMessage(red + "This player has no ranks.");
						}
					} else {
						s.sendMessage(red + "This player is not registered.");
					}
				} else if (args.length == 1) {
					if (isRegistered(p.getName())) {
						if (config.getStringList("staff." + p.getName() + ".ranks").size() > 0) {
							p.sendMessage(ChatColor.AQUA + "Your ranks: "
									+ color(ChatColor.GOLD, config.getStringList("staff." + p.getName() + ".ranks").toString().replace("[", "").replace("]", "")));
						} else {
							Editor(p, red + "You have no ranks.", show, "Add ranks?", suggest, "/sm add " + p.getName());
						}
					} else {
						Editor(p, red + "You aren't registered.", show, "Click to register.", suggest, "/sm register " + p.getName() + " password");
					}
				} else {
					s.sendMessage(red + "Invalid arguments.");
				}

			} else {

				Log(p.getName() + ": " + args);

				if (isRegistered(p.getName())) {
					if (args.length == 1) {
						if (config.getStringList("staff." + p.getName() + ".ranks").size() > 0) {
							p.sendMessage(ChatColor.AQUA + "Your ranks: "
									+ color(ChatColor.GOLD, config.getStringList("staff." + p.getName() + ".ranks").toString().replace("[", "").replace("]", "")));
						} else {
							p.sendMessage(red + "You have no ranks.");
						}
					} else {
						Editor(p, invalidArgs, show, "/sm get", suggest, "/sm get");
					}
				} else {
					p.sendMessage(red + "You aren't registered.");
				}

			}
		}
	}

	// /sm _0_help
	private void help(CommandSender s, Command command, String label, String[] args) {

		Help(s);
	}

	// /sm _0_reload _1_[config]
	private void reload(CommandSender s, Command command, String label, String[] args) {

		if (!(s instanceof Player)) {

			if (args.length == 1) {
				reloadConfig();
				s.sendMessage(green + "Reloaded config.");
				return;
			}

			if (args.length == 2) {
				if (args[1].equals("config")) {
					reloadConfig();
					s.sendMessage(green + "Reloaded config.");
					return;
				}
			}

			s.sendMessage(red + "Invalid arguments.");
			s.sendMessage(ChatColor.GOLD + "Usage: /sm reload");

		} else {
			Player p = (Player) s;
			if (p.isOp()) {

				if (args.length == 1) {
					reloadConfig();
					p.sendMessage(green + "Reloaded configuraion.");
					return;
				}

				if (args.length == 2) {
					if (args[1].equals("config")) {
						reloadConfig();
						s.sendMessage(green + "Reloaded config.");
						return;
					}
				}

				Editor(p, invalidArgs, show, "/sm reload", suggest, "/sm reload");
				return;

			}

			p.sendMessage(noPerm);
		}
	}

	// /sm _0_add _1_name _2_ranks
	private void add(CommandSender s, Command command, String label, String[] args) {

		List<String> added = new ArrayList<String>();
		List<String> has = new ArrayList<String>();
		List<String> exist = new ArrayList<String>();

		if (!(s instanceof Player)) {

			if (args.length >= 3) {
				String add = config.getString("AddRanks").replace("%player%", args[1]);
				if (isRegistered(args[1])) {

					List<String> list = new ArrayList<String>();
					for (int i = 2; i < args.length; i++) {
						if (ranksAll.contains(args[i])) {
							if (!config.getStringList("staff." + args[1] + ".ranks").contains(args[i])) {
								list.add(args[i]);
								ArrayList<String> newList = new ArrayList<String>();
								for (String element : list) {
									if (!newList.contains(element)) {
										newList.add(element);
									}
								}
								list = newList;
								cmd(add.replace("%group%", args[i]));
								added.add(args[i]);
							} else {
								has.add(args[i]);
							}
						} else {
							exist.add(args[i]);
						}
					}

					List<String> ranks = config.getStringList("staff." + args[1] + ".ranks");
					ranks.addAll(list);
					config.set("staff." + args[1] + ".ranks", ranks);
					saveConfig();
					if (added.size() > 0) {
						s.sendMessage(green + "Added " + color(darkG, added.toString().replace("[", "").replace("]", "")) + color(green, " to ")
								+ color(darkG, args[1]) + color(green, "."));
					}
					if (has.size() > 0) {
						s.sendMessage(red + "This player already has " + color(darkR, has.toString().replace("[", "").replace("]", "")) + color(red, "."));
					}
					if (exist.size() > 0) {
						if (exist.size() == 1) {
							s.sendMessage(color(darkR, exist.toString().replace("[", "").replace("]", "")) + color(red, " rank doesn't exist."));
						} else {
							s.sendMessage(color(darkR, exist.toString().replace("[", "").replace("]", "")) + color(red, " ranks do not exist."));
						}
					}
				} else {
					s.sendMessage(red + "This player isn't registered.");
				}
			} else {
				s.sendMessage(invalidArgs);
			}

		} else {
			Player p = (Player) s;
			if (p.isOp()) {

				if (args.length >= 3) {
					String add = config.getString("AddRanks").replace("%player%", args[1]);
					if (isRegistered(args[1])) {
						List<String> list = new ArrayList<String>();
						for (int i = 2; i < args.length; i++) {
							if (ranksAll.contains(args[i])) {
								if (!config.getStringList("staff." + args[1] + ".ranks").contains(args[i])) {

									list.add(args[i]);

									ArrayList<String> newList = new ArrayList<String>();

									for (String element : list) {
										if (!newList.contains(element)) {
											newList.add(element);
										}
									}
									list = newList;
									cmd(add.replace("%group%", args[i]));
									added.add(args[i]);
								} else {
									has.add(args[i]);
								}
							} else {
								exist.add(args[i]);
							}
						}

						List<String> ranks = config.getStringList("staff." + args[1] + ".ranks");
						ranks.addAll(list);
						config.set("staff." + args[1] + ".ranks", ranks);
						saveConfig();

						if (added.size() > 0) {
							Editor(p,
									green + "Added " + color(darkG, added.toString().replace("[", "").replace("]", "")) + color(green, " to ")
											+ color(darkG, args[1]) + color(green, "."),
									show,
									"Remove " + ChatColor.GRAY + added.toString().replace("[", "").replace("]", "") + ChatColor.RESET + " from " + ChatColor.GRAY
											+ args[1] + ChatColor.RESET + " ?",
									suggest, "/sm remove " + args[1] + " " + added.toString().replace("[", "").replace("]", "").replace(",", ""));
						}

						if (has.size() > 0) {
							Editor(p, color(red, "This player already has ") + color(darkR, has.toString().replace("[", "").replace("]", "")) + color(red, "."),
									show, "Add other rank?", suggest, "/sm add " + args[1] + " ");
						}

						if (exist.size() > 0) {
							if (exist.size() == 1) {
								Editor(p, color(darkR, exist.toString().replace("[", "").replace("]", "")) + color(red, " doesn't exist. Create it?"), show,
										net.md_5.bungee.api.ChatColor.RESET + "/sm create " + ChatColor.GRAY + "(rank) (section)", suggest,
										"/sm create " + exist.toString().replace("[", "").replace("]", "").replace(",", "") + " ");
							} else {
								Editor(p, color(red, "Theese ranks do not exist. Create them?"), show,
										net.md_5.bungee.api.ChatColor.RESET + "/sm create " + ChatColor.GRAY + "(rank) (section)", suggest,
										"/sm create (section) " + exist.toString().replace("[", "").replace("]", "").replace(",", ""));
							}
						}

					} else {
						Editor(p, red + "This player isn't registered.", show, "/sm register (player) (password) <ranks...>", suggest, "/sm register " + args[1]);
					}
				} else {
					Editor(p, invalidArgs, show, "/sm add" + ChatColor.GRAY + " (player) (rank...)", suggest, "/sm add ");
				}
			} else {
				p.sendMessage(noPerm);
			}
		}
	}

	// /sm _0_create _1_section _2_rank
	private void create(CommandSender s, Command command, String label, String[] args) {

		List<String> created = new ArrayList<String>();
		List<String> notcreated = new ArrayList<String>();

		if (!(s instanceof Player)) {

			if (args.length >= 3) {

				if (config.getConfigurationSection("ranks").getKeys(false).contains(args[1])) {
					for (int i = 2; i < args.length; i++) {
						if (!ranksAll.contains(args[i])) {
							created.add(args[i]);
							ranksAll.add(args[i]);
						} else {
							notcreated.add(args[i]);
						}
					}
				} else {
					s.sendMessage(red + "Invalid section.");
				}

				if (created.size() > 0) {
					List<String> list = new ArrayList<String>();
					list.addAll(config.getStringList("ranks." + args[1]));
					list.addAll(created);
					config.set("ranks." + args[1], list);
					saveConfig();
					if (created.size() == 1) {
						s.sendMessage(color(green, "Created ") + color(darkG, created.toString().replace("[", "").replace("]", ""))
								+ color(green, " rank under the ") + color(darkG, args[1]) + color(green, " section."));
					} else {
						s.sendMessage(color(green, "Created ") + color(darkG, created.toString().replace("[", "").replace("]", ""))
								+ color(green, " ranks under the ") + color(darkG, args[1]) + color(green, " section."));
					}
				}

				if (notcreated.size() > 0) {
					if (notcreated.size() == 1) {
						s.sendMessage(color(darkR, notcreated.toString().replace("[", "").replace("]", "")) + color(red, " rank already exist."));
					} else {
						s.sendMessage(color(darkR, notcreated.toString().replace("[", "").replace("]", "")) + color(red, " ranks already exist."));
					}
				}

			} else {
				s.sendMessage(invalidArgs);
			}

		} else {
			Player p = (Player) s;
			if (p.isOp()) {
				List<String> arg = new ArrayList<String>();
				for (int i = 1; i < args.length; i++) {
					arg.add(args[i]);
				}
				if (args.length >= 3) {

					if (config.getConfigurationSection("ranks").getKeys(false).contains(args[1])) {
						for (int i = 2; i < args.length; i++) {
							if (!ranksAll.contains(args[i])) {
								created.add(args[i]);
								ranksAll.add(args[i]);
							} else {
								notcreated.add(args[i]);
							}
						}
					} else {
						Editor(p, red + "Invalid section.", show, "/sm create" + ChatColor.GRAY + " (section) (ranks)", suggest,
								"/sm create (section) " + arg.toString().replace("[", "").replace("]", "").replace(",", ""));
					}

					if (created.size() > 0) {
						List<String> list = new ArrayList<String>();
						list.addAll(config.getStringList("ranks." + args[1]));
						list.addAll(created);
						config.set("ranks." + args[1], list);
						saveConfig();
						if (created.size() == 1) {
							Editor(p,
									color(green, "Created ") + color(darkG, created.toString().replace("[", "").replace("]", "")) + color(green, " rank under the ")
											+ color(darkG, args[1]) + color(green, " section."),
									show,
									ChatColor.RESET + "Delete " + ChatColor.GRAY + created.toString().replace("[", "").replace("]", "") + ChatColor.RESET + "?",
									suggest, "/sm delete " + created.toString().replace("[", "").replace("]", "").replace(",", ""));
						} else {
							Editor(p,
									color(green, "Created ") + color(darkG, created.toString().replace("[", "").replace("]", "")) + color(green, " ranks under the ")
											+ color(darkG, args[1]) + color(green, " section."),
									show,
									ChatColor.RESET + "Delete " + ChatColor.GRAY + created.toString().replace("[", "").replace("]", "") + ChatColor.RESET + "?",
									suggest, "/sm delete " + created.toString().replace("[", "").replace("]", "").replace(",", ""));
						}
					}

					if (notcreated.size() > 0) {
						if (notcreated.size() == 1) {
							s.sendMessage(color(darkR, notcreated.toString().replace("[", "").replace("]", "")) + color(red, " rank already exist."));
						} else {
							s.sendMessage(color(darkR, notcreated.toString().replace("[", "").replace("]", "")) + color(red, " ranks already exist."));
						}
					}
				} else {
					Editor(p, invalidArgs, show, "/sm create" + ChatColor.GRAY + " (section) (ranks)", suggest, "/sm create ");
				}

			} else {
				p.sendMessage(noPerm);
			}
		}
	}

	// /sm _0_delete _1_ranks...
	private void delete(CommandSender s, Command command, String label, String[] args) {

		List<String> deleted = new ArrayList<String>();
		List<String> notdeleted = new ArrayList<String>();

		if (!(s instanceof Player)) {

			if (args.length >= 2) {

				for (int i = 1; i < args.length; i++) {
					if (ranksAll.contains(args[i])) {

						List<String> list = new ArrayList<String>();

						for (String string : config.getConfigurationSection("ranks").getKeys(false)) {
							if (config.getStringList("ranks." + string).contains(args[i])) {
								list.addAll(config.getStringList("ranks." + string));
								list.remove(args[i]);
								ranksAll.remove(args[i]);
								deleted.add(args[i]);
								config.set("ranks." + string, list);
								saveConfig();
							}
						}

						for (String string : config.getConfigurationSection("staff").getKeys(false)) {
							if (config.getStringList("staff." + string + ".ranks").contains(args[i])) {

								List<String> lists = config.getStringList("staff." + string + ".ranks");
								lists.remove(args[i]);
								config.set("staff." + string + ".ranks", lists);
								saveConfig();
							}
						}

					} else {
						notdeleted.add(args[i]);
					}
				}

				if (deleted.size() != 0) {
					if (deleted.size() == 1) {
						s.sendMessage(green + "Deleted " + color(darkG, deleted.toString().replace("[", "").replace("]", "")) + color(green, " rank."));
					} else {
						s.sendMessage(green + "Deleted " + color(darkG, deleted.toString().replace("[", "").replace("]", "")) + color(green, " ranks."));
					}
				}

				if (notdeleted.size() != 0) {
					if (notdeleted.size() == 1) {
						s.sendMessage(color(darkR, notdeleted.toString().replace("[", "").replace("]", "")) + color(red, " rank does not exist."));
					} else {
						s.sendMessage(color(darkR, notdeleted.toString().replace("[", "").replace("]", "")) + color(red, " ranks do not exist."));
					}
				}

			} else {
				s.sendMessage(invalidArgs);
			}

		} else {

			Player p = (Player) s;

			if (p.isOp()) {
				if (args.length >= 2) {
					for (int i = 1; i < args.length; i++) {
						if (ranksAll.contains(args[i])) {

							List<String> list = new ArrayList<String>();

							for (String string : config.getConfigurationSection("ranks").getKeys(false)) {
								if (config.getStringList("ranks." + string).contains(args[i])) {
									list.addAll(config.getStringList("ranks." + string));
									list.remove(args[i]);
									ranksAll.remove(args[i]);
									deleted.add(args[i]);
									config.set("ranks." + string, list);
									saveConfig();
								}
							}

							for (String string : config.getConfigurationSection("staff").getKeys(false)) {
								if (config.getStringList("staff." + string + ".ranks").contains(args[i])) {

									List<String> lists = config.getStringList("staff." + string + ".ranks");
									lists.remove(args[i]);
									config.set("staff." + string + ".ranks", lists);
									saveConfig();
								}
							}

						} else {
							notdeleted.add(args[i]);
						}
					}

					if (deleted.size() != 0) {
						if (deleted.size() == 1) {
							Editor(p, green + "Deleted " + color(darkG, deleted.toString().replace("[", "").replace("]", "")) + color(green, " rank."), show,
									green + "Create " + darkG + deleted.toString().replace("[", "").replace("]", "") + green + " rank back.", suggest,
									"/staffmode create (section) " + deleted.toString().replace("[", "").replace("]", ""));
						} else {
							Editor(p, color(green, "Deleted ") + color(darkG, deleted.toString().replace("[", "").replace("]", "")) + color(green, " ranks."), show,
									green + "Create " + darkG + deleted.toString().replace("[", "").replace("]", "") + green + " ranks back.", suggest,
									"/staffmode create (section) " + deleted.toString().replace("[", "").replace("]", "").replace(",", ""));
						}
					}

					if (notdeleted.size() != 0) {
						if (notdeleted.size() == 1) {
							p.sendMessage(color(darkR, notdeleted.toString().replace("[", "").replace("]", "")) + color(red, " rank does not exist."));
						} else {
							p.sendMessage(color(darkR, notdeleted.toString().replace("[", "").replace("]", "")) + color(red, " ranks do not exist."));
						}
					}
				} else {
					Editor(p, invalidArgs, show, "/sm delete" + ChatColor.GRAY + " (rank) (section)", suggest, "/sm delete ");
				}
			}

			if (!p.isOp()) {
				p.sendMessage(noPerm);
			}
		}
	}

	// /sm _0_remove _1_name _2_ranks...
	private void remove(CommandSender s, Command command, String label, String[] args) {

		List<String> removed = new ArrayList<String>();
		List<String> notremoved = new ArrayList<String>();
		List<String> exist = new ArrayList<String>();
		String remove = config.getString("RemoveRanks").replace("%player%", args[1]);

		if (!(s instanceof Player)) {

			if (args.length >= 3) {
				if (isRegistered(args[1])) {
					List<String> list = new ArrayList<String>();
					for (int i = 2; i < args.length; i++) {
						if (ranksAll.contains(args[i])) {
							if (config.getStringList("staff." + args[1] + ".ranks").contains(args[i])) {

								list.add(args[i]);
								cmd(remove.replace("%group%", args[i]));

								List<String> ranks = config.getStringList("staff." + args[1] + ".ranks");
								ranks.removeAll(list);
								removed.add(args[i]);
								config.set("staff." + args[1] + ".ranks", ranks);
								saveConfig();

							} else {
								notremoved.add(args[i]);
							}
						} else {
							exist.add(args[i]);
						}
					}

					if (removed.size() != 0) {
						if (removed.size() == 1) {
							s.sendMessage(
									green + "Removed " + color(darkG, removed.toString().replace("[", "").replace("]", "")) + color(green, " rank from " + args[1]));
						} else {
							s.sendMessage(green + "Removed " + color(darkG, removed.toString().replace("[", "").replace("]", ""))
									+ color(green, " ranks from " + args[1]));
						}
					}

					if (notremoved.size() != 0) {
						s.sendMessage(
								darkR + args[1] + red + " doesn't have " + color(darkR, notremoved.toString().replace("[", "").replace("]", "")) + color(red, "."));
					}

					if (exist.size() != 0) {
						if (exist.size() == 1) {
							s.sendMessage(color(darkR, exist.toString().replace("[", "").replace("]", "")) + color(red, " rank doesn't exist."));
						} else {
							s.sendMessage(color(darkR, exist.toString().replace("[", "").replace("]", "")) + color(red, " ranks do not exist."));
						}
					}
				} else {
					s.sendMessage(red + "Invalid player name.");
				}
			} else {
				s.sendMessage(invalidArgs);
			}

		} else {

			Player p = (Player) s;

			if (p.isOp()) {

				if (args.length >= 3) {
					if (isRegistered(args[1])) {
						List<String> list = new ArrayList<String>();
						for (int i = 2; i < args.length; i++) {
							if (ranksAll.contains(args[i])) {
								if (config.getStringList("staff." + args[1] + ".ranks").contains(args[i])) {

									list.add(args[i]);
									cmd(remove.replace("%group%", args[i]));

									List<String> ranks = config.getStringList("staff." + args[1] + ".ranks");
									ranks.removeAll(list);
									removed.add(args[i]);
									config.set("staff." + args[1] + ".ranks", ranks);
									saveConfig();

								} else {
									notremoved.add(args[i]);
								}
							} else {
								exist.add(args[i]);
							}
						}

						if (removed.size() != 0) {
							if (removed.size() == 1) {
								Editor(p,
										green + "Removed " + color(darkG, removed.toString().replace("[", "").replace("]", "")) + color(green, " rank from ")
												+ color(darkG, args[1]) + color(green, "."),
										show,
										"Add " + ChatColor.GRAY + removed.toString().replace("[", "").replace("]", "") + ChatColor.RESET + " to " + ChatColor.GRAY
												+ args[1] + ChatColor.RESET + " back?",
										suggest, "/sm add " + args[1] + " " + removed.toString().replace("[", "").replace("]", "").replace(",", ""));
							} else {
								Editor(p,
										green + "Removed " + color(darkG, removed.toString().replace("[", "").replace("]", "")) + color(green, " ranks from ")
												+ color(darkG, args[1]) + color(green, "."),
										show,
										"Add " + ChatColor.GRAY + removed.toString().replace("[", "").replace("]", "") + ChatColor.RESET + " to " + ChatColor.GRAY
												+ args[1] + ChatColor.RESET + " back?",
										suggest, "/sm add " + args[1] + " " + removed.toString().replace("[", "").replace("]", "").replace(",", ""));
							}
						}

						if (notremoved.size() != 0) {
							s.sendMessage(darkR + args[1] + red + " doesn't have " + color(darkR, notremoved.toString().replace("[", "").replace("]", ""))
									+ color(red, "."));
						}

						if (exist.size() != 0) {
							if (exist.size() == 1) {
								s.sendMessage(color(darkR, exist.toString().replace("[", "").replace("]", "")) + color(red, " rank doesn't exist."));
							} else {
								s.sendMessage(color(darkR, exist.toString().replace("[", "").replace("]", "")) + color(red, " ranks do not exist."));
							}
						}

					} else {
						p.sendMessage(red + "Invalid player name.");
					}
				} else {
					Editor(p, invalidArgs, show, "/sm remove " + ChatColor.GRAY + "(player) (ranks)", suggest, "/sm remove ");
				}

			}

			if (!p.isOp()) {
				p.sendMessage(noPerm);
			}
		}
	}

	// /sm _0_register _1_name _2_password <_3_ranks...>
	private void register(CommandSender s, Command command, String label, String[] args) {

		if (!(s instanceof Player)) {
			if (args.length >= 3) {
				if (!isRegistered(args[1])) {

					List<String> list = new ArrayList<String>();

					config.set("staff." + args[1] + ".password", args[2]);
					config.set("staff." + args[1] + ".ranks", null);

					for (int i = 3; i < args.length; i++) {
						list.add(args[i]);
					}

					config.set("staff." + args[1] + ".ranks", list);
					config.set("staff." + args[1] + ".logged", false);
					if (getServer().getOnlinePlayers().toString().contains(args[1])) {
						config.set("staff." + args[1] + ".online", true);
					} else {
						config.set("staff." + args[1] + ".online", false);
					}
					
					if (config.getConfigurationSection("unregistered").getKeys(false).contains(args[1])) {
						config.set("unregistered." + args[1], null);
					}

					s.sendMessage(green + "Registered " + darkG + args[1] + color(green, " successfully with ranks ")
							+ color(darkG, list.toString().replace("[", "").replace("]", "")) + color(green, "."));
					saveConfig();
				} else {
					s.sendMessage(red + "This player is already registered.");
				}
			} else {
				s.sendMessage(invalidArgs);
			}
		} else {

			Player p = (Player) s;

			if (p.isOp()) {
				if (args.length >= 3) {
					if (!isRegistered(args[1])) {

						List<String> list = new ArrayList<String>();

						config.set("staff." + args[1] + ".password", args[2]);
						config.createSection("staff." + args[1] + ".ranks");

						for (int i = 3; i < args.length; i++) {
							list.add(args[i]);
						}

						config.set("staff." + args[1] + ".ranks", list);
						config.set("staff." + args[1] + ".logged", false);
						if (getServer().getPlayer(args[1]).isOnline()) {
							config.set("staff." + args[1] + ".online", true);
						} else {
							config.set("staff." + args[1] + ".online", false);
						}
						
						if (config.getConfigurationSection("unregistered").getKeys(false).contains(args[1])) {
							config.set("unregistered." + args[1], null);
						}

						Editor(p,
								green + "Registered " + darkG + args[1] + color(green, " successfully with ranks ")
										+ color(darkG, list.toString().replace("[", "").replace("]", "")) + color(green, "."),
								show, "Unregister " + ChatColor.GRAY + args[1] + ChatColor.RESET + ".", suggest, "/sm unregister " + args[1]);
						saveConfig();
					} else {
						p.sendMessage(red + "This player is already registered.");
					}
				} else {
					Editor(p, invalidArgs, show, "/sm register " + ChatColor.GRAY + "(player) (password) <ranks...>", suggest, "/sm register ");
				}
			}

			if (!p.isOp()) {
				p.sendMessage(noPerm);
			}
		}
	}

	// /sm _0_unregister _1_name
	private void unregister(CommandSender s, Command command, String label, String[] args) {

		if (!(s instanceof Player)) {
			if (args.length == 2) {
				if (isRegistered(args[1])) {

					config.getConfigurationSection("staff").getKeys(false).forEach(string -> {
						if (string.equals(args[1])) {
							config.set("staff." + string, null);
							saveConfig();
						}
					});

					s.sendMessage(green + "Unregistered " + darkG + args[1] + green + " successfully.");
				} else {
					s.sendMessage(red + "Invalid player name.");
				}
			} else {
				s.sendMessage(invalidArgs);
			}
		} else {

			Player p = (Player) s;

			if (p.isOp()) {
				if (args.length == 2) {
					if (isRegistered(args[1])) {

						config.getConfigurationSection("staff").getKeys(false).forEach(string -> {
							if (string.equals(args[1])) {
								config.set("staff." + string, null);
								saveConfig();
							}
						});

						Editor(p, green + "Unregistered " + darkG + args[1] + green + " successfully.", show,
								"Register " + net.md_5.bungee.api.ChatColor.GRAY + args[1] + net.md_5.bungee.api.ChatColor.RESET + " back?", suggest,
								"/sm register " + args[1] + " ");
					} else {
						p.sendMessage(red + "Invalid player name.");
					}
				} else {
					Editor(p, invalidArgs, show, "/sm unregister" + ChatColor.GRAY + " (player)", suggest, "/sm unregister ");
				}
			}

			if (!p.isOp()) {
				p.sendMessage(noPerm);
			}
		}
	}

	// /sm _0_login _1_password
	private void login(CommandSender s, Command command, String label, String[] args) {

		if (!(s instanceof Player)) {
			s.sendMessage(ChatColor.RED + "You have to be a player to execute this command.");
		} else {

			Player p = (Player) s;

			if (isRegistered(p.getName())) {
				if (args.length == 2) {
					if (!config.getBoolean("staff." + p.getName() + ".logged")) {
						if (config.getString("staff." + p.getName() + ".password").equals(args[1])) {

							addPlayerConfigRanks(p);

							config.set("staff." + p.getName() + ".logged", true);

							saveConfig();

							p.sendMessage(green + "Logged in successfully.");
						} else {
							Editor(p, red + "Invalid password.", show, net.md_5.bungee.api.ChatColor.RESET + "Try again?", suggest, "/sm login ");
						}
					} else {
						p.sendMessage(red + "You are already logged in.");
					}
				} else {
					Editor(p, invalidArgs, show, "/sm login" + ChatColor.GRAY + " (password)", suggest, "/sm login ");
				}
			} else {
				p.sendMessage(red + "You aren't registered.");
			}
		}
	}

	// /sm _0_password _1_old _2_new
	private void password(CommandSender s, Command command, String label, String[] args) {

		if (!(s instanceof Player)) {

			s.sendMessage(ChatColor.RED + "You have to be a player to execute this command.");

		} else {

			Player p = (Player) s;

			if (args.length == 3) {
				if (isRegistered(p.getName())) {
					if (config.getBoolean("staff." + p.getName() + ".logged")) {
						if (config.getString("staff." + p.getName() + ".password").equals(args[1])) {

							config.set("staff." + p.getName() + ".password", args[2]);
							saveConfig();
							Editor(p, green + "Password changed successfully.", show, "Change again?", suggest, "/sm password " + args[2] + " ");

						} else {
							p.sendMessage(red + "Invalid password.");
						}
					} else {
						Editor(p, red + "You aren't logged in.", show, "Login?", suggest, "/sm login ");
					}
				} else {
					p.sendMessage(red + "You aren't registered.");
				}
			} else {
				Editor(p, invalidArgs, show, "/sm password" + ChatColor.GRAY + " (old) (new)", suggest, "/sm password ");
			}
		}
	}

	// /sm _0_set _1_player _2_password
	private void set(CommandSender s, Command command, String label, String[] args) {

		if (!(s instanceof Player)) {
			if (args.length == 3) {
				if (isRegistered(args[1])) {

					config.set("staff." + args[1] + ".password", args[2]);
					saveConfig();
					s.sendMessage(green + "Password for " + darkG + args[1] + green + " changed successfully.");

				} else {
					s.sendMessage(red + "This player isn't registered.");
				}
			} else {
				s.sendMessage(invalidArgs);
			}
		} else {

			Player p = (Player) s;

			if (p.isOp()) {
				if (args.length == 3) {
					if (isRegistered(args[1])) {

						config.set("staff." + args[1] + ".password", args[2]);
						saveConfig();
						s.sendMessage(green + "Password for " + darkG + args[1] + green + " changed successfully.");

					} else {
						s.sendMessage(red + "This player isn't registered.");
					}
				} else {
					s.sendMessage(invalidArgs);
				}
			}

			if (!p.isOp()) {
				p.sendMessage(noPerm);
			}
		}
	}


	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {

		// /cmd args[0] args[1] args[2]

		try {

			ArrayList<String> list = new ArrayList<String>();
			ArrayList<String> commands = new ArrayList<String>();
			commands.add("password");
			commands.add("register");
			commands.add("create");
			commands.add("delete");
			commands.add("help");
			commands.add("reload");

			if (!config.getConfigurationSection("staff").getKeys(false).isEmpty()) {
				commands.add("login");
				commands.add("get");
				commands.add("add");
				commands.add("remove");
				commands.add("set");
				commands.add("unregister");
			}

			if (!(s instanceof Player)) {

				// +++++++++++++++ CONSOLE +++++++++++++++ //

				if (args[0].equals("")) {
					commands.remove("login");
					commands.remove("password");
					list.addAll(commands);
					return list;
				}

				if (args.length == 1) {
					commands.remove("login");
					commands.remove("password");
					commands.forEach(string -> {
						if (string.toLowerCase().startsWith(args[0].toLowerCase())) {
							list.add(string);
						}
					});
					return list;
				}

				switch (args[0]) {

				case "g":
				case "get": // /sm _0_get <_1_name>
					if (args.length == 2) {
						if (args[1].equals("")) {
							config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
							return list;
						}
					}
					break;

				case "a":
				case "add": // /sm _0_add _1_name _2_ranks...

					if (args[1].equals("")) {
						config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
						return list;
					}

					if (args[2].equals("")) {
						List<String> staff = config.getStringList("ranks.staff");
						List<String> regular = config.getStringList("ranks.regular");
						list.addAll(staff);
						list.addAll(regular);

						for (String string : config.getStringList("staff." + args[1] + ".ranks")) {
							if (list.contains(string)) {
								list.remove(string);
							}
						}

						return list;
					}

					if (args.length == 2) {
						config.getConfigurationSection("staff").getKeys(false).forEach(string -> {
							if (string.toLowerCase().startsWith(args[1].toLowerCase())) {
								list.add(string);
							}
						});
						return list;
					}

					if (args.length >= 3) {
						ranksAll.forEach(string -> {
							if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
								list.add(string);
								for (String check : config.getStringList("staff." + args[1] + ".ranks")) {
									if (list.contains(check)) {
										list.remove(check);
									}
								}
							}
						});

						for (int i = 0; i < args.length; i++) {
							if (list.contains(args[i])) {
								list.remove(args[i]);
							}
						}
						return list;
					}
					break;

				case "c":
				case "create": // /sm _0_create _1_rank _2_section

					if (args[1].equals("")) {
						config.getConfigurationSection("ranks").getKeys(false).forEach(string -> list.add(string));
						return list;
					}

					if (args[2].equals("")) {
						list.clear();
						return list;
					}

					if (!args[2].equals("")) {
						list.clear();
						return list;
					}
					break;

				case "rm":
				case "remove":// /sm _0_remove _1_name _2_ranks...

					if (args[1].equals("")) {
						config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
						return list;
					}

					if (args[2].equals("")) {
						config.getStringList("staff." + args[1] + ".ranks").forEach(string -> list.add(string));
						return list;
					}

					if (args.length >= 3) {
						config.getStringList("staff." + args[1] + ".ranks").forEach(string -> {
							if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
								list.add(string);
							}
						});

						for (int i = 0; i < args.length; i++) {
							if (list.contains(args[i])) {
								list.remove(args[i]);
							}
						}
						return list;
					}
					break;

				case "d":
				case "del":
				case "delete":// /sm _0_delete _1_ranks...

					if (args[1].equals("")) {
						list.addAll(ranksAll);
						return list;
					}

					if (args.length >= 2) {
						ranksAll.forEach(string -> {
							if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
								list.add(string);
							}
						});

						for (int i = 0; i < args.length; i++) {
							if (list.contains(args[i])) {
								list.remove(args[i]);
							}
						}
						return list;
					}
					break;

				case "r":
				case "register":// /sm _0_register _1_name _2_password <_3_ranks..>

					if (args[1].equals("")) {
						return null;
					}

					if (args.length == 3) {
						list.clear();
						return list;
					}

					ranksAll.forEach(string -> {
						if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
							list.add(string);
						}
					});

					for (int i = 0; i < args.length; i++) {
						if (list.contains(args[i])) {
							list.remove(args[i]);
						}
					}
					return list;

				case "u":
				case "unregister":// /sm _0_unregister _1_name

					if (args[1].equals("")) {
						config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
						return list;
					}

					if (args.length > 2) {
						list.clear();
						return list;
					}
					break;

				case "s":
				case "set":// /sm _0_set _1_player _2_password

					if (args[1].equals("")) {
						config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
						return list;
					}

					if (!args[1].equals("")) {
						list.clear();
						return list;
					}

					break;

				case "h":
				case "help":

					commands.remove("help");
					list.addAll(commands);
					return list;

				case "rl":
				case "reload":

					if (args[1].equals("")) {
						list.add("config");
						return list;
					}

					if (args.length == 2) {
						if ("config".toLowerCase().startsWith(args[1].toLowerCase())) {
							list.add("config");
						}
					} else {
						list.clear();
					}
					return list;
				}

				// --------------- CONSOLE --------------- //

			} else {
				Player p = (Player) s;
				if (p.isOp()) {

					// +++++++++++++++ OPPED PLAYER +++++++++++++++ //

					if (args[0].equals("")) {
						list.addAll(commands);
						list.remove("login");
						list.remove("password");
						if (isRegistered(p.getName())) {
							if (!config.getBoolean("staff." + p.getName() + ".logged")) {
								list.add("login");
							} else {
								list.add("password");
							}
							list.add("help");
						}
						return list;
					}

					if (args.length == 1) {
						commands.remove("login");
						commands.remove("password");
						commands.forEach(string -> {
							if (string.toLowerCase().startsWith(args[0].toLowerCase())) {
								list.add(string);
							}
						});
						return list;
					}

					switch (args[0]) {

					case "g":
					case "get": // /sm _0_get <_1_name>
						if (args.length == 2) {
							if (args[1].equals("")) {
								config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
								return list;
							}
						} else {
							list.clear();
							return list;
						}

						break;

					case "a":
					case "add": // /sm _0_add _1_name _2_ranks...

						if (args[1].equals("")) {
							config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
							return list;
						}

						if (args.length == 2) {
							config.getConfigurationSection("staff").getKeys(false).forEach(string -> {
								if (string.toLowerCase().startsWith(args[1].toLowerCase())) {
									list.add(string);
								}
							});
							return list;
						}

						if (args[2].equals("")) {
							List<String> staff = config.getStringList("ranks.staff");
							List<String> regular = config.getStringList("ranks.regular");
							list.addAll(staff);
							list.addAll(regular);

							for (String string : config.getStringList("staff." + args[1] + ".ranks")) {
								if (list.contains(string)) {
									list.remove(string);
								}
							}

							return list;
						}

						if (args.length >= 3) {
							ranksAll.forEach(string -> {
								if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
									list.add(string);
									for (String check : config.getStringList("staff." + args[1] + ".ranks")) {
										if (list.contains(check)) {
											list.remove(check);
										}
									}
								}
							});

							for (int i = 0; i < args.length; i++) {
								if (list.contains(args[i])) {
									list.remove(args[i]);
								}
							}
							return list;
						}
						break;

					case "c":
					case "create": // /sm _0_create _1_section _2_ranks...

						if (args[1].equals("")) {
							config.getConfigurationSection("ranks").getKeys(false).forEach(string -> list.add(string));
							return list;
						}

						if (args.length > 2) {
							list.clear();
							return list;
						}

						if (!args[1].equals("")) {
							config.getConfigurationSection("ranks").getKeys(false).forEach(string -> {
								if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
									list.add(string);
								}
							});
							return list;
						}
						break;

					case "rm":
					case "remove":// /sm _0_remove _1_name _2_ranks...

						if (args[1].equals("")) {
							config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
							return list;
						}

						if (args[2].equals("")) {
							config.getStringList("staff." + args[1] + ".ranks").forEach(string -> list.add(string));
							return list;
						}

						if (args.length >= 3) {
							config.getStringList("staff." + args[1] + ".ranks").forEach(string -> {
								if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
									list.add(string);
								}
							});

							for (int i = 0; i < args.length; i++) {
								if (list.contains(args[i])) {
									list.remove(args[i]);
								}
							}
							return list;
						}
						break;

					case "d":
					case "del":
					case "delete":// /sm _0_delete _1_ranks...

						if (args[1].equals("")) {
							list.addAll(ranksAll);
							return list;
						}

						if (args.length >= 2) {
							ranksAll.forEach(string -> {
								if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
									list.add(string);
								}
							});

							for (int i = 0; i < args.length; i++) {
								if (list.contains(args[i])) {
									list.remove(args[i]);
								}
							}
							return list;
						}
						break;

					case "l":
					case "login":// /sm _0_login _1_password

						list.clear();
						return list;

					case "r":
					case "register":// /sm _0_register _1_name _2_password <_3_ranks..>

						if (args[1].equals("")) {
							return null;
						}

						if (args.length == 3) {
							list.clear();
							return list;
						}

						ranksAll.forEach(string -> {
							if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
								list.add(string);
							}
						});

						for (int i = 0; i < args.length; i++) {
							if (list.contains(args[i])) {
								list.remove(args[i]);
							}
						}
						return list;

					case "u":
					case "unregister":// /sm _0_unregister _1_name

						if (args[1].equals("")) {
							config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
							return list;
						}

						if (args.length > 2) {
							list.clear();
							return list;
						}
						break;

					case "p":
					case "pass":
					case "password":// /sm _0_password _1_old _2_new

						list.clear();
						return list;

					case "s":
					case "set":// /sm _0_set _1_player _2_password

						if (args[1].equals("")) {
							config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
							return list;
						}

						if (!args[1].equals("")) {
							list.clear();
							return list;
						}

						break;

					case "h":
					case "help":

						commands.remove("help");
						list.addAll(commands);
						return list;
					}

					// --------------- OPPED PLAYER --------------- //

				} else {

					// ++++++++++++++++ REGULAR PLAYER +++++++++++++++ //

					if (args[0].equals("")) {
						if (isRegistered(p.getName())) {
							if (!config.getBoolean("staff." + p.getName() + ".logged")) {
								list.add("login");
							} else {
								list.add("password");
							}
							list.add("help");
						}
						return list;
					}

					if (args.length == 1) {
						commands.clear();
						if (isRegistered(p.getName())) {
							commands.add("login");
							commands.add("password");
							commands.add("help");
							commands.forEach(string -> {
								if (string.toLowerCase().startsWith(args[0].toLowerCase())) {
									list.add(string);
								}
							});
						}
						return list;
					}

					switch (args[0]) {

					case "l":
					case "login": // /sm _0_login _1_password

						list.clear();
						return list;

					case "p":
					case "pass":
					case "password": // /sm _0_password _1_old _2_new

						list.clear();
						return list;

					case "h":
					case "help": // /sm _0_help

						list.add("login");
						list.add("password");
						return list;

					}

					// --------------- REGULAR PLAYER --------------- //

				}
			}

		} catch (ArrayIndexOutOfBoundsException e) {

		}

		return null;
	}

//	private void Editor(Player p, String chatMessage, net.md_5.bungee.api.chat.HoverEvent.Action show, String hoverText,
//			net.md_5.bungee.api.chat.ClickEvent.Action suggest, String actionText) {
//
//		TextComponent tc1 = new TextComponent();
//		tc1.setText(chatMessage);
//		tc1.setClickEvent(new ClickEvent(suggest, actionText));
//		tc1.setHoverEvent(new HoverEvent(show, new ComponentBuilder(hoverText).create()));
//
//		p.spigot().sendMessage(tc1);
//	}

	private void Help(CommandSender s) {

		String point = ChatColor.BLUE + "" + ChatColor.BOLD + ">> " + ChatColor.AQUA;
		String click = ChatColor.GREEN + "Use command.";

		if (!(s instanceof Player)) {
			s.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Staffmode commands:");

			s.sendMessage(point + "login " + ChatColor.DARK_GRAY + "(password)");
			s.sendMessage(point + "password " + ChatColor.DARK_GRAY + "(old) (new)");
			s.sendMessage(point + "set " + ChatColor.DARK_GRAY + "(player) (password)");
			s.sendMessage(point + "register " + ChatColor.DARK_GRAY + "(name) (password) <ranks>");
			s.sendMessage(point + "unregister " + ChatColor.DARK_GRAY + "(name)");
			s.sendMessage(point + "get " + ChatColor.DARK_GRAY + "(name)");
			s.sendMessage(point + "add " + ChatColor.DARK_GRAY + "(name) (rank)");
			s.sendMessage(point + "remove " + ChatColor.DARK_GRAY + "(name) (rank)");
			s.sendMessage(point + "create " + ChatColor.DARK_GRAY + "(rank) (section)");
			s.sendMessage(point + "delete " + ChatColor.DARK_GRAY + "(rank)");
			s.sendMessage(point + "reload " + ChatColor.GRAY + "[config]");

		} else {
			Player p = (Player) s;

			if (p.isOp()) {
				s.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Staffmode commands:");

				Editor(p, point + "login " + ChatColor.GRAY + "(password)", show, click, suggest, "/staffmode login ");
				Editor(p, point + "password " + ChatColor.GRAY + "(old) (new)", show, click, suggest, "/staffmode password ");
				Editor(p, point + "set " + ChatColor.GRAY + "(player) (password)", show, click, suggest, "/staffmode set ");
				Editor(p, point + "register " + ChatColor.GRAY + "(name) (password) <ranks>", show, click, suggest, "/staffmode register ");
				Editor(p, point + "unregister " + ChatColor.GRAY + "(name)", show, click, suggest, "/staffmode unregister ");
				Editor(p, point + "get " + ChatColor.GRAY + "(name)", show, click, suggest, "/staffmode get");
				Editor(p, point + "add " + ChatColor.GRAY + "(name) (rank)", show, click, suggest, "/staffmode add ");
				Editor(p, point + "remove " + ChatColor.GRAY + "(name) (rank)", show, click, suggest, "/staffmode remove ");
				Editor(p, point + "create " + ChatColor.GRAY + "(rank) (section)", show, click, suggest, "/staffmode create ");
				Editor(p, point + "delete " + ChatColor.GRAY + "(rank)", show, click, suggest, "/staffmode delete ");
				Editor(p, point + "reload " + ChatColor.DARK_GRAY + "[config]", show, click, suggest, "/staffmode reload");

			} else if (isRegistered(p.getName())) {
				s.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Staffmode commands:");

				Editor(p, point + "login " + ChatColor.GRAY + "(password)", show, click, suggest, "/staffmode login ");
				Editor(p, point + "password " + ChatColor.GRAY + "(old) (new)", show, click, suggest, "/staffmode password ");
			} else {
				s.sendMessage(red + "You have no access to this plugin.");
			}
		}
	}

	private void Editor(Player p, String message, net.md_5.bungee.api.chat.HoverEvent.Action show, String hover, Action suggest, String click) {

		net.md_5.bungee.api.chat.TextComponent text = new net.md_5.bungee.api.chat.TextComponent();
		text.setText(message);
		text.setClickEvent(new ClickEvent(suggest, click));
		text.setHoverEvent(new HoverEvent(show, new ComponentBuilder(hover).create()));

		p.spigot().sendMessage(text);

	}

	private String Time() {
		Calendar currentDate = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String dateNow = formatter.format(currentDate.getTime());
		return (dateNow);
	}

	private void Log(String message) {

		try {
			File dataFolder = getDataFolder();

			if (!dataFolder.exists()) {
				dataFolder.mkdir();
			}

			File saveTo = new File(getDataFolder(), "logs.txt");

			if (!saveTo.exists()) {
				saveTo.createNewFile();
			}

			FileWriter fw = new FileWriter(saveTo, true);
			PrintWriter pw = new PrintWriter(fw);

			pw.println("[" + Time() + "] " + message);
			pw.flush();
			pw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String color(ChatColor ChatColor, String message) {
		char[] split = message.toCharArray();
		String whole = "";
		for (char character : split) {
			whole += ChatColor + "" + character;
		}
		return whole;
	}

}

//	import java.util.ArrayList;
//	import java.util.Collection;
//	import java.util.List;
//	
//	import org.bukkit.Bukkit;
//	import org.bukkit.ChatColor;
//	import org.bukkit.command.Command;
//	import org.bukkit.command.CommandSender;
//	import org.bukkit.command.ConsoleCommandSender;
//	import org.bukkit.configuration.MemorySection;
//	import org.bukkit.configuration.file.FileConfiguration;
//	import org.bukkit.entity.Player;
//	import org.bukkit.event.EventHandler;
//	import org.bukkit.event.Listener;
//	import org.bukkit.event.player.PlayerJoinEvent;
//	import org.bukkit.event.player.PlayerQuitEvent;
//	import org.bukkit.plugin.java.JavaPlugin;
//	
//	import net.md_5.bungee.api.chat.ClickEvent;
//	import net.md_5.bungee.api.chat.ClickEvent.Action;
//	import net.md_5.bungee.api.chat.ComponentBuilder;
//	import net.md_5.bungee.api.chat.HoverEvent;
//	import net.md_5.bungee.api.chat.TextComponent;
//	
//	public class Main extends JavaPlugin implements Listener {
//	
//		ConsoleCommandSender console = getServer().getConsoleSender();
//		FileConfiguration config;
//	
//		static Collection<String> ranksStaff = new ArrayList<>();
//		static Collection<String> ranksRegular = new ArrayList<>();
//		static Collection<String> ranksAll = new ArrayList<>();
//	
//		static Action suggest = Action.SUGGEST_COMMAND;
//		static net.md_5.bungee.api.chat.HoverEvent.Action show = net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;
//		static ChatColor red = ChatColor.RED;
//		static ChatColor darkR = ChatColor.DARK_RED;
//		static ChatColor green = ChatColor.GREEN;
//		static ChatColor darkG = ChatColor.DARK_GREEN;
//	
//		@Override
//		public void onEnable() {
//	
//			saveConfig();
//	
//			config = getConfig();
//	
//			getConfig().options().copyDefaults(true);
//			saveConfig();
//	
//			if (config.getConfigurationSection("ranks").equals(null)) {
//				MemorySection.createPath(config.getConfigurationSection("ranks"), "staff");
//			}
//	
//			ranksStaff = config.getStringList("ranks.staff");
//			ranksRegular = config.getStringList("ranks.regular");
//			config.getConfigurationSection("ranks").getKeys(false).forEach(string -> ranksAll.addAll(config.getStringList("ranks." + string)));
//	
//			Bukkit.getPluginManager().registerEvents(this, this);
//			getCommand("staffmode").setExecutor(this);
//	
//			console.sendMessage(green + "Enabled " + getDescription().getName());
//			saveConfig();
//	
//			if (config.getBoolean("AddRanks")) {
//				String command = config.getString("RanksToAdd");
//				ranksAll.forEach(string -> Bukkit.dispatchCommand(console, command.replace("%groups%", string)));
//			} else {
//				config.set("Create command", null);
//			}
//	
//			for (String name : config.getConfigurationSection("staff").getKeys(false)) {
//				try {
//					if (!getServer().getPlayer(name).isOnline()) {
//						config.set("staff." + name + ".online", false);
//						config.set("staff." + name + ".logged", false);
//						saveConfig();
//					} else {
//						config.set("staff." + name + ".online", true);
//						saveConfig();
//					}
//				} catch (NullPointerException e) {
//	
//				}
//			}
//		}
//	
//		@Override
//		public void onDisable() {
//	
//			console.sendMessage(red + "Disabled " + getDescription().getName());
//	
//		}
//	
//		// returns all the ranks the player has from the config
//		public List<String> getPlayerRanks(String player) {
//			if (config.contains("staff." + player + ".ranks")) {
//			} else {
//				config.set("staff." + player + ".ranks", "");
//				saveConfig();
//			}
//			return config.getStringList(player + ".ranks");
//		}
//	
//		public List<String> names = new ArrayList<>();
//	
//		// checks if the values from 2 lists match
//		// checks if the player has any staff ranks
//		public boolean ranksTest(Collection<String> ranksPlayer, Collection<String> ranksStaff) {
//			try {
//				for (String player : ranksPlayer) {
//					for (String staff : ranksStaff) {
//						if (player == staff) {
//							return true;
//						}
//					}
//				}
//			} catch (NullPointerException e) {
//				return false;
//			}
//			return false;
//		}
//	
//		// should return all the ranks of the player they had
//		public void addPlayerRanks(Player player) {
//			String command = "";
//			for (String group : config.getStringList("staff." + player.getName() + ".ranks")) {
//				command = config.getString("addPlayerRanks").replace("%player%", player.getName()).replace("%group%", group);
//				Bukkit.dispatchCommand(console, command);
//			}
//		}
//	
//		@EventHandler
//		public void onJoin(PlayerJoinEvent event) {
//	
//			Player p = event.getPlayer();
//	
//			p.setOp(false);
//	
//			CommandSender console = Bukkit.getConsoleSender();
//			if (isRegistered(p.getName())) {
//				config.set("staff." + p.getName() + ".online", true); V
//			}
//	
//			if (config.getConfigurationSection("staff").?contains(p.getName())) {
//				String command = config.getString("addPlayerRanks").replace("%player%", p.getName());
//				Bukkit.dispatchCommand(console, command);
//			}
//	
//			if (ranksTest(getPlayerRanks(p.getName()), ranksStaff)) {
//	
//			}
//	
//			saveConfig();
//		}
//	
//		@EventHandler
//		public void onLeave(PlayerQuitEvent event) {
//			Player p = event.getPlayer();
//			String command = config.getString("onLeave").replace("%player%", p.getName());
//			Bukkit.dispatchCommand(console, command);
//			p.setOp(false);
//			if (isRegistered(p.getName())) {
//				config.set("staff." + p.getName() + ".online", false);
//				config.set("staff." + p.getName() + ".logged", false);
//				saveConfig();
//			}
//		}
//	
//		@Override
//		public boolean onCommand(CommandSender s, Command command, String label, String[] args) {
//	
//			// Handles the reload command
//			if (args[0].equalsIgnoreCase("rl") || args[0].equalsIgnoreCase("rl")) {
//				if (args.length == 2) {
//					if (args[1].equals("config") || args[1].equals("c")) {
//						reloadConfig();
//						s.sendMessage(green + "Reloaded configuraion.");
//						return true;
//					}
//					s.sendMessage(red + "Invalid arguments.");
//					s.sendMessage(ChatColor.GOLD + "Usage: /sm reload config");
//					return true;
//	
//				} else if (args.length == 1) { // Handles the reload config command
//					getServer().getPluginManager().disablePlugin(this);
//					getServer().getPluginManager().enablePlugin(this);
//					s.sendMessage(green + "Reloaded Staffmode.");
//					return true;
//				} else {
//					s.sendMessage(red + "Invalid arguments.");
//					s.sendMessage(ChatColor.GOLD + "Usage: /sm reload");
//					return true;
//				}
//			}
//	
//			if (!(s instanceof Player)) { // checks for the arguments after /staffmode
//				if (args.length > 0) {
//					switch (args[0]) {
//	
//					case "h":
//					case "help":
//						Commands(s);
//						break;
//					case "g":
//					case "get": // /sm _0_get <_1_name>
//	
//						if (args.length == 2) {
//							if (config.contains("staff." + args[1] + ".ranks") && config.getStringList("staff." + args[1] + ".ranks").size() > 0) {
//								if (args[1].endsWith("s")) {
//									s.sendMessage(ChatColor.AQUA + args[1] + "' ranks: " + ChatColor.GOLD
//											+ config.getStringList("staff." + args[1] + ".ranks").toString().replace("[", "").replace("]", ""));
//								} else {
//									s.sendMessage(ChatColor.AQUA + args[1] + "'s ranks: " + ChatColor.GOLD
//											+ config.getStringList("staff." + args[1] + ".ranks").toString().replace("[", "").replace("]", ""));
//								}
//							} else if (config.contains("staff." + args[1])) {
//								Editor(s, red + "This player has no ranks.");
//							} else {
//								s.sendMessage(red + "Invalid player name.");
//							}
//						} else {
//							Editor(s, red + "Invalid arguments.");
//						}
//						break;
//	
//					case "a":
//					case "add": // /sm _0_add_ _1_name_ _2_ranks_
//	
//						if (args.length >= 3) {
//							if (isRegistered(args[1])) {
//								List<String> list = new ArrayList<String>();
//								for (int i = 2; i < args.length; i++) {
//									if (ranksAll.contains(args[i])) {
//										if (!config.getStringList("staff." + args[1] + ".ranks").contains(args[i])) {
//	
//											list.add(args[i]);
//	
//											ArrayList<String> newList = new ArrayList<String>();
//	
//											for (String element : list) {
//												if (!newList.contains(element)) {
//													newList.add(element);
//												}
//											}
//											list = newList;
//	
//											Editor(s, green + "Added " + darkG + args[i] + ChatColor.RESET + green + " to " + darkG + args[1] + ChatColor.RESET + green
//													+ ".");
//										} else {
//											Editor(s, red + "This player already has " + darkR + args[i] + red + ".");
//										}
//									} else {
//										Editor(s, red + "This rank doesn't exist. Create it?");
//									}
//								}
//	
//								List<String> ranks = config.getStringList("staff." + args[1] + ".ranks");
//								ranks.addAll(list);
//								config.set("staff." + args[1] + ".ranks", ranks);
//								saveConfig();
//							} else {
//								Editor(s, red + "This player isn't registered.");
//							}
//						} else {
//							Editor(s, red + "Invalid arguments.");
//						}
//						break;
//	
//					case "c":
//					case "create": // /sm _0_create _1_rank _2_section
//	
//						if (args.length == 3) {
//							if (config.getConfigurationSection("ranks").getKeys(false).contains(args[2])) {
//								if (!ranksAll.contains(args[1])) {
//	
//									List<String> list = new ArrayList<String>();
//									list.addAll(config.getStringList("ranks." + args[2]));
//									list.add(args[1]);
//									ranksAll.add(args[1]);
//									config.set("ranks." + args[2], list);
//									saveConfig();
//									Editor(s, green + "Created " + darkG + args[1] + green + " rank under the " + darkG + args[2] + green + " section.");
//								} else {
//									s.sendMessage(red + "This rank exists already.");
//								}
//							} else {
//								s.sendMessage(red + "Invalid section.");
//							}
//						} else {
//							Editor(s, red + "Invalid arguments.");
//						}
//						break;
//	
//					case "rm":
//					case "remove": // /sm _0_remove _1_name _2_rank...
//	
//						if (args.length >= 3) {
//							if (isRegistered(args[1])) {
//								List<String> list = new ArrayList<String>();
//								for (int i = 2; i < args.length; i++) {
//									if (ranksAll.contains(args[i])) {
//										if (config.getStringList("staff." + args[1] + ".ranks").contains(args[i])) {
//	
//											list.add(args[i]);
//	
//											List<String> ranks = config.getStringList("staff." + args[1] + ".ranks");
//											ranks.removeAll(list);
//											config.set("staff." + args[1] + ".ranks", ranks);
//											saveConfig();
//	
//											Editor(s, green + "Removed " + darkG + args[i] + green + " rank from " + darkG + args[1] + green + ".");
//										} else {
//											s.sendMessage(red + "This player doesn't have " + darkR + args[i] + red + ".");
//										}
//									} else {
//										s.sendMessage(red + "This rank doesn't exist.");
//									}
//								}
//							} else {
//								s.sendMessage(red + "Invalid player name.");
//							}
//						} else {
//							Editor(s, red + "Invalid arguments.");
//						}
//	
//						break;
//	
//					case "d":
//					case "del":
//					case "delete": // /sm _0_delete _1_ranks
//	
//						if (args.length >= 2) {
//							for (int i = 1; i < args.length; i++) {
//								if (ranksAll.contains(args[i])) {
//	
//									List<String> list = new ArrayList<String>();
//	
//									for (String string : config.getConfigurationSection("ranks").getKeys(false)) {
//										if (config.getStringList("ranks." + string).contains(args[i])) {
//											list.addAll(config.getStringList("ranks." + string));
//											list.remove(args[i]);
//											ranksAll.remove(args[i]);
//											config.set("ranks." + string, list);
//											saveConfig();
//											Editor(s, green + "Deleted " + darkG + args[i] + green + " rank from " + darkG + string + green + ".");
//										}
//									}
//	
//									for (String string : config.getConfigurationSection("staff").getKeys(false)) {
//										if (config.getStringList("staff." + string + ".ranks").contains(args[1])) {
//	
//											List<String> lists = config.getStringList("staff." + string + ".ranks");
//											lists.remove(args[1]);
//											config.set("staff." + string + ".ranks", lists);
//											saveConfig();
//										}
//									}
//	
//								} else {
//									s.sendMessage(red + "This rank doesn't exist.");
//								}
//							}
//						} else {
//							Editor(s, red + "Invalid arguments.");
//						}
//						break;
//	
//					case "r":
//					case "register": // /sm _0_register _1_name _2_password <_3_ranks...>
//	
//						if (args.length >= 3) {
//							if (!config.getConfigurationSection("staff").contains(args[1])) {
//	
//								List<String> list = new ArrayList<String>();
//	
//								config.set("staff." + args[1] + ".password", args[2]);
//								config.createSection("staff." + args[1] + ".ranks");
//	
//								for (int i = 4; i < args.length; i++) {
//									list.add(args[i]);
//								}
//	
//								config.set("staff." + args[1] + ".ranks", list);
//								config.set("staff." + args[1] + ".logged", false);
//								if (getServer().getPlayer(args[1]).isOnline()) {
//									config.set("staff." + args[1] + ".online", true);
//								} else {
//									config.set("staff." + args[1] + ".online", false);
//								}
//	
//								Editor(s, green + "Registered " + darkG + args[1] + green + " successfully.");
//								saveConfig();
//							} else {
//								s.sendMessage(red + "This player is already registered.");
//							}
//						} else {
//							Editor(s, red + "Invalid arguments.");
//						}
//						break;
//	
//					case "u":
//					case "unregister": // /sm _0_unregister _1_name
//	
//						if (args.length == 2) {
//							if (config.getConfigurationSection("staff").contains(args[1])) {
//	
//								config.getConfigurationSection("staff").getKeys(false).forEach(string -> {
//									if (string.equals(args[1])) {
//										config.set("staff." + string, null);
//										saveConfig();
//									}
//								});
//	
//								Editor(s, green + "Unregistered " + darkG + args[1] + green + " successfully.");
//							} else {
//								s.sendMessage(red + "Invalid player name.");
//							}
//						} else {
//							Editor(s, red + "Invalid arguments.");
//						}
//						break;
//	
//					default:
//						Help(s);
//						return false;
//					}
//					return true;
//				}
//			} else {
//	
//				Player p = (Player) s;
//	
//				// checks for the arguments after /staffmode
//				if (args.length > 0) {
//					switch (args[0]) {
//	
//					case "h":
//					case "help":
//						Commands(p);
//						break;
//					case "g":
//					case "get": // /sm _0_get <_1_name>
//	
//						if (p.isOp()) {
//							if (args.length == 1) {
//								if (config.contains("staff." + p.getName())) {
//									if (config.getStringList("staff." + p.getName() + ".ranks").size() > 0) {
//										p.sendMessage(ChatColor.AQUA + "Your ranks: " + ChatColor.GOLD
//												+ config.getStringList("staff." + p.getName() + ".ranks").toString().replace("[", "").replace("]", ""));
//									} else {
//										Editor(p, red + "You have no ranks.", suggest, "/sm add " + p.getName(), show, "Add ranks?");
//									}
//								} else {
//									Editor(p, red + "You aren't registered.", suggest, "/sm register " + p.getName() + " password", show, "Click to register.");
//								}
//							} else if (args.length == 2) {
//								if (config.contains("staff." + args[1] + ".ranks") && config.getStringList("staff." + args[1] + ".ranks").size() > 0) {
//									if (args[1].endsWith("s")) {
//										p.sendMessage(ChatColor.AQUA + args[1] + "' ranks: " + ChatColor.GOLD
//												+ config.getStringList("staff." + args[1] + ".ranks").toString().replace("[", "").replace("]", ""));
//									} else {
//										p.sendMessage(ChatColor.AQUA + args[1] + "'s ranks: " + ChatColor.GOLD
//												+ config.getStringList("staff." + args[1] + ".ranks").toString().replace("[", "").replace("]", ""));
//									}
//								} else if (config.contains("staff." + args[1])) {
//									Editor(p, red + "This player has no ranks.", suggest, "/sm add ", show, "Add ranks?");
//								} else {
//									p.sendMessage(red + "Invalid player name.");
//								}
//							} else {
//								Editor(p, red + "Invalid arguments.", suggest, "/sm get ", show, "/sm get (player) (rank...)");
//							}
//						} else {
//							p.sendMessage(red + "You have no permission to execute this command.");
//						}
//						break;
//	
//					case "a":
//					case "add": // sm _0_add_ _1_name_ _2_ranks_
//	
//						if (p.isOp()) {
//							if (args.length >= 3) {
//								if (isRegistered(args[1])) {
//									List<String> list = new ArrayList<String>();
//									for (int i = 2; i < args.length; i++) {
//										if (ranksAll.contains(args[i])) {
//											if (!config.getStringList("staff." + args[1] + ".ranks").contains(args[i])) {
//	
//												list.add(args[i]);
//	
//												ArrayList<String> newList = new ArrayList<String>();
//	
//												for (String element : list) {
//													if (!newList.contains(element)) {
//														newList.add(element);
//													}
//												}
//												list = newList;
//	
//												Editor(p,
//														green + "Added " + darkG + args[i] + ChatColor.RESET + green + " to " + darkG + args[1] + ChatColor.RESET + green
//																+ ".",
//														suggest, "/sm remove " + args[1] + " " + args[i], show, "Remove " + ChatColor.GRAY + args[i] + ChatColor.RESET
//																+ " from " + ChatColor.GRAY + args[1] + ChatColor.RESET + " ?");
//											} else {
//												Editor(p, red + "This player already has " + darkR + args[i] + red + ".", suggest, "/sm add " + args[1] + " ", show,
//														"Add other rank?");
//											}
//										} else {
//											Editor(p, red + "This rank doesn't exist. Create it?", suggest, "/sm create " + args[i] + " ", show,
//													net.md_5.bungee.api.ChatColor.RESET + "/sm create " + ChatColor.GRAY + "(rank) (section)");
//										}
//									}
//	
//									List<String> ranks = config.getStringList("staff." + args[1] + ".ranks");
//									ranks.addAll(list);
//									config.set("staff." + args[1] + ".ranks", ranks);
//									saveConfig();
//								} else {
//									Editor(p, red + "This player isn't registered.", suggest, "/sm register " + args[1], show,
//											"/sm register (player) (password) <ranks...>");
//								}
//							} else {
//								Editor(p, red + "Invalid arguments.", suggest, "/sm add ", show, "/sm add (player) (rank...)");
//							}
//						} else {
//							p.sendMessage(red + "You have no permission to execute this command.");
//						}
//						break;
//	
//					case "c":
//					case "create": // /sm _0_create _1_rank _2_section
//	
//						if (p.isOp()) {
//							if (args.length == 3) {
//								if (config.getConfigurationSection("ranks").getKeys(false).contains(args[2])) {
//									if (!ranksAll.contains(args[1])) {
//	
//										List<String> list = new ArrayList<String>();
//										list.addAll(config.getStringList("ranks." + args[2]));
//										list.add(args[1]);
//										ranksAll.add(args[1]);
//										config.set("ranks." + args[2], list);
//										saveConfig();
//										Editor(p, green + "Created " + darkG + args[1] + green + " rank under the " + darkG + args[2] + green + " section.", suggest,
//												"/sm delete " + args[1], show, ChatColor.RESET + "Delete " + ChatColor.GRAY + args[1] + ChatColor.RESET + "?");
//									} else {
//										p.sendMessage(red + "This rank exists already.");
//									}
//								} else {
//									p.sendMessage(red + "Invalid section.");
//								}
//							} else {
//								Editor(p, red + "Invalid arguments.", suggest, "/sm create ", show, "/sm create (rank) (section)");
//							}
//						} else {
//							p.sendMessage(red + "You have no permission to execute this command.");
//						}
//						break;
//	
//					case "rm":
//					case "remove": // /sm _0_remove _1_name _2_rank...
//	
//						if (p.isOp()) {
//							if (args.length >= 3) {
//								if (isRegistered(args[1])) {
//									List<String> list = new ArrayList<String>();
//									for (int i = 2; i < args.length; i++) {
//										if (ranksAll.contains(args[i])) {
//											if (config.getStringList("staff." + args[1] + ".ranks").contains(args[i])) {
//	
//												list.add(args[i]);
//	
//												List<String> ranks = config.getStringList("staff." + args[1] + ".ranks");
//												ranks.removeAll(list);
//												config.set("staff." + args[1] + ".ranks", ranks);
//												saveConfig();
//	
//												Editor(p, green + "Removed " + darkG + args[i] + green + " rank from " + darkG + args[1] + green + ".", suggest,
//														"/sm add " + args[1] + " " + args[i], show, "Add " + ChatColor.GRAY + args[i] + ChatColor.RESET + " to "
//																+ ChatColor.GRAY + args[1] + ChatColor.RESET + " back?");
//											} else {
//												p.sendMessage(red + "This player doesn't have " + darkR + args[i] + red + ".");
//											}
//										} else {
//											p.sendMessage(red + "This rank doesn't exist.");
//										}
//									}
//								} else {
//									p.sendMessage(red + "Invalid player name.");
//								}
//							} else {
//								Editor(p, red + "Invalid arguments.", suggest, "/sm remove ", show, "/sm remove (player) (ranks)");
//							}
//						} else {
//							p.sendMessage(red + "You have no permission to execute this command.");
//						}
//	
//						break;
//	
//					case "d":
//					case "del":
//					case "delete": // /sm _0_delete _1_ranks
//	
//						if (p.isOp()) {
//							if (args.length >= 2) {
//								for (int i = 1; i < args.length; i++) {
//									if (ranksAll.contains(args[i])) {
//	
//										List<String> list = new ArrayList<String>();
//	
//										for (String string : config.getConfigurationSection("ranks").getKeys(false)) {
//											if (config.getStringList("ranks." + string).contains(args[i])) {
//												list.addAll(config.getStringList("ranks." + string));
//												list.remove(args[i]);
//												ranksAll.remove(args[i]);
//												config.set("ranks." + string, list);
//												saveConfig();
//												Editor(p, green + "Deleted " + darkG + args[i] + green + " rank from " + darkG + string + green + ".", suggest,
//														"/sm create " + args[i] + " " + string, show, ChatColor.RESET + "Create " + ChatColor.GRAY + args[i]
//																+ ChatColor.RESET + " in " + ChatColor.GRAY + string + ChatColor.RESET + " section back?");
//											}
//										}
//	
//										for (String string : config.getConfigurationSection("staff").getKeys(false)) {
//											if (config.getStringList("staff." + string + ".ranks").contains(args[1])) {
//	
//												List<String> lists = config.getStringList("staff." + string + ".ranks");
//												lists.remove(args[1]);
//												config.set("staff." + string + ".ranks", lists);
//												saveConfig();
//											}
//										}
//	
//									} else {
//										p.sendMessage(red + "This rank doesn't exist.");
//									}
//								}
//							} else {
//								Editor(p, red + "Invalid arguments.", suggest, "/sm delete ", show, "/sm delete (rank) (section)");
//							}
//						} else {
//							p.sendMessage(red + "You have no permission to execute this command.");
//						}
//						break;
//	
//					case "l":
//					case "login": // /sm _0_login _1_password
//	
//						if (config.getConfigurationSection("staff").contains(p.getName())) {
//							if (args.length == 2) {
//								if (!config.getBoolean("staff." + p.getName() + ".logged")) {
//									if (config.getString("staff." + p.getName() + ".password").equals(args[1])) {
//	
//										addPlayerRanks(p);
//										config.set("staff." + p.getName() + ".logged", true);
//	
//										saveConfig();
//	
//										p.sendMessage(green + "Logged in successfully.");
//									} else {
//										Editor(p, red + "Invalid password.", suggest, "/sm login ", show, net.md_5.bungee.api.ChatColor.RESET + "Try again?");
//									}
//								} else {
//									p.sendMessage(red + "You are already logged in.");
//								}
//							} else {
//								Editor(p, red + "Invalid arguments.", suggest, "/sm login ", show, "/sm login (password)");
//							}
//						} else {
//							p.sendMessage(red + "You aren't registered.");
//						}
//						break;
//	
//					case "r":
//					case "register": // /sm _0_register _1_name _2_password <_3_ranks...>
//	
//						if (s.isOp()) {
//							if (args.length >= 3) {
//								if (!config.getConfigurationSection("staff").contains(args[1])) {
//	
//									List<String> list = new ArrayList<String>();
//	
//									config.set("staff." + args[1] + ".password", args[2]);
//									config.createSection("staff." + args[1] + ".ranks");
//	
//									for (int i = 4; i < args.length; i++) {
//										list.add(args[i]);
//									}
//	
//									config.set("staff." + args[1] + ".ranks", list);
//									config.set("staff." + args[1] + ".logged", false);
//									if (getServer().getPlayer(args[1]).isOnline()) {
//										config.set("staff." + args[1] + ".online", true);
//									} else {
//										config.set("staff." + args[1] + ".online", false);
//									}
//	
//									Editor(p, green + "Registered " + darkG + args[1] + green + " successfully.", suggest, "/sm unregister " + args[1], show,
//											"Wrong person? Click me!");
//									saveConfig();
//								} else {
//									p.sendMessage(red + "This player is already registered.");
//								}
//							} else {
//								Editor(p, red + "Invalid arguments.", suggest, "/sm register ", show, "/sm register (player) <ranks...>");
//							}
//						} else {
//							p.sendMessage(red + "You have no permission to execute this command.");
//						}
//						break;
//	
//					case "u":
//					case "unregister": // /sm _0_unregister _1_name
//	
//						if (s.isOp()) {
//							if (args.length == 2) {
//								if (config.getConfigurationSection("staff").contains(args[1])) {
//	
//									config.getConfigurationSection("staff").getKeys(false).forEach(string -> {
//										if (string.equals(args[1])) {
//											config.set("staff." + string, null);
//											saveConfig();
//										}
//									});
//	
//									Editor(p, green + "Unregistered " + darkG + args[1] + green + " successfully.", suggest, "/sm register " + args[1], show,
//											"Register " + net.md_5.bungee.api.ChatColor.GRAY + args[1] + net.md_5.bungee.api.ChatColor.RESET + " back?");
//								} else {
//									p.sendMessage(red + "Invalid player name.");
//								}
//							} else {
//								Editor(p, red + "Invalid arguments.", suggest, "/sm unregister ", show, "/sm unregister (player)");
//							}
//						} else {
//							p.sendMessage(red + "You have no permission to execute this command.");
//						}
//						break;
//	
//					case "p":
//					case "pass":
//					case "password": // /sm _0_password _1_old _2_new
//	
//						if (args.length == 3) {
//							if (config.getConfigurationSection("staff").contains(p.getName())) {
//								if (config.getBoolean("staff." + p.getName() + ".logged")) {
//									if (config.getString("staff." + p.getName() + ".password").equals(args[1])) {
//	
//										config.set("staff." + p.getName() + ".password", args[2]);
//										saveConfig();
//										Editor(p, green + "Password changed successfully.", suggest, "/sm password " + args[2] + " ", show, "Change again?");
//	
//									} else {
//										p.sendMessage(red + "Invalid password.");
//									}
//								} else {
//									Editor(p, red + "You aren't logged in.", suggest, "/sm login ", show, "Login?");
//								}
//							} else {
//								p.sendMessage(red + "You aren't registered.");
//							}
//						} else {
//							Editor(p, red + "Invalid arguments.", suggest, "/sm password ", show, "/sm password (old) (new)");
//						}
//						break;
//	
//					default:
//						Help(p);
//						return false;
//					}
//					return true;
//				}
//	
//				Help(p);
//			}
//			return false;
//	
//		}
//	
//		@Override
//		public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
//	
//			ArrayList<String> list = new ArrayList<String>();
//			ArrayList<String> commands = new ArrayList<String>();
//			commands.add("login");
//			commands.add("unregister");
//			commands.add("register");
//			commands.add("password");
//			commands.add("get");
//			commands.add("add");
//			commands.add("remove");
//			commands.add("create");
//			commands.add("delete");
//	
//			try {
//	
//				if (sender.isOp()) {
//	
//					if (args[0].equals("")) {
//						list.addAll(commands);
//						return list;
//					}
//	
//					if (args.length == 1) {
//						commands.forEach(string -> {
//							if (string.toLowerCase().startsWith(args[0].toLowerCase())) {
//								list.add(string);
//							}
//						});
//						return list;
//					}
//	
//					switch (args[0]) {
//	
//					case "g":
//					case "get": // /sm _0_get <_1_name>
//	
//						if (args[1].equals("")) {
//							config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
//							return list;
//						}
//	
//						if (args.length == 2) {
//							config.getConfigurationSection("staff").getKeys(false).forEach(string -> {
//								if (string.toLowerCase().startsWith(args[1].toLowerCase())) {
//									list.add(string);
//								}
//							});
//							return list;
//						}
//	
//						if (!args[2].equals("")) {
//							list.clear();
//							return list;
//						}
//						break;
//	
//					case "a":
//					case "add": // /sm _0_add _1_name _2_ranks...
//	
//						if (args[1].equals("")) {
//							config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
//							return list;
//						}
//	
//						if (args.length == 2) {
//							config.getConfigurationSection("staff").getKeys(false).forEach(string -> {
//								if (string.toLowerCase().startsWith(args[1].toLowerCase())) {
//									list.add(string);
//								}
//							});
//							return list;
//						}
//	
//						if (args[2].equals("")) {
//							List<String> staff = config.getStringList("ranks.staff");
//							List<String> regular = config.getStringList("ranks.regular");
//							list.addAll(staff);
//							list.addAll(regular);
//	
//							for (String string : config.getStringList("staff." + args[1] + ".ranks")) {
//								if (list.contains(string)) {
//									list.remove(string);
//								}
//							}
//	
//							return list;
//						}
//	
//						if (args.length >= 3) {
//							ranksAll.forEach(string -> {
//								if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
//									list.add(string);
//									for (String s : config.getStringList("staff." + args[1] + ".ranks")) {
//										if (list.contains(s)) {
//											list.remove(s);
//										}
//									}
//								}
//							});
//	
//							for (int i = 0; i < args.length; i++) {
//								if (list.contains(args[i])) {
//									list.remove(args[i]);
//								}
//							}
//							return list;
//						}
//						break;
//	
//					case "c":
//					case "create": // /sm _0_create _1_rank _2_section
//	
//						if (args[1].equals("")) {
//							list.clear();
//							return list;
//						}
//	
//						if (args[2].equals("")) {
//							config.getConfigurationSection("ranks").getKeys(false).forEach(string -> list.add(string));
//							return list;
//						}
//	
//						if (!args[2].equals("")) {
//							list.clear();
//							return list;
//						}
//						break;
//	
//					case "rm":
//					case "remove": // /sm _0_remove _1_name _2_ranks...
//	
//						if (args[1].equals("")) {
//							config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
//							return list;
//						}
//	
//						if (args[2].equals("")) {
//							config.getStringList("staff." + args[1] + ".ranks").forEach(string -> list.add(string));
//							return list;
//						}
//	
//						if (args.length >= 3) {
//							config.getStringList("staff." + args[1] + ".ranks").forEach(string -> {
//								if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
//									list.add(string);
//								}
//							});
//	
//							for (int i = 0; i < args.length; i++) {
//								if (list.contains(args[i])) {
//									list.remove(args[i]);
//								}
//							}
//							return list;
//						}
//						break;
//	
//					case "d":
//					case "del":
//					case "delete": // /sm _0_delete _1_ranks...
//	
//						if (args[1].equals("")) {
//							list.addAll(ranksAll);
//							return list;
//						}
//	
//						if (args.length >= 2) {
//							ranksAll.forEach(string -> {
//								if (string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
//									list.add(string);
//								}
//							});
//							return list;
//						}
//						break;
//	
//					case "l":
//					case "login": // /sm _0_login _1_password
//	
//						list.clear();
//						return list;
//	
//					case "r":
//					case "register": // /sm _0_register _1_name _2_password <_3_ranks..>
//	
//						if (args[1].equals("")) {
//							return null;
//						}
//	
//						if (!args[2].equals("")) {
//							list.clear();
//							return list;
//						}
//						break;
//	
//					case "u":
//					case "unregister": // /sm _0_unregister _1_name
//	
//						if (args[1].equals("")) {
//							config.getConfigurationSection("staff").getKeys(false).forEach(string -> list.add(string));
//							return list;
//						}
//	
//						if (!args[2].equals("")) {
//							list.clear();
//							return list;
//						}
//						break;
//	
//					case "p":
//					case "pass":
//					case "password": // /sm _0_password _1_old _2_new
//	
//						list.clear();
//						return list;
//					}
//				} else {
//	
//					commands.clear();
//					if (!config.getBoolean("staff." + sender.getName() + ".logged")) {
//						commands.add("login");
//					} else {
//						commands.add("password");
//					}
//	
//					if (sender instanceof Player) {
//	
//						if (config.getConfigurationSection("staff").contains(sender.getName())) {
//							if (args[0].equals("")) {
//								return commands;
//							}
//	
//							if (args.length == 1) {
//								commands.forEach(string -> {
//									if (string.toLowerCase().startsWith(args[0].toLowerCase())) {
//										list.add(string);
//									}
//								});
//							}
//	
//							return list;
//						}
//					}
//	
//					list.clear();
//					return list;
//				}
//			} catch (ArrayIndexOutOfBoundsException e) {
//	
//			}
//	
//			return null;
//		}
//	
//		private void Editor(Player player, String message1, Action clickAction1, String actionValue1, net.md_5.bungee.api.chat.HoverEvent.Action hoverAction1,
//				String hoverValue1) {
//	
//			TextComponent tc1 = new TextComponent();
//			tc1.setText(message1);
//			tc1.setClickEvent(new ClickEvent(clickAction1, actionValue1));
//			tc1.setHoverEvent(new HoverEvent(hoverAction1, new ComponentBuilder(hoverValue1).create()));
//	
//			player.spigot().sendMessage(tc1);
//		}
//	
//		private void Commands(Player player) {
//	
//			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Staffmode commands:");
//			Editor(player, "- login", suggest, "/sm login ", show,
//					net.md_5.bungee.api.ChatColor.RESET + "/staffmode login " + net.md_5.bungee.api.ChatColor.GRAY + "(password)");
//			Editor(player, "- password", suggest, "/sm password ", show,
//					net.md_5.bungee.api.ChatColor.RESET + "/staffmode password " + net.md_5.bungee.api.ChatColor.GRAY + "(old) (new)");
//	
//			if (player.isOp()) {
//	
//				Editor(player, "- register", suggest, "/sm register ", show,
//						net.md_5.bungee.api.ChatColor.RESET + "/staffmode register " + net.md_5.bungee.api.ChatColor.GRAY + "(name) (password) <ranks>");
//				Editor(player, "- unregister", suggest, "/sm unregister ", show,
//						net.md_5.bungee.api.ChatColor.RESET + "/staffmode unregister " + net.md_5.bungee.api.ChatColor.GRAY + "(name)");
//				Editor(player, "- get", suggest, "/sm get ", show,
//						net.md_5.bungee.api.ChatColor.RESET + "/staffmode get " + net.md_5.bungee.api.ChatColor.DARK_GRAY + "(name)");
//				Editor(player, "- add", suggest, "/sm add ", show,
//						net.md_5.bungee.api.ChatColor.RESET + "/staffmode add " + net.md_5.bungee.api.ChatColor.GRAY + "(name) (rank)");
//				Editor(player, "- remove", suggest, "/sm remove ", show,
//						net.md_5.bungee.api.ChatColor.RESET + "/staffmode remove " + net.md_5.bungee.api.ChatColor.GRAY + "(name) (rank)");
//				Editor(player, "- create", suggest, "/sm create ", show,
//						net.md_5.bungee.api.ChatColor.RESET + "/staffmode create " + ChatColor.GRAY + "(rank) (section)");
//				Editor(player, "- delete", suggest, "/sm delete ", show, net.md_5.bungee.api.ChatColor.RESET + "/staffmode delete " + ChatColor.GRAY + "(rank)");
//			}
//		}
//	
//		private void Help(Player player) {
//			Editor(player, red + "Invalid command.", Action.RUN_COMMAND, "/sm help", show, "Click for help.");
//		}
//	
//		private void Editor(CommandSender s, String message) {
//			s.sendMessage(message);
//		}
//	
//		private void Commands(CommandSender s) {
//	
//			s.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Staffmode commands:");
//			Editor(s, "/staffmode login " + ChatColor.GRAY + "(password)");
//			Editor(s, "/staffmode password " + ChatColor.GRAY + "(old) (new)");
//	
//			if (s.isOp()) {
//	
//				Editor(s, "/staffmode register " + ChatColor.GRAY + "(name) (password) <ranks>");
//				Editor(s, "/staffmode unregister " + ChatColor.GRAY + "(name)");
//				Editor(s, "/staffmode get " + ChatColor.DARK_GRAY + "(name)");
//				Editor(s, "/staffmode add " + ChatColor.GRAY + "(name) (rank)");
//				Editor(s, "/staffmode remove " + ChatColor.GRAY + "(name) (rank)");
//				Editor(s, "/staffmode create " + ChatColor.GRAY + "(rank) (section)");
//				Editor(s, "/staffmode delete " + ChatColor.GRAY + "(rank)");
//			}
//		}
//	
//		private void Help(CommandSender s) {
//			Editor(s, red + "Invalid command.");
//		}
//	}
