package me.atomiz.wg_nbs;

import org.bukkit.ChatColor;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;

public class MusicHandler extends FlagValueChangeHandler<String> {

	public static final Factory FACTORY = new Factory();

	public static class Factory extends Handler.Factory<MusicHandler> {
		@Override
		public MusicHandler create(Session session) {
			// create an instance of a handler for the particular session
			// if you need to pass certain variables based on, for example, the player
			// whose session this is, do it here
			return new MusicHandler(session);
		}
	}

	// construct with your desired flag to track changes
	public MusicHandler(Session session) {
		super(session, Main.MUSIC);
	}

	@Override
	protected void onInitialValue(LocalPlayer arg0, ApplicableRegionSet arg1, String arg2) {}

	@Override // on region enter or value changed to !value
	protected boolean onSetValue(LocalPlayer player, Location arg1, Location arg2, ApplicableRegionSet set, String arg4, String arg5, MoveType arg6) {
		if (Main.debug)
			Main.pr(ChatColor.GREEN + "Setting 'unavailable' to false.");
		if (Main.debug)
			Main.pr(ChatColor.GREEN + "Getting the hightest priority music flag and playing music");
		String query = set.queryValue(player, Main.MUSIC);
		Main.MusicPlayer(player, query);
		return true;
	}

	@Override // on region leave or value is to null
	protected boolean onAbsentValue(LocalPlayer player, Location arg1, Location arg2, ApplicableRegionSet set, String arg4, MoveType arg5) {
		return true;
	}
}