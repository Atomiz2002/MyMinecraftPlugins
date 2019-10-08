package me.atomiz.gtab.NMS;

import org.bukkit.Material;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import me.atomiz.gtab.Main;
import me.atomiz.gtab.Settings.Settings;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.EntitySkeleton;
import net.minecraft.server.v1_12_R1.PathfinderGoalFloat;
import net.minecraft.server.v1_12_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_12_R1.PathfinderGoalMoveIndoors;
import net.minecraft.server.v1_12_R1.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_12_R1.PathfinderGoalOpenDoor;
import net.minecraft.server.v1_12_R1.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_12_R1.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_12_R1.World;

public class Sniper extends EntitySkeleton {

	/* getting the skely equipment from the config */
	private static org.bukkit.inventory.ItemStack[] equip = Settings.getSniperEquip();

	public Sniper(World world) {
		super(world);
//		try {
//			Field bField = PathfinderGoalSelector.class.getDeclaredField("b");
//			bField.setAccessible(true);
//			Field cField = PathfinderGoalSelector.class.getDeclaredField("c");
//			cField.setAccessible(true);
//			bField.set(goalSelector, new UnsafeList<PathfinderGoalSelector>());
//			bField.set(targetSelector, new UnsafeList<PathfinderGoalSelector>());
//			cField.set(goalSelector, new UnsafeList<PathfinderGoalSelector>());
//			cField.set(targetSelector, new UnsafeList<PathfinderGoalSelector>());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		/* set the name and if it should be visible depending on the config */
		// setting the skely name to the one in the config
		setCustomName(Settings.getSniperName());
		// setting it in/visible depending on the config
		setCustomNameVisible(Settings.isMobNameVisible());

		/* set the skely targets... (adaption from the default example and the original code) */
		goalSelector.a(0, new PathfinderGoalFloat(this));
		goalSelector.a(1, new PathfinderGoalMoveIndoors(this));
		goalSelector.a(4, new PathfinderGoalRandomLookaround(this));
		goalSelector.a(5, new PathfinderGoalOpenDoor(this, true));
		goalSelector.a(5, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
		goalSelector.a(5, new PathfinderGoalRandomStroll(this, 1.0D));

		targetSelector.a(2, new PathfinderGoalNearestAttackableTarget<EntityHuman>(this, EntityHuman.class, true));

//		/* selects the hightest level around */
//		// the list storing the entities
//		List<Entity> levels = new ArrayList<>();
//		// getting the entities in the world
//		for (Entity ent : world.players) {
//			// getting the players only
//			// adding the player to the list with index its level
//			Entity en = world.getEntity(ent.getId());
//			System.out.println(ChatColor.GREEN + "Added " + en);
//			levels.add(CrimeManager.getWantedLevel(en.getBukkitEntity()), en);
//		}
//
//		// setting the goal target to the player at the last index in the list (aka. hightest level)
//		// TO-DO find a way to have the player from a CraftCreature as an EntityCreature
//		goalSelector.a(2, new PathfinderGoalMeleeAttack((EntityCreature) levels.get(levels.size() - 1), 1.0D, false));

		/* Setting the skely equipment */
		EntityEquipment e = ((Skeleton) getBukkitEntity()).getEquipment();
		e.setHelmet(equip[0]);
		e.setChestplate(equip[1]);
		e.setLeggings(equip[2]);
		e.setBoots(equip[3]);
		e.setItemInMainHand(new ItemStack(Material.BOW));

		/* adding the skely to the policemen list */
		Main.policeUUIDs.add(getUniqueID());
	}
}
