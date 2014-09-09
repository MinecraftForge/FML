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

/**
 * A wrapper interface that contains a reference to a Bukkit plugin.
 *
 * At an implementation-defined time, fields of this type annotated with
 * {@link cpw.mods.fml.common.BukkitPluginRef} will be initialized. Once they
 * are, the {@link #initialized()} method will begin returning true, and the
 * {@link #get()} method will return either null, if the plugin is not
 * present, or the Bukkit Plugin object (instances of
 * <code>org.bukkit.plugin.Plugin</code>).
 *
 * @see BukkitPluginRef
 */
public interface BukkitPluginWrapper
{
    /**
     * This method returns false until the presence or non-presence of the
     * Bukkit plugins has been determined.
     *
     * @return whether the wrappers have been filled yet
     */
    boolean initialized();

    /**
     * Attempt to get the Bukkit Plugin object.
     *
     * @return the bukkit Plugin object, or null if plugin is not found or
     *         Bukkit is not running
     */
    Object get();
}
