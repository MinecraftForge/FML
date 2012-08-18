package cpw.mods.mockmod;

import net.minecraft.src.Block;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.Material;

public class MockBlock extends Block
{
    public MockBlock(int id)
    {
        super(id, 138, Material.field_76259_v);
        func_71849_a(CreativeTabs.field_78026_f);
        System.out.printf("Mockblock is id %d\n", id);
    }
}
