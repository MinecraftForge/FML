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

package cpw.mods.fml.common.discovery.asm;

import java.util.LinkedList;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.google.common.collect.Lists;

public class ModMethodVisitor extends MethodVisitor
{

    private ASMModParser discoverer;
    private boolean inCode;
    private LinkedList<Label> labels = Lists.newLinkedList();
    private String foundProperties;
    private boolean validProperties;

    public ModMethodVisitor(String name, ASMModParser discoverer)
    {
        super(Opcodes.ASM4);
        this.discoverer = discoverer;
    }
    @Override
    public void visitCode()
    {
        labels.clear();
    }
    
    @Override
    public void visitLdcInsn(Object cst)
    {
        if (cst instanceof String && labels.size() == 1)
        {
            foundProperties = (String) cst;
        }
    }
    @Override
    public void visitInsn(int opcode)
    {
        if (Opcodes.ARETURN == opcode && labels.size() == 1 && foundProperties != null)
        {
            validProperties = true;
        }
    }
    @Override
    public void visitLabel(Label label)
    {
        labels.push(label);
    }
    
    @Override
    public void visitEnd()
    {
        if (validProperties)
        {
            discoverer.setBaseModProperties(foundProperties);
        }
    }
}
