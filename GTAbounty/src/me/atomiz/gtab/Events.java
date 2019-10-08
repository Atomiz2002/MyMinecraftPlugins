package me.atomiz.gtab;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import me.atomiz.gtab.NMS.PoliceOfficer;
import me.atomiz.gtab.NMS.PoliceType;
import me.atomiz.gtab.NMS.Sniper;
import me.atomiz.gtab.NMS.Swat;
import me.atomiz.gtab.Utils.CrimeManager;
import me.atomiz.gtab.Utils.MetaDataManager;
import me.atomiz.gtab.Utils.SpawnPolice;
import me.atomiz.gtab.Utils.StaticMetaDataValue;

public class Events implements Listener {

	// prevents the police from burning
	@EventHandler
	public void onBurn(EntityCombustEvent e) { // NO_UCD (unused code)
		if (Main.policeUUIDs.contains(e.getEntity().getUniqueId()))
			e.setCancelled(true);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) { // NO_UCD (unused code)
		SpawnPolice.respawnPolice(e.getPlayer());
		Helpers.updateScoreboard();
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) { // NO_UCD (unused code)
		CrimeManager.removePlayerPolicemen(e.getPlayer());
		Helpers.updateScoreboard();
	}

	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent e) { // NO_UCD (unused code)
		if (e.getNewGameMode() == GameMode.SPECTATOR) {
			CrimeManager.removePlayerPolicemen(e.getPlayer());
			Helpers.updateScoreboard();
		} else if (e.getNewGameMode() == GameMode.CREATIVE) {
			SpawnPolice.respawnPolice(e.getPlayer());
			Helpers.updateScoreboard();
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) { // NO_UCD (unused code)

		if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {

			EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) e.getEntity().getLastDamageCause();

			Entity damager = event.getDamager();
			Player p = e.getEntity();

			if (event.getCause() == DamageCause.PROJECTILE)
				if (damager instanceof Arrow)
					if (((Arrow) damager).getShooter() instanceof Skeleton)
						damager = (Entity) ((Arrow) damager).getShooter();

			if (Main.policeUUIDs.contains(damager.getUniqueId())) {
				e.setDeathMessage(p.getName() + " was slain by " + PoliceType.getPoliceName(damager));
				CrimeManager.getInstance().resetPlayer(p);
			}
		}
	}

	@EventHandler
	public void onPoliceDeath(EntityDeathEvent event) { // NO_UCD (unused code)
		Entity e = event.getEntity();
		if (e instanceof PoliceOfficer || e instanceof Sniper || e instanceof Swat || Main.policeUUIDs.contains(e.getUniqueId())) {
			event.getDrops().clear();
			event.setDroppedExp(0);
			Main.policeUUIDs.remove(e.getUniqueId());
			MetaDataManager.removePlayerTarget(e.getServer().getPlayer(UUID.fromString(MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target))),
					e.getUniqueId().toString());
			MetaDataManager.removePlayerPolice(e.getServer().getPlayer(UUID.fromString(MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target))),
					PoliceType.getTypeFromEntity(e));
			if (Main.debug)
				Helpers.pr("removed " + e.getUniqueId() + " from "
						+ e.getServer().getPlayer(UUID.fromString(MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target))));
		}
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) { // NO_UCD (unused code)
		for (Entity e : event.getChunk().getEntities())
			if (e instanceof PoliceOfficer || e instanceof Sniper || e instanceof Swat || Main.policeUUIDs.contains(e.getUniqueId())) {
				Main.policeUUIDs.remove(e.getUniqueId());
				MetaDataManager.removePlayerTarget(e.getServer().getPlayer(UUID.fromString(MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target))),
						e.getUniqueId().toString());
				MetaDataManager.removePlayerPolice(e.getServer().getPlayer(UUID.fromString(MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target))),
						PoliceType.getTypeFromEntity(e));
				if (Main.debug)
					Helpers.pr("removed " + e.getUniqueId() + " from "
							+ e.getServer().getPlayer(UUID.fromString(MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target))));
			}
	}

	@EventHandler
	public void onTarget(EntityTargetEvent event) { // NO_UCD (unused code)

		if (Main.policeUUIDs.contains(event.getEntity().getUniqueId())) {

			Entity e = event.getEntity();
			String metaTarget = MetaDataManager.getPoliceMetaData(e, StaticMetaDataValue.Target);

			try {
				if (e.hasMetadata(StaticMetaDataValue.Target) && e.getServer().getPlayer(UUID.fromString(metaTarget)).isOnline()
						&& CrimeManager.getWantedLevel(e.getServer().getPlayer(UUID.fromString(metaTarget))) > 0)
					event.setTarget(e.getServer().getPlayer(UUID.fromString(metaTarget)));
				else
					e.remove();
			} catch (NullPointerException ex) {
				System.out.println(ChatColor.RED + "Some entity didn't get removed and couldn't retarget so I removed it for you.");
				e.remove();
			}

			if (event.getTarget() == null) {
				e.remove();
				if (Main.policeUUIDs.contains(e.getUniqueId()))
					Main.policeUUIDs.remove(e.getUniqueId());
			}

			if (Main.debug) {
				e.setCustomName(event.getTarget() == null ? "null" : event.getTarget().getName());
				e.setCustomNameVisible(true);
			}
		}
	}

	/*
	 * maybe working code for storing several targets into one policeman
	 * 
		@EventHandler
		public void onTarget(EntityTargetEvent event) { // NO_UCD (unused code)
	
			Entity e = event.getEntity();
			if (policeUUIDs.contains(e.getUniqueId())) {
	
				UUID lastOnline = MetaDataManager.getPoliceLastTarget(e, true);
	
				// check if the last targer online isnt null and the current target is different
				if (lastOnline != null)
					if (event.getTarget() != getServer().getPlayer(lastOnline)) {
						event.setTarget(getServer().getPlayer(lastOnline));
						return;
					}
	
				// stores the nearby players at index - their level
				List<Player> playersLevels = new ArrayList<>();
	
				playersLevels.add(0, null);
				playersLevels.add(1, null);
				playersLevels.add(2, null);
				playersLevels.add(3, null);
				playersLevels.add(4, null);
				playersLevels.add(5, null);
	
				for (Entity entity : e.getNearbyEntities(15, 15, 15))
					if (entity instanceof Player)
						playersLevels.set(CrimeManager.getWantedLevel((Player) entity), (Player) entity);
	
				int i = playersLevels.size() - 1;
				while (i > 0 && playersLevels.get(i) == null)
					i--;
	
				if (i > 0)
					event.setTarget(playersLevels.get(i));
	
				return;
			}
		}*/

	boolean r;

	@EventHandler
	public void OnRClick(PlayerInteractEntityEvent e) { // NO_UCD (unused code)
		if (Main.debug)
			if (Main.policeUUIDs.contains(e.getRightClicked().getUniqueId()))
				if (r ^= true)
					e.getPlayer().sendMessage(ChatColor.GOLD + MetaDataManager.getPoliceMetaData(e.getRightClicked(), StaticMetaDataValue.Target));
				else
					e.getPlayer().sendMessage(ChatColor.YELLOW + e.getRightClicked().getUniqueId().toString());

	}
}
