package fr.ribesg.bukkit.ncore.utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** @author Ribesg */
public class PlayerUtils {

	private static final int LINEOFSIGHT_MAXDISTANCE = 120;

	/**
	 * Gets all blocks along the living entity's line of sight.
	 * <p/>
	 * This list contains all blocks from the living entity's eye position
	 * to target inclusive.
	 * <p/>
	 * This is a direct translation of the following deprecated method:
	 * {@link org.bukkit.entity.LivingEntity#getLineOfSight(java.util.HashSet, int)}
	 *
	 * @param transparent HashSet containing all transparent block IDs
	 *                    (set to null for only air)
	 * @param maxDistance this is the maximum distance to scan (may be
	 *                    limited by server by at least 100 blocks, no less)
	 *
	 * @return list containing all blocks along the living entity's line
	 *         of sight
	 */
	public static List<Block> getLineOfSight(final Player player, final Set<Material> transparent, int maxDistance, final int maxLength) {
		if (maxDistance > LINEOFSIGHT_MAXDISTANCE) {
			maxDistance = LINEOFSIGHT_MAXDISTANCE;
		}
		final List<Block> blocks = new ArrayList<>();
		final Iterator<Block> itr = new BlockIterator(player, maxDistance);
		while (itr.hasNext()) {
			final Block b = itr.next();
			blocks.add(b);
			if (maxLength != 0 && blocks.size() > maxLength) {
				blocks.remove(0);
			}
			final Material m = b.getType();
			if (transparent == null) {
				if (m != Material.AIR) {
					break;
				}
			} else {
				if (!transparent.contains(m)) {
					break;
				}
			}
		}
		return blocks;
	}

	/**
	 * Gets all blocks along the living entity's line of sight.
	 * <p/>
	 * This list contains all blocks from the living entity's eye position
	 * to the block at distance distance inclusive.
	 *
	 * @param distance this is the distance to scan (may be
	 *                 limited by server by at least 100 blocks, no less)
	 *
	 * @return list containing all blocks along the living entity's line
	 *         of sight
	 */
	public static List<Block> getAllInLineOfSight(final Player player, int distance) {
		if (distance > LINEOFSIGHT_MAXDISTANCE) {
			distance = LINEOFSIGHT_MAXDISTANCE;
		}
		final List<Block> blocks = new ArrayList<>();
		final Iterator<Block> itr = new BlockIterator(player, distance);
		while (itr.hasNext()) {
			final Block b = itr.next();
			blocks.add(b);
		}
		return blocks;
	}

	/**
	 * Gets the block that the living entity has targeted.
	 * <p/>
	 * This is a direct translation of the following deprecated method:
	 * {@link org.bukkit.entity.LivingEntity#getTargetBlock(java.util.HashSet, int)}
	 *
	 * @param transparent HashSet containing all transparent block IDs
	 *                    (set to null for only air)
	 * @param maxDistance this is the maximum distance to scan (may be
	 *                    limited by server by at least 100 blocks, no less)
	 *
	 * @return block that the living entity has targeted or null is only
	 *         transparent blocks in line of sight
	 */
	public static Block getTargetBlock(final Player player, final Set<Material> transparent, int maxDistance) {
		if (maxDistance > LINEOFSIGHT_MAXDISTANCE) {
			maxDistance = LINEOFSIGHT_MAXDISTANCE;
		}
		final Iterator<Block> itr = new BlockIterator(player, maxDistance);
		Block result = null;
		while (itr.hasNext()) {
			result = itr.next();
			final Material m = result.getType();
			if (transparent == null) {
				if (m != Material.AIR) {
					break;
				}
			} else {
				if (!transparent.contains(m)) {
					break;
				}
			}
		}
		return result;
	}
}