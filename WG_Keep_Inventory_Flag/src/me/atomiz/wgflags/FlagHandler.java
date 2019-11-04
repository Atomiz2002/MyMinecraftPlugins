package me.atomiz.wgflags;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;

public class FlagHandler extends FlagValueChangeHandler<State> {

	public static final Factory FACTORY = new Factory();

	public static class Factory extends Handler.Factory<FlagHandler> {
		@Override
		public FlagHandler create(Session session) {
			// create an instance of a handler for the particular session
			// if you need to pass certain variables based on, for example, the
			// player
			// whose session this is, do it here
			return new FlagHandler(session);
		}
	}

	// construct with your desired flag to track changes
	public FlagHandler(Session session) {
		super(session, Main.keepInv);
	}

	@Override
	protected void onInitialValue(LocalPlayer paramLocalPlayer, ApplicableRegionSet paramApplicableRegionSet, State paramT) {}

	@Override
	protected boolean onSetValue(LocalPlayer paramLocalPlayer, Location paramLocation1, Location paramLocation2, ApplicableRegionSet paramApplicableRegionSet,
			State paramT1, State paramT2, MoveType paramMoveType) {
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer paramLocalPlayer, Location paramLocation1, Location paramLocation2, ApplicableRegionSet paramApplicableRegionSet,
			State paramT, MoveType paramMoveType) {
		return true;
	}
}