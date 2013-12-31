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

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;


/**
 * The main class for non-obfuscated hook handling code
 *
 * Anything that doesn't require obfuscated or client/server specific code should
 * go in this handler
 *
 * It also contains a reference to the sided handler instance that is valid
 * allowing for common code to access specific properties from the obfuscated world
 * without a direct dependency
 *
 * @author cpw
 *
 */
public class FMLCommonHandler
{
    /**
     * The singleton
     */
    private static final FMLCommonHandler INSTANCE = new FMLCommonHandler();
    /**
     * The delegate for side specific data and functions
     */
    private IFMLSidedHandler sidedDelegate;

    private Class<?> forge;
    private boolean noForge;
    private List<String> brandings;
    private List<String> brandingsNoMC;
    private List<ICrashCallable> crashCallables = Lists.newArrayList(Loader.instance().getCallableCrashInformation());
    private Set<SaveHandler> handlerSet = Sets.newSetFromMap(new MapMaker().weakKeys().<SaveHandler,Boolean>makeMap());
    private EventBus eventBus = new EventBus();

    /**
     * The FML event bus. Subscribe here for FML related events
     *
     * @return the event bus
     */
    public EventBus bus()
    {
        return eventBus;
    }

    /**
     * Attempt to load minecraft early, as well as register which side we are on.
     * @param handler The side we are on
     */
    public void beginLoading(IFMLSidedHandler handler)
    {
        sidedDelegate = handler;
        FMLLog.log("MinecraftForge", Level.INFO, "Attempting early MinecraftForge initialization");
        callForgeMethod("initialize");
        callForgeMethod("registerCrashCallable");
        FMLLog.log("MinecraftForge", Level.INFO, "Completed early MinecraftForge initialization");
    }

    /**
     * Get the instance of this singleton
     * @return the instance
     */
    public static FMLCommonHandler instance()
    {
        return INSTANCE;
    }
    /**
     * Find the container that associates with the supplied mod object
     * @param mod
     */
    public ModContainer findContainerFor(Object mod)
    {
        return Loader.instance().getReversedModObjectList().get(mod);
    }
    /**
     * Get the forge mod loader logging instance (goes to the forgemodloader log file)
     * @return The log instance for the FML log file
     */
    public Logger getFMLLogger()
    {
        return FMLLog.getLogger();
    }

    /**
     * Get the side we are on
     * @return The side we are on
     */
    public Side getSide()
    {
        return sidedDelegate.getSide();
    }

    /**
     * Return the effective side for the context in the game. This is dependent
     * on thread analysis to try and determine whether the code is running in the
     * server or not. Use at your own risk
     */
    public Side getEffectiveSide()
    {
        Thread thr = Thread.currentThread();
        if ((thr.getName().equals("Server thread")))
        {
            return Side.SERVER;
        }

        return Side.CLIENT;
    }
    /**
     * Raise an exception
     */
    public void raiseException(Throwable exception, String message, boolean stopGame)
    {
        FMLLog.log(Level.ERROR, exception, "Something raised an exception. The message was '%s'. 'stopGame' is %b", message, stopGame);
        if (stopGame)
        {
            getSidedDelegate().haltGame(message,exception);
        }
    }


    /**
     * Attempt to load Minecraft Forge
     * @return The main Minecraft Forge class, or null if it cannot be found
     */
    private Class<?> findMinecraftForge()
    {
        if (forge==null && !noForge)
        {
            try {
                forge = Class.forName("net.minecraftforge.common.MinecraftForge");
            } catch (Exception ex) {
                noForge = true;
            }
        }
        return forge;
    }

    /** 
     * Attempt to call a no-args method on the main Minecraft forge class
     * @param method The name of the method to call
     * @return The object returned by the called method or null if forge was not found
     */
    private Object callForgeMethod(String method)
    {
        if (noForge)
            return null;
        try
        {
            return findMinecraftForge().getMethod(method).invoke(null);
        }
        catch (Exception e)
        {
            // No Forge installation
            return null;
        }
    }

    /**
     * Attempt to calculate the list of brandings for this version of minecraft.
     */
    public void computeBranding()
    {
        if (brandings == null)
        {
            Builder<String> brd = ImmutableList.<String>builder();
            brd.add(Loader.instance().getMCVersionString());
            brd.add(Loader.instance().getMCPVersionString());
            brd.add("FML v"+Loader.instance().getFMLVersionString());
            String forgeBranding = (String) callForgeMethod("getBrandingVersion");
            if (!Strings.isNullOrEmpty(forgeBranding))
            {
                brd.add(forgeBranding);
            }
            if (sidedDelegate!=null)
            {
            	brd.addAll(sidedDelegate.getAdditionalBrandingInformation());
            }
            if (Loader.instance().getFMLBrandingProperties().containsKey("fmlbranding"))
            {
                brd.add(Loader.instance().getFMLBrandingProperties().get("fmlbranding"));
            }
            int tModCount = Loader.instance().getModList().size();
            int aModCount = Loader.instance().getActiveModList().size();
            brd.add(String.format("%d mod%s loaded, %d mod%s active", tModCount, tModCount!=1 ? "s" :"", aModCount, aModCount!=1 ? "s" :"" ));
            brandings = brd.build();
            brandingsNoMC = brandings.subList(1, brandings.size());
        }
    }
    /**
     * Get the brandings for this minecraft instance
     * @param includeMC Whether to include minecraft in the brandings
     * @return The list of brandings
     */
    public List<String> getBrandings(boolean includeMC)
    {
        if (brandings == null)
        {
            computeBranding();
        }
        return includeMC ? ImmutableList.copyOf(brandings) : ImmutableList.copyOf(brandingsNoMC);
    }

    /**
     * Get the sided delegate
     * @return The sided delegate
     */
    public IFMLSidedHandler getSidedDelegate()
    {
        return sidedDelegate;
    }

    /**
     * Called just after a server tick has happened
     */
    public void onPostServerTick()
    {
        bus().post(new TickEvent.ServerTickEvent(Phase.END));
    }

    /**
     * Every tick just after world and other ticks occur
     */
    public void onPostWorldTick(World world)
    {
        bus().post(new TickEvent.WorldTickEvent(Side.SERVER, Phase.END, world));
    }
    
    /**
     * Called just before a server tick has happened
     */
    public void onPreServerTick()
    {
        bus().post(new TickEvent.ServerTickEvent(Phase.START));
    }

    /**
     * Every tick just before world and other ticks occur
     */
    public void onPreWorldTick(World world)
    {
        bus().post(new TickEvent.WorldTickEvent(Side.SERVER, Phase.START, world));
    }

    /**
     * Tell the loader a server is about to start
     * @param server The server that is about to start
     * @return If an exception occured when telling the loader a server was about to start
     */
    public boolean handleServerAboutToStart(MinecraftServer server)
    {
        return Loader.instance().serverAboutToStart(server);
    }

    /**
     * Tell the loader a server is starting
     * @param server The server that is starting
     * @return If an exception occured when telling the loader a server was starting
     */
    public boolean handleServerStarting(MinecraftServer server)
    {
        return Loader.instance().serverStarting(server);
    }

    /**
     * Tell the loader a server has started
     */
    public void handleServerStarted()
    {
        Loader.instance().serverStarted();
    }

    /**
     * Tell the loader a server is stopping
     */
    public void handleServerStopping()
    {
        Loader.instance().serverStopping();
    }

    /**
     * Get the current server
     * @return The current server
     */
    public MinecraftServer getMinecraftServerInstance()
    {
        return sidedDelegate.getServer();
    }

    /**
     * Show a GUI element
     * @param clientGuiElement The GUI element to show
     */
    public void showGuiScreen(Object clientGuiElement)
    {
        sidedDelegate.showGuiScreen(clientGuiElement);
    }

    /**
     * Start a dedicated server
     * @param dedicatedServer The dedicated server that is started
     */
    public void onServerStart(MinecraftServer dedicatedServer)
    {
        FMLServerHandler.instance();
        sidedDelegate.beginServerLoading(dedicatedServer);
    }

    /**
     * Called when a dedicated server has been started
     */
    public void onServerStarted()
    {
        sidedDelegate.finishServerLoading();
    }

    /**
     * Called just before a client tick happens.
     */
    public void onPreClientTick()
    {
        bus().post(new TickEvent.ClientTickEvent(Phase.START));
    }

    /**
     * Called just after a client tick happens 
     */
    public void onPostClientTick()
    {
        bus().post(new TickEvent.ClientTickEvent(Phase.END));
    }

    /**
     * Called when a render tick starts
     * @param timer Time since the last render tick
     */
    public void onRenderTickStart(float timer)
    {
        bus().post(new TickEvent.RenderTickEvent(Phase.START, timer));
    }

    /**
     * Called when a render tick ends
     * @param timer How long this render tick took
     */
    public void onRenderTickEnd(float timer)
    {
        bus().post(new TickEvent.RenderTickEvent(Phase.END, timer));
    }

    /**
     * Called just before a player is ticked
     * @param player The player about to be ticked
     */
    public void onPlayerPreTick(EntityPlayer player)
    {
        bus().post(new TickEvent.PlayerTickEvent(Phase.START, player));
    }

    /**
     * Called just after a player has been ticked
     * @param player The player that has just been ticked
     */
    public void onPlayerPostTick(EntityPlayer player)
    {
        bus().post(new TickEvent.PlayerTickEvent(Phase.END, player));
    }
    
    /** 
     * Register something to be called when we crash
     * @param callable The thing to call when we crash
     */
    public void registerCrashCallable(ICrashCallable callable)
    {
        crashCallables.add(callable);
    }

    /**
     * Enhance a crash report in the given category with the register  crashCallables
     * @param crashReport The crash report that will be issued
     * @param category The category of a crash report
     */
    public void enhanceCrashReport(CrashReport crashReport, CrashReportCategory category)
    {
        for (ICrashCallable call: crashCallables)
        {
            category.func_71500_a(call.getLabel(), call);
        }
    }

    /**
     * Save mod data from a world into a NBTTag
     * @param handler The save handler for this save
     * @param worldInfo The info for the world being saved
     * @param tagCompound The NBTTag for the world save
     */
    public void handleWorldDataSave(SaveHandler handler, WorldInfo worldInfo, NBTTagCompound tagCompound)
    {
        for (ModContainer mc : Loader.instance().getModList())
        {
            if (mc instanceof InjectedModContainer)
            {
                WorldAccessContainer wac = ((InjectedModContainer)mc).getWrappedWorldAccessContainer();
                if (wac != null)
                {
                    NBTTagCompound dataForWriting = wac.getDataForWriting(handler, worldInfo);
                    tagCompound.func_74782_a(mc.getModId(), dataForWriting);
                }
            }
        }
    }

    /**
     * Load mod data from a world into a NBTTag
     * @param handler The save handler for the world
     * @param worldInfo The info of the world being saved
     * @param tagCompound The NBTTag for the world being saved
     */
    public void handleWorldDataLoad(SaveHandler handler, WorldInfo worldInfo, NBTTagCompound tagCompound)
    {
        if (getEffectiveSide()!=Side.SERVER)
        {
            return;
        }
        if (handlerSet.contains(handler))
        {
            return;
        }
        handlerSet.add(handler);
        Map<String,NBTBase> additionalProperties = Maps.newHashMap();
//        worldInfo.setAdditionalProperties(additionalProperties);
        for (ModContainer mc : Loader.instance().getModList())
        {
            if (mc instanceof InjectedModContainer)
            {
                WorldAccessContainer wac = ((InjectedModContainer)mc).getWrappedWorldAccessContainer();
                if (wac != null)
                {
                    wac.readData(handler, worldInfo, additionalProperties, tagCompound.func_74775_l(mc.getModId()));
                }
            }
        }
    }

    /**
     * Should the server die quietly
     * @return Whether the server died quietly
     */
    public boolean shouldServerBeKilledQuietly()
    {
        if (sidedDelegate == null)
        {
            return false;
        }
        return sidedDelegate.shouldServerShouldBeKilledQuietly();
    }

    /**
     * Stop the server and tell everyone that it has stopped
     */
    public void handleServerStopped()
    {
        sidedDelegate.serverStopped();
        MinecraftServer server = getMinecraftServerInstance();
        Loader.instance().serverStopped();
        // FORCE the internal server to stop: hello optifine workaround!
        if (server!=null) ObfuscationReflectionHelper.setPrivateValue(MinecraftServer.class, server, false, "field_71316"+"_v", "u", "serverStopped");
    }

    /**
     * Get the list of builtin mods
     * @return The list of builtin mods
     */
    public String getModName()
    {
        List<String> modNames = Lists.newArrayListWithExpectedSize(3);
        modNames.add("fml");
        if (!noForge)
        {
            modNames.add("forge");
        }

        if (Loader.instance().getFMLBrandingProperties().containsKey("snooperbranding"))
        {
            modNames.add(Loader.instance().getFMLBrandingProperties().get("snooperbranding"));
        }
        return Joiner.on(',').join(modNames);
    }

    /**
     * Add a mod to a resource pack
     * @param container The mod to add
     */
    public void addModToResourcePack(ModContainer container)
    {
        sidedDelegate.addModAsResource(container);
    }

    /**
     * Refresh the resource pack list
     */
    public void updateResourcePackList()
    {
        sidedDelegate.updateResourcePackList();
    }

    /**
     * Get the current language for the game
     * @return
     */
    public String getCurrentLanguage()
    {

        return sidedDelegate.getCurrentLanguage();
    }

    // Why is this here? It seems to serve no purpose
    @Deprecated
    public void bootstrap()
    {
    }

    /**
     * Get the network manager for the client-to-server connection
     * @return The network manager for the client-to-server connection
     */
    public NetworkManager getClientToServerNetworkManager()
    {
        return sidedDelegate.getClientToServerNetworkManager();
    }

    /**
     * Fire a mouse input event over the event bus
     */
    public void fireMouseInput()
    {
        bus().post(new InputEvent.MouseInputEvent());
    }

    /**
     * Fire a key input event over the event bus
     */
    public void fireKeyInput()
    {
        bus().post(new InputEvent.KeyInputEvent());
    }

    /** 
     * Fire a event that the player changed dimensions
     * @param player The player that is changing dimensions
     * @param fromDim The dimension the player was in
     * @param toDim The dimension the player is going to
     */
    public void firePlayerChangedDimensionEvent(EntityPlayer player, int fromDim, int toDim)
    {
        bus().post(new PlayerEvent.PlayerChangedDimensionEvent(player, fromDim, toDim));
    }

    /**
     * Fire an event that a player logged in
     * @param player The player that has logged in
     */
    public void firePlayerLoggedIn(EntityPlayer player)
    {
        bus().post(new PlayerEvent.PlayerLoggedInEvent(player));
    }
    
    /**
     * Fire an event that a player logged out
     * @param player The player that is logging out
     */
    public void firePlayerLoggedOut(EntityPlayer player)
    {
        bus().post(new PlayerEvent.PlayerLoggedOutEvent(player));
    }

    /**
     * Fire an event that a player has respawned
     * @param player The player that is respawning
     */
    public void firePlayerRespawnEvent(EntityPlayer player)
    {
        bus().post(new PlayerEvent.PlayerRespawnEvent(player));
    }

    /**
     * Fire an event that a player has picked up a item that currently exists as a entity
     * @param player The player that is picking up the item
     * @param item The entity for the item that is being picked up
     */
    public void firePlayerItemPickupEvent(EntityPlayer player, EntityItem item)
    {
        bus().post(new PlayerEvent.ItemPickupEvent(player, item));
    }

    /**
     * Fire an event that a player has crafted an item
     * @param player The player that is crafting the item
     * @param crafted the item that has been crafted
     * @param craftMatrix The inventory the item was crafted from
     */
    public void firePlayerCraftingEvent(EntityPlayer player, ItemStack crafted, IInventory craftMatrix)
    {
        bus().post(new PlayerEvent.ItemCraftedEvent(player, crafted, craftMatrix));
    }

    /**
     * Fire an event that a player has smelted an item
     * @param player The player smelting the item
     * @param smelted The item that was the result of smelting
     */
    public void firePlayerSmeltedEvent(EntityPlayer player, ItemStack smelted)
    {
        bus().post(new PlayerEvent.ItemSmeltedEvent(player, smelted));
    }
}
