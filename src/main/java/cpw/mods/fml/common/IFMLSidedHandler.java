/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     cpw - implementation
 */

package cpw.mods.fml.common;

import java.util.List;

import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.relauncher.Side;

/**
 * Interface for handlers for a specific side
 *
 */
public interface IFMLSidedHandler
{
	/**
	 * Any additional branding info that should be added
	 * @return Branding info that should be added
	 */
    List<String> getAdditionalBrandingInformation();

    /**
     * The side this is a handler for
     * @return Side this is a handler for
     */
    Side getSide();

    /**
     * Halt the game abnormally due to an exception 
     * @param message The message to halt with
     * @param exception The exception that caused the halt
     */
    void haltGame(String message, Throwable exception);

    /**
     * Show a GUI screen
     * @param clientGuiElement The GUI screen to show
     */
    void showGuiScreen(Object clientGuiElement);

    /**
     * Begin loading a server
     * @param server The server that is loading
     */
    void beginServerLoading(MinecraftServer server);

    /**
     * Finish loading a server
     */
    void finishServerLoading();

    /**
     * Get the server we are using
     * @return The server we are using
     */
    MinecraftServer getServer();

    /**
     * Should the server die quietly
     * @return Should the server die quietly
     */
    boolean shouldServerShouldBeKilledQuietly();

    /**
     * Add a mod as a resource
     * @param container The mod to add as a resource
     */
    void addModAsResource(ModContainer container);

    /**
     * Update the list of resource packs
     */
    void updateResourcePackList();

    /**
     * Get the language the interface is set to
     * @return The language of the interface
     */
    String getCurrentLanguage();

    /**
     * Called when the server is stopped
     */
    void serverStopped();

    /**
     * Get the network manager for the client-server connection
     * @return The network manager for the client-server connection
     */
    NetworkManager getClientToServerNetworkManager();

    INetHandler getClientPlayHandler();
}
