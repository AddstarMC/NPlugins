package fr.ribesg.bukkit.ncuboid.listeners.flag;

import fr.ribesg.bukkit.ncuboid.NCuboid;
import fr.ribesg.bukkit.ncuboid.beans.Flag;
import fr.ribesg.bukkit.ncuboid.beans.GeneralRegion;
import fr.ribesg.bukkit.ncuboid.listeners.AbstractListener;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFormEvent;

public class SnowFlagListener extends AbstractListener {

	public SnowFlagListener(final NCuboid instance) {
		super(instance);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockForm(final BlockFormEvent event) {
		final BlockState newState = event.getNewState();
		if (newState.getType() == Material.SNOW || newState.getType() == Material.ICE) {
			final GeneralRegion region = getPlugin().getDb().getPriorByLocation(event.getBlock().getLocation());
			if (region != null && region.getFlag(Flag.SNOW)) {
				event.setCancelled(true);
			}
		}
	}
}