/***************************************************************************
 * Project file:    NPlugins - NTheEndAgain - ChunkListener.java           *
 * Full Class name: fr.ribesg.bukkit.ntheendagain.listener.ChunkListener   *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.ntheendagain.listener;

import fr.ribesg.bukkit.ncore.event.theendagain.ChunkRegenEvent;
import fr.ribesg.bukkit.ncore.util.StringUtil;
import fr.ribesg.bukkit.ntheendagain.Config;
import fr.ribesg.bukkit.ntheendagain.NTheEndAgain;
import fr.ribesg.bukkit.ntheendagain.handler.EndWorldHandler;
import fr.ribesg.bukkit.ntheendagain.world.EndChunk;
import fr.ribesg.bukkit.ntheendagain.world.EndChunks;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World.Environment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Handles Chunk Load and Unload events
 *
 * @author Ribesg
 */
public class ChunkListener implements Listener {

    private final int MESSAGE_INTERVAL_MILLIS = 5000;
    private final int STALE_CHUNK_INTERVAL_MILLIS = 10000;

    private final NTheEndAgain plugin;

    private Boolean lastChunkCoordsValid = false;
    private int lastChunkCoordX;
    private int lastChunkCoordZ;
    private long lastChunkRegenTime = System.currentTimeMillis() - STALE_CHUNK_INTERVAL_MILLIS;

    private long lastInfoTime = System.currentTimeMillis() - MESSAGE_INTERVAL_MILLIS * 2;

    // This is a count of the chunks regenerated during the current regen task
	// It gets reset if a chunk regen event is more than 10 seconds since the previous event
    private int chunksRegenerated = 0;

    public ChunkListener(final NTheEndAgain instance) {
        this.plugin = instance;
    }

    /**
     * Handles Chunk regen at load, with still-alive EnderDragons consideration,
     * and EnderDragon spawn / load on Chunk Load.
     *
     * @param event a Chunk Load Event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEndChunkLoad(final ChunkLoadEvent event) {

        // Note: enable debug messages in game by having both NCore.jar and NTheEndAgain.jar
        // in the plugins folder, then issue command /debug enable NTheEndAgain

        if (event.getWorld().getEnvironment() == Environment.THE_END) {
            final String worldName = event.getWorld().getName();
            final EndWorldHandler handler = this.plugin.getHandler(StringUtil.toLowerCamelCase(worldName));
            if (handler != null) {

                long currentTime = System.currentTimeMillis();

                final EndChunks chunks = handler.getChunks();
                final Chunk chunk = event.getChunk();
                EndChunk endChunk = chunks.getChunk(worldName, chunk.getX(), chunk.getZ());

                /*
                 * Chunk has to be regen
                 *   - Forget every dragon in it
                 *   - Regenerate the chunk
                 *   - Schedule a refresh
                 */
                if (endChunk != null && endChunk.hasToBeRegen()) {

                    int chunkX = endChunk.getX();
                    int chunkZ = endChunk.getZ();

                    if (lastChunkCoordsValid) {
                        if (chunkX == lastChunkCoordX && chunkZ == lastChunkCoordZ) {
                            if (lastChunkRegenTime < currentTime + 5000) {
                                // Regen of this chunk was initiated less than 5 seconds ago
                                // Exit this function to avoid an endless loop and stack overflow
                                // this.plugin.debug(" ... skip regen of chunk at " + chunkX + ", " + chunkZ);
                                return;
                            }
                            this.plugin.debug(" ... repeating regen of chunk at " + chunkX + ", " + chunkZ);
                        }
                    }

                    if (currentTime > lastChunkRegenTime + STALE_CHUNK_INTERVAL_MILLIS)
                        chunksRegenerated = 0;
                    else
                        chunksRegenerated++;

                    // Keep track of the last chunk regenerated
                    lastChunkCoordsValid = true;
                    lastChunkCoordX = chunkX;
                    lastChunkCoordZ = chunkZ;
                    lastChunkRegenTime = currentTime;

                    Config config = handler.getConfig();
                    Boolean verboseLogging = config.getVerboseRegenLogging();
                    String regenMessage = " ... regen chunk at " + chunkX + ", " + chunkZ +
                            " (" + chunksRegenerated + " chunks regenerated)";

                    if (verboseLogging) {
                        if (this.plugin.isDebugEnabled())
                            this.plugin.debug(regenMessage);
                        else
                            this.plugin.log(Level.INFO, regenMessage);
                    } else if (currentTime > lastInfoTime + MESSAGE_INTERVAL_MILLIS) {
                        // Only show this message every 5 seconds
                        this.plugin.debug(regenMessage);
                        lastInfoTime = currentTime;
                    }

                    final ChunkRegenEvent regenEvent = new ChunkRegenEvent(chunk);
                    Bukkit.getPluginManager().callEvent(regenEvent);
                    if (!regenEvent.isCancelled()) {

                        // this.plugin.debug(" ..... examine entities");

                        for (final Entity e : chunk.getEntities()) {
                            if (e.getType() == EntityType.ENDER_DRAGON) {
                                final EnderDragon ed = (EnderDragon)e;
                                UUID dragonId = ed.getUniqueId();
                                this.plugin.debug(" ... remove EnderDragon, UUID " + dragonId);

                                if (handler.getDragons().containsKey(dragonId)) {
                                    handler.getDragons().remove(dragonId);
                                    handler.getLoadedDragons().remove(dragonId);
                                }
                            }
                            e.remove();
                        }

                        // this.plugin.debug(" ..... remove crystals");
                        endChunk.cleanCrystalLocations();
                        final int x = endChunk.getX(), z = endChunk.getZ();

                        // this.plugin.debug(" ..... regenerate chunk now");
                        event.getWorld().regenerateChunk(x, z);

                        Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {

                            @Override
                            public void run() {
                                // Note that .refreshChunk() is deprecated with explanation
                                // "This method is not guaranteed to work suitably across all client implementations"
                                event.getWorld().refreshChunk(x, z);
                            }
                        }, 100L);

                    }

                    // this.plugin.debug(" ..... setToBeRegen(false)");
                    endChunk.setToBeRegen(false);
                }

                /*
                 * Chunk does not need to be regen
                 *   - Check if we knew this chunk, if not, now we do
                 *   - Check for new EnderDragons
                 *   - Re-add known Dragons to Loaded set
                 */
                else {
                    if (endChunk == null) {
                        endChunk = chunks.addChunk(chunk);
                    }

                    // this.plugin.debug(" ... chunk regen not required at " + endChunk.getX() + ", " + endChunk.getZ());

                    for (final Entity e : chunk.getEntities()) {

                        // this.plugin.debug(" ..... examine entities");
                        if (e.getType() == EntityType.ENDER_DRAGON && event.getWorld().getEnvironment() == Environment.THE_END) {

                            final EnderDragon ed = (EnderDragon)e;
                            UUID dragonId = ed.getUniqueId();

                            this.plugin.debug("onEndChunkLoad ... found EnderDragon, UUID " + dragonId);

                            if (!handler.getDragons().containsKey(dragonId)) {

                                int initialHealth = handler.getConfig().getEdHealth();
                                this.plugin.debug("onEndChunkLoad ... add EnderDragon, UUID " + dragonId + ", health " + initialHealth);

                                ed.setMaxHealth(initialHealth);
                                ed.setHealth(ed.getMaxHealth());

                                // plugin.debug("onEndChunkLoad ... actual health is " + new DecimalFormat("#.##").format(ed.getHealth()));

                                handler.getDragons().put(dragonId, new HashMap<String, Double>());
                            }
                            handler.getLoadedDragons().add(dragonId);

                        } else if (e.getType() == EntityType.ENDER_CRYSTAL) {

                            this.plugin.debug("onEndChunkLoad ... found crystal at " +
                                    e.getLocation().getX() + ", " + e.getLocation().getY() + ", " + e.getLocation().getZ());
                            endChunk.addCrystalLocation(e);
                        }
                    }
                }

                endChunk.resetSavedDragons();
            }
        }
    }

    /**
     * Remove the unloaded EnderDragons from the loaded set
     *
     * @param event a Chunk Unload Event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEndChunkUnload(final ChunkUnloadEvent event) {
        if (event.getWorld().getEnvironment() == Environment.THE_END) {
            final String worldName = event.getWorld().getName();
            final EndWorldHandler handler = this.plugin.getHandler(StringUtil.toLowerCamelCase(worldName));
            if (handler != null) {
                EndChunk chunk = handler.getChunks().getChunk(event.getChunk());
                if (chunk == null) {
                    chunk = handler.getChunks().addChunk(event.getChunk());
                }
                for (final Entity e : event.getChunk().getEntities()) {
                    if (e.getType() == EntityType.ENDER_DRAGON) {
                        final EnderDragon ed = (EnderDragon)e;
                        UUID dragonId = ed.getUniqueId();
                        this.plugin.debug("onEndChunkUnload ... remove EnderDragon, UUID " + dragonId);

                        handler.getLoadedDragons().remove(dragonId);
                        chunk.incrementSavedDragons();
                    }
                }
            }
        }
    }
}
