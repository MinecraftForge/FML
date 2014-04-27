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

package cpw.mods.fml.common;

import java.io.File;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;

import org.apache.logging.log4j.Level;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.client.FMLFileResourcePack;
import cpw.mods.fml.client.FMLFolderResourcePack;
import cpw.mods.fml.common.asm.FMLSanityChecker;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.Side;

/**
 * @author cpw
 *
 */
public class FMLContainer extends DummyModContainer implements WorldAccessContainer
{
    public FMLContainer()
    {
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId="FML";
        meta.name="Forge Mod Loader";
        meta.version=Loader.instance().getFMLVersionString();
        meta.credits="Made possible with help from many people";
        meta.authorList=Arrays.asList("cpw", "LexManos", "Player");
        meta.description="The Forge Mod Loader provides the ability for systems to load mods " +
                    "from the file system. It also provides key capabilities for mods to be able " +
                    "to cooperate and provide a good modding environment. ";
        meta.url="https://github.com/MinecraftForge/FML/wiki";
        meta.updateUrl="https://github.com/MinecraftForge/FML/wiki";
        meta.screenshots=new String[0];
        meta.logoFile="";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void modConstruction(FMLConstructionEvent evt)
    {
        NetworkRegistry.INSTANCE.register(this, this.getClass(), null, evt.getASMHarvestedData());
        FMLNetworkHandler.registerChannel(this, evt.getSide());
    }

    @NetworkCheckHandler
    public boolean checkModLists(Map<String,String> modList, Side side)
    {
        return Loader.instance().checkRemoteModList(modList,side);
    }
    @Override
    public NBTTagCompound getDataForWriting(SaveHandler handler, WorldInfo info)
    {
        NBTTagCompound fmlData = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (ModContainer mc : Loader.instance().getActiveModList())
        {
            NBTTagCompound mod = new NBTTagCompound();
            mod.setString("ModId", mc.getModId());
            mod.setString("ModVersion", mc.getVersion());
            list.appendTag(mod);
        }
        fmlData.setTag("ModList", list);
        // name <-> id mappings
        NBTTagList dataList = new NBTTagList();
        FMLLog.fine("Gathering id map for writing to world save %s", info.getWorldName());
        Map<String,Integer> itemList = GameData.buildItemDataList();
        for (Entry<String, Integer> item : itemList.entrySet())
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("K",item.getKey());
            tag.setInteger("V",item.getValue());
            dataList.appendTag(tag);
        }
        fmlData.setTag("ItemData", dataList);
        // blocked ids
        fmlData.setIntArray("BlockedItemIds", GameData.getBlockedIds());
        // block aliases
        NBTTagList blockAliasList = new NBTTagList();
        for (Entry<String, String> entry : GameData.getBlockRegistry().getAliases().entrySet())
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("K", entry.getKey());
            tag.setString("V", entry.getValue());
            blockAliasList.appendTag(tag);
        }
        fmlData.setTag("BlockAliases", blockAliasList);
        // item aliases
        NBTTagList itemAliasList = new NBTTagList();
        for (Entry<String, String> entry : GameData.getItemRegistry().getAliases().entrySet())
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("K", entry.getKey());
            tag.setString("V", entry.getValue());
            itemAliasList.appendTag(tag);
        }
        fmlData.setTag("ItemAliases", itemAliasList);

        return fmlData;
    }

    @Override
    public void readData(SaveHandler handler, WorldInfo info, Map<String, NBTBase> propertyMap, NBTTagCompound tag)
    {
        if (tag.hasKey("ModList"))
        {
            NBTTagList modList = tag.getTagList("ModList", (byte)10);
            for (int i = 0; i < modList.tagCount(); i++)
            {
                NBTTagCompound mod = modList.getCompoundTagAt(i);
                String modId = mod.getString("ModId");
                String modVersion = mod.getString("ModVersion");
                ModContainer container = Loader.instance().getIndexedModList().get(modId);
                if (container == null)
                {
                    FMLLog.log("fml.ModTracker", Level.ERROR, "This world was saved with mod %s which appears to be missing, things may not work well", modId);
                    continue;
                }
                if (!modVersion.equals(container.getVersion()))
                {
                    FMLLog.log("fml.ModTracker", Level.INFO, "This world was saved with mod %s version %s and it is now at version %s, things may not work well", modId, modVersion, container.getVersion());
                }
            }
        }

        List<String> failedElements = null;

        if (tag.hasKey("ModItemData"))
        {
            FMLLog.info("Attempting to convert old world data to new system. This may be trouble!");
            NBTTagList modList = tag.getTagList("ModItemData", (byte)10);
            Map<String,Integer> dataList = Maps.newLinkedHashMap();
            for (int i = 0; i < modList.tagCount(); i++)
            {
                NBTTagCompound itemTag = modList.getCompoundTagAt(i);
                String modId = itemTag.getString("ModId");
                String itemType = itemTag.getString("ItemType");
                int itemId = itemTag.getInteger("ItemId");
                int ordinal = itemTag.getInteger("ordinal");
                String forcedModId = itemTag.hasKey("ForcedModId") ? itemTag.getString("ForcedModId") : null;
                String forcedName = itemTag.hasKey("ForcedName") ? itemTag.getString("ForcedName") : null;
                if (forcedName == null)
                {
                    FMLLog.warning("Found unlabelled item in world save, this may cause problems. The item type %s:%d will not be present", itemType, ordinal);
                }
                else
                {
                    // all entries are Items, blocks were only saved through their ItemBlock
                    String itemLabel = String.format("%c%s:%s", '\u0002', forcedModId != null ? forcedModId : modId, forcedName);
                    dataList.put(itemLabel, itemId);
                }
            }
            failedElements = GameData.injectWorldIDMap(dataList, true, true);

        }
        else if (tag.hasKey("ItemData"))
        {
            // name <-> id mappings
            NBTTagList list = tag.getTagList("ItemData", 10);
            Map<String,Integer> dataList = Maps.newLinkedHashMap();
            for (int i = 0; i < list.tagCount(); i++)
            {
                NBTTagCompound dataTag = list.getCompoundTagAt(i);
                dataList.put(dataTag.getString("K"), dataTag.getInteger("V"));
            }

            Set<Integer> blockedIds = new HashSet<Integer>();

            if (!tag.hasKey("BlockedItemIds")) // no blocked id info -> old 1.7 save
            {
                // old early 1.7 save potentially affected by the registry mapping bug
                // fix the ids the best we can...
                GameData.fixBrokenIds(dataList, blockedIds);
            }

            // blocked ids
            for (int id : tag.getIntArray("BlockedItemIds"))
            {
                blockedIds.add(id);
            }
            // block aliases
            Map<String, String> blockAliases = new HashMap<String, String>();
            list = tag.getTagList("BlockAliases", 10);
            for (int i = 0; i < list.tagCount(); i++)
            {
                NBTTagCompound dataTag = list.getCompoundTagAt(i);
                blockAliases.put(dataTag.getString("K"), dataTag.getString("V"));
            }
            // item aliases
            Map<String, String> itemAliases = new HashMap<String, String>();
            list = tag.getTagList("ItemAliases", 10);
            for (int i = 0; i < list.tagCount(); i++)
            {
                NBTTagCompound dataTag = list.getCompoundTagAt(i);
                itemAliases.put(dataTag.getString("K"), dataTag.getString("V"));
            }

            failedElements = GameData.injectWorldIDMap(dataList, blockedIds, blockAliases, itemAliases, true, true);
        }

        if (failedElements != null && !failedElements.isEmpty())
        {
            String text = "Forge Mod Loader could not load this save.\n\n" +
            "There are "+failedElements.size()+" unassigned blocks and items in this save.\n" +
                    "You will not be able to load until they are present again.\n\n" +
                    "Missing Blocks/Items:\n";

            for (String s : failedElements) text += s + "\n";

            StartupQuery.notify(text);
            StartupQuery.abort();
        }
    }


    @Override
    public Certificate getSigningCertificate()
    {
        Certificate[] certificates = getClass().getProtectionDomain().getCodeSource().getCertificates();
        return certificates != null ? certificates[0] : null;
    }

    @Override
    public File getSource()
    {
        return FMLSanityChecker.fmlLocation;
    }

    @Override
    public Class<?> getCustomResourcePackClass()
    {
        return getSource().isDirectory() ? FMLFolderResourcePack.class : FMLFileResourcePack.class;
    }

    @Override
    public String getGuiClassName()
    {
        return "cpw.mods.fml.client.FMLConfigGuiFactory";
    }

    @Override
    public Object getMod()
    {
        return this;
    }
}
