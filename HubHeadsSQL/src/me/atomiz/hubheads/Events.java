package me.atomiz.hubheads;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;

class Events implements Listener {

	@EventHandler
	void onHeadFind(PlayerInteractAtEntityEvent event) { // NO_UCD (unused code)
		Helpers.p("onHeadFind");
		if (Main.enabled) {
			Player p = event.getPlayer();
			Entity e = event.getRightClicked();
			UUID uuid = p.getUniqueId();
			if (Helpers.isPresent(e)) {
				event.setCancelled(true);
				if (!Helpers.getEventRemainingDelay(p).equals("0:00") || Main.onlinePlayerHeads.get(uuid) < 0) {
					Helpers.NotifyPlayer(p, Main.wrongMessage);
					return;
				}
				Helpers.onceScheduler(p, Main.findCommands, e.getLocation());
				if (Helpers.removePresent(p, e, true))
					Main.StartEvent(p, false);
				new BukkitRunnable() {
					@Override
					public void run() {
						Helpers.removeHeadsFromInventory(p);
					}
				}.runTaskLater(Main.main(), 20L);
			}
		}
	}

	@EventHandler
	void onPlayerJoin(PlayerJoinEvent event) { // NO_UCD (unused code)
		Helpers.p("onPlayerJoin");
		if (Main.enabled)
			synchronized (Main.main()) {
				new BukkitRunnable() {
					Player p = event.getPlayer();

					@Override
					public void run() {
						SQL.load(p);
						if (Helpers.checkPlayerData(p, true, false))
							Main.StartEvent(p, false);
					}
				}.runTaskLater(Main.main(), 1 * 10);
			}
	}

	@EventHandler
	void onPlayerLeave(PlayerQuitEvent event) { // NO_UCD (unused code)
		Helpers.p("onPlayerLeave");
		if (Main.enabled)
			Bukkit.getScheduler().runTaskAsynchronously(Main.main(), () -> {
				Player p = event.getPlayer();
				UUID uuid = p.getUniqueId();
				if (Helpers.checkPlayerData(p, true, false)) {
					int remove = Integer.min(Integer.min(Main.maxHeadsPerPlayer, Main.busyLocations.size()),
							Main.headsGoal - Main.onlinePlayerHeads.get(uuid));
					for (int i = remove; i > 0; --i)
						Helpers.removePresent(p, Helpers.getRandomElement(Main.presents), false);
				}
				if (Main.onlinePlayerHeads.containsKey(uuid) && Main.onlinePlayerDelay.containsKey(uuid))
					SQL.update(p);
			});
	}

	@EventHandler
	void onNPCSpawn(NPCSpawnEvent event) {
		if (Main.enabled && event.getNPC().getName().equals("HubHeads")) {
			Main.npcs.put(event.getNPC(), null);
			for (Player player : Bukkit.getServer().getOnlinePlayers())
				Helpers.createHolograms(player);
		}
	}

	@EventHandler
	void onNPCDespawn(NPCDespawnEvent event) {
		if (Main.enabled && event.getNPC().getName().equals("HubHeads")) {
			Main.npcs.get(event.getNPC()).forEach(holo -> {
				holo.delete();
			});
			Main.npcs.remove(event.getNPC());
		}
	}
}