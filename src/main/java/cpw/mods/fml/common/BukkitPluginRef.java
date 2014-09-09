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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare a variable to be populated by a reference to an instance of
 * Bukkit's JavaPlugin class. It should be applied to a field typed as
 * {@link cpw.mods.fml.common.BukkitPluginWrapper} or Object.
 *
 * The field will be filled with a BukkitPluginWrapper instance at the same
 * time that the {@link cpw.mods.fml.common.Mod.Instance} fields are
 * populated, but the Wrapper will not be
 * {@link cpw.mods.fml.common.BukkitPluginWrapper#initialized() initialized}
 * until a later time.
 *
 * Some names have special meanings, and they must be initialized immediately.
 *
 * A BukkitPluginRef to "Bukkit" can be used to determine whether Bukkit is
 * present at all. This will always be initialized to null on the client, and
 * will be initialized to some unspecified object on the server.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BukkitPluginRef {
    /**
     * A reference to a Bukkit Plugin by its name. The name must match exactly
     * to be populated correctly.
     *
     * @return The name of the plugin which will be filled
     */
    String value();
}
