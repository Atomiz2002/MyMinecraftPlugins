package me.atomiz.wgflags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.session.SessionManager;

import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener {

	// declare your flag as a field accessible to other parts of your code (so
	// you can use this to check it)
	// note: if you want to use a different type of flag, make sure you change
	// StateFlag here and below to that type

	public static StateFlag keepInv;

	@Override
	public void onEnable() {
		getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Enabled " + getName());

		SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
		// second param allows for ordering of handlers - see the JavaDocs
		sessionManager.registerHandler(FlagHandler.FACTORY, null);

		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		getServer().getConsoleSender().sendMessage(ChatColor.RED + "Disabled " + getName());
	}

	@Override
	public void onLoad() {
		// ... do your own plugin things, etc

		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		try {
			// create a flag with the name "keep-inv", defaulting to false
			StateFlag flag = new StateFlag("keep-inv", false);
			registry.register(flag);
			keepInv = flag; // only set our field if there was no error
		} catch (FlagConflictException e) {
			// some other plugin registered a flag by the same name already.
			// you can use the existing flag, but this may cause conflicts - be
			// sure to check type
			Flag<?> existing = registry.get("keep-inv");
			if (existing instanceof StateFlag) {
				keepInv = (StateFlag) existing;
			} else {
				// types don't match - this is bad news! some other plugin
				// conflicts with you
				// hopefully this never actually happens
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {

		LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(e.getEntity());
		Location loc = lp.getLocation();

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();

		if (query.testState(loc, lp, keepInv)) {
			e.setKeepInventory(true);
			e.setKeepLevel(true);
			e.getDrops().clear();
		} else {
			e.setKeepInventory(false);
			e.setKeepLevel(false);
		}
	}
}
