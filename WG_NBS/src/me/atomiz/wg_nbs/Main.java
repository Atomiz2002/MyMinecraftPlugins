package me.atomiz.wg_nbs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.session.SessionManager;
import com.xxmicloxx.NoteBlockAPI.model.FadeType;
import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.Fade;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;

import me.atomiz.wg_nbs.Utils.Helpers;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;

public class Main extends JavaPlugin implements Listener {

	public static Main main;
	public static boolean debug = false;
	public static StringFlag MUSIC;
	static HashMap<UUID, RadioSongPlayer> radios = new HashMap<UUID, RadioSongPlayer>();
	private HoverEvent.Action show = HoverEvent.Action.SHOW_TEXT;
	private ClickEvent.Action run = ClickEvent.Action.RUN_COMMAND;
	public static List<String> loadedSongs;

	public static String path;
	public static final String radio = "radio";

	@Override
	public void onEnable() {

		main = this;
		path = getDataFolder().getPath() + "\\songs\\";

		if (!new File(getDataFolder().getPath() + "\\songs").exists())
			new File(getDataFolder().getPath() + "\\songs").mkdirs();

		SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
		// second param allows for ordering of handlers - see the JavaDocs
		sessionManager.registerHandler(MusicHandler.FACTORY, null);

		getCommand("wgnbs").setExecutor(this);

		if (Helpers.getFolderFiles(path, ".nbs") != null)
			loadedSongs = Helpers.getFolderFiles(path, ".nbs");
		else
			Helpers.pr(ChatColor.RED + "Songs folder empty");

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);

		getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Enabled " + getName());
	}

	@Override
	public void onDisable() {
		getServer().getConsoleSender().sendMessage(ChatColor.RED + "Disabled " + getName());
	}

	// declare your flag as a field accessible to other parts of your code (so you can use this to check it)
	// note: if you want to use a different type of flag, make sure you change StateFlag here and below to that type

	// every region will go with its own radio
	@Override
	public void onLoad() {
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		try {
			StringFlag flag = new StringFlag("play-music", "none");
			registry.register(flag);
			MUSIC = flag;
			if (debug)
				Helpers.pr(ChatColor.GREEN + "registered new flag: " + ChatColor.DARK_GREEN + MUSIC.getName());
		} catch (FlagConflictException e) {
			Flag<?> existing = registry.get("play-music");
			if (debug)
				Helpers.pr(ChatColor.DARK_RED + MUSIC.getName() + ChatColor.RED + " conflicts with another flag");
			if (existing instanceof StateFlag)
				MUSIC = (StringFlag) existing;
			else
				Helpers.pr(ChatColor.DARK_RED + MUSIC.getName() + ChatColor.RED + " already exists as another flag." + ChatColor.DARK_RED + "Please report this!");
		}
	}

	/*if a radio exists
	if the songs list contains the current song being checked
	if (radios.contains(song.getTitle()))
	// Defining RadioSongPlayer.
	// setting the radio to
	radio = radios.get(songs.indexOf(song.getTitle()));
	else {
	// Creating a new RadioSongPlayer.
	radio = new RadioSongPlayer(song);
	// This whole thing is to prevent playing several radios to one player
	radios.add(radio);
	// And this is to replace the radio names or ids
	songs.add(song.getTitle());
	}
	
	if (p.hasMetadata("radio"))
		radio = Helpers.getPlayerMetaData(p, "radio");
	else {
		radio = new RadioSongPlayer(song);
		Helpers.setPlayerMetaData(p, "radio", radio);
	}
	
	if (!radio.getPlayerUUIDs().contains(p.getUniqueId())) {
		// adding player to SongPlayer so he will hear the song.
		radio.addPlayer(p);
		// setting the radio to the player meta
		Helpers.setPlayerMetaData(p, Main.radio, radio);
	}
	
	if (debug)
		// Helpers.pr(ChatColor.GREEN + "Playing radio with song: " + ChatColor.DARK_GREEN + play + ChatColor.GREEN + ", at " + ChatColor.DARK_GREEN + "x: " +
		// (int) player.getLocation().getX() + ", y:" + (int) player.getLocation().getY() + ", z:" + (int) player.getLocation().getZ());
		Helpers.pr("Playing" + radio.getSong());
	if (!radio.isPlaying())
		// starting RadioSongPlayer playback
		radio.setPlaying(true);
	
	if (radio.getRepeatMode() != RepeatMode.NO)
		radio.setRepeatMode(RepeatMode.ALL);
	
	if (debug)
		Helpers.pr(ChatColor.GREEN + "Repeating all songs: " + ChatColor.DARK_GREEN + radio.getPlaylist() + ChatColor.GREEN + ", at " + ChatColor.DARK_GREEN
				+ "x: " + (int) player.getLocation().getX() + ", y:" + (int) player.getLocation().getY() + ", z:" + (int) player.getLocation().getZ());
	
	i++;*/

	@EventHandler
	public void PlayerQuitEvent(PlayerQuitEvent e) {
		MusicPlayer(e.getPlayer().getUniqueId(), "none");
	}

	public static void MusicPlayer(UUID uuid, String music) {

		Player p = main.getServer().getPlayer(uuid);
		if (music != "none") { // if there is a song to be played
			String[] oldSongs = music.split(";");
			List<String> songs = new ArrayList<String>(); // converting the String[] to an arraylist to remove the first song V
			for (String song : oldSongs)
				songs.add(song);
			List<Song> playlistSongs = new ArrayList<Song>(); // will store the existing songs
			for (String name : songs) // for every song name
				if (new File(path + name + ".nbs").exists()) { // if the song exists
					if (debug)
						Helpers.pr(ChatColor.DARK_GREEN + name + ChatColor.GREEN + " exists");
					playlistSongs.add(getSongByPath(name)); // add it to the playlist
				} else
					// let the console know its unavailable
					Helpers.pr(ChatColor.DARK_RED + "Unavailable song " + ChatColor.RED + name + ChatColor.DARK_RED + " tried playing at " + ChatColor.RED + " x: "
							+ (int) p.getLocation().getX() + ", y: " + (int) p.getLocation().getY() + ", z: " + (int) p.getLocation().getZ());

			// unfortunately you cant have an empty playlist so i use the first song and then add the others
			Playlist playlist = new Playlist(playlistSongs.get(0)); // adds the first song
			RadioSongPlayer radio = new RadioSongPlayer(playlist);
			playlistSongs.remove(0); // removes the first because its already added
			for (Song song : playlistSongs)
				playlist.add(song); // adds the rest of the songs

			radio.setPlaylist(playlist);
			radio.addPlayer(p);
			radio.setPlaying(true);
			radio.setRepeatMode(RepeatMode.ALL);
			Fade fadeIn = radio.getFadeIn();
			fadeIn.setType(FadeType.LINEAR);
			fadeIn.setFadeDuration(60); // in ticks
			Fade fadeOut = radio.getFadeIn();
			fadeOut.setType(FadeType.LINEAR);
			fadeOut.setFadeDuration(60);
			radios.put(uuid, radio);
		} else {
			if (debug)
				Helpers.pr(ChatColor.GREEN + "The song is none. Removing the player from the radio he has been in.");

			// i need to remove the player from the radio hes in
			if (radios.containsKey(uuid)) {
				RadioSongPlayer radio = radios.get(uuid);
				radio.removePlayer(p);
				radio.destroy();
				radios.remove(uuid);
			}
		}
	}

	public static Song getSongByPath(String file) {
		if (debug)
			Helpers.pr(ChatColor.GREEN + "Trying to get the song: " + ChatColor.DARK_GREEN + file);
		if (file.endsWith(".nbs"))
			return NBSDecoder.parse(new File(path + file));
		else
			return NBSDecoder.parse(new File(path + file + ".nbs"));
	}

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {

		// check the help menus

		if (s.hasPermission("wgnbs.use"))
			if (args.length == 1)
				switch (args[0].toLowerCase()) {
				case "help":
					s.sendMessage(ChatColor.GOLD + "Commands:");
					if (s instanceof Player) {
						Helpers.Editor((Player) s, ChatColor.DARK_AQUA + "debug" + ChatColor.GOLD + " - " + ChatColor.AQUA
								+ "Shows more detailed logs in the console that you might find helpful", show, "", run, "/wgnbs debug");
						Helpers.Editor((Player) s, ChatColor.DARK_AQUA + "songs" + ChatColor.GOLD + " - " + ChatColor.AQUA + "Lists the usable songs", show, "", run,
								"/wgnbs songs");
					} else {
						s.sendMessage(ChatColor.DARK_AQUA + "debug" + ChatColor.GOLD + " - " + ChatColor.AQUA
								+ "Shows more detailed logs in the console that you might find helpful");
						s.sendMessage(ChatColor.DARK_AQUA + "songs" + ChatColor.GOLD + " - " + ChatColor.AQUA + "Lists the usable songs");
					}
					return true;
				case "debug":
					debug ^= true;
					if (debug) {
						if (s instanceof Player)
							s.getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Enabled debugging mode");
						s.sendMessage(ChatColor.GREEN + "Enabled debugging mode");
					} else {
						if (s instanceof Player)
							s.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Disabled debugging mode");
						s.sendMessage(ChatColor.RED + "Disabled debugging mode");
					}
					return true;
				case "songs":
					if (debug)
						Helpers.pr("Loaded songs: " + loadedSongs);
					if (Helpers.reloadSongs()) {
						s.sendMessage(ChatColor.GOLD + "All loaded songs:");
						s.sendMessage(ChatColor.YELLOW + loadedSongs.toString().replace("[", "").replace("]", ""));
					} else
						s.sendMessage(ChatColor.RED + "Songs folder empty");
					return true;
				case "stop":
					if (s instanceof Player) {
						if (debug) {
							Helpers.pr(ChatColor.GREEN + "Stopping the music for " + ChatColor.DARK_GREEN + s.getName());
							MusicPlayer(((Player) s).getUniqueId(), "none");
							return true;
						}
					} else
						Helpers.pr(ChatColor.RED + "You must be a player to execute this command");
				default:
					if (s instanceof Player)
						Helpers.Editor((Player) s, ChatColor.RED + "Invalid command", show, ChatColor.GREEN + "/wgnbs help", run, "/wgnbs help");
					else
						s.sendMessage(ChatColor.RED + "Invalid command.");
				}

		// if (!s.hasPermission("wgnbs.use"))
		// s.sendMessage(ChatColor.RED + "You have no permission to execute this command.");

		if (args.length != 1)
			s.sendMessage(ChatColor.RED + "Invalid args.");
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		List<String> list = new ArrayList<String>();

		if (args.length == 1) {
			list.add("help");
			list.add("songs");
			list.add("debug");
		} else
			list.clear();

		return list;
	}
}
