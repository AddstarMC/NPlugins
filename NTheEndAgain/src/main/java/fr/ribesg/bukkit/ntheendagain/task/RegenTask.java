/***************************************************************************
 * Project file:    NPlugins - NTheEndAgain - RegenTask.java               *
 * Full Class name: fr.ribesg.bukkit.ntheendagain.task.RegenTask           *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.ntheendagain.task;

import fr.ribesg.bukkit.ntheendagain.handler.EndWorldHandler;

/**
 * @author Ribesg
 */
public class RegenTask extends RandomRepeatingTask {

    public RegenTask(final EndWorldHandler handler) {
        super(handler);
    }

    @Override
    /**
     * Execute the task.
     * This is equivalent to the run() task of a standard repeating task
     */
    public boolean exec() {
        this.worldHandler.getPlugin().entering(this.getClass(), "exec");

        this.worldHandler.getRegenHandler().regen();

        this.worldHandler.getPlugin().exiting(this.getClass(), "exec");
        return true;
    }

    @Override
    /**
     * Get the initial delay for this task
     *
     * @return the initial delay, in seconds
     */
    protected long getInitialDelay() {
        long nextRegenTaskTime = this.worldHandler.getConfig().getNextRegenTaskTime();
        if (this.worldHandler.getConfig().getRegenType() == 2) {
            nextRegenTaskTime = 0;
        }
        return this.buildInitialDelay(nextRegenTaskTime);
    }

    @Override
    /**
     * @return a Random value between the minimum and maximum delay set in config
     */
    protected long getDelay() {
        return this.worldHandler.getConfig().getRegenTimer();
    }

    @Override
    /**
     * Sets the next execution time for this task.
     *
     * @param nextTaskTime the next execution time for this task, based on System.nanoTime
     */
    protected void setNextConfigTime(final long nextTaskTime) {
        this.worldHandler.getConfig().setNextRegenTaskTime(nextTaskTime);
    }
}
