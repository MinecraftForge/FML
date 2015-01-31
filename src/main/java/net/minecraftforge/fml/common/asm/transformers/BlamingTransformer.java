package net.minecraftforge.fml.common.asm.transformers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.google.common.collect.ImmutableSet;

public class BlamingTransformer implements IClassTransformer
{
    private static final Map<String, String> classMap = new HashMap<String, String>();
    private static final Set<String> naughtyMods = new HashSet<String>();
    private static final Set<String> naughtyClasses = new TreeSet<String>();
    private static final Set<String> orphanNaughtyClasses = new HashSet();

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        if (bytes == null) { return null; }

        ClassReader classReader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        VersionVisitor visitor = new VersionVisitor(writer);
        classReader.accept(visitor, 0);
        return writer.toByteArray();
    }

    public static void blame(String modId, String cls)
    {
        naughtyClasses.add(cls);
        naughtyMods.add(modId);
        FMLLog.severe("Unsupported class format in mod %s: class %s", modId, cls);
    }

    public static class VersionVisitor extends ClassVisitor
    {
        public VersionVisitor(ClassVisitor cv)
        {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if(version > Opcodes.V1_6)
            {
                if(classMap.containsKey(name)) blame(classMap.get(name), name);
                else orphanNaughtyClasses.add(name);
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }

    private static void checkPendingNaughty()
    {
        ImmutableSet.Builder<String> toRemove = ImmutableSet.builder();
        for(String cls : orphanNaughtyClasses)
        {
            if(classMap.containsKey(cls))
            {
                String modId = classMap.get(cls);
                blame(modId, cls);
                toRemove.add(cls);
            }
        }
        orphanNaughtyClasses.removeAll(toRemove.build());
    }

    public static void addClasses(String modId, Set<String> classList)
    {
        for(String cls : classList)
        {
            classMap.put(cls, modId);
        }
        checkPendingNaughty();
    }

    public static void onCrash(StringBuilder builder)
    {
        checkPendingNaughty();
        if(!naughtyClasses.isEmpty())
        {
            builder.append("\n*** ATTENTION: detected classes with unsupported format ***\n");
            builder.append("*** DO NOT SUBMIT THIS CRASH REPORT TO FORGE ***\n\n");
            if(!naughtyMods.isEmpty())
            {
                builder.append("Contact authors of the following mods: \n");
                for(String modId : naughtyMods)
                {
                    builder.append("  ").append(modId).append("\n");
                }
            }
            if(!orphanNaughtyClasses.isEmpty())
            {
                builder.append("Unidentified unsupported classes: \n");
                for(String cls : orphanNaughtyClasses)
                {
                    builder.append("  ").append(cls).append("\n");
                }
            }
            builder.append('\n');
        }
    }
}
