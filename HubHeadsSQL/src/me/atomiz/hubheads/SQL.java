package me.atomiz.hubheads;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.atomiz.hubheads.Main.Logger;

public class SQL {

	public static boolean testConnection() {
		Helpers.p("testConnection");
		try {
			synchronized (Main.main()) {
				if (!openConnection()) {
					// throw new SQLException();
					Helpers.pr("Could not connect to database: " + Main.host + ":" + Main.port, Logger.ERROR);
					Main.enabled = false;
					return false;
				}
				Main.statement = Main.SQLconnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				Main.statement.execute(
						"CREATE TABLE IF NOT EXISTS HubHeads(uuid varchar(40) NOT NULL PRIMARY KEY, heads int NOT NULL, delay long NOT NULL, timeout long NOT NULL)");
				Helpers.pr(ChatColor.GREEN + "Database connection successful", Logger.INFO);
				Main.enabled = true;
				return true;
			}
		} catch (SQLException ex) {
			Helpers.pr("Could not connect to database: " + Main.host + ":" + Main.port, Logger.ERROR);
			Main.enabled = false;
			return false;
		} finally {
			closeConnection();
		}
	}

	public static boolean openConnection() {
		Helpers.p("openConnection");
		try {
			synchronized (Main.main()) {
				if (Main.SQLconnection != null && !Main.SQLconnection.isClosed())
					return true;
				try {
					Class.forName("com.mysql.jdbc.Driver");
				} catch (ClassNotFoundException ex) {
					System.err.println("jdbc driver unavailable!");
					return false;
				}
				Main.SQLconnection = DriverManager
						.getConnection(
								"jdbc:mysql://" + Main.host + ":" + Main.port + "/" + Main.database
										+ "?useSSL=false&connectTimeout=" + Main.connectTimeoutSeconds * 1000,
								Main.username, Main.password);
				Main.statement = Main.SQLconnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				return true;
			}
		} catch (SQLException ex) {
			return false;
		}
	}

	public static void closeConnection() {
		Helpers.p("closeConnection");
		if (Main.SQLconnection == null)
			return;
		try {
			if (!Main.SQLconnection.isClosed())
				Main.SQLconnection.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public static void load(Player p) {
		Helpers.p("load");
		UUID uuid = p.getUniqueId();
		synchronized (Main.main()) {
			Main.onlinePlayerHeads.put(uuid, -1);
			Main.onlinePlayerDelay.put(uuid, -1L);
			Main.onlinePlayerTimeout.put(uuid, -1L);
			Main.headsSpawned.put(uuid, 0);
			try {

				if (!openConnection())
					return;
				ResultSet result = Main.statement.executeQuery("SELECT * FROM HubHeads WHERE uuid = '" + p.getUniqueId() + "'");
				if (result.first()) {
					Main.onlinePlayerHeads.put(uuid, result.getInt(2));
					Main.onlinePlayerDelay.put(uuid, result.getLong(3));
					Main.onlinePlayerTimeout.put(uuid, result.getLong(4));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeConnection();
				if (Helpers.getEventRemainingDelay(p).equals("0:00")
						&& Main.onlinePlayerHeads.getOrDefault(p.getUniqueId(), -1) < 0) // if the event can start and isnt running
					Main.holosReady.add(p);
				Helpers.createHolograms(p);
			}
		}
	}

	public static void update(Player p) {
		Helpers.p("update");
		UUID uuid = p.getUniqueId();
		try {
			if (openConnection())
				Main.statement.execute("REPLACE INTO HubHeads VALUES('" + uuid + "'," + Main.onlinePlayerHeads.get(uuid) + ","
						+ Main.onlinePlayerDelay.get(uuid) + "," + Main.onlinePlayerTimeout.getOrDefault(uuid, -1L) + ")");
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeConnection();
			Helpers.removeHolograms(p);
		}
		Main.onlinePlayerHeads.remove(uuid);
		Main.onlinePlayerDelay.remove(uuid);
		Main.headsSpawned.remove(uuid);
	}
}
