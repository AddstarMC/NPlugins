package fr.ribesg.bukkit.ncuboid.commands;

import fr.ribesg.bukkit.ncore.lang.MessageId;
import fr.ribesg.bukkit.ncuboid.NCuboid;
import fr.ribesg.bukkit.ncuboid.Perms;
import fr.ribesg.bukkit.ncuboid.commands.subexecutors.CreateSubcmdExecutor;
import fr.ribesg.bukkit.ncuboid.commands.subexecutors.ReloadSubcmdExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MainCommandExecutor implements CommandExecutor {

    private final NCuboid plugin;

    public MainCommandExecutor(final NCuboid instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String cmdLabel, final String[] args) {
        if (cmd.getName().equals("cuboid")) {
            if (!Perms.hasGeneral(sender)) {
                plugin.sendMessage(sender, MessageId.noPermissionForCommand);
                return true;
            } else {
                if (args.length == 0) {
                    return cmdDefault(sender);
                } else {
                    // TODO Better way to handle subcommand aliases ?
                    switch (args[0]) {
                        case "reload":
                        case "rld":
                            return new ReloadSubcmdExecutor(plugin, sender, args).exec();
                        case "create":
                        case "c":
                            return new CreateSubcmdExecutor(plugin, sender, args).exec();
                        default:
                            return false;
                    }
                }
            }
        } else {
            return false;
        }
    }

    private boolean cmdDefault(final CommandSender sender) {
        // TODO
        return false;
    }
}
