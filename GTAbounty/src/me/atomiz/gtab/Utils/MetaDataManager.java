package me.atomiz.gtab.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import me.atomiz.gtab.Helpers;
import me.atomiz.gtab.Main;
import me.atomiz.gtab.NMS.PoliceType;

public class MetaDataManager {

	public static void setPlayerMetaData(Player p, String key, Object value) {
		p.setMetadata(key, new FixedMetadataValue(Main.main, value));
	}

	public static String getPlayerMetaData(Player p, String key) {

		for (MetadataValue v : p.getMetadata(key))
			if (v.getOwningPlugin() == Main.main)
				return v.value().toString();
		return null;
	}

	public static void setPoliceMetaData(Entity e, String key, Object value) {
		e.setMetadata(key, new FixedMetadataValue(Main.main, value));
	}

	public static String getPoliceMetaData(Entity e, String key) {

		for (MetadataValue v : e.getMetadata(key))
			if (v.getOwningPlugin() == Main.main)
				if (v.value() != null)
					return v.value().toString();
				else
					return "null";
		return "null";
	}

	public static void addPlayerTarget(Player p, String string) {

		/* i ended up storing the data in lists and adding later like so:
		 * add the new target to the list
		 * remove the targets from the player meta
		 * assign the list values to the player meta
		 */

		ArrayList<String> targets = new ArrayList<String>();
		int i = 0;
		while (p.hasMetadata(StaticMetaDataValue.Target + i)) {
			targets.add(getPlayerMetaData(p, StaticMetaDataValue.Target + i));
			p.removeMetadata(StaticMetaDataValue.Target + i, Main.main);
			i++;
		}

		targets.add(string);
		if (Main.debug)
			Helpers.pr(ChatColor.GREEN + "(+) " + ChatColor.DARK_GREEN + PoliceType.getPoliceName(p.getServer().getEntity(UUID.fromString(string))) + ": "
					+ ChatColor.GREEN + string);

		for (i = 0; i < targets.size(); i++)
			setPlayerMetaData(p, StaticMetaDataValue.Target + i, targets.get(i));

		ArrayList<String> police = new ArrayList<String>();
		i = 0;
		while (p.hasMetadata(StaticMetaDataValue.Police + i)) {
			police.add(getPlayerMetaData(p, StaticMetaDataValue.Police + i)); // add to the list
			p.removeMetadata(StaticMetaDataValue.Police + i, Main.main); // remove from the player
			i++;
		}

		police.add(PoliceType.getPoliceName(p.getServer().getEntity(UUID.fromString(string))));

		for (i = 0; i < police.size(); i++)
			setPlayerMetaData(p, StaticMetaDataValue.Police + i, police.get(i));
	}

	public static void removePlayerTarget(Player p, String string) {

		ArrayList<String> targets = new ArrayList<String>();
		int i = 0;
		while (p.hasMetadata(StaticMetaDataValue.Target + i)) {
			targets.add(getPlayerMetaData(p, StaticMetaDataValue.Target + i));
			p.removeMetadata(StaticMetaDataValue.Target + i, Main.main);
			i++;
		}

		if (targets.contains(string)) {
			if (Main.debug)
				Helpers.pr(ChatColor.RED + "(-) " + ChatColor.DARK_RED + PoliceType.getPoliceName(p.getServer().getEntity(UUID.fromString(string))) + ": " + ChatColor.RED
						+ string);
			p.getServer().getEntity(UUID.fromString(string)).remove();
			targets.remove(string);
			Main.policeUUIDs.remove(UUID.fromString(string));
		} else
			Helpers.pr("Target doesn't exist");

		for (i = 0; i < targets.size(); i++)
			setPlayerMetaData(p, StaticMetaDataValue.Target + i, targets.get(i));

	}

	public static void removePlayerPolice(Player p, PoliceType pt) {

		ArrayList<String> police = new ArrayList<String>();
		int i = 0;
		while (p.hasMetadata(StaticMetaDataValue.Police + i)) {
			police.add(getPlayerMetaData(p, StaticMetaDataValue.Police + i));
			p.removeMetadata(StaticMetaDataValue.Police + i, Main.main);
			i++;
		}

		police.remove(PoliceType.getPoliceName(pt));

		for (i = 0; i < police.size(); i++)
			setPlayerMetaData(p, StaticMetaDataValue.Police + i, police.get(i));
	}

	public static List<String> getPlayersMetaList(Player p, String key) {
		List<String> list = new ArrayList<String>();
		if (key.contains(StaticMetaDataValue.Target) || key.contains(StaticMetaDataValue.Police)) {
			int i = 0;
			while (p.hasMetadata(key + i)) {
				list.add(getPlayerMetaData(p, key + i));
				i++;
			}
		} else
			list.add(getPlayerMetaData(p, key));
		return list;
	}
}
