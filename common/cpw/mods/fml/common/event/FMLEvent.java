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

package cpw.mods.fml.common.event;

import net.minecraftforge.event.Event;
import cpw.mods.fml.common.ModContainer;

public class FMLEvent extends Event
{
    public final String getEventType()
    {
        return getClass().getSimpleName();
    }

	public void applyModContainer(ModContainer activeContainer) {
	    // NO OP
	}
}
