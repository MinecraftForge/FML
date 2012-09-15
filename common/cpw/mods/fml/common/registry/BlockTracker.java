package cpw.mods.fml.common.registry;

import java.util.BitSet;

import net.minecraft.src.Block;
import net.minecraft.src.Item;

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
        for (int i = 1; i < blockList.length; i++)
        {
            if (blockList[i] != null)
            {
                allocatedBlocks.set(i);
            }
        }
        // We also do not want to use a block ID that conflicts with an item.
        Item[] itemList = Item.field_77698_e;
        for (int i = 1; i < blockList.length && i < itemList.length; i++)
        {
            if (itemList[i] != null)
            {
                allocatedBlocks.set(i);
            }
        }
    }

    public static int nextBlockId()
    {
        Block[] blockList = Block.field_71973_m;
        Item[] itemList = Item.field_77698_e;

        int blockId;
        do
        {
            blockId = INSTANCE.getNextBlockId();
        } while (blockList[blockId] != null || itemList[blockId] != null);

        return blockId;
    }

    private int getNextBlockId()
    {
        int idx = allocatedBlocks.nextClearBit(1);
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

    public static void releaseBlockId(int id)
    {
        INSTANCE.doReleaseId(id);
    }

    private void doReleaseId(int id)
    {
        allocatedBlocks.clear(id);
    }
}
