package me.atomiz.hubheads;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import me.atomiz.hubheads.Main.Logger;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CommandTrait;
import net.citizensnpcs.trait.CommandTrait.Hand;
import net.citizensnpcs.trait.CommandTrait.NPCCommandBuilder;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;

class Commands implements TabExecutor {

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {

		if (args.length > 0) {
			if (s.hasPermission("hubheads.use"))
				switch (args[0]) {

				case "start": // /hh start <player>
					if (s instanceof Player) {
						Player p = (Player) s;
						try {
							if (args.length == 1)
								Main.StartEvent(p, false);
							else if (s.getServer().getPlayer(args[1]).isOnline()) {
								Player player = s.getServer().getPlayer(args[1]);
								if (args.length == 2) {
									if (Helpers.checkPlayerData(player, false, false))
										Main.StartEvent(player, false);
									else if (Helpers.checkPlayerData(player, false, true))
										p.sendMessage(ChatColor.RED + "This player has a delay " + ChatColor.DARK_RED
												+ Helpers.getEventRemainingDelay(player) + ChatColor.RED + ". Use "
												+ ChatColor.DARK_RED + "/hh start " + args[1] + " force" + ChatColor.RED
												+ " to force the event for this player");
									else if (Helpers.checkPlayerData(player, true, false))
										p.sendMessage(ChatColor.RED + "The event is already running for this player");
									else
										s.sendMessage(ChatColor.RED + "Well that's odd...can you please report this message");
								} else if (args.length == 3) {
									if (args[2].equals("force")) {
										Main.onlinePlayerDelay.put(player.getUniqueId(), -1L);
										Main.StartEvent(player, false);
									} else
										s.sendMessage(ChatColor.RED + "Invalid command");
								} else
									Helpers.Editor(p,
											ChatColor.RED + "Invalid arguments." + ChatColor.GOLD + " Usage: " + ChatColor.AQUA
													+ "/hh start <player>",
											Action.SHOW_TEXT, ChatColor.GREEN + "Click to insert.",
											ClickEvent.Action.SUGGEST_COMMAND, "/hh start ");
							}
						} catch (NullPointerException ex) {
							p.sendMessage(ChatColor.RED + "This player is not online");
							ex.printStackTrace();
						}
					} else
						try {
							if (args.length > 1 && s.getServer().getPlayer(args[1]).isOnline()) {
								Player player = s.getServer().getPlayer(args[1]);
								if (args.length == 2) {
									if (Helpers.checkPlayerData(player, false, false))
										Main.StartEvent(player, false);
									else if (Helpers.checkPlayerData(player, false, true)) // the player has delay
										Main.StartEvent(player, false);
									else if (Helpers.checkPlayerData(player, true, false)) // the event is running
										player.sendMessage(ChatColor.GREEN + "You have " + ChatColor.BOLD
												+ (Main.headsGoal - Main.onlinePlayerHeads.get(player.getUniqueId()))
												+ ChatColor.RESET + ChatColor.GREEN + " more heads to find!");
									else
										s.sendMessage(ChatColor.RED + "Well that's odd...can you please report this message");
								} else if (args.length == 3)
									if (args[2].equals("force")) {
										Main.onlinePlayerDelay.put(player.getUniqueId(), -1L);
										Main.StartEvent(player, true);
									} else
										s.sendMessage(ChatColor.RED + "Invalid command");
							} else
								s.sendMessage(ChatColor.RED + "Invalid arguments." + ChatColor.GOLD + " Usage: " + ChatColor.AQUA
										+ "/hh start (player)");
						} catch (NullPointerException ex) { // player is invalid
							s.sendMessage(ChatColor.RED + "This player is not online");
						}
					return true;

				case "npc": // /hh npc
					if (Main.citizens)
						if (s instanceof Player) {
							Player p = (Player) s;
							NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "HubHeads");

							SkinTrait skin = npc.getTrait(SkinTrait.class); // adds SkinTrait trait automatically
							skin.setSkinPersistent("Alloushghost",
									"kPzV18Er13p+oRbRN69FdEbKvC5wBsRAhqgZ9cw83TrjiNVLBseSr6wGXMxzmhsyGeOdiNkaf9pjuFbGI52vL2nCNvSI60Nqfms3AjBIXSvaIdSeJ67I3bl/5geI7U/VO3hEKE89WXL0XCXncq9OVwFmZkuQwFYXOSHxDzuwJsqlGIZpM4qa/x9Vu3rGojyxGzh9Q58f8iTQhpK+mJt3VrGzsc2wkmj27DKX36Oit/ZJgDpIn8NziE/ZNKB1Tjj+BkOUsHHK1s6qB+cFkK5RhTI6UBkq0QyqGJsLWTPN4DaXp7dIO3kRbhfqPDu4Gkphvx49f3PYSMJwA2ZI7Q0ndNVSlEDm82m2iGQ/yRKn02nGUz5YByDlC3bl3miuVU23w8loJqGk6X2CM70VXq2wpFB0oFEZUqwI7eHbiWtjnV536i7LJD2bwDT63HmS3RcnLV+/wYlysT+SSc9LzQSdz6+cCi/Saq0j4l47bVaKs6IpbfpMsCk+rRL9hbK+bIzi8JrhnVe0YaWwqLSsH4bL34MclU3GrS5KGm3MkwO1x6YoMbx4BLZdxmEAIpccjLZYzv4itYfp3RShpLSZHK3BFsJWF7vwLDKJmgmY2a70WEGwyu//ZoVDYPNydEr82Hb/SdVbwRmCnaF0Nuf04x5BNAL6I2npKjs5KIXZxzLNuR8=",
									"eyJ0aW1lc3RhbXAiOjE1ODc5MTY5ODA3MzcsInByb2ZpbGVJZCI6IjIxZTNjYzBmYjZhNDQ5NTFiMTNhNDY2YzFlZWY3NzkyIiwicHJvZmlsZU5hbWUiOiJBbGxvdXNoZ2hvc3QiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzczNjlhYjU0YzNmY2Q5YzkxMTE2ZTdmNjBlNDU1ZTYwMDNkY2M2OTQ0Njg2YjcyNGM1N2ZlMDZkODNlMjk1YzIifX19");

							CommandTrait command = npc.getTrait(CommandTrait.class);
							command.addCommand(new NPCCommandBuilder("hh start <p>", Hand.RIGHT).player(false));

							LookClose look = npc.getTrait(LookClose.class);
							look.lookClose(true);

							npc.data().setPersistent(NPC.NAMEPLATE_VISIBLE_METADATA, false);
							npc.spawn(p.getLocation());
						} else
							s.sendMessage(ChatColor.RED + "You have to be a player to execute this command");
					else
						s.sendMessage(ChatColor.RED + "Citizens is not enabled");
					return true;

				case "kill": // /hh kill
					if (Main.presents.size() > 0)
						for (Entity entity : Main.presents) {
							entity.remove();
							Main.busyLocations.remove(entity.getLocation());
						}
					Main.presents.clear();
					for (Player p : Bukkit.getServer().getOnlinePlayers())
						if (Helpers.checkPlayerData(p, true, false))
							Main.StartEvent(p, false);
					return true;

				case "unsafekill": // /hh unsafekill rX rY rZ hasGravity isInvulnerable isVisible hasHelmet hasName
					if (s instanceof Player) {

						Player p = (Player) s;
						if (p.getGameMode().equals(GameMode.SPECTATOR))
							if (p.getSpectatorTarget() != null)
								if (!p.getSpectatorTarget().getType().equals(EntityType.PLAYER))
									p.getSpectatorTarget().remove();
								else
									p.sendMessage(ChatColor.GOLD + "XD dont try to remove players lmao");
							else
								p.sendMessage(ChatColor.RED + "You need to be in an entity to do that");
						else
							p.sendMessage(ChatColor.RED + "You need to be in spectator mode to do that");

					} else if (args.length < 2)
						Helpers.pr(ChatColor.RED + "This will removed all possible presents in the world "
								+ "(all armorstands with no gravity, invisible, invulnerable, with custom name \"present\" and a player head). "
								+ "To confirm this action type " + ChatColor.DARK_RED + "/hh unsafekill confirm", Logger.INFO);
					else if (args[1].equals("confirm")) {

						Helpers.pr(ChatColor.RED + "Removing all possible presents", Logger.INFO);
						for (Entity entity : Bukkit.getServer().getWorld(Main.hub).getEntities())
							if (entity.getType().equals(EntityType.ARMOR_STAND) && ((ArmorStand) entity).hasGravity() == false
									&& ((ArmorStand) entity).isInvulnerable() == true
									&& ((ArmorStand) entity).isVisible() == false
									&& ((ArmorStand) entity).getHelmet().getType().equals(Material.PLAYER_HEAD))
								entity.remove();

						for (Player p : Bukkit.getServer().getOnlinePlayers())
							if (Helpers.checkPlayerData(p, true, false))
								Main.StartEvent(p, false);
					}
					return true;

				case "data": // /hh data
					if (s instanceof Player) {
						Player p = (Player) s;
						p.sendMessage(ChatColor.LIGHT_PURPLE + "heads: " + Main.onlinePlayerHeads.get(p.getUniqueId()));
						p.sendMessage(ChatColor.DARK_PURPLE + "delay: " + Main.onlinePlayerDelay.get(p.getUniqueId()));
						p.sendMessage(ChatColor.LIGHT_PURPLE + "timeout: " + Main.onlinePlayerTimeout.get(p.getUniqueId()));
					} else if (args.length == 2) {
						Player p = s.getServer().getPlayer(args[1]);
						s.sendMessage(ChatColor.LIGHT_PURPLE + "heads: " + Main.onlinePlayerHeads.get(p.getUniqueId()));
						s.sendMessage(ChatColor.DARK_PURPLE + "delay: " + Main.onlinePlayerDelay.get(p.getUniqueId()));
						s.sendMessage(ChatColor.LIGHT_PURPLE + "timeout: " + Main.onlinePlayerTimeout.get(p.getUniqueId()));
					} else
						s.sendMessage(ChatColor.RED + "You have to be a player to execute this command");
					return true;

				case "test": // /hh test
					Helpers.updateHolograms((Player) s);
					return true;

				case "show": // /hh show variable
					if (args.length == 2) {
						List<Object> list = new ArrayList<>();
						list.add(Main.busyLocations.size());
						list.add(Main.availableLocations.size());
						list.add(Main.validHeadsBlocks.size() + ": " + Main.validHeadsBlocks);
						list.add(Main.presentsSkinsValues);
						list.add(Main.presents.size());
						list.add(Main.onlinePlayerHeads.entrySet());
						list.add(Main.onlinePlayerDelay.entrySet());
						list.add(Main.onlinePlayerTimeout.entrySet());
						list.add(Main.npcs.size());
						s.sendMessage(list.get(Integer.parseInt(args[1])).toString().replace("[", "").replace("]", "")
								.replace(",", "\n"));
					}
					return true;

				case "debug": // /hh debug
					Main.debug = !Main.debug;
					if (Main.debug)
						Helpers.pr(ChatColor.GREEN + "Debug mode enabled", Logger.INFO);
					else
						Helpers.pr(ChatColor.RED + "Debug mode disabled", Logger.INFO);

					return true;

				case "scan": // /hh scan
					if (args.length == 2 && args[1].equals("confirm")) {
						s.sendMessage(ChatColor.GREEN + "Rescanning the world. This may take some time.");
						Main.availableLocations.clear();
						Main.headsUnableToSpawn = 0;
						Helpers.scanWorldBlocks(s);
						return true;
					}
					if (s instanceof Player)
						s.sendMessage(ChatColor.RED + "Rescanning the world may cause " + ChatColor.DARK_RED + "lag"
								+ ChatColor.RED + "! All presents will be " + ChatColor.DARK_RED + "removed " + ChatColor.RED
								+ "for safety! Type " + ChatColor.DARK_RED + "/hh scan confirm" + ChatColor.RED
								+ " to confirm the scan.");
					else
						Helpers.pr(ChatColor.RED + "Rescanning the world may cause " + ChatColor.DARK_RED + "lag" + ChatColor.RED
								+ "! All presents will be " + ChatColor.DARK_RED + "removed " + ChatColor.RED
								+ "for safety! Type " + ChatColor.DARK_RED + "/hh scan confirm" + ChatColor.RED
								+ " to confirm the scan.", Logger.INFO);
					return true;

				case "reload": // /hh reload
					Main.main().reloadConfig();
					Main.config = Main.main().getConfig();
					Main.loadConfig();
					for (Player p : s.getServer().getOnlinePlayers())
						if (Helpers.checkPlayerData(p, true, false))
							Main.StartEvent(p, false);
					if (s instanceof Player)
						s.sendMessage(ChatColor.GREEN + "Config reloaded");
					else
						Helpers.pr(ChatColor.GREEN + "Config reloaded", Logger.INFO);
					return true;

				case "help": // /hh help
					if (s instanceof Player) {
						Action show = Action.SHOW_TEXT;
						ClickEvent.Action run = ClickEvent.Action.RUN_COMMAND;
						ClickEvent.Action suggest = ClickEvent.Action.SUGGEST_COMMAND;

						s.sendMessage(ChatColor.GOLD + "Commands:");
						if (s.hasPermission("hubheads.use")) {
							Player p = (Player) s;
							Helpers.Editor(p,
									ChatColor.DARK_AQUA + "start" + ChatColor.GOLD + " - " + ChatColor.AQUA
											+ "Starts the event for a player",
									show, ChatColor.GREEN + "/hh start <player>", suggest, "/hh start ");
							Helpers.Editor(p,
									ChatColor.DARK_AQUA + "debug" + ChatColor.GOLD + " - " + ChatColor.AQUA
											+ "Helpful when checking an issue",
									show, ChatColor.GREEN + "/hh debug", run, "/hh debug");
							Helpers.Editor(p,
									ChatColor.DARK_AQUA + "kill" + ChatColor.GOLD + " - " + ChatColor.AQUA
											+ "Kills and respawns the heads",
									show, ChatColor.GREEN + "/hh kill", run, "/hh kill");
							Helpers.Editor(p,
									ChatColor.DARK_AQUA + "unsafekill" + ChatColor.GOLD + " - " + ChatColor.AQUA
											+ "Removes the entity you are in while in spectator mode",
									show, ChatColor.GREEN + "/hh unsafekill", suggest, "/hh unsafekill");
							Helpers.Editor(p,
									ChatColor.DARK_AQUA + "npc" + ChatColor.GOLD + " - " + ChatColor.AQUA + "Spawns the npc",
									show, ChatColor.GREEN + "/hh npc", run, "/hh npc");
							Helpers.Editor(p,
									ChatColor.DARK_AQUA + "scan" + ChatColor.GOLD + " - " + ChatColor.AQUA
											+ "Rescans the world. You would need that in case of any world modification",
									show, ChatColor.GREEN + "/hh scan", run, "/hh scan");
							Helpers.Editor(p, ChatColor.DARK_AQUA + "reload" + ChatColor.GOLD + " - " + ChatColor.AQUA
									+ "Reloads the config", show, ChatColor.GREEN + "/hh reload", run, "/hh reload");
						}
					} else {
						s.sendMessage(ChatColor.DARK_AQUA + "start <player>" + ChatColor.GOLD + " - " + ChatColor.AQUA
								+ "Starts the event for a player");
						s.sendMessage(ChatColor.DARK_AQUA + "debug" + ChatColor.GOLD + " - " + ChatColor.AQUA
								+ "Helpful when checking an issue");
						s.sendMessage(ChatColor.DARK_AQUA + "kill" + ChatColor.GOLD + " - " + ChatColor.AQUA
								+ "Kills and respawns the heads");
						s.sendMessage(ChatColor.DARK_AQUA
								+ "unsafekill <rX> <rY> <rZ> <hasGravity> <isInvulnerable> <isVisible> <hasHelmet> <hasName>"
								+ ChatColor.GOLD + " - " + ChatColor.AQUA + "Kills specified entites that could be presents");
						s.sendMessage(ChatColor.DARK_AQUA + "npc" + ChatColor.GOLD + " - " + ChatColor.AQUA + "Spawns the npc");
						s.sendMessage(ChatColor.DARK_AQUA + "scan" + ChatColor.GOLD + " - " + ChatColor.AQUA
								+ "Rescans the world. You would need that in case of any world modification");
						s.sendMessage(
								ChatColor.DARK_AQUA + "reload" + ChatColor.GOLD + " - " + ChatColor.AQUA + "Reloads the config");
					}
					return true;
				default:
					s.sendMessage(
							ChatColor.RED + "Invalid command. " + ChatColor.DARK_RED + "/hh help" + ChatColor.RED + " for help");
					return true;
				}
			s.sendMessage(ChatColor.RED + "Sorry but you can not execute this command");
			return true;
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender s, Command command, String alias, String[] args) {

		if (s.hasPermission("hubheads.use")) {
			List<String> list = new ArrayList<>();
			List<String> commands = new ArrayList<>();
			commands.add("start");
			commands.add("debug");
			commands.add("kill");
			commands.add("scan");
			commands.add("reload");
			commands.add("npc");
			commands.add("unsafekill");

			try {

				if (args[0].equals(""))
					return commands;

				if (args.length == 1) {
					commands.forEach(string -> {
						if (string.toLowerCase().startsWith(args[0].toLowerCase()))
							list.add(string);
					});
					return list;
				}

				if (args[0].equals("start")) {
					if (args[1].equals("")) {
						for (Player p : s.getServer().getOnlinePlayers())
							if (p.getWorld().getName().equals(Main.hub))
								if (Helpers.checkPlayerData(p, false, true))
									list.add(p.getName());
						return list;
					}

					if (args.length == 2) {
						for (Player p : s.getServer().getOnlinePlayers())
							if (p.getWorld().getName().equals(Main.hub))
								if (p.getName().toLowerCase().startsWith(args[1].toLowerCase()))
									list.add(p.getName());
						return list;
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {}
			return list;
		}
		return null;
	}
}
