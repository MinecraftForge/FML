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

package net.minecraftforge.fml.client.registry;

import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

public class ClientRegistry
{
    /**
     *
     * Utility method for registering a tile entity and it's renderer at once - generally you should register them separately
     *
     * @param tileEntityClass
     * @param id
     * @param specialRenderer
     */
    public static void registerTileEntity(Class <? extends TileEntity > tileEntityClass, String id, TileEntitySpecialRenderer specialRenderer)
    {
        GameRegistry.registerTileEntity(tileEntityClass, id);
        bindTileEntitySpecialRenderer(tileEntityClass, specialRenderer);
    }

    @SuppressWarnings("unchecked")
    public static void bindTileEntitySpecialRenderer(Class <? extends TileEntity> tileEntityClass, TileEntitySpecialRenderer specialRenderer)
    {
        TileEntityRendererDispatcher.instance.mapSpecialRenderers.put(tileEntityClass, specialRenderer);
        specialRenderer.setRendererDispatcher(TileEntityRendererDispatcher.instance);
    }

    public static void registerKeyBinding(KeyBinding key)
    {
        Minecraft.getMinecraft().gameSettings.keyBindings = ArrayUtils.add(Minecraft.getMinecraft().gameSettings.keyBindings, key);
    }
    
    public void setItemModel(Item item, int meta, String model)
    {
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, meta, new ModelResourceLocation(this.addPrefix(model), "inventory"));
    }
    
    public void setItemModel(Item item, String model)
    {
        this.setItemModel(item, 0, model);
    }

    public void setBlockItemModel(Block block, int itemMeta, String model)
    {
        this.setItemModel(Item.getItemFromBlock(block), itemMeta, model);
    }

    public void setBlockItemModel(Block block, String model)
    {
        this.setBlockItemModel(block, 0, model);
    }
    
    private String addPrefix(String name)
    {
        int index = name.lastIndexOf(':');
        String oldPrefix = "";
        String newPrefix = "";
        
        ModContainer mc = Loader.instance().activeModContainer();
        if (mc != null)
        {
            newPrefix = mc.getModId();
        }
        else
        {
            newPrefix = "minecraft";
        }

        if (index != -1)
        {
            oldPrefix = name.substring(0, index);
            name = name.substring(index + 1);
        }

        if (!oldPrefix.isEmpty())
        {
            newPrefix = oldPrefix;
        }
        
        return newPrefix + ":" + name;
    }
}
