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

package net.minecraftforge.fml.common.discovery;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.List;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.LoaderException;
import net.minecraftforge.fml.common.ModClassLoader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.FileListHelper;
import net.minecraftforge.fml.relauncher.ModListHelper;

import org.apache.logging.log4j.Level;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

public class ModDiscoverer
{
    private List<ModCandidate> candidates = Lists.newArrayList();

    private ASMDataTable dataTable = new ASMDataTable();

    private List<File> nonModLibs = Lists.newArrayList();

    public void findClasspathMods(ModClassLoader modClassLoader)
    {
        List<String> knownLibraries = ImmutableList.<String>builder()
                // skip default libs
                .addAll(modClassLoader.getDefaultLibraries())
                // skip loaded coremods
                .addAll(CoreModManager.getLoadedCoremods())
                // skip reparse coremods here
                .addAll(CoreModManager.getReparseableCoremods())
                .build();
        File[] minecraftSources = modClassLoader.getParentSources();
        if (minecraftSources.length == 1 && minecraftSources[0].isFile())
        {
            FMLLog.fine("Minecraft is a file at %s, loading", minecraftSources[0].getAbsolutePath());
            addCandidate(new ModCandidate(minecraftSources[0], minecraftSources[0], ContainerType.JAR, true, true));
        }
        else
        {
            for (int i = 0; i < minecraftSources.length; i++)
            {
                if (minecraftSources[i].isFile())
                {
                    if (knownLibraries.contains(minecraftSources[i].getName()))
                    {
                        FMLLog.finer("Skipping known library file %s", minecraftSources[i].getAbsolutePath());
                    }
                    else
                    {
                        FMLLog.fine("Found a minecraft related file at %s, examining for mod candidates", minecraftSources[i].getAbsolutePath());
                        addCandidate(new ModCandidate(minecraftSources[i], minecraftSources[i], ContainerType.JAR, i==0, true));
                    }
                }
                else if (minecraftSources[i].isDirectory())
                {
                    FMLLog.fine("Found a minecraft related directory at %s, examining for mod candidates", minecraftSources[i].getAbsolutePath());
                    addCandidate(new ModCandidate(minecraftSources[i], minecraftSources[i], ContainerType.DIR, i==0, true));
                }
            }
        }

    }

    public void findModDirMods(File modsDir)
    {
        // find all files ending in .jar in the mods folder
        File[] modsDirMods = modsDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname)
            {
                return pathname.getName().endsWith(".jar") || pathname.isDirectory();
            }
        });
        
        File[] additionalMods = ModListHelper.additionalMods.values().toArray(new File[ModListHelper.additionalMods.size()]);
        
        File[] sortedModsList = FileListHelper.sortFileList(ObjectArrays.concat(modsDirMods, additionalMods, File.class));

        for (File modFile : sortedModsList)
        {
            // skip loaded coremods
            if (CoreModManager.getLoadedCoremods().contains(modFile.getName()))
            {
                FMLLog.finer("Skipping already parsed coremod or tweaker %s", modFile.getName());
            }
            else if (modFile.isDirectory())
            {
                FMLLog.fine("Found a candidate mod directory %s", modFile.getName());
                addCandidate(new ModCandidate(modFile, modFile, ContainerType.DIR));
            }
            else
            {
                FMLLog.fine("Found a candidate jar file %s", modFile.getName());
                addCandidate(new ModCandidate(modFile, modFile, ContainerType.JAR));
            }
        }
    }

    public List<ModContainer> identifyMods()
    {
        List<ModContainer> modList = Lists.newArrayList();

        for (ModCandidate candidate : candidates)
        {
            try
            {
                List<ModContainer> mods = candidate.explore(dataTable);
                if (mods.isEmpty() && !candidate.isClasspath())
                {
                    nonModLibs.add(candidate.getModContainer());
                }
                else
                {
                    modList.addAll(mods);
                }
            }
            catch (LoaderException le)
            {
                FMLLog.log(Level.WARN, le, "Identified a problem with the mod candidate %s, ignoring this source", candidate.getModContainer());
            }
            catch (Throwable t)
            {
                Throwables.propagate(t);
            }
        }

        return modList;
    }

    public ASMDataTable getASMTable()
    {
        return dataTable;
    }

    public List<File> getNonModLibs()
    {
        return nonModLibs;
    }

    private void addCandidate(ModCandidate candidate)
    {
        for (ModCandidate c : candidates)
        {
            if (c.getModContainer().equals(candidate.getModContainer()))
            {
                FMLLog.finer("  Skipping already in list %s", candidate.getModContainer());
                return;
            }
        }
        candidates.add(candidate);
    }
}
