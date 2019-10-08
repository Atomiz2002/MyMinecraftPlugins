package me.atomiz.gtab.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityTargetEvent;

import me.atomiz.gtab.Helpers;
import me.atomiz.gtab.Main;
import me.atomiz.gtab.NMS.PoliceOfficer;
import me.atomiz.gtab.NMS.PoliceType;
import me.atomiz.gtab.NMS.Sniper;
import me.atomiz.gtab.NMS.Swat;
import me.atomiz.gtab.Settings.Settings;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityMonster;

public class SpawnPolice {

	private static CrimeManager cm = CrimeManager.getInstance();

	private static Entity spawnPolice(PoliceType type, Location loc) {
		try {
			if (type == PoliceType.POLICEOFFICER) {
				PoliceOfficer e = new PoliceOfficer(((CraftWorld) loc.getWorld()).getHandle());
				e.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
				((CraftWorld) loc.getWorld()).getHandle().addEntity(e, SpawnReason.CUSTOM);
				return e;
			} else if (type == PoliceType.SNIPER) {
				Sniper e = new Sniper(((CraftWorld) loc.getWorld()).getHandle());
				e.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
				((CraftWorld) loc.getWorld()).getHandle().addEntity(e, SpawnReason.CUSTOM);
				return e;
			} else if (type == PoliceType.SWAT) {
				Swat e = new Swat(((CraftWorld) loc.getWorld()).getHandle());
				e.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
				((CraftWorld) loc.getWorld()).getHandle().addEntity(e, SpawnReason.CUSTOM);
				return e;
			}
		} catch (NullPointerException n) {
			n.printStackTrace();
		}
		return null;
	}

	public static void respawnPolice(Player p) {

		List<PoliceType> spawn = new ArrayList<PoliceType>();

		int i = 0;
		while (p.hasMetadata(StaticMetaDataValue.Police + i)) {
			spawn.add(PoliceType.getTypeFromName(MetaDataManager.getPlayerMetaData(p, StaticMetaDataValue.Police + i)));
			i++;
		}

		for (PoliceType pt : spawn) {
			int x = (int) p.getLocation().getX();
			int z = (int) p.getLocation().getZ();
			int randomX = Helpers.getRandomNumberInRange(x - 15, x + 15);
			int randomZ = Helpers.getRandomNumberInRange(z - 15, z + 15);
			Entity e = spawnPolice(pt, new Location(p.getWorld(), randomX, p.getWorld().getHighestBlockYAt(randomX, randomZ), randomZ));
			Helpers.pr("setting target to: " + p.getUniqueId());
			MetaDataManager.setPoliceMetaData(e.getBukkitEntity(), StaticMetaDataValue.Target, p.getUniqueId());
			MetaDataManager.addPlayerTarget(p, e.getUniqueID().toString());
			MetaDataManager.removePlayerPolice(p, pt); // here...
			((EntityMonster) e).setGoalTarget(((CraftPlayer) p).getHandle(), EntityTargetEvent.TargetReason.CUSTOM, true);
			if (Main.main.getConfig().getBoolean("MobNameVisible"))
				e.getBukkitEntity().setCustomNameVisible(true);
			if (Main.main.getConfig().getBoolean("SetNamesToTargets"))
				e.getBukkitEntity().setCustomName(p.getName());
			if (Main.debug)
				Helpers.pr("spawned: " + e.getUniqueID());
		}
	}

	public static void spawnPolice(Player p) {
		HashMap<PoliceType, Integer> spawns = Settings.getPoliceSpawn(CrimeManager.getWantedLevel(p));

		for (PoliceType pt : spawns.keySet())
			for (int i = 0; i < spawns.get(pt); i++) {
				int x = (int) p.getLocation().getX();
				int z = (int) p.getLocation().getZ();
				int randomX = Helpers.getRandomNumberInRange(x - 15, x + 15);
				int randomZ = Helpers.getRandomNumberInRange(z - 15, z + 15);
				Entity e = spawnPolice(pt, new Location(p.getWorld(), randomX, p.getWorld().getHighestBlockYAt(randomX, randomZ), randomZ));
				MetaDataManager.setPoliceMetaData(e.getBukkitEntity(), StaticMetaDataValue.Target, p.getUniqueId());
				MetaDataManager.addPlayerTarget(p, e.getUniqueID().toString());
				((EntityMonster) e).setGoalTarget(((CraftPlayer) p).getHandle(), EntityTargetEvent.TargetReason.CUSTOM, false);
				if (Main.main.getConfig().getBoolean("MobNameVisible"))
					e.getBukkitEntity().setCustomNameVisible(true);
				if (Main.main.getConfig().getBoolean("SetNamesToTargets"))
					e.getBukkitEntity().setCustomName(p.getName());
			}
	}

	static void spawnPolice(Player p, int level) {
		HashMap<PoliceType, Integer> spawns = Settings.getPoliceSpawn(level);

		for (PoliceType pt : spawns.keySet())
			for (int i = 0; i < spawns.get(pt); i++) {
				int x = (int) p.getLocation().getX();
				int z = (int) p.getLocation().getZ();
				int randomX = Helpers.getRandomNumberInRange(x - 15, x + 15);
				int randomZ = Helpers.getRandomNumberInRange(z - 15, z + 15);
				Entity e = spawnPolice(pt, new Location(p.getWorld(), randomX, p.getWorld().getHighestBlockYAt(randomX, randomZ), randomZ));
				MetaDataManager.setPoliceMetaData(e.getBukkitEntity(), StaticMetaDataValue.Target, p.getUniqueId());
				MetaDataManager.addPlayerTarget(p, e.getUniqueID().toString());
				((EntityMonster) e).setGoalTarget(((CraftPlayer) p).getHandle(), EntityTargetEvent.TargetReason.CUSTOM, false);
				if (Main.main.getConfig().getBoolean("MobNameVisible"))
					e.getBukkitEntity().setCustomNameVisible(true);
				if (Main.main.getConfig().getBoolean("SetNamesToTargets"))
					e.getBukkitEntity().setCustomName(p.getName());
			}
	}

	static void spawnPolice(Player p, Location loc, int level) {
		if (level == 0) {
			cm.setWantedLevel(p, 1);
			level = 1;
		}
		HashMap<PoliceType, Integer> spawns = Settings.getPoliceSpawn(level);
		for (PoliceType pt : spawns.keySet())
			for (int i = 0; i < spawns.get(pt); i++) {
				Entity e = spawnPolice(pt, loc);
				MetaDataManager.setPoliceMetaData(e.getBukkitEntity(), StaticMetaDataValue.Target, p.getUniqueId());
				MetaDataManager.addPlayerTarget(p, e.getUniqueID().toString());
				((EntityMonster) e).setGoalTarget(((CraftPlayer) p).getHandle(), EntityTargetEvent.TargetReason.CUSTOM, false);
				if (Main.main.getConfig().getBoolean("MobNameVisible"))
					e.getBukkitEntity().setCustomNameVisible(true);
				if (Main.main.getConfig().getBoolean("SetNamesToTargets"))
					e.getBukkitEntity().setCustomName(p.getName());
			}
	}
}
