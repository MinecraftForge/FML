package cpw.mods.fml.common.asm.transformers;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ASMDataTable.ASMData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class ModAPITransformer implements IClassTransformer {

    private static final boolean logDebugInfo = Boolean.valueOf(System.getProperty("fml.debugAPITransformer", "false"));
    private ListMultimap<String, ASMData> optionals;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (optionals == null || !optionals.containsKey(name))
        {
            return basicClass;
        }
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, 0);

        if (logDebugInfo) FMLRelaunchLog.finest("Optional removal - found optionals for class %s - processing", name);
        final String removalSkipped = "Optional removal skipped on %s %s - mod present %s";
        final String removalTriggered = "Optional on %s triggered - mod missing %s";
        for (ASMData optional : optionals.get(name))
        {
            if ("cpw.mods.fml.common.Optional$Interface".equals(optional.getAnnotationName()))
            {
                final String modId = (String) optional.getAnnotationInfo().get("modid");
                final String iFace = (String) optional.getAnnotationInfo().get("iface");

                if (Loader.isModLoaded(modId))
                {
                    if (logDebugInfo) FMLRelaunchLog.finest(removalSkipped, "interface", iFace, modId);
                    continue;
                }
                if (logDebugInfo) FMLRelaunchLog.finest(removalTriggered, name, modId);
                stripInterface(classNode, iFace);
            }
            else if ("cpw.mods.fml.common.Optional$Interfaces".equals(optional.getAnnotationName()))
            {
                List<Optional.Interface> interfaces = (List<Optional.Interface>) optional.getAnnotationInfo().get("value");
                for (Optional.Interface optionalInterface : interfaces)
                {
                    final String modId = optionalInterface.modid();
                    final String iFace = optionalInterface.iface();

                    if (Loader.isModLoaded(modId))
                    {
                        if (logDebugInfo) FMLRelaunchLog.finest(removalSkipped, "interface", iFace, modId);
                        continue;
                    }
                    if (logDebugInfo) FMLRelaunchLog.finest(removalTriggered, name, modId);
                    stripInterface(classNode, iFace);
                }
            }
            else
            {
                final String modId = (String) optional.getAnnotationInfo().get("modid");
                if (Loader.isModLoaded(modId))
                    if (logDebugInfo) FMLRelaunchLog.finest(removalSkipped, "method", optional.getObjectName(), modId);
                else
                    stripMethod(classNode, optional.getObjectName());
            }

        }
        if (logDebugInfo) FMLRelaunchLog.finest("Optional removal - class %s processed", name);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private void stripMethod(ClassNode classNode, String methodDescriptor)
    {
        for (ListIterator<MethodNode> iterator = classNode.methods.listIterator(); iterator.hasNext();)
        {
            MethodNode method = iterator.next();
            if (methodDescriptor.equals(method.name+method.desc))
            {
                iterator.remove();
                if (logDebugInfo) FMLRelaunchLog.finest("Optional removal - method %s removed", methodDescriptor);
                return;
            }
        }
        if (logDebugInfo) FMLRelaunchLog.finest("Optional removal - method %s NOT removed - not found", methodDescriptor);
    }

    private void stripInterface(ClassNode classNode, String interfaceName)
    {
        String ifaceName = interfaceName.replace('.', '/');
        boolean found = classNode.interfaces.remove(ifaceName);
        if (found && logDebugInfo) FMLRelaunchLog.finest("Optional removal - interface %s removed", interfaceName);
        if (!found && logDebugInfo) FMLRelaunchLog.finest("Optional removal - interface %s NOT removed - not found", interfaceName);
    }

    public void initTable(ASMDataTable dataTable)
    {
        optionals = ArrayListMultimap.create();
        Set<ASMData> interfaces = dataTable.getAll("cpw.mods.fml.common.Optional$Interface");
        addData(interfaces);
        interfaces = dataTable.getAll("cpw.mods.fml.common.Optional$Interfaces");
        addData(interfaces);
        Set<ASMData> methods = dataTable.getAll("cpw.mods.fml.common.Optional$Method");
        addData(methods);
    }

    private void addData(Set<ASMData> interfaces)
    {
        for (ASMData data : interfaces)
        {
            optionals.put(data.getClassName(),data);
        }
    }

}
