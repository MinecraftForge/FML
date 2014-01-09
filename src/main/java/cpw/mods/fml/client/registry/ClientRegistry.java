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

package cpw.mods.fml.client.registry;

import org.apache.commons.lang3.ArrayUtils;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.tileentity.TileEntity;

/**
 * Static utlity class for registering various client related things
 */
public class ClientRegistry
{
    /**
     *
     * Utility method for registering a tile entity and it's renderer at once - generally you should register them separately
     *
     * @param tileEntityClass The class of the tile entity being registered
     * @param id The id to register the tile entity under
     * @param specialRenderer The {@link TileEntitySpecialRenderer} for this tile entity
     */
    public static void registerTileEntity(Class <? extends TileEntity > tileEntityClass, String id, TileEntitySpecialRenderer specialRenderer)
    {
        GameRegistry.registerTileEntity(tileEntityClass, id);
        bindTileEntitySpecialRenderer(tileEntityClass, specialRenderer);
    }

    /**
     * Bind a special renderer to a given tile entity
     * @param tileEntityClass The tile entity to bind the renderer to
     * @param specialRenderer The special renderer that will be bound to the entity
     */
    @SuppressWarnings("unchecked")
    public static void bindTileEntitySpecialRenderer(Class <? extends TileEntity> tileEntityClass, TileEntitySpecialRenderer specialRenderer)
    {
        TileEntityRendererDispatcher.field_147556_a.field_147559_m.put(tileEntityClass, specialRenderer);
        specialRenderer.func_147497_a(TileEntityRendererDispatcher.field_147556_a);
    }

    /**
     * Register a keybinding
     * @param key The keybinding to register
     */
    public static void registerKeyBinding(KeyBinding key)
    {
        Minecraft.func_71410_x().field_71474_y.field_74324_K = ArrayUtils.add(Minecraft.func_71410_x().field_71474_y.field_74324_K, key);
    }
}
