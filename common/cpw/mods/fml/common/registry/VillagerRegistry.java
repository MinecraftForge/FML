package cpw.mods.fml.common.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Collection;
import java.util.Collections;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.Item;
import net.minecraft.util.Tuple;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.gen.structure.ComponentVillageStartPiece;
import net.minecraft.world.gen.structure.StructureVillagePieceWeight;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import cpw.mods.fml.common.FMLLog;

/**
 * Registry for villager trading control
 *
 * @author cpw
 *
 */
public class VillagerRegistry
{
    private static final VillagerRegistry INSTANCE = new VillagerRegistry();

    private Multimap<Integer, IVillageTradeHandler> tradeHandlers = ArrayListMultimap.create();
    private Map<Class<?>, IVillageCreationHandler> villageCreationHandlers = Maps.newHashMap();
    private Map<Integer, String> newVillagers = Maps.newHashMap();
    private List<Integer> newVillagerIds = Lists.newArrayList();

    /**
     * Allow access to the {@link StructureVillagePieces} array controlling new village
     * creation so you can insert your own new village pieces
     *
     * @author cpw
     *
     */
    public interface IVillageCreationHandler
    {
        /**
         * Called when {@link MapGenVillage} is creating a new village
         *
         * @param random
         * @param i
         */
        StructureVillagePieceWeight getVillagePieceWeight(Random random, int i);

        /**
         * The class of the root structure component to add to the village
         */
        Class<?> getComponentClass();


        /**
         * Build an instance of the village component {@link StructureVillagePieces}
         * @param villagePiece
         * @param startPiece
         * @param pieces
         * @param random
         * @param p1
         * @param p2
         * @param p3
         * @param p4
         * @param p5
         */
        Object buildComponent(StructureVillagePieceWeight villagePiece, ComponentVillageStartPiece startPiece, List pieces, Random random, int p1,
                int p2, int p3, int p4, int p5);
    }

    /**
     * Allow access to the {@link MerchantRecipeList} for a villager type for manipulation
     *
     * @author cpw
     *
     */
    public interface IVillageTradeHandler
    {
        /**
         * Called to allow changing the content of the {@link MerchantRecipeList} for the villager
         * supplied during creation
         *
         * @param villager
         * @param recipeList
         */
        void manipulateTradesForVillager(EntityVillager villager, MerchantRecipeList recipeList, Random random);
    }

    public static VillagerRegistry instance()
    {
        return INSTANCE;
    }

    /**
     * Register a new skin for a villager type
     *
     * @param villagerId
     * @param villagerSkin
     */
    public void registerVillagerType(int villagerId, String villagerSkin)
    {
        if (newVillagers.containsKey(villagerId))
        {
            FMLLog.severe("Attempt to register duplicate villager id %d", villagerId);
            throw new RuntimeException();
        }
        newVillagers.put(villagerId, villagerSkin);
        newVillagerIds.add(villagerId);
    }

    /**
     * Register a new village creation handler
     *
     * @param handler
     */
    public void registerVillageCreationHandler(IVillageCreationHandler handler)
    {
        villageCreationHandlers.put(handler.getComponentClass(), handler);
    }

    /**
     * Register a new villager trading handler for the specified villager type
     *
     * @param villagerId
     * @param handler
     */
    public void registerVillageTradeHandler(int villagerId, IVillageTradeHandler handler)
    {
        tradeHandlers.put(villagerId, handler);
    }

    /**
     * Callback to setup new villager types
     *
     * @param villagerType
     * @param defaultSkin
     */
    public static String getVillagerSkin(int villagerType, String defaultSkin)
    {
        if (instance().newVillagers.containsKey(villagerType))
        {
            return instance().newVillagers.get(villagerType);
        }
        return defaultSkin;
    }
    
    /**
     * Returns a list of all added villager types
     * 
     * @return newVillagerIds
     */
    public static Collection<Integer> getRegisteredVillagers()
    {
        return Collections.unmodifiableCollection(instance().newVillagerIds);
    }
    /**
     * Callback to handle trade setup for villagers
     *
     * @param recipeList
     * @param villager
     * @param villagerType
     * @param random
     */
    public static void manageVillagerTrades(MerchantRecipeList recipeList, EntityVillager villager, int villagerType, Random random)
    {
        for (IVillageTradeHandler handler : instance().tradeHandlers.get(villagerType))
        {
            handler.manipulateTradesForVillager(villager, recipeList, random);
        }
    }

    public static void addExtraVillageComponents(ArrayList components, Random random, int i)
    {
        List<StructureVillagePieceWeight> parts = components;
        for (IVillageCreationHandler handler : instance().villageCreationHandlers.values())
        {
            parts.add(handler.getVillagePieceWeight(random, i));
        }
    }

    public static Object getVillageComponent(StructureVillagePieceWeight villagePiece, ComponentVillageStartPiece startPiece, List pieces, Random random,
            int p1, int p2, int p3, int p4, int p5)
    {
        return instance().villageCreationHandlers.get(villagePiece.field_75090_a).buildComponent(villagePiece, startPiece, pieces, random, p1, p2, p3, p4, p5);
    }


    public static void addEmeraldBuyRecipe(EntityVillager villager, MerchantRecipeList list, Random random, Item item, float chance, int min, int max)
    {
        if (min > 0 && max > 0)
        {
            EntityVillager.field_70958_bB.put(item.field_77779_bT, new Tuple(min, max));
        }
        villager.func_70948_a(list, item.func_77612_l(), random, chance);
    }

    public static void addEmeraldSellRecipe(EntityVillager villager, MerchantRecipeList list, Random random, Item item, float chance, int min, int max)
    {
        if (min > 0 && max > 0)
        {
            EntityVillager.field_70960_bC.put(item.field_77779_bT, new Tuple(min, max));
        }
        villager.func_70949_b(list, item.func_77612_l(), random, chance);
    }

    public static void applyRandomTrade(EntityVillager villager, Random rand)
    {
        int extra = instance().newVillagerIds.size();
        int trade = rand.nextInt(5 + extra);
        villager.func_70938_b(trade < 5 ? trade : instance().newVillagerIds.get(trade - 5));
    }
}
