/***************************************************************************
 * Project file:    NPlugins - NTheEndAgain - EnderDragonListener.java     *
 * Full Class name: fr.ribesg.bukkit.ntheendagain.listener.EnderDragonListener
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.ntheendagain.listener;

import fr.ribesg.bukkit.ncore.common.collection.pairlist.Pair;
import fr.ribesg.bukkit.ncore.event.theendagain.XPDistributionEvent;
import fr.ribesg.bukkit.ncore.lang.MessageId;
import fr.ribesg.bukkit.ncore.util.StringUtil;
import fr.ribesg.bukkit.ntheendagain.Config;
import fr.ribesg.bukkit.ntheendagain.NTheEndAgain;
import fr.ribesg.bukkit.ntheendagain.handler.EndWorldHandler;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.inventory.ItemStack;

/**
 * Handles EnderDragons spawn, health regain and death
 *
 * @author Ribesg
 */
public class EnderDragonListener implements Listener {

    /**
     * Players that did less than threshold % of total damages
     * have no chance to receive the Egg with custom handling
     */
    private static final float         THRESHOLD = 0.15f;
    private static final Random        RANDOM    = new Random();
    private static final DecimalFormat FORMAT    = new DecimalFormat("#0.00");

    private final NTheEndAgain plugin;

    public EnderDragonListener(final NTheEndAgain instance) {
        this.plugin = instance;
    }

    /**
     * Handles custom XP handling on ED death
     *
     * @param event an EntityDeathEvent
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEnderDragonDeath(final EntityDeathEvent event) {

        // Note: enable debug messages in game by having both NCore.jar and NTheEndAgain.jar
        // in the plugins folder, then issue command /debug enable NTheEndAgain

        if (event.getEntityType() != EntityType.ENDER_DRAGON) {
            return;
        }

        this.plugin.entering(this.getClass(), "onEnderDragonDeath");

        final World endWorld = event.getEntity().getWorld();
        final EndWorldHandler handler = this.plugin.getHandler(StringUtil.toLowerCamelCase(endWorld.getName()));
        if (handler == null) {
            this.plugin.debug(" ... no handler for EnderDragon death in world " + endWorld.getName());
            return;
        }

        this.plugin.debug(" ... handling EnderDragon death in world " + endWorld.getName());

        final Config config = handler.getConfig();

        /* Compute damages */

        final Map<UUID, Map<String, Double>> dragonInfo = handler.getDragons();

        final HashMap<String, Double> dmgMap;
        try {
            dmgMap = new HashMap<>(dragonInfo.get(event.getEntity().getUniqueId()));
        } catch (final NullPointerException e) {
            // Dragon not found
            this.plugin.error("Dragon info not found for UUID " + event.getEntity().getUniqueId() + "; " +
                    "cannot award XP or trigger regen / respawn tasks");
            return;
        }

        // We ignore offline players
        final Iterator<Entry<String, Double>> dmgMapIterator = dmgMap.entrySet().iterator();
        while (dmgMapIterator.hasNext()) {
            final Entry<String, Double> e = dmgMapIterator.next();
            if (this.plugin.getServer().getPlayerExact(e.getKey()) == null) {
                dmgMapIterator.remove();
            }
        }

        // Get total damages done to the ED by Online players
        double totalDamagesXp = 0;
        for (final double v : dmgMap.values()) {
            totalDamagesXp += v;
        }

        // Create map of damages percentages
        final Map<String, Float> dmgPercentageMap = new HashMap<>();
        for (final Entry<String, Double> entry : dmgMap.entrySet()) {
            dmgPercentageMap.put(entry.getKey(), (float)(entry.getValue() / totalDamagesXp));
        }

        /* XP Handling */

        switch (config.getEdExpHandling()) {
            case 0:
                int xpAmount = config.getEdExpReward();
                this.plugin.debug(" ... explicitly setting XP drop from EnderDragon to " + xpAmount);
                event.setDroppedExp(xpAmount);
                break;
            case 1:
                this.plugin.debug(" ... cancelling XP drop from EnderDragon, then manually distributing XP");
                event.setDroppedExp(1);

                // Create map of XP to give
                final Map<String, Integer> xpMap = new HashMap<>(dmgMap.size());
                for (final Entry<String, Float> entry : dmgPercentageMap.entrySet()) {
                    final int reward = (int)(config.getEdExpReward() * dmgPercentageMap.get(entry.getKey()));
                    xpMap.put(entry.getKey(), Math.min(reward, config.getEdExpReward()));
                }

                // Call event for external plugins to be able to play with this map
                final XPDistributionEvent xpDistributionEvent = new XPDistributionEvent(xpMap, config.getEdExpReward());
                Bukkit.getPluginManager().callEvent(xpDistributionEvent);

                if (!xpDistributionEvent.isCancelled()) {
                    // Give exp to players
                    for (final Entry<String, Integer> entry : xpDistributionEvent.getXpMap().entrySet()) {
                        final Player p = this.plugin.getServer().getPlayerExact(entry.getKey());
                        p.giveExp(entry.getValue());
                        this.plugin.sendMessage(p, MessageId.theEndAgain_receivedXP, Integer.toString(entry.getValue()));
                        this.plugin.debug(" ... gave player " + p.getDisplayName() + " " + entry.getValue() + " XP");
                    }
                }
                break;
            default:
                break;
        }

        // dropTableHandling:
        // 0: Stock. Drops will just fall from the EnderDragon death Location
        // 1: Distribution. Drops will be distributed exactly like the DragonEgg

        if (config.getDropTableHandling() == 0) {
            final Location loc = event.getEntity().getLocation();
            for (final Pair<ItemStack, Float> pair : config.getDropTable()) {
                final ItemStack is = pair.getKey().clone();
                is.setAmount(1);
                for (int i = 0; i < pair.getKey().getAmount(); i++) {
                    if (RANDOM.nextFloat() <= pair.getValue()) {
                        endWorld.dropItemNaturally(loc, is);
                        this.plugin.debug(" ... Dropping item " + is.toString() +
                                " at " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ());
                    }
                }
            }
        }

        final MessageId playerKilled, playersKilled, playersKilledLine;
        if (config.getRespawnNumber() == 1) {
            playerKilled = MessageId.theEndAgain_playerKilledTheDragon;
            playersKilled = MessageId.theEndAgain_playersKilledTheDragon;
            playersKilledLine = MessageId.theEndAgain_playersKilledTheDragon_line;
        } else {
            playerKilled = MessageId.theEndAgain_playerKilledADragon;
            playersKilled = MessageId.theEndAgain_playersKilledADragon;
            playersKilledLine = MessageId.theEndAgain_playersKilledADragon_line;
        }
        if (dmgPercentageMap.size() == 1) {
            this.plugin.broadcastMessage(playerKilled, dmgPercentageMap.entrySet().iterator().next().getKey());
        } else {
            this.plugin.broadcastMessage(playersKilled);
            final Set<String> players = dmgPercentageMap.keySet();
            final String[] sortedPlayers = players.toArray(new String[players.size()]);
            Arrays.sort(sortedPlayers, new Comparator<String>() {

                @Override
                public int compare(final String a, final String b) {
                    return -Float.compare(dmgPercentageMap.get(a), dmgPercentageMap.get(b));
                }
            });
            for (final String playerName : sortedPlayers) {
                final float percentage = dmgPercentageMap.get(playerName);
                if (percentage < THRESHOLD) {
                    break;
                } else {
                    this.plugin.broadcastMessage(playersKilledLine, playerName, FORMAT.format(percentage * 100f));
                }
            }
        }

        // Check for custom egg handling
        final int eH = config.getEdEggHandling();
        final Location deathLocation = event.getEntity().getLocation();
        final String deathCoords = (int)deathLocation.getX() + ", " + (int)deathLocation.getY() + ", " + (int)deathLocation.getZ();

        // Possibly award or drop a dragon egg
        // 0: no special behavior; let the server handle the egg
        // 1: award to best player
        // 2: drop on the ground

        if (eH == 1) {
            AwardEnderDragonEgg(endWorld, deathLocation, deathCoords, dmgMap);
        } else if (eH == 2) {
            endWorld.dropItem(deathLocation, new ItemStack(Material.DRAGON_EGG));
            this.plugin.info("Dropped Ender Dragon egg at " + deathCoords);
        }

        // Forget about this dragon
        UUID dragonId = event.getEntity().getUniqueId();

        try {
            this.plugin.debug(" ... remove EnderDragon, UUID" + dragonId);
            dragonInfo.remove(dragonId);
            handler.getLoadedDragons().remove(dragonId);
        } catch (Exception ex) {
            this.plugin.debug(" ... exception removing EnderDragon, UUID" + dragonId + ": " + ex.getMessage());
        }

        // Handle on-ED-death regen/respawn
        int respawnType = config.getRespawnType();

        int dragonCountAlive = handler.getNumberOfAliveEnderDragons();

        if (respawnType == 1) {
            this.plugin.debug(" ... respawnType is 1; call handler.getRespawnHandler().respawnLater");
            handler.getRespawnHandler().respawnLater();
        } else {
            if (dragonCountAlive == 0) {
                if (respawnType == 2) {
                    this.plugin.debug(" ... respawnType is 2 and no dragons remain; call handler.getRespawnHandler().respawnLater");
                    handler.getRespawnHandler().respawnLater();
                } else {
                    this.plugin.debug(" ... respawnType is " + respawnType + "; take no action");
                }
            } else {
                this.plugin.debug(" ... live dragon count is " + dragonCountAlive + " (respawnType is " + respawnType + ")");
            }
        }

    }

    private void AwardEnderDragonEgg(World endWorld, Location deathLocation, String deathCoords, HashMap<String, Double> dmgMapSource) {

        this.plugin.debug("Awarding EnderDragon egg using custom logic");

        // Copy values from dmgMapSource into a new HashMap
        final HashMap<String, Double> dmgMap = new HashMap<>();
        for (final Entry<String, Double> e : dmgMapSource.entrySet()) {
            dmgMap.put(e.getKey(), e.getValue());
        }

        if (dmgMap.size() < 1) {
            this.plugin.debug(" ... Bug: dmgMap is empty; cannot award an egg");
            return;
        }

        // Step 1: % of total damages done to the ED ; Player name
        TreeMap<Float, String> damageRatioMap = new TreeMap<>();

        long totalDamages = 0;
        for (final Entry<String, Double> e : dmgMap.entrySet()) {
            totalDamages += e.getValue();
        }

        this.plugin.debug(" ... totalDamages: " + totalDamages);

        for (final Entry<String, Double> e : dmgMap.entrySet()) {
            float damageRatio = (float) (e.getValue() / (double) totalDamages);
            damageRatioMap.put(damageRatio, e.getKey());

            this.plugin.debug(" ... add to damageRatioMap: " + damageRatio + " for " + e.getKey());
        }

        // Step 2: Remove entries for Players whom done less damages than threshold
        final Iterator<Entry<Float, String>> damageRatioMapIterator = damageRatioMap.entrySet().iterator();
        while (damageRatioMapIterator.hasNext()) {
            final Entry<Float, String> e = damageRatioMapIterator.next();
            if (e.getKey() <= THRESHOLD) {
                damageRatioMapIterator.remove();
                this.plugin.debug(" ... remove from damageRatioMap: " + e.getKey() +
                        " (inflicted less than " + (int) (THRESHOLD * 100) + "% of the damage)");
            }
        }

        if (damageRatioMap.size() < 1) {
            this.plugin.debug(" ... Bug: damageRatioMap is now empty; using all players");
            dmgMap.clear();
            damageRatioMap.clear();

            for (final Entry<String, Double> e : dmgMapSource.entrySet()) {
                dmgMap.put(e.getKey(), e.getValue());
            }

            for (final Entry<String, Double> e : dmgMap.entrySet()) {
                float damageRatio = (float) (e.getValue() / (double) totalDamages);
                damageRatioMap.put(damageRatio, e.getKey());
            }
        }

        // Step 3: Update ratio according to removed parts of total (was 1 obviously)
        float remainingRatioTotal = 0f;
        for (final float f : damageRatioMap.keySet()) {
            // Computing new total (should be <=1)
            remainingRatioTotal += f;
        }

        // Step 4: Now update what part of the new total damages each player did
        float highestScore = 0;
        if (remainingRatioTotal != 1) {
            final TreeMap<Float, String> newRatioMap = new TreeMap<>();
            for (final Entry<Float, String> e : damageRatioMap.entrySet()) {
                float newRatio = e.getKey() * 1 / remainingRatioTotal;
                newRatioMap.put(newRatio, e.getValue());
                this.plugin.debug(" ... add to newRatioMap: " + newRatio + " for " + e.getValue());

                if (newRatio > highestScore)
                    highestScore = newRatio;
            }
            damageRatioMap = newRatioMap;
        }

        String playerName = null;

        if (damageRatioMap.size() == 1) {
            // Only one person to consider
            Entry<Float, String> e = damageRatioMap.firstEntry();
            playerName = e.getValue();
            this.plugin.debug(" ... will award egg to " + playerName + " (only possibility); damage ratio " + e.getKey());
        } else {

            // Step 5: Find all the players with a score of highestScore * .75 or higher
            float scoreThreshold = highestScore * 0.75f;

            final TreeMap<Float, String> highScoringPlayers = new TreeMap<>();
            for (final Entry<Float, String> e : damageRatioMap.entrySet()) {
                if (e.getKey() >= scoreThreshold) {
                    highScoringPlayers.put(e.getKey(), e.getValue());
                    this.plugin.debug(" ... " + e.getValue() + " has a chance to receive the egg; damage " + e.getKey() + " >= " + scoreThreshold);
                }
            }

            if (highScoringPlayers.size() == 0) {
                this.plugin.debug(" ... Bug: highScoringPlayers is empty; adding all players");
                for (final Entry<Float, String> e : damageRatioMap.entrySet()) {
                    highScoringPlayers.put(e.getKey(), e.getValue());
                    this.plugin.debug(" ... " + e.getValue() + " has a chance to receive the egg; damage " + e.getKey());
                }
            }

            // Step 6: Pick the winner
            if (highScoringPlayers.size() == 1) {
                Entry<Float, String> e = highScoringPlayers.firstEntry();
                playerName = e.getValue();
                this.plugin.debug(" ... highScoringPlayers has just 1 player; " +
                        "will award egg to " + playerName + "; damage ratio " + e.getKey());
            } else if (highScoringPlayers.size() > 1) {

                // Pick a random player from the high scoring players
                int rand = new Random().nextInt(highScoringPlayers.size());
                int iterator = 0;
                for (final Entry<Float, String> e : damageRatioMap.entrySet()) {
                    if (iterator >= rand) {
                        playerName = e.getValue();
                        this.plugin.debug(" ... will award egg to " + playerName + " (randomly chosen, index " + rand + ")");
                        break;
                    }
                    iterator++;
                }

            }
        }

        // Step 7: Give the Dragon Egg to playerName
        if (playerName == null) {
            // Security
            endWorld.dropItem(deathLocation, new ItemStack(Material.DRAGON_EGG));
            this.plugin.info("PlayerName is null; dropped dragon egg at " + deathCoords);
        } else {
            final Player p = Bukkit.getServer().getPlayerExact(playerName);
            if (p == null) {
                // Security
                endWorld.dropItem(deathLocation, new ItemStack(Material.DRAGON_EGG));
                this.plugin.info("Player is null (name " + playerName + " not found); dropped dragon egg at " + deathCoords);
            } else {
                // Try to give the Egg
                final HashMap<Integer, ItemStack> notGiven = p.getInventory().addItem(new ItemStack(Material.DRAGON_EGG));
                if (!notGiven.isEmpty()) {
                    // Inventory full, drop the egg at Player's foot
                    p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.DRAGON_EGG));
                    this.plugin.sendMessage(p, MessageId.theEndAgain_droppedDragonEgg);
                    this.plugin.info("Could not give dragon egg to " + playerName + " since inventory is full; " +
                            "dropped dragon egg at " + deathCoords);
                } else {
                    this.plugin.sendMessage(p, MessageId.theEndAgain_receivedDragonEgg);
                    this.plugin.info("Gave dragon egg to " + playerName + "");
                }
            }
        }
    }

    /**
     * Handles:
     * - Portal creation
     * - Custom Egg drop handling
     * - Cleaning the dead EnderDragon
     *
     * @param event an EntityCreatePortalEvent
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEnderDragonCreatePortal(final EntityCreatePortalEvent event) {

        EntityType entityType = event.getEntityType();
        this.plugin.debug("EntityCreatePortalEvent fired for entityType " + entityType.toString());

        if (entityType != EntityType.ENDER_DRAGON) {
            return;
        }

        // NOTE: This event is not firing for EnderDragons on Spigot 1.9 or Spigot 1.10
        // See JIRA issue at https://hub.spigotmc.org/jira/browse/SPIGOT-1812

        final World endWorld = event.getEntity().getWorld();

        this.plugin.info("Event EntityCreatePortalEvent was fired; this is unexpected for Spigot 1.9 or 1.10");

        final EndWorldHandler handler = this.plugin.getHandler(StringUtil.toLowerCamelCase(endWorld.getName()));
        if (handler == null) {
            this.plugin.debug("No EndWorldHandler for world " + endWorld.getName() + "; custom portal handling disabled");
            return;
        }

        final Config config = handler.getConfig();
        final int pH = config.getEdPortalSpawn();
        final int eH = config.getEdEggHandling();
        final int dH = config.getDropTableHandling();
        final Location deathLocation = event.getEntity().getLocation();

        /*
         * Explanation of algorithm:
         *
         * Variables:
         * - pH is the portalHandling configuration value
         * - eH is the eggHandling configuration value
         * - dH is the dropTableHandling configuration value
         *
         * "Things" to do:
         * - 1a = Cancel Egg spawn (on the portal)
         * - 1b = Cancel non-Egg portal blocks spawn
         * - 2a = Distribute Egg correctly, based on Damages done
         * - 2b = Distribute Drops correctly, based on Damages done
         * - 3  = Cancel the Event, it's better to use this when we just want
         *        no portal at all than setting everything to air later.
         *
         * Notes:
         * - 1a and 1b AND 2a and 2b are "things" that should be done at the same time
         *
         *         +--------+-------+-------+
         *         |  pH=0  |  pH=1 |  pH=2 |
         *  +------+--------+-------+-------+  2b = dH
         *  | eH=0 |    -   |   1b  |    3  |
         *  | eH=1 | 1a, 2a | 2a, 3 | 2a, 3 |
         *  +------+--------+-------+-------+
         */

        // (1a)
        final boolean cancelEgg = pH == 0 && eH >= 1 || event.isCancelled();

        // (1b)
        final boolean cancelPortalBlocks = pH == 1 && eH == 0 || event.isCancelled();

        // (2a)
        final boolean customEggHandling = eH == 1;

        // (2b)
        final boolean customDropHandling = dH == 1;

        // (3)
        final boolean cancelEvent = pH == 1 && eH == 1 || pH == 2;

        // 1a & 1b
        if (cancelEgg || cancelPortalBlocks) {
            // Change block types accordingly
            BlockState eggBlock = null;
            for (final BlockState b : event.getBlocks()) {
                if (b.getType() == Material.DRAGON_EGG) {
                    if (cancelEgg) {
                        b.setType(Material.AIR);
                    }
                    eggBlock = b;
                } else if (cancelPortalBlocks) {
                    b.setType(Material.AIR);
                }
            }

            // Refresh chunks to prevent client-side glitch
            if (eggBlock != null) {
                final Chunk c = eggBlock.getChunk();
                for (int x = c.getX() - 1; x <= c.getX() + 1; x++) {
                    for (int z = c.getZ() - 1; z <= c.getZ() + 1; z++) {
                        c.getWorld().refreshChunk(x, z);
                    }
                }
            }
        }

        // 2a & 2b
        if (customEggHandling || customDropHandling) {
            // Step 1: % of total damages done to the ED ; Player name
            TreeMap<Float, String> ratioMap = new TreeMap<>();
            long totalDamages = 0;
            for (final Entry<String, Double> e : handler.getDragons().get(event.getEntity().getUniqueId()).entrySet()) {
                totalDamages += e.getValue();
            }
            for (final Entry<String, Double> e : handler.getDragons().get(event.getEntity().getUniqueId()).entrySet()) {
                ratioMap.put((float)(e.getValue() / (double)totalDamages), e.getKey());
            }

            // Step 2: Remove entries for Players whom done less damages than threshold
            final Iterator<Entry<Float, String>> it = ratioMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<Float, String> e = it.next();
                if (e.getKey() <= THRESHOLD) {
                    it.remove();
                }
            }

            // Step 3: Update ratio according to removed parts of total (was 1 obviously)
            float remainingRatioTotal = 0f;
            for (final float f : ratioMap.keySet()) {
                // Computing new total (should be <=1)
                remainingRatioTotal += f;
            }

            // Step 4: Now update what part of the new total damages each player did
            if (remainingRatioTotal != 1) {
                final TreeMap<Float, String> newRatioMap = new TreeMap<>();
                for (final Entry<Float, String> e : ratioMap.entrySet()) {
                    newRatioMap.put(e.getKey() * 1 / remainingRatioTotal, e.getValue());
                }
                ratioMap = newRatioMap;
            }

            /*
             * This code has been moved to onEnderDragonDeath
             *

            if (customEggHandling) {
                // Step 5: Now we will take a random player, the best fighter has the best chance to be chosen
                float rand = new Random().nextFloat();
                String playerName = null;
                for (final Entry<Float, String> e : ratioMap.entrySet()) {
                    if (rand < e.getKey()) {
                        playerName = e.getValue();
                        break;
                    }
                    rand -= e.getKey();
                }

                // Step 6: And now we give him a Dragon Egg
                if (playerName == null) {
                    // Security
                    endWorld.dropItem(deathLocation, new ItemStack(Material.DRAGON_EGG));
                } else {
                    final Player p = Bukkit.getServer().getPlayerExact(playerName);
                    if (p == null) {
                        // Security
                        endWorld.dropItem(deathLocation, new ItemStack(Material.DRAGON_EGG));
                    } else {
                        // Try to give the Egg
                        final HashMap<Integer, ItemStack> notGiven = p.getInventory().addItem(new ItemStack(Material.DRAGON_EGG));
                        if (!notGiven.isEmpty()) {
                            // Inventory full, drop the egg at Player's foot
                            p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.DRAGON_EGG));
                            this.plugin.sendMessage(p, MessageId.theEndAgain_droppedDragonEgg);
                        } else {
                            this.plugin.sendMessage(p, MessageId.theEndAgain_receivedDragonEgg);
                        }
                    }
                }
            }
            */

            // Step 7: And now we redo steps 5 and 6 for each Drop
            if (customDropHandling) {
                for (final Pair<ItemStack, Float> pair : config.getDropTable()) {
                    final ItemStack is = pair.getKey().clone();
                    is.setAmount(1);
                    for (int i = 0; i < pair.getKey().getAmount(); i++) {
                        if (RANDOM.nextFloat() <= pair.getValue()) {
                            // Step 5 again
                            float rand = new Random().nextFloat();
                            String playerName = null;
                            for (final Entry<Float, String> e : ratioMap.entrySet()) {
                                if (rand < e.getKey()) {
                                    playerName = e.getValue();
                                    break;
                                }
                                rand -= e.getKey();
                            }

                            // Step 6 again
                            if (playerName == null) {
                                // Security
                                endWorld.dropItem(deathLocation, is);
                            } else {
                                final Player p = Bukkit.getServer().getPlayerExact(playerName);
                                if (p == null) {
                                    // Security
                                    endWorld.dropItem(deathLocation, is);
                                } else {
                                    // Try to give the Drop
                                    final HashMap<Integer, ItemStack> notGiven = p.getInventory().addItem(is);
                                    if (!notGiven.isEmpty()) {
                                        // Inventory full, drop the drop at Player's foot
                                        p.getWorld().dropItem(p.getLocation(), is);
                                        this.plugin.sendMessage(p, MessageId.theEndAgain_droppedDrop);
                                    } else {
                                        this.plugin.sendMessage(p, MessageId.theEndAgain_receivedDrop);
                                    } // Here starts the bracket waterfall!
                                }
                            } // Yay!
                        }
                    } // Again!
                }
            } // One more!
        } // Woo!

        // 3
        if (!event.isCancelled()) {
            event.setCancelled(cancelEvent);
        }

        /*
         * This code has been moved to onEnderDragonDeath
         * because event EntityCreatePortalEvent was not firing
         *

        // Forget about this dragon
        UUID dragonId = event.getEntity().getUniqueId();
        this.plugin.debug(" ... remove EnderDragon, UUID " + dragonId);

        handler.getDragons().remove(dragonId);
        handler.getLoadedDragons().remove(dragonId);

        // Handle on-ED-death regen/respawn
        int respawnType = config.getRespawnType();

        if (respawnType == 1) {
            this.plugin.debug(" ... respawnType is 1; call handler.getRespawnHandler().respawnLater");
            handler.getRespawnHandler().respawnLater();
        } else {
            int dragonCountAlive = handler.getNumberOfAliveEnderDragons();
            if (dragonCountAlive == 0) {
                if (respawnType == 2) {
                    this.plugin.debug(" ... respawnType is 2 and no dragons remain; call handler.getRespawnHandler().respawnLater");
                    handler.getRespawnHandler().respawnLater();
                } else {
                    this.plugin.debug(" ... respawnType is " + respawnType + " and no dragons remain; take no action");

                    // if (config.getRespawnType() == 6) {
                    //    // Respawn Type 6 (deprecated):
                    //    // Respawn every X seconds after the last Dragon alive's death, persistent through reboots/reloads
                    //    config.setNextRespawnTaskTime(System.currentTimeMillis() + config.getRandomRespawnTimer() * 1000);
                    //    handler.getTasks().add(Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
                    //        @Override
                    //        public void run() {
                    //            handler.getRespawnHandler().respawn();
                    //        }
                    //    }, config.getNextRespawnTaskTime() / 1000 * 20));
                    // }
                }
            } else {
                this.plugin.debug(" ... live dragon count is " + dragonCountAlive + " (respawnType is " + respawnType + ")");
            }
        }

        */

    }

    /**
     * - Prevents natural spawn of EnderDragon
     * - Prevents too many Dragons in the End world
     * - Handle custom health and take care of this new Dragon
     *
     * @param event a CreatureSpawnEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnderDragonSpawn(final CreatureSpawnEvent event) {

        if (event.getEntityType() == EntityType.ENDER_DRAGON) {

            Environment currentEnvironment = event.getEntity().getWorld().getEnvironment();

            if (currentEnvironment != Environment.THE_END) {
                this.plugin.debug("An EnderDragon has spawned in " + currentEnvironment.toString() + "; will not track this dragon");
            } else
            {
                this.plugin.debug("An EnderDragon has spawned in the End");

                String worldName = event.getLocation().getWorld().getName();
                final EndWorldHandler handler = this.plugin.getHandler(StringUtil.toLowerCamelCase(worldName));

                if (handler != null) {
                    this.plugin.debug(" ... EndWorld handler found for " + worldName);

                    SpawnReason spawnReason = event.getSpawnReason();
                    if (spawnReason == SpawnReason.CUSTOM) {
                        this.plugin.debug(" ... EnderDragon spawn reason: CUSTOM; will not track this dragon");
                    } else if (spawnReason == SpawnReason.SPAWNER_EGG) {
                        this.plugin.debug(" ... EnderDragon spawn reason: SPAWNER_EGG; will not track this dragon");
                    } else {

                        this.plugin.debug(" ... EnderDragon spawn reason: " + spawnReason.toString());

                        final EnderDragon ed = (EnderDragon)event.getEntity();
                        UUID dragonId = ed.getUniqueId();

                        if (!handler.getDragons().containsKey(dragonId) && event.getLocation().getWorld().getEnvironment() == Environment.THE_END) {

                            int initialHealth = handler.getConfig().getEdHealth();
                            this.plugin.debug("onEnderDragonSpawn ... spawned EnderDragon, UUID " + dragonId + ", health " + initialHealth);

                            handler.getDragons().put(dragonId, new HashMap<String, Double>());

                            ed.setMaxHealth(initialHealth);
                            ed.setHealth(ed.getMaxHealth());

                            // plugin.debug("onEnderDragonSpawn ... actual health is " + new DecimalFormat("#.##").format(ed.getHealth()));

                        } else {
                            this.plugin.debug("onEnderDragonSpawn ... spawned EnderDragon, UUID " + dragonId +
                                    "; default health of " + new DecimalFormat("#.##").format(ed.getHealth()));
                        }
                        handler.getLoadedDragons().add(dragonId);
                    }
                }
        	}
        }
    }

    /**
     * Handle EnderDragon regen
     *
     * @param event an EntityRegainHealthEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnderDragonRegainHealth(final EntityRegainHealthEvent event) {

    	if(event.getEntity().getWorld().getEnvironment() != Environment.THE_END ||
           event.getEntityType() != EntityType.ENDER_DRAGON ||
           event.getRegainReason() != RegainReason.ENDER_CRYSTAL) {
            return;
        }

        String worldName = event.getEntity().getLocation().getWorld().getName();
        final EndWorldHandler handler = this.plugin.getHandler(StringUtil.toLowerCamelCase(worldName));
        if (handler == null) {
            this.plugin.debug("onEnderDragonRegainHealth: no handler for " + worldName);
            return;
        }

        final EnderDragon ed = (EnderDragon)event.getEntity();
        UUID dragonId = ed.getUniqueId();

        final float rate = handler.getConfig().getEcHealthRegainRate();
        if (rate < 1.0) {
            float nextFloat = RANDOM.nextFloat();
            if (nextFloat >= rate) {
                this.plugin.debug("Cancel health gain for EnderDragon, " + "UUID " + dragonId + ": " + nextFloat + " >= " + rate);
                event.setCancelled(true);
            } else {
                // this.plugin.debug("Allow health gain for EnderDragon, " + "UUID " + dragonId + ": " + event.getAmount());
            }
        } else if (rate > 1.0) {
            int healthAmount = (int)(rate * event.getAmount());
            this.plugin.debug("Set health gain for EnderDragon, " + "UUID " + dragonId + ": " + healthAmount);
            event.setAmount(healthAmount);
        } else {
            // this.plugin.debug("Vanilla health gain for EnderDragon, " + "UUID " + dragonId + ": " + event.getAmount());
        }
    }
}
