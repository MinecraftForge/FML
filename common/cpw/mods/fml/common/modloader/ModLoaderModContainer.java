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
package cpw.mods.fml.common.modloader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderException;
import cpw.mods.fml.common.MetadataCollection;
import cpw.mods.fml.common.ModClassLoader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.ProxyInjector;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ASMDataTable.ASMData;
import cpw.mods.fml.common.discovery.ContainerType;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.FMLNetworkHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;

public class ModLoaderModContainer implements ModContainer
{
    public BaseModProxy mod;
    private File modSource;
    public List<ArtifactVersion> requirements = Lists.newArrayList();
    public ArrayList<ArtifactVersion> dependencies = Lists.newArrayList();
    public ArrayList<ArtifactVersion> dependants = Lists.newArrayList();
    private ContainerType sourceType;
    private ModMetadata metadata;
    private ProxyInjector sidedProxy;
    private BaseModTicker gameTickHandler;
    private BaseModTicker guiTickHandler;
    private String modClazzName;
    private String modId;
    private EventBus bus;
    private LoadController controller;
    private boolean enabled = true;
    private String sortingProperties;
    private ArtifactVersion processedVersion;
    private boolean isNetworkMod;

    public ModLoaderModContainer(String className, File modSource, String sortingProperties)
    {
        this.modClazzName = className;
        this.modSource = modSource;
        this.modId = className.contains(".") ? className.substring(className.lastIndexOf('.')+1) : className;
        this.sortingProperties = Strings.isNullOrEmpty(sortingProperties) ? "" : sortingProperties;
    }

    /**
     * We only instantiate this for "not mod mods"
     * @param instance
     */
    ModLoaderModContainer(BaseModProxy instance)
    {
        this.mod=instance;
        this.gameTickHandler = new BaseModTicker(instance, false);
        this.guiTickHandler = new BaseModTicker(instance, true);
    }

    /**
     *
     */
    private void configureMod(Class<? extends BaseModProxy> modClazz, ASMDataTable asmData)
    {
        File configDir = Loader.instance().getConfigDir();
        File modConfig = new File(configDir, String.format("%s.cfg", modClazzName));
        Properties props = new Properties();

        boolean existingConfigFound = false;
        boolean mlPropFound = false;

        if (modConfig.exists())
        {
            try
            {
                FMLLog.fine("Reading existing configuration file for %s : %s", modClazzName, modConfig.getName());
                FileReader configReader = new FileReader(modConfig);
                props.load(configReader);
                configReader.close();
            }
            catch (Exception e)
            {
                FMLLog.log(Level.SEVERE, e, "Error occured reading mod configuration file %s", modConfig.getName());
                throw new LoaderException(e);
            }
            existingConfigFound = true;
        }

        StringBuffer comments = new StringBuffer();
        comments.append("MLProperties: name (type:default) min:max -- information\n");


        List<ModProperty> mlPropFields = Lists.newArrayList();
        try
        {
            for (ASMData dat : Sets.union(asmData.getAnnotationsFor(this).get("net.minecraft.src.MLProp"), asmData.getAnnotationsFor(this).get("MLProp")))
            {
                if (dat.getClassName().equals(modClazzName))
                {
                    try
                    {
                        mlPropFields.add(new ModProperty(modClazz.getDeclaredField(dat.getObjectName()), dat.getAnnotationInfo()));
                    }
                    catch (Exception e)
                    {
                        FMLLog.log(Level.WARNING, e, "An error occured trying to access field %s in mod %s", dat.getObjectName(), getModId());
                    }
                }
            }
            for (ModProperty property : mlPropFields)
            {
                if (!Modifier.isStatic(property.field().getModifiers()))
                {
                    FMLLog.info("The MLProp field %s in mod %s appears not to be static", property.field().getName(), getModId());
                    continue;
                }
                Field f = property.field();
                String propertyName = property.name().length() > 0 ? property.name() : f.getName();
                String propertyValue = null;
                Object defaultValue = null;

                try
                {
                    defaultValue = f.get(null);
                    propertyValue = props.getProperty(propertyName, extractValue(defaultValue));
                    Object currentValue = parseValue(propertyValue, property, f.getType(), propertyName, modClazzName);
                    FMLLog.finest("Configuration for %s.%s found values default: %s, configured: %s, interpreted: %s", modClazzName, propertyName, defaultValue, propertyValue, currentValue);

                    if (currentValue != null && !currentValue.equals(defaultValue))
                    {
                        FMLLog.finest("Configuration for %s.%s value set to: %s", modClazzName, propertyName, currentValue);
                        f.set(null, currentValue);
                    }
                }
                catch (Exception e)
                {
                    FMLLog.log(Level.SEVERE, e, "Invalid configuration found for %s in %s", propertyName, modConfig.getName());
                    throw new LoaderException(e);
                }
                finally
                {
                    comments.append(String.format("MLProp : %s (%s:%s", propertyName, f.getType().getName(), defaultValue));

                    if (property.min() != Double.MIN_VALUE)
                    {
                        comments.append(",>=").append(String.format("%.1f", property.min()));
                    }

                    if (property.max() != Double.MAX_VALUE)
                    {
                        comments.append(",<=").append(String.format("%.1f", property.max()));
                    }

                    comments.append(")");

                    if (property.info().length() > 0)
                    {
                        comments.append(" -- ").append(property.info());
                    }

                    if (propertyValue != null)
                    {
                        props.setProperty(propertyName, extractValue(propertyValue));
                    }
                    comments.append("\n");
                }
                mlPropFound = true;
            }
        }
        finally
        {
            if (!mlPropFound && !existingConfigFound)
            {
                FMLLog.fine("No MLProp configuration for %s found or required. No file written", modClazzName);
                return;
            }

            if (!mlPropFound && existingConfigFound)
            {
                File mlPropBackup = new File(modConfig.getParent(),modConfig.getName()+".bak");
                FMLLog.fine("MLProp configuration file for %s found but not required. Attempting to rename file to %s", modClazzName, mlPropBackup.getName());
                boolean renamed = modConfig.renameTo(mlPropBackup);
                if (renamed)
                {
                    FMLLog.fine("Unused MLProp configuration file for %s renamed successfully to %s", modClazzName, mlPropBackup.getName());
                }
                else
                {
                    FMLLog.fine("Unused MLProp configuration file for %s renamed UNSUCCESSFULLY to %s", modClazzName, mlPropBackup.getName());
                }

                return;
            }
            try
            {
                FileWriter configWriter = new FileWriter(modConfig);
                props.store(configWriter, comments.toString());
                configWriter.close();
                FMLLog.fine("Configuration for %s written to %s", modClazzName, modConfig.getName());
            }
            catch (IOException e)
            {
                FMLLog.log(Level.SEVERE, e, "Error trying to write the config file %s", modConfig.getName());
                throw new LoaderException(e);
            }
        }
    }

    private Object parseValue(String val, ModProperty property, Class<?> type, String propertyName, String modConfigName)
    {
        if (type.isAssignableFrom(String.class))
        {
            return (String)val;
        }
        else if (type.isAssignableFrom(Boolean.TYPE) || type.isAssignableFrom(Boolean.class))
        {
            return Boolean.parseBoolean(val);
        }
        else if (Number.class.isAssignableFrom(type) || type.isPrimitive())
        {
            Number n = null;

            if (type.isAssignableFrom(Double.TYPE) || Double.class.isAssignableFrom(type))
            {
                n = Double.parseDouble(val);
            }
            else if (type.isAssignableFrom(Float.TYPE) || Float.class.isAssignableFrom(type))
            {
                n = Float.parseFloat(val);
            }
            else if (type.isAssignableFrom(Long.TYPE) || Long.class.isAssignableFrom(type))
            {
                n = Long.parseLong(val);
            }
            else if (type.isAssignableFrom(Integer.TYPE) || Integer.class.isAssignableFrom(type))
            {
                n = Integer.parseInt(val);
            }
            else if (type.isAssignableFrom(Short.TYPE) || Short.class.isAssignableFrom(type))
            {
                n = Short.parseShort(val);
            }
            else if (type.isAssignableFrom(Byte.TYPE) || Byte.class.isAssignableFrom(type))
            {
                n = Byte.parseByte(val);
            }
            else
            {
                throw new IllegalArgumentException(String.format("MLProp declared on %s of type %s, an unsupported type",propertyName, type.getName()));
            }

            if (n.doubleValue() < property.min() || n.doubleValue() > property.max())
            {
                FMLLog.warning("Configuration for %s.%s found value %s outside acceptable range %s,%s", modConfigName,propertyName, n, property.min(), property.max());
                return null;
            }
            else
            {
                return n;
            }
        }

        throw new IllegalArgumentException(String.format("MLProp declared on %s of type %s, an unsupported type",propertyName, type.getName()));
    }
    private String extractValue(Object value)
    {
        if (String.class.isInstance(value))
        {
            return (String)value;
        }
        else if (Number.class.isInstance(value) || Boolean.class.isInstance(value))
        {
            return String.valueOf(value);
        }
        else
        {
            throw new IllegalArgumentException("MLProp declared on non-standard type");
        }
    }

    @Override
    public String getName()
    {
        return mod != null ? mod.getName() : modId;
    }

    @Deprecated
    public static ModContainer findContainerFor(BaseModProxy mod)
    {
        return FMLCommonHandler.instance().findContainerFor(mod);
    }

    @Override
    public String getSortingRules()
    {
        return sortingProperties;
    }

    @Override
    public boolean matches(Object mod)
    {
        return this.mod == mod;
    }

    /**
     * Find all the BaseMods in the system
     * @param <A>
     * @return
     */
    public static <A extends BaseModProxy> List<A> findAll(Class<A> clazz)
    {
        ArrayList<A> modList = new ArrayList<A>();

        for (ModContainer mc : Loader.instance().getActiveModList())
        {
            if (mc instanceof ModLoaderModContainer && mc.getMod()!=null)
            {
                modList.add((A)((ModLoaderModContainer)mc).mod);
            }
        }

        return modList;
    }

    @Override
    public File getSource()
    {
        return modSource;
    }

    @Override
    public Object getMod()
    {
        return mod;
    }

    @Override
    public List<ArtifactVersion> getRequirements()
    {
        return requirements;
    }

    @Override
    public List<ArtifactVersion> getDependants()
    {
        return dependants;
    }

    @Override
    public List<ArtifactVersion> getDependencies()
    {
        return dependencies;
    }


    public String toString()
    {
        return modId;
    }

    @Override
    public ModMetadata getMetadata()
    {
        return metadata;
    }

    @Override
    public String getVersion()
    {
        if (mod == null || mod.getVersion() == null)
        {
            return "Not available";
        }
        return mod.getVersion();
    }

    /**
     * @return
     */
    public BaseModTicker getGameTickHandler()
    {
        return this.gameTickHandler;
    }
    /**
     * @return
     */
    public BaseModTicker getGUITickHandler()
    {
        return this.guiTickHandler;
    }

    @Override
    public String getModId()
    {
        return modId;
    }

    @Override
    public void bindMetadata(MetadataCollection mc)
    {
        Map<String, Object> dummyMetadata = ImmutableMap.<String,Object>builder().put("name", modId).put("version", "").build();
        this.metadata = mc.getMetadataForId(modId, dummyMetadata);
        Loader.instance().computeDependencies(sortingProperties, getRequirements(), getDependencies(), getDependants());
    }

    @Override
    public void setEnabledState(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        if (this.enabled)
        {
            FMLLog.fine("Enabling mod %s", getModId());
            this.bus = bus;
            this.controller = controller;
            bus.register(this);
            return true;
        }
        else
        {
            return false;
        }
    }

    // Lifecycle mod events

    @Subscribe
    public void constructMod(FMLConstructionEvent event)
    {
        try
        {
            ModClassLoader modClassLoader = event.getModClassLoader();
            modClassLoader.addFile(modSource);
            EnumSet<TickType> ticks = EnumSet.noneOf(TickType.class);
            this.gameTickHandler = new BaseModTicker(ticks, false);
            this.guiTickHandler = new BaseModTicker(ticks.clone(), true);
            Class<? extends BaseModProxy> modClazz = (Class<? extends BaseModProxy>) Class.forName(modClazzName, true, modClassLoader);
            configureMod(modClazz, event.getASMHarvestedData());
            isNetworkMod = FMLNetworkHandler.instance().registerNetworkMod(this, modClazz, event.getASMHarvestedData());
            mod = (BaseModProxy)modClazz.newInstance();
            ProxyInjector.inject(this, event.getASMHarvestedData(), FMLCommonHandler.instance().getSide());
        }
        catch (Exception e)
        {
            controller.errorOccurred(this, e);
            Throwables.propagateIfPossible(e);
        }
    }

    @Subscribe
    public void preInit(FMLPreInitializationEvent event)
    {
        try
        {
            this.gameTickHandler.setMod(mod);
            this.guiTickHandler.setMod(mod);
            TickRegistry.registerTickHandler(this.gameTickHandler, Side.CLIENT);
            TickRegistry.registerTickHandler(this.guiTickHandler, Side.CLIENT);
            GameRegistry.registerWorldGenerator(ModLoaderHelper.buildWorldGenHelper(mod));
            GameRegistry.registerFuelHandler(ModLoaderHelper.buildFuelHelper(mod));
            GameRegistry.registerCraftingHandler(ModLoaderHelper.buildCraftingHelper(mod));
            GameRegistry.registerPickupHandler(ModLoaderHelper.buildPickupHelper(mod));
            NetworkRegistry.instance().registerConnectionHandler(ModLoaderHelper.buildConnectionHelper(mod));
        }
        catch (Exception e)
        {
            controller.errorOccurred(this, e);
            Throwables.propagateIfPossible(e);
        }
    }


    @Subscribe
    public void init(FMLInitializationEvent event)
    {
        try
        {
            mod.load();
        }
        catch (Throwable t)
        {
            controller.errorOccurred(this, t);
            Throwables.propagateIfPossible(t);
        }
    }

    @Subscribe
    public void postInit(FMLPostInitializationEvent event)
    {
        try
        {
            mod.modsLoaded();
        }
        catch (Throwable t)
        {
            controller.errorOccurred(this, t);
            Throwables.propagateIfPossible(t);
        }
    }

    @Override
    public ArtifactVersion getProcessedVersion()
    {
        if (processedVersion == null)
        {
            processedVersion = new DefaultArtifactVersion(modId, getVersion());
        }
        return processedVersion;
    }

    @Override
    public boolean isImmutable()
    {
        return false;
    }

    @Override
    public boolean isNetworkMod()
    {
        return this.isNetworkMod;
    }
}
