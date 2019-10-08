package me.atomiz.wg_nbs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.session.SessionManager;
import com.xxmicloxx.NoteBlockAPI.NoteBlockAPI;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;

import me.atomiz.wg_nbs.Meta.MetaDataManager;
import me.atomiz.wg_nbs.Meta.StaticMetaDataValues;

public class Main extends JavaPlugin {

	public static Main main;
	public static boolean debug = false;
	public static StringFlag MUSIC;
	static List<RadioSongPlayer> songPlayer = new ArrayList<RadioSongPlayer>();
	static List<String> songsTitles = new ArrayList<String>();

	@Override
	public void onEnable() {
		if (!getServer().getPluginManager().isPluginEnabled("NoteBlockAPI") || getServer().getPluginManager().getPlugin("NoteBlockAPI") == null) {
			getLogger().severe("NoteBlockAPI isn't loaded. Please install it on your server and restart it.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
			getLogger().info("This JukeBox version requires NoteBlockAPI version 1.4.3 or more. Please ensure that before using JukeBox (you are using NBAPI ver. "
					+ getPlugin(NoteBlockAPI.class).getDescription().getVersion() + ")");
		main = this;

		if (!new File(getDataFolder().getPath() + "\\songs").exists())
			new File(getDataFolder().getPath() + "\\songs").mkdirs();

		SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
		// second param allows for ordering of handlers - see the JavaDocs
		sessionManager.registerHandler(MusicHandler.FACTORY, null);

		getCommand("wgnbs").setExecutor(this);

		getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Enabled " + getName());
	}

	@Override
	public void onDisable() {
		getServer().getConsoleSender().sendMessage(ChatColor.RED + "Disabled " + getName());
	}

	// declare your flag as a field accessible to other parts of your code (so you can use this to check it)
	// note: if you want to use a different type of flag, make sure you change StateFlag here and below to that type

	@Override
	public void onLoad() {
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		try {
			StringFlag flag = new StringFlag("play-music", "none");
			registry.register(flag);
			MUSIC = flag;
			if (debug)
				pr(ChatColor.GREEN + "registered new flag: " + ChatColor.DARK_GREEN + MUSIC.getName());
		} catch (FlagConflictException e) {
			Flag<?> existing = registry.get("play-music");
			if (debug)
				pr(ChatColor.DARK_RED + MUSIC.getName() + ChatColor.RED + " conflicts with another flag");
			if (existing instanceof StateFlag)
				MUSIC = (StringFlag) existing;
			else
				pr(ChatColor.DARK_RED + MUSIC.getName() + ChatColor.RED + " already exists as another flag." + ChatColor.DARK_RED + "Please report this!");
		}
	}

	public static void MusicPlayer(LocalPlayer player, String music) {

		Player p = main.getServer().getPlayer(player.getUniqueId());
		RadioSongPlayer radio;

		if (new File(Main.main.getDataFolder().getPath() + "\\songs\\" + music + ".nbs").exists()) {

			if (debug)
				pr(ChatColor.DARK_GREEN + music + ChatColor.GREEN + " exists");

			Song song = getSongByPath(music); // Preloaded song

			if (songsTitles.contains(song.getTitle()))
				// Defining RadioSongPlayer.
				radio = songPlayer.get(songsTitles.indexOf(song.getTitle()));
			else {
				// Creating a new RadioSongPlayer.
				radio = new RadioSongPlayer(song);
				// This whole thing is to prevent playing several radios to one player
				songPlayer.add(radio);
				// And this is to replace the radio names or ids
				songsTitles.add(song.getTitle());
			}

			if (!radio.getPlayerUUIDs().contains(p.getUniqueId())) {
				// adding player to SongPlayer so he will hear the song.
				radio.addPlayer(p);
				// setting the radio to the player meta
				MetaDataManager.setPlayerMetaData(p, StaticMetaDataValues.radio, radio);
			}

			if (debug)
				pr(ChatColor.GREEN + "Playing radio with song: " + ChatColor.DARK_GREEN + music + ChatColor.GREEN + " at " + ChatColor.DARK_GREEN + "x: "
						+ (int) player.getLocation().getX() + ", y:" + (int) player.getLocation().getY() + ", z:" + (int) player.getLocation().getZ());

			if (!radio.isPlaying())
				// starting RadioSongPlayer playback
				radio.setPlaying(true);
			return;
		} else if (music != "none")
			if (debug)
				Main.main.getServer().getConsoleSender()
						.sendMessage(ChatColor.DARK_RED + "Unavailable song " + ChatColor.RED + music + ChatColor.DARK_RED + " tried playing at " + ChatColor.RED + " x: "
								+ (int) player.getLocation().getX() + ", y: " + (int) player.getLocation().getY() + ", z: " + (int) player.getLocation().getZ());
		if (p.hasMetadata(StaticMetaDataValues.radio)) {
			if (debug)
				pr(ChatColor.GREEN + "The song is none or missing. Either way we remove the player from the radio he has been in.");
			// getting the player radio from his meta
			radio = MetaDataManager.getPlayerMetaData(p, StaticMetaDataValues.radio);
			try {
				if (radio.getPlayerUUIDs().contains(p.getUniqueId()))
					// removing the player from the radio
					radio.removePlayer(p);
			} catch (NullPointerException ex) {
				// the player meta for radio is already null
			}
			if (debug)
				pr(ChatColor.GREEN + "Removing the radio meta for " + ChatColor.DARK_GREEN + p.getName());
			// removing the player meta radio
			MetaDataManager.setPlayerMetaData(p, StaticMetaDataValues.radio, null);
		}
	}

	public static Song getSongByPath(String file) {
		if (debug)
			pr(ChatColor.GREEN + "Trying to get the song: " + ChatColor.DARK_GREEN + file);
		if (file.endsWith(".nbs"))
			return NBSDecoder.parse(new File(Main.main.getDataFolder().getPath() + "\\songs\\" + file));
		else
			return NBSDecoder.parse(new File(Main.main.getDataFolder().getPath() + "\\songs\\" + file + ".nbs"));
	}

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		try {
			if (args[0].equalsIgnoreCase("debug") && args.length == 1) {
				if (s.hasPermission("wgnbs.use"))
					debug ^= true;
				if (debug)
					s.sendMessage(ChatColor.GREEN + "Enabled debugging mode");
				else
					s.sendMessage(ChatColor.RED + "Disabled debugging mode");
				return true;
			}
		} catch (NullPointerException | ArrayIndexOutOfBoundsException ex) {}
		s.sendMessage(ChatColor.RED + "Invalid command. Proper usage: " + ChatColor.DARK_RED + "/wgnbs debug");
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		List<String> list = new ArrayList<String>();

		if (args.length == 1)
			list.add("debug");
		else
			list.clear();

		return list;
	}

	public static void pr(Object o) {
		System.out.println(o);
	}
}
