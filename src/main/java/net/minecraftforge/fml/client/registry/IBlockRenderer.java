package net.minecraftforge.fml.client.registry;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;

public interface IBlockRenderer {

    public boolean renderWorldBlock(IBlockState blockState, BlockPos blockPos, IBlockAccess blockAccess, WorldRenderer worldRenderer, int modelId);

    public int getRenderID();
}
