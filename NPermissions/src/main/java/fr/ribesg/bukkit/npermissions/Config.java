/***************************************************************************
 * Project file:    NPlugins - NPermissions - Config.java                  *
 * Full Class name: fr.ribesg.bukkit.npermissions.Config                   *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.npermissions;

import fr.ribesg.bukkit.ncore.config.AbstractConfig;
import fr.ribesg.bukkit.ncore.utils.FrameBuilder;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config extends AbstractConfig<NPermissions> {

	public Config(final NPermissions instance) {
		super(instance);

		// TODO
	}

	/**
	 * @see AbstractConfig#handleValues(YamlConfiguration)
	 */
	@Override
	protected void handleValues(final YamlConfiguration config) {
		// TODO
	}

	/**
	 * @see AbstractConfig#getConfigString()
	 */
	@Override
	protected String getConfigString() {
		final StringBuilder content = new StringBuilder();
		final FrameBuilder frame;

		// Header
		frame = new FrameBuilder();
		frame.addLine("Config file for NPermissions plugin", FrameBuilder.Option.CENTER);
		frame.addLine("If you don't understand something, please ask on dev.bukkit.org");
		frame.addLine("Ribesg", FrameBuilder.Option.RIGHT);
		for (final String line : frame.build()) {
			content.append(line + '\n');
		}

		// TODO

		return content.toString();
	}
}
