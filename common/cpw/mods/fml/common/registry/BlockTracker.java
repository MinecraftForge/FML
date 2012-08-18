package cpw.mods.fml.common.registry;

import java.util.BitSet;

import net.minecraft.src.Block;

class BlockTracker
{
    private static final BlockTracker INSTANCE = new BlockTracker();
    private BitSet allocatedBlocks;

    private BlockTracker()
    {
        if (allocatedBlocks != null)
        {
            return;
        }
        
        allocatedBlocks = new BitSet(4096);
        allocatedBlocks.set(0);  // Never use ID 0.
        Block[] blockList = Block.field_71973_m;
        for (int i = 0; i < blockList.length; i++)
        {
            if (blockList[i] != null)
            {
                allocatedBlocks.set(i);
            }
        }
    }    
    
    public static int nextBlockId()
    {
        return INSTANCE.getNextBlockId();
    }

    private int getNextBlockId()
    {
        int idx = allocatedBlocks.nextClearBit(0);
        allocatedBlocks.set(idx);
        return idx;
    }

    public static void reserveBlockId(int id)
    {
        INSTANCE.doReserveId(id);
    }

    private void doReserveId(int id)
    {
        allocatedBlocks.set(id);
    }
}
