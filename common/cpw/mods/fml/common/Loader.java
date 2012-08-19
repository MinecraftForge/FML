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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import net.minecraft.src.CallableMinecraftVersion;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import cpw.mods.fml.common.LoaderState.ModState;
import cpw.mods.fml.common.discovery.ModDiscoverer;
import cpw.mods.fml.common.event.FMLLoadEvent;
import cpw.mods.fml.common.functions.ModIdFunction;
import cpw.mods.fml.common.modloader.BaseModProxy;
import cpw.mods.fml.common.toposort.ModSorter;
import cpw.mods.fml.common.toposort.ModSortingException;
import cpw.mods.fml.common.toposort.TopologicalSort;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;

/**
 * The loader class performs the actual loading of the mod code from disk.
 *
 * <p>
 * There are several {@link LoaderState}s to mod loading, triggered in two
 * different stages from the FML handler code's hooks into the minecraft code.
 * </p>
 *
 * <ol>
 * <li>LOADING. Scanning the filesystem for mod containers to load (zips, jars,
 * directories), adding them to the {@link #modClassLoader} Scanning, the loaded
 * containers for mod classes to load and registering them appropriately.</li>
 * <li>PREINIT. The mod classes are configured, they are sorted into a load
 * order, and instances of the mods are constructed.</li>
 * <li>INIT. The mod instances are initialized. For BaseMod mods, this involves
 * calling the load method.</li>
 * <li>POSTINIT. The mod instances are post initialized. For BaseMod mods this
 * involves calling the modsLoaded method.</li>
 * <li>UP. The Loader is complete</li>
 * <li>ERRORED. The loader encountered an error during the LOADING phase and
 * dropped to this state instead. It will not complete loading from this state,
 * but it attempts to continue loading before abandoning and giving a fatal
 * error.</li>
 * </ol>
 *
 * Phase 1 code triggers the LOADING and PREINIT states. Phase 2 code triggers
 * the INIT and POSTINIT states.
 *
 * @author cpw
 *
 */
public class Loader
{
    private static final Splitter DEPENDENCYPARTSPLITTER = Splitter.on(":").omitEmptyStrings().trimResults();
    private static final Splitter DEPENDENCYSPLITTER = Splitter.on(";").omitEmptyStrings().trimResults();
    /**
     * The singleton instance
     */
    private static Loader instance;
    /**
     * Build information for tracking purposes.
     */
    private static String major;
    private static String minor;
    private static String rev;
    private static String build;
    private static String mccversion;
    private static String mcsversion;

    /**
     * The class loader we load the mods into.
     */
    private ModClassLoader modClassLoader;
    /**
     * The sorted list of mods.
     */
    private List<ModContainer> mods;
    /**
     * A named list of mods
     */
    private Map<String, ModContainer> namedMods;
    /**
     * The canonical configuration directory
     */
    private File canonicalConfigDir;
    /**
     * The canonical minecraft directory
     */
    private File canonicalMinecraftDir;
    /**
     * The captured error
     */
    private Exception capturedError;
    private File canonicalModsDir;
    private LoadController modController;

    private static File minecraftDir;
    private static List<String> injectedContainers;

    public static Loader instance()
    {
        if (instance == null)
        {
            instance = new Loader();
        }

        return instance;
    }

    public static void injectData(Object... data)
    {
        major = (String) data[0];
        minor = (String) data[1];
        rev = (String) data[2];
        build = (String) data[3];
        mccversion = (String) data[4];
        mcsversion = (String) data[5];
        minecraftDir = (File) data[6];
        injectedContainers = (List<String>)data[7];
    }

    private Loader()
    {
        modClassLoader = new ModClassLoader(getClass().getClassLoader());
        String actualMCVersion = new CallableMinecraftVersion(null).func_71493_a();
        if (!mccversion.equals(actualMCVersion))
        {
            FMLLog.severe("This version of FML is built for Minecraft %s, we have detected Minecraft %s in your minecraft jar file", mccversion, actualMCVersion);
            throw new LoaderException();
        }
    }

    /**
     * Sort the mods into a sorted list, using dependency information from the
     * containers. The sorting is performed using a {@link TopologicalSort}
     * based on the pre- and post- dependency information provided by the mods.
     */
    private void sortModList()
    {
        FMLLog.fine("Verifying mod requirements are satisfied");
        try
        {
            Map<String, ArtifactVersion> modVersions = Maps.newHashMap();
            for (ModContainer mod : mods)
            {
                modVersions.put(mod.getModId(), mod.getProcessedVersion());
            }

            for (ModContainer mod : mods)
            {
                ImmutableList<ArtifactVersion> allDeps = ImmutableList.<ArtifactVersion>builder().addAll(mod.getDependants()).addAll(mod.getDependencies()).build();
                for (ArtifactVersion v : allDeps)
                {
                    if (modVersions.containsKey(v.getLabel()))
                    {
                        if (!v.containsVersion(modVersions.get(v.getLabel())))
                        {
                            FMLLog.log(Level.SEVERE, "The mod %s (%s) requires mods %s to be available, one or more are not", mod.getModId(), mod.getName(), allDeps);
                            throw new LoaderException();
                        }
                    }
                }
            }

            FMLLog.fine("All mod requirements are satisfied");

            ModSorter sorter = new ModSorter(mods, namedMods);

            try
            {
                FMLLog.fine("Sorting mods into an ordered list");
                mods = sorter.sort();
                FMLLog.fine("Mod sorting completed successfully");
            }
            catch (ModSortingException sortException)
            {
                FMLLog.severe("A dependency cycle was detected in the input mod set so an ordering cannot be determined");
                FMLLog.severe("The visited mod list is %s", sortException.getExceptionData().getVisitedNodes());
                FMLLog.severe("The first mod in the cycle is %s", sortException.getExceptionData().getFirstBadNode());
                FMLLog.log(Level.SEVERE, sortException, "The full error");
                throw new LoaderException(sortException);
            }
        }
        finally
        {
            FMLLog.fine("Mod sorting data:");
            for (ModContainer mod : mods)
            {
                if (!mod.isImmutable())
                {
                    FMLLog.fine("\t%s(%s): %s (%s)", mod.getModId(), mod.getName(), mod.getSource().getName(), mod.getSortingRules());
                }
            }
            if (mods.size()==0)
            {
                FMLLog.fine("No mods found to sort");
            }
        }

    }

    /**
     * The primary loading code
     *
     * This is visited during first initialization by Minecraft to scan and load
     * the mods from all sources 1. The minecraft jar itself (for loading of in
     * jar mods- I would like to remove this if possible but forge depends on it
     * at present) 2. The mods directory with expanded subdirs, searching for
     * mods named mod_*.class 3. The mods directory for zip and jar files,
     * searching for mod classes named mod_*.class again
     *
     * The found resources are first loaded into the {@link #modClassLoader}
     * (always) then scanned for class resources matching the specification
     * above.
     *
     * If they provide the {@link Mod} annotation, they will be loaded as
     * "FML mods", which currently is effectively a NO-OP. If they are
     * determined to be {@link BaseModProxy} subclasses they are loaded as such.
     *
     * Finally, if they are successfully loaded as classes, they are then added
     * to the available mod list.
     */
    private ModDiscoverer identifyMods()
    {
        FMLLog.fine("Building injected Mod Containers %s", injectedContainers);
        File coremod = new File(minecraftDir,"coremods");
        for (String cont : injectedContainers)
        {
            ModContainer mc;
            try
            {
                mc = (ModContainer) Class.forName(cont,true,modClassLoader).newInstance();
            }
            catch (Exception e)
            {
                FMLLog.log(Level.SEVERE, e, "A problem occured instantiating the injected mod container %s", cont);
                throw new LoaderException(e);
            }
            mods.add(new InjectedModContainer(mc,coremod));
        }
        ModDiscoverer discoverer = new ModDiscoverer();
        FMLLog.fine("Attempting to load mods contained in the minecraft jar file and associated classes");
        discoverer.findClasspathMods(modClassLoader);
        FMLLog.fine("Minecraft jar mods loaded successfully");

        FMLLog.info("Searching %s for mods", canonicalModsDir.getAbsolutePath());
        discoverer.findModDirMods(canonicalModsDir);

        mods.addAll(discoverer.identifyMods());
        identifyDuplicates(mods);
        namedMods = Maps.uniqueIndex(mods, new ModIdFunction());
        FMLLog.info("Forge Mod Loader has identified %d mod%s to load", mods.size(), mods.size() != 1 ? "s" : "");
        return discoverer;
    }

    private class ModIdComparator implements Comparator<ModContainer>
    {
        @Override
        public int compare(ModContainer o1, ModContainer o2)
        {
            return o1.getModId().compareTo(o2.getModId());
        }

    }

    private void identifyDuplicates(List<ModContainer> mods)
    {
        boolean foundDupe = false;
        TreeMultimap<ModContainer, File> dupsearch = TreeMultimap.create(new ModIdComparator(), Ordering.arbitrary());
        for (ModContainer mc : mods)
        {
            if (mc.getSource() != null)
            {
                dupsearch.put(mc, mc.getSource());
            }
        }

        ImmutableMultiset<ModContainer> duplist = Multisets.copyHighestCountFirst(dupsearch.keys());
        for (Entry<ModContainer> e : duplist.entrySet())
        {
            if (e.getCount() > 1)
            {
                FMLLog.severe("Found a duplicate mod %s at %s", e.getElement().getModId(), dupsearch.get(e.getElement()));
                foundDupe = true;
            }
        }
        if (foundDupe)
        {
            throw new LoaderException();
        }
    }

    /**
     * @return
     */
    private void initializeLoader()
    {
        File modsDir = new File(minecraftDir, "mods");
        File configDir = new File(minecraftDir, "config");
        String canonicalModsPath;
        String canonicalConfigPath;

        try
        {
            canonicalMinecraftDir = minecraftDir.getCanonicalFile();
            canonicalModsPath = modsDir.getCanonicalPath();
            canonicalConfigPath = configDir.getCanonicalPath();
            canonicalConfigDir = configDir.getCanonicalFile();
            canonicalModsDir = modsDir.getCanonicalFile();
        }
        catch (IOException ioe)
        {
            FMLLog.log(Level.SEVERE, ioe, "Failed to resolve loader directories: mods : %s ; config %s", canonicalModsDir.getAbsolutePath(),
                       configDir.getAbsolutePath());
            throw new LoaderException(ioe);
        }

        if (!canonicalModsDir.exists())
        {
            FMLLog.info("No mod directory found, creating one: %s", canonicalModsPath);
            boolean dirMade = canonicalModsDir.mkdir();
            if (!dirMade)
            {
                FMLLog.severe("Unable to create the mod directory %s", canonicalModsPath);
                throw new LoaderException();
            }
            FMLLog.info("Mod directory created successfully");
        }

        if (!canonicalConfigDir.exists())
        {
            FMLLog.fine("No config directory found, creating one: %s", canonicalConfigPath);
            boolean dirMade = canonicalConfigDir.mkdir();
            if (!dirMade)
            {
                FMLLog.severe("Unable to create the config directory %s", canonicalConfigPath);
                throw new LoaderException();
            }
            FMLLog.info("Config directory created successfully");
        }

        if (!canonicalModsDir.isDirectory())
        {
            FMLLog.severe("Attempting to load mods from %s, which is not a directory", canonicalModsPath);
            throw new LoaderException();
        }

        if (!configDir.isDirectory())
        {
            FMLLog.severe("Attempting to load configuration from %s, which is not a directory", canonicalConfigPath);
            throw new LoaderException();
        }
    }

    public List<ModContainer> getModList()
    {
        return ImmutableList.copyOf(instance().mods);
    }

    /**
     * Called from the hook to start mod loading. We trigger the
     * {@link #identifyMods()} and {@link #preModInit()} phases here. Finally,
     * the mod list is frozen completely and is consider immutable from then on.
     */
    public void loadMods()
    {
        initializeLoader();
        mods = Lists.newArrayList();
        namedMods = Maps.newHashMap();
        modController = new LoadController(this);
        modController.transition(LoaderState.LOADING);
        ModDiscoverer disc = identifyMods();
        disableRequestedMods();
        sortModList();
        mods = ImmutableList.copyOf(mods);
        modController.distributeStateMessage(FMLLoadEvent.class);
        modController.transition(LoaderState.CONSTRUCTING);
        modController.distributeStateMessage(LoaderState.CONSTRUCTING, modClassLoader, disc.getASMTable());
        modController.transition(LoaderState.PREINITIALIZATION);
        modController.distributeStateMessage(LoaderState.PREINITIALIZATION, disc.getASMTable(), canonicalConfigDir);
        modController.transition(LoaderState.INITIALIZATION);
    }

    private void disableRequestedMods()
    {
        String disabledModList = System.getProperty("fml.modStates", "");
        FMLLog.fine("Received a system property request \'%s\'",disabledModList);
        Map<String, String> sysPropertyStateList = Splitter.on(CharMatcher.anyOf(";:"))
                .omitEmptyStrings().trimResults().withKeyValueSeparator("=")
                .split(disabledModList);
        FMLLog.fine("System property request managing the state of %d mods", sysPropertyStateList.size());
        Map<String, String> modStates = Maps.newHashMap();

        File disabledModFile = new File(canonicalConfigDir, "fmlModState.properties");
        Properties disabledModListProperties = new Properties();
        if (disabledModFile.exists() && disabledModFile.isFile())
        {
            FMLLog.fine("Found a mod state file %s", disabledModFile.getName());
            try
            {
                disabledModListProperties.load(new FileReader(disabledModFile));
                FMLLog.fine("Loaded states for %d mods from file", disabledModListProperties.size());
            }
            catch (Exception e)
            {
                FMLLog.log(Level.INFO, e, "An error occurred reading the fmlModState.properties file");
            }
        }
        modStates.putAll(Maps.fromProperties(disabledModListProperties));
        modStates.putAll(sysPropertyStateList);
        FMLLog.fine("After merging, found state information for %d mods", modStates.size());

        Map<String, Boolean> isEnabled = Maps.transformValues(modStates, new Function<String, Boolean>()
        {
            public Boolean apply(String input)
            {
                return !Boolean.parseBoolean(input);
            }
        });

        for (Map.Entry<String, Boolean> entry : isEnabled.entrySet())
        {
            if (namedMods.containsKey(entry.getKey()))
            {
                FMLLog.info("Setting mod %s to enabled state %b", entry.getKey(), entry.getValue());
                namedMods.get(entry.getKey()).setEnabledState(entry.getValue());
            }
        }
    }

    /**
     * Query if we know of a mod named modname
     *
     * @param modname
     * @return
     */
    public static boolean isModLoaded(String modname)
    {
        return instance().namedMods.containsKey(modname);
    }

    /**
     * @return
     */
    public File getConfigDir()
    {
        return canonicalConfigDir;
    }

    public String getCrashInformation()
    {
        StringBuilder ret = new StringBuilder();
        List<String> branding = FMLCommonHandler.instance().getBrandings();

        Joiner.on(' ').skipNulls().appendTo(ret, branding.subList(1, branding.size()));
        if (modController!=null)
        {
            modController.printModStates(ret);
        }
        return ret.toString();
    }

    /**
     * @return
     */
    public String getFMLVersionString()
    {
        return String.format("FML v%s.%s.%s.%s", major, minor, rev, build);
    }

    /**
     * @return
     */
    public ClassLoader getModClassLoader()
    {
        return modClassLoader;
    }

    public void computeDependencies(String dependencyString, List<ArtifactVersion> requirements, List<ArtifactVersion> dependencies, List<ArtifactVersion> dependants)
    {
        if (dependencyString == null || dependencyString.length() == 0)
        {
            return;
        }

        boolean parseFailure=false;

        for (String dep : DEPENDENCYSPLITTER.split(dependencyString))
        {
            List<String> depparts = Lists.newArrayList(DEPENDENCYPARTSPLITTER.split(dep));
            // Need two parts to the string
            if (depparts.size() != 2)
            {
                parseFailure=true;
                continue;
            }
            String instruction = depparts.get(0);
            String target = depparts.get(1);
            boolean targetIsAll = target.startsWith("*");

            // Cannot have an "all" relationship with anything except pure *
            if (targetIsAll && target.length()>1)
            {
                parseFailure = true;
                continue;
            }

            // If this is a required element, add it to the required list
            if ("required-before".equals(instruction) || "required-after".equals(instruction))
            {
                // You can't require everything
                if (!targetIsAll)
                {
                    requirements.add(VersionParser.parseVersionReference(target));
                }
                else
                {
                    parseFailure=true;
                    continue;
                }
            }

            // You cannot have a versioned dependency on everything
            if (targetIsAll && target.indexOf('@')>-1)
            {
                parseFailure = true;
                continue;
            }
            // before elements are things we are loaded before (so they are our dependants)
            if ("required-before".equals(instruction) || "before".equals(instruction))
            {
                dependants.add(VersionParser.parseVersionReference(target));
            }
            // after elements are things that load before we do (so they are out dependencies)
            else if ("required-after".equals(instruction) || "after".equals(instruction))
            {
                dependencies.add(VersionParser.parseVersionReference(target));
            }
            else
            {
                parseFailure=true;
            }
        }

        if (parseFailure)
        {
            FMLLog.log(Level.WARNING, "Unable to parse dependency string %s", dependencyString);
            throw new LoaderException();
        }
    }

    public Map<String,ModContainer> getIndexedModList()
    {
        return ImmutableMap.copyOf(namedMods);
    }

    public void initializeMods()
    {
        // Mod controller should be in the initialization state here
        modController.distributeStateMessage(LoaderState.INITIALIZATION);
        modController.transition(LoaderState.POSTINITIALIZATION);
        modController.distributeStateMessage(LoaderState.POSTINITIALIZATION);
        modController.transition(LoaderState.AVAILABLE);
        modController.distributeStateMessage(LoaderState.AVAILABLE);
        FMLLog.info("Forge Mod Loader has successfully loaded %d mod%s", mods.size(), mods.size()==1 ? "" : "s");
    }

    public Callable getCallableCrashInformation()
    {
        return new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                return getCrashInformation();
            }
        };
    }

    public List<ModContainer> getActiveModList()
    {
        return modController.getActiveModList();
    }

    public ModState getModState(ModContainer selectedMod)
    {
        return modController.getModState(selectedMod);
    }

    public String getMCVersionString()
    {
        return "Minecraft " + mccversion;
    }

    public void serverStarting(Object server)
    {
        modController.distributeStateMessage(LoaderState.SERVER_STARTING, server);
        modController.transition(LoaderState.SERVER_STARTING);
    }

    public void serverStarted()
    {
        modController.distributeStateMessage(LoaderState.SERVER_STARTED);
        modController.transition(LoaderState.SERVER_STARTED);
    }

    public void serverStopping()
    {
        modController.distributeStateMessage(LoaderState.SERVER_STOPPING);
        modController.transition(LoaderState.SERVER_STOPPING);
        modController.transition(LoaderState.AVAILABLE);

    }

    public BiMap<ModContainer, Object> getModObjectList()
    {
        return modController.getModObjectList();
    }

    public BiMap<Object, ModContainer> getReversedModObjectList()
    {
        return getModObjectList().inverse();
    }

    public ModContainer activeModContainer()
    {
        return modController.activeContainer();
    }

    public boolean isInState(LoaderState state)
    {
        return modController.isInState(state);
    }
}
