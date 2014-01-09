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

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

/**
 * Implement this to get your block to render in a special manner
 *
 */
public interface ISimpleBlockRenderingHandler
{
	/**
	 * Render a block in the inventory
	 * @param block The block to render
	 * @param metadata The metadate of the block
	 * @param modelId The id of the block's model
	 * @param renderer A renderer for rendering the block
	 */
    public abstract void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer);
    /**
     * Render a block in the world
     * @param world The world to render in
     * @param x The x position of the block
     * @param y The y position of the block
     * @param z The z position of the block
     * @param block The block to render
     * @param modelId The id of the block's model
     * @param renderer A renderer to use to render the block
     * @return
     */
    public abstract boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer);

    /**
     * Should the block render 3D in the inventory
     * @param modelId
     * @return Whether or not to render as 3D in the inventory
     */
    public abstract boolean shouldRender3DInInventory(int modelId);

    /**
     * Get the model ID for the block
     * @return The model ID for the block
     */
    public abstract int getRenderId();
}
