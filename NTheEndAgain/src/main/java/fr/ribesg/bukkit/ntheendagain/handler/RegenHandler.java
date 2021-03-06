/***************************************************************************
 * Project file:    NPlugins - NTheEndAgain - RegenHandler.java            *
 * Full Class name: fr.ribesg.bukkit.ntheendagain.handler.RegenHandler     *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.ntheendagain.handler;

import fr.ribesg.bukkit.ncore.lang.MessageId;
import fr.ribesg.bukkit.ncore.node.world.WorldNode;
import fr.ribesg.bukkit.ntheendagain.Config;
import fr.ribesg.bukkit.ntheendagain.NTheEndAgain;
import fr.ribesg.bukkit.ntheendagain.task.SlowSoftRegeneratorTaskHandler;
import fr.ribesg.bukkit.ntheendagain.world.EndChunk;
import fr.ribesg.bukkit.ntheendagain.world.EndChunks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Ribesg
 */
public class RegenHandler {

    /**
     * The End spawn Location is always [100;50;0]
     */
    private static final int END_SPAWN_CHUNK_X = 100 >> 4;
    /**
     * The End spawn Location is always [100;50;0]
     */
    private static final int END_SPAWN_CHUNK_Z = 0;

    private final EndWorldHandler worldHandler;
    private final NTheEndAgain    plugin;

    public RegenHandler(final EndWorldHandler worldHandler) {
        this.worldHandler = worldHandler;
        this.plugin = worldHandler.getPlugin();
    }

    public void regen() {
        // this.plugin.entering(this.getClass(), "regen");
        Config config = this.worldHandler.getConfig();

        int regenMethod = config.getRegenMethod();
        Boolean regenOuterEndNow = checkRegenOuterEndNow(config);

        this.regen(regenMethod, regenOuterEndNow);

        // this.plugin.exiting(this.getClass(), "regen");
    }

    public void regen(final int regenMethod) {
        Config config = this.worldHandler.getConfig();
        Boolean regenOuterEndNow = checkRegenOuterEndNow(config);
        this.regen(regenMethod, regenOuterEndNow);
    }

    private void regen(final int regenMethod, final Boolean regenOuterEndNow) {
        // this.plugin.entering(this.getClass(), "regen(int, int)");

        this.plugin.debug("Kicking players out of the world/server...");
        this.kickPlayers();

        this.plugin.debug("Schedule regen task in " + EndWorldHandler.KICK_TO_REGEN_DELAY + " ticks");
        Bukkit.getScheduler().runTaskLater(this.worldHandler.getPlugin(), new Runnable() {

            @Override
            public void run() {
                fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.plugin.entering(this.getClass(), "run", "task from regen(int)");

                switch (regenMethod) {
                    case 0:
                        fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.plugin.debug("Hard regen...");
                        fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.hardRegen(false);
                        break;
                    case 1:
                        fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.plugin.debug("Soft regen...");
                        fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.softRegen(regenOuterEndNow, false, false);
                        break;
                    case 2:
                        fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.plugin.debug("Crystal regen...");
                        fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.crystalRegen();
                        break;
                    default:
                        break;
                }

                fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.plugin.exiting(this.getClass(), "run", "task from regen(int)");
            }
        }, EndWorldHandler.KICK_TO_REGEN_DELAY);

        // this.plugin.exiting(this.getClass(), "regen(int, int)");
    }

    public void hardRegenOnStop() {
        this.plugin.entering(this.getClass(), "hardRegenOnStop");

        // Pass true for pluginDisabled
        this.hardRegen(true);

        this.plugin.exiting(this.getClass(), "hardRegenOnStop");
    }

    /*package*/
    void regenThenRespawn(boolean forceSpawn) {
        // this.plugin.entering(this.getClass(), "regenThenRespawn");

        this.regen();

        this.plugin.debug("Scheduling respawn task in " + EndWorldHandler.REGEN_TO_RESPAWN_DELAY + "ticks");
        Bukkit.getScheduler().runTaskLater(this.worldHandler.getPlugin(), new Runnable() {

            @Override
            public void run() {
                fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.plugin.entering(this.getClass(), "run", "task from regenThenRespawn");

                fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.worldHandler.getRespawnHandler().respawnNoRegen(forceSpawn);

                fr.ribesg.bukkit.ntheendagain.handler.RegenHandler.this.plugin.exiting(this.getClass(), "run", "task from regenThenRespawn");
            }
        }, EndWorldHandler.REGEN_TO_RESPAWN_DELAY);

        // this.plugin.exiting(this.getClass(), "regenThenRespawn");
    }

    /**
      * Determine whether the outer end should be regenerated now
      */
    private Boolean checkRegenOuterEndNow(Config config) {

        int regenOuterEnd = config.getRegenOuterEnd();

        switch (regenOuterEnd) {
            case 0:
                return false;
            case 1:
                return true;
            case 2:
                long lastOuterEndRegenTimeMillis = config.getLastOuterEndRegenTime();
                int outerEndRegenHours = config.getOuterEndRegenHours();

                if (outerEndRegenHours < 1) {
                    outerEndRegenHours = 1;
                    this.plugin.debug("outerEndRegenHours cannot be 0 when regenOuterEnd is 2; setting to 1");
                    config.setOuterEndRegenHours(outerEndRegenHours);
                }

                // Compute the number of hours ago that the outer end was last regen'd
                double outerEndRegenElapsedTimeHours = (System.currentTimeMillis() - lastOuterEndRegenTimeMillis) / 1000.0 / 60 / 60;

                if (outerEndRegenElapsedTimeHours > outerEndRegenHours) {
                    this.plugin.debug("Need to regen the outer end: " +
                            this.plugin.formatNumber(outerEndRegenElapsedTimeHours) + " > " + outerEndRegenHours);
                    return true;
                } else {
                    this.plugin.debug("Do not regen the outer end: " +
                            this.plugin.formatNumber(outerEndRegenElapsedTimeHours) + " < " + outerEndRegenHours);
                }

                break;
            default:
                // Unknown mode
                this.plugin.error("Unknown value for regenOuterEnd: " + regenOuterEnd);
                break;
        }

        return false;
    }

    private void hardRegen(final boolean pluginDisabled) {
        // this.plugin.entering(this.getClass(), "hardRegen");

        final NTheEndAgain plugin = this.worldHandler.getPlugin();
        final World endWorld = this.worldHandler.getEndWorld();
        final EndChunks chunks = this.worldHandler.getChunks();

        final String prefix = "[REGEN " + endWorld.getName() + "] ";

        // Never regen the outer end via hard regen; it takes way too long
        boolean regenOuterEndNow = false;
        plugin.info(prefix + "Regenerating end world, central island only (hard) ...");

        plugin.debug("Kicking players out of the world/server...");
        this.kickPlayers();

        plugin.debug("Calling softRegen to set all chunks to toBeRegen...");
        this.softRegen(regenOuterEndNow, pluginDisabled, true);

        final long totalChunks = chunks.size();
        long i = 0, regen = 0;
        long lastTime = System.currentTimeMillis();

        plugin.debug("Starting regeneration...");

        // Increment the regen counts
        IncrementRegenCounts(regenOuterEndNow);

        // Reset the cancelRequested flag
        this.plugin.resetCancelRegenFlag();

        for (final EndChunk c : chunks) {
            if (System.currentTimeMillis() - lastTime > 500) {
                plugin.info(prefix + regen + " chunks regenerated (" + i * 100 / totalChunks + "% done)");
                lastTime = System.currentTimeMillis();
            }
            if (c.hasToBeRegen()) {
                c.cleanCrystalLocations();
                c.resetSavedDragons();
                for (final Entity e : endWorld.getChunkAt(c.getX(), c.getZ()).getEntities()) {
                    if (e.getType() == EntityType.ENDER_DRAGON) {
                        UUID dragonId = e.getUniqueId();
                        plugin.debug("remove EnderDragon, UUID " + dragonId);
                        this.worldHandler.getDragons().remove(dragonId);
                        this.worldHandler.getLoadedDragons().remove(dragonId);
                    }
                    e.remove();
                }
                endWorld.regenerateChunk(c.getX(), c.getZ());
                c.setToBeRegen(false);
                regen++;
            }
            i++;

            if (this.plugin.getCancelRegenFlag()) {
                plugin.info(prefix + "Aborted regeneration (hard)");
                break;
            }
        }
        plugin.info(prefix + "Done.");

        // plugin.exiting(this.getClass(), "hardRegen");
    }

    private void softRegen(
            final boolean regenOuterEndNow,
            final boolean pluginDisabled,
            final boolean hardRegenInProgress) {

        // this.plugin.entering(this.getClass(), "softRegen");

        World endWorld = this.worldHandler.getEndWorld();

        Config config = this.worldHandler.getConfig();
        Boolean verboseLogging = config.getVerboseRegenLogging();

        if (hardRegenInProgress) {
            plugin.debug("using softRegen to find chunks to process via hardRegen");
        } else {
            final String worldName = endWorld.getName();
            final String prefix = "[REGEN " + worldName + "] ";

            if (regenOuterEndNow)
                plugin.info(prefix + "Regenerating end world, including outer islands (soft regen) ...");
            else
                plugin.info(prefix + "Regenerating end world, central island only (soft regen) ...");

            // Increment the regen counts
            IncrementRegenCounts(regenOuterEndNow);

            // Reset the cancelRequested flag
            this.plugin.resetCancelRegenFlag();
        }

        this.plugin.debug("Calling softRegen on chunks...");
        this.worldHandler.getChunks().softRegen(regenOuterEndNow, verboseLogging, this.plugin);

		/*
         * Instantly regen the spawn chunk to prevent NPE when an Entity
		 * tries to teleport here and we regen the chunk on Chunk Load.
		 */
        this.plugin.debug("Regenerating spawn chunks...");
        this.worldHandler.getEndWorld().getChunkAt(END_SPAWN_CHUNK_X, END_SPAWN_CHUNK_Z).load(true);

        if (!pluginDisabled) {
            // Launch Slow Soft Regen task
            this.plugin.debug("Not disabling plugin, launch slow regen task...");
            this.worldHandler.setSlowSoftRegeneratorTaskHandler(new SlowSoftRegeneratorTaskHandler(this.worldHandler));
            this.worldHandler.getSlowSoftRegeneratorTaskHandler().run();
        }

        // this.plugin.exiting(this.getClass(), "softRegen");
    }

    private void crystalRegen() {
        this.plugin.entering(this.getClass(), "crystalRegen");

        World endWorld = this.worldHandler.getEndWorld();
        final String prefix = "[REGEN " + endWorld.getName() + "] ";
        plugin.info(prefix + "Regenerating crystals ...");

        this.worldHandler.getChunks().crystalRegen();

        this.plugin.exiting(this.getClass(), "crystalRegen");
    }

    private void kickPlayers() {
        // this.plugin.entering(this.getClass(), "kickPlayers");

        final Config config = this.worldHandler.getConfig();
        final NTheEndAgain plugin = this.worldHandler.getPlugin();
        final World endWorld = this.worldHandler.getEndWorld();

        switch (config.getRegenAction()) {
            case 0:
                plugin.debug("Kicking players...");
                final String[] lines = plugin.getMessages().get(MessageId.theEndAgain_worldRegenerating);
                final StringBuilder messageBuilder = new StringBuilder(lines[0]);
                for (int i = 1; i < lines.length; i++) {
                    messageBuilder.append('\n');
                    messageBuilder.append(lines[i]);
                }
                final String message = messageBuilder.toString();
                for (final Player p : endWorld.getPlayers()) {
                    p.kickPlayer(message);
                }
                break;
            case 1:
                plugin.debug("Teleporting players...");
                final World world = Bukkit.getWorlds().get(0);
                final WorldNode worldNode = plugin.getCore().getWorldNode();
                final Location spawnLoc;
                if (worldNode == null) {
                    spawnLoc = world.getSpawnLocation();
                } else {
                    spawnLoc = worldNode.getWorldSpawnLocation(world.getName());
                }
                //for (final Player p : endWorld.getPlayers()) {
                for (final Player player : Bukkit.getOnlinePlayers()) {
	                if(player.getWorld().getEnvironment() == World.Environment.THE_END)
	                {
		                player.teleport(spawnLoc);
	                }
                    plugin.sendMessage(player, MessageId.theEndAgain_worldRegenerating);
                }
                break;
            case 2:
                // Do nothing
                break;
            default:
                throw new IllegalStateException("Invalid configuration value regenAction found at Runtime, please report this!");
        }

        // plugin.exiting(this.getClass(), "kickPlayers");
    }

    private void IncrementRegenCounts(Boolean regenOuterEndNow) {
        Config config = this.worldHandler.getConfig();
        int newCentralEndRegenCount = config.getCentralEndRegenCount() + 1;

        plugin.debug("Updating centralEndRegenCount to " + newCentralEndRegenCount);
        config.setCentralEndRegenCount(newCentralEndRegenCount);

        if (regenOuterEndNow) {
            int newOuterEndRegenCount = config.getOuterEndRegenCount() + 1;
            plugin.debug("Updating centralEndRegenCount to " + newOuterEndRegenCount);
            config.setOuterEndRegenCount(newOuterEndRegenCount);

            config.setLastOuterEndRegenTime(System.currentTimeMillis(), "IncrementRegenCounts");
        }

        try {
            this.worldHandler.saveConfig();
        } catch (final IOException e) {
            this.plugin.getLogger().severe("An error occured, stacktrace follows:");
            e.printStackTrace();
            this.plugin.getLogger().severe("This error occurred when NTheEndAgain.IncrementRegenCounts tried to save " + e.getMessage() + ".yml");
        }
    }
}
