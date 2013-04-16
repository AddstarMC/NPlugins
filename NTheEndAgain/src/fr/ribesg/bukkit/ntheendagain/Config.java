package fr.ribesg.bukkit.ntheendagain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.bukkit.configuration.file.YamlConfiguration;

import fr.ribesg.bukkit.ncore.AbstractConfig;
import fr.ribesg.bukkit.ncore.Utils;
import fr.ribesg.bukkit.ncore.lang.MessageId;

public class Config extends AbstractConfig {

    private final NTheEndAgain                         plugin;
    private final String                               worldName;

    @Getter @Setter(AccessLevel.PRIVATE) private int   nbEnderDragons;
    @Getter @Setter(AccessLevel.PRIVATE) private int   enderDragonHealth;
    @Getter @Setter(AccessLevel.PRIVATE) private float enderDragonDamageMultiplier;
    @Getter @Setter(AccessLevel.PRIVATE) private int   portalHandling;
    @Getter @Setter(AccessLevel.PRIVATE) private int   dragonEggHandling;
    @Getter @Setter(AccessLevel.PRIVATE) private int   xpHandling;
    @Getter @Setter(AccessLevel.PRIVATE) private int   xpReward;
    @Getter @Setter(AccessLevel.PRIVATE) private int   respawnTimer;
    @Getter @Setter(AccessLevel.PRIVATE) private int   respawnOnBoot;
    @Getter @Setter(AccessLevel.PRIVATE) private int   regenOnRespawn;
    @Getter @Setter(AccessLevel.PRIVATE) private int   actionOnRegen;
    @Getter @Setter(AccessLevel.PRIVATE) private int   customEdPushPlayer;
    @Getter @Setter private long                       lastTaskExecTime;

    public Config(final NTheEndAgain instance, final String world) {
        plugin = instance;
        worldName = world;

        setNbEnderDragons(1);
        setEnderDragonHealth(200);
        setEnderDragonDamageMultiplier(1.0f);
        setPortalHandling(0);
        setDragonEggHandling(0);
        setXpHandling(0);
        setXpReward(12_000);
        setRespawnTimer(0);
        setRespawnOnBoot(1);
        setRegenOnRespawn(1);
        setActionOnRegen(0);
        setCustomEdPushPlayer(1);
        setLastTaskExecTime(0);
    }

    /**
     * @see AbstractConfig#setValues(YamlConfiguration)
     */
    @Override
    protected void setValues(final YamlConfiguration config) {

        // nbEnderDragons. Default: 1. Possible values: positive integers
        setNbEnderDragons(config.getInt("nbEnderDragons", 1));
        if (getNbEnderDragons() < 0) {
            setNbEnderDragons(1);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "nbEnderDragons", "1");
        }

        // enderDragonHealth. Default: 200. Possible values: positive integers
        setEnderDragonHealth(config.getInt("enderDragonHealth", 200));
        if (getEnderDragonHealth() < 0) {
            setEnderDragonHealth(200);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "enderDragonHealth", "200");
        }

        // enderDragonDamageMultiplier. Default: 1.0. Possible values: positive floats
        setEnderDragonDamageMultiplier((float) config.getDouble("enderDragonDamageMultiplier", 1.0f));
        if (getEnderDragonDamageMultiplier() < 0.0) {
            setEnderDragonDamageMultiplier(1.0f);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "enderDragonDamageMultiplier", "1.0");
        }

        // portalHandling. Default: 0. Possible values: 0,1,2
        setPortalHandling(config.getInt("portalHandling", 0));
        if (getPortalHandling() < 0 || getPortalHandling() > 2) {
            setPortalHandling(0);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "portalHandling", "0");
        }

        // dragonEggHandling. Default: 0. Possible values: 0,1
        setDragonEggHandling(config.getInt("dragonEggHandling", 0));
        if (getDragonEggHandling() < 0 || getDragonEggHandling() > 1) {
            setDragonEggHandling(0);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "dragonEggHandling", "0");
        }

        // xpHandling. Default: 0. Possible values: 0,1
        setXpHandling(config.getInt("xpHandling", 0));
        if (getXpHandling() < 0 || getXpHandling() > 1) {
            setXpHandling(0);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "xpHandling", "0");
        }

        // xpReward. Default: 12 000. Possible values: positive or null integers
        setXpReward(config.getInt("xpReward", 12_000));
        if (getXpReward() < 0) {
            setXpReward(12_000);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "xpReward", "12 000");
        }

        // respawnTimer. Default: 0. Possible values: positive or null integers
        setRespawnTimer(config.getInt("respawnTimer", 0));
        if (getRespawnTimer() < 0) {
            setRespawnTimer(0);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "respawnTimer", "0");
        }

        // respawnOnBoot. Default: 1. Possible values: 0,1
        setRespawnOnBoot(config.getInt("respawnOnBoot", 1));
        if (getRespawnOnBoot() < 0 || getRespawnOnBoot() > 1) {
            setRespawnOnBoot(1);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "respawnOnBoot", "1");
        }

        // regenOnRespawn. Default: 1. Possible values: 0,1,2
        setRegenOnRespawn(config.getInt("regenOnRespawn", 1));
        if (getRegenOnRespawn() < 0 || getRegenOnRespawn() > 2) {
            setRegenOnRespawn(1);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "regenOnRespawn", "1");
        }

        // actionOnRegen. Default: 0. Possible values: 0,1
        setActionOnRegen(config.getInt("actionOnRegen", 0));
        if (getActionOnRegen() < 0 || getActionOnRegen() > 1) {
            setActionOnRegen(0);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "actionOnRegen", "0");
        }

        // customEdPushPlayer. Default: 1. Possible values: 0,1
        setCustomEdPushPlayer(config.getInt("customEdPushPlayer", 1));
        if (getCustomEdPushPlayer() < 0 || getCustomEdPushPlayer() > 1) {
            setCustomEdPushPlayer(1);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "customEdPushPlayer", "1");
        }

        // lastTaskStartTime.
        setLastTaskExecTime(config.getLong("lastTaskExecTime", 0L));
        if (getLastTaskExecTime() < 0 || getLastTaskExecTime() > System.currentTimeMillis()) {
            setLastTaskExecTime(0);
            plugin.sendMessage(plugin.getServer().getConsoleSender(), MessageId.incorrectValueInConfiguration, Utils.toLowerCamelCase(worldName) + "Config.yml", "lastTaskStartTime", "0");
        }

    }

    /**
     * @see AbstractConfig#getConfigString()
     */
    @Override
    protected String getConfigString() {
        final StringBuilder content = new StringBuilder();

        // Header
        content.append("################################################################################\n");
        content.append("# Config file for NTheEndAgain plugin. If you don't understand something,      #\n");
        content.append("# please ask on dev.bukkit.org or on forum post.                        Ribesg #\n");
        content.append("################################################################################\n\n");

        content.append("# This config file is about the world \"" + worldName + "\"\n\n");

        // nbEnderDragons. Default: 1
        content.append("# The number of EnderDragons that will be at the same time in an End world. Default: 1\n");
        content.append("nbEnderDragons: " + getNbEnderDragons() + "\n\n");

        // enderDragonHealth. Default: 200
        content.append("# The health value EnderDragons will spawn with. Default: 200\n");
        content.append("enderDragonHealth: " + getEnderDragonHealth() + "\n\n");

        // enderDragonDamageMultiplier. Default: 1.0
        content.append("# Scale damages done by EnderDragon. Default: 1.0\n");
        content.append("enderDragonDamageMultiplier: " + getEnderDragonDamageMultiplier() + "\n\n");

        // portalHandling. Default: 0
        content.append("# The way portal spawn will be handled. Default: 0\n");
        content.append("# 	0: Disabled. Portal will spawn normally.\n");
        content.append("# 	1: Egg. Portal will be removed but not the DragonEgg\n");
        content.append("# 	2: Enabled. Portal will not spawn. No more cuted obsidian towers. No Egg if dragonEggHandling=0.\n");
        content.append("portalHandling: " + getPortalHandling() + "\n\n");

        // dragonEggHandling. Default: 0
        content.append("# The way the DragonEgg will spawn. Default: 0\n");
        content.append("# 	0: Disabled. The egg will spawn normally if portalHandling is set to 0 or 1.\n");
        content.append("# 	1: Enabled. The egg will be semi-randomly given to one of the best fighters.\n");
        content.append("dragonEggHandling: " + getDragonEggHandling() + "\n\n");

        // xpHandling. Default: 0
        content.append("# The way the reward XP will be given to player. Default: 0\n");
        content.append("# 	0: Disabled. XP orbs will spawn normally.\n");
        content.append("# 	1: Enabled. XP will be splitted between fighters, more XP for better fighters.\n");
        content.append("xpHandling: " + getXpHandling() + "\n\n");

        // xpReward. Default: 12 000
        content.append("# The value of the XP drop. Default: 12 000\n");
        content.append("xpReward: " + getXpReward() + "\n\n");

        // respawnTimer. Default: 21 600 (6 hours)
        content.append("# The time between checks for respawning EnderDragons, in seconds. Default: 0 (Disabled)\n");
        content.append("# Here are some values:\n");
        content.append("#   Value   --   Description\n");
        content.append("#          0: Disabled\n");
        content.append("#       1800: 30 minutes\n");
        content.append("#       3600: 1 hour\n");
        content.append("#       7200: 2 hours\n");
        content.append("#      10800: 3 hours\n");
        content.append("#      14400: 4 hours\n");
        content.append("#      21600: 6 hours\n");
        content.append("#      28800: 8 hours\n");
        content.append("#      43200: 12 hours\n");
        content.append("#      86400: 24 hours - 1 day\n");
        content.append("#     172800: 48 hours - 2 days\n");
        content.append("#     604800: 7 days\n");
        content.append("# You can use any value you want, just be sure to convert it to seconds.\n");
        content.append("respawnTimer: " + getRespawnTimer() + "\n\n");

        // respawnOnBoot. Default: 1
        content.append("# Should we respawn EnderDragons at server boot. Default: 1\n");
        content.append("#       0: Disabled.\n");
        content.append("#       1: Enabled. There will be nbEnderDragons (" + getNbEnderDragons() + ") in this world after each reboot\n");
        content.append("respawnOnBoot: " + getRespawnOnBoot() + "\n\n");

        // regenOnRespawn. Default: 1
        content.append("# Should we regen the End world before respawning Dragons ? Default: 1\n");
        content.append("#       0: Disabled.\n");
        content.append("#       1: Enabled. World will be regen, even if EnderDragons are still alive.\n");
        content.append("#       2: Enabled. World will be regen ONLY if there are NO EnderDragon alive.\n");
        content.append("# Note: This regen method does not instantly regen the world. Chunks are regenerated at\n");
        content.append("#       the moment there are loaded, so you may experience a tiny lag when joining the End.\n");
        content.append("#       It's nothing compared to the 2-10 seconds freeze a HARD regen cause.\n");
        content.append("regenOnRespawn: " + getRegenOnRespawn() + "\n\n");

        // Comment on Hard Reset
        content.append("# Note: If you have\n");
        content.append("# - respawnTimer set to 0\n");
        content.append("# - respawnOnBoot set to 1\n");
        content.append("# - regenOnRespawn set to 1\n");
        content.append("# As the above values, the actual regeneration will be a HARD regen occuring at server stop\n");
        content.append("# This mean there will be no lag when entering the End !\n\n");

        // actionOnRegen. Default: 0
        content.append("# What do we do to players in the End when we want to regen the world ? Default: 1\n");
        content.append("#       0: Kick them. This way they can rejoin immediatly in the End\n");
        content.append("#          WARNING: Mass rejoin after mass kick in the End could cause lag because chunks are\n");
        content.append("#                   regen on chunk loading and mass join = mass load of chunks at the same time\n");
        content.append("#       1: Teleport them to the spawn point of the Main (= first) world.\n");
        content.append("actionOnRegen: " + getActionOnRegen() + "\n\n");

        // customEdPushPlayer. Default: 1
        content.append("# Do we 'simulate' the EnderDragon-Pushes-Player behaviour ? Default: 1\n");
        content.append("# This feature apply a kind-of random velocity to a Player when it is damaged by an EnderDragon\n");
        content.append("#       0: Disabled.\n");
        content.append("#       1: Enabled.\n");
        content.append("customEdPushPlayer: " + getCustomEdPushPlayer() + "\n\n");

        // lastTaskStartTime. Default: 0
        content.append("# Used to allow task timer persistence. /!\\ PLEASE DO NOT TOUCH THIS !\n");
        content.append("lastTaskExecTime: " + (getRespawnTimer() == 0 ? "0" : getLastTaskExecTime()) + "\n\n");

        return content.toString();
    }
}
