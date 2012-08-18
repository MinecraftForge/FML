/*
 * The FML Forge Mod Loader suite.
 * Copyright (C) 2012 cpw
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package cpw.mods.fml.common;

import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.DedicatedServer;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.World;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.network.EntitySpawnAdjustmentPacket;
import cpw.mods.fml.common.network.EntitySpawnPacket;
import cpw.mods.fml.common.registry.TickRegistry;
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

    private List<IScheduledTickHandler> scheduledClientTicks = Lists.newArrayList();
    private List<IScheduledTickHandler> scheduledServerTicks = Lists.newArrayList();

    public void beginLoading(IFMLSidedHandler handler)
    {
        sidedDelegate = handler;
        FMLLog.info("Attempting early MinecraftForge initialization");
        callForgeMethod("initialize");
        FMLLog.info("Completed early MinecraftForge initialization");
    }

    public void rescheduleTicks(Side side)
    {
        TickRegistry.updateTickQueue(side.isClient() ? scheduledClientTicks : scheduledServerTicks, side);
    }
    public void tickStart(EnumSet<TickType> ticks, Side side, Object ... data)
    {
        List<IScheduledTickHandler> scheduledTicks = side.isClient() ? scheduledClientTicks : scheduledServerTicks;

        if (scheduledTicks.size()==0)
        {
            return;
        }
        for (IScheduledTickHandler ticker : scheduledTicks)
        {
            EnumSet<TickType> ticksToRun = EnumSet.copyOf(ticker.ticks());
            ticksToRun.removeAll(EnumSet.complementOf(ticks));
            if (!ticksToRun.isEmpty())
            {
                ticker.tickStart(ticksToRun, data);
            }
        }
    }

    public void tickEnd(EnumSet<TickType> ticks, Side side, Object ... data)
    {
        List<IScheduledTickHandler> scheduledTicks = side.isClient() ? scheduledClientTicks : scheduledServerTicks;

        if (scheduledTicks.size()==0)
        {
            return;
        }
        for (IScheduledTickHandler ticker : scheduledTicks)
        {
            EnumSet<TickType> ticksToRun = EnumSet.copyOf(ticker.ticks());
            ticksToRun.removeAll(EnumSet.complementOf(ticks));
            if (!ticksToRun.isEmpty())
            {
                ticker.tickEnd(ticksToRun, data);
            }
        }
    }

    /**
     * @return the instance
     */
    public static FMLCommonHandler instance()
    {
        return INSTANCE;
    }
    /**
     * Find the container that associates with the supplied mod object
     * @param mod
     * @return
     */
    public ModContainer findContainerFor(Object mod)
    {
        return Loader.instance().getReversedModObjectList().get(mod);
    }
    /**
     * Get the forge mod loader logging instance (goes to the forgemodloader log file)
     * @return
     */
    public Logger getFMLLogger()
    {
        return FMLLog.getLogger();
    }

    /**
     * @param key
     * @param lang
     * @param value
     */
    /**
     * @param languagePack
     * @param lang
     */

    public Side getSide()
    {
        return sidedDelegate.getSide();
    }

    /**
     * Raise an exception
     *
     * @param exception
     * @param message
     * @param stopGame
     */
    public void raiseException(Throwable exception, String message, boolean stopGame)
    {
        FMLCommonHandler.instance().getFMLLogger().throwing("FMLHandler", "raiseException", exception);
        if (stopGame)
        {
            getSidedDelegate().haltGame(message,exception);
        }
    }


    private Class<?> forge;
    private boolean noForge;
    private List<String> brandings;
    private Class<?> findMinecraftForge()
    {
        if (forge==null && !noForge)
        {
            try
            {
                forge = Class.forName("net.minecraftforge.common.MinecraftForge");
            }
            catch (Exception ex)
            {
                noForge = true;
            }
        }
        return forge;
    }

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
     * @param string
     * @return
     */
    public void computeBranding()
    {
        if (brandings == null)
        {
            Builder brd = ImmutableList.<String>builder();
            brd.add(Loader.instance().getMCVersionString());
            brd.add(Loader.instance().getFMLVersionString());
            String forgeBranding = (String) callForgeMethod("getBrandingVersion");
            if (!Strings.isNullOrEmpty(forgeBranding))
            {
                brd.add(forgeBranding);
            }
            brd.addAll(sidedDelegate.getAdditionalBrandingInformation());
            try
            {
                Properties props=new Properties();
                props.load(getClass().getClassLoader().getResourceAsStream("fmlbranding.properties"));
                brd.add(props.getProperty("fmlbranding"));
            }
            catch (Exception ex)
            {
                // Ignore - no branding file found
            }
            int tModCount = Loader.instance().getModList().size();
            int aModCount = Loader.instance().getActiveModList().size();
            brd.add(String.format("%d mod%s loaded, %d mod%s active", tModCount, tModCount!=1 ? "s" :"", aModCount, aModCount!=1 ? "s" :"" ));
            brandings = brd.build();
        }
    }
    public List<String> getBrandings()
    {
        if (brandings == null)
        {
            computeBranding();
        }
        return ImmutableList.copyOf(brandings);
    }

    /**
     * @return
     */
    public IFMLSidedHandler getSidedDelegate()
    {
        return sidedDelegate;
    }

    public void onPostServerTick()
    {
        tickEnd(EnumSet.of(TickType.SERVER), Side.SERVER);
    }

    /**
     * Every tick just after world and other ticks occur
     */
    public void onPostWorldTick(Object world)
    {
        tickEnd(EnumSet.of(TickType.WORLD), Side.SERVER, world);
    }

    public void onPreServerTick()
    {
        tickStart(EnumSet.of(TickType.SERVER), Side.SERVER);
    }

    /**
     * Every tick just before world and other ticks occur
     */
    public void onPreWorldTick(Object world)
    {
        tickStart(EnumSet.of(TickType.WORLD), Side.SERVER, world);
    }

    public void onWorldLoadTick(World[] worlds)
    {
        rescheduleTicks(Side.SERVER);
        for (World w : worlds)
        {
            tickStart(EnumSet.of(TickType.WORLDLOAD), Side.SERVER, w);
        }
    }

    public void handleServerStarting(MinecraftServer server)
    {
        Loader.instance().serverStarting(server);
    }

    public void handleServerStarted()
    {
        Loader.instance().serverStarted();
    }

    public void handleServerStopping()
    {
        Loader.instance().serverStopping();
    }

    public MinecraftServer getMinecraftServerInstance()
    {
        return sidedDelegate.getServer();
    }

    public void showGuiScreen(Object clientGuiElement)
    {
        sidedDelegate.showGuiScreen(clientGuiElement);
    }

    public Entity spawnEntityIntoClientWorld(Class<? extends Entity> cls, EntitySpawnPacket entitySpawnPacket)
    {
        return sidedDelegate.spawnEntityIntoClientWorld(cls, entitySpawnPacket);
    }

    public void adjustEntityLocationOnClient(EntitySpawnAdjustmentPacket entitySpawnAdjustmentPacket)
    {
        sidedDelegate.adjustEntityLocationOnClient(entitySpawnAdjustmentPacket);
    }

    public void onServerStart(DedicatedServer dedicatedServer)
    {
        FMLServerHandler.instance();
        sidedDelegate.beginServerLoading(dedicatedServer);
    }

    public void onServerStarted()
    {
        sidedDelegate.finishServerLoading();
    }


    public void onPreClientTick()
    {
        tickStart(EnumSet.of(TickType.CLIENT), Side.CLIENT);

    }

    public void onPostClientTick()
    {
        tickEnd(EnumSet.of(TickType.CLIENT), Side.CLIENT);
    }

    public void onRenderTickStart(float timer)
    {
        tickStart(EnumSet.of(TickType.RENDER), Side.CLIENT, timer);
    }

    public void onRenderTickEnd(float timer)
    {
        tickEnd(EnumSet.of(TickType.RENDER), Side.CLIENT, timer);
    }

    public void onPlayerPreTick(EntityPlayer player)
    {
        Side side = player instanceof EntityPlayerMP ? Side.SERVER : Side.CLIENT;
        tickStart(EnumSet.of(TickType.PLAYER), side, player);
    }

    public void onPlayerPostTick(EntityPlayer player)
    {
        Side side = player instanceof EntityPlayerMP ? Side.SERVER : Side.CLIENT;
        tickEnd(EnumSet.of(TickType.PLAYER), side, player);
    }
}
