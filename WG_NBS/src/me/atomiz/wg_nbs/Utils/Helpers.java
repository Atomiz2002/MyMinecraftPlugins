package me.atomiz.wg_nbs.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import me.atomiz.wg_nbs.Main;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Helpers {

	// easier way of sending better messages to the player
	public static void Editor(Player p, String message, net.md_5.bungee.api.chat.HoverEvent.Action show, String hover, Action run, String click) {

		TextComponent text = new TextComponent();
		text.setText(message);
		text.setClickEvent(new ClickEvent(run, click));
		if (!hover.isEmpty())
			text.setHoverEvent(new HoverEvent(show, new ComponentBuilder(hover).create()));

		p.spigot().sendMessage(text);
	}

	// get the files in a folder
	public static List<String> getFolderFiles(String dir, String ext) {

		List<String> list = new ArrayList<String>();
		File[] files = new File(dir).listFiles();

		for (File file : files)
			if (file.getName().endsWith(ext))
				list.add(file.getName());

		return list;
	}

	public static void setPlayerMetaData(Player p, String key, Object value) {
		if (Main.debug)
			try {
				Helpers.pr(ChatColor.GREEN + "Setting the player " + ChatColor.DARK_GREEN + p.getName() + ChatColor.GREEN + " metadata value of " + ChatColor.DARK_GREEN
						+ key + ChatColor.GREEN + " to " + ChatColor.DARK_GREEN + value.toString());
			} catch (NullPointerException ex) {
				Helpers.pr(ChatColor.GREEN + "Setting the player " + ChatColor.DARK_GREEN + p.getName() + ChatColor.GREEN + " metadata value of " + ChatColor.DARK_GREEN
						+ key + ChatColor.GREEN + " to " + ChatColor.DARK_GREEN + "null");
			}
		p.setMetadata(key, new FixedMetadataValue(Main.main, value));
	}

	@SuppressWarnings("unchecked")
	public static <T> T getPlayerMetaData(Player p, String key) {
		if (Main.debug)
			Helpers.pr(ChatColor.GREEN + "Getting the player " + ChatColor.DARK_GREEN + p.getName() + ChatColor.GREEN + " metadata value " + ChatColor.DARK_GREEN + key);
		for (MetadataValue v : p.getMetadata(key))
			if (v.getOwningPlugin() == Main.main)
				return (T) v.value();
		return null;
	}

	public static boolean reloadSongs() {
		// check if there are any songs in the folder
		if (Helpers.getFolderFiles(Main.path, ".nbs") != null && !Helpers.getFolderFiles(Main.path, ".nbs").isEmpty()) {
			Main.loadedSongs = Helpers.getFolderFiles(Main.path, ".nbs");
			return true;
		} else
			return false;
	}

	public static void pr(Object o) {
		System.out.println(o);
	}
}
