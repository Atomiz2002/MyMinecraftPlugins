package me.atomiz.wg_nbs;

import org.bukkit.ChatColor;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;

import me.atomiz.wg_nbs.Utils.Helpers;

public class MusicHandler extends FlagValueChangeHandler<String> {

	public static final Factory FACTORY = new Factory();

	public static class Factory extends Handler.Factory<MusicHandler> {
		@Override
		public MusicHandler create(Session session) {
			return new MusicHandler(session);
		}
	}

	public MusicHandler(Session session) {
		super(session, Main.MUSIC);
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, String arg2) {
		if (Main.debug)
			Helpers.pr(ChatColor.GREEN + "Playing music");
		String query = set.queryValue(player, Main.MUSIC);
		Main.MusicPlayer(player.getUniqueId(), query);
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location arg1, Location arg2, ApplicableRegionSet set, String arg4, String arg5, MoveType arg6) {
		if (Main.debug)
			Helpers.pr(ChatColor.GREEN + "Playing music");
		String query = set.queryValue(player, Main.MUSIC);
		Main.MusicPlayer(player.getUniqueId(), query);
		return true;
	}

	@Override // on region leave or value is to null
	protected boolean onAbsentValue(LocalPlayer player, Location arg1, Location arg2, ApplicableRegionSet set, String arg4, MoveType arg5) {
		return true;
	}
}