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

import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;

/**
 * @author cpw
 *
 */
public class RenderingRegistry
{
    private static final RenderingRegistry INSTANCE = new RenderingRegistry();

    private List<EntityRendererInfo> entityRenderers = Lists.newArrayList();

    private Map<Integer, IBlockRenderer> blockRendererMap = Maps.newHashMap();
    
    /**
     * Register an entity rendering handler. This will, after mod initialization, be inserted into the main
     * render map for entities
     *
     * @param entityClass
     * @param renderer
     */
    public static void registerEntityRenderingHandler(Class<? extends Entity> entityClass, Render renderer)
    {
        instance().entityRenderers.add(new EntityRendererInfo(entityClass, renderer));
    }

    public static void registerBlockRenderer(IBlockRenderer blockRenderer)
    {
        instance().blockRendererMap.put(blockRenderer.getRenderID(), blockRenderer);
    }
    
    public static void registerBlockRenderer(IBlockRenderer blockRenderer, int renderId)
    {
        instance().blockRendererMap.put(renderId, blockRenderer);
    }
    
    @Deprecated public static RenderingRegistry instance()
    {
        return INSTANCE;
    }

    private static class EntityRendererInfo
    {
        public EntityRendererInfo(Class<? extends Entity> target, Render renderer)
        {
            this.target = target;
            this.renderer = renderer;
        }
        private Class<? extends Entity> target;
        private Render renderer;
    }

    public static boolean renderWorldBlock(IBlockState blockState, BlockPos blockPos, IBlockAccess blockAccess, WorldRenderer worldRenderer, int modelId)
    {
        IBlockRenderer blockRenderer = instance().blockRendererMap.get(modelId);
        return blockRenderer == null ? false : blockRenderer.renderWorldBlock(blockState, blockPos, blockAccess, worldRenderer, modelId);
    }
    
    /*
    public void loadEntityRenderers(Map<Class<? extends Entity>, Render> rendererMap)
    {
        for (EntityRendererInfo info : entityRenderers)
        {
            rendererMap.put(info.target, info.renderer);
            info.renderer.setRenderManager(Minecraft.getMinecraft().func_175598_ae());
        }
    }*/
}
