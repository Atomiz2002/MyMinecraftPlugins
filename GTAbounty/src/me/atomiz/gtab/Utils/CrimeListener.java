package me.atomiz.gtab.Utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import me.atomiz.gtab.Helpers;
import me.atomiz.gtab.Main;
import me.atomiz.gtab.NMS.PoliceOfficer;
import me.atomiz.gtab.NMS.Sniper;
import me.atomiz.gtab.NMS.Swat;
import me.atomiz.gtab.Settings.SettingPath;

public class CrimeListener implements Listener {

	private CrimeManager cm = CrimeManager.getInstance();

	private static void hurtEntity(Entity edamager, Damageable victim, double damage) {
		if (!(edamager instanceof Player))
			return;
		Player damager = (Player) edamager;
		net.minecraft.server.v1_12_R1.Entity handle = ((CraftEntity) victim).getHandle();
		int level;
		try {
			level = Integer.parseInt(MetaDataManager.getPlayerMetaData(damager, StaticMetaDataValue.Level));
		} catch (Exception ex) {
			level = 0;
		}
		FileConfiguration config = Main.main.getConfig();
		if (victim instanceof Player && damager != victim) { // hurt player
			if (victim.getHealth() <= damage) {
				level = Math.max(level, config.getInt(SettingPath.CONDITION_KILL_PLAYER, 2));
				CrimeManager.getInstance().setWantedLevel(damager, level, false);
				SpawnPolice.spawnPolice(damager, victim.getLocation(), CrimeManager.getWantedLevel(damager));
				CrimeManager.getInstance().increaseCrimes(damager);
			} else {
				level = Math.max(level, config.getInt(SettingPath.CONDITION_ATTACK_PLAYER, 1));
				CrimeManager.getInstance().setWantedLevel(damager, level);
			}
		} else if (handle instanceof PoliceOfficer || handle instanceof Swat || handle instanceof Sniper) { // hurt police
			if (victim.getHealth() <= damage) {
				level = Math.max(level, config.getInt(SettingPath.CONDITION_KILL_POLICE, 4));
				CrimeManager.getInstance().setWantedLevel(damager, level, false);
				SpawnPolice.spawnPolice(damager, victim.getLocation(), CrimeManager.getWantedLevel(damager));
				CrimeManager.getInstance().increaseCrimes(damager);
				if (Main.debug) {
					Helpers.pr("removing police meta from player");
					Main.main.pm(damager, null);
				}
				MetaDataManager.removePlayerTarget(damager, victim.getUniqueId().toString());
				if (Main.debug) {
					Main.main.pm(damager, null);
					Helpers.pr(victim.getUniqueId().toString());
				}
			} else {
				level = Math.max(level, config.getInt(SettingPath.CONDITION_ATTACK_POLICE, 3));
				CrimeManager.getInstance().setWantedLevel(damager, level);
			}
		} else if (victim.getHealth() <= damage) { // kill something else
			level = Math.max(level, config.getInt(SettingPath.CONDITION_KILL_MOB, 0));
			CrimeManager.getInstance().setWantedLevel(damager, level, false);
			// SpawnPolice.spawnPolice(damager, victim.getLocation(), CrimeManager.getWantedLevel(damager));
			CrimeManager.getInstance().increaseCrimes(damager);
		} else { // hurt something else
			level = Math.max(level, config.getInt(SettingPath.CONDITION_ATTACK_MOB, 0));
			CrimeManager.getInstance().setWantedLevel(damager, level);
		}
	}

	@EventHandler
	public void hurtEntity(EntityDamageByEntityEvent e) { // NO_UCD (unused code)
		if (e.getEntity() instanceof Damageable)
			hurtEntity(e.getDamager(), (Damageable) e.getEntity(), e.getDamage());
	}

	@EventHandler
	public void onHorseTheft(VehicleEnterEvent e) { // NO_UCD (unused code)
		if (e.getVehicle() instanceof Horse && e.getEntered() instanceof Player) {
			final Horse h = (Horse) e.getVehicle();
			if (h.getOwner() != null && h.getOwner() != e.getEntered())
				if (CrimeManager.getWantedLevel((Player) e.getEntered()) < 2) {
					cm.setWantedLevel((Player) e.getEntered(), 2);
					cm.increaseCrimes((Player) e.getEntered());
				}
		}
	}
}
