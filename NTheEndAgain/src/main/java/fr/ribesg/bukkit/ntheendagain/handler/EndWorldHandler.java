/***************************************************************************
 * Project file:    NPlugins - NTheEndAgain - EndWorldHandler.java         *
 * Full Class name: fr.ribesg.bukkit.ntheendagain.handler.EndWorldHandler  *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.ntheendagain.handler;

import fr.ribesg.bukkit.ncore.util.StringUtil;
import fr.ribesg.bukkit.ntheendagain.Config;
import fr.ribesg.bukkit.ntheendagain.NTheEndAgain;
import fr.ribesg.bukkit.ntheendagain.task.RegenTask;
import fr.ribesg.bukkit.ntheendagain.task.RespawnTask;
import fr.ribesg.bukkit.ntheendagain.task.SlowSoftRegeneratorTaskHandler;
import fr.ribesg.bukkit.ntheendagain.task.UnexpectedDragonDeathHandlerTask;
import fr.ribesg.bukkit.ntheendagain.world.EndChunk;
import fr.ribesg.bukkit.ntheendagain.world.EndChunks;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;

public class EndWorldHandler {

    // 20 ticks (1 second)
    static final long KICK_TO_REGEN_DELAY    = 20L;

    // 100 ticks (5 seconds)
    static final long REGEN_TO_RESPAWN_DELAY = KICK_TO_REGEN_DELAY + 100L;

    private final String camelCaseWorldName;

    private final NTheEndAgain                   plugin;
    private final World                          endWorld;
    private final EndChunks                      chunks;
    private final Config                         config;
    private final Map<UUID, Map<String, Double>> dragons;
    private final Set<UUID>                      loadedDragons;
    private final Set<BukkitTask>                tasks;
    private final RespawnHandler                 respawnHandler;
    private final RegenHandler                   regenHandler;

    private SlowSoftRegeneratorTaskHandler slowSoftRegeneratorTaskHandler;

    /**
     * Class constructor
     * - Initialize all variables
     * <p/>
     * First thing to do after call to constructor is config load
     *
     * @param instance the Plugin instance
     * @param world    the related World
     */
    public EndWorldHandler(final NTheEndAgain instance, final World world) {
        this.plugin = instance;
        this.endWorld = world;
        this.camelCaseWorldName = StringUtil.toLowerCamelCase(this.endWorld.getName());
        this.chunks = new EndChunks(this, world.getName());
        this.config = new Config(this.plugin, this.endWorld.getName());
        this.dragons = new HashMap<>();
        this.loadedDragons = new HashSet<>();
        this.tasks = new HashSet<>();
        this.respawnHandler = new RespawnHandler(this);
        this.regenHandler = new RegenHandler(this);
        this.slowSoftRegeneratorTaskHandler = null;

        // Config is not yet loaded here
    }

    public void loadConfig() throws IOException, InvalidConfigurationException {
        this.config.loadConfig(this.camelCaseWorldName + "Config.yml");
    }

    public void loadChunks() {
        this.chunks.load(this.plugin.getConfigFilePath(this.camelCaseWorldName + "Chunks"));
    }

    public void saveConfig() throws IOException {
        this.config.writeConfig(this.camelCaseWorldName + "Config.yml");
    }

    public void saveChunks() throws IOException {
        this.chunks.write(this.plugin.getConfigFilePath(this.camelCaseWorldName + "Chunks"));
    }

    public void initLater() {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> EndWorldHandler.this.init(), 1L);
    }

    /**
     * Post-config-load initialization method
     * - Count existing EDs
     * - Respawn Dragons if needed
     * - Schedule respawn and regen tasks
     */
    public void init() {
        // Config is now loaded
    	if(this.endWorld.getEnvironment() == Environment.THE_END) {
            this.countEntities();

            if (this.config.getRespawnType() == 3) {
                // Respawn the dragons now, but do not regenerate the chunks
                this.respawnHandler.respawnNoRegen();
            } /*
               * Respawn Type 6 (deprecated)
               else if (this.config.getRespawnType() == 6) {
                if (this.config.getNextRespawnTaskTime() > System.currentTimeMillis()) {
                    this.tasks.add(Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {

                        @Override
                        public void run() {
                            fr.ribesg.bukkit.ntheendagain.handler.EndWorldHandler.this.respawnHandler.respawn();
                        }
                    }, this.config.getNextRespawnTaskTime() / 1000 * 20));
                } else {
                    this.respawnHandler.respawn();
                }
            }
            */

            // Create the background tasks used by this handler
            createTasks();
    	}
    }

    /**
     * To be called in plugin's onDisable() method or
     * when the world is unloaded.
     * - Cancel all tasks
     * - Hard regen if needed and if plugin disable
     * - Make scheduled tasks persistent
     * - Save configs
     */
    public void unload(final boolean pluginDisabled) throws InvalidConfigurationException {
        this.cancelTasks();
        if (pluginDisabled && this.config.getHardRegenOnStop() == 1) {
            this.regenHandler.hardRegenOnStop();
        }
        try {
            // Reload-friendly lastExecTime storing in config file
            final long nextRegenExecTime = this.config.getNextRegenTaskTime();
            final long nextRespawnExecTime = this.config.getNextRespawnTaskTime();
            this.loadConfig();

            this.config.setNextRegenTaskTime(this.config.getRegenTimer() == 0 ? 0 : nextRegenExecTime);
            this.config.setNextRespawnTaskTime(this.config.getRespawnTimerMax() == 0 ? 0 : nextRespawnExecTime);

            int regenOuterEnd = this.config.getRegenOuterEnd();
            switch (regenOuterEnd) {
                case 0:
                case 1:
                    this.config.setNextOuterEndRegenTime(0);
                    break;
                case 2:
                    if (this.config.getNextOuterEndRegenTime() <= 0) {
                        int outerEndRegenHours = this.config.getOuterEndRegenHours();
                        if (outerEndRegenHours < 1) {
                            outerEndRegenHours = 1;
                            this.plugin.debug("outerEndRegenHours cannot be 0 when regenOuterEnd is 2; setting to 1");
                            config.setOuterEndRegenHours(outerEndRegenHours);
                        }
                        this.config.setNextOuterEndRegenTime(System.currentTimeMillis() + outerEndRegenHours * 60L * 60L * 1000L);
                    }
                    break;
                default:
                    // Unknown mode
                    this.plugin.error("Unknown value for regenOuterEnd: " + regenOuterEnd);
                    break;
            }

            this.saveConfig();
        } catch (final IOException e) {
            this.plugin.getLogger().severe("An error occured, stacktrace follows:");
            e.printStackTrace();
            this.plugin.getLogger().severe("This error occured when NTheEndAgain tried to save " + e.getMessage() + ".yml");
        }
        try {
            this.saveChunks();
        } catch (final IOException e) {
            this.plugin.getLogger().severe("An error occured, stacktrace follows:");
            e.printStackTrace();
            this.plugin.getLogger().severe("This error occured when NTheEndAgain tried to save " + e.getMessage() + ".yml");
            this.plugin.getLogger().severe("/!\\ THIS MEANS THAT PROTECTED CHUNKS COULD BE REGENERATED ON NEXT REGEN IN THIS WORLD /!\\");
        }
    }

    public void cancelTasks() {
        for (final BukkitTask t : this.tasks) {
            t.cancel();
        }
        this.tasks.clear();
    }

	/**
     * This method will remove any existing tasks then create new ones
     */
    public void recreateTasksLater() {

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> EndWorldHandler.this.recreateTasks(), 10L);

    }

    private void recreateTasks() {

        int existingTaskCount = this.tasks.size();
        if (this.tasks.size() > 0) {
            if (existingTaskCount == 1)
                plugin.debug("Cancelling 1 existing task");
            else
                plugin.debug("Cancelling " + existingTaskCount + " existing tasks");
            cancelTasks();
        }

        createTasks();
    }

    /**
     * Create the background tasks used by this handler
     */
    private void createTasks() {
        int respawnType = this.config.getRespawnType();
        int respawnTimerMin = this.config.getRespawnTimerMin();
        int respawnTimerMax = this.config.getRespawnTimerMax();

        int regenTimer = this.config.getRegenTimer();
        int regenType = this.config.getRegenType();

        this.tasks.add(new UnexpectedDragonDeathHandlerTask(this).schedule(this.plugin));

        if (respawnTimerMax != 0 && (respawnType == 4 || respawnType == 5)) {
            // Create a persistent respawn task
            // 4: persistent after boot/load
            // 5: persistent through reboots/reloads
            plugin.debug("Creating RespawnTask, type " + respawnType + ", TimerRange " + respawnTimerMin + " to " + respawnTimerMax);
            this.tasks.add(new RespawnTask(this).schedule(this.plugin));
        }

        if (regenTimer != 0 && (regenType == 2 || regenType == 3)) {
            plugin.debug("Creating RegenTask, type " + regenType + ", Timer " + regenTimer);
            this.tasks.add(new RegenTask(this).schedule(this.plugin));
        }

    }

    /**
     * Counts:
     * - EnderDragons
     * - EnderCrystals
     */
    private void countEntities() {
    	if(this.endWorld.getEnvironment() == Environment.THE_END){
    	this.plugin.getLogger().info("Counting existing EDs in " + this.endWorld.getName() + "...");
        for (final EndChunk c : this.chunks.getSafeChunksList()) {
            if (this.endWorld.isChunkLoaded(c.getX(), c.getZ())) {
                final Chunk chunk = this.endWorld.getChunkAt(c.getX(), c.getZ());
                for (final Entity e : chunk.getEntities()) {
                    if (e.getType() == EntityType.ENDER_DRAGON) {
                        final EnderDragon ed = (EnderDragon)e;
                        UUID dragonId = ed.getUniqueId();

                        if (!this.dragons.containsKey(dragonId)) {
                            int initialHealth = this.config.getEdHealth();
                            plugin.debug("EndWorldHandler.countEntities ... found EnderDragon, UUID " + dragonId + ", health " + initialHealth);

                            ed.setMaxHealth(initialHealth);
                            ed.setHealth(ed.getMaxHealth());

                            // plugin.debug("EndWorldHandler.countEntities ... actual health is " + new DecimalFormat("#.##").format(ed.getHealth()));

                            this.dragons.put(dragonId, new HashMap<>());
                            this.loadedDragons.add(dragonId);
                        }
                    } else if (e.getType() == EntityType.ENDER_CRYSTAL) {
                        c.addCrystalLocation(e);
                    }
                }
            } else {
                this.endWorld.loadChunk(c.getX(), c.getZ());
                c.resetSavedDragons();
                this.endWorld.unloadChunkRequest(c.getX(), c.getZ());
            }
        }
        this.plugin.getLogger().info("Done, " + this.getNumberOfAliveEnderDragons() + " EnderDragon(s) found.");
    	}
    }

    /**
     * Called when a Player hits an EnderDragon
     *
     * @param enderDragonID the EnderDragon's ID
     * @param playerName    the Player's name
     * @param dmg           the amount of damages done
     */
    public void playerHitED(final UUID enderDragonID, final String playerName, final double dmg) {
        final Map<String, Double> dragonMap;
        if (!this.dragons.containsKey(enderDragonID)) {
            dragonMap = new HashMap<>();
            this.dragons.put(enderDragonID, dragonMap);
        } else {
            dragonMap = this.dragons.get(enderDragonID);
        }
        if (dragonMap.containsKey(playerName)) {
            dragonMap.put(playerName, dragonMap.get(playerName) + dmg);
        } else {
            dragonMap.put(playerName, dmg);
        }
    }

    public EndChunks getChunks() {
        return this.chunks;
    }

    public Config getConfig() {
        return this.config;
    }

    public Map<UUID, Map<String, Double>> getDragons() {
        return this.dragons;
    }

    public World getEndWorld() {
        return this.endWorld;
    }

    public Set<UUID> getLoadedDragons() {
        return this.loadedDragons;
    }

    public int getNumberOfAliveEnderDragons() {
        return this.loadedDragons.size() + this.chunks.getTotalSavedDragons();
    }

    public NTheEndAgain getPlugin() {
        return this.plugin;
    }

    public Set<BukkitTask> getTasks() {
        return this.tasks;
    }

    public RespawnHandler getRespawnHandler() {
        return this.respawnHandler;
    }

    public RegenHandler getRegenHandler() {
        return this.regenHandler;
    }

    public String getCamelCaseWorldName() {
        return this.camelCaseWorldName;
    }

    public SlowSoftRegeneratorTaskHandler getSlowSoftRegeneratorTaskHandler() {
        return this.slowSoftRegeneratorTaskHandler;
    }

    public void setSlowSoftRegeneratorTaskHandler(final SlowSoftRegeneratorTaskHandler slowSoftRegeneratorTaskHandler) {
        this.slowSoftRegeneratorTaskHandler = slowSoftRegeneratorTaskHandler;
    }
}
