package me.atomiz.gtab.NMS;

import org.bukkit.entity.EntityType;

import me.atomiz.gtab.Settings.Settings;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityLiving;
import net.minecraft.server.v1_12_R1.EntityPigZombie;
import net.minecraft.server.v1_12_R1.EntitySkeleton;
import net.minecraft.server.v1_12_R1.EntityTypes;
import net.minecraft.server.v1_12_R1.EntityZombie;
import net.minecraft.server.v1_12_R1.MinecraftKey;

public enum PoliceType {
	POLICEOFFICER("PoliceOfficer", 54, EntityType.ZOMBIE, EntityZombie.class, PoliceOfficer.class),
	SNIPER("Sniper", 51, EntityType.SKELETON, EntitySkeleton.class, Sniper.class),
	SWAT("Swat", 57, EntityType.PIG_ZOMBIE, EntityPigZombie.class, Swat.class);

	private int id;
	private Class<? extends Entity> nmsClass;
	private Class<? extends Entity> customClass;
	private MinecraftKey key;
	private MinecraftKey oldKey;

	private PoliceType(String name, int id, EntityType entityType, Class<? extends Entity> nmsClass, Class<? extends Entity> customClass) {
		this.id = id;
		this.nmsClass = nmsClass;
		this.customClass = customClass;
		key = new MinecraftKey(name);
		oldKey = EntityTypes.b.b(nmsClass);
	}

	public static PoliceType getTypeFromEntity(org.bukkit.entity.Entity e) {
		if (e.getType() == EntityType.ZOMBIE)
			return POLICEOFFICER;
		else if (e.getType() == EntityType.SKELETON)
			return SNIPER;
		else
			return SWAT;
	}

	public static PoliceType getTypeFromName(String s) {
		switch (s) {
		case "Police Officer":
			return POLICEOFFICER;
		case "Sniper":
			return SNIPER;
		case "SWAT":
			return SWAT;
		default:
			return null;
		}
	}

	public static String getPoliceName(org.bukkit.entity.Entity e) {
		switch (getTypeFromEntity(e)) {
		case POLICEOFFICER:
			return Settings.getOfficerName();
		case SNIPER:
			return Settings.getSniperName();
		case SWAT:
			return Settings.getSwatName();
		default:
			return null;
		}
	}

	public static String getPoliceName(PoliceType pt) {
		switch (pt) {
		case POLICEOFFICER:
			return Settings.getOfficerName();
		case SNIPER:
			return Settings.getSniperName();
		case SWAT:
			return Settings.getSwatName();
		default:
			return null;
		}
	}

	public static void registerEntities() {
		for (PoliceType pt : PoliceType.values())
			pt.register();
	}

	public static void unregisterEntities() {
		for (PoliceType pt : PoliceType.values())
			pt.unregister();
	}

	private void register() {
		EntityTypes.d.add(key);
		EntityTypes.b.a(id, key, customClass);
	}

	private void unregister() {
		EntityTypes.d.remove(key);
		EntityTypes.b.a(id, oldKey, nmsClass);
	}

	private Class<? extends EntityLiving> s;

	public Class<? extends EntityLiving> getTypeClass() {
		return s;
	}
}