package me.atomiz.gtab;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import me.atomiz.gtab.Utils.CrimeManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

public class Helpers {

	public static String S(String name) {

		if (name.endsWith("s"))
			return name + "'";
		else
			return name + "'s";
	}

	// checks if the given string is an integer
	public static boolean isStringInt(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	// chooses a random number between the min and max values
	public static int getRandomNumberInRange(int min, int max) {

		if (min >= max)
			throw new IllegalArgumentException("max must be greater than min");

		Random r = new Random();
		return r.nextInt(max - min + 1) + min;
	}

	// easier way of sending better messages to the player
	public static void Editor(Player p, String message, net.md_5.bungee.api.chat.HoverEvent.Action show, String hover, Action suggest, String click) {

		net.md_5.bungee.api.chat.TextComponent text = new net.md_5.bungee.api.chat.TextComponent();
		text.setText(message);
		text.setClickEvent(new ClickEvent(suggest, click));
		if (!hover.isEmpty())
			text.setHoverEvent(new HoverEvent(show, new ComponentBuilder(hover).create()));

		p.spigot().sendMessage(text);
	}

	public static void updateScoreboard() {

		for (Player p : Bukkit.getOnlinePlayers()) {
			Main.board = Main.manager.getNewScoreboard();
			CrimeManager.getInstance();
			objectives(DisplaySlot.SIDEBAR, "Wanted Level:", CrimeManager.getWantedLevel(p), "Crimes:", CrimeManager.getInstance().getCrimes(p),
					CrimeManager.cooldowns.containsKey(p.getUniqueId()) ? CrimeManager.Cooldown.getSecondsLeft() : 0);
			p.setScoreboard(Main.board);
		}
	}

	public static void objectives(DisplaySlot slot, String level, int value, String crimes, int value2, long value3) {
		Objective obj = Main.board.registerNewObjective("GTA Stats", "dummy");
		obj.setDisplaySlot(slot);
		obj.setDisplayName("GTA Stats");
		obj.getScore(level).setScore(value);
		obj.getScore(crimes).setScore(value2);
		if (Main.debug)
			obj.getScore("Cooldown").setScore((int) value3);
	}

	public static void pr(Object o) {
		if (Main.debug)
			try {
				System.out.println(o);
			} catch (NullPointerException ex) {
				System.out.println("print null");
			}
	}
}
