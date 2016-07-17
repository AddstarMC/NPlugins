/***************************************************************************
 * Project file:    NPlugins - NTheEndAgain - EndChunks.java               *
 * Full Class name: fr.ribesg.bukkit.ntheendagain.world.EndChunks          *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.ntheendagain.world;

import fr.ribesg.bukkit.ncore.common.ChunkCoord;
import fr.ribesg.bukkit.ntheendagain.Config;
import fr.ribesg.bukkit.ntheendagain.NTheEndAgain;
import fr.ribesg.bukkit.ntheendagain.handler.EndWorldHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class EndChunks implements Iterable<EndChunk> {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private final EndWorldHandler handler;

    private final String worldName;

    private final HashMap<ChunkCoord, EndChunk> chunks;

    private int totalSavedDragons;

    public EndChunks(final EndWorldHandler handler, final String worldName) {
        this.chunks = new HashMap<>();
        this.totalSavedDragons = 0;
        this.handler = handler;
        this.worldName = worldName;
    }

    public EndChunk addChunk(final Chunk bukkitChunk) {
        final EndChunk res = new EndChunk(this, bukkitChunk);
        res.setProtected(this.handler.getConfig().getDefaultProtected());
        this.addChunk(res);
        return res;
    }

    private void addChunk(final EndChunk endChunk) {
        this.checkWorld(endChunk.getWorldName());
        this.chunks.put(endChunk.getCoords(), endChunk);
    }

    public EndChunk getChunk(final String world, final int x, final int z) {
        this.checkWorld(world);
        return this.get(new ChunkCoord(x, z, world));
    }

    public EndChunk getChunk(final Chunk bukkitChunk) {
        this.checkWorld(bukkitChunk.getWorld().getName());
        return this.get(new ChunkCoord(bukkitChunk));
    }

    private EndChunk get(final ChunkCoord coord) {
        EndChunk res = this.chunks.get(coord);
        if (res == null) {
            res = new EndChunk(this, coord);
            res.setProtected(this.handler.getConfig().getDefaultProtected());
            this.chunks.put(res.getCoords(), res);
        }
        return res;
    }

    private void checkWorld(final String worldName) {
        if (!worldName.equals(this.worldName)) {
            throw new IllegalArgumentException("Wrong world, this EndChunks object handles world \"" + this.worldName + "\", " +
                                               "not world \"" + worldName + '"');
        }
    }

    /**
     * Forget all saved dragons currently being tracked by EndChunks
     * Note that dragons are added to an EndChunk when it is unloaded,
     * but they are removed from the chunk when it is re-loaded
     */
    public void forgetAllDragons(CommandSender sender, boolean checkOuterEnd, NTheEndAgain plugin) {
        int dragonsRemoved = 0;
        for (final EndChunk ec : this) {
            if (!checkOuterEnd) {
                if (Math.abs(ec.getX()) >= 30 || Math.abs(ec.getZ()) >= 30 ) {
                    // Skip this outer end chunk
                    continue;
                }
            }
            int dragonCount = ec.getSavedDragons();
            if (dragonCount > 0) {
                if (dragonCount > 1)
                    plugin.info("Removing " + dragonCount + " saved dragons from chunk " + ec.getCoordsString(false));
                else
                    plugin.info("Removing 1 saved dragon from chunk " + ec.getCoordsString(false));

                ec.resetSavedDragons();

                dragonsRemoved += dragonCount;
            }
        }

        String msg;
        if (dragonsRemoved == 0) {
            msg = "Did not find any saved dragons to remove from unloaded chunks";
        } else {
            msg = "Removed " + dragonsRemoved + " saved dragon(s) from unloaded chunks";
        }

        plugin.debug(msg);
        plugin.showMessage(sender, msg);

    }

    public void softRegen(Boolean regenOuterEnd, Boolean verboseLogging, NTheEndAgain plugin) {

        for (final EndChunk ec : this) {
            int chunkX = ec.getX();
            int chunkZ = ec.getZ();

            if (regenOuterEnd) {

                if (Math.abs(chunkX) > plugin.MAX_TRACKED_CHUNK_X ||
                    Math.abs(chunkZ) > plugin.MAX_TRACKED_CHUNK_Z) {
                    if (chunkX % 10 == 0 && chunkZ % 10 == 0)
                        plugin.debug(" ... skip outer island chunk at " + ec.getCoordsString(false) + " (outside RegenCoord limits)");
                    continue;
                }

            } else {
                if (Math.abs(chunkX) >= 30 || Math.abs(chunkZ) >= 30 ) {
                    // Skip this outer end chunk
                    if (verboseLogging && chunkX % 10 == 0 && chunkZ % 10 == 0)
                        plugin.debug(" ... skip outer island chunk at " + ec.getCoordsString(false) + " (skipping all)");
                    continue;
                }
            }
            ec.setToBeRegen(!ec.isProtected());
        }
    }

    public void crystalRegen() {
        Chunk bukkitChunk;
        for (final EndChunk c : this.chunks.values()) {
            if (c.containsCrystal()) {
                final World w = Bukkit.getWorld(c.getWorldName());
                bukkitChunk = w.getChunkAt(c.getX(), c.getZ());
                final Set<Location> locs = c.getCrystalLocations();
                for (final Entity e : bukkitChunk.getEntities()) {
                    if (e.getType() == EntityType.ENDER_CRYSTAL) {
                        e.remove();
                    }
                }
                for (final Location loc : locs) {
                    w.spawnEntity(loc.clone().add(0, -1, 0), EntityType.ENDER_CRYSTAL);
                    loc.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
                }
            }
        }
    }

    public void load(final Path pathEndChunks) {
        if (Files.exists(pathEndChunks)) {
            final YamlConfiguration config = new YamlConfiguration();
            try (BufferedReader reader = Files.newBufferedReader(pathEndChunks, CHARSET)) {
                final StringBuilder s = new StringBuilder();
                while (reader.ready()) {
                    s.append(reader.readLine()).append('\n');
                }
                config.loadFromString(s.toString());
            } catch (final Exception e) {
                e.printStackTrace();
            }

            for (final String chunkCoordString : config.getKeys(false)) {
                final ConfigurationSection sec = config.getConfigurationSection(chunkCoordString);
                if (sec != null) {
                    final EndChunk ec = EndChunk.rebuild(this, sec);
                    this.addChunk(ec);
                }
            }
        }
    }

    public void write(final Path pathEndChunks) throws IOException {
        if (!Files.exists(pathEndChunks)) {
            Files.createFile(pathEndChunks);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(pathEndChunks, CHARSET, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            final YamlConfiguration config = new YamlConfiguration();
            for (final EndChunk c : this.chunks.values()) {
                c.store(config);
            }
            writer.write(config.saveToString());
        }
    }

    @Override
    public Iterator<EndChunk> iterator() {
        return this.chunks.values().iterator();
    }

    public int size() {
        return this.chunks.size();
    }

    public List<EndChunk> getSafeChunksList() {
        final List<EndChunk> result = new ArrayList<>();
        result.addAll(this.chunks.values());
        return result;
    }

    public int getTotalSavedDragons() {
        return this.totalSavedDragons;
    }

    /*package*/ void incrementTotalSavedDragons() {
        this.totalSavedDragons++;
    }

    /*package*/ void decrementTotalSavedDragons(final int quantity) {
        this.totalSavedDragons -= quantity;
        if (this.totalSavedDragons < 0) {
            this.totalSavedDragons = 0;
        }
    }
}
