/***************************************************************************
 * Project file:    NPlugins - NTheEndAgain - RespawnTask.java             *
 * Full Class name: fr.ribesg.bukkit.ntheendagain.task.RespawnTask         *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.ntheendagain.task;

import fr.ribesg.bukkit.ntheendagain.Config;
import fr.ribesg.bukkit.ntheendagain.handler.EndWorldHandler;

/**
 * @author Ribesg
 */
public class RespawnTask extends RandomRepeatingTask {

    public RespawnTask(final EndWorldHandler handler) {
        super(handler);
    }

    @Override
    /**
     * Execute the task.
     * This is equivalent to the run() task of a standard repeating task
     */
    public boolean exec() {
        this.worldHandler.getPlugin().entering(this.getClass(), "exec");

        final boolean res = this.worldHandler.getRespawnHandler().respawn(false);

        this.worldHandler.getPlugin().exiting(this.getClass(), "exec", Boolean.toString(res));
        return res;
    }

    @Override
    /**
     * Get the initial delay for this task
     *
     * @return the initial delay, in seconds
     */
    protected long getInitialDelay() {
        Config config = this.worldHandler.getConfig();

        // This time is based on System.currentTimeMillis()
        long nextRespawnTaskTime = config.getNextRespawnTaskTime();
        if (this.worldHandler.getConfig().getRespawnType() == 4) {
            nextRespawnTaskTime = 0;
        }

        long initialDelaySeconds = this.buildInitialDelay(nextRespawnTaskTime);
        config.updateNextExpectedRespawnTime(initialDelaySeconds);

        return initialDelaySeconds;
    }

    @Override
    /**
     * @return Random value, in seconds between the minimum and maximum delay set in config
     */
    protected long getDelay() {
        return this.worldHandler.getConfig().getRandomRespawnTimeSeconds();
    }

    @Override
    /**
     * Sets the next execution time for this task.
     *
     * @param nextTaskTime the next execution time for this task, based on System.currentTimeMillis
     */
    protected void setNextConfigTime(final long nextTaskTime) {
        Config config = this.worldHandler.getConfig();
        config.setNextRespawnTaskTime(nextTaskTime, "RespawnTask.setNextConfigTime");

        int respawnDelaySeconds = (int)((nextTaskTime - System.currentTimeMillis()) / 1000);
        config.updateNextExpectedRespawnTime(respawnDelaySeconds);
    }
}
