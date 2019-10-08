package me.atomiz.wg_nbs.Meta;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import me.atomiz.wg_nbs.Main;

public class MetaDataManager {

	public static void setPlayerMetaData(Player p, String key, Object value) {
		if (Main.debug)
			try {
				Main.pr(ChatColor.GREEN + "Setting the player: " + ChatColor.DARK_GREEN + p.getName() + ChatColor.GREEN + " metadata value of: " + ChatColor.DARK_GREEN
						+ key + ChatColor.GREEN + " to " + ChatColor.DARK_GREEN + value.toString());
			} catch (NullPointerException ex) {
				Main.pr(ChatColor.GREEN + "Setting the player: " + ChatColor.DARK_GREEN + p.getName() + ChatColor.GREEN + " metadata value of: " + ChatColor.DARK_GREEN
						+ key + ChatColor.GREEN + " to " + ChatColor.DARK_GREEN + "null");
			}
		p.setMetadata(key, new FixedMetadataValue(Main.main, value));
	}

	@SuppressWarnings("unchecked")
	public static <T> T getPlayerMetaData(Player p, String key) {
		if (Main.debug)
			Main.pr(ChatColor.GREEN + "Getting the player: " + ChatColor.DARK_GREEN + p.getName() + ChatColor.GREEN + " metadata value: " + ChatColor.DARK_GREEN + key);
		for (MetadataValue v : p.getMetadata(key))
			if (v.getOwningPlugin() == Main.main)
				return (T) v.value();
		return null;
	}
}
