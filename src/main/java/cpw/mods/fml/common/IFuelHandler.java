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

import net.minecraft.item.ItemStack;

/**
 * Interface for adding custom fuels to the furnace
 */
public interface IFuelHandler
{
	/**
	 * Get the number of ticks a fuel burns
	 * @param fuel The fuel to consider
	 * @return The # of ticks the fuel will burn
	 */
    int getBurnTime(ItemStack fuel);
}
