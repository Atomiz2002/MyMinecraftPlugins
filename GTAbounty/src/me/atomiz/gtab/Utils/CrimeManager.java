package me.atomiz.gtab.Utils;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;

import me.atomiz.gtab.Helpers;
import me.atomiz.gtab.Main;
import me.atomiz.gtab.NMS.PoliceType;
import me.atomiz.gtab.Settings.SettingPath;

public class CrimeManager {

	private static CrimeManager cm = CrimeManager.getInstance();
	private Main main;
	public static HashMap<UUID, Cooldown> cooldowns = new HashMap<UUID, Cooldown>();
	private final Integer[] cooldownlength;

	private CrimeManager() {
		cm = this;
		main = Main.main;
		FileConfiguration c = main.getConfig();
		cooldownlength = new Integer[] { c.getInt(SettingPath.TIME_ONE_STAR), c.getInt(SettingPath.TIME_TWO_STAR), c.getInt(SettingPath.TIME_THREE_STAR),
				c.getInt(SettingPath.TIME_FOUR_STAR), c.getInt(SettingPath.TIME_FIVE_STAR) };

		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

			@Override
			public void run() {
				if (!cooldowns.isEmpty())
					for (Cooldown c : cooldowns.values()) {
						if (Cooldown.getSecondsLeft() == 0)
							setWantedLevel(c.getPlayer(), 0);
						else {
							for (Entity e : c.getPlayer().getNearbyEntities(15, 15, 15))
								if ((e instanceof Zombie || e instanceof Skeleton) && c.getPlayer().hasLineOfSight(e)) {
									c.resetCooldown();
									break;
								}
							c.decrement();
						}
						Helpers.updateScoreboard();
					}
			}
		}, 0L, 20L);
	}

	public static CrimeManager getInstance() {
		if (cm == null)
			cm = new CrimeManager();
		return cm;
	}

	public void setWantedLevel(Player p, int level) {
		setWantedLevel(p, level, true);
	}

	void setWantedLevel(Player p, int level, boolean spawnPolice) {
		if (level < 0)
			level = 0;
		if (level > 5)
			level = 5;
		if (checkRepeatedCrimes(p) != 0)
			level = checkRepeatedCrimes(p);
		if (level == 0) {
			if (cooldowns.containsKey(p.getUniqueId())) {
				cooldowns.remove(p.getUniqueId());
				MetaDataManager.setPlayerMetaData(p, StaticMetaDataValue.Crimes, 0);
				if (!MetaDataManager.getPlayersMetaList(p, StaticMetaDataValue.Target).isEmpty())
					MetaDataManager.getPlayersMetaList(p, StaticMetaDataValue.Target).forEach(target -> {
						MetaDataManager.removePlayerTarget(p, target);
						MetaDataManager.removePlayerPolice(p, PoliceType.getTypeFromEntity(p.getServer().getEntity(UUID.fromString(target))));
					});
			}
		} else if (spawnPolice && !cooldowns.containsKey(p.getUniqueId()) || level != getWantedLevel(p)) {
			cooldowns.put(p.getUniqueId(), new Cooldown(p, getCooldownlengthForLevel(level)));
			SpawnPolice.spawnPolice(p, level);
		} else
			cooldowns.put(p.getUniqueId(), new Cooldown(p, getCooldownlengthForLevel(level)));
		MetaDataManager.setPlayerMetaData(p, StaticMetaDataValue.Level, level);
		Helpers.updateScoreboard();
	}

	public static int getWantedLevel(Player p) {
		try {
			return Integer.parseInt(MetaDataManager.getPlayerMetaData(p, StaticMetaDataValue.Level));
		} catch (Exception e) {
			return 0;
		}
	}

	public int getCrimes(Player p) {
		try {
			return Integer.parseInt(MetaDataManager.getPlayerMetaData(p, StaticMetaDataValue.Crimes));
		} catch (Exception e) {
			return 0;
		}
	}

	public void resetPlayer(Player p) {
		if (p == null) {
			System.err.print("Player is null");
			return;
		}
		MetaDataManager.setPlayerMetaData(p, StaticMetaDataValue.Crimes, 0);
		setWantedLevel(p, 0);
	}

	public static void removePlayerPolicemen(Player p) {
		for (String element : MetaDataManager.getPlayersMetaList(p, StaticMetaDataValue.Target)) {
			Helpers.pr("trying to remove: " + element);
			p.getServer().getEntity(UUID.fromString(element)).remove();
			MetaDataManager.removePlayerTarget(p, element);
		}
	}

	private int checkRepeatedCrimes(Player p) {
		int amount;
		try {
			amount = Integer.parseInt(MetaDataManager.getPlayerMetaData(p, StaticMetaDataValue.Crimes));
		} catch (Exception ex) {
			amount = 0;
		}
		if (amount > 20 && amount < 50)
			return 4;
		else if (amount >= 50)
			return 5;
		else
			return 0;
	}

	void increaseCrimes(Player p) {
		int amount;
		try {
			amount = Integer.parseInt(MetaDataManager.getPlayerMetaData(p, StaticMetaDataValue.Crimes));
		} catch (Exception ex) {
			amount = 0;
		}
		MetaDataManager.setPlayerMetaData(p, StaticMetaDataValue.Crimes, amount + 1);
	}

	private int getCooldownlengthForLevel(int level) {
		return cooldownlength[level - 1];
	}

	public static class Cooldown {

		private final long lengthInSeconds;
		private static long secondsLeft;
		private final Player p;

		public Cooldown(Player p, long lengthInSeconds) {
			this.p = p;
			this.lengthInSeconds = lengthInSeconds;
			secondsLeft = lengthInSeconds;
		}

		public void decrement() {
			secondsLeft--;
		}

		public static long getSecondsLeft() {
			return secondsLeft;
		}

		public void resetCooldown() {
			secondsLeft = lengthInSeconds;
		}

		public Player getPlayer() {
			return p;
		}
	}
}
