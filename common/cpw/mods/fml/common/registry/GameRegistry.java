package cpw.mods.fml.common.registry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.CraftingManager;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.FurnaceRecipes;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.IInventory;
import net.minecraft.src.IRecipe;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraft.src.WorldType;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.IDispenseHandler;
import cpw.mods.fml.common.IFuelHandler;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderException;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.Mod.Block;
import cpw.mods.fml.common.ModContainer;

@SuppressWarnings("serial")
class InvalidBlockAnnotation extends RuntimeException
{
    InvalidBlockAnnotation(String modId, String typeName, Throwable e)
    {
        super("The mod " + modId + " has an invalid block annotation or uses an invalid block type (" + typeName + ").",
            e);
    }

    public InvalidBlockAnnotation(String modId, String reason)
    {
        super("The mod " + modId + " has an invalid block annotation: " + reason);
    }
}

public class GameRegistry
{
    // private static Multimap<ModContainer, BlockProxy> blockRegistry = ArrayListMultimap.create();
    // private static Multimap<ModContainer, ItemProxy> itemRegistry = ArrayListMultimap.create();
    private static Set<IWorldGenerator> worldGenerators = Sets.newHashSet();
    private static List<IFuelHandler> fuelHandlers = Lists.newArrayList();
    private static List<ICraftingHandler> craftingHandlers = Lists.newArrayList();
    private static List<IDispenseHandler> dispenserHandlers = Lists.newArrayList();

    /**
     * Register a world generator - something that inserts new block types into the world
     *
     * @param generator
     */
    public static void registerWorldGenerator(IWorldGenerator generator)
    {
        worldGenerators.add(generator);
    }

    /**
     * Callback hook for world gen - if your mod wishes to add extra mod related generation to the world
     * call this
     *
     * @param chunkX
     * @param chunkZ
     * @param world
     * @param chunkGenerator
     * @param chunkProvider
     */
    public static void generateWorld(int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
    {
        long worldSeed = world.func_72905_C();
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        fmlRandom.setSeed((xSeed * chunkX + zSeed * chunkZ) ^ worldSeed);

        for (IWorldGenerator generator : worldGenerators)
        {
            generator.generate(fmlRandom, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
        }
    }

    /**
     * Register a handler for dispensers
     *
     * @param handler
     */
    public static void registerDispenserHandler(IDispenseHandler handler)
    {
        dispenserHandlers.add(handler);
    }


    /**
     * Callback hook for dispenser activities - if you add a block and want mods to be able
     * to extend their dispenser related activities to it call this
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @param xVelocity
     * @param zVelocity
     * @param item
     * @return
     */
    public static int tryDispense(World world, double x, double y, double z, int xVelocity, int zVelocity, ItemStack item)
    {
        for (IDispenseHandler handler : dispenserHandlers)
        {
            int dispensed = handler.dispense(x, y, z, xVelocity, zVelocity, world, item);
            if (dispensed>-1)
            {
                return dispensed;
            }
        }
        return -1;
    }

    /**
     * Internal method for creating an @Block instance
     * 
     * @param container
     * @param type
     * @param annotation
     * @return
     * @throws InvalidBlockAnnotation
     */
    public static net.minecraft.src.Block buildBlock(ModContainer container, Class<?> type, Block annotation)
            throws InvalidBlockAnnotation
    {
        Class<? extends net.minecraft.src.Block> typeCast;
        try
        {
            typeCast = type.asSubclass(net.minecraft.src.Block.class);
        }
        catch (ClassCastException e)
        {
            throw new InvalidBlockAnnotation(container.getModId(), type.getName(), e);
        }

        Class<? extends ItemBlock> itemTypeClass = annotation.itemTypeClass();
        if (itemTypeClass == null)
        {
            throw new InvalidBlockAnnotation(container.getModId(), "The itemTypeClass parameter is null.");
        }

        net.minecraft.src.Block o;
        try
        {
            o = typeCast.getConstructor(int.class).newInstance(findSpareBlockId());
        }
        catch (NoSuchMethodException e)
        {
            throw new InvalidBlockAnnotation(container.getModId(), type.getName(), e);
        }
        catch (InstantiationException e)
        {
            throw new InvalidBlockAnnotation(container.getModId(), type.getName(), e);
        }
        catch (IllegalAccessException e)
        {
            throw new InvalidBlockAnnotation(container.getModId(), type.getName(), e);
        }
        catch (InvocationTargetException e)
        {
            Throwables.propagateIfPossible(e.getTargetException());
            throw new InvalidBlockAnnotation(container.getModId(), type.getName(), e);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        registerBlock(o, itemTypeClass);
        return o;
    }

    /**
     * Private and not yet working properly
     *
     * @return
     */
    private static int findSpareBlockId()
    {
        return BlockTracker.nextBlockId();
    }

    /**
     * Register a block with the world
     *
     */
    public static void registerBlock(net.minecraft.src.Block block)
    {
        registerBlock(block, ItemBlock.class);
    }

    /**
     * Register a block with the world, with the specified item class
     *
     * @param block
     * @param itemclass
     */
    public static void registerBlock(net.minecraft.src.Block block, Class<? extends ItemBlock> itemclass)
    {
        if (Loader.instance().isInState(LoaderState.CONSTRUCTING))
        {
            FMLLog.warning(
                    "The mod %s is attempting to register a block while it is being constructed. This is bad practice -- please use a proper lifecycle event.",
                    Loader.instance().activeModContainer());
        }
        try
        {
            assert block != null : "registerBlock: block cannot be null";
            assert itemclass != null : "registerBlock: itemclass cannot be null";
            int blockItemId = block.field_71990_ca - 256;
            try
            {
                itemclass.getConstructor(int.class, block.getClass()).newInstance(blockItemId, block);
            }
            catch (NoSuchMethodException e)
            {
                itemclass.getConstructor(int.class).newInstance(blockItemId);
            }
        }
        catch (InvocationTargetException e)
        {
            Throwables.propagateIfPossible(e);
            throw new RuntimeException(e);
        }
        catch (Exception e)
        {
            throw new LoaderException(e);
        }
        // TODO
        // blockRegistry.put(Loader.instance().activeModContainer(),
        // (BlockProxy) block);
    }

    public static void addRecipe(ItemStack output, Object... params)
    {
        CraftingManager.func_77594_a().func_77595_a(output, params);
    }

    public static void addShapelessRecipe(ItemStack output, Object... params)
    {
        CraftingManager.func_77594_a().func_77596_b(output, params);
    }

    public static void addRecipe(IRecipe recipe)
    {
        CraftingManager.func_77594_a().func_77592_b().add(recipe);
    }

    public static void addSmelting(int input, ItemStack output, float xp)
    {
        FurnaceRecipes.func_77602_a().func_77600_a(input, output, xp);
    }

    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id)
    {
        TileEntity.func_70306_a(tileEntityClass, id);
    }

    public static void addBiome(BiomeGenBase biome)
    {
        WorldType.field_77137_b.addNewBiome(biome);
    }

    public static void removeBiome(BiomeGenBase biome)
    {
        WorldType.field_77137_b.removeBiome(biome);
    }

    public static void registerFuelHandler(IFuelHandler handler)
    {
        fuelHandlers.add(handler);
    }
    public static int getFuelValue(ItemStack itemStack)
    {
        int fuelValue = 0;
        for (IFuelHandler handler : fuelHandlers)
        {
            fuelValue = Math.max(fuelValue, handler.getBurnTime(itemStack));
        }
        return fuelValue;
    }

    public static void registerCraftingHandler(ICraftingHandler handler)
    {
        craftingHandlers.add(handler);
    }

    public static void onItemCrafted(EntityPlayer player, ItemStack item, IInventory craftMatrix)
    {
        for (ICraftingHandler handler : craftingHandlers)
        {
            handler.onCrafting(player, item, craftMatrix);
        }
    }

    public static void onItemSmelted(EntityPlayer player, ItemStack item)
    {
        for (ICraftingHandler handler : craftingHandlers)
        {
            handler.onSmelting(player, item);
        }
    }

}
