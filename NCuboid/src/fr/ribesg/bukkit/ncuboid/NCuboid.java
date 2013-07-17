package fr.ribesg.bukkit.ncuboid;

import fr.ribesg.bukkit.ncore.lang.MessageId;
import fr.ribesg.bukkit.ncore.nodes.cuboid.CuboidNode;
import fr.ribesg.bukkit.ncuboid.beans.CuboidDB;
import fr.ribesg.bukkit.ncuboid.beans.CuboidDBPersistenceHandler;
import fr.ribesg.bukkit.ncuboid.beans.WorldCuboid;
import fr.ribesg.bukkit.ncuboid.commands.MainCommandExecutor;
import fr.ribesg.bukkit.ncuboid.lang.Messages;
import fr.ribesg.bukkit.ncuboid.listeners.EventExtensionListener;
import fr.ribesg.bukkit.ncuboid.listeners.PlayerStickListener;
import fr.ribesg.bukkit.ncuboid.listeners.WorldLoadingListener;
import fr.ribesg.bukkit.ncuboid.listeners.flag.*;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginManager;

import java.io.IOException;

/**
 * TODO
 *
 * @author Ribesg
 */
public class NCuboid extends CuboidNode {

    // Configs
    private Messages messages;
    private Config   pluginConfig;

    // Useful Nodes
    // // None

    // Cuboids base
    private CuboidDB db;

    @Override
    protected String getMinCoreVersion() {
        return "0.2.1";
    }

    /** @see fr.ribesg.bukkit.ncore.nodes.NPlugin#onNodeEnable() */
    @Override
    protected boolean onNodeEnable() {
        // Messages first !
        try {
            if (!getDataFolder().isDirectory()) {
                getDataFolder().mkdir();
            }
            messages = new Messages();
            messages.loadMessages(this);
        } catch (final IOException e) {
            getLogger().severe("An error occured, stacktrace follows:");
            e.printStackTrace();
            getLogger().severe("This error occured when NCuboid tried to load messages.yml");
            return false;
        }

        // Config
        try {
            pluginConfig = new Config(this);
            pluginConfig.loadConfig();
        } catch (final IOException e) {
            getLogger().severe("An error occured, stacktrace follows:");
            e.printStackTrace();
            getLogger().severe("This error occured when NCuboid tried to load config.yml");
            return false;
        }

        // Create the CuboidDB
        try {
            db = CuboidDBPersistenceHandler.loadDB(this);
        } catch (final IOException e) {
            // TODO
            e.printStackTrace();
        } catch (final InvalidConfigurationException e) {
            // TODO
            e.printStackTrace();
        }

        // Listeners
        final PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new EventExtensionListener(this), this);
        pm.registerEvents(new PlayerStickListener(this), this);
        pm.registerEvents(new WorldLoadingListener(this), this);

        // Flag Listeners
        pm.registerEvents(new BoosterFlagListener(this), this);
        pm.registerEvents(new BuildFlagListener(this), this);
        pm.registerEvents(new ChatFlagListener(this), this);
        pm.registerEvents(new ChestFlagListener(this), this);
        pm.registerEvents(new ClosedFlagListener(this), this);
        pm.registerEvents(new CreativeFlagListener(this), this);
        pm.registerEvents(new DropFlagListener(this), this);
        pm.registerEvents(new EndermanGriefFlagListener(this), this);
        pm.registerEvents(new ExplosionFlagListener(this), this);
        pm.registerEvents(new FarmFlagListener(this), this);
        pm.registerEvents(new FireFlagListener(this), this);
        pm.registerEvents(new GodFlagListener(this), this);
        pm.registerEvents(new InvisibleFlagListener(this), this);
        pm.registerEvents(new MobFlagListener(this), this);
        pm.registerEvents(new PassFlagListener(this), this);
        pm.registerEvents(new PVPFlagListener(this), this);
        pm.registerEvents(new SnowFlagListener(this), this);
        pm.registerEvents(new TeleportFlagListener(this), this);
        pm.registerEvents(new UseFlagListener(this), this);
        pm.registerEvents(new WarpgateFlagListener(this), this);

        // Command
        getCommand("cuboid").setExecutor(new MainCommandExecutor(this));

        return true;
    }

    /** @see fr.ribesg.bukkit.ncore.nodes.NPlugin#handleOtherNodes() */
    @Override
    protected void handleOtherNodes() {
        // See if there are new worlds
        for (final World world : getServer().getWorlds()) {
            if (db.getByWorld(world) == null) {
                db.addByWorld(new WorldCuboid(world));
            }
        }
    }

    /** @see fr.ribesg.bukkit.ncore.nodes.NPlugin#onNodeDisable() */
    @Override
    protected void onNodeDisable() {
        try {
            CuboidDBPersistenceHandler.saveDB(this, getDb());
        } catch (final IOException e) {
            // TODOs
            e.printStackTrace();
        }

        // TODO Save CuboidDB, do other things eventually (stop tasks etc)
    }

    /**
     * Send a message with arguments
     * TODO <b>This may be moved<b>
     *
     * @param to        Receiver
     * @param messageId The Message Id
     * @param args      The arguments
     */
    public void sendMessage(final CommandSender to, final MessageId messageId, final String... args) {
        final String[] m = messages.get(messageId, args);
        to.sendMessage(m);
    }

    public CuboidDB getDb() {
        return db;
    }

    public Messages getMessages() {
        return messages;
    }

    public Config getPluginConfig() {
        return pluginConfig;
    }
}
