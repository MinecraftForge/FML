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

package cpw.mods.fml.common.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import cpw.mods.fml.common.registry.BlockProxy;

public class ASMTransformer implements IClassTransformer
{
    @Override
    public byte[] transform(String name,String transformedName, byte[] bytes)
    {
        if ("net.minecraft.src.Block".equals(name))
        {
            ClassReader cr = new ClassReader(bytes);
            ClassNode cn = new ClassNode(Opcodes.ASM4);
            cr.accept(cn, ClassReader.EXPAND_FRAMES);
            cn.interfaces.add(Type.getInternalName(BlockProxy.class));
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        }

        return bytes;
    }

}
