/***************************************************************************
 * Project file:    NPlugins - NCore - Node.java                           *
 * Full Class name: fr.ribesg.bukkit.ncore.node.Node                       *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.ncore.node;

/**
 * This interface is implemented by all nodes.
 *
 * @author Ribesg
 */
public interface Node {

    String CUBOID         = "NCuboid";
    String ENCHANTING_EGG = "NEnchantingEgg";
    String GENERAL        = "NGeneral";
    String PERMISSIONS    = "NPermissions";
    String PLAYER         = "NPlayer";
    String TALK           = "NTalk";
    String THE_END_AGAIN  = "NTheEndAgain";
    String WORLD          = "NWorld";

    String getNodeName();
}
