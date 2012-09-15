package cpw.mods.fml.common.registry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.CraftingManager;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.FurnaceRecipes;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.IInventory;
import net.minecraft.src.IRecipe;
import net.minecraft.src.Item;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTBase;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraft.src.WorldInfo;
import net.minecraft.src.WorldType;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.IDispenseHandler;
import cpw.mods.fml.common.IDispenserHandler;
import cpw.mods.fml.common.IFuelHandler;
import cpw.mods.fml.common.IPickupNotifier;
import cpw.mods.fml.common.IPlayerTracker;
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
        super("The mod " + modId + " has an invalid block annotation or uses invalid block type " + typeName + ".", e);
    }

    public InvalidBlockAnnotation(String modId, String reason)
    {
        super("The mod " + modId + " has an invalid block annotation: " + reason);
    }
}

public class GameRegistry
{
    private static HashMap<ModContainer, HashMap<String, net.minecraft.src.Block>> blockRegistry = new HashMap();
    private static BitSet movableBlocks = new BitSet(4096);
    // private static Multimap<ModContainer, ItemProxy> itemRegistry = ArrayListMultimap.create();
    private static Set<IWorldGenerator> worldGenerators = Sets.newHashSet();
    private static List<IFuelHandler> fuelHandlers = Lists.newArrayList();
    private static List<ICraftingHandler> craftingHandlers = Lists.newArrayList();
    private static List<IDispenserHandler> dispenserHandlers = Lists.newArrayList();
    private static List<IPickupNotifier> pickupHandlers = Lists.newArrayList();
    private static List<IPlayerTracker> playerTrackers = Lists.newArrayList();

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

    public static void registerDispenserHandler(IDispenserHandler handler)
    {
        dispenserHandlers.add(handler);
    }
    /**
     * Register a handler for dispensers
     *
     * @param handler
     */
    @Deprecated
    public static void registerDispenserHandler(final IDispenseHandler handler)
    {
        registerDispenserHandler(new IDispenserHandler()
        {

            @Override
            public int dispense(int x, int y, int z, int xVelocity, int zVelocity, World world, ItemStack item, Random random, double entX, double entY, double entZ)
            {
                return handler.dispense(x, y, z, xVelocity, zVelocity, world, item, random, entX, entY, entZ);
            }
        });
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
    public static int tryDispense(World world, int x, int y, int z, int xVelocity, int zVelocity, ItemStack item, Random random, double entX, double entY, double entZ)
    {
        for (IDispenserHandler handler : dispenserHandlers)
        {
            int dispensed = handler.dispense(x, y, z, xVelocity, zVelocity, world, item, random, entX, entY, entZ);
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
     * @return Block instance that was created
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
        // TODO: use java7 multi-catch when available.
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

        registerManagedBlock(annotation.name(), o, itemTypeClass);
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
            FMLLog.warning("The mod %s is attempting to register a block whilst it it being constructed. This is bad modding practice - please use a proper mod lifecycle event.", Loader.instance().activeModContainer());
        }
        try
        {
            assert block != null : "registerBlock: block cannot be null";
            assert itemclass != null : "registerBlock: itemclass cannot be null";
            int blockItemId = block.field_71990_ca - 256;
            itemclass.getConstructor(int.class).newInstance(blockItemId);
        }
        catch (Exception e)
        {
            FMLLog.log(Level.SEVERE, e, "Caught an exception during block registration");
            throw new LoaderException(e);
        }
    }

    /**
     * Register an FML-managed block with the world. This block's ID may be changed by FML at world load time.
     *
     * @param name (optional) Name under which the block ID will be stored. If null, the block's class name will be used.
     * @param block Block object that should be registered. It must have a valid block ID.
     */
    public static void registerManagedBlock(String name, net.minecraft.src.Block block)
    {
        registerManagedBlock(name, block, ItemBlock.class);
    }

    /**
     * Register an FML-managed block with the world using a custom ItemBlock. This block's ID may be changed by FML at world load time.
     *
     * @param name (optional) Name under which the block ID will be stored. If null, the block's class name will be used.
     * @param block Block object that should be registered. It must have a valid block ID.
     * @param itemclass Custom ItemBlock class to use with this block.
     */
    public static void registerManagedBlock(String name, net.minecraft.src.Block block, Class<? extends ItemBlock> itemclass)
    {
        ModContainer activeModContainer = Loader.instance().activeModContainer();

        registerBlock(block, itemclass);

        if (name == null) {
            name = block.getClass().getName();
        }
        HashMap<String, net.minecraft.src.Block> blockMap = blockRegistry.get(activeModContainer);
        if (blockMap == null)
        {
            blockMap = new HashMap();
            blockRegistry.put(activeModContainer, blockMap);
        }
        if (blockMap.containsKey(name)) {
            FMLLog.warning(
                    "The mod %s is attempting to register multiple blocks under name %s. This is almost always an error.",
                    activeModContainer, name);
            return;
        }
        blockMap.put(name, block);
        movableBlocks.set(block.field_71990_ca);
    }

    /**
     * FML internal use only.
     */
    public static NBTTagCompound getBlockData()
    {
        NBTTagCompound blockData = new NBTTagCompound();
        for (Entry<ModContainer, HashMap<String, net.minecraft.src.Block>> modEntry : blockRegistry.entrySet())
        {
            ModContainer container = modEntry.getKey();
            NBTTagCompound modData = new NBTTagCompound();
            for (Entry<String, net.minecraft.src.Block> blockEntry : modEntry.getValue().entrySet())
            {
                String name = blockEntry.getKey();
                net.minecraft.src.Block block = blockEntry.getValue();
                modData.func_74768_a(name, block.field_71990_ca);
            }
            blockData.func_74782_a(container.getModId(), modData);
        }
        return blockData;
    }

    /**
     * FML internal use only.
     */
    public static void setBlockData(NBTTagCompound blockData)
    {
        Loader loader = Loader.instance();
        for (Object modObj : blockData.func_74758_c())
        {
            if (!(modObj instanceof NBTTagCompound))
            {
                throw new RuntimeException("Corruption detected in world save FML data");
            }

            NBTTagCompound modNbt = (NBTTagCompound) modObj;
            String modId = modNbt.func_74740_e();
            ModContainer mod = loader.getModById(modId);

            if (mod == null)
            {
                throw new RuntimeException("World info references missing mod " + modId);
            }

            HashMap<String, net.minecraft.src.Block> blockMap = blockRegistry.get(mod);
            if (blockMap == null)
            {
                throw new RuntimeException("World/Mod mismatch: mod " + modId + " has no registered blocks.");
            }

            for (Object blockObj : modNbt.func_74758_c())
            {
                if (!(blockObj instanceof NBTTagInt))
                {
                    throw new RuntimeException("Corruption detected in world save for mod " + modId);
                }

                NBTTagInt blockNbt = (NBTTagInt) blockObj;
                String blockName = blockNbt.func_74740_e();
                net.minecraft.src.Block block = blockMap.get(blockName);
                if (block == null)
                {
                    throw new RuntimeException("World/Mod mismatch: mod " + modId + " has no block " + blockName);
                }
                forceBlockId(block, blockNbt.field_74748_a);
            }
        }
    }

    private static void forceBlockId(net.minecraft.src.Block block, int newBlockId)
    {
        int curBlockId = block.field_71990_ca;
        if (curBlockId == newBlockId)
        {
            return;
        }

        assert movableBlocks.get(curBlockId);

        net.minecraft.src.Block[] blockList = net.minecraft.src.Block.field_71973_m;
        Item[] itemList = Item.field_77698_e;
        Item item = itemList[curBlockId];

        if (blockList[newBlockId] != null)
        {
            // Swap the blocks if possible.
            if (!movableBlocks.get(newBlockId))
            {
                throw new RuntimeException("Unable to relocate block id " + newBlockId);
            }

            net.minecraft.src.Block movingBlock = blockList[newBlockId];
            Item movingItem = itemList[newBlockId];

            blockList[curBlockId] = movingBlock;
            itemList[curBlockId] = movingItem;

            movingBlock.field_71990_ca = curBlockId;
            movingItem.field_77779_bT = curBlockId;
        } else {
            BlockTracker.releaseBlockId(curBlockId);
            movableBlocks.clear(curBlockId);

            blockList[curBlockId] = null;
            itemList[curBlockId] = null;

            BlockTracker.reserveBlockId(newBlockId);
            movableBlocks.set(newBlockId);
        }

        blockList[newBlockId] = block;
        itemList[newBlockId] = item;

        block.field_71990_ca = newBlockId;
        item.field_77779_bT = newBlockId;
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

    public static void registerPickupHandler(IPickupNotifier handler)
    {
        pickupHandlers.add(handler);
    }

    public static void onPickupNotification(EntityPlayer player, EntityItem item)
    {
        for (IPickupNotifier notify : pickupHandlers)
        {
            notify.notifyPickup(item, player);
        }
    }

    public static void registerPlayerTracker(IPlayerTracker tracker)
	{
		playerTrackers.add(tracker);
	}

	public static void onPlayerLogin(EntityPlayer player)
	{
		for(IPlayerTracker tracker : playerTrackers)
			tracker.onPlayerLogin(player);
	}

	public static void onPlayerLogout(EntityPlayer player)
	{
		for(IPlayerTracker tracker : playerTrackers)
			tracker.onPlayerLogout(player);
	}

	public static void onPlayerChangedDimension(EntityPlayer player)
	{
		for(IPlayerTracker tracker : playerTrackers)
			tracker.onPlayerChangedDimension(player);
	}

	public static void onPlayerRespawn(EntityPlayer player)
	{
		for(IPlayerTracker tracker : playerTrackers)
			tracker.onPlayerRespawn(player);
	}
}
