package fr.ribesg.bukkit.ncore.node.world;

import fr.ribesg.bukkit.ncore.node.NPlugin;

/**
 * Represents the NWorld plugin
 *
 * @author Ribesg
 */
public abstract class WorldNode extends NPlugin {

	/** @see fr.ribesg.bukkit.ncore.node.NPlugin#linkCore() */
	@Override
	protected void linkCore() {
		getCore().setWorldNode(this);
	}
}
