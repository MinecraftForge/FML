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

package cpw.mods.fml.common.asm.transformers;

import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.RemappingClassAdapter;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import cpw.mods.fml.common.asm.transformers.deobf.FMLRemappingAdapter;
import cpw.mods.fml.relauncher.IClassNameTransformer;
import cpw.mods.fml.relauncher.IClassTransformer;
import cpw.mods.fml.relauncher.RelaunchLibraryManager;
import cpw.mods.fml.relauncher.FMLRelaunchLog;

public class DeobfuscationTransformer implements IClassTransformer, IClassNameTransformer {

    private static List<String> forcedPrefixes;
    
    private static List<String> getForcedPrefixes() 
    {
        if (forcedPrefixes == null) 
        {
            String prefixList = System.getProperty("fml.forcedDeobfuscationPrefixes");
            if (prefixList != null && FMLDeobfuscatingRemapper.INSTANCE.isDeobfuscationDataLoaded())
            {
                forcedPrefixes = ImmutableList.copyOf(Splitter.on(';').split(prefixList));
                FMLRelaunchLog.fine("Prefixes selected for deobfuscation: " + prefixList);
            } 
            else
            {
                forcedPrefixes = ImmutableList.of();
            }
        }
        
        return forcedPrefixes;
    }
    
    private static boolean canTransform(String name) 
    {
        if (!RelaunchLibraryManager.isInDeobfuscatedEnvironment())
        {
            return true;
        }
        
        if (!FMLDeobfuscatingRemapper.INSTANCE.isDeobfuscationDataLoaded())
        {
            return false;
        }
        
        for (String prefix : getForcedPrefixes())
        {
            if (name.startsWith(prefix))
            {
                return true;
            }
        }
        
        return false;
    }
  
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        if (bytes == null || !canTransform(name))
        {
            return bytes;
        }
        
        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        RemappingClassAdapter remapAdapter = new FMLRemappingAdapter(classWriter);
        classReader.accept(remapAdapter, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    @Override
    public String remapClassName(String name)
    {
        return FMLDeobfuscatingRemapper.INSTANCE.map(name.replace('.','/')).replace('/', '.');
    }

    @Override
    public String unmapClassName(String name)
    {
        return FMLDeobfuscatingRemapper.INSTANCE.unmap(name.replace('.', '/')).replace('/','.');
    }

}
