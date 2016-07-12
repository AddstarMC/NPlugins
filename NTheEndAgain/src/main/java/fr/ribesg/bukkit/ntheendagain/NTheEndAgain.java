/***************************************************************************
 * Project file:    NPlugins - NTheEndAgain - NTheEndAgain.java            *
 * Full Class name: fr.ribesg.bukkit.ntheendagain.NTheEndAgain             *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.ntheendagain;

import fr.ribesg.bukkit.ncore.node.NPlugin;
import fr.ribesg.bukkit.ncore.node.theendagain.TheEndAgainNode;
import fr.ribesg.bukkit.ntheendagain.handler.EndWorldHandler;
import fr.ribesg.bukkit.ntheendagain.lang.Messages;
import fr.ribesg.bukkit.ntheendagain.listener.ChunkListener;
import fr.ribesg.bukkit.ntheendagain.listener.DamageListener;
import fr.ribesg.bukkit.ntheendagain.listener.EnderDragonListener;
import fr.ribesg.bukkit.ntheendagain.listener.WorldListener;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

public class NTheEndAgain extends NPlugin implements TheEndAgainNode {

    // Configs
    private Messages messages;

    // Useful Nodes
    // // None

    // Actual plugin data
    private HashMap<String, EndWorldHandler> worldHandlers;

    @Override
    protected String getMinCoreVersion() {
        return "0.6.9";
    }

	/**
     * Reload the config
     * @param sender The Sender
     * @throws IOException if the config cant be loaded
     */
    protected void reloadConfig(final CommandSender sender) throws IOException {
        this.entering(this.getClass(), "reLoadConfig");

        this.debug("Reloading End world config ...");

        for (final EndWorldHandler handler : this.worldHandlers.values()) {

            Config config = handler.getConfig();
            World currentWorld = handler.getEndWorld();

            this.debug("Reloading config for world " + config.getWorldName() + " (" + currentWorld.getEnvironment().toString() + ")");

            try {

                final int savedRespawnType = config.getRespawnType();
                final int savedRespawnTimerMin = config.getRespawnTimerMin();
                final int savedRespawnTimerMax = config.getRespawnTimerMax();

                final int savedRegenType = config.getRegenType();
                final int savedRegenTimer = config.getRegenTimer();

                final int savedRegenOuterEnd = config.getRegenOuterEnd();
                final int savedOuterEndRegenHours = config.getOuterEndRegenHours();

                try {
                    handler.loadConfig();
                } catch (final IOException e) {
                    this.error("This error occured when NTheEndAgain tried to load " + e.getMessage() + ".yml", e);
                    break;
                }

                // Look for changed values

                int updatedRespawnType = config.getRespawnType();
                int updatedRespawnTimerMin = config.getRespawnTimerMin();
                int updatedRespawnTimerMax = config.getRespawnTimerMax();

                int updatedRegenType = config.getRegenType();
                int updatedRegenTimer = config.getRegenTimer();

                int updatedRegenOuterEnd = config.getRegenOuterEnd();
                int updatedOuterEndRegenHours = config.getOuterEndRegenHours();

                Boolean showDetails = (currentWorld.getEnvironment() == World.Environment.THE_END);
                StringBuilder msgAddon = new StringBuilder();

                String msg;

                if (savedRespawnType != updatedRespawnType) {
                    this.info("Re-creating tasks because respawnType has changed from " + savedRespawnType + " to " + updatedRespawnType);
                    handler.recreateTasksLater();
                    showDetails = true;
                } else if (savedRegenType != updatedRegenType) {
                    this.info("Re-creating tasks because regenType has changed from " + savedRegenType + " to " + updatedRegenType);
                    handler.recreateTasksLater();
                    showDetails = true;
                } else if (savedRespawnTimerMin != updatedRespawnTimerMin ||
                           savedRespawnTimerMax != updatedRespawnTimerMax) {
                    if (config.getSecondsUntilNextExpectedRespawn() > 0) {
                        msg = "Note: the new respawn timer values will not take effect until after the next respawn event " +
                                "(changed from " + savedRespawnTimerMin + "-" + savedRespawnTimerMax +
                                " seconds to " + updatedRespawnTimerMin + "-" + updatedRespawnTimerMax + " seconds)";
                        this.debug(msg);
                        msgAddon.append('\n').append(msg);
                    }
                    showDetails = true;
                } else if (savedRegenTimer != updatedRegenTimer) {
                    if (updatedRegenType == 2 || updatedRegenType == 3) {
                        msg = "Note: the new regen timer value will not take effect until after the next regen " +
                                "(changed from " + savedRegenTimer + " to " + updatedRegenTimer + ")";
                        this.debug(msg);
                        msgAddon.append('\n').append(msg);
                    }
                    showDetails = true;
                }

                if (savedRegenOuterEnd != updatedRegenOuterEnd)
                    showDetails = true;

                switch (updatedRegenOuterEnd) {
                    case 0:
                        config.setNextOuterEndRegenTime(0);
                        if (showDetails) {
                            msg = "Outer end islands will not auto-regen";
                        }
                        break;
                    case 1:
                        config.setNextOuterEndRegenTime(0);
                        if (showDetails) {
                            msg = "Outer end islands will regenerate every time the central island is regenerated";
                        }
                        break;
                    case 2:
                        if (updatedOuterEndRegenHours < 1) {
                            updatedOuterEndRegenHours = 1;
                            this.debug("outerEndRegenHours cannot be 0 when regenOuterEnd is 2; setting to 1");
                            config.setOuterEndRegenHours(updatedOuterEndRegenHours);
                        }

                        long newOuterEndRegenTimeMillis;
                        long systemTime = System.currentTimeMillis();
                        long currentOuterEndRegenTime = config.getNextOuterEndRegenTime();

                        if (currentOuterEndRegenTime <= 0 || savedOuterEndRegenHours <= 0) {
                            newOuterEndRegenTimeMillis = systemTime + updatedOuterEndRegenHours * 60L * 60L * 1000L;
                        } else {
                            newOuterEndRegenTimeMillis = currentOuterEndRegenTime + (updatedOuterEndRegenHours - savedOuterEndRegenHours) * 60L * 60L * 1000L;
                            if (newOuterEndRegenTimeMillis < systemTime)
                                newOuterEndRegenTimeMillis = systemTime;
                        }

                        this.debug("systemTimeMillis:               " + systemTime);
                        this.debug("currentOuterEndRegenTimeMillis: " + currentOuterEndRegenTime);
                        this.debug("new    OuterEndRegenTimeMillis: " + newOuterEndRegenTimeMillis);

                        break;
                    default:
                        // Unknown mode
                        msg = "Unknown value for regenOuterEnd: " + updatedRegenOuterEnd;
                        this.error(msg);
                        msgAddon.append('\n').append(msg);
                        break;
                }

                if (showDetails) {
                    showStatus(sender, handler, msgAddon);
                }

            } catch (final InvalidConfigurationException e) {
                this.error("An error occured when NTheEndAgain tried to load \"" + handler.getCamelCaseWorldName() + "\"'s config file.", e);
                break;
            }
        }

    }

	/**
     * Show NTheEndAgain config values for each world of type THE_END
     * @param sender the Commandsender
     */
    public void showStatus(final CommandSender sender) {

        final StringBuilder msgAddon = new StringBuilder();

        for (final EndWorldHandler handler : this.worldHandlers.values()) {

            World currentWorld = handler.getEndWorld();
            if (currentWorld.getEnvironment() != World.Environment.THE_END)
                continue;

            showStatus(sender, handler, msgAddon);
        }
    }

    /**
     * Show NTheEndAgain config values for each world of type THE_END
     * @param sender the Commandsender
     * @param handler End world handler
     * @param msgAddon Additional text to append (assumed to start with \n)
     */
    private void showStatus(final CommandSender sender, EndWorldHandler handler, final StringBuilder msgAddon) {

        Config config = handler.getConfig();
        World currentWorld = handler.getEndWorld();

        this.debug("Config details for world " + config.getWorldName() + " (" + currentWorld.getEnvironment().toString() + ")");

        StringBuilder multiLineMsg = new StringBuilder();
        multiLineMsg.append("Config for ").append(currentWorld.getName());

        String msg;

        int respawnType = config.getRespawnType();
        int respawnTimerMin = config.getRespawnTimerMin();
        int respawnTimerMax = config.getRespawnTimerMax();
        String respawnInterval = "between " + respawnTimerMin + " and " + respawnTimerMax + " seconds";

        String dragonSpawnDesc;
        int dragonRespawnCount = config.getRespawnNumber();
        if (dragonRespawnCount == 1) {
            dragonSpawnDesc = "1 new EnderDragon";
        } else {
            dragonSpawnDesc = dragonRespawnCount + " new EnderDragons";
        }

        switch (respawnType) {
            case 0:
                msg = "EnderDragon auto-respawn is disabled (respawnType=0)";
                break;
            case 1:
                msg = "1 new EnderDragon will respawn " + respawnInterval +
                        " after each Dragon's death (respawnType=1)";
                break;
            case 2:
                // Example messages:
                // 1 new EnderDragon will respawn between x and y seconds after the last dragon alive's death
                // 2 new EnderDragons will respawn between x and y seconds after the last dragon alive's death
                msg = dragonSpawnDesc + " will respawn " + respawnInterval + " after the last dragon alive's death (respawnType=2)";
                break;
            case 3:
                msg = dragonSpawnDesc + " will respawn on server start (respawnType=3)";
                break;
            case 4:
                msg = dragonSpawnDesc + " will respawn " + respawnInterval + " after boot/load (respawnType=4)";
                break;
            case 5:
                msg = dragonSpawnDesc + " will respawn " + respawnInterval + " persistent through reboots/reloads (respawnType=5)";
                break;
            default:
                msg = "Unknown value for respawnType: " + respawnType;
                break;
        }

        this.debug(msg);
        multiLineMsg.append('\n').append(msg);

        int regenType = config.getRegenType();

        switch (regenType) {
            case 0:
                msg = "End island chunk regen is disabled (regenType=0)";
                break;
            case 1:
                msg = "End island chunks will regenerate just before a new EnderDragon is spawned (regenType=1)";
                break;
            case 2:
                msg = "End island chunks will regenerate " + config.getRegenTimer() + " seconds after boot/load (regenType=2)";
                break;
            case 3:
                msg = "End island chunks will regenerate every " + config.getRegenTimer() + " seconds (regenType=3)";
                break;
            default:
                msg = "Unknown value for regenType: " + regenType;
                break;
        }

        this.debug(msg);
        multiLineMsg.append('\n').append(msg);

        int regenOuterEnd = config.getRegenOuterEnd();

        switch (regenOuterEnd) {
            case 0:
                msg = "Outer end islands will not auto-regen";
                this.debug(msg);
                multiLineMsg.append('\n').append(msg);
            case 1:
                msg = "Outer end islands will regenerate every time the central island is regenerated";
                this.debug(msg);
                multiLineMsg.append('\n').append(msg);
                break;
            case 2:
                long systemTime = System.currentTimeMillis();
                long outerEndRegenTimeMillis = config.getNextOuterEndRegenTime();

                this.debug("systemTimeMillis:        " + systemTime);
                this.debug("outerEndRegenTimeMillis: " + outerEndRegenTimeMillis);

                float outerEndRegenTimeDays = (outerEndRegenTimeMillis - systemTime) / 1000.0F / 60.0F / 60.0F / 24.0F;

                if (outerEndRegenTimeDays < 1) {
                    if (outerEndRegenTimeDays <= 0) {
                        msg = "Outer end islands will regen the next time an auto-regen occurs";
                    } else {
                        msg = "Outer end islands will regen in " + new DecimalFormat("#.#").format(outerEndRegenTimeDays * 24) + " hours";
                    }
                } else {
                    msg = "Outer end islands will regen in " + new DecimalFormat("#.##").format(outerEndRegenTimeDays) + " days";
                }

                this.debug(msg);
                multiLineMsg.append('\n').append(msg);

                break;
            default:
                // Unknown mode
                msg = "Unknown value for regenOuterEnd: " + regenOuterEnd;
                this.error(msg);
                ShowMessage(sender, msg, ChatColor.RED);
                break;
        }

        // Check whether any known dragons are alive
        final Integer nb = handler.getNumberOfAliveEnderDragons();

        if (nb == 0) {
            multiLineMsg.append('\n' + "There is no EnderDragon alive");
        } else if (nb == 1) {
            multiLineMsg.append('\n' + "There is 1 EnderDragon alive");
        } else {
            multiLineMsg.append('\n' + "There are ").append(nb).append(" EnderDragons alive");
        }

        // Look for a scheduled respawn event
        long secondsUntilNextExpectedRespawn = config.getSecondsUntilNextExpectedRespawn();
        if (secondsUntilNextExpectedRespawn <= 0) {
            msg = "At present, no EnderDragons are actively scheduled to be respawned";
        } else {
            if (secondsUntilNextExpectedRespawn < 3600)
                msg = "An EnderDragon is expected to respawn in " + secondsUntilNextExpectedRespawn + " seconds";
            else
                msg = "An EnderDragon is expected to respawn in " +
                        new DecimalFormat("#.#").format(secondsUntilNextExpectedRespawn / 3600F) + " hours";
        }

        this.debug(msg);
        multiLineMsg.append('\n').append(msg);

        // Append any extra text
        if (msgAddon.length() > 0)
            multiLineMsg.append(msgAddon.toString());

        ShowMessage(sender, multiLineMsg.toString());

    }

    @Override
    protected void loadMessages() throws IOException {

        this.entering(this.getClass(), "loadMessages");

        // Uncomment to test different log levels
        // this.getLogger().log(Level.SEVERE, "Log level severe");
        // this.getLogger().log(Level.WARNING, "Log level warning");
        // this.getLogger().log(Level.INFO, "Log level info");

        this.debug("Loading plugin Messages...");
        if (!this.getDataFolder().isDirectory()) {
            this.getDataFolder().mkdir();
        }

        final Messages messages = new Messages();
        messages.loadMessages(this);

        this.messages = messages;
    }

    @Override
    public boolean onNodeEnable() {
        this.entering(this.getClass(), "onNodeEnable");

        this.debug("Loading End world config and Chunk data...");
        this.worldHandlers = new HashMap<>();
        boolean res = true;

        this.debug("Analysing all worlds...");
        for (final World w : Bukkit.getWorlds()) {
            this.debug("World " + w.getName() + " is of type " + w.getEnvironment());
            try {
                this.debug("Trying to load world " + w.getName());
                res = this.loadWorld(w);
                if (!res) {
                    this.debug("Load of world " + w.getName() + " failed!");
                    break;
                }
            } catch (final InvalidConfigurationException e) {
                this.error("An error occured when NTheEndAgain tried to load \"" + w.getName() + "\"'s config file.", e);
                break;
            }
        }

        if (!res) {
            this.error("Failed to load a configuration, please triple-check them. Disabling plugin...");
            for (final EndWorldHandler handler : this.worldHandlers.values()) {
                handler.cancelTasks();
            }
            return false;
        }

        this.debug("Activating filter if needed...");
        this.activateFilter();

        this.debug("Registering event handlers...");
        this.getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
        this.getServer().getPluginManager().registerEvents(new EnderDragonListener(this), this);
        this.getServer().getPluginManager().registerEvents(new DamageListener(this), this);

        this.debug("Registering command...");
        this.setCommandExecutor("nend", new TheEndAgainCommandExecutor(this));

        this.exiting(this.getClass(), "onNodeEnable");
        return true;
    }

    @Override
    protected void handleOtherNodes() {
        // Nothing to do here for now
    }

    @Override
    public void onNodeDisable() {
        this.entering(this.getClass(), "onNodeDisable");

        for (final EndWorldHandler handler : this.worldHandlers.values()) {
            try {
                handler.unload(true);
            } catch (final InvalidConfigurationException e) {
                this.error("Unable to disable \"" + handler.getEndWorld().getName() + "\"'s world handler. Server should be " +
                           "stopped now (Were you reloading?)", e);
            }
        }

        this.exiting(this.getClass(), "onNodeDisable");
    }

    public Path getConfigFilePath(final String fileName) {
        return Paths.get(this.getDataFolder().getPath(), fileName + ".yml");
    }

    public boolean loadWorld(final World endWorld) throws InvalidConfigurationException {
        final EndWorldHandler handler = new EndWorldHandler(this, endWorld);
        try {
            handler.loadConfig();
            handler.loadChunks();
            this.worldHandlers.put(handler.getCamelCaseWorldName(), handler);
            handler.initLater();
            return true;
        } catch (final IOException e) {
            this.error("This error occured when NTheEndAgain tried to load " + e.getMessage() + ".yml", e);
            return false;
        }
    }

    /**
     * @param lowerCamelCaseWorldName Key
     *
     * @return Value
     */
    public EndWorldHandler getHandler(final String lowerCamelCaseWorldName) {
        return this.worldHandlers.get(lowerCamelCaseWorldName);
    }

    /**
     * Activate the "Moved too quickly!" messages filter if at least one
     * End world require it
     */
    public void activateFilter() {
        boolean filterActivated = false;
        for (final EndWorldHandler handler : this.worldHandlers.values()) {
            if (handler.getConfig().getFilterMovedTooQuicklySpam() == 1) {
                this.debug("Filter needs to be actiavted for world " + handler.getEndWorld().getName());
                filterActivated = true;
                break;
            }
        }
        if (filterActivated) {
            this.debug("Filter activated!");
            this.getCore().getFilterManager().addDenyFilter(new MovedTooQuicklyDenyFilter(this));
        } else {
            this.debug("Filter was not activated");
        }
    }

    @Override
    public Messages getMessages() {
        return this.messages;
    }

    public HashMap<String, EndWorldHandler> getWorldHandlers() {
        return this.worldHandlers;
    }

    // API for other nodes

    @Override
    public String getNodeName() {
        return THE_END_AGAIN;
    }

    private void ShowMessage(final CommandSender sender, String message) {
        ShowMessage(sender, message, ChatColor.AQUA);
    }

    private void ShowMessage(final CommandSender sender, String message, ChatColor messageColor)
    {
        // [NTheEndAgain]
        String PREFIX = "§0[§c§lN§6TheEndAgain§0] §f";
        sender.sendMessage(PREFIX + messageColor+ message);
    }

}
