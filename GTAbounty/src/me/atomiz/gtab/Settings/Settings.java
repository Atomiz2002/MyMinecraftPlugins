package me.atomiz.gtab.Settings;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import me.atomiz.gtab.Main;
import me.atomiz.gtab.NMS.PoliceType;

public class Settings {

	private static ItemStack[] OffiserEquip = new ItemStack[5];
	private static ItemStack[] SniperEquip = new ItemStack[4];
	private static ItemStack[] SwatEquip = new ItemStack[5];
	private static String OfficerName = "Police Officer";
	private static String SniperName = "Sniper";
	private static String SwatName = "SWAT";
	private static HashMap<Integer, HashMap<PoliceType, Integer>> policeSpawn = new HashMap<Integer, HashMap<PoliceType, Integer>>();

	private static boolean MobNameVisible = true;
	private static boolean targetNames = true;
	private static FileConfiguration config;

	public static void loadConfig() {
		Main.main.saveDefaultConfig();
		config = Main.main.getConfig();

		MobNameVisible = config.getBoolean(SettingPath.MOB_NAME_VISIBLE);
		targetNames = config.getBoolean(SettingPath.NAMES_TO_TARGETS);

		OfficerName = config.getString(SettingPath.OFFICER_NAME, "Police Officer");
		OffiserEquip[0] = getItemStack(SettingPath.OFFICER_HELMET);
		OffiserEquip[1] = getItemStack(SettingPath.OFFICER_CHESTPLATE);
		OffiserEquip[2] = getItemStack(SettingPath.OFFICER_LEGGINGS);
		OffiserEquip[3] = getItemStack(SettingPath.OFFICER_BOOTS);
		OffiserEquip[4] = getItemStack(SettingPath.OFFICER_WEAPON);

		SniperName = config.getString(SettingPath.SNIPER_NAME, "Sniper");
		SniperEquip[0] = getItemStack(SettingPath.SNIPER_HELMET);
		SniperEquip[1] = getItemStack(SettingPath.SNIPER_CHESTPLATE);
		SniperEquip[2] = getItemStack(SettingPath.SNIPER_LEGGINGS);
		SniperEquip[3] = getItemStack(SettingPath.SNIPER_BOOTS);

		SwatName = config.getString(SettingPath.SWAT_NAME, "SWAT");
		SwatEquip[0] = getItemStack(SettingPath.SWAT_HELMET);
		SwatEquip[1] = getItemStack(SettingPath.SWAT_CHESTPLATE);
		SwatEquip[2] = getItemStack(SettingPath.SWAT_LEGGINGS);
		SwatEquip[3] = getItemStack(SettingPath.SWAT_BOOTS);
		SwatEquip[4] = getItemStack(SettingPath.SWAT_WEAPON);

		String path = "WantedLevel.Spawn.";
		String path2;
		int i = 1;
		HashMap<PoliceType, Integer> level = new HashMap<PoliceType, Integer>();
		level.put(PoliceType.POLICEOFFICER, 0);
		level.put(PoliceType.SNIPER, 0);
		level.put(PoliceType.SWAT, 0);
		policeSpawn.put(0, level);
		for (String s : new String[] { "OneStar", "TwoStars", "ThreeStarts", "FourStars", "FiveStars" }) {
			path2 = path + s + ".";
			level = new HashMap<PoliceType, Integer>();
			level.put(PoliceType.POLICEOFFICER, config.getInt(path2 + "Officers"));
			level.put(PoliceType.SNIPER, config.getInt(path2 + "Snipers"));
			level.put(PoliceType.SWAT, config.getInt(path2 + "Swats"));
			policeSpawn.put(i, level);
			i++;
		}
	}

	public static HashMap<PoliceType, Integer> getPoliceSpawn(int level) {
		return policeSpawn.get(level);
	}

	public static ItemStack[] getOfficerEquip() {
		return OffiserEquip;
	}

	public static String getOfficerName() {
		return OfficerName;
	}

	public static ItemStack[] getSniperEquip() {
		return SniperEquip;
	}

	public static String getSniperName() {
		return SniperName;
	}

	public static ItemStack[] getSwatEquip() {
		return SwatEquip;
	}

	public static String getSwatName() {
		return SwatName;
	}

	public static boolean isMobNameVisible() {
		return MobNameVisible;
	}

	public static boolean setNamesToTargets() {
		return targetNames;
	}

	private static ItemStack getItemStack(String path) {
		try {
			return new ItemStack(Material.getMaterial(config.getString(path)));
		} catch (Exception e) {
			return null;
		}
	}
}
