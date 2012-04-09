/*
 * The FML Forge Mod Loader suite. Copyright (C) 2012 cpw
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

import java.util.ArrayList;
import java.util.List;

public class FMLModContainer implements ModContainer
{
    private Mod modDescriptor;
    private Object modInstance;
    private String source;

    public FMLModContainer(String source)
    {
        this.source = source;
    }

    public FMLModContainer(Class<?> clazz)
    {
        if (clazz == null)
        {
            return;
        }

        modDescriptor = clazz.getAnnotation(Mod.class);

        try
        {
            modInstance = clazz.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean wantsPreInit()
    {
        return modDescriptor.wantsPreInit();
    }

    @Override
    public boolean wantsPostInit()
    {
        return modDescriptor.wantsPostInit();
    }

    @Override
    public void preInit()
    {
    }

    @Override
    public void init()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void postInit()
    {
        // TODO Auto-generated method stub
    }

    public static ModContainer buildFor(Class<?> clazz)
    {
        return new FMLModContainer(clazz);
    }

    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void tickStart()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void tickEnd()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean matches(Object mod)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getSource()
    {
        return source;
    }

    @Override
    public Object getMod()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean generatesWorld()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IWorldGenerator getWorldGenerator()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int lookupFuelValue(int itemId, int itemDamage)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean wantsPickupNotification()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IPickupNotifier getPickupNotifier()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#wantsToDispense()
     */
    @Override
    public boolean wantsToDispense()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#getDispenseHandler()
     */
    @Override
    public IDispenseHandler getDispenseHandler()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#wantsCraftingNotification()
     */
    @Override
    public boolean wantsCraftingNotification()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#getCraftingHandler()
     */
    @Override
    public ICraftingHandler getCraftingHandler()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#getDependencies()
     */
    @Override
    public List<String> getDependencies()
    {
        // TODO Auto-generated method stub
        return new ArrayList<String>(0);
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#getPreDepends()
     */
    @Override
    public List<String> getPreDepends()
    {
        // TODO Auto-generated method stub
        return new ArrayList<String>(0);
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#getPostDepends()
     */
    @Override
    public List<String> getPostDepends()
    {
        // TODO Auto-generated method stub
        return new ArrayList<String>(0);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getSource();
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#wantsNetworkPackets()
     */
    @Override
    public boolean wantsNetworkPackets()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#getNetworkHandler()
     */
    @Override
    public INetworkHandler getNetworkHandler()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#ownsNetworkChannel(java.lang.String)
     */
    @Override
    public boolean ownsNetworkChannel(String channel)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#wantsConsoleCommands()
     */
    @Override
    public boolean wantsConsoleCommands()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#getConsoleHandler()
     */
    @Override
    public IConsoleHandler getConsoleHandler()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#wantsPlayerTracking()
     */
    @Override
    public boolean wantsPlayerTracking()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.ModContainer#getPlayerTracker()
     */
    @Override
    public IPlayerTracker getPlayerTracker()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
